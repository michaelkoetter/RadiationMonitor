package de.mkoetter.radmon.device;

/**
 * Created by Michael on 14.03.14.
 */
public interface DeviceClient {
    public void onUpdateCPM(long cpm);
    public void onConnectionStatusChange(ConnectionStatus status, String message);
}
