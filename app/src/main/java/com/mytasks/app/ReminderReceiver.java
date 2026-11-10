package com.mytasks.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Показывает уведомление, когда наступило время напоминания. */
public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL = "zamenki_reminders";

    private static int requestCode(long id) {
        return (int) (id % Integer.MAX_VALUE);
    }

    public static void schedule(Context c, Store.Reminder r) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        Intent i = new Intent(c, ReminderReceiver.class);
        i.putExtra("id", r.id);
        i.putExtra("text", r.text);
        PendingIntent pi = PendingIntent.getBroadcast(c, requestCode(r.id), i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.time, pi);
    }

    public static void cancel(Context c, long id) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        Intent i = new Intent(c, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, requestCode(id), i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra("id", 0L);
        String text = intent.getStringExtra("text");
        if (text == null) {
            text = "";
        }

        // Вовремя сработавшее напоминание автоматически отмечается как «ўтди».
        Store store = Store.get(context);
        for (Store.Reminder r : store.reminders) {
            if (r.id == id) {
                r.done = true;
            }
        }
        store.saveReminders();

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL, "Эслатмалар", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(ch);
            b = new Notification.Builder(context, CHANNEL);
        } else {
            b = new Notification.Builder(context);
        }

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        b.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Эслатма")
                .setContentText(text)
                .setContentIntent(content)
                .setAutoCancel(true);

        nm.notify(requestCode(id), b.build());
    }
}
