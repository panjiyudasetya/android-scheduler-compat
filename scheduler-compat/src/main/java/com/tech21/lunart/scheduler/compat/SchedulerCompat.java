package com.tech21.lunart.scheduler.compat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.util.SparseArray;

import com.tech21.lunart.scheduler.compat.v21.SchedulerService;
import com.tech21.lunart.scheduler.compat.v4.AlarmReceiver;
import com.tech21.lunart.scheduler.compat.v4.AlarmService;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Calendar;

public class SchedulerCompat implements IScheduler<SchedulerCompat>, IScheduler.Lifecycle {
    @IntDef({
        OCCUR_EVERY_MIDNIGHT,
        OCCUR_EVERY_DAYLIGHT,
        OCCUR_ONCE,
        OCCUR_ONCE_IMMEDIATELY,
        OCCUR_EVERY_SPECIFIC_TIME
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RecurringType { }

    public static final int OCCUR_EVERY_MIDNIGHT = 0;
    public static final int OCCUR_EVERY_DAYLIGHT = 1;
    public static final int OCCUR_ONCE = 2;
    public static final int OCCUR_ONCE_IMMEDIATELY = 3;
    public static final int OCCUR_EVERY_SPECIFIC_TIME = 4;

    private final SparseArray<Pair<String, BroadcastReceiver>> scheduleReceivers = new SparseArray<>();
    private WeakReference<Context> context;
    private ReceiverState receiverState;
    private static SchedulerCompat sInstance;

    private SchedulerCompat(@NonNull Context context) {
        this.context = new WeakReference<>(context.getApplicationContext());
        this.receiverState = new ReceiverState(context);
        registerReceiverPreOreo();
    }

    public static SchedulerCompat with(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new SchedulerCompat(context);
        }
        if (sInstance.context == null || sInstance.context.get() == null) {
            sInstance.context = new WeakReference<>(context);
        }
        return sInstance;
    }

    @Override
    public void onStart() {
        registerReceiverPreOreo();
        registerReceivers();
    }

    @Override
    public SchedulerCompat add(@NonNull SchedulerOption option) {
        assert context != null && context.get() != null;

        registerReceiver(option.getScheduleId(), option.getScheduleReceiver());

        Context context = this.context.get();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SchedulerService.with(context).add(option);
        } else {
            AlarmService.with(context).add(option);
        }

        return this;
    }

    @Override
    public void cancel(int scheduleId) {
        assert context != null && context.get() != null;

        Context context = this.context.get();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SchedulerService.with(context).cancel(scheduleId);
            return;
        }
        AlarmService.with(context).cancel(scheduleId);
        unregisterReceiver(scheduleId);
    }

    @Override
    public void cancelAll() {
        assert context != null && context.get() != null;

        Context context = this.context.get();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SchedulerService.with(context).cancelAll();
            return;
        }
        AlarmService.with(context).cancelAll();
    }

    @Override
    public void onStop() {
        unregisterReceiverPreOreo();
        unregisterReceivers();
    }

    private void registerReceiver(
            int scheduleId,
            @NonNull Pair<String, BroadcastReceiver> scheduleReceiver
    ) {
        if (!receiverState.didReceiverRegistered(scheduleReceiver.first)) {
            context.get().registerReceiver(scheduleReceiver.second, new IntentFilter(scheduleReceiver.first));
            receiverState.setDidReceiverRegistered(scheduleReceiver.first, true);
            scheduleReceivers.append(scheduleId, scheduleReceiver);
        }
    }

    private void registerReceiverPreOreo() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (!receiverState.didReceiverRegistered(AlarmService.ACTION_SCHEDULE_ALARM_SERVICE_INTENT)) {
                context.get().registerReceiver(new AlarmReceiver(),
                        new IntentFilter(AlarmService.ACTION_SCHEDULE_ALARM_SERVICE_INTENT));
                receiverState.setDidReceiverRegistered(AlarmService.ACTION_SCHEDULE_ALARM_SERVICE_INTENT, true);
            }
        }
    }

    private void registerReceivers() {
        for (int i = 0; i < scheduleReceivers.size(); i++) {
            Pair<String, BroadcastReceiver> receiverPair = scheduleReceivers.valueAt(i);
            registerReceiver(scheduleReceivers.keyAt(i), receiverPair);
        }
    }

    private void unregisterReceiver(int scheduleId) {
        Pair<String, BroadcastReceiver> scheduleReceiver = scheduleReceivers.get(scheduleId);
        if (receiverState.didReceiverRegistered(scheduleReceiver.first)) {
            context.get().unregisterReceiver(scheduleReceiver.second);
            receiverState.setDidReceiverRegistered(scheduleReceiver.first, false);
            scheduleReceivers.remove(scheduleId);
        }
    }

    private void unregisterReceiverPreOreo() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (receiverState.didReceiverRegistered(AlarmService.ACTION_SCHEDULE_ALARM_SERVICE_INTENT)) {
                context.get().unregisterReceiver(new AlarmReceiver());
                receiverState.setDidReceiverRegistered(AlarmService.ACTION_SCHEDULE_ALARM_SERVICE_INTENT, false);
            }
        }
    }

    private void unregisterReceivers() {
        for (int i = 0; i < scheduleReceivers.size(); i++) {
            unregisterReceiver(scheduleReceivers.keyAt(i));
        }
    }

    public static long scheduleFor(int hourOfDay, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    class ReceiverState {
        private Context context;

        ReceiverState(@NonNull Context context) {
            this.context = context;
        }

        boolean didReceiverRegistered(String actionName) {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(actionName, false);
        }

        void setDidReceiverRegistered(String actionName, boolean status) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean(actionName, status)
                    .apply();
        }
    }
}
