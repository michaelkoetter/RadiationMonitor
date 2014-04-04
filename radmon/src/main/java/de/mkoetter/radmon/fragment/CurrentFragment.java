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
import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    private static final DecimalFormat Y_LABEL_FORMAT = new DecimalFormat("#0.0",
            DecimalFormatSymbols.getInstance(Locale.US));

    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    // TODO make this a preference
    private static final int MEASUREMENTS_LIMIT = 30; // ~ 5 minutes at 10s interval

    private RadmonService radmonService = null;
    private Uri currentSession = null;
    private ContentObserver measurementsObserver = null;

    private GraphView cpmGraphView = null;
    private GraphViewSeries cpmGraphViewSeries = null;

    private class MeasurementsContentObserver extends ContentObserver {

        public MeasurementsContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            // FIXME uri might not work (api level 16+)

            // FIXME somehow we get onChange events for ancestors too ?!?!

            if (uri.getPath().endsWith("measurements")) {
                Log.d("radmon", "content update, uri: " + uri);

                // get session & latest measurements
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        updateContent(1); // limit to one measurement
                    }
                }).start();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        measurementsObserver = new MeasurementsContentObserver(new Handler());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        cpmGraphViewSeries = new GraphViewSeries(new GraphView.GraphViewData[0]);
        cpmGraphView = new BarGraphView(getActivity(),"");
        cpmGraphView.addSeries(cpmGraphViewSeries);
        cpmGraphView.setCustomLabelFormatter(new CustomLabelFormatter() {
            @Override
            public String formatLabel(double v, boolean xvalue) {
                if (xvalue) {
                    return TIME_FORMAT.format(new Date(Double.valueOf(v).longValue()));
                } else {
                    return Y_LABEL_FORMAT.format(v);
                }
            }
        });
        cpmGraphView.setManualYMinBound(0d);

        LinearLayout graphViewContainer = (LinearLayout) view.findViewById(R.id.cpm_graph);
        graphViewContainer.addView(cpmGraphView);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getContentResolver().unregisterContentObserver(measurementsObserver);

        if (radmonService != null) {
            radmonService.removeServiceClient(this);
        }

        getActivity().unbindService(radmonServiceConnection);
    }

    @Override
    public void onResume() {
        super.onResume();

        // bind service
        Intent radmonServiceIntent = new Intent(getActivity(), RadmonService.class);
        getActivity().bindService(radmonServiceIntent, radmonServiceConnection, Context.BIND_AUTO_CREATE);
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
            cpmGraphViewSeries.resetData(new GraphView.GraphViewData[0]);

            if (currentSession != null) {
                // register for updates of session & descendants
                registerMeasurementsContentObserver(currentSession);
                updateContent(MEASUREMENTS_LIMIT);
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

        // (re)register for updates of session & descendants
        getActivity().getContentResolver().unregisterContentObserver(measurementsObserver);
        registerMeasurementsContentObserver(currentSession);
        updateContent(MEASUREMENTS_LIMIT);
    }

    @Override
    public void onStopSession(Uri session) {
        currentSession = null;
        getActivity().getContentResolver().unregisterContentObserver(measurementsObserver);
    }

    private void registerMeasurementsContentObserver(Uri session) {
        Uri measurementsUri = RadmonSessionContentProvider.getMeasurementsUri(ContentUris.parseId(session));
        getActivity().getContentResolver().registerContentObserver(
                measurementsUri,
                true,
                measurementsObserver);
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

    private Cursor loadMeasurements(Uri session, int limit) {
        if (session != null) {
            long sessionId = ContentUris.parseId(session);
            Uri measurementsUri = RadmonSessionContentProvider.getMeasurementsUri(sessionId)
                    .buildUpon().appendQueryParameter(
                            RadmonSessionContentProvider.PARAM_LIMIT, Integer.toString(limit))
                    .build();

            Log.d("radmon", "measurements query uri: " + measurementsUri);

            Cursor _measurements = getActivity().getContentResolver().query(
                    measurementsUri,
                    MeasurementTable.ALL_COLUMNS,
                    null, null,
                    MeasurementTable.COLUMN_TIME + " DESC");
            return _measurements;
        }

        return null;
    }


    private void updateContent(int limit) {
        final Cursor session = loadSession(currentSession);
        final Cursor measurements = loadMeasurements(currentSession, limit);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    contentUpdated(session, measurements);
                } finally {
                    if (session != null) session.close();
                    if (measurements != null) measurements.close();
                }
            }
        });
    }

    private void contentUpdated(final Cursor session, final Cursor measurements) {
        final int _conversionFactor = session.getColumnIndex(SessionTable.COLUMN_CONVERSION_FACTOR);
        final int _unit = session.getColumnIndex(SessionTable.COLUMN_UNIT);
        final int _accumulated = session.getColumnIndex(SessionTable.COLUMN_ACCUMULATED_DOSE);

        final int _cpm = measurements.getColumnIndex(MeasurementTable.COLUMN_CPM);
        final int _time = measurements.getColumnIndex(MeasurementTable.COLUMN_TIME);

        TextView txtCPM = (TextView) getView().findViewById(R.id.txtCPM);
        TextView txtDose = (TextView) getView().findViewById(R.id.txtDose);
        TextView txtUnit = (TextView) getView().findViewById(R.id.txtDoseUnits);
        TextView txtAccumulatedDose = (TextView) getView().findViewById(R.id.txtAccumulatedDose);

        if (session != null && session.moveToFirst()) {

            Double conversionFactor = session.getDouble(_conversionFactor);
            String unit = session.getString(_unit);
            Long accumulated = session.isNull(_accumulated) ? null : session.getLong(_accumulated);

            if (accumulated != null) {
                Double accumulatedDose = accumulated / conversionFactor / 60d;
                txtAccumulatedDose.setText(DECIMAL_FORMAT.format(accumulatedDose));
            }

            if (measurements != null && measurements.moveToFirst()) {
                // first record is the latest
                long cpm = measurements.getLong(_cpm);
                long time;

                Double dose = cpm / conversionFactor;

                txtUnit.setText(unit);
                txtCPM.setText(Long.toString(cpm));
                txtDose.setText(DECIMAL_FORMAT.format(dose));

                measurements.moveToLast();
                do {
                    cpm = measurements.getLong(_cpm);
                    time = measurements.getLong(_time);
                    GraphView.GraphViewData data = new GraphView.GraphViewData(time, cpm);
                    cpmGraphViewSeries.appendData(data, false, MEASUREMENTS_LIMIT);
                } while (measurements.moveToPrevious());

                cpmGraphView.redrawAll();
            }
        }
    }
}
