package de.mkoetter.radmon;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceActivity;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Michael on 13.03.14.
 */
public class BluetoothDevicePreference extends DialogPreference {
    public BluetoothDevicePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt != null) {
            if (bt.isEnabled()) {
                final BluetoothDeviceListAdapter bluetoothDevices =
                        new BluetoothDeviceListAdapter(getContext(), bt.getBondedDevices());

                builder.setSingleChoiceItems(bluetoothDevices, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // we save the MAC address
                        persistString(bluetoothDevices.getItem(i).getAddress());

                        // .. and display the name in the summary
                        setSummary(bluetoothDevices.getItem(i).getName());
                        dialogInterface.dismiss();
                    }
                });
            } else {
                builder.setMessage(R.string.bluetooth_disabled);
            }
        } else {
            builder.setMessage(R.string.bluetooth_not_supported);
        }

        builder.setPositiveButton(null, null);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        String address = null;
        if (restorePersistedValue) {
            address = getPersistedString(null);
        } else {
            address = (String) defaultValue;
        }

        if (address != null) {
            // let's see if we can get the name of the device
            if (bt != null && bt.isEnabled()) {
                for (BluetoothDevice device : bt.getBondedDevices()) {
                    if (address.equals(device.getAddress())) {
                        setSummary(device.getName());
                        break;
                    }
                }
            } else {
                // bluetooth disabled or not available
                setSummary("(" + address + ")");
            }
        } else {
            // address not set - do nothing
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }
}
