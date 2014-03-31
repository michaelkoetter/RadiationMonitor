package de.mkoetter.radmon.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;

import java.util.Date;

/**
 * Created by Michael on 31.03.14.
 */
public class SessionDataSource {

    private SessionDatabaseOpenHelper openHelper;

    public SessionDataSource(Context context) {
        openHelper = new SessionDatabaseOpenHelper(context);
    }

    private SQLiteDatabase getDatabase() {
        return openHelper.getWritableDatabase(); // this is cached by the SQLiteOpenHelper
    }

    public long createSession(Date startTime, String device, Double conversionFactor) {
        ContentValues values = new ContentValues();
        values.put(SessionTable.COLUMN_START_TIME, startTime.getTime());
        values.put(SessionTable.COLUMN_DEVICE, device);
        values.put(SessionTable.COLUMN_CONVERSION_FACTOR, conversionFactor);

        long sessionId = getDatabase().insert(SessionTable.TABLE_NAME, null, values);
        return sessionId;
    }

    public long addMeasurement(long sessionId, Date time, long cpm,
                               Location location) {
        ContentValues values = new ContentValues();
        values.put(MeasurementTable.COLUMN_SESSION_ID, sessionId);
        values.put(MeasurementTable.COLUMN_TIME, time.getTime());
        values.put(MeasurementTable.COLUMN_CPM, cpm);
        if (location != null) {
            values.put(MeasurementTable.COLUMN_LATITUDE, location.getLatitude());
            values.put(MeasurementTable.COLUMN_LONGITUDE, location.getLongitude());
            if (location.hasAltitude())
                values.put(MeasurementTable.COLUMN_ALTITUDE, location.getAltitude());
            if (location.hasAccuracy()) {
                values.put(MeasurementTable.COLUMN_ACCURACY, location.getAccuracy());
            }
        }

        long measurementId = getDatabase().insert(MeasurementTable.TABLE_NAME, null, values);
        return measurementId;
    }

    public Session getSession(long sessionId) {
        Cursor cursor = getDatabase().query(SessionTable.TABLE_NAME, SessionTable.ALL_COLUMNS,
                SessionTable.COLUMN_ID + " = " + sessionId, null, null, null, null);
        if (cursor.getCount() == 1) {
            cursor.moveToFirst();
            Session session = Session.fromCursor(cursor);
            cursor.close();
            return session;
        } else {
            throw new SQLiteException("expected one result for session id " + sessionId);
        }
    }
}
