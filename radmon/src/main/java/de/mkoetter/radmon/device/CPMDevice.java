package de.mkoetter.radmon.device;

/**
 * Created by Michael on 14.03.14.
 */
public interface CPMDevice {
    public String getDeviceName();
    public double getConversionFactor();
    public String getUnit();

    /**
     * Connect to the device
     *
     * @param client A device client that receives status and cpm updates
     */
    public void connect(DeviceClient client);

    /**
     * Disconnect the device
     */
    public void disconnect();
}
