package de.mkoetter.radmon.fragment;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.mkoetter.radmon.R;
import de.mkoetter.radmon.RadmonService;
import de.mkoetter.radmon.RadmonServiceClient;
import de.mkoetter.radmon.contentprovider.RadmonSessionContentProvider;
import de.mkoetter.radmon.db.MeasurementTable;
import de.mkoetter.radmon.db.SessionTable;

/**
 * Created by mk on 02.04.14.
 */
public class CurrentFragment extends Fragment implements RadmonServiceClient {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.00",
            DecimalFormatSymbols.getInstance(Locale.US));

    private RadmonService radmonService = null;
    private Uri currentSession = null;
    private ContentObserver sessionObserver = null;

    private GraphView cpmGraphView = null;
    private GraphViewSeries cpmGraphViewSeries = null;

    private class SessionContentObserver extends ContentObserver {

        public SessionContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            // uri might not work (api level 16+)

            Log.d("radmon", "content update, uri: " + uri);

            // get session & latest measurement
            // FIXME this should be done in an async task

            updateContent();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionObserver = new SessionContentObserver(new Handler());

        // bind service
        Intent radmonServiceIntent = new Intent(getActivity(), RadmonService.class);
        getActivity().bindService(radmonServiceIntent, radmonServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        cpmGraphViewSeries = new GraphViewSeries(new GraphView.GraphViewData[0]);

        cpmGraphView = new BarGraphView(getActivity(),"");
        cpmGraphView.addSeries(cpmGraphViewSeries);

        LinearLayout graphViewContainer = (LinearLayout) view.findViewById(R.id.cpm_graph);
        graphViewContainer.addView(cpmGraphView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (radmonService != null) {
            radmonService.removeServiceClient(this);
        }

        getActivity().unbindService(radmonServiceConnection);
        getActivity().getContentResolver().unregisterContentObserver(sessionObserver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_current, container, false);
        return rootView;
    }

    private ServiceConnection radmonServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            RadmonService.LocalBinder binder = (RadmonService.LocalBinder)iBinder;
            radmonService = binder.getService();

            radmonService.addServiceClient(CurrentFragment.this);
            currentSession = radmonService.getCurrentSession();

            if (currentSession != null) {
                // register for updates of session & descendants
                getActivity().getContentResolver().registerContentObserver(currentSession, true, sessionObserver);
                updateContent();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            radmonService = null;
        }
    };

    @Override
    public void onStartSession(Uri session) {
        currentSession = session;

        // register for updates of session & descendants
        getActivity().getContentResolver().registerContentObserver(session, true, sessionObserver);
    }

    @Override
    public void onStopSession(Uri session) {
        currentSession = null;
        getActivity().getContentResolver().unregisterContentObserver(sessionObserver);
    }

    private Cursor loadSession(Uri session) {
        if (session != null) {
            Cursor _session = getActivity().getContentResolver().query(
                    session,
                    SessionTable.ALL_COLUMNS,
                    null, null, null);
            return _session;
        }

        return null;
    }

    private Cursor loadMeasurements(Uri session, boolean descending) {
        if (session != null) {
            long sessionId = ContentUris.parseId(session);
            Uri measurementsUri = RadmonSessionContentProvider.getMeasurementsUri(sessionId);

            Cursor _measurements = getActivity().getContentResolver().query(
                    measurementsUri,
                    MeasurementTable.ALL_COLUMNS,
                    null, null, MeasurementTable.COLUMN_TIME + (descending ? " DESC" : " ASC"));
            return _measurements;
        }

        return null;
    }


    private void updateContent() {
        Cursor session = loadSession(currentSession);
        Cursor measurements = loadMeasurements(currentSession, true);
        try {
            contentUpdated(session, measurements);
        } finally {
            if (session != null) session.close();
            if (measurements != null) measurements.close();
        }
    }

    private void contentUpdated(final Cursor session, final Cursor measurements) {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (session != null && session.moveToFirst()) {
                    int _conversionFactor = session.getColumnIndex(SessionTable.COLUMN_CONVERSION_FACTOR);
                    int _unit = session.getColumnIndex(SessionTable.COLUMN_UNIT);

                    Double conversionFactor = session.getDouble(_conversionFactor);
                    String unit = session.getString(_unit);

                    if (measurements != null && measurements.moveToFirst()) {
                        int _cpm = measurements.getColumnIndex(MeasurementTable.COLUMN_CPM);
                        int _time = measurements.getColumnIndex(MeasurementTable.COLUMN_TIME);
                        long cpm = measurements.getLong(_cpm);
                        long time = measurements.getLong(_time);

                        Double dose = cpm / conversionFactor;

                        TextView txtCPM = (TextView) getView().findViewById(R.id.txtCPM);
                        TextView txtDose = (TextView) getView().findViewById(R.id.txtDose);
                        TextView txtUnit = (TextView) getView().findViewById(R.id.txtDoseUnits);
                        txtUnit.setText(unit);
                        txtCPM.setText(Long.toString(cpm));
                        txtDose.setText(DECIMAL_FORMAT.format(dose));

                        GraphView.GraphViewData data = new GraphView.GraphViewData(time, cpm);
                        cpmGraphViewSeries.appendData(data, false, 30);
                        cpmGraphView.redrawAll();
                    }
                }

            }
        });
    }
}
