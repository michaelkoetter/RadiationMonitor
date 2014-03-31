package de.mkoetter.radmon;

import de.mkoetter.radmon.db.Session;

/**
 * Created by Michael on 31.03.14.
 */
public interface RadmonServiceClient {
    public void onUpdateCPM(Long cpm);
    public void onUpdateSession(Session session);

}
