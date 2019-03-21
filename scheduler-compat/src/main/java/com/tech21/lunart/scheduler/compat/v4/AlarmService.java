package com.tech21.lunart.scheduler.compat.v4;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.tech21.lunart.scheduler.compat.IScheduler;
import com.tech21.lunart.scheduler.compat.SchedulerCompat;
import com.tech21.lunart.scheduler.compat.SchedulerOptions;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import static com.tech21.lunart.scheduler.compat.SchedulerCompat.OCCUR_EVERY_DAYLIGHT;
import static com.tech21.lunart.scheduler.compat.SchedulerCompat.OCCUR_EVERY_MIDNIGHT;
import static com.tech21.lunart.scheduler.compat.SchedulerCompat.OCCUR_ONCE;
import static com.tech21.lunart.scheduler.compat.SchedulerCompat.OCCUR_ONCE_PRIORITY_URGENT;

public class AlarmService implements IScheduler<AlarmService>, IScheduler.Lifecycle {
    private static final String ACTION_SCHEDULE_ALARM_SERVICE_INTENT
            = "ACTION_SCHEDULE_ALARM_SERVICE_INTENT";

    private final SparseArray<SchedulerOptions> options = new SparseArray<>();
    private final AlarmReceiver alarmReceiver = new AlarmReceiver();
    private WeakReference<Context> context;
    private ReceiverState receiverState;
    private AlarmManager alarmManager;

    private static AlarmService sInstance;

    private AlarmService(Context context) {
        this.context = new WeakReference<>(context);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.receiverState = new ReceiverState(context);
        registerReceiver();
    }

    private void registerReceiver() {
        if (!receiverState.didSuccessfullyRegisterAlarmReceiver()) {
            context.get().registerReceiver(alarmReceiver, new IntentFilter(ACTION_SCHEDULE_ALARM_SERVICE_INTENT));
            receiverState.setDidSuccessfullyRegisterAlarmReceiver(true);
        }
    }

    private void unregisterReceiver() {
        if (receiverState.didSuccessfullyRegisterAlarmReceiver()) {
            context.get().unregisterReceiver(alarmReceiver);
            receiverState.setDidSuccessfullyRegisterAlarmReceiver(false);
        }
    }

    public static AlarmService with(@NonNull Context context) {
        if (sInstance == null || sInstance.context == null || sInstance.context.get() == null) {
            sInstance = null;
            sInstance = new AlarmService(context);
        }
        return sInstance;
    }

    @Override
    public void onStart() {
        registerReceiver();
    }

    @Override
    public AlarmService add(@NonNull SchedulerOptions options) {
        assert context != null && context.get() != null;

        this.options.append(options.getScheduleId(), options);
        switch (options.getRecurringType()) {
            case OCCUR_EVERY_MIDNIGHT:
                setRepeatingSchedule(options.getScheduleId(), 0, 0);
                break;
            case OCCUR_EVERY_DAYLIGHT:
                setRepeatingSchedule(options.getScheduleId(), 12, 0);
                break;
            case OCCUR_ONCE:
                setSchedule(options.getScheduleFor(), options.getScheduleId());
                break;
            case OCCUR_ONCE_PRIORITY_URGENT:
                setUrgentSchedule(options.getScheduleId());
                break;
        }

        return this;
    }

    @Override
    public void cancel(int scheduleId) {
        assert context != null && context.get() != null;
        Intent alarmIntent = new Intent(context.get(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context.get(),
                scheduleId,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(pendingIntent);
        options.remove(scheduleId);
    }

    @Override
    public void cancelAll() {
        assert context != null && context.get() != null;

        for (int i = 0; i < options.size(); i++) {
            int scheduleId = options.keyAt(i);
            cancel(scheduleId);
        }
    }

    @Override
    public void onStop() {
        unregisterReceiver();
    }

    private void setUrgentSchedule(int scheduleId) {
        final long fewMinutesFromNow = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        Intent exactIntent = new Intent(context.get(), AlarmReceiver.class);
        setSchedule(fewMinutesFromNow, scheduleId, exactIntent);
    }

    private void setRepeatingSchedule(int scheduleId, int hourOfDay, int minutes) {
        final long oneDayInMillis = TimeUnit.DAYS.toMillis(1);

        Intent alarmIntent = new Intent(context.get(), AlarmReceiver.class);
        setRepeatingSchedule(scheduleId, SchedulerCompat.scheduleFor(hourOfDay, minutes),
                oneDayInMillis, alarmIntent);
    }

    private void setRepeatingSchedule(
            int scheduleId,
            long triggerAtMillis,
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
                triggerAtMillis,
                intervalInMillis,
                pendingIntent
        );
    }

    private void setSchedule(long triggeredAtMillis, int scheduleId) {
        Intent exactIntent = new Intent(context.get(), AlarmReceiver.class);
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

    class ReceiverState {
        private Context context;
        private static final String DID_SUCCESSFULLY_REGISTER_ALARM_RECEIVER
                = "DID_SUCCESSFULLY_REGISTER_ALARM_RECEIVER";

        ReceiverState(@NonNull Context context) {
            this.context = context;
        }

        boolean didSuccessfullyRegisterAlarmReceiver() {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(DID_SUCCESSFULLY_REGISTER_ALARM_RECEIVER, false);
        }

        void setDidSuccessfullyRegisterAlarmReceiver(boolean status) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean(DID_SUCCESSFULLY_REGISTER_ALARM_RECEIVER, status)
                    .apply();
        }
    }
}