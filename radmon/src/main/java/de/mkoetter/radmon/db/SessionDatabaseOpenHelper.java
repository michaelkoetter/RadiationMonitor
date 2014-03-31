package de.mkoetter.radmon.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Michael on 31.03.14.
 */
public class SessionDatabaseOpenHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "session.db";
    public static final int DB_VERSION = 1;

    public SessionDatabaseOpenHelper(Context context) {
        super(context,DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        SessionTable.onCreate(sqLiteDatabase);
        MeasurementTable.onCreate(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        SessionTable.onUpgrade(sqLiteDatabase, i, i2);
        MeasurementTable.onUpgrade(sqLiteDatabase, i, i2);
    }
}
