package de.mkoetter.radmon.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Michael on 31.03.14.
 */
public class SessionTable {

    public static final String TABLE_NAME = "session";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_START_TIME = "start_time";
    public static final String COLUMN_END_TIME = "end_time";
    public static final String COLUMN_DEVICE = "device";
    public static final String COLUMN_CONVERSION_FACTOR = "conversion_factor";
    public static final String COLUMN_UNIT = "unit";

    public static final String[] ALL_COLUMNS = {
            COLUMN_ID, COLUMN_START_TIME, COLUMN_END_TIME, COLUMN_DEVICE, COLUMN_CONVERSION_FACTOR, COLUMN_UNIT
    };

    private static final String TABLE_SESSION_CREATE = "CREATE TABLE " +
            TABLE_NAME + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY, " +
            COLUMN_START_TIME + " INTEGER NOT NULL, " +
            COLUMN_END_TIME + " INTEGER, " +
            COLUMN_DEVICE + " TEXT NOT NULL, " +
            COLUMN_CONVERSION_FACTOR + " REAL NOT NULL, " +
            COLUMN_UNIT + " TEXT NOT NULL );";

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(TABLE_SESSION_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        // not implemented
    }
}
