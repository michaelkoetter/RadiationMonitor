package de.mkoetter.radmon;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
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
    public static final String DATA_KEY_DOSE_RATE = "dose_rate";
    public static final String DATA_KEY_HISTORY = "history";

    public static final String BROADCAST_UPDATE_DATA = "de.mkoetter.radmon.UPDATE_DATA";

    private static final String TAG = "RadmonDataListenerSvc";

    private RadmonWearNotificationReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();

        receiver = new RadmonWearNotificationReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(BROADCAST_UPDATE_DATA));

        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);

        Log.d(TAG, "onDataChanged");

        for (DataEvent event : dataEvents) {
            DataItem item = event.getDataItem();
            if (DATA_PATH.equals(item.getUri().getPath())) {
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                updateData(
                        dataMap.getLong(DATA_KEY_CPM, -1),
                        dataMap.getDouble(DATA_KEY_DOSE_RATE, Double.NaN),
                        dataMap.getLongArray(DATA_KEY_HISTORY));
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived: " + messageEvent.getPath());

        if (DATA_PATH.equals(messageEvent.getPath())) {
            DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());
            updateData(
                    dataMap.getLong(DATA_KEY_CPM, -1),
                    dataMap.getDouble(DATA_KEY_DOSE_RATE, Double.NaN),
                    dataMap.getLongArray(DATA_KEY_HISTORY));
        }
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);

        Log.d(TAG, "onPeerDisconnected");

        // FIXME should broadcast something else
        updateData(-1, Double.NaN, null);
    }

    @Override
    public void onChannelClosed(Channel channel, int closeReason, int appSpecificErrorCode) {
        super.onChannelClosed(channel, closeReason, appSpecificErrorCode);

        Log.d(TAG, "onChannelClosed");

        // FIXME should broadcast something else
        updateData(-1, Double.NaN, null);
    }

    private void updateData(long cpm, double doseRate, long[] history) {
        // broadcast data
        Intent broadcastUpdateData = new Intent();
        broadcastUpdateData.setAction(BROADCAST_UPDATE_DATA);
        broadcastUpdateData.putExtra(DATA_KEY_CPM, cpm);
        broadcastUpdateData.putExtra(DATA_KEY_HISTORY, history);
        broadcastUpdateData.putExtra(DATA_KEY_DOSE_RATE, doseRate);

        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastUpdateData);
    }
}
