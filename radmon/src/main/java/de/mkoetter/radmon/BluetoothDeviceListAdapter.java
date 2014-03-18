package de.mkoetter.radmon;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Set;

/**
 * Created by Michael on 14.03.14.
 */
public class BluetoothDeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

    public BluetoothDeviceListAdapter(Context context, Set<BluetoothDevice> deviceSet) {
        super(context, R.layout.list_bluetooth_device, deviceSet.toArray(new BluetoothDevice[0]));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_bluetooth_device, null);
        }

        TextView txtName = (TextView) convertView.findViewById(R.id.txtName);
        TextView txtAddress = (TextView) convertView.findViewById(R.id.txtAddress);
        txtName.setText(getItem(position).getName());
        txtAddress.setText(getItem(position).getAddress());

        return convertView;
    }
}
