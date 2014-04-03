package de.mkoetter.radmon;

import android.net.Uri;

/**
 * Created by Michael on 31.03.14.
 */
public interface RadmonServiceClient {
    public void onStartSession(Uri session);
    public void onStopSession(Uri session);
}
