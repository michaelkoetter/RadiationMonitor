package de.mkoetter.radmon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.util.Date;

import de.mkoetter.radmon.db.Session;
import de.mkoetter.radmon.db.SessionDataSource;
import de.mkoetter.radmon.device.ConnectionStatus;
import de.mkoetter.radmon.device.CPMDevice;
import de.mkoetter.radmon.device.DeviceClient;
import de.mkoetter.radmon.device.DeviceFactory;

public class RadmonService extends Service implements DeviceClient {

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    private static final int ID_NOTIFICATION = 1;

    private DeviceFactory deviceFactory;
    private CPMDevice cpmDevice = null;

    private SessionDataSource sessionDataSource;
    private Session currentSession = null;
    private Long currentCPM = null;

    private RadmonServiceClient client = null;

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
        deviceFactory = new DeviceFactory(this);

        Intent mainActivity = new Intent(this, MainActivity.class);
        mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent mainActivityIntent = PendingIntent.getActivity(
                this,
                0,
                mainActivity,
                PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder = new NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentIntent(mainActivityIntent);
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
        notificationManager.notify(ID_NOTIFICATION, getServiceNotification("Current CPM: " + cpm));
        currentCPM = cpm;
        if (currentSession != null) {
            // TODO add location
            sessionDataSource.addMeasurement(currentSession.getId(),
                    new Date(), cpm, null);
        }

        if (client != null) {
            client.onUpdateCPM(cpm);
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
                startForeground(ID_NOTIFICATION, getServiceNotification("Starting session..."));

                long sessionId = sessionDataSource.createSession(new Date(),
                        cpmDevice.getDeviceName(),
                        cpmDevice.getConversionFactor(),
                        cpmDevice.getUnit());
                currentSession = sessionDataSource.getSession(sessionId);

                if (client != null) {
                    client.onUpdateSession(currentSession);
                }

                cpmDevice.connect(this);

            } else {
                Toast.makeText(this, "Session already in progress", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Could not configure device", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopSession(Session session) {
        if (cpmDevice != null) {
            cpmDevice.disconnect();
            cpmDevice = null;
        }

        if (currentSession != null) {
            // TODO finalize session
            currentSession = null;
            if (client != null) {
                client.onUpdateSession(null);
            }
        }

        stopForeground(true);
    }

    public Session getCurrentSession() {
        return currentSession;
    }

    public void setServiceClient(RadmonServiceClient serviceClient) {
        this.client = serviceClient;

        client.onUpdateSession(currentSession);
        client.onUpdateCPM(currentCPM);
    }
}
