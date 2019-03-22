package com.tech21.lunart.scheduler.compat.v4;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.tech21.lunart.scheduler.compat.IScheduler;
import com.tech21.lunart.scheduler.compat.SchedulerCompat;
import com.tech21.lunart.scheduler.compat.SchedulerOption;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import static com.tech21.lunart.scheduler.compat.SchedulerCompat.OCCUR_EVERY_DAYLIGHT;
import static com.tech21.lunart.scheduler.compat.SchedulerCompat.OCCUR_EVERY_MIDNIGHT;
import static com.tech21.lunart.scheduler.compat.SchedulerCompat.OCCUR_EVERY_SPECIFIC_TIME;
import static com.tech21.lunart.scheduler.compat.SchedulerCompat.OCCUR_ONCE;
import static com.tech21.lunart.scheduler.compat.SchedulerCompat.OCCUR_ONCE_IMMEDIATELY;

public class AlarmService implements IScheduler<AlarmService> {
    public static final String ACTION_SCHEDULE_ALARM_SERVICE_INTENT
            = "ACTION_SCHEDULE_ALARM_SERVICE_INTENT";

    private final SparseArray<SchedulerOption> optHistory = new SparseArray<>();
    private WeakReference<Context> context;
    private AlarmManager alarmManager;

    private static AlarmService sInstance;

    private AlarmService(Context context) {
        this.context = new WeakReference<>(context);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public static AlarmService with(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new AlarmService(context);
        }
        if (sInstance.context == null || sInstance.context.get() == null) {
            sInstance.context = new WeakReference<>(context);
        }
        return sInstance;
    }

    @Override
    public AlarmService add(@NonNull SchedulerOption option) {
        assert context != null && context.get() != null;
        optHistory.append(option.getScheduleId(), option);

        switch (option.getRecurringType()) {
            case OCCUR_EVERY_MIDNIGHT:
                setRepeatingSchedule(option.getScheduleId(), 0, 0);
                break;
            case OCCUR_EVERY_DAYLIGHT:
                setRepeatingSchedule(option.getScheduleId(), 12, 0);
                break;
            case OCCUR_ONCE:
                setSchedule(option.getScheduleFor(), option.getScheduleId());
                break;
            case OCCUR_ONCE_IMMEDIATELY:
                setUrgentSchedule(option.getScheduleId());
                break;
            case OCCUR_EVERY_SPECIFIC_TIME:
                setRepeatingSchedule(option.getScheduleId(), option.getScheduleFor());
                break;
        }

        return this;
    }

    @Override
    public void cancel(int scheduleId) {
        assert context != null && context.get() != null;
        Intent alarmIntent = constructIntent(scheduleId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context.get(),
                scheduleId,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(pendingIntent);
        optHistory.remove(scheduleId);
    }

    @Override
    public void cancelAll() {
        assert context != null && context.get() != null;

        for (int i = 0; i < optHistory.size(); i++) {
            int scheduleId = optHistory.keyAt(i);
            cancel(scheduleId);
        }
    }

    private void setUrgentSchedule(int scheduleId) {
        final long fewMinutesFromNow = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        Intent exactIntent = constructIntent(scheduleId);
        setSchedule(fewMinutesFromNow, scheduleId, exactIntent);
    }

    private void setRepeatingSchedule(int scheduleId, int hourOfDay, int minutes) {
        final long oneDayInMillis = TimeUnit.DAYS.toMillis(1);
        Intent alarmIntent = constructIntent(scheduleId);
        setRepeatingSchedule(scheduleId, SchedulerCompat.scheduleFor(hourOfDay, minutes),
                oneDayInMillis, alarmIntent);
    }

    private void setRepeatingSchedule(int scheduleId, long triggeredAtMillis) {
        final long oneDayInMillis = TimeUnit.DAYS.toMillis(1);
        Intent alarmIntent = constructIntent(scheduleId);
        setRepeatingSchedule(scheduleId, triggeredAtMillis,
                oneDayInMillis, alarmIntent);
    }

    private void setRepeatingSchedule(
            int scheduleId,
            long triggeredAtMillis,
            long intervalInMillis,
            @NonNull Intent repeatingHandlerIntent
    ) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context.get(),
                scheduleId,
                repeatingHandlerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggeredAtMillis,
                intervalInMillis,
                pendingIntent
        );
    }

    private void setSchedule(long triggeredAtMillis, int scheduleId) {
        Intent exactIntent = constructIntent(scheduleId);
        setSchedule(triggeredAtMillis, scheduleId, exactIntent);
    }

    private void setSchedule(long triggeredAtMillis, int alarmId, @NonNull Intent exactHandlerIntent) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context.get(),
                alarmId,
                exactHandlerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggeredAtMillis,
                    pendingIntent
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggeredAtMillis,
                    pendingIntent
            );
        }
    }

    private Intent constructIntent(int scheduleId) {
        Intent intent = new Intent(context.get(), AlarmReceiver.class);
        SchedulerOption option = optHistory.get(scheduleId);
        intent.putExtras(new SchedulerOption.Builder().toBundle(option));
        return intent;
    }
}