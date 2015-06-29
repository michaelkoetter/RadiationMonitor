package de.mkoetter.radmon;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

public class RadmonDataListenerService extends WearableListenerService {
    private static final String DATA_PATH = "/radmon_data";

    public static final String DATA_KEY_CPM = "cpm";
    public static final String DATA_KEY_DOSE_RATE = "dose_rate";

    public static final String BROADCAST_UPDATE_DATA = "de.mkoetter.radmon.UPDATE_DATA";
    public static final String BROADCAST_CANCEL = "de.mkoetter.radmon.CANCEL";

    private static final String TAG = "RadmonDataListenerSvc";

    private RadmonWearNotificationReceiver receiver;

    private Uri currentSession = null;

    @Override
    public void onCreate() {
        super.onCreate();

        receiver = new RadmonWearNotificationReceiver(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_CANCEL);
        intentFilter.addAction(BROADCAST_UPDATE_DATA);

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged...");
        for (DataEvent dataEvent : dataEvents) {

            Log.d(TAG, "... " +
                    (dataEvent.getType() == DataEvent.TYPE_DELETED ? "D: " : "C: ") + dataEvent.getDataItem().getUri());

            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                if (currentSession == null) {
                    // start new session
                    currentSession = dataEvent.getDataItem().getUri();
                    onSessionStart(currentSession);
                }

                // handle data
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataEvent.getDataItem());
                onSessionUpdate(currentSession, dataMapItem.getDataMap());
            } else if (dataEvent.getType() == DataEvent.TYPE_DELETED) {
                if (currentSession.equals(dataEvent.getDataItem().getUri())) {
                    onSessionEnd(currentSession);
                    currentSession = null;
                } else {
                    Log.w(TAG, "received TYPE_DELETED for session I don't know: " + dataEvent.getDataItem().getUri());
                }
            }
        }
    }


    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);

        Log.d(TAG, "onPeerDisconnected: " + peer.getId());

        if (currentSession != null && currentSession.getAuthority().equals(peer.getId())) {
            // the peer who "owns" our session disconnected
            onSessionEnd(currentSession);
            currentSession = null;
        }
    }

    @Override
    public void onChannelClosed(Channel channel, int closeReason, int appSpecificErrorCode) {
        super.onChannelClosed(channel, closeReason, appSpecificErrorCode);

        Log.d(TAG, "onChannelClosed");

        // no data connection, end session
        if (currentSession != null) onSessionEnd(currentSession);
    }

    private void onSessionStart(Uri session) {
        // try to keep running...
        startService(new Intent(this, RadmonDataListenerService.class));
    }

    private void onSessionEnd(Uri session) {
        Intent broadcastCancel = new Intent(BROADCAST_CANCEL);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastCancel);

        // allow system to destroy us
        stopSelf();
    }

    private void onSessionUpdate(Uri session, DataMap data) {
        Intent broadcastUpdate = new Intent(BROADCAST_UPDATE_DATA);
        broadcastUpdate.putExtra(DATA_KEY_CPM, data.getLong(DATA_KEY_CPM));
        broadcastUpdate.putExtra(DATA_KEY_DOSE_RATE, data.getDouble(DATA_KEY_DOSE_RATE));
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastUpdate);
    }
}
