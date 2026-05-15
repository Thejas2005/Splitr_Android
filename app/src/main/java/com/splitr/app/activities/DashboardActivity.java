package com.splitr.app.activities;

import android.content.Intent;
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
import com.splitr.app.models.Expense;
import com.splitr.app.models.GenericResponse;
import com.splitr.app.models.MyBalances;
import com.splitr.app.models.SplitItem;
import com.splitr.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private SessionManager session;

    private TextView tvUsername, tvNetBalance, tvIOwe, tvOwedToMe;
    private TextView tabExpenses, tabIOwe, tabOwed;
    private RecyclerView rvExpenses, rvIOwe, rvOwed;
    private ExpenseAdapter  expenseAdapter;
    private SplitItemAdapter iOweAdapter, owedAdapter;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) { startActivity(new Intent(this, LoginActivity.class)); finish(); return; }

        bindViews();
        setupRecyclers();
        setupTabs();
        setupBottomNav();
        fabAdd.setOnClickListener(v -> AddExpenseActivity.launch(this, null, null));
        tvUsername.setText("Hey, " + session.getUsername() + " 👋");
    }

    @Override protected void onResume() { super.onResume(); loadData(); }

    private void bindViews() {
        tvUsername   = findViewById(R.id.tvUsername);
        tvNetBalance = findViewById(R.id.tvNetBalance);
        tvIOwe       = findViewById(R.id.tvIOwe);
        tvOwedToMe   = findViewById(R.id.tvOwedToMe);
        tabExpenses  = findViewById(R.id.tabExpenses);
        tabIOwe      = findViewById(R.id.tabIOwe);
        tabOwed      = findViewById(R.id.tabOwed);
        rvExpenses   = findViewById(R.id.rvExpenses);
        rvIOwe       = findViewById(R.id.rvIOwe);
        rvOwed       = findViewById(R.id.rvOwed);
        bottomNav    = findViewById(R.id.bottomNav);
        fabAdd       = findViewById(R.id.fabAdd);
    }

    private void setupRecyclers() {
        expenseAdapter = new ExpenseAdapter(new ArrayList<>(), this::onExpenseClick);
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        rvExpenses.setAdapter(expenseAdapter);
        rvExpenses.setVisibility(View.VISIBLE);

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
        rvExpenses.setVisibility(idx == 0 ? View.VISIBLE : View.GONE);
        rvIOwe.setVisibility(idx == 1 ? View.VISIBLE : View.GONE);
        rvOwed.setVisibility(idx == 2 ? View.VISIBLE : View.GONE);

        int activeColor = getColor(R.color.blue_bright);
        int mutedColor  = getColor(R.color.muted);
        tabExpenses.setTextColor(idx == 0 ? activeColor : mutedColor);
        tabIOwe.setTextColor(idx == 1 ? activeColor : mutedColor);
        tabOwed.setTextColor(idx == 2 ? activeColor : mutedColor);
    }

    // ─── Data ────────────────────────────────────────────────────────────────

    private void loadData() { loadExpenses(); loadBalances(); }

    private void loadExpenses() {
        RetrofitClient.getService().getUserExpenses(session.getUserId())
                .enqueue(new Callback<List<Expense>>() {
                    @Override public void onResponse(Call<List<Expense>> call, Response<List<Expense>> resp) {
                        if (resp.isSuccessful() && resp.body() != null) expenseAdapter.setData(resp.body());
                    }
                    @Override public void onFailure(Call<List<Expense>> call, Throwable t) { toast("Failed to load expenses"); }
                });
    }

    private void loadBalances() {
        RetrofitClient.getService().getMyBalances(session.getUserId())
                .enqueue(new Callback<MyBalances>() {
                    @Override public void onResponse(Call<MyBalances> call, Response<MyBalances> resp) {
                        if (!resp.isSuccessful() || resp.body() == null) return;
                        MyBalances b = resp.body();

                        tvIOwe.setText(String.format(Locale.getDefault(), "₹%.2f", b.totalIOwe));
                        tvOwedToMe.setText(String.format(Locale.getDefault(), "₹%.2f", b.totalOwedToMe));

                        String netText = String.format(Locale.getDefault(), "₹%.2f", Math.abs(b.net));
                        if (b.net >= 0) {
                            tvNetBalance.setText("+" + netText);
                            tvNetBalance.setTextColor(getColor(R.color.green));
                        } else {
                            tvNetBalance.setText("-" + netText);
                            tvNetBalance.setTextColor(getColor(R.color.red));
                        }

                        iOweAdapter.setData(b.iOwe != null ? b.iOwe : new ArrayList<>());
                        owedAdapter.setData(b.owedToMe != null ? b.owedToMe : new ArrayList<>());
                    }
                    @Override public void onFailure(Call<MyBalances> call, Throwable t) { toast("Failed to load balances"); }
                });
    }

    // ─── Expense Detail Popup ────────────────────────────────────────────────

    /**
     * Shows a BottomSheet with full expense details when user taps an expense row.
     * Matches the web dashboard's clickable expense cards.
     */
    private void onExpenseClick(Expense e) {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View v = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_expense_detail, null);
        sheet.setContentView(v);

        TextView tvTitle    = v.findViewById(R.id.tvDetailTitle);
        TextView tvAmount   = v.findViewById(R.id.tvDetailAmount);
        TextView tvType     = v.findViewById(R.id.tvDetailType);
        TextView tvSplit    = v.findViewById(R.id.tvDetailSplit);
        TextView tvLocation = v.findViewById(R.id.tvDetailLocation);
        TextView tvGroup    = v.findViewById(R.id.tvDetailGroup);
        LinearLayout llSplits = v.findViewById(R.id.llDetailSplits);
        Button btnDelete    = v.findViewById(R.id.btnDetailDelete);
        Button btnClose     = v.findViewById(R.id.btnDetailClose);

        tvTitle.setText(e.description != null ? e.description : "—");
        tvAmount.setText(String.format(Locale.getDefault(), "₹%.2f", e.amount));
        tvType.setText(e.groupId != null ? "Group Expense" : "Personal");
        tvSplit.setText(e.splitType != null ? e.splitType : "—");
        tvLocation.setText(e.latitude != null
                ? String.format(Locale.getDefault(), "📍 %.5f, %.5f", e.latitude, e.longitude)
                : "No location");
        tvGroup.setText(e.groupId != null ? "Group #" + e.groupId : "—");

        // If group expense, load splits to show who owes what
        if (e.groupId != null) {
            RetrofitClient.getService().getMyBalances(session.getUserId())
                    .enqueue(new Callback<MyBalances>() {
                        @Override public void onResponse(Call<MyBalances> call, Response<MyBalances> resp) {
                            if (!resp.isSuccessful() || resp.body() == null) return;
                            MyBalances b = resp.body();
                            boolean found = false;
                            if (b.owedToMe != null)
                                for (SplitItem s : b.owedToMe)
                                    if (s.description != null && s.description.equals(e.description)
                                            && s.groupId != null && s.groupId == e.groupId) {
                                        addSplitDetail(llSplits, "← " + s.fromUsername, s.amount, false);
                                        found = true;
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

        btnDelete.setOnClickListener(vv -> {
            new androidx.appcompat.app.AlertDialog.Builder(this, R.style.DarkDialogTheme)
                    .setTitle("Delete Expense")
                    .setMessage("Delete \"" + e.description + "\"? This cannot be undone.")
                    .setPositiveButton("Delete", (d, w) -> {
                        RetrofitClient.getService().deleteExpense(e.id)
                                .enqueue(new Callback<GenericResponse>() {
                                    @Override public void onResponse(Call<GenericResponse> c, Response<GenericResponse> r) {
                                        if (r.isSuccessful()) { toast("Deleted"); sheet.dismiss(); loadData(); }
                                        else toast("Failed to delete");
                                    }
                                    @Override public void onFailure(Call<GenericResponse> c, Throwable t) { toast("Network error"); }
                                });
                    })
                    .setNegativeButton("Cancel", null).show();
        });

        btnClose.setOnClickListener(vv -> sheet.dismiss());
        sheet.show();
    }

    private void addSplitDetail(LinearLayout parent, String label, double amount, boolean iOwe) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_balance_row, parent, false);
        ((TextView) row.findViewById(R.id.tvBalanceParty)).setText(label);
        ((TextView) row.findViewById(R.id.tvBalanceAmt)).setText(
                String.format(Locale.getDefault(), "₹%.2f", amount));
        ((TextView) row.findViewById(R.id.tvBalanceAmt))
                .setTextColor(getColor(iOwe ? R.color.red : R.color.green));
        row.findViewById(R.id.btnBalanceSettle).setVisibility(View.GONE);
        row.findViewById(R.id.tvBalanceDesc).setVisibility(View.GONE);
        parent.addView(row);
    }

    // ─── Settle ──────────────────────────────────────────────────────────────

    private void onSettleIOwe(SplitItem item) {
        RetrofitClient.getService().settlePayment(item.splitId)
                .enqueue(new Callback<GenericResponse>() {
                    @Override public void onResponse(Call<GenericResponse> call, Response<GenericResponse> resp) {
                        if (resp.isSuccessful()) { toast("Settled ₹" + item.amount + " with " + item.toUsername); loadData(); }
                    }
                    @Override public void onFailure(Call<GenericResponse> c, Throwable t) { toast("Failed to settle"); }
                });
    }

    private void onSettleOwed(SplitItem item) {
        RetrofitClient.getService().settlePayment(item.splitId)
                .enqueue(new Callback<GenericResponse>() {
                    @Override public void onResponse(Call<GenericResponse> call, Response<GenericResponse> resp) {
                        if (resp.isSuccessful()) { toast("Marked received from " + item.fromUsername); loadData(); }
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
