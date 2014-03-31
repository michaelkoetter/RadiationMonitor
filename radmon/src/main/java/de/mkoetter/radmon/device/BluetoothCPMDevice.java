package de.mkoetter.radmon.device;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Created by Michael on 14.03.14.
 */
public class BluetoothCPMDevice extends AbstractCPMDevice implements Runnable {
    private static final UUID UUID_SPP =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private String address = null;
    private DeviceClient client = null;

    private BluetoothSocket socket = null;
    public static final String CONNECTION_TYPE = "BLUETOOTH";

    public BluetoothCPMDevice(SharedPreferences preferences) {
        super(CONNECTION_TYPE, preferences);
        this.address = preferences.getString("bluetoothDevice", null);
    }

    @Override
    public void connect(DeviceClient client) {
        disconnect();
        this.client = client;
        new Thread(this).start();
    }

    @Override
    public void disconnect() {
        if (socket != null) {
            try {
                socket.close();
                socket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void run() {


        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt != null && bt.isEnabled()) {
            BluetoothDevice remoteDevice = bt.getRemoteDevice(address);
            client.onConnectionStatusChange(ConnectionStatus.Connecting, "Connecting to '" + remoteDevice.getName() + "'...");

            try {
                socket = remoteDevice.createRfcommSocketToServiceRecord(UUID_SPP);
                socket.connect();

                InputStream cpmIn = socket.getInputStream();
                client.onConnectionStatusChange(ConnectionStatus.Connected, "Connected");

                StringBuffer sbCPM = new StringBuffer();
                int byteIn = 0;
                while ((byteIn = cpmIn.read()) >= 0) {
                    if (Character.isDigit(byteIn)) {
                        sbCPM.append((char)byteIn);
                    } else {
                        if (sbCPM.length() > 0) {
                            // output cpm
                            long cpm = Long.valueOf(sbCPM.toString());
                            client.onUpdateCPM(cpm);
                        }

                        sbCPM.setLength(0);
                    }
                }
                client.onConnectionStatusChange(ConnectionStatus.Disconnected, "Disconnected");
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (Exception e1) {
                    // ignore
                }
                client.onConnectionStatusChange(ConnectionStatus.Disconnected, "Bluetooth error: " + e.getMessage());
            }
        } else {
            client.onConnectionStatusChange(ConnectionStatus.Disconnected, "Bluetooth disabled");
        }
    }
}
