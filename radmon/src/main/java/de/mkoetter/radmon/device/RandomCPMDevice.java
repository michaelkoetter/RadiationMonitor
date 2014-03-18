package de.mkoetter.radmon.device;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Michael on 14.03.14.
 */
public class RandomCPMDevice implements Device, Runnable {

    private ScheduledExecutorService scheduler = null;
    private DeviceClient client;

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
                        scheduler.scheduleAtFixedRate(RandomCPMDevice.this, 5, 10, TimeUnit.SECONDS);
                    }
                }, 3, TimeUnit.SECONDS);
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
