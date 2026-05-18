package com.splitr.app.workers;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.splitr.app.api.RetrofitClient;
import com.splitr.app.cache.PendingExpense;
import com.splitr.app.cache.SplitRDatabase;
import com.splitr.app.models.ExpenseCreate;
import com.splitr.app.models.FindOrCreateGroupRequest;
import com.splitr.app.models.FindOrCreateGroupResponse;
import com.splitr.app.models.GenericResponse;
import com.splitr.app.models.GroupExpenseCreate;
import com.splitr.app.models.GroupExpenseResponse;
import com.splitr.app.models.MemberSplit;
import com.splitr.app.models.MemberSpec;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import retrofit2.Response;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private final Gson gson = new Gson();

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SplitRDatabase db = SplitRDatabase.get(getApplicationContext());
        List<PendingExpense> pending = db.pendingExpenseDao().getAllPending();

        if (pending.isEmpty()) return Result.success();

        Log.d(TAG, "Syncing " + pending.size() + " expense(s)");
        boolean allOk = true;

        for (PendingExpense p : pending) {
            try {
                if (p.isGroupExpense()) {
                    syncGroup(p, db);
                } else {
                    syncPersonal(p, db);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed: " + e.getMessage());
                p.status = "failed";
                p.retryCount++;
                p.errorMessage = e.getMessage();
                db.pendingExpenseDao().update(p);
                if (p.retryCount >= 5) db.pendingExpenseDao().deleteById(p.localId);
                allOk = false;
            }
        }
        return allOk ? Result.success() : Result.retry();
    }

    private void syncPersonal(PendingExpense p, SplitRDatabase db) throws IOException {
        Response<GenericResponse> r = RetrofitClient.getService()
                .addPersonalExpense(new ExpenseCreate(
                        p.amount, p.description, p.latitude, p.longitude, p.userId))
                .execute();
        if (r.isSuccessful()) {
            db.pendingExpenseDao().deleteById(p.localId);
            Log.d(TAG, "✓ Synced: " + p.description);
        } else {
            throw new IOException("Error " + r.code());
        }
    }

    private void syncGroup(PendingExpense p, SplitRDatabase db) throws IOException {
        Type specType = new TypeToken<List<MemberSpec>>(){}.getType();
        List<MemberSpec> specs = gson.fromJson(p.membersJson, specType);

        Response<FindOrCreateGroupResponse> gr = RetrofitClient.getService()
                .findOrCreateGroup(new FindOrCreateGroupRequest(p.userId, specs, null))
                .execute();
        if (!gr.isSuccessful() || gr.body() == null)
            throw new IOException("Group failed: " + gr.code());

        List<MemberSplit> splits = null;
        if (p.splitsJson != null) {
            Type splitType = new TypeToken<List<MemberSplit>>(){}.getType();
            splits = gson.fromJson(p.splitsJson, splitType);
        }

        Response<GroupExpenseResponse> r = RetrofitClient.getService()
                .addGroupExpense(new GroupExpenseCreate(
                        gr.body().groupId, p.amount, p.description,
                        p.latitude, p.longitude, p.userId, p.splitType, splits))
                .execute();
        if (r.isSuccessful()) {
            db.pendingExpenseDao().deleteById(p.localId);
            Log.d(TAG, "✓ Synced group expense: " + p.description);
        } else {
            throw new IOException("Error " + r.code());
        }
    }
}