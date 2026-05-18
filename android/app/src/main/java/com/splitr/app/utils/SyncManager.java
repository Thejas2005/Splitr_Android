package com.splitr.app.utils;

import android.content.Context;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.splitr.app.workers.SyncWorker;
import java.util.concurrent.TimeUnit;

public class SyncManager {

    private static final String SYNC_WORK_NAME = "splitr_sync";

    public static void scheduleSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
        );
    }

    public static void syncNow(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
        );
    }
}