package de.mkoetter.radmon.wearable;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import de.mkoetter.radmon.RadmonServiceClient;

/**
 * Created by Michael on 29.06.2015.
 */
public class WearableRadmonServiceClient implements RadmonServiceClient,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String DATA_KEY_CPM = "cpm";
    private static final String DATA_KEY_DOSE_RATE = "dose_rate";

    private static final String TAG = "WearableRadmonSvcClient";

    private GoogleApiClient googleApiClient;

    public WearableRadmonServiceClient(Context context) {
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApiIfAvailable(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }


    // RadmonServiceClient callbacks
    @Override
    public void onStartSession(Uri session) {
        googleApiClient.connect();

        PutDataRequest putSession = PutDataRequest.create(session.getPath());
        PendingResult<DataApi.DataItemResult> putDataItemResult =
                Wearable.DataApi.putDataItem(googleApiClient, putSession);

        // we don't need the result...
    }

    @Override
    public void onStopSession(Uri session) {
        Uri sessionDataItem = new Uri.Builder()
                .scheme(PutDataRequest.WEAR_URI_SCHEME)
                .path(session.getPath())
                .build();

        Wearable.DataApi.deleteDataItems(googleApiClient, sessionDataItem);

        googleApiClient.disconnect();
    }

    @Override
    public void onUpdateCPM(Uri session, long cpm, double doseRate) {
        if (googleApiClient.isConnected()) {
            PutDataMapRequest putSessionData = PutDataMapRequest.create(session.getPath());
            DataMap sessionDataMap = putSessionData.getDataMap();
            sessionDataMap.putLong(DATA_KEY_CPM, cpm);
            sessionDataMap.putDouble(DATA_KEY_DOSE_RATE, doseRate);

            PendingResult<DataApi.DataItemResult> putDataItemResult =
                    Wearable.DataApi.putDataItem(googleApiClient, putSessionData.asPutDataRequest());

            // we don't need the result...
        } else {
            // should not happen
            Log.w(TAG, "onUpdateCPM called while not connected to GoogleAPIClient");
        }
    }

    // GoogleAPI callbacks
    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
