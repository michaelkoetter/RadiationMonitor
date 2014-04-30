package de.mkoetter.radmon.util;

import android.database.Cursor;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by mk on 30.04.14.
 */
public class CSVUtil {

    public static final String DELIMITER = ",";

    public interface Formatter {
        public String format(Cursor data, int column);
    }

    public static void writeHeader(Cursor data, OutputStream out) throws IOException {
        String[] columns = data.getColumnNames();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) out.write(DELIMITER.getBytes());
            out.write(columns[i].getBytes());
        }

        out.write("\n".getBytes());
    }

    public static void writeData(Cursor data, Formatter formatter, OutputStream out) throws IOException {

        for (int i = 0; i < data.getColumnCount(); i++) {
            String _data = null;

            if (formatter != null) {
                _data = formatter.format(data, i);
            } else {
                _data = data.getString(i);
            }

            if (i > 0) out.write(DELIMITER.getBytes());
            if (_data != null) out.write(_data.getBytes());
        }

        out.write("\n".getBytes());
    }
}
