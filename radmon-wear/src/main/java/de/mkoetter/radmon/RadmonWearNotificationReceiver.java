package de.mkoetter.radmon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;


public class RadmonWearNotificationReceiver extends BroadcastReceiver {

    private static final long NO_DATA = -1;

    public RadmonWearNotificationReceiver() {
    }
    @Override
    public void onReceive(Context context, Intent intent) {

        NotificationManager notificationManager = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
        long cpm = intent.getLongExtra(RadmonDataListenerService.DATA_KEY_CPM, NO_DATA);

        if (cpm != NO_DATA) {
            SpannableStringBuilder sb = new SpannableStringBuilder()
                    // FIXME use actual value
                    .append(context.getString(R.string.dose_rate_microsievert, 1.234f),
                            new TextAppearanceSpan(context, R.style.SummaryDoseRateText), 0)

                    .append("\n\n", new TextAppearanceSpan(context, R.style.SummarySpacing), 0)

                    .append(context.getString(R.string.cpm, cpm),
                            new TextAppearanceSpan(context, R.style.SummaryCPMText), 0);

            Notification notification = new Notification.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setStyle(new Notification.BigTextStyle().bigText(sb))
                    .setOngoing(true)
                    .build();
            notificationManager.notify(0, notification);
        } else {
            notificationManager.cancel(0);
        }

    }
}