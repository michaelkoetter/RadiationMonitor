package de.mkoetter.radmon;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

import de.mkoetter.radmon.device.ConnectionStatus;
import de.mkoetter.radmon.device.Device;
import de.mkoetter.radmon.device.DeviceClient;
import de.mkoetter.radmon.device.RandomCPMDevice;
import de.mkoetter.radmon.device.SimpleBluetoothDevice;

public class MainActivity extends ActionBarActivity implements DeviceClient {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.00",
            DecimalFormatSymbols.getInstance(Locale.US));

    private ConnectionStatus connectionStatus = ConnectionStatus.Disconnected;
    private Device device = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the action bar to show a dropdown list.
        // final ActionBar actionBar = getSupportActionBar();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem connect = menu.findItem(R.id.action_connect);
        switch (connectionStatus) {
            case Disconnected:
                connect.setEnabled(true);
                connect.setTitle(R.string.action_connect);
                break;
            case Connected:
                connect.setEnabled(true);
                connect.setTitle(R.string.action_disconnect);
                break;
            case Connecting:
                connect.setEnabled(false);
                break;
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;

            case R.id.action_connect:
                toggleConnect();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void toggleConnect() {
        switch (connectionStatus) {
            case Disconnected:
                connect();
                break;
            case Connected:
                disconnect();
                break;
        }
    }
    private void disconnect() {
        device.disconnect();
    }

    private void connect() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String connectionType = prefs.getString("connectionType", null);
        if ("BLUETOOTH".equals(connectionType)) {
            String deviceAddress = prefs.getString("bluetoothDevice", null);
            if (deviceAddress != null) {
                device = new SimpleBluetoothDevice(deviceAddress);
            } else {
                Toast.makeText(this, R.string.bluetooth_device_not_set, Toast.LENGTH_LONG);
            }
        } else if ("RANDOM".equals(connectionType)) {
            device = new RandomCPMDevice();
        }

        if (device != null) {
            device.connect(this);
        }
    }

    @Override
    public void onUpdateCPM(final long cpm) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                String _conversionFactor = prefs.getString("conversionFactor", null);
                Double dose = 0d;

                if (_conversionFactor != null) {
                    try {
                        Number conversionFactor = SettingsActivity.DECIMAL_FORMAT.parse(_conversionFactor);
                        dose = cpm / conversionFactor.doubleValue();
                    } catch (ParseException e) {
                         // ignore
                    }
                }

                TextView txtCPM = (TextView) findViewById(R.id.txtCPM);
                TextView txtDose = (TextView) findViewById(R.id.txtDose);
                txtCPM.setText(Long.toString(cpm));
                txtDose.setText(DECIMAL_FORMAT.format(dose));
            }
        });
    }

    @Override
    public void onConnectionStatusChange(final ConnectionStatus status, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionStatus = status;
                if (message != null) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
                supportInvalidateOptionsMenu();
            }
        });
    }
}
