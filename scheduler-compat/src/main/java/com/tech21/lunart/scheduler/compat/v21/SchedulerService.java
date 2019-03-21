package com.tech21.lunart.scheduler.compat.v21;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.SparseArray;

import com.tech21.lunart.scheduler.compat.IScheduler;
import com.tech21.lunart.scheduler.compat.SchedulerCompat;
import com.tech21.lunart.scheduler.compat.SchedulerCompat.RecurringType;
import com.tech21.lunart.scheduler.compat.SchedulerOptions;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import static android.os.Build.VERSION.SDK_INT;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SchedulerService extends JobService implements IScheduler<SchedulerService> {
    private static final String TAG = SchedulerService.class.getSimpleName();

    private WeakReference<Context> context;
    private final SparseArray<SchedulerOptions> options = new SparseArray<>();
    private JobScheduler jobScheduler;
    private static SchedulerService sInstance;

    private SchedulerService(@NonNull Context context) {
        this.context = new WeakReference<>(context);
        this.jobScheduler = getAndroidJobScheduler();
    }

    public static SchedulerService with(@NonNull Context context) {
        if (sInstance == null || sInstance.context == null || sInstance.context.get() == null) {
            sInstance = null;
            sInstance = new SchedulerService(context);
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
                               @NonNull SchedulerOptions options) {
        PersistableBundle extra = new SchedulerOptions.Builder().toBundle(options);
        return new JobInfo.Builder(options.getScheduleId(), componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresDeviceIdle(false)
                .setMinimumLatency(getMinimumLatency(
                        options.getScheduleFor(),
                        System.currentTimeMillis()))
                .setExtras(extra)
                .build();
    }

    @Override
    public SchedulerService add(@NonNull SchedulerOptions options) {
        assert context != null && context.get() != null;

        ComponentName componentName = new ComponentName(context.get(), SchedulerService.class);
        JobInfo jobInfo = getJobInfo(componentName, options);
        jobScheduler = getAndroidJobScheduler();
        if (jobScheduler.schedule(jobInfo) == JobScheduler.RESULT_SUCCESS) {
            this.options.append(options.getScheduleId(), options);
        }
        return this;
    }

    @Override
    public void cancel(int scheduleId) {
        assert context != null && context.get() != null;

        jobScheduler = getAndroidJobScheduler();
        jobScheduler.cancel(scheduleId);
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
    public boolean onStartJob(JobParameters jobInfo) {
        // TODO : Do something
        // ...

        // Reschedule the job when it's required
        PersistableBundle extra = jobInfo.getExtras();
        if (isRecurringSchedule(extra.getInt(SchedulerOptions.RECURRING_TYPE_KEY))) {
            jobFinished(jobInfo, false);
            rescheduleForNext(new SchedulerOptions.Builder().fromBundle(extra));
        }

        jobFinished(jobInfo, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }

    private void rescheduleForNext(@NonNull SchedulerOptions options) {
        with(this).add(new SchedulerOptions.Builder()
                .scheduleId(options.getScheduleId())
                .scheduleName(options.getScheduleName())
                .recurringType(options.getRecurringType())
                .scheduleFor(options.getScheduleFor() + TimeUnit.DAYS.toMillis(1))
                .build());
    }

    private long getMinimumLatency(long triggeredAtMillis, long currentTime) {
        return Math.abs(triggeredAtMillis - currentTime);
    }

    private boolean isRecurringSchedule(@RecurringType int type) {
        return type == SchedulerCompat.OCCUR_EVERY_MIDNIGHT
                || type == SchedulerCompat.OCCUR_EVERY_DAYLIGHT;
    }
}
