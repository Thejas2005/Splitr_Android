package com.splitr.app.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.splitr.app.R;
import com.splitr.app.api.RetrofitClient;
import com.splitr.app.models.ExpenseCreate;
import com.splitr.app.models.FindOrCreateGroupRequest;
import com.splitr.app.models.FindOrCreateGroupResponse;
import com.splitr.app.models.GenericResponse;
import com.splitr.app.models.GroupExpenseCreate;
import com.splitr.app.models.GroupExpenseResponse;
import com.splitr.app.models.MemberSplit;
import com.splitr.app.models.MemberSpec;
import com.splitr.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Add Expense screen.
 *
 * Features:
 *  - Personal or group expense
 *  - Members: registered users (looked up) OR guest names (free text)
 *  - Auto find-or-create group by member set (same logic as web dashboard)
 *  - Split types: Equal / Percentage / Dutch (custom amounts)
 *  - Dutch split: Bill Splitter bottom sheet (item-level assignment)
 *  - Split builder shows ALL members including payer
 *  - Validation matching backend rules
 */
public class AddExpenseActivity extends AppCompatActivity {

    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LNG = "lng";

    public static void launch(Context ctx, Double lat, Double lng) {
        Intent i = new Intent(ctx, AddExpenseActivity.class);
        if (lat != null) { i.putExtra(EXTRA_LAT, lat); i.putExtra(EXTRA_LNG, lng); }
        ctx.startActivity(i);
    }

    // ─── State ────────────────────────────────────────────────────────────────
    private SessionManager session;
    private Double pickedLat, pickedLng;
    private List<Map<String, Object>> allUsers = new ArrayList<>();

    // Members: { userId (nullable), guestName (nullable), displayName, isGuest }
    private final List<MemberEntry> members = new ArrayList<>();

    // Bill splitter items
    private final List<BillItem> billItems = new ArrayList<>();

