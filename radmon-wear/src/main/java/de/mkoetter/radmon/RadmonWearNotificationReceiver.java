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
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;


public class RadmonWearNotificationReceiver extends BroadcastReceiver {

    private static final long NO_DATA = -1;

    private synchronized Bitmap getBackground(Context context) {
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.notify_backround);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        long cpm = intent.getLongExtra(RadmonDataListenerService.DATA_KEY_CPM, NO_DATA);

        if (cpm != NO_DATA) {
            long[] history = intent.getLongArrayExtra(RadmonDataListenerService.DATA_KEY_HISTORY);
            double doseRate = intent.getDoubleExtra(RadmonDataListenerService.DATA_KEY_DOSE_RATE, Double.NaN);
            boolean reducedUpdateRate = intent.getBooleanExtra(RadmonDataListenerService.DATA_KEY_REDUCED_UPDATE_RATE, false);

            SpannableStringBuilder sb = new SpannableStringBuilder()
                    // FIXME use actual value
                    .append(context.getString(R.string.dose_rate_microsievert, doseRate),
                            new TextAppearanceSpan(context, R.style.SummaryDoseRateText), 0)

                    .append("\n\n", new TextAppearanceSpan(context, R.style.SummarySpacing), 0)

                    .append(context.getString(R.string.cpm, cpm),
                            new TextAppearanceSpan(context, R.style.SummaryCPMText), 0);

            if (reducedUpdateRate) {
                sb.append("\n").append(context.getString(R.string.reduced_update_rate),
                        new TextAppearanceSpan(context, R.style.SummaryNoteText), 0);
            }


            Notification.WearableExtender wearableExtender = new Notification.WearableExtender()
                    .setBackground(getBackground(context));

            if (history != null) {
                Intent notificationGraphIntent = new Intent(context, NotificationGraphActivity.class);
                notificationGraphIntent.putExtra(NotificationGraphActivity.EXTRA_DATA_POINTS, history);
                notificationGraphIntent.putExtra(NotificationGraphActivity.EXTRA_TITLE,
                        context.getString(R.string.history));


                PendingIntent notificationGraphPendingIntent = PendingIntent.getActivity(
                        context, 0, notificationGraphIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                Notification notificationGraph = new Notification.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .extend(new Notification.WearableExtender()
                                .setHintHideIcon(true)
                                .setDisplayIntent(notificationGraphPendingIntent)
                                .setCustomSizePreset(Notification.WearableExtender.SIZE_LARGE)
                                .setCustomContentHeight(context.getResources().getDimensionPixelSize(R.dimen.GraphNotificationHeight)))
                        .build();

                wearableExtender.addPage(notificationGraph);
            }

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