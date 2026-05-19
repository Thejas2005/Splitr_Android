package com.splitr.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.splitr.app.R;
import com.splitr.app.adapters.GroupAdapter;
import com.splitr.app.api.RetrofitClient;
import com.splitr.app.models.AddMemberRequest;
import com.splitr.app.models.Expense;
import com.splitr.app.models.FindOrCreateGroupResponse;
import com.splitr.app.models.GenericResponse;
import com.splitr.app.models.Group;
import com.splitr.app.models.GroupCreate;
import com.splitr.app.models.GroupMember;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.splitr.app.utils.NetworkUtils;
import com.splitr.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupActivity extends AppCompatActivity {

    private SessionManager session;
    private SharedPreferences groupPrefs;
    private final Gson gson = new Gson();
    private RecyclerView rvGroups;
    private GroupAdapter groupAdapter;
    private FloatingActionButton fabCreate;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        session    = new SessionManager(this);
        groupPrefs = getSharedPreferences("group_cache_" + session.getUserId(), MODE_PRIVATE);
        rvGroups  = findViewById(R.id.rvGroups);
        fabCreate = findViewById(R.id.fabCreate);
        bottomNav = findViewById(R.id.bottomNav);

        groupAdapter = new GroupAdapter(new ArrayList<>(), this::onGroupClick);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));
        rvGroups.setAdapter(groupAdapter);

        setupBottomNav();
        fabCreate.setOnClickListener(v -> showCreateGroupDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGroups();
    }

    private void loadGroups() {
        // Show cached groups immediately so the screen isn't empty offline
        restoreCachedGroups();

        if (!NetworkUtils.isOnline(this)) return;

        RetrofitClient.getService().getUserGroups(session.getUserId())
                .enqueue(new Callback<List<Group>>() {
                    @Override public void onResponse(Call<List<Group>> call, Response<List<Group>> resp) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            saveCachedGroups(resp.body());
                            groupAdapter.setData(resp.body());
                        }
                    }
                    @Override public void onFailure(Call<List<Group>> c, Throwable t) {
                        toast("Offline — showing cached groups");
                    }
                });
    }

    private void saveCachedGroups(List<Group> groups) {
        groupPrefs.edit()
                .putString("groups_json", gson.toJson(groups))
                .apply();
    }

    private void restoreCachedGroups() {
        String json = groupPrefs.getString("groups_json", null);
        if (json == null) return;
        List<Group> cached = gson.fromJson(json,
                new TypeToken<List<Group>>(){}.getType());
        if (cached != null && !cached.isEmpty()) groupAdapter.setData(cached);
    }

    // ─── Group click → full group detail bottom sheet ────────────────────────

    private void onGroupClick(Group group) {
        showGroupDetailSheet(group);
    }

    /**
     * Full group detail bottom sheet matching the web dashboard's openGroupDetail():
     *  - Members list
     *  - Add member input
     *  - Group expenses list with split tags
     *  - Pending balances with settle button
     */
    private void showGroupDetailSheet(Group group) {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View v = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_group_detail, null);
        sheet.setContentView(v);

        TextView tvTitle    = v.findViewById(R.id.tvGroupDetailTitle);
        LinearLayout llMembers   = v.findViewById(R.id.llGroupMembers);
        LinearLayout llExpenses  = v.findViewById(R.id.llGroupExpenses);
        LinearLayout llBalances  = v.findViewById(R.id.llGroupBalances);
        EditText etAddMember     = v.findViewById(R.id.etAddGroupMember);
        Button btnAddMember      = v.findViewById(R.id.btnAddGroupMember);
        Button btnClose          = v.findViewById(R.id.btnGroupDetailClose);

        int groupId = group.getId();
        String groupName = group.name != null ? group.name : "Group " + groupId;
        tvTitle.setText(groupName);

        // Load members
        RetrofitClient.getService().getGroupMembers(groupId)
                .enqueue(new Callback<List<GroupMember>>() {
                    @Override public void onResponse(Call<List<GroupMember>> call, Response<List<GroupMember>> resp) {
                        if (resp.isSuccessful() && resp.body() != null) renderMembers(llMembers, resp.body());
                    }
                    @Override public void onFailure(Call<List<GroupMember>> c, Throwable t) {}
                });

        // Load group expenses (filter all expenses by group_id)
        RetrofitClient.getService().getAllExpenses()
                .enqueue(new Callback<List<Expense>>() {
                    @Override public void onResponse(Call<List<Expense>> call, Response<List<Expense>> resp) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            List<Expense> groupExp = new ArrayList<>();
                            for (Expense e : resp.body())
                                if (e.groupId != null && e.groupId == groupId) groupExp.add(e);
                            renderGroupExpenses(llExpenses, groupExp);
                        }
                    }
                    @Override public void onFailure(Call<List<Expense>> c, Throwable t) {}
                });

        // Load pending balances
        loadGroupBalances(groupId, llBalances);

        // Add member
        btnAddMember.setOnClickListener(vv -> {
            String input = etAddMember.getText().toString().trim();
            if (input.isEmpty()) return;
            // Try to match registered user via allUsers, otherwise guest
            fetchAllUsersAndAddMember(groupId, input, etAddMember, llMembers, llBalances);
        });

        btnClose.setOnClickListener(vv -> sheet.dismiss());
        sheet.show();
    }

    private void renderMembers(LinearLayout ll, List<GroupMember> members) {
        ll.removeAllViews();
        for (GroupMember m : members) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_group_member_row, ll, false);
            TextView tvName   = row.findViewById(R.id.tvMemberRowName);
            TextView tvBadge  = row.findViewById(R.id.tvMemberRowBadge);
            String name = m.username != null ? m.username : "Guest";
            tvName.setText(name);
            if (m.isGuest) {
                tvBadge.setText("guest"); tvBadge.setVisibility(View.VISIBLE);
            } else if (m.userId != null && m.userId == session.getUserId()) {
                tvBadge.setText("You"); tvBadge.setVisibility(View.VISIBLE);
                tvBadge.setBackgroundResource(R.drawable.bg_pill_blue);
            } else {
                tvBadge.setVisibility(View.GONE);
            }
            ll.addView(row);
        }
    }

    private void renderGroupExpenses(LinearLayout ll, List<Expense> expenses) {
        ll.removeAllViews();
        if (expenses.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No expenses yet.");
            tv.setTextColor(getColor(R.color.muted));
            tv.setTextSize(13);
            tv.setPadding(0, 8, 0, 8);
            ll.addView(tv);
            return;
        }
        for (Expense e : expenses) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_group_expense_row, ll, false);
            TextView tvDesc   = row.findViewById(R.id.tvGroupExpDesc);
            TextView tvAmt    = row.findViewById(R.id.tvGroupExpAmt);
            TextView tvSplit  = row.findViewById(R.id.tvGroupExpSplit);
            TextView tvPayer  = row.findViewById(R.id.tvGroupExpPayer);
            tvDesc.setText(e.description != null ? e.description : "—");
            tvAmt.setText(String.format(Locale.getDefault(), "₹%.2f", e.amount));
            tvSplit.setText(e.splitType != null ? e.splitType : "equal");
            tvPayer.setText("Paid by: " + (e.username != null ? e.username : "unknown"));
            ll.addView(row);
        }
    }

    private void loadGroupBalances(int groupId, LinearLayout llBalances) {
        // Use the balances endpoint which returns splits with payer/debtor info
        RetrofitClient.getService().getMyBalances(session.getUserId())
                .enqueue(new Callback<com.splitr.app.models.MyBalances>() {
                    @Override public void onResponse(Call<com.splitr.app.models.MyBalances> call,
                                                     Response<com.splitr.app.models.MyBalances> resp) {
                        if (!resp.isSuccessful() || resp.body() == null) return;
                        llBalances.removeAllViews();
                        com.splitr.app.models.MyBalances b = resp.body();

                        boolean any = false;
                        // Owe rows for this group
                        if (b.iOwe != null) {
                            for (com.splitr.app.models.SplitItem s : b.iOwe) {
                                if (s.groupId == null || s.groupId != groupId) continue;
                                addBalanceRow(llBalances, "→ " + s.toUsername, s.description,
                                        s.amount, true, s.splitId);
                                any = true;
                            }
                        }
                        // Owed rows for this group
                        if (b.owedToMe != null) {
                            for (com.splitr.app.models.SplitItem s : b.owedToMe) {
                                if (s.groupId == null || s.groupId != groupId) continue;
                                String from = s.fromUsername != null ? s.fromUsername : "Guest";
                                addBalanceRow(llBalances, "← " + from + (s.isGuest ? " (guest)" : ""),
                                        s.description, s.amount, false, s.splitId);
                                any = true;
                            }
                        }
                        if (!any) {
                            TextView tv = new TextView(GroupActivity.this);
                            tv.setText("🎉 All settled!");
                            tv.setTextColor(getColor(R.color.muted));
                            tv.setTextSize(13);
                            tv.setPadding(0, 12, 0, 12);
                            llBalances.addView(tv);
                        }
                    }
                    @Override public void onFailure(Call<com.splitr.app.models.MyBalances> c, Throwable t) {}
                });
    }

    private void addBalanceRow(LinearLayout parent, String party, String desc,
                               double amount, boolean iOwe, int splitId) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_balance_row, parent, false);
        TextView tvParty = row.findViewById(R.id.tvBalanceParty);
        TextView tvDesc  = row.findViewById(R.id.tvBalanceDesc);
        TextView tvAmt   = row.findViewById(R.id.tvBalanceAmt);
        Button   btnSettle = row.findViewById(R.id.btnBalanceSettle);

        tvParty.setText(party);
        tvDesc.setText(desc != null ? desc : "");
        tvAmt.setText(String.format(Locale.getDefault(), "₹%.2f", amount));
        tvAmt.setTextColor(getColor(iOwe ? R.color.red : R.color.green));
        btnSettle.setVisibility(View.VISIBLE);
        btnSettle.setText(iOwe ? "Mark Paid" : "Mark Received");
        btnSettle.setOnClickListener(v -> {
            RetrofitClient.getService().settlePayment(splitId)
                    .enqueue(new Callback<GenericResponse>() {
                        @Override public void onResponse(Call<GenericResponse> c, Response<GenericResponse> r) {
                            if (r.isSuccessful()) {
                                toast("Settled!");
                                parent.removeView(row);
                            }
                        }
                        @Override public void onFailure(Call<GenericResponse> c, Throwable t) { toast("Failed"); }
                    });
        });
        parent.addView(row);
    }

    private void fetchAllUsersAndAddMember(int groupId, String input, EditText etField,
                                           LinearLayout llMembers, LinearLayout llBalances) {
        RetrofitClient.getService().getAllUsers()
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override public void onResponse(Call<List<Map<String, Object>>> call,
                                                     Response<List<Map<String, Object>>> resp) {
                        AddMemberRequest req;
                        if (resp.isSuccessful() && resp.body() != null) {
                            Integer uid = null;
                            for (Map<String, Object> u : resp.body()) {
                                String uname = (String) u.get("username");
                                if (uname != null && uname.equalsIgnoreCase(input)) {
                                    uid = (int) ((double) u.get("id"));
                                    break;
                                }
                            }
                            req = uid != null
                                    ? new AddMemberRequest(groupId, uid, null)
                                    : new AddMemberRequest(groupId, null, input);
                        } else {
                            req = new AddMemberRequest(groupId, null, input);
                        }
                        doAddMember(req, input, etField, groupId, llMembers, llBalances);
                    }
                    @Override public void onFailure(Call<List<Map<String, Object>>> c, Throwable t) {
                        // Fallback to guest
                        doAddMember(new AddMemberRequest(groupId, null, input), input, etField, groupId, llMembers, llBalances);
                    }
                });
    }

    private void doAddMember(AddMemberRequest req, String name, EditText etField,
                             int groupId, LinearLayout llMembers, LinearLayout llBalances) {
        RetrofitClient.getService().addMember(req).enqueue(new Callback<GenericResponse>() {
            @Override public void onResponse(Call<GenericResponse> call, Response<GenericResponse> resp) {
                if (resp.isSuccessful()) {
                    toast(name + " added!");
                    etField.setText("");
                    // Refresh members
                    RetrofitClient.getService().getGroupMembers(groupId)
                            .enqueue(new Callback<List<GroupMember>>() {
                                @Override public void onResponse(Call<List<GroupMember>> c, Response<List<GroupMember>> r) {
                                    if (r.isSuccessful() && r.body() != null) renderMembers(llMembers, r.body());
                                }
                                @Override public void onFailure(Call<List<GroupMember>> c, Throwable t) {}
                            });
                } else {
                    toast("Already a member or error");
                }
            }
            @Override public void onFailure(Call<GenericResponse> c, Throwable t) { toast("Network error"); }
        });
    }

    // ─── Create group dialog ─────────────────────────────────────────────────

    private void showCreateGroupDialog() {
        View dv = LayoutInflater.from(this).inflate(R.layout.dialog_create_group, null);
        EditText etName = dv.findViewById(R.id.etGroupName);
        new AlertDialog.Builder(this, R.style.DarkDialogTheme)
                .setTitle("Create Group")
                .setView(dv)
                .setPositiveButton("Create", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) { toast("Enter a name"); return; }
                    createGroup(name);
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void createGroup(String name) {
        RetrofitClient.getService().createGroup(new GroupCreate(name, session.getUserId()))
                .enqueue(new Callback<FindOrCreateGroupResponse>() {
                    @Override public void onResponse(Call<FindOrCreateGroupResponse> call, Response<FindOrCreateGroupResponse> resp) {
                        if (resp.isSuccessful()) { toast("Group \"" + name + "\" created"); loadGroups(); } // loadGroups() refreshes + re-caches
                        else toast("Failed to create group");
                    }
                    @Override public void onFailure(Call<FindOrCreateGroupResponse> c, Throwable t) { toast("Network error"); }
                });
    }

    // ─── Bottom Nav ──────────────────────────────────────────────────────────

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navDashboard) {
                startActivity(new Intent(this, DashboardActivity.class)); finish(); return true;
            } else if (id == R.id.navMap) {
                startActivity(new Intent(this, MapActivity.class)); finish(); return true;
            } else if (id == R.id.navGroups) return true;
            return false;
        });
        bottomNav.setSelectedItemId(R.id.navGroups);
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}