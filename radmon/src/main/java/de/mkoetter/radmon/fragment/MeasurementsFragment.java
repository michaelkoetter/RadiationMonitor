package de.mkoetter.radmon.fragment;


import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.Locale;

import de.mkoetter.radmon.R;
import de.mkoetter.radmon.db.MeasurementTable;

/**
 * Created by mk on 02.04.14.
 */
public class MeasurementsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int MEASUREMENT_LOADER = 0;
    private SimpleCursorAdapter adapter = null;

    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(
            DateFormat.SHORT, DateFormat.SHORT
    );

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.00",
            DecimalFormatSymbols.getInstance(Locale.US));

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new SimpleCursorAdapter(getActivity(),
                R.layout.list_measurement,
                null,
                new String[] {MeasurementTable.COLUMN_ID, MeasurementTable.COLUMN_TIME, MeasurementTable.COLUMN_CPM},
                new int[] {R.id.txtMeasurementId, R.id.txtMeasurementTime, R.id.txtCPM},
                0);

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (MeasurementTable.COLUMN_TIME.equals(cursor.getColumnName(columnIndex))) {

                    Long _timestamp = cursor.isNull(columnIndex) ? null : cursor.getLong(columnIndex);
                    if (_timestamp != null) {
                        ((TextView)view).setText(DATE_FORMAT.format(new Date(_timestamp)));
                    } else {
                        ((TextView)view).setText(null);
                    }
                    return true;

                }

                return false;
            }
        });

        setListAdapter(adapter);
        getLoaderManager().initLoader(MEASUREMENT_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri sessionUri = getActivity().getIntent().getData();

        switch (id) {
            case MEASUREMENT_LOADER:
                return new CursorLoader(getActivity(),
                        Uri.withAppendedPath(sessionUri, "measurements"),
                        MeasurementTable.ALL_COLUMNS,
                        null,
                        null,
                        null);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.d("radmon", "Measurement selected: " + id);
    }
}
