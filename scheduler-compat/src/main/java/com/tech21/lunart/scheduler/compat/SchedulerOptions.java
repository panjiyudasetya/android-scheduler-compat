package com.tech21.lunart.scheduler.compat;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;

import com.tech21.lunart.scheduler.compat.SchedulerCompat.RecurringType;

public class SchedulerOptions {
    public static final String SCHEDULE_ID_KEY      = "SCHEDULE_ID_KEY";
    public static final String SCHEDULE_NAME_KEY    = "SCHEDULE_NAME_KEY";
    public static final String SCHEDULE_FOR_KEY     = "SCHEDULE_FOR_KEY";
    public static final String RECURRING_TYPE_KEY   = "RECURRING_TYPE_KEY";


    private int scheduleId;
    private String scheduleName;
    private long scheduleFor;
    private int recurringType;

    private SchedulerOptions(
            int scheduleId,
            String scheduleName,
            long scheduleFor,
            int recurringType
    ) {
        this.scheduleId = scheduleId;
        this.scheduleName = scheduleName;
        this.scheduleFor = scheduleFor;
        this.recurringType = recurringType;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public String getScheduleName() {
        return scheduleName;
    }

    public long getScheduleFor() {
        return scheduleFor;
    }

    public @RecurringType int getRecurringType() {
        return recurringType;
    }

    public static class Builder {
        private int scheduleId;
        private String scheduleName;
        private long scheduleFor;
        private int recurringType;

        public Builder scheduleId(int scheduleId) {
            this.scheduleId = scheduleId;
            return this;
        }

        public Builder scheduleName(String scheduleName) {
            this.scheduleName = scheduleName;
            return this;
        }

        public Builder scheduleFor(long timestamp) {
            this.scheduleFor = timestamp;
            return this;
        }

        public Builder scheduleFor(int hourOfDay, int minutes) {
            this.scheduleFor = SchedulerCompat.scheduleFor(hourOfDay, minutes);
            return this;
        }

        public Builder recurringType(@RecurringType int recurringType) {
            this.recurringType = recurringType;
            return this;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public SchedulerOptions fromBundle(@NonNull PersistableBundle bundle) {
            this.scheduleId = bundle.getInt(SCHEDULE_ID_KEY, -1);
            this.scheduleName = bundle.getString(SCHEDULE_NAME_KEY, null);
            this.scheduleFor = bundle.getLong(SCHEDULE_FOR_KEY, 0);
            this.recurringType = bundle.getInt(RECURRING_TYPE_KEY, -1);
            return build();
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public PersistableBundle toBundle(@NonNull SchedulerOptions options) {
            PersistableBundle bundle = new PersistableBundle();
            bundle.putInt(SCHEDULE_ID_KEY, options.getScheduleId());
            bundle.putString(SCHEDULE_NAME_KEY, options.getScheduleName());
            bundle.putLong(SCHEDULE_FOR_KEY, options.getScheduleFor());
            bundle.putInt(RECURRING_TYPE_KEY, options.getRecurringType());
            return bundle;
        }

        public SchedulerOptions build() {
            assert scheduleFor > 0;
            return new SchedulerOptions(
                    scheduleId,
                    scheduleName,
                    scheduleFor,
                    recurringType
            );
        }
    }
}
