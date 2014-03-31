package de.mkoetter.radmon;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
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

import de.mkoetter.radmon.db.Session;
import de.mkoetter.radmon.device.ConnectionStatus;
import de.mkoetter.radmon.device.Device;
import de.mkoetter.radmon.device.DeviceClient;
import de.mkoetter.radmon.device.RandomCPMDevice;
import de.mkoetter.radmon.device.SimpleBluetoothDevice;

public class MainActivity extends ActionBarActivity implements RadmonServiceClient {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.00",
            DecimalFormatSymbols.getInstance(Locale.US));

    private RadmonService radmonService = null;
    private Session currentSession = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the action bar to show a dropdown list.
        // final ActionBar actionBar = getSupportActionBar();

        // bind our helper service
        Intent radmonServiceIntent = new Intent(this, RadmonService.class);
        startService(radmonServiceIntent);
        bindService(radmonServiceIntent, radmonServiceConnection, BIND_AUTO_CREATE);
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

    /*
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
    */

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
        if (radmonService != null) {
            radmonService.connect();
        } else {
            Toast.makeText(this, "Service not bound", Toast.LENGTH_SHORT).show();
        }
    }

    public void onUpdateCPM(final long cpm) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Double dose = 0d;

                if (currentSession != null) {
                    Double conversionFactor = currentSession.getConversionFactor();
                    dose = cpm / conversionFactor;
                }

                TextView txtCPM = (TextView) findViewById(R.id.txtCPM);
                TextView txtDose = (TextView) findViewById(R.id.txtDose);
                txtCPM.setText(Long.toString(cpm));
                txtDose.setText(DECIMAL_FORMAT.format(dose));
            }
        });
    }

    private ServiceConnection radmonServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            RadmonService.LocalBinder binder = (RadmonService.LocalBinder)iBinder;
            radmonService = binder.getService();

            radmonService.setServiceClient(MainActivity.this);
            currentSession = radmonService.getCurrentSession();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            radmonService = null;
        }
    };
}
