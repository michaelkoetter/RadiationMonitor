package de.mkoetter.radmon.device;

import android.content.SharedPreferences;

/**
 * Created by michael on 31.03.14.
 */
public abstract class AbstractCPMDevice implements CPMDevice {

    private static final String UNIT_MICROSIEVERTS_PER_HOUR = "µSv/h";
    private Double conversionFactor;
    private String deviceName;

    public AbstractCPMDevice(String deviceName, SharedPreferences preferences) {
        String _conversionFactor = preferences.getString("conversionFactor", null);
        conversionFactor = _conversionFactor == null ? 1 : Double.valueOf(_conversionFactor);

        this.deviceName = deviceName;
    }

    @Override
    public double getConversionFactor() {
        return conversionFactor;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getUnit() {
        return UNIT_MICROSIEVERTS_PER_HOUR;
    }
}
