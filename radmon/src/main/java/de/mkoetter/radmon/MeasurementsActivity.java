package de.mkoetter.radmon;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by mk on 03.04.14.
 */
public class MeasurementsActivity extends AppCompatActivity {

    private static final int SESSION_LOADER = 0;
    private static final int MEASUREMENTS_LOADER = 1;

    private Cursor session, measurements;

    ShareActionProvider shareActionProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_measurements);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.measurements, menu);

        MenuItem shareMenu = menu.findItem(R.id.action_share);
        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenu);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        Uri sessionUri = getIntent().getData();
        Uri shareUri = Uri.withAppendedPath(sessionUri, "measurements.csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, shareUri);
        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        shareActionProvider.setShareIntent(shareIntent);

        return true;
    }
}
