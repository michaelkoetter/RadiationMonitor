package de.mkoetter.radmon;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class OngoingNotificationListenerService extends WearableListenerService {
    private static final String DATA_PATH = "/radmon/data";
    private static final String DATA_KEY_CPM = "cpm";
    private static final String DATA_KEY_DOSE_RATE = "dose_rate";
    private static final String DATA_KEY_DOSE_ACC = "dose_acc";

    private static final int ID_NOTIFICATION = 1;
    private static final long NO_DATA = -1;

    private GoogleApiClient googleApiClient;

    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    @Override
    public void onCreate() {
        super.onCreate();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();

        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Radiation Monitor")
                .setOngoing(true);

        notificationManager = NotificationManagerCompat.from(this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);

        for (DataEvent event : dataEvents) {
            DataItem item = event.getDataItem();
            if (DATA_PATH.equals(item.getUri().getPath())) {
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                updateData(
                        dataMap.getLong(DATA_KEY_CPM, -1),
                        dataMap.getDouble(DATA_KEY_DOSE_RATE, Double.NaN),
                        dataMap.getDouble(DATA_KEY_DOSE_ACC, Double.NaN));

            }
        }
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);

        notificationManager.cancel(ID_NOTIFICATION);
    }

    @Override
    public void onChannelClosed(Channel channel, int closeReason, int appSpecificErrorCode) {
        super.onChannelClosed(channel, closeReason, appSpecificErrorCode);

        notificationManager.cancel(ID_NOTIFICATION);
    }

    private void updateData(long cpm, double doseRate, double doseAccumulated) {
        if (cpm == NO_DATA) {
            notificationManager.cancel(ID_NOTIFICATION);
        } else {
            Notification notification = notificationBuilder
                    .setContentText("Current CPM: " + cpm)
                    .build();

            notificationManager.notify(ID_NOTIFICATION, notification);
        }
    }
}
