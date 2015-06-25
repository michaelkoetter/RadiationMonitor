package de.mkoetter.radmon.contentprovider;

import android.database.MatrixCursor;
import android.provider.MediaStore;

/**
 * Created by mk on 25.06.15.
 */
public class CSVFileInfoCursorWrapper extends MatrixCursor {
    public CSVFileInfoCursorWrapper(String[] projection) {
        super(projection, 1);

        RowBuilder row = newRow();
        for (String column : projection) {
            if (MediaStore.MediaColumns.DISPLAY_NAME.equals(column)) {
                row.add("radmon.csv");
            }
        }
    }
}
