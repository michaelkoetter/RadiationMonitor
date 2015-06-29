package de.mkoetter.radmon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;


public class RadmonWearNotificationReceiver extends BroadcastReceiver {

    private static final long NO_DATA = -1;
    private static final int ID_NOTIFICATION = 20;

    private Bitmap cachedBackground = null;

    private static final int HISTORY_SIZE = 30;
    private static long[] history = new long[HISTORY_SIZE];

    private Notification.Builder notificationBuilder = null;

    public RadmonWearNotificationReceiver(Context context) {
        notificationBuilder = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true);
    }

    private synchronized Bitmap getBackground(Context context) {
        if (cachedBackground == null) {
            cachedBackground = BitmapFactory.decodeResource(context.getResources(), R.drawable.notify_backround);
        }

        return cachedBackground;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        long cpm = intent.getLongExtra(RadmonDataListenerService.DATA_KEY_CPM, NO_DATA);

        if (cpm != NO_DATA) {
            // update history
            System.arraycopy(history, 1, history, 0, HISTORY_SIZE - 1);
            history[HISTORY_SIZE-1] = cpm;

            double doseRate = intent.getDoubleExtra(RadmonDataListenerService.DATA_KEY_DOSE_RATE, Double.NaN);

            SpannableStringBuilder sb = new SpannableStringBuilder()
                    .append(context.getString(R.string.dose_rate_microsievert, doseRate),
                            new TextAppearanceSpan(context, R.style.SummaryDoseRateText), 0)

                    .append("\n\n", new TextAppearanceSpan(context, R.style.SummarySpacing), 0)

                    .append(context.getString(R.string.cpm, cpm),
                            new TextAppearanceSpan(context, R.style.SummaryCPMText), 0);


            Intent notificationGraphIntent = new Intent(context, NotificationGraphActivity.class);
            notificationGraphIntent.putExtra(NotificationGraphActivity.EXTRA_DATA_POINTS, history);
            notificationGraphIntent.putExtra(NotificationGraphActivity.EXTRA_TITLE,
                    context.getString(R.string.history, HISTORY_SIZE));

            PendingIntent notificationGraphPendingIntent = PendingIntent.getActivity(
                    context, 0, notificationGraphIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification.Builder historyNotificationBuilder = new Notification.Builder(context)
                    .extend(new Notification.WearableExtender()
                    .setHintHideIcon(true)
                    .setDisplayIntent(notificationGraphPendingIntent)
                    .setCustomSizePreset(Notification.WearableExtender.SIZE_LARGE)
                    .setCustomContentHeight(context.getResources().getDimensionPixelSize(R.dimen.GraphNotificationHeight)));

            Notification.WearableExtender wearableExtender = new Notification.WearableExtender()
                    .setBackground(getBackground(context))
                    .addPage(historyNotificationBuilder.build());

            Notification notification = notificationBuilder
                    .extend(wearableExtender)
                    .setStyle(new Notification.BigTextStyle().bigText(sb)).build();

            notificationManager.notify(ID_NOTIFICATION, notification);
        } else {
            notificationManager.cancel(ID_NOTIFICATION);
        }

    }
}
