package de.mkoetter.radmon.device;

/**
 * Created by Michael on 14.03.14.
 */
public interface Device {
    public void connect(DeviceClient client);
    public void disconnect();
}
