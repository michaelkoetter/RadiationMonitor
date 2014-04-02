package de.mkoetter.radmon.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import de.mkoetter.radmon.R;
import de.mkoetter.radmon.RadmonService;
import de.mkoetter.radmon.RadmonServiceClient;
import de.mkoetter.radmon.db.Session;

/**
 * Created by mk on 02.04.14.
 */
public class CurrentFragment extends Fragment implements RadmonServiceClient {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.00",
            DecimalFormatSymbols.getInstance(Locale.US));

    private RadmonService radmonService = null;
    private Session currentSession = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // bind service
        Intent radmonServiceIntent = new Intent(getActivity(), RadmonService.class);
        getActivity().bindService(radmonServiceIntent, radmonServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (radmonService != null) {
            radmonService.removeServiceClient(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_current, container, false);
        return rootView;
    }

    @Override
    public void onUpdateCPM(final Long cpm) {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Double dose = 0d;

                if (currentSession != null) {
                    Double conversionFactor = currentSession.getConversionFactor();
                    dose = cpm / conversionFactor;

                    TextView txtCPM = (TextView) getView().findViewById(R.id.txtCPM);
                    TextView txtDose = (TextView) getView().findViewById(R.id.txtDose);
                    TextView txtUnit = (TextView) getView().findViewById(R.id.txtDoseUnits);
                    txtUnit.setText(currentSession.getUnit());
                    txtCPM.setText(Long.toString(cpm));
                    txtDose.setText(DECIMAL_FORMAT.format(dose));
                }

            }
        });
    }

    @Override
    public void onUpdateSession(Session session) {
        this.currentSession = session;
    }

    private ServiceConnection radmonServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            RadmonService.LocalBinder binder = (RadmonService.LocalBinder)iBinder;
            radmonService = binder.getService();

            radmonService.addServiceClient(CurrentFragment.this);
            // this calls onUpdateSession, onUpdateCPM
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            radmonService = null;
        }
    };
}
