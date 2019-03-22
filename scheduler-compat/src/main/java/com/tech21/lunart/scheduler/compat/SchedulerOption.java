package com.tech21.lunart.scheduler.compat;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Pair;

import com.tech21.lunart.scheduler.compat.SchedulerCompat.RecurringType;

import java.util.Objects;

public class SchedulerOption {
    public static final String SCHEDULE_ID_KEY      = "SCHEDULE_ID_KEY";
    public static final String SCHEDULE_NAME_KEY    = "SCHEDULE_NAME_KEY";
    public static final String SCHEDULE_FOR_KEY     = "SCHEDULE_FOR_KEY";
    public static final String RECURRING_TYPE_KEY   = "RECURRING_TYPE_KEY";
    public static final String SCHEDULE_RECEIVER_ACTION_NAME_KEY = "SCHEDULE_RECEIVER_ACTION_NAME_KEY";


    private int scheduleId;
    private String scheduleName;
    private long scheduleFor;
    private int recurringType;
    private Pair<String, BroadcastReceiver> scheduleReceiver;

    private SchedulerOption(
            int scheduleId,
            String scheduleName,
            long scheduleFor,
            int recurringType,
            Pair<String, BroadcastReceiver> scheduleReceiver
    ) {
        this.scheduleId = scheduleId;
        this.scheduleName = scheduleName;
        this.scheduleFor = scheduleFor;
        this.recurringType = recurringType;
        this.scheduleReceiver = scheduleReceiver;
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

    public Pair<String, BroadcastReceiver> getScheduleReceiver() {
        return scheduleReceiver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchedulerOption that = (SchedulerOption) o;
        return scheduleId == that.scheduleId &&
                scheduleFor == that.scheduleFor &&
                recurringType == that.recurringType &&
                Objects.equals(scheduleName, that.scheduleName) &&
                Objects.equals(scheduleReceiver, that.scheduleReceiver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheduleId, scheduleName, scheduleFor, recurringType, scheduleReceiver);
    }

    @Override
    public String toString() {
        return "SchedulerOption{" +
                "scheduleId=" + scheduleId +
                ", scheduleName='" + scheduleName + '\'' +
                ", scheduleFor=" + scheduleFor +
                ", recurringType=" + recurringType +
                ", scheduleReceiver=" + scheduleReceiver +
                '}';
    }

    public static class Builder {
        private int scheduleId;
        private String scheduleName;
        private long scheduleFor;
        private int recurringType;
        private Pair<String, BroadcastReceiver> scheduleReceiver;

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

        public Builder scheduleReceiver(
                @NonNull String actionId,
                @NonNull BroadcastReceiver scheduleReceiver
        ) {
            this.scheduleReceiver = Pair.create(actionId, scheduleReceiver);
            return this;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public SchedulerOption fromBundle(@NonNull PersistableBundle bundle) {
            this.scheduleId = bundle.getInt(SCHEDULE_ID_KEY, -1);
            this.scheduleName = bundle.getString(SCHEDULE_NAME_KEY, null);
            this.scheduleFor = bundle.getLong(SCHEDULE_FOR_KEY, 0);
            this.recurringType = bundle.getInt(RECURRING_TYPE_KEY, -1);
            return build();
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public PersistableBundle toPersistableBundle(@NonNull SchedulerOption option) {
            PersistableBundle bundle = new PersistableBundle();
            bundle.putInt(SCHEDULE_ID_KEY, option.getScheduleId());
            bundle.putString(SCHEDULE_NAME_KEY, option.getScheduleName());
            bundle.putLong(SCHEDULE_FOR_KEY, option.getScheduleFor());
            bundle.putInt(RECURRING_TYPE_KEY, option.getRecurringType());
            bundle.putString(SCHEDULE_RECEIVER_ACTION_NAME_KEY, option.getScheduleReceiver().first);
            return bundle;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public Bundle toBundle(@NonNull PersistableBundle option) {
            Bundle bundle = new Bundle();
            bundle.putInt(SCHEDULE_ID_KEY, option.getInt(SCHEDULE_ID_KEY, -1));
            bundle.putString(SCHEDULE_NAME_KEY, option.getString(SCHEDULE_NAME_KEY));
            bundle.putLong(SCHEDULE_FOR_KEY, option.getLong(SCHEDULE_FOR_KEY, 0));
            bundle.putInt(RECURRING_TYPE_KEY, option.getInt(RECURRING_TYPE_KEY, -1));
            bundle.putString(SCHEDULE_RECEIVER_ACTION_NAME_KEY, option.getString(SCHEDULE_RECEIVER_ACTION_NAME_KEY));
            return bundle;
        }

        public Bundle toBundle(@NonNull SchedulerOption option) {
            Bundle bundle = new Bundle();
            bundle.putInt(SCHEDULE_ID_KEY, option.getScheduleId());
            bundle.putString(SCHEDULE_NAME_KEY, option.getScheduleName());
            bundle.putLong(SCHEDULE_FOR_KEY, option.getScheduleFor());
            bundle.putInt(RECURRING_TYPE_KEY, option.getRecurringType());
            bundle.putString(SCHEDULE_RECEIVER_ACTION_NAME_KEY, option.getScheduleReceiver().first);
            return bundle;
        }

        public SchedulerOption build() {
            validateOption();
            return new SchedulerOption(
                    scheduleId,
                    scheduleName,
                    scheduleFor,
                    recurringType,
                    scheduleReceiver
            );
        }

        private void validateOption() {
            if (scheduleFor <= 0) {
                throw new IllegalStateException("You must set schedule time.");
            }

            if (scheduleFor < System.currentTimeMillis()) {
                throw new IllegalStateException("You can't add schedule for the past time.");
            }

            if (TextUtils.isEmpty(scheduleReceiver.first)) {
                throw new IllegalStateException("Action ID must be unique, and shouldn't be empty");
            }
        }
    }
}