    // ─── Views ────────────────────────────────────────────────────────────────
    private EditText etAmount, etDescription;
    private TextView tvLocation, tvSplitValidation;
    private AutoCompleteTextView etMemberInput;
    private LinearLayout llMemberChips, llSplitSection, llSplitRows, llBillBtn;
    private RadioGroup rgSplitType;
    private Button btnSave, btnCancel, btnAddMember, btnBillSplitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense_v2);

        session   = new SessionManager(this);
        pickedLat = getIntent().hasExtra(EXTRA_LAT) ? getIntent().getDoubleExtra(EXTRA_LAT, 0) : null;
        pickedLng = getIntent().hasExtra(EXTRA_LNG) ? getIntent().getDoubleExtra(EXTRA_LNG, 0) : null;

        bindViews();
        loadAllUsers();
        setupLocationDisplay();
        setupMemberInput();
        setupSplitTypeToggle();
        setupButtons();
    }

    // ─── Bind ────────────────────────────────────────────────────────────────

    private void bindViews() {
        etAmount         = findViewById(R.id.etAmount);
        etDescription    = findViewById(R.id.etDescription);
        tvLocation       = findViewById(R.id.tvLocation);
        tvSplitValidation= findViewById(R.id.tvSplitValidation);
        etMemberInput    = findViewById(R.id.etMemberInput);
        llMemberChips    = findViewById(R.id.llMemberChips);
        llSplitSection   = findViewById(R.id.llSplitSection);
        llSplitRows      = findViewById(R.id.llSplitRows);
        llBillBtn        = findViewById(R.id.llBillBtn);
        rgSplitType      = findViewById(R.id.rgSplitType);
        btnSave          = findViewById(R.id.btnSave);
        btnCancel        = findViewById(R.id.btnCancel);
        btnAddMember     = findViewById(R.id.btnAddMember);
        btnBillSplitter  = findViewById(R.id.btnBillSplitter);
    }

    private void setupLocationDisplay() {
        if (pickedLat != null) {
            tvLocation.setText(String.format(Locale.getDefault(), "📍 %.5f, %.5f", pickedLat, pickedLng));
        } else {
            tvLocation.setText("No location selected");
        }
    }

    // ─── Users / Members ──────────────────────────────────────────────────────

    private void loadAllUsers() {
        RetrofitClient.getService().getAllUsers().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(Call<List<Map<String, Object>>> call,
                                              Response<List<Map<String, Object>>> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    allUsers = resp.body();
                    setupAutocomplete();
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
        });
    }

    private void setupAutocomplete() {
        List<String> names = new ArrayList<>();
        for (Map<String, Object> u : allUsers) {
            String uname = (String) u.get("username");
            if (uname != null && !uname.equals(session.getUsername())) names.add(uname);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names);
        etMemberInput.setAdapter(adapter);
        etMemberInput.setThreshold(1);
    }

    private void setupMemberInput() {
        btnAddMember.setOnClickListener(v -> addMemberChip());
        etMemberInput.setOnEditorActionListener((v, actionId, event) -> {
            addMemberChip(); return true;
        });
    }

    private void addMemberChip() {
        String val = etMemberInput.getText().toString().trim();
        if (val.isEmpty()) return;

        // Block adding self
        if (val.equalsIgnoreCase(session.getUsername())) {
            toast("You are always the payer!"); etMemberInput.setText(""); return;
        }

        // Check duplicate
        for (MemberEntry m : members) {
            if ((m.displayName != null && m.displayName.equalsIgnoreCase(val))
                    || (m.guestName != null && m.guestName.equalsIgnoreCase(val))) {
                toast("Already added"); etMemberInput.setText(""); return;
            }
        }

        // Match registered user
        MemberEntry entry = null;
        for (Map<String, Object> u : allUsers) {
            String uname = (String) u.get("username");
            if (uname != null && uname.equalsIgnoreCase(val)) {
                double uid = (double) u.get("id");
                entry = new MemberEntry((int) uid, null, uname, false);
                break;
            }
        }
        if (entry == null) {
            // Guest
            entry = new MemberEntry(null, val, val, true);
        }

        members.add(entry);
        etMemberInput.setText("");
        renderMemberChips();
        updateSplitSection();
    }

    private void renderMemberChips() {
        llMemberChips.removeAllViews();
        for (int i = 0; i < members.size(); i++) {
            MemberEntry m = members.get(i);
            View chip = LayoutInflater.from(this).inflate(R.layout.item_member_chip, llMemberChips, false);
            TextView tvName   = chip.findViewById(R.id.tvChipName);
            Button   btnRemove= chip.findViewById(R.id.btnChipRemove);
            tvName.setText(m.displayName + (m.isGuest ? " (guest)" : ""));
            final int idx = i;
            btnRemove.setOnClickListener(v -> { members.remove(idx); renderMemberChips(); updateSplitSection(); });
            llMemberChips.addView(chip);
        }
    }

    // ─── Split Section ────────────────────────────────────────────────────────

    private void setupSplitTypeToggle() {
        llSplitSection.setVisibility(View.GONE);
        rgSplitType.setOnCheckedChangeListener((group, checkedId) -> {
            rebuildSplitRows();
            boolean isDutch = (checkedId == R.id.rbDutch);
            llBillBtn.setVisibility(isDutch && !members.isEmpty() ? View.VISIBLE : View.GONE);
        });
        btnBillSplitter.setOnClickListener(v -> openBillSplitter());
    }

    private void updateSplitSection() {
        if (members.isEmpty()) {
            llSplitSection.setVisibility(View.GONE);
            llBillBtn.setVisibility(View.GONE);
        } else {
            llSplitSection.setVisibility(View.VISIBLE);
            rebuildSplitRows();
            boolean isDutch = (rgSplitType.getCheckedRadioButtonId() == R.id.rbDutch);
            llBillBtn.setVisibility(isDutch ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Builds split input rows for ALL participants: payer first, then members.
     * This matches the web dashboard's renderSplitBuilder() exactly.
     */
    private void rebuildSplitRows() {
        llSplitRows.removeAllViews();
        tvSplitValidation.setText("");
        int checkedId = rgSplitType.getCheckedRadioButtonId();
        if (checkedId == R.id.rbEqual) return; // equal = no inputs needed

        // Payer row
        addSplitRow(session.getUserId(), null, session.getUsername() + " (you, payer)", true);

        // Member rows
        for (MemberEntry m : members) {
            addSplitRow(m.userId != null ? m.userId : -1, m.guestName,
                    m.displayName + (m.isGuest ? " (guest)" : ""), false);
        }

        // Live validation as user types
        setupSplitValidationWatcher();
    }

    private void addSplitRow(int userId, String guestName, String label, boolean isPayer) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_split_row_v2, llSplitRows, false);
        TextView tvName = row.findViewById(R.id.tvMemberName);
        EditText etVal  = row.findViewById(R.id.etSplitValue);
        TextView tvSuffix = row.findViewById(R.id.tvSplitSuffix);

        tvName.setText(label);
        boolean isPct = (rgSplitType.getCheckedRadioButtonId() == R.id.rbPercentage);
        tvSuffix.setText(isPct ? "%" : "₹");
        etVal.setHint(isPct ? "0" : "0.00");

        // Tag: "userId|guestName" — used when collecting splits
        etVal.setTag(userId + "|" + (guestName != null ? guestName : ""));
        llSplitRows.addView(row);
    }

    private void setupSplitValidationWatcher() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateSplitInputs(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        for (int i = 0; i < llSplitRows.getChildCount(); i++) {
            EditText et = llSplitRows.getChildAt(i).findViewById(R.id.etSplitValue);
            if (et != null) et.addTextChangedListener(watcher);
        }
    }

    private void validateSplitInputs() {
        boolean isPct = (rgSplitType.getCheckedRadioButtonId() == R.id.rbPercentage);
        double total = getSplitTotal();
        if (isPct) {
            double remaining = 100.0 - total;
            if (Math.abs(remaining) < 0.01)
                tvSplitValidation.setText("✓ Percentages sum to 100%");
            else
                tvSplitValidation.setText(String.format(Locale.getDefault(), "⚠ %.1f%% remaining", remaining));
        } else {
            double amount = getAmountInput();
            if (amount > 0) {
                double diff = amount - total;
                if (Math.abs(diff) < 0.5)
                    tvSplitValidation.setText("✓ Amounts add up");
                else
                    tvSplitValidation.setText(String.format(Locale.getDefault(), "⚠ ₹%.2f %s",
                            Math.abs(diff), diff > 0 ? "remaining" : "over"));
            }
        }
    }

    private double getSplitTotal() {
        double total = 0;
        for (int i = 0; i < llSplitRows.getChildCount(); i++) {
            EditText et = llSplitRows.getChildAt(i).findViewById(R.id.etSplitValue);
            if (et != null) {
                try { total += Double.parseDouble(et.getText().toString()); } catch (NumberFormatException ignored) {}
            }
        }
        return total;
    }

    private double getAmountInput() {
        try { return Double.parseDouble(etAmount.getText().toString().trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    // ─── Bill Splitter ────────────────────────────────────────────────────────

    private void openBillSplitter() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View v = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_bill_splitter, null);
        sheet.setContentView(v);

        // Build all-member list: payer + members
        List<BillMember> allBillMembers = getAllBillMembers();
        if (billItems.isEmpty()) billItems.add(new BillItem("", 1, 0, allBillKeys(allBillMembers)));

        LinearLayout llItems   = v.findViewById(R.id.llBillItems);
        EditText etTax         = v.findViewById(R.id.etBillTax);
        EditText etService     = v.findViewById(R.id.etBillService);
        TextView tvSubtotal    = v.findViewById(R.id.tvBillSubtotal);
        TextView tvTaxLine     = v.findViewById(R.id.tvBillTaxLine);
        TextView tvServiceLine = v.findViewById(R.id.tvBillServiceLine);
        TextView tvGrandTotal  = v.findViewById(R.id.tvBillGrandTotal);
        LinearLayout llPersonTotals = v.findViewById(R.id.llBillPersonTotals);
        Button btnApply        = v.findViewById(R.id.btnApplyBill);
        Button btnAddItem      = v.findViewById(R.id.btnAddBillItem);

        // Render helper: captures all views
        Runnable[] renderAll = {null};
        renderAll[0] = () -> renderBillSheet(llItems, etTax, etService,
                tvSubtotal, tvTaxLine, tvServiceLine, tvGrandTotal,
                llPersonTotals, allBillMembers, renderAll);

        renderAll[0].run();

        btnAddItem.setOnClickListener(vv -> {
            billItems.add(new BillItem("", 1, 0, allBillKeys(allBillMembers)));
            renderAll[0].run();
        });

        TextWatcher recalcWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { renderAll[0].run(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        etTax.addTextChangedListener(recalcWatcher);
        etService.addTextChangedListener(recalcWatcher);

        btnApply.setOnClickListener(vv -> {
            applyBillToDutch(allBillMembers, etTax, etService);
            sheet.dismiss();
        });

        sheet.show();
    }

    private List<BillMember> getAllBillMembers() {
        List<BillMember> list = new ArrayList<>();
        list.add(new BillMember(session.getUserId(), null, session.getUsername() + " ★", false));
        for (MemberEntry m : members)
            list.add(new BillMember(m.userId != null ? m.userId : -1, m.guestName,
                    m.displayName + (m.isGuest ? " (guest)" : ""), m.isGuest));
        return list;
    }

    private List<String> allBillKeys(List<BillMember> members) {
        List<String> keys = new ArrayList<>();
        for (BillMember m : members) keys.add(billKey(m));
        return keys;
    }

    private String billKey(BillMember m) {
        return m.guestName != null ? "g:" + m.guestName : "u:" + m.userId;
    }

    private void renderBillSheet(LinearLayout llItems, EditText etTax, EditText etService,
                                  TextView tvSubtotal, TextView tvTaxLine, TextView tvServiceLine,
                                  TextView tvGrandTotal, LinearLayout llPersonTotals,
                                  List<BillMember> allBillMembers, Runnable[] renderAll) {
        // Recalc totals
        double tax     = parseOr0(etTax.getText().toString());
        double service = parseOr0(etService.getText().toString());
        double subtotal = 0;
        for (BillItem it : billItems) subtotal += it.qty * it.price;
        double taxAmt  = subtotal * tax / 100;
        double svcAmt  = subtotal * service / 100;
        double grand   = subtotal + taxAmt + svcAmt;
        double factor  = grand / (subtotal > 0 ? subtotal : 1);

        // Per-person amounts
        double[] personAmts = new double[allBillMembers.size()];
        for (BillItem it : billItems) {
            double itemTotal = it.qty * it.price;
            int assigned = it.assignedKeys.size();
            if (assigned == 0) continue;
            double share = itemTotal / assigned;
            for (int pi = 0; pi < allBillMembers.size(); pi++) {
                if (it.assignedKeys.contains(billKey(allBillMembers.get(pi))))
                    personAmts[pi] += share;
            }
        }
        for (int pi = 0; pi < personAmts.length; pi++) personAmts[pi] *= factor;

        // Render item rows
        llItems.removeAllViews();
        for (int idx = 0; idx < billItems.size(); idx++) {
            BillItem item = billItems.get(idx);
            View row = LayoutInflater.from(this).inflate(R.layout.item_bill_row, llItems, false);
            EditText etName  = row.findViewById(R.id.etBillItemName);
            EditText etQty   = row.findViewById(R.id.etBillItemQty);
            EditText etPrice = row.findViewById(R.id.etBillItemPrice);
            Button   btnDel  = row.findViewById(R.id.btnDeleteBillItem);
            LinearLayout llAssign = row.findViewById(R.id.llBillAssign);

            etName.setText(item.name);
            etQty.setText(item.qty > 0 ? String.valueOf(item.qty) : "");
            etPrice.setText(item.price > 0 ? String.format(Locale.getDefault(), "%.2f", item.price) : "");

            final int fIdx = idx;
            TextWatcher tw = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    billItems.get(fIdx).name  = etName.getText().toString();
                    billItems.get(fIdx).qty   = parseIntOr1(etQty.getText().toString());
                    billItems.get(fIdx).price = parseOr0(etPrice.getText().toString());
                    renderAll[0].run();
                }
                @Override public void afterTextChanged(Editable s) {}
            };
            etName.addTextChangedListener(tw);
            etQty.addTextChangedListener(tw);
            etPrice.addTextChangedListener(tw);

            btnDel.setOnClickListener(v -> { billItems.remove(fIdx); renderAll[0].run(); });

            // Assign chips
            llAssign.removeAllViews();
            for (BillMember bm : allBillMembers) {
                String key = billKey(bm);
                Button chip = new Button(this);
                chip.setText(bm.displayName);
                chip.setTextSize(11);
                chip.setPadding(20, 6, 20, 6);
                boolean sel = item.assignedKeys.contains(key);
                chip.setBackgroundResource(sel ? R.drawable.bg_button_primary : R.drawable.bg_btn_secondary);
                chip.setTextColor(getColor(sel ? R.color.white : R.color.muted));
                chip.setOnClickListener(v -> {
                    if (item.assignedKeys.contains(key)) {
                        if (item.assignedKeys.size() > 1) item.assignedKeys.remove(key);
                        else toast("At least one person must be assigned");
                    } else {
                        item.assignedKeys.add(key);
                    }
                    renderAll[0].run();
                });
                llAssign.addView(chip);
            }
            llItems.addView(row);
        }

        // Summary
        tvSubtotal.setText(fmt(subtotal));
        tvTaxLine.setText(taxAmt > 0 ? "Tax: " + fmt(taxAmt) : "");
        tvServiceLine.setText(svcAmt > 0 ? "Service: " + fmt(svcAmt) : "");
        tvGrandTotal.setText(fmt(grand));

        // Per-person
        llPersonTotals.removeAllViews();
        for (int pi = 0; pi < allBillMembers.size(); pi++) {
            BillMember bm = allBillMembers.get(pi);
            View prow = LayoutInflater.from(this).inflate(R.layout.item_bill_person_total, llPersonTotals, false);
            ((TextView) prow.findViewById(R.id.tvBillPersonName)).setText(bm.displayName);
            ((TextView) prow.findViewById(R.id.tvBillPersonAmt)).setText(fmt(personAmts[pi]));
            llPersonTotals.addView(prow);
        }

        // Store for apply
        llPersonTotals.setTag(personAmts);
        tvGrandTotal.setTag(grand);
    }

    private void applyBillToDutch(List<BillMember> allBillMembers, EditText etTax, EditText etService) {
        // Switch to dutch
        rgSplitType.check(R.id.rbDutch);
        rebuildSplitRows();

        // Get grand total from current bill state
        double subtotal = 0;
        for (BillItem it : billItems) subtotal += it.qty * it.price;
        double tax     = parseOr0(etTax.getText().toString());
        double service = parseOr0(etService.getText().toString());
        double grand   = subtotal * (1 + tax / 100 + service / 100);
        double factor  = grand / (subtotal > 0 ? subtotal : 1);

        double[] personAmts = new double[allBillMembers.size()];
        for (BillItem it : billItems) {
            double itemTotal = it.qty * it.price;
            int assigned = it.assignedKeys.size();
            if (assigned == 0) continue;
            for (int pi = 0; pi < allBillMembers.size(); pi++) {
                if (it.assignedKeys.contains(billKey(allBillMembers.get(pi))))
                    personAmts[pi] += (itemTotal / assigned);
            }
        }
        for (int pi = 0; pi < personAmts.length; pi++) personAmts[pi] *= factor;

        // Set total amount
        etAmount.setText(String.format(Locale.getDefault(), "%.2f", grand));

        // Fill split rows — order matches: payer first, then members
        for (int i = 0; i < llSplitRows.getChildCount(); i++) {
            EditText et = llSplitRows.getChildAt(i).findViewById(R.id.etSplitValue);
            if (et != null && i < personAmts.length)
                et.setText(String.format(Locale.getDefault(), "%.2f", personAmts[i]));
        }

        validateSplitInputs();
        toast("Bill amounts applied! ₹" + String.format(Locale.getDefault(), "%.2f", grand));
    }

    // ─── Save ────────────────────────────────────────────────────────────────

    private void setupButtons() {
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void validateAndSave() {
        String amtStr = etAmount.getText().toString().trim();
        String desc   = etDescription.getText().toString().trim();
        if (amtStr.isEmpty()) { toast("Enter amount"); return; }
        double amount;
        try { amount = Double.parseDouble(amtStr); } catch (NumberFormatException e) { toast("Invalid amount"); return; }
        if (amount <= 0) { toast("Amount must be > 0"); return; }
        if (desc.isEmpty()) { toast("Enter description"); return; }

        btnSave.setEnabled(false);
        btnSave.setText("Saving…");

        if (members.isEmpty()) {
            savePersonalExpense(amount, desc);
        } else {
            saveGroupExpense(amount, desc);
        }
    }

    private void savePersonalExpense(double amount, String desc) {
        ExpenseCreate req = new ExpenseCreate(amount, desc, pickedLat, pickedLng, session.getUserId());
        RetrofitClient.getService().addPersonalExpense(req).enqueue(new Callback<GenericResponse>() {
            @Override public void onResponse(Call<GenericResponse> call, Response<GenericResponse> resp) {
                if (resp.isSuccessful()) { toast("Expense added!"); finish(); }
                else { resetSave(); toast("Failed to add expense"); }
            }
            @Override public void onFailure(Call<GenericResponse> c, Throwable t) { resetSave(); toast("Network error"); }
        });
    }

    private void saveGroupExpense(double amount, String desc) {
        int checkedId = rgSplitType.getCheckedRadioButtonId();
        String splitType = "equal";
        if (checkedId == R.id.rbPercentage) splitType = "percentage";
        else if (checkedId == R.id.rbDutch)  splitType = "dutch";

        List<MemberSplit> splits = null;
        if (!splitType.equals("equal")) {
            splits = collectSplits();
            if (splits == null) return; // validation failed
        }

        final String finalSplitType = splitType;
        final List<MemberSplit> finalSplits = splits;

        // Build member specs for find-or-create: only non-payer members
        List<MemberSpec> specs = new ArrayList<>();
        for (MemberEntry m : members)
            specs.add(new MemberSpec(m.userId, m.guestName));

        FindOrCreateGroupRequest gcReq = new FindOrCreateGroupRequest(session.getUserId(), specs, null);
        RetrofitClient.getService().findOrCreateGroup(gcReq)
                .enqueue(new Callback<FindOrCreateGroupResponse>() {
            @Override public void onResponse(Call<FindOrCreateGroupResponse> call,
                                              Response<FindOrCreateGroupResponse> resp) {
                if (!resp.isSuccessful() || resp.body() == null) { resetSave(); toast("Failed to setup group"); return; }
                int groupId = resp.body().groupId;
                String groupName = resp.body().name;
                boolean created = resp.body().created;

                GroupExpenseCreate req = new GroupExpenseCreate(
                        groupId, amount, desc, pickedLat, pickedLng,
                        session.getUserId(), finalSplitType, finalSplits);

                RetrofitClient.getService().addGroupExpense(req)
                        .enqueue(new Callback<GroupExpenseResponse>() {
                    @Override public void onResponse(Call<GroupExpenseResponse> call,
                                                      Response<GroupExpenseResponse> resp2) {
                        if (resp2.isSuccessful()) {
                            toast("Split in " + (created ? "new" : "existing") + " group \"" + groupName + "\"! ✓");
                            finish();
                        } else {
                            resetSave();
                            toast("Failed: check split values");
                        }
                    }
                    @Override public void onFailure(Call<GroupExpenseResponse> c, Throwable t) { resetSave(); toast("Network error"); }
                });
            }
            @Override public void onFailure(Call<FindOrCreateGroupResponse> c, Throwable t) { resetSave(); toast("Network error"); }
        });
    }

    /**
     * Collects split rows. Row order = payer first, then members (matching rebuildSplitRows).
     * Returns null if validation fails.
     */
    private List<MemberSplit> collectSplits() {
        boolean isPct = (rgSplitType.getCheckedRadioButtonId() == R.id.rbPercentage);
        List<MemberSplit> splits = new ArrayList<>();
        double total = 0;

        for (int i = 0; i < llSplitRows.getChildCount(); i++) {
            View row = llSplitRows.getChildAt(i);
            EditText et = row.findViewById(R.id.etSplitValue);
            if (et == null) continue;

            String tag = (String) et.getTag(); // "userId|guestName"
            String[] parts = tag.split("\\|", 2);
            Integer userId = null;
            String guestName = null;
            try { int uid = Integer.parseInt(parts[0]); if (uid > 0) userId = uid; }
            catch (NumberFormatException e) { /* guest */ }
            if (parts.length > 1 && !parts[1].isEmpty()) guestName = parts[1];

            double val = parseOr0(et.getText().toString());
            total += val;
            splits.add(new MemberSplit(userId, guestName, val));
        }

        // Validate
        if (isPct && Math.abs(total - 100) > 0.1) {
            toast(String.format(Locale.getDefault(), "Percentages must sum to 100. Got %.1f%%", total));
            resetSave();
            return null;
        }
        double amount = getAmountInput();
        if (!isPct && amount > 0 && Math.abs(total - amount) > 0.5) {
            toast(String.format(Locale.getDefault(), "Amounts must sum to ₹%.2f. Got ₹%.2f", amount, total));
            resetSave();
            return null;
        }
        return splits;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void resetSave() {
        btnSave.setEnabled(true);
        btnSave.setText("Save Expense");
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private double parseOr0(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; }
    }

    private int parseIntOr1(String s) {
        try { int v = Integer.parseInt(s.trim()); return v > 0 ? v : 1; } catch (Exception e) { return 1; }
    }

    private String fmt(double v) {
        return String.format(Locale.getDefault(), "₹%.2f", v);
    }

    // ─── Inner data classes ───────────────────────────────────────────────────

    static class MemberEntry {
        Integer userId;
        String guestName;
        String displayName;
        boolean isGuest;
        MemberEntry(Integer userId, String guestName, String displayName, boolean isGuest) {
            this.userId = userId; this.guestName = guestName;
            this.displayName = displayName; this.isGuest = isGuest;
        }
    }

    static class BillMember {
        int userId;
        String guestName;
        String displayName;
        boolean isGuest;
        BillMember(int userId, String guestName, String displayName, boolean isGuest) {
            this.userId = userId; this.guestName = guestName;
            this.displayName = displayName; this.isGuest = isGuest;
        }
    }

    static class BillItem {
        String name;
        int qty;
        double price;
        List<String> assignedKeys; // billKey strings
        BillItem(String name, int qty, double price, List<String> assignedKeys) {
            this.name = name; this.qty = qty; this.price = price;
            this.assignedKeys = new ArrayList<>(assignedKeys);
        }
    }
}
