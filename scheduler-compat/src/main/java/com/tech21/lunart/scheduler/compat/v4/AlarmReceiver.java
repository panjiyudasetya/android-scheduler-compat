package com.tech21.lunart.scheduler.compat.v4;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.tech21.lunart.scheduler.compat.SchedulerOption;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extra = intent.getExtras();
        if (extra != null) {
            String actionName = extra.getString(SchedulerOption.SCHEDULE_RECEIVER_ACTION_NAME_KEY, "");

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(actionName);
            broadcastIntent.putExtras(extra);
            context.sendBroadcast(broadcastIntent);
        }
    }
}
