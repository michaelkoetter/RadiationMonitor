package de.mkoetter.radmon;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import de.mkoetter.radmon.db.Session;

public class MainActivity extends ActionBarActivity implements RadmonServiceClient, ActionBar.TabListener, ViewPager.OnPageChangeListener {


    private RadmonService radmonService = null;
    private Session currentSession = null;

    private ViewPager viewPager;
    private TabsPagerAdapter mAdapter;
    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // bind our helper service
        Intent radmonServiceIntent = new Intent(this, RadmonService.class);
        startService(radmonServiceIntent);
        bindService(radmonServiceIntent, radmonServiceConnection, BIND_AUTO_CREATE);

        actionBar = getSupportActionBar();
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setOnPageChangeListener(this);
        mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(mAdapter);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        String[] tabs = {getString(R.string.tab_current), getString(R.string.tab_logs)};
        for (String tab : tabs) {
            actionBar.addTab(actionBar.newTab().setText(tab).setTabListener(this));
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (radmonService != null) {
            radmonService.removeServiceClient(this);
        }
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
            if (currentSession == null) {
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
            if (currentSession != null) {
                // disconnect / stop
                radmonService.stopSession(currentSession);
            } else {
                // connect / start
                radmonService.startSession();
            }
        }
    }

    public void onUpdateCPM(final Long cpm) {
    }

    @Override
    public void onUpdateSession(final Session session) {
        currentSession = session;
        supportInvalidateOptionsMenu();
    }

    private ServiceConnection radmonServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            RadmonService.LocalBinder binder = (RadmonService.LocalBinder)iBinder;
            radmonService = binder.getService();

            radmonService.addServiceClient(MainActivity.this);
            // this calls onUpdateSession, onUpdateCPM
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
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onPageSelected(int position) {
        getSupportActionBar().setSelectedNavigationItem(position);
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }


    @Override
    public void onPageScrollStateChanged(int state) {
    }
}
