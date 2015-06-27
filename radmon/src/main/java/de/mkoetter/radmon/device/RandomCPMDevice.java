package de.mkoetter.radmon.device;

import android.content.SharedPreferences;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Michael on 14.03.14.
 */
public class RandomCPMDevice extends AbstractCPMDevice implements Runnable {

    private ScheduledExecutorService scheduler = null;
    private DeviceClient client;

    public static final String CONNECTION_TYPE = "RANDOM";

    public RandomCPMDevice(SharedPreferences preferences) {
        super(CONNECTION_TYPE, preferences);
    }

    @Override
    public synchronized void connect(final DeviceClient client) {
        disconnect();
        this.client = client;

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {

                client.onConnectionStatusChange(ConnectionStatus.Connecting, "Connecting...");

                scheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        client.onConnectionStatusChange(ConnectionStatus.Connected, "Connected");
                        scheduler.scheduleAtFixedRate(RandomCPMDevice.this, 0, 10, TimeUnit.SECONDS);
                    }
                }, 1, TimeUnit.SECONDS);
            }
        }, 0, TimeUnit.SECONDS);
    }

    @Override
    public synchronized void disconnect() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            if (client != null) {
                client.onConnectionStatusChange(ConnectionStatus.Disconnected, "Disconnected");
            }

            scheduler = null;
        }
    }

    @Override
    public void run() {
        long cpm = 15l + (new Random().nextInt(100) % 20);
        if (client != null) {
            client.onUpdateCPM(cpm);
        }
    }
}
