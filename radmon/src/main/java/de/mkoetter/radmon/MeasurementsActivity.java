package de.mkoetter.radmon;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import de.mkoetter.radmon.db.MeasurementTable;
import de.mkoetter.radmon.db.SessionTable;
import de.mkoetter.radmon.util.CSVUtil;

/**
 * Created by mk on 03.04.14.
 */
public class MeasurementsActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int SESSION_LOADER = 0;
    private static final int MEASUREMENTS_LOADER = 1;

    private Cursor session, measurements;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_measurements);

        Button btnSave = (Button)findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSupportLoaderManager().initLoader(SESSION_LOADER, null, MeasurementsActivity.this);
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri = getIntent().getData();
        String[] columns = SessionTable.ALL_COLUMNS;

        switch (i) {
            case MEASUREMENTS_LOADER:
                uri = Uri.withAppendedPath(uri, "measurements");
                columns = MeasurementTable.ALL_COLUMNS;
            case SESSION_LOADER:
                return new CursorLoader(this,
                        uri,
                        columns,
                        null,
                        null,
                        null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        switch(cursorLoader.getId()) {
            case SESSION_LOADER:
                session = cursor;
                // now load measurements
                getSupportLoaderManager().initLoader(MEASUREMENTS_LOADER, null, this);
                break;
            case MEASUREMENTS_LOADER:
                measurements = cursor;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        saveData();
                    }
                }).start();

                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void saveData() {
        if (this.session != null && this.measurements != null) {
            try {
                session.moveToFirst();
                measurements.moveToFirst();

                int _sessionId = session.getColumnIndex(SessionTable.COLUMN_ID);
                Long sessionId = session.getLong(_sessionId);

                // save in documents dir
                File documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                final File csv = new File(documents, "radmon-" + sessionId + ".csv");
                if (documents != null) {
                    documents.mkdirs();

                    try {
                        FileOutputStream out = new FileOutputStream(csv);

                        writeHeader(session, measurements, out);
                        writeMeasurements(measurements, out);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MeasurementsActivity.this, "Saved file: " + csv.getName(), Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (final IOException ioe) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MeasurementsActivity.this, "Save failed: " + ioe.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MeasurementsActivity.this, "Save failed: External storage unavailable", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } finally {
                // if (session != null) session.close();
                // if (measurements != null) measurements.close();
            }
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MeasurementsActivity.this, "Save failed: Loader init error", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void writeHeader(Cursor session, Cursor measurements, OutputStream out) throws IOException {
        int _sessionId = session.getColumnIndex(SessionTable.COLUMN_ID);
        int _conversionFactor = session.getColumnIndex(SessionTable.COLUMN_CONVERSION_FACTOR);

        StringBuffer sbHeader = new StringBuffer("# Session: ")
                .append(session.getLong(_sessionId))
                .append("\n")
                .append("# CPM Conversion Factor: ")
                .append(session.getDouble(_conversionFactor))
                .append("\n");

        out.write(sbHeader.toString().getBytes());
        CSVUtil.writeHeader(measurements, out);
    }

    private void writeMeasurements(Cursor measurements, OutputStream out) throws IOException {
        do {
            CSVUtil.writeData(measurements, null, out);
        } while (measurements.moveToNext());
    }
}
