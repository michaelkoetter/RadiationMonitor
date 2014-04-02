package de.mkoetter.radmon.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by michael on 31.03.14.
 */
public class DeviceFactory {

    private Context context;

    public DeviceFactory(Context context) {
        this.context = context;
    }

    /**
     * Get a new instance of the currently configured device
     * (Random or Bluetooth)
     *
     * @return
     */
    public CPMDevice getConfiguredDevice() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String connectionType = prefs.getString("connectionType", null);
        if (RandomCPMDevice.CONNECTION_TYPE.equals(connectionType)) {
            return new RandomCPMDevice(prefs);
        } else if (BluetoothCPMDevice.CONNECTION_TYPE.equals(connectionType)) {
            return new BluetoothCPMDevice(prefs);
        }

        return null;
    }
}
