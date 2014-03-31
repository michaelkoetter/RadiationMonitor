package de.mkoetter.radmon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.util.Date;

import de.mkoetter.radmon.db.Session;
import de.mkoetter.radmon.db.SessionDataSource;
import de.mkoetter.radmon.device.ConnectionStatus;
import de.mkoetter.radmon.device.Device;
import de.mkoetter.radmon.device.DeviceClient;
import de.mkoetter.radmon.device.RandomCPMDevice;
import de.mkoetter.radmon.device.SimpleBluetoothDevice;

public class RadmonService extends Service implements DeviceClient {

    private NotificationManager notificationManager;
    private static final int ID_NOTIFICATION = 1;

    private Device device = null;
    private ConnectionStatus connectionStatus = null;

    private RadmonServiceClient client = null;

    // session data
    // TODO encapsulate session data
    private Long sessionId = null;
    private String connectionType = null;
    private Double conversionFactor = null;
    private Long lastCPM = null;

    private SessionDataSource sessionDataSource;

    public class LocalBinder extends Binder {
        RadmonService getService() {
            return RadmonService.this;
        }
    }

    private final IBinder localBinder = new LocalBinder();

    @Override
    public void onCreate() {
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        sessionDataSource = new SessionDataSource(this);
        startForeground(ID_NOTIFICATION, getServiceNotification("Idle"));
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private Notification getServiceNotification(String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Intent mainActivity = new Intent(this, MainActivity.class);
        mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent mainActivityIntent = PendingIntent.getActivity(
                this,
                0,
                mainActivity,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return builder
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Radiation Monitor")
                .setContentText(text)
                .setContentIntent(mainActivityIntent)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public void onUpdateCPM(final long cpm) {
        notificationManager.notify(ID_NOTIFICATION, getServiceNotification("New CPM: " + cpm));

        new Thread(new Runnable() {
            @Override
            public void run() {
                sessionDataSource.addMeasurement(sessionId, new Date(), cpm, null);
            }
        }).start();

        if (client != null) {
            client.onUpdateCPM(cpm);
        }
    }

    @Override
    public void onConnectionStatusChange(ConnectionStatus status, String message) {
        connectionStatus = status;
        if (message != null) {
            notificationManager.notify(ID_NOTIFICATION, getServiceNotification(message));
        }

        switch (connectionStatus) {
            case Connected:
                newSession();
                break;
            case Disconnected:
                finalizeSession();
                break;
        }
    }

    public void connect() {
        if (device == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            connectionType = prefs.getString("connectionType", null);
            if ("BLUETOOTH".equals(connectionType)) {
                String deviceAddress = prefs.getString("bluetoothDevice", null);
                if (deviceAddress != null) {
                    device = new SimpleBluetoothDevice(deviceAddress);
                } else {
                    Toast.makeText(this, R.string.bluetooth_device_not_set, Toast.LENGTH_LONG).show();
                }
            } else if ("RANDOM".equals(connectionType)) {
                device = new RandomCPMDevice();
            }

            String _conversionFactor = prefs.getString("conversionFactor", null);
            conversionFactor = Double.valueOf(_conversionFactor);

            if (device != null) {
                device.connect(this);
            }
        }
    }

    public void disconnect() {
        if (device != null) {
            device.disconnect();
            device = null;
        }
    }

    public boolean isConnected() {
        return device != null;
    }

    public Long getLastCPM() {
        return lastCPM;
    }

    public Session getCurrentSession() {
        if (sessionId != null) {
            return sessionDataSource.getSession(sessionId);
        }
        return null;
    }

    public void setServiceClient(RadmonServiceClient serviceClient) {
        this.client = serviceClient;
    }

    private void newSession() {
        if (sessionId != null) {
            finalizeSession();
        }
        sessionId = sessionDataSource.createSession(new Date(), connectionType, conversionFactor);
    }

    private void finalizeSession() {
        // ... TODO finalize the session
        this.sessionId = null;
    }
}
