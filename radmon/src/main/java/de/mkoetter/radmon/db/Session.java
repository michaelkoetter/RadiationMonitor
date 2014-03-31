package de.mkoetter.radmon.db;

import android.database.Cursor;

import java.util.Date;

/**
 * Created by Michael on 31.03.14.
 */
public class Session {

    private long id;
    private Date startTime;
    private Date endTime;
    private String device;
    private Double conversionFactor;

    public Double getConversionFactor() {
        return conversionFactor;
    }

    public void setConversionFactor(Double conversionFactor) {
        this.conversionFactor = conversionFactor;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public static Session fromCursor(Cursor cursor) {
        Session session = new Session();
        session.id = cursor.getInt(0);
        session.startTime = new Date(cursor.getLong(1));
        session.endTime = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        session.device = cursor.getString(3);
        session.conversionFactor = cursor.isNull(4) ? null : cursor.getDouble(4);

        return session;
    }
}
