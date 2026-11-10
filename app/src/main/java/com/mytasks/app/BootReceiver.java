package com.mytasks.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** После перезагрузки телефона заново ставит будильники для будущих напоминаний. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        Store store = Store.get(context);
        long now = System.currentTimeMillis();
        for (Store.Reminder r : store.reminders) {
            if (!r.done && r.time > now) {
                ReminderReceiver.schedule(context, r);
            }
        }
    }
}
