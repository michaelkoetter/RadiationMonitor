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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import de.mkoetter.radmon.R;
import de.mkoetter.radmon.contentprovider.RadmonSessionContentProvider;
import de.mkoetter.radmon.db.MeasurementTable;
import de.mkoetter.radmon.db.SessionTable;

/**
 * Created by mk on 02.04.14.
 */
public class MeasurementsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int MEASUREMENT_LOADER = 0;
    private SimpleCursorAdapter adapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new SimpleCursorAdapter(getActivity(),
                R.layout.list_measurement,
                null,
                new String[] {MeasurementTable.COLUMN_ID, MeasurementTable.COLUMN_TIME, MeasurementTable.COLUMN_CPM},
                new int[] {R.id.txtMeasurementId, R.id.txtMeasurementTime, R.id.txtCPM},
                0);

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
