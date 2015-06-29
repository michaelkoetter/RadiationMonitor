package de.mkoetter.radmon.contentprovider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import de.mkoetter.radmon.db.MeasurementTable;
import de.mkoetter.radmon.db.SessionDatabaseOpenHelper;
import de.mkoetter.radmon.db.SessionTable;
import de.mkoetter.radmon.util.CSVUtil;

/**
 * A content provider for Radmon session data and measurements.
 *
 * Created by mk on 03.04.14.
 */
public class RadmonSessionContentProvider extends ContentProvider {

    private static final String AUTHORITY = "de.mkoetter.radmon.contentprovider";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/sessions");

    public static final String CONTENT_TYPE_SESSIONS = ContentResolver.CURSOR_DIR_BASE_TYPE + "/radmon_session";
    public static final String CONTENT_TYPE_SESSIONS_ID = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/radmon_session";
    public static final String CONTENT_TYPE_MEASUREMENTS = ContentResolver.CURSOR_DIR_BASE_TYPE + "/radmon_measurement";
    public static final String CONTENT_TYPE_MEASUREMENTS_CSV = "text/csv";
    public static final String CONTENT_TYPE_MEASUREMENTS_ID = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/radmon_measurement";

    public static final String PARAM_LIMIT = "limit";

    // uri matcher
    private static final int URI_SESSIONS = 10;
    private static final int URI_SESSIONS_ID = 20;
    private static final int URI_MEASUREMENTS = 30;
    private static final int URI_MEASUREMENTS_CSV = 50;
    private static final int URI_MEASUREMENTS_ID = 40;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, "sessions", URI_SESSIONS);
        uriMatcher.addURI(AUTHORITY, "sessions/#", URI_SESSIONS_ID);
        uriMatcher.addURI(AUTHORITY, "sessions/#/measurements", URI_MEASUREMENTS);
        uriMatcher.addURI(AUTHORITY, "sessions/#/measurements.csv", URI_MEASUREMENTS_CSV);
        uriMatcher.addURI(AUTHORITY, "sessions/#/measurements/#", URI_MEASUREMENTS_ID);
    }

    private SessionDatabaseOpenHelper database;

    @Override
    public boolean onCreate() {
        database = new SessionDatabaseOpenHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String[] originalProjection = Arrays.copyOf(projection, projection.length);

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        String limit = uri.getQueryParameter(PARAM_LIMIT);
        int uriType = uriMatcher.match(uri);

        switch (uriType) {
            case URI_SESSIONS_ID:
                queryBuilder.appendWhere(SessionTable.COLUMN_ID + "=" + getIdFromPath(uri, 1));
            case URI_SESSIONS:
                queryBuilder.setTables(SessionTable.TABLE_NAME);
                break;
            case URI_MEASUREMENTS_CSV:
                return new CSVFileInfoCursorWrapper(projection);
            case URI_MEASUREMENTS_ID:
                queryBuilder.appendWhere(MeasurementTable.COLUMN_ID + "=" + getIdFromPath(uri, 3));
            case URI_MEASUREMENTS:
                queryBuilder.appendWhere(MeasurementTable.COLUMN_SESSION_ID + "=" + getIdFromPath(uri, 1));
                queryBuilder.setTables(MeasurementTable.TABLE_NAME);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }


        checkColumns(projection, uriType);

        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder, limit);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);


        return cursor;
    }

    private void writeHeader(Cursor session, Cursor measurements, OutputStream out) throws IOException {
        int _sessionId = session.getColumnIndex(SessionTable.COLUMN_ID);
        int _conversionFactor = session.getColumnIndex(SessionTable.COLUMN_CONVERSION_FACTOR);

        StringBuffer sbHeader = new StringBuffer("# Session: ")
                .append(session.getLong(_sessionId))
                .append("\n")
                .append("# CPM Conversion Factor: ")
                .append(session.getDouble(_conversionFactor))
                .append("\n");

        out.write(sbHeader.toString().getBytes());
    }

    private void writeMeasurements(Cursor measurements, OutputStream out) throws IOException {
        measurements.moveToFirst();
        CSVUtil.writeHeader(measurements, out);
        do {
            CSVUtil.writeData(measurements, null, out);
        } while (measurements.moveToNext());
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return openPipeHelper(uri, null, null, null, new PipeDataWriter<Object>() {
            @Override
            public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType, Bundle opts, Object args) {
                Uri contentUri = Uri.parse(uri.toString().replace(".csv", ""));
                Cursor measurements = query(contentUri, MeasurementTable.ALL_COLUMNS, null, null, null);

                try {
                    OutputStream out = new FileOutputStream(output.getFileDescriptor());
                    writeMeasurements(measurements, out);
                    out.close();
                } catch (IOException e) {
                    Log.e("RadmonContentProvider", "Error writing CSV data", e);
                }
            }
        });

    }

    @Override
    public String getType(Uri uri) {
        int uriType = uriMatcher.match(uri);
        switch (uriType) {
            case URI_SESSIONS:
                return CONTENT_TYPE_SESSIONS;
            case URI_SESSIONS_ID:
                return CONTENT_TYPE_SESSIONS_ID;
            case URI_MEASUREMENTS:
                return CONTENT_TYPE_MEASUREMENTS;
            case URI_MEASUREMENTS_CSV:
                return CONTENT_TYPE_MEASUREMENTS_CSV;
            case URI_MEASUREMENTS_ID:
                return CONTENT_TYPE_MEASUREMENTS_ID;
        }

        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase db = database.getWritableDatabase();
        long id = 0;
        switch (uriType) {
            case URI_SESSIONS:
                id = db.insert(SessionTable.TABLE_NAME, null, contentValues);
                break;
            case URI_MEASUREMENTS:
                id = db.insert(MeasurementTable.TABLE_NAME, null, contentValues);
                break;
            default:
                throw new IllegalArgumentException("Invalid URI for insert: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase db = database.getWritableDatabase();

        int updatedRows = 0;

        switch (uriType) {
            case URI_SESSIONS_ID:
                long sessionId = ContentUris.parseId(uri);
                updatedRows = db.update(SessionTable.TABLE_NAME,
                        contentValues,
                        SessionTable.COLUMN_ID + "=" + sessionId,
                        null);
                break;
            default:
                throw new IllegalArgumentException("Invalid URI for update: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return updatedRows;
    }

    private long getIdFromPath(Uri uri, int index) {
        List<String> segments = uri.getPathSegments();
        String _id = segments.get(index);
        return Long.valueOf(_id);
    }

    private void checkColumns(String[] columns, int uriType) {
        String[] validColumns = null;
        switch (uriType) {
            case URI_SESSIONS:
            case URI_SESSIONS_ID:
                validColumns = SessionTable.ALL_COLUMNS;
                break;
            case URI_MEASUREMENTS:
            case URI_MEASUREMENTS_CSV:
            case URI_MEASUREMENTS_ID:
                validColumns = MeasurementTable.ALL_COLUMNS;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI type " + uriType);
        }

        Arrays.sort(validColumns);

        for (String projectionCol : columns) {
            if (Arrays.binarySearch(validColumns, projectionCol) < 0) {
                throw new IllegalArgumentException("Invalid column in projection: " + projectionCol);
            }
        }
    }

    public static Uri getSessionsUri() {
        return CONTENT_URI;
    }

    public static Uri getMeasurementsUri(long sessionId) {
        return Uri.withAppendedPath(ContentUris.withAppendedId(CONTENT_URI, sessionId), "measurements");
    }
}
