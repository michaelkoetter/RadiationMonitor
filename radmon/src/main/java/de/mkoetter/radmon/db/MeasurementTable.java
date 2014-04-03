package de.mkoetter.radmon.db;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Michael on 31.03.14.
 */
public class MeasurementTable {

    public static final String TABLE_NAME = "measurement";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SESSION_ID = "session_id";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_CPM = "cpm";

    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_ALTITUDE = "altitude";
    public static final String COLUMN_ACCURACY = "radius";
    public static final String COLUMN_FIX_TIME = "fix_time";

    public static final String[] ALL_COLUMNS = {
            COLUMN_ID,
            COLUMN_SESSION_ID,
            COLUMN_TIME,
            COLUMN_CPM,
            COLUMN_LATITUDE,
            COLUMN_LONGITUDE,
            COLUMN_ALTITUDE,
            COLUMN_ACCURACY,
            COLUMN_FIX_TIME
    };

    private static final String TABLE_MEASUREMENT_CREATE = "CREATE TABLE " +
            TABLE_NAME + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY, " +
            COLUMN_SESSION_ID + " INTEGER NOT NULL, " +
            COLUMN_TIME + " INTEGER NOT NULL, " +
            COLUMN_CPM + " INTEGER NOT NULL, " +
            COLUMN_LATITUDE + " REAL, " +
            COLUMN_LONGITUDE + " REAL, " +
            COLUMN_ALTITUDE + " REAL, " +
            COLUMN_ACCURACY + " REAL, " +
            COLUMN_FIX_TIME + " INTEGER, " +
            "FOREIGN KEY (" + COLUMN_SESSION_ID + ") REFERENCES session(_id) );";

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(TABLE_MEASUREMENT_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        // not implemented
    }

}
