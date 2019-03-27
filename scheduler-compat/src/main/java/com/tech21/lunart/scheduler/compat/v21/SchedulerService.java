package com.tech21.lunart.scheduler.compat.v21;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.SparseArray;

import com.tech21.lunart.scheduler.compat.IScheduler;
import com.tech21.lunart.scheduler.compat.SchedulerCompat;
import com.tech21.lunart.scheduler.compat.SchedulerCompat.RecurringType;
import com.tech21.lunart.scheduler.compat.SchedulerOption;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static android.os.Build.VERSION.SDK_INT;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SchedulerService extends JobService implements IScheduler<SchedulerService> {
    private static final String TAG = SchedulerService.class.getSimpleName();

    private WeakReference<Context> context;
    private static final SparseArray<SchedulerOption> optHistory = new SparseArray<>();
    private JobScheduler jobScheduler;
    private static SchedulerService sInstance;

    public SchedulerService() {
        // Default constructor, used by Android system service
    }

    private SchedulerService(@NonNull Context context) {
        this();
        this.context = new WeakReference<>(context);
        this.jobScheduler = getAndroidJobScheduler();
    }

    public static SchedulerService with(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new SchedulerService(context);
        }
        if (sInstance.context == null || sInstance.context.get() == null) {
            sInstance.context = new WeakReference<>(context);
        }
        return sInstance;
    }

    private JobScheduler getAndroidJobScheduler() {
        assert context != null && context.get() != null;

        if (SDK_INT >= Build.VERSION_CODES.M) {
            jobScheduler = context.get().getSystemService(JobScheduler.class);
        } else {
            jobScheduler = (JobScheduler) context.get().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        }

        return jobScheduler;
    }

    private JobInfo getJobInfo(@NonNull ComponentName componentName,
                               @NonNull SchedulerOption option) {
        PersistableBundle extra = new SchedulerOption.Builder().toPersistableBundle(option);
        return new JobInfo.Builder(option.getScheduleId(), componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresDeviceIdle(false)
                .setMinimumLatency(getMinimumLatency(
                        option.getRecurringType(),
                        option.getScheduleFor()))
                .setExtras(extra)
                .build();
    }

    @Override
    public SchedulerService add(@NonNull SchedulerOption option) {
        assert context != null && context.get() != null;

        ComponentName componentName = new ComponentName(context.get(), SchedulerService.class);
        JobInfo jobInfo = getJobInfo(componentName, option);
        jobScheduler = getAndroidJobScheduler();
        if (jobScheduler.schedule(jobInfo) == JobScheduler.RESULT_SUCCESS) {
            optHistory.append(option.getScheduleId(), option);
        }
        return this;
    }

    @Override
    public void cancel(int scheduleId) {
        assert context != null && context.get() != null;

        jobScheduler = getAndroidJobScheduler();
        jobScheduler.cancel(scheduleId);
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

    @Override
    public boolean onStartJob(JobParameters jobInfo) {
        PersistableBundle extra = jobInfo.getExtras();

        sendBroadcast(extra);
        jobFinished(jobInfo, false);

        if (isDailySchedule(extra.getInt(SchedulerOption.RECURRING_TYPE_KEY))) {
            rescheduleForNext(new SchedulerOption.Builder().fromBundle(extra),
                    TimeUnit.DAYS.toMillis(1));
        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }

    private void sendBroadcast(PersistableBundle extra) {
        String actionName = extra.getString(SchedulerOption.SCHEDULE_RECEIVER_ACTION_NAME_KEY, "");
        Intent intent = new Intent();
        intent.setAction(actionName);
        intent.putExtras(new SchedulerOption.Builder().toBundle(extra));
        sendBroadcast(intent);
    }

    private void rescheduleForNext(@NonNull SchedulerOption options, long intervalInMillis) {
        with(this).add(new SchedulerOption.Builder()
                .scheduleId(options.getScheduleId())
                .scheduleName(options.getScheduleName())
                .recurringType(options.getRecurringType())
                .scheduleFor(options.getScheduleFor() + intervalInMillis)
                .build());
    }

    private long getMinimumLatency(@RecurringType int type, long triggeredAtMillis) {
        if (triggeredAtMillis > System.currentTimeMillis()) {
            return triggeredAtMillis;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(triggeredAtMillis);
        if (isDailySchedule(type)) {
            cal.add(Calendar.DATE, 1);
            return cal.getTimeInMillis();
        }
        return triggeredAtMillis;
    }

    private boolean isDailySchedule(@RecurringType int type) {
        return type == SchedulerCompat.OCCUR_EVERY_MIDNIGHT
                || type == SchedulerCompat.OCCUR_EVERY_DAYLIGHT
                || type == SchedulerCompat.OCCUR_EVERY_SPECIFIC_TIME;
    }
}
