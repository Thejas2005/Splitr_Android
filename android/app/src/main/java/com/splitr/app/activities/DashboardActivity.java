package com.splitr.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.splitr.app.R;
import com.splitr.app.adapters.ExpenseAdapter;
import com.splitr.app.adapters.SplitItemAdapter;
import com.splitr.app.api.RetrofitClient;
import com.splitr.app.cache.CachedExpense;
import com.splitr.app.cache.SplitRDatabase;
import com.splitr.app.models.Expense;
import com.splitr.app.models.GenericResponse;
import com.splitr.app.models.MyBalances;
import com.splitr.app.models.SplitItem;
import com.splitr.app.utils.NetworkUtils;
import com.splitr.app.utils.SessionManager;
import com.splitr.app.utils.SyncManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private SessionManager session;
    private SharedPreferences balancePrefs;

    private TextView tvUsername, tvNetBalance, tvIOwe, tvOwedToMe;
    private TextView tvIOweCount, tvOwedCount;
    private TextView tabExpenses, tabIOwe, tabOwed;
    private TextView tvOfflineBanner, tvPendingBanner, tvSummaryBar;
    private TextView btnLogout;
    private RecyclerView rvExpenses, rvIOwe, rvOwed;
    private ExpenseAdapter expenseAdapter;
    private SplitItemAdapter iOweAdapter, owedAdapter;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabAdd;

    // Track counts for summary bar
    private int totalExpenseCount = 0;
    private int iOweCount = 0;
    private int owedCount = 0;
    private double totalExpenseAmount = 0;
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        session = new SessionManager(this);
        balancePrefs = getSharedPreferences("balance_cache_" + session.getUserId(), MODE_PRIVATE);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bindViews();
        setupRecyclers();
        setupTabs();
        setupBottomNav();
        fabAdd.setOnClickListener(v -> AddExpenseActivity.launch(this, null, null));
        tvUsername.setText("Hey, " + session.getUsername() + " 👋");

        btnLogout.setOnClickListener(v ->
                new androidx.appcompat.app.AlertDialog.Builder(this, R.style.DarkDialogTheme)
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Log Out", (d, w) -> {
                            session.clearSession();
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("Cancel", null)
                        .show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    // ─── Bind ────────────────────────────────────────────────────────────────

    private void bindViews() {
        tvUsername      = findViewById(R.id.tvUsername);
        tvNetBalance    = findViewById(R.id.tvNetBalance);
        tvIOwe          = findViewById(R.id.tvIOwe);
        tvOwedToMe      = findViewById(R.id.tvOwedToMe);
        tvIOweCount     = findViewById(R.id.tvIOweCount);
        tvOwedCount     = findViewById(R.id.tvOwedCount);
        tabExpenses     = findViewById(R.id.tabExpenses);
        tabIOwe         = findViewById(R.id.tabIOwe);
        tabOwed         = findViewById(R.id.tabOwed);
        tvSummaryBar    = findViewById(R.id.tvSummaryBar);
        rvExpenses      = findViewById(R.id.rvExpenses);
        rvIOwe          = findViewById(R.id.rvIOwe);
        rvOwed          = findViewById(R.id.rvOwed);
        bottomNav       = findViewById(R.id.bottomNav);
        fabAdd          = findViewById(R.id.fabAdd);
        tvOfflineBanner = findViewById(R.id.tvOfflineBanner);
        tvPendingBanner = findViewById(R.id.tvPendingBanner);
        btnLogout       = findViewById(R.id.btnLogout);
    }

    private void setupRecyclers() {
        expenseAdapter = new ExpenseAdapter(new ArrayList<>(), this::onExpenseClick);
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        rvExpenses.setAdapter(expenseAdapter);

        iOweAdapter = new SplitItemAdapter(new ArrayList<>(), true, this::onSettleIOwe);
        rvIOwe.setLayoutManager(new LinearLayoutManager(this));
        rvIOwe.setAdapter(iOweAdapter);

        owedAdapter = new SplitItemAdapter(new ArrayList<>(), false, this::onSettleOwed);
        rvOwed.setLayoutManager(new LinearLayoutManager(this));
        rvOwed.setAdapter(owedAdapter);
    }

    private void setupTabs() {
        tabExpenses.setOnClickListener(v -> showTab(0));
        tabIOwe.setOnClickListener(v -> showTab(1));
        tabOwed.setOnClickListener(v -> showTab(2));
        showTab(0);
    }

    private void showTab(int idx) {
        currentTab = idx;
        rvExpenses.setVisibility(idx == 0 ? View.VISIBLE : View.GONE);
        rvIOwe.setVisibility(idx == 1 ? View.VISIBLE : View.GONE);
        rvOwed.setVisibility(idx == 2 ? View.VISIBLE : View.GONE);

        int activeColor = getColor(R.color.blue_bright);
        int mutedColor  = getColor(R.color.muted);
        tabExpenses.setTextColor(idx == 0 ? activeColor : mutedColor);
        tabIOwe.setTextColor(idx == 1 ? activeColor : mutedColor);
        tabOwed.setTextColor(idx == 2 ? activeColor : mutedColor);

        updateSummaryBar();
    }

    /** Updates the summary bar text based on the active tab */
    private void updateSummaryBar() {
        tvSummaryBar.setVisibility(View.VISIBLE);
        switch (currentTab) {
            case 0:
                if (totalExpenseCount > 0) {
                    tvSummaryBar.setText(totalExpenseCount + " expense"
                            + (totalExpenseCount == 1 ? "" : "s")
                            + "  ·  Total spent: ₹"
                            + String.format(Locale.getDefault(), "%.2f", totalExpenseAmount));
                } else {
                    tvSummaryBar.setText("No expenses yet");
                }
                break;
            case 1:
                tvSummaryBar.setText(iOweCount > 0
                        ? "You owe " + iOweCount + " person" + (iOweCount == 1 ? "" : "s")
                        : "You're all settled up! 🎉");
                break;
            case 2:
                tvSummaryBar.setText(owedCount > 0
                        ? owedCount + " person" + (owedCount == 1 ? "" : "s")
                        + " owe" + (owedCount == 1 ? "s" : "") + " you"
                        : "Nobody owes you anything yet");
                break;
        }
    }

    // ─── Data ────────────────────────────────────────────────────────────────

    private void loadData() {
        checkPendingSync();
        loadExpenses();
        restoreCachedBalances();
        loadBalances();
    }

    private void checkPendingSync() {
        new Thread(() -> {
            int count = SplitRDatabase.get(DashboardActivity.this)
                    .pendingExpenseDao().getPendingCount();
            runOnUiThread(() -> {
                if (count > 0) {
                    tvPendingBanner.setText("⏳ " + count + " expense(s) waiting to sync");
                    tvPendingBanner.setVisibility(View.VISIBLE);
                    if (NetworkUtils.isOnline(DashboardActivity.this)) {
                        SyncManager.syncNow(DashboardActivity.this);
                    }
                } else {
                    tvPendingBanner.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    // ─── Expenses ────────────────────────────────────────────────────────────

    private void loadExpenses() {
        if (NetworkUtils.isOnline(this)) {
            RetrofitClient.getService().getUserExpenses(session.getUserId())
                    .enqueue(new Callback<List<Expense>>() {
                        @Override
                        public void onResponse(Call<List<Expense>> call,
                                               Response<List<Expense>> resp) {
                            if (resp.isSuccessful() && resp.body() != null) {
                                List<Expense> expenses = resp.body();
                                expenseAdapter.setData(expenses);
                                tvOfflineBanner.setVisibility(View.GONE);

                                // Update expense count + total
                                totalExpenseCount  = expenses.size();
                                totalExpenseAmount = 0;
                                for (Expense e : expenses) totalExpenseAmount += e.amount;
                                updateSummaryBar();

                                // Cache in background
                                new Thread(() -> {
                                    SplitRDatabase db = SplitRDatabase.get(DashboardActivity.this);
                                    db.expenseDao().clearForUser(session.getUserId());
                                    List<CachedExpense> list = new ArrayList<>();
                                    for (Expense e : expenses) {
                                        CachedExpense c = new CachedExpense();
                                        c.id = e.id; c.amount = e.amount;
                                        c.description = e.description;
                                        c.latitude = e.latitude; c.longitude = e.longitude;
                                        c.userId = e.userId; c.groupId = e.groupId;
                                        c.splitType = e.splitType; c.username = e.username;
                                        c.cachedAt = System.currentTimeMillis();
                                        list.add(c);
                                    }
                                    db.expenseDao().insertAll(list);
                                }).start();
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Expense>> call, Throwable t) {
                            loadExpensesFromCache();
                        }
                    });
        } else {
            loadExpensesFromCache();
        }
    }

    private void loadExpensesFromCache() {
        tvOfflineBanner.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<CachedExpense> cached = SplitRDatabase.get(DashboardActivity.this)
                    .expenseDao().getExpensesForUser(session.getUserId());
            List<Expense> list = new ArrayList<>();
            double total = 0;
            for (CachedExpense c : cached) {
                Expense e = new Expense();
                e.id = c.id; e.amount = c.amount; e.description = c.description;
                e.latitude = c.latitude; e.longitude = c.longitude;
                e.userId = c.userId; e.groupId = c.groupId;
                e.splitType = c.splitType; e.username = c.username;
                list.add(e);
                total += c.amount;
            }
            final double fTotal = total;
            runOnUiThread(() -> {
                expenseAdapter.setData(list);
                totalExpenseCount  = list.size();
                totalExpenseAmount = fTotal;
                updateSummaryBar();
            });
        }).start();
    }

    // ─── Balances ────────────────────────────────────────────────────────────

    private void loadBalances() {
        if (!NetworkUtils.isOnline(this)) return; // cached data already shown by restoreCachedBalances()

        RetrofitClient.getService().getMyBalances(session.getUserId())
                .enqueue(new Callback<MyBalances>() {
                    @Override
                    public void onResponse(Call<MyBalances> call, Response<MyBalances> resp) {
                        if (!resp.isSuccessful() || resp.body() == null) return;
                        MyBalances b = resp.body();

                        // Persist to cache for offline use
                        saveCachedBalances(b);

                        // Totals
                        tvIOwe.setText(String.format(Locale.getDefault(), "₹%.2f", b.totalIOwe));
                        tvOwedToMe.setText(String.format(Locale.getDefault(), "₹%.2f", b.totalOwedToMe));

                        // Net
                        String netText = String.format(Locale.getDefault(), "₹%.2f", Math.abs(b.net));
                        if (b.net >= 0) {
                            tvNetBalance.setText("+" + netText);
                            tvNetBalance.setTextColor(getColor(R.color.green));
                        } else {
                            tvNetBalance.setText("-" + netText);
                            tvNetBalance.setTextColor(getColor(R.color.red));
                        }

                        // Counts
                        iOweCount = b.iOwe != null ? b.iOwe.size() : 0;
                        owedCount = b.owedToMe != null ? b.owedToMe.size() : 0;

                        tvIOweCount.setText(iOweCount + " person" + (iOweCount == 1 ? "" : "s"));
                        tvOwedCount.setText(owedCount + " person" + (owedCount == 1 ? "" : "s"));

                        // Lists
                        iOweAdapter.setData(b.iOwe != null ? b.iOwe : new ArrayList<>());
                        owedAdapter.setData(b.owedToMe != null ? b.owedToMe : new ArrayList<>());

                        updateSummaryBar();
                    }

                    @Override
                    public void onFailure(Call<MyBalances> call, Throwable t) {
                        toast("Failed to load balances");
                    }
                });
    }

    private void saveCachedBalances(MyBalances b) {
        SharedPreferences.Editor ed = balancePrefs.edit();
        ed.putFloat("net",       (float) b.net);
        ed.putFloat("i_owe",     (float) b.totalIOwe);
        ed.putFloat("owed_me",   (float) b.totalOwedToMe);
        ed.putInt  ("i_owe_cnt", b.iOwe     != null ? b.iOwe.size()     : 0);
        ed.putInt  ("owed_cnt",  b.owedToMe != null ? b.owedToMe.size() : 0);
        ed.apply();
    }

    private void restoreCachedBalances() {
        if (!balancePrefs.contains("net")) return; // no cache yet

        float net     = balancePrefs.getFloat("net",       0f);
        float iOwe    = balancePrefs.getFloat("i_owe",     0f);
        float owedMe  = balancePrefs.getFloat("owed_me",   0f);
        int   iOweCnt = balancePrefs.getInt  ("i_owe_cnt", 0);
        int   owedCnt = balancePrefs.getInt  ("owed_cnt",  0);

        tvIOwe.setText(String.format(Locale.getDefault(), "₹%.2f", (double) iOwe));
        tvOwedToMe.setText(String.format(Locale.getDefault(), "₹%.2f", (double) owedMe));

        String netText = String.format(Locale.getDefault(), "₹%.2f", Math.abs((double) net));
        if (net >= 0) {
            tvNetBalance.setText("+" + netText);
            tvNetBalance.setTextColor(getColor(R.color.green));
        } else {
            tvNetBalance.setText("-" + netText);
            tvNetBalance.setTextColor(getColor(R.color.red));
        }

        iOweCount = iOweCnt;
        owedCount = owedCnt;
        tvIOweCount.setText(iOweCnt + " person" + (iOweCnt == 1 ? "" : "s"));
        tvOwedCount.setText(owedCnt + " person" + (owedCnt == 1 ? "" : "s"));

        updateSummaryBar();
    }

    // ─── Expense Detail Popup ────────────────────────────────────────────────

    private void onExpenseClick(Expense e) {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View v = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_expense_detail, null);
        sheet.setContentView(v);

        TextView     tvTitle    = v.findViewById(R.id.tvDetailTitle);
        TextView     tvAmount   = v.findViewById(R.id.tvDetailAmount);
        TextView     tvType     = v.findViewById(R.id.tvDetailType);
        TextView     tvSplit    = v.findViewById(R.id.tvDetailSplit);
        TextView     tvLocation = v.findViewById(R.id.tvDetailLocation);
        TextView     tvGroup    = v.findViewById(R.id.tvDetailGroup);
        LinearLayout llSplits   = v.findViewById(R.id.llDetailSplits);
        Button       btnDelete  = v.findViewById(R.id.btnDetailDelete);
        Button       btnClose   = v.findViewById(R.id.btnDetailClose);

        tvTitle.setText(e.description != null ? e.description : "—");
        tvAmount.setText(String.format(Locale.getDefault(), "₹%.2f", e.amount));
        tvType.setText(e.groupId != null ? "Group Expense" : "Personal");
        tvSplit.setText(e.splitType != null ? e.splitType : "—");
        tvLocation.setText(e.latitude != null
                ? String.format(Locale.getDefault(), "📍 %.5f, %.5f", e.latitude, e.longitude)
                : "No location");
        tvGroup.setText(e.groupId != null ? "Group #" + e.groupId : "—");

        if (e.groupId != null) {
            RetrofitClient.getService().getMyBalances(session.getUserId())
                    .enqueue(new Callback<MyBalances>() {
                        @Override
                        public void onResponse(Call<MyBalances> call, Response<MyBalances> resp) {
                            if (!resp.isSuccessful() || resp.body() == null) return;
                            MyBalances b = resp.body();
                            boolean found = false;
                            if (b.owedToMe != null) {
                                for (SplitItem s : b.owedToMe) {
                                    if (s.description != null
                                            && s.description.equals(e.description)
                                            && s.groupId != null && s.groupId == e.groupId) {
                                        addSplitDetail(llSplits, "← " + s.fromUsername, s.amount, false);
                                        found = true;
                                    }
                                }
                            }
                            if (!found) {
                                TextView tv = new TextView(DashboardActivity.this);
                                tv.setText("No pending splits");
                                tv.setTextColor(getColor(R.color.muted));
                                tv.setTextSize(13);
                                llSplits.addView(tv);
                            }
                        }
                        @Override public void onFailure(Call<MyBalances> call, Throwable t) {}
                    });
        }

        btnDelete.setOnClickListener(vv ->
                new androidx.appcompat.app.AlertDialog.Builder(
                        DashboardActivity.this, R.style.DarkDialogTheme)
                        .setTitle("Delete Expense")
                        .setMessage("Delete \"" + e.description + "\"?")
                        .setPositiveButton("Delete", (d, w) ->
                                RetrofitClient.getService().deleteExpense(e.id)
                                        .enqueue(new Callback<GenericResponse>() {
                                            @Override public void onResponse(Call<GenericResponse> c,
                                                                             Response<GenericResponse> r) {
                                                if (r.isSuccessful()) {
                                                    toast("Deleted");
                                                    sheet.dismiss();
                                                    loadData();
                                                } else toast("Failed to delete");
                                            }
                                            @Override public void onFailure(Call<GenericResponse> c,
                                                                            Throwable t) { toast("Network error"); }
                                        }))
                        .setNegativeButton("Cancel", null).show());

        btnClose.setOnClickListener(vv -> sheet.dismiss());
        sheet.show();
    }

    private void addSplitDetail(LinearLayout parent, String label, double amount, boolean iOwe) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_balance_row, parent, false);
        ((TextView) row.findViewById(R.id.tvBalanceParty)).setText(label);
        TextView tvAmt = row.findViewById(R.id.tvBalanceAmt);
        tvAmt.setText(String.format(Locale.getDefault(), "₹%.2f", amount));
        tvAmt.setTextColor(getColor(iOwe ? R.color.red : R.color.green));
        row.findViewById(R.id.btnBalanceSettle).setVisibility(View.GONE);
        row.findViewById(R.id.tvBalanceDesc).setVisibility(View.GONE);
        parent.addView(row);
    }

    // ─── Settle ──────────────────────────────────────────────────────────────

    private void onSettleIOwe(SplitItem item) {
        RetrofitClient.getService().settlePayment(item.splitId)
                .enqueue(new Callback<GenericResponse>() {
                    @Override public void onResponse(Call<GenericResponse> call,
                                                     Response<GenericResponse> resp) {
                        if (resp.isSuccessful()) {
                            toast("Settled ₹" + item.amount + " with " + item.toUsername);
                            loadData();
                        }
                    }
                    @Override public void onFailure(Call<GenericResponse> c, Throwable t) { toast("Failed"); }
                });
    }

    private void onSettleOwed(SplitItem item) {
        RetrofitClient.getService().settlePayment(item.splitId)
                .enqueue(new Callback<GenericResponse>() {
                    @Override public void onResponse(Call<GenericResponse> call,
                                                     Response<GenericResponse> resp) {
                        if (resp.isSuccessful()) {
                            toast("Marked received from " + item.fromUsername);
                            loadData();
                        }
                    }
                    @Override public void onFailure(Call<GenericResponse> c, Throwable t) { toast("Failed"); }
                });
    }

    // ─── Bottom Nav ──────────────────────────────────────────────────────────

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navDashboard) return true;
            else if (id == R.id.navMap) { startActivity(new Intent(this, MapActivity.class)); return true; }
            else if (id == R.id.navGroups) { startActivity(new Intent(this, GroupActivity.class)); return true; }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.navDashboard);
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}