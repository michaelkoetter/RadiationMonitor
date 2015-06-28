package de.mkoetter.radmon;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BaseSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

import de.mkoetter.radmon.contentprovider.RadmonSessionContentProvider;
import de.mkoetter.radmon.db.MeasurementTable;
import de.mkoetter.radmon.db.SessionTable;
import de.mkoetter.radmon.device.CPMDevice;
import de.mkoetter.radmon.device.ConnectionStatus;
import de.mkoetter.radmon.device.DeviceClient;
import de.mkoetter.radmon.device.DeviceFactory;

public class RadmonService extends Service implements DeviceClient,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String DATA_PATH = "/radmon_data";
    private static final String DATA_KEY_CPM = "cpm";
    private static final String DATA_KEY_DOSE_RATE = "dose_rate";
    private static final String DATA_KEY_DOSE_ACC = "dose_acc";
    private static final String DATA_KEY_HISTORY = "history";

    private static final long NO_DATA = -1;
    private static final int HISTORY_SIZE = 30;


    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private GoogleApiClient googleApiClient;

    private static final int ID_NOTIFICATION = 1;

    private DeviceFactory deviceFactory;
    private CPMDevice cpmDevice = null;

    private Uri currentSession = null;

    // accumulation:
    private long lastCpmTime = 0;
    private double totalCounts = 0;

    // history
    private long[] history = new long[HISTORY_SIZE];

    private List<RadmonServiceClient> serviceClients;


    private GraphView wearableGraphView;
    private BaseSeries<DataPoint> wearableGraphSeries;

    public class LocalBinder extends Binder {
        public RadmonService getService() {
            return RadmonService.this;
        }
    }

    private final IBinder localBinder = new LocalBinder();

    @Override
    public void onCreate() {

        wearableGraphView = new GraphView(this);
        wearableGraphSeries = new LineGraphSeries<>();
        wearableGraphView.addSeries(wearableGraphSeries);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        notificationManager = NotificationManagerCompat.from(this);

        deviceFactory = new DeviceFactory(this);

        Intent viewIntent = new Intent(this, MainActivity.class);
        viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent viewPendingIntent = PendingIntent.getActivity(
                this,
                0,
                viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder = new NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentIntent(viewPendingIntent);

        serviceClients = new ArrayList<RadmonServiceClient>();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);

        disconnectWearable();

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private Notification getServiceNotification(String text) {
        return notificationBuilder
                .setContentText(text)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public void onUpdateCPM(final long cpm) {
        // update history
        System.arraycopy(history, 1, history, 0, HISTORY_SIZE-1);
        history[HISTORY_SIZE-1] = cpm;

        notificationManager.notify(ID_NOTIFICATION, getServiceNotification("Current CPM: " + cpm));

        // update accumulated values
        if (lastCpmTime > 0) {
            long diff = System.currentTimeMillis() - lastCpmTime;
            double actualCounts = cpm * ((double)diff / 60000); // estimated counts in interval
            totalCounts += actualCounts;

            Log.d("radmon", "accumulation: cpm = " + cpm + " actualCounts = " + actualCounts + " totalCounts = " + totalCounts);
        }

        lastCpmTime = System.currentTimeMillis();

        if (currentSession != null) {
            long sessionId = ContentUris.parseId(currentSession);

            // update session
            ContentValues session = new ContentValues();
            session.put(SessionTable.COLUMN_END_TIME, System.currentTimeMillis());
            session.put(SessionTable.COLUMN_ACCUMULATED_DOSE,
                    Double.valueOf(totalCounts).longValue());
            getContentResolver().update(currentSession, session, null, null);

            // add measurement
            // TODO add location
            ContentValues measurement = new ContentValues();
            measurement.put(MeasurementTable.COLUMN_SESSION_ID, sessionId);
            measurement.put(MeasurementTable.COLUMN_TIME, System.currentTimeMillis());
            measurement.put(MeasurementTable.COLUMN_CPM, cpm);

            getContentResolver().insert(
                    RadmonSessionContentProvider.getMeasurementsUri(sessionId),
                    measurement);

            updateWearable(cpm);
        }
    }

    @Override
    public void onConnectionStatusChange(ConnectionStatus status, String message) {
        if (message != null) {
            notificationManager.notify(ID_NOTIFICATION, getServiceNotification(message));
        }
    }

    public void startSession() {
        if (cpmDevice != null) {
            cpmDevice.disconnect();
        }

        cpmDevice = deviceFactory.getConfiguredDevice();
        if (cpmDevice != null) {
            if (currentSession == null) {
                connectWearable();

                startForeground(ID_NOTIFICATION, getServiceNotification("Starting session..."));

                ContentValues values = new ContentValues();
                values.put(SessionTable.COLUMN_START_TIME, System.currentTimeMillis());
                values.put(SessionTable.COLUMN_DEVICE, cpmDevice.getDeviceName());
                values.put(SessionTable.COLUMN_CONVERSION_FACTOR, cpmDevice.getConversionFactor());
                values.put(SessionTable.COLUMN_UNIT, cpmDevice.getUnit());

                currentSession = getContentResolver().insert(
                        RadmonSessionContentProvider.getSessionsUri(),
                        values);

                for (RadmonServiceClient client : serviceClients) {
                    client.onStartSession(currentSession);
                }

                cpmDevice.connect(this);

            } else {
                Toast.makeText(this, "Session already in progress", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Could not configure device", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopSession() {
        if (cpmDevice != null) {
            cpmDevice.disconnect();
            cpmDevice = null;
        }

        if (currentSession != null) {
            // TODO finalize session
            for (RadmonServiceClient client : serviceClients) {
                client.onStopSession(currentSession);
            }

            currentSession = null;
        }

        stopForeground(true);
        disconnectWearable();
    }

    public Uri getCurrentSession() {
        return currentSession;
    }

    public synchronized void addServiceClient(RadmonServiceClient serviceClient) {

        if (!serviceClients.contains(serviceClient))
            serviceClients.add(serviceClient);
    }

    public synchronized void removeServiceClient(RadmonServiceClient serviceClient) {
        serviceClients.remove(serviceClient);
    }

    private void updateWearable(final long cpm) {
        // update wearable data
        if (googleApiClient.isConnected()) {
            Wearable.NodeApi.getConnectedNodes(googleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                @Override
                public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                    for (Node node : getConnectedNodesResult.getNodes()) {
                        DataMap dataMap = new DataMap();

                        dataMap.putLong(DATA_KEY_CPM, cpm);

                        if (cpmDevice != null) {
                            double doseRate = cpm / cpmDevice.getConversionFactor();
                            dataMap.putDouble(DATA_KEY_DOSE_RATE, doseRate);
                        }

                        if (history != null) {
                            dataMap.putLongArray(DATA_KEY_HISTORY, history);
                        }

                        Wearable.MessageApi.sendMessage(googleApiClient,
                                node.getId(), DATA_PATH, dataMap.toByteArray());
                    }

                }
            });

        }
    }

    private void connectWearable() {
        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
        }
    }

    private void disconnectWearable() {
        updateWearable(NO_DATA);

        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    // GoogleApiClient callbacks

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
