package com.tech21.lunart.scheduler.compat;

import android.content.Context;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.tech21.lunart.scheduler.compat.v21.SchedulerService;
import com.tech21.lunart.scheduler.compat.v4.AlarmService;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Calendar;

public class SchedulerCompat {
    @IntDef({OCCUR_EVERY_MIDNIGHT, OCCUR_EVERY_DAYLIGHT, OCCUR_ONCE, OCCUR_ONCE_PRIORITY_URGENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RecurringType { }

    public static final int OCCUR_EVERY_MIDNIGHT = 0;
    public static final int OCCUR_EVERY_DAYLIGHT = 1;
    public static final int OCCUR_ONCE = 2;
    public static final int OCCUR_ONCE_PRIORITY_URGENT = 3;

    private static SchedulerCompat sInstance;
    private WeakReference<Context> context;

    private SchedulerCompat(@NonNull Context context) {
        this.context = new WeakReference<>(context.getApplicationContext());
    }

    public static SchedulerCompat with(@NonNull Context context) {
        if (sInstance == null || sInstance.context == null || sInstance.context.get() == null) {
            sInstance = null;
            sInstance = new SchedulerCompat(context);
        }
        return sInstance;
    }

    public SchedulerCompat addSchedule(@NonNull SchedulerOptions options) {
        assert context != null && context.get() != null;

        Context context = this.context.get();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SchedulerService.with(context).add(options);
        } else {
            AlarmService.with(context).add(options);
        }
        return this;
    }

    public void cancelSchedule(int scheduleId) {
        assert context != null && context.get() != null;

        Context context = this.context.get();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SchedulerService.with(context).cancel(scheduleId);
            return;
        }
        AlarmService.with(context).cancel(scheduleId);
    }

    public static long scheduleFor(int hourOfDay, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
