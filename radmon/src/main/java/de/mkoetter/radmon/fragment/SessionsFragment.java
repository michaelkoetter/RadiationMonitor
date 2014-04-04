package de.mkoetter.radmon.fragment;


import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
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
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.mkoetter.radmon.MeasurementsActivity;
import de.mkoetter.radmon.R;
import de.mkoetter.radmon.contentprovider.RadmonSessionContentProvider;
import de.mkoetter.radmon.db.SessionTable;

/**
 * Created by mk on 02.04.14.
 */
public class SessionsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int SESSION_LOADER = 0;
    private SimpleCursorAdapter adapter = null;

    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(
            DateFormat.SHORT, DateFormat.SHORT
    );

    private String[] from = new String[]{
            SessionTable.COLUMN_ID,
            SessionTable.COLUMN_START_TIME,
            SessionTable.COLUMN_END_TIME,
            SessionTable.COLUMN_DEVICE
    };

    private final int[] to = new int[]{
            R.id.txtSessionId,
            R.id.txtStartTime,
            R.id.txtEndTime,
            R.id.txtDevice
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new SimpleCursorAdapter(getActivity(),
                R.layout.list_session,
                null,
                from,
                to,
                0);

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (SessionTable.COLUMN_START_TIME.equals(cursor.getColumnName(columnIndex))
                    || SessionTable.COLUMN_END_TIME.equals(cursor.getColumnName(columnIndex))) {

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
        getLoaderManager().initLoader(SESSION_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case SESSION_LOADER:
                return new CursorLoader(getActivity(),
                        RadmonSessionContentProvider.CONTENT_URI,
                        SessionTable.ALL_COLUMNS,
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
        Log.d("radmon", "Session selected: " + id);

        Intent measurementsIntent = new Intent(getActivity(), MeasurementsActivity.class);
        measurementsIntent.setData(ContentUris.withAppendedId(RadmonSessionContentProvider.CONTENT_URI, id));
        startActivity(measurementsIntent);
    }
}
