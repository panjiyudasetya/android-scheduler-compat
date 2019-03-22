package com.tech21.lunart.scheduler.compat;

import android.support.annotation.NonNull;

public interface IScheduler<T> {
    T add(@NonNull SchedulerOption options);
    void cancel(int scheduleId);
    void cancelAll();

    interface Lifecycle {
        void onStart();
        void onStop();
    }
}
