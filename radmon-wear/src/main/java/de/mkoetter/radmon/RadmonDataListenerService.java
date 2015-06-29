package de.mkoetter.radmon;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class RadmonDataListenerService extends WearableListenerService {
    private static final String DATA_PATH = "/radmon_data";

    public static final String DATA_KEY_CPM = "cpm";
    public static final String DATA_KEY_SEQUENCE = "sequence";
    public static final String DATA_KEY_DOSE_RATE = "dose_rate";
    public static final String DATA_KEY_HISTORY = "history";
    public static final String DATA_KEY_REDUCED_UPDATE_RATE = "reduced_update_rate";

    public static final String BROADCAST_UPDATE_DATA = "de.mkoetter.radmon.UPDATE_DATA";
    public static final String BROADCAST_CANCEL = "de.mkoetter.radmon.CANCEL";

    private static long DOZE_UPDATE_INTERVAL = 10;

    private static final String TAG = "RadmonDataListenerSvc";

    private RadmonWearNotificationReceiver receiver;

    private PowerManager powerManager;

    @Override
    public void onCreate() {
        super.onCreate();

        receiver = new RadmonWearNotificationReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_CANCEL);
        intentFilter.addAction(BROADCAST_UPDATE_DATA);

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived: " + messageEvent.getPath());

        if (DATA_PATH.equals(messageEvent.getPath())) {
            DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());
            if (shouldUpdate(dataMap)) {
                Intent broadcastUpdateData = new Intent();
                broadcastUpdateData.setAction(BROADCAST_UPDATE_DATA);
                broadcastUpdateData.putExtra(DATA_KEY_CPM, dataMap.getLong(DATA_KEY_CPM, -1));
                broadcastUpdateData.putExtra(DATA_KEY_HISTORY, dataMap.getLongArray(DATA_KEY_HISTORY));
                broadcastUpdateData.putExtra(DATA_KEY_DOSE_RATE, dataMap.getDouble(DATA_KEY_DOSE_RATE));
                broadcastUpdateData.putExtra(DATA_KEY_REDUCED_UPDATE_RATE, dataMap.getBoolean(DATA_KEY_REDUCED_UPDATE_RATE, false));

                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastUpdateData);
            }
        }
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);

        Log.d(TAG, "onPeerDisconnected");

        Intent broadcastCancel = new Intent();
        broadcastCancel.setAction(BROADCAST_CANCEL);

        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastCancel);
    }

    @Override
    public void onChannelClosed(Channel channel, int closeReason, int appSpecificErrorCode) {
        super.onChannelClosed(channel, closeReason, appSpecificErrorCode);

        Log.d(TAG, "onChannelClosed");

        Intent broadcastCancel = new Intent();
        broadcastCancel.setAction(BROADCAST_CANCEL);

        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastCancel);
    }

    /**
     * Decide if we should update the screen. This will lower the update rate when the device is asleep.
     *
     * @param dataMap
     * @return
     */
    private boolean shouldUpdate(DataMap dataMap) {;
        if (!powerManager.isInteractive()) {
            // we use a simple sequence and update every n-th call...
            long sequence = dataMap.getLong(DATA_KEY_SEQUENCE, 1);
            dataMap.putBoolean(DATA_KEY_REDUCED_UPDATE_RATE, true);
            return (sequence % DOZE_UPDATE_INTERVAL) == 0;
        }

        dataMap.putBoolean(DATA_KEY_REDUCED_UPDATE_RATE, false);
        return true;
    }

}
