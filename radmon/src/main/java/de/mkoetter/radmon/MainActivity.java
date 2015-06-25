package de.mkoetter.radmon;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity implements RadmonServiceClient {

    private RadmonService radmonService = null;
    private boolean sessionActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // bind our helper service
        Intent radmonServiceIntent = new Intent(this, RadmonService.class);
        startService(radmonServiceIntent);
        bindService(radmonServiceIntent, radmonServiceConnection, BIND_AUTO_CREATE);

        ActionBar actionBar = getSupportActionBar();

        actionBar.setHomeButtonEnabled(false);

        String[] tabs = {getString(R.string.tab_current), getString(R.string.tab_logs)};
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new TabsPagerAdapter(tabs, getSupportFragmentManager()));

        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (radmonService != null) {
            radmonService.removeServiceClient(this);
        }

        unbindService(radmonServiceConnection);
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
        if (radmonService != null) {
            connect.setEnabled(true);
            if (!sessionActive) {
                connect.setTitle(R.string.action_connect);
            } else {
                connect.setTitle(R.string.action_disconnect);
            }
        } else {
            connect.setEnabled(false);
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
        if (radmonService != null) {
            if (sessionActive) {
                radmonService.stopSession();
            } else {
                radmonService.startSession();
            }
        }
    }

    private ServiceConnection radmonServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            RadmonService.LocalBinder binder = (RadmonService.LocalBinder)iBinder;
            radmonService = binder.getService();

            radmonService.addServiceClient(MainActivity.this);
            sessionActive = radmonService.getCurrentSession() != null;

            invalidateOptionsMenu();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            radmonService = null;
        }
    };

    public RadmonService getRadmonService() {
        return radmonService;
    }

    @Override
    public void onStartSession(Uri session) {
        sessionActive = true;
        invalidateOptionsMenu();
    }

    @Override
    public void onStopSession(Uri session) {
        sessionActive = false;
        invalidateOptionsMenu();
    }

    private interface FragmentInstantiationCallback {
        void instantiated(Fragment fragment);
        void exists(Fragment fragment);
    }
}
