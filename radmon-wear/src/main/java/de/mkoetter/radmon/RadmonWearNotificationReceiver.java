package de.mkoetter.radmon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;


public class RadmonWearNotificationReceiver extends BroadcastReceiver {

    private static final long NO_DATA = -1;

    private Bitmap background = null;

    public RadmonWearNotificationReceiver() {
    }

    private synchronized Bitmap getBackground(Context context) {
        if (background == null) {
            background = BitmapFactory.decodeResource(context.getResources(), R.drawable.notify_backround);
        }
        return background;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        NotificationManager notificationManager = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
        long cpm = intent.getLongExtra(RadmonDataListenerService.DATA_KEY_CPM, NO_DATA);
        double doseRate = intent.getDoubleExtra(RadmonDataListenerService.DATA_KEY_DOSE_RATE, Double.NaN);

        if (cpm != NO_DATA) {
            SpannableStringBuilder sb = new SpannableStringBuilder()
                    // FIXME use actual value
                    .append(context.getString(R.string.dose_rate_microsievert, doseRate),
                            new TextAppearanceSpan(context, R.style.SummaryDoseRateText), 0)

                    .append("\n\n", new TextAppearanceSpan(context, R.style.SummarySpacing), 0)

                    .append(context.getString(R.string.cpm, cpm),
                            new TextAppearanceSpan(context, R.style.SummaryCPMText), 0);


            Notification.WearableExtender wearableExtender = new Notification.WearableExtender()
                    .setBackground(getBackground(context));

            Notification notification = new Notification.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setStyle(new Notification.BigTextStyle().bigText(sb))
                    .setOngoing(true)
                    .extend(wearableExtender)
                    .build();

            notificationManager.notify(0, notification);
        } else {
            notificationManager.cancel(0);
        }

    }
}