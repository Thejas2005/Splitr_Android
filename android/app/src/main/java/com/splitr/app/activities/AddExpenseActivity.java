package com.splitr.app.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.splitr.app.R;
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
import com.splitr.app.utils.NetworkUtils;
import com.splitr.app.utils.SessionManager;
import com.splitr.app.utils.SyncManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddExpenseActivity extends AppCompatActivity {

    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LNG = "lng";
    private static final int LOCATION_PERMISSION = 2001;

    public static void launch(Context ctx, Double lat, Double lng) {
        Intent i = new Intent(ctx, AddExpenseActivity.class);
        if (lat != null) { i.putExtra(EXTRA_LAT, lat); i.putExtra(EXTRA_LNG, lng); }
        ctx.startActivity(i);
    }

    // ─── State ────────────────────────────────────────────────────────────────
    private SessionManager session;
    private FusedLocationProviderClient fusedLocation;
    private final Gson gson = new Gson();

    private Double pickedLat, pickedLng;
    private String pickedLocationName = null;

    private List<Map<String, Object>> allUsers = new ArrayList<>();
    private final List<MemberEntry> members    = new ArrayList<>();
    private final List<BillItem>    billItems  = new ArrayList<>();

    // ─── Views ────────────────────────────────────────────────────────────────
    private EditText          etAmount, etDescription, etLocationSearch;
    private AutoCompleteTextView etMemberInput;
    private TextView          tvLocation;
    private Button            btnClearLocation, btnUseCurrentLocation, btnPickOnMap;
    private LinearLayout      llMemberChips, llSplitSection, llSplitRows, llBillBtn;
    private RadioGroup        rgSplitType;
    private TextView          tvSplitValidation;
    private Button            btnSave, btnCancel, btnAddMember, btnBillSplitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense_v2);

        session       = new SessionManager(this);
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        // Location passed from map screen
        if (getIntent().hasExtra(EXTRA_LAT)) {
            pickedLat = getIntent().getDoubleExtra(EXTRA_LAT, 0);
            pickedLng = getIntent().getDoubleExtra(EXTRA_LNG, 0);
        }

        bindViews();
        loadAllUsers();
        setupLocationSection();
        setupMemberInput();
        setupSplitTypeToggle();
        setupButtons();
    }

    // ─── Bind ─────────────────────────────────────────────────────────────────

    private void bindViews() {
        etAmount             = findViewById(R.id.etAmount);
        etDescription        = findViewById(R.id.etDescription);
        etLocationSearch     = findViewById(R.id.etLocationSearch);
        tvLocation           = findViewById(R.id.tvLocation);
        btnClearLocation     = findViewById(R.id.btnClearLocation);
        btnUseCurrentLocation= findViewById(R.id.btnUseCurrentLocation);
        btnPickOnMap         = findViewById(R.id.btnPickOnMap);
        tvSplitValidation    = findViewById(R.id.tvSplitValidation);
        etMemberInput        = findViewById(R.id.etMemberInput);
        llMemberChips        = findViewById(R.id.llMemberChips);
        llSplitSection       = findViewById(R.id.llSplitSection);
        llSplitRows          = findViewById(R.id.llSplitRows);
        llBillBtn            = findViewById(R.id.llBillBtn);
        rgSplitType          = findViewById(R.id.rgSplitType);
        btnSave              = findViewById(R.id.btnSave);
        btnCancel            = findViewById(R.id.btnCancel);
        btnAddMember         = findViewById(R.id.btnAddMember);
        btnBillSplitter      = findViewById(R.id.btnBillSplitter);
    }

    // ─── Location section ─────────────────────────────────────────────────────

    private void setupLocationSection() {
        updateLocationDisplay();

        // If launched from map with a location, try reverse geocode immediately
        if (pickedLat != null) reverseGeocode(pickedLat, pickedLng);

        // Search bar typing
        etLocationSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                btnClearLocation.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Search on keyboard "Search" / Enter
        etLocationSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                searchLocation(etLocationSearch.getText().toString().trim());
                hideKeyboard(etLocationSearch);
                return true;
            }
            return false;
        });

        btnClearLocation.setOnClickListener(v -> {
            etLocationSearch.setText("");
            pickedLat = null; pickedLng = null; pickedLocationName = null;
            updateLocationDisplay();
        });

        // Current location button
        btnUseCurrentLocation.setOnClickListener(v -> requestCurrentLocation());

        // Pick on map — goes back to map screen, location comes back via launch()
        btnPickOnMap.setOnClickListener(v -> {
            startActivity(new Intent(this, MapActivity.class));
            // MapActivity FAB will call AddExpenseActivity.launch() with coords
        });
    }

    private void updateLocationDisplay() {
        if (pickedLat != null) {
            String display = pickedLocationName != null
                    ? "📍 " + pickedLocationName
                    : String.format(Locale.getDefault(), "📍 %.5f, %.5f", pickedLat, pickedLng);
            tvLocation.setText(display);
            tvLocation.setTextColor(getColor(R.color.blue_bright));
        } else {
            tvLocation.setText("No location selected");
            tvLocation.setTextColor(getColor(R.color.muted));
        }
    }

    private void searchLocation(String query) {
        if (query.isEmpty()) return;
        new Thread(() -> {
            try {
                Geocoder geo = new Geocoder(this, Locale.getDefault());
                List<Address> results = geo.getFromLocationName(query, 1);
                if (results != null && !results.isEmpty()) {
                    Address addr = results.get(0);
                    pickedLat = addr.getLatitude();
                    pickedLng = addr.getLongitude();
                    pickedLocationName = addr.getFeatureName() != null
                            ? addr.getFeatureName() : query;
                    runOnUiThread(this::updateLocationDisplay);
                } else {
                    runOnUiThread(() -> toast("Place not found"));
                }
            } catch (IOException e) {
                runOnUiThread(() -> toast("Search failed"));
            }
        }).start();
    }

    private void reverseGeocode(double lat, double lng) {
        new Thread(() -> {
            try {
                Geocoder geo = new Geocoder(this, Locale.getDefault());
                List<Address> results = geo.getFromLocation(lat, lng, 1);
                if (results != null && !results.isEmpty()) {
                    Address addr = results.get(0);
                    pickedLocationName = addr.getFeatureName() != null
                            ? addr.getFeatureName()
                            : (addr.getThoroughfare() != null ? addr.getThoroughfare() : null);
                    runOnUiThread(this::updateLocationDisplay);
                }
            } catch (IOException ignored) {}
        }).start();
    }

    private void requestCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION);
            return;
        }
        btnUseCurrentLocation.setEnabled(false);
        btnUseCurrentLocation.setText("Getting location…");
        fusedLocation.getLastLocation().addOnSuccessListener(location -> {
            btnUseCurrentLocation.setEnabled(true);
            btnUseCurrentLocation.setText("📍 Use Current Location");
            if (location != null) {
                pickedLat = location.getLatitude();
                pickedLng = location.getLongitude();
                reverseGeocode(pickedLat, pickedLng);
                updateLocationDisplay();
                toast("Location set ✓");
            } else {
                toast("Could not get location — try again");
            }
        }).addOnFailureListener(e -> {
            btnUseCurrentLocation.setEnabled(true);
            btnUseCurrentLocation.setText("📍 Use Current Location");
            toast("Location error");
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestCurrentLocation();
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
        if (val.equalsIgnoreCase(session.getUsername())) {
            toast("You are always the payer!"); etMemberInput.setText(""); return;
        }
        for (MemberEntry m : members) {
            if (val.equalsIgnoreCase(m.displayName)) {
                toast("Already added"); etMemberInput.setText(""); return;
            }
        }
        MemberEntry entry = null;
        for (Map<String, Object> u : allUsers) {
            String uname = (String) u.get("username");
            if (uname != null && uname.equalsIgnoreCase(val)) {
                int uid = (int) ((double) u.get("id"));
                entry = new MemberEntry(uid, null, uname, false);
                break;
            }
        }
        if (entry == null) entry = new MemberEntry(null, val, val, true);
        members.add(entry);
        etMemberInput.setText("");
        renderMemberChips();
        updateSplitSection();
    }

    private void renderMemberChips() {
        llMemberChips.removeAllViews();
        for (int i = 0; i < members.size(); i++) {
            MemberEntry m = members.get(i);
            View chip = LayoutInflater.from(this)
                    .inflate(R.layout.item_member_chip, llMemberChips, false);
            TextView tvName    = chip.findViewById(R.id.tvChipName);
            Button   btnRemove = chip.findViewById(R.id.btnChipRemove);
            tvName.setText(m.displayName + (m.isGuest ? " (guest)" : ""));
            final int idx = i;
            btnRemove.setOnClickListener(v -> {
                members.remove(idx);
                renderMemberChips();
                updateSplitSection();
            });
            llMemberChips.addView(chip);
        }
    }

    // ─── Split section ────────────────────────────────────────────────────────

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

    private void rebuildSplitRows() {
        llSplitRows.removeAllViews();
        tvSplitValidation.setText("");
        int checkedId = rgSplitType.getCheckedRadioButtonId();
        if (checkedId == R.id.rbEqual) return;

        boolean isPct = (checkedId == R.id.rbPercentage);

        // Payer row first
        addSplitRow(session.getUserId(), null,
                session.getUsername() + " (you / payer)", isPct);
        // All member rows
        for (MemberEntry m : members) {
            addSplitRow(
                    m.userId != null ? m.userId : -1,
                    m.guestName,
                    m.displayName + (m.isGuest ? " (guest)" : ""),
                    isPct);
        }
        setupSplitValidationWatcher();
    }

    /**
     * Creates one split input row.
     * KEY FIX: uses InputType.TYPE_CLASS_NUMBER | TYPE_NUMBER_FLAG_DECIMAL
     * so the decimal keyboard opens and every character is accepted,
     * fixing the "one char at a time" bug caused by inputType="numberDecimal"
     * combined with a TextWatcher that was recreating the view.
     */
    private void addSplitRow(int userId, String guestName, String label, boolean isPct) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_split_row_v2, llSplitRows, false);
        TextView tvName   = row.findViewById(R.id.tvMemberName);
        EditText etVal    = row.findViewById(R.id.etSplitValue);
        TextView tvSuffix = row.findViewById(R.id.tvSplitSuffix);

        tvName.setText(label);
        tvSuffix.setText(isPct ? "%" : "₹");

        // ── FIX: set input type programmatically, NOT via XML ──────────────
        // XML android:inputType="numberDecimal" combined with a TextWatcher
        // that calls notifyDataSetChanged / removeAllViews causes the cursor
        // to reset after every character.  Setting it here on the already-
        // inflated view avoids that race condition entirely.
        etVal.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etVal.setHint(isPct ? "0" : "0.00");

        // Tag: "userId|guestName" — read back in collectSplits()
        etVal.setTag(userId + "|" + (guestName != null ? guestName : ""));
        llSplitRows.addView(row);
    }

    private void setupSplitValidationWatcher() {
        // Attach a single watcher per field AFTER all rows are added
        // so we never rebuild rows inside onTextChanged (root cause of the bug)
        for (int i = 0; i < llSplitRows.getChildCount(); i++) {
            EditText et = llSplitRows.getChildAt(i).findViewById(R.id.etSplitValue);
            if (et == null) continue;
            et.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    // Only update the validation text — never touch llSplitRows here
                    validateSplitInputs();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void validateSplitInputs() {
        boolean isPct = (rgSplitType.getCheckedRadioButtonId() == R.id.rbPercentage);
        double total  = getSplitTotal();
        if (isPct) {
            double rem = 100.0 - total;
            tvSplitValidation.setText(Math.abs(rem) < 0.01
                    ? "✓ Percentages sum to 100%"
                    : String.format(Locale.getDefault(), "⚠ %.1f%% remaining", rem));
        } else {
            double amount = getAmountInput();
            if (amount > 0) {
                double diff = amount - total;
                tvSplitValidation.setText(Math.abs(diff) < 0.5
                        ? "✓ Amounts add up"
                        : String.format(Locale.getDefault(), "⚠ ₹%.2f %s",
                        Math.abs(diff), diff > 0 ? "remaining" : "over budget"));
            }
        }
    }

    private double getSplitTotal() {
        double total = 0;
        for (int i = 0; i < llSplitRows.getChildCount(); i++) {
            EditText et = llSplitRows.getChildAt(i).findViewById(R.id.etSplitValue);
            if (et != null) {
                try { total += Double.parseDouble(et.getText().toString()); }
                catch (NumberFormatException ignored) {}
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
        View v = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_bill_splitter, null);
        sheet.setContentView(v);

        List<BillMember> allBillMembers = getAllBillMembers();
        if (billItems.isEmpty()) {
            billItems.add(new BillItem("", 1, 0, allBillKeys(allBillMembers)));
        }

        LinearLayout llItems        = v.findViewById(R.id.llBillItems);
        EditText     etTax          = v.findViewById(R.id.etBillTax);
        EditText     etService      = v.findViewById(R.id.etBillService);
        TextView     tvSubtotal     = v.findViewById(R.id.tvBillSubtotal);
        TextView     tvTaxLine      = v.findViewById(R.id.tvBillTaxLine);
        TextView     tvServiceLine  = v.findViewById(R.id.tvBillServiceLine);
        TextView     tvGrandTotal   = v.findViewById(R.id.tvBillGrandTotal);
        LinearLayout llPersonTotals = v.findViewById(R.id.llBillPersonTotals);
        Button       btnApply       = v.findViewById(R.id.btnApplyBill);
        Button       btnAddItem     = v.findViewById(R.id.btnAddBillItem);

        // ── FIX: bill tax/service inputs also need programmatic inputType ──
        etTax.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etService.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        Runnable[] render = {null};
        render[0] = () -> renderBillSheet(llItems, etTax, etService,
                tvSubtotal, tvTaxLine, tvServiceLine, tvGrandTotal,
                llPersonTotals, allBillMembers, render);

        render[0].run();

        btnAddItem.setOnClickListener(vv -> {
            billItems.add(new BillItem("", 1, 0, allBillKeys(allBillMembers)));
            render[0].run();
        });

        // Tax / service re-render only on focus lost to avoid one-char bug
        etTax.setOnFocusChangeListener((vv, hasFocus) -> { if (!hasFocus) render[0].run(); });
        etService.setOnFocusChangeListener((vv, hasFocus) -> { if (!hasFocus) render[0].run(); });

        btnApply.setOnClickListener(vv -> {
            applyBillToDutch(allBillMembers, etTax, etService);
            sheet.dismiss();
        });

        v.findViewById(R.id.btnCancelBill).setOnClickListener(vv -> sheet.dismiss());
        sheet.show();
    }

    private List<BillMember> getAllBillMembers() {
        List<BillMember> list = new ArrayList<>();
        list.add(new BillMember(session.getUserId(), null,
                session.getUsername() + " ★", false));
        for (MemberEntry m : members)
            list.add(new BillMember(
                    m.userId != null ? m.userId : -1,
                    m.guestName,
                    m.displayName + (m.isGuest ? " (guest)" : ""),
                    m.isGuest));
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
                                 TextView tvSubtotal, TextView tvTaxLine,
                                 TextView tvServiceLine, TextView tvGrandTotal,
                                 LinearLayout llPersonTotals,
                                 List<BillMember> allBillMembers, Runnable[] render) {
        double tax      = parseOr0(etTax.getText().toString());
        double service  = parseOr0(etService.getText().toString());
        double subtotal = 0;
        for (BillItem it : billItems) subtotal += it.qty * it.price;
        double taxAmt   = subtotal * tax / 100;
        double svcAmt   = subtotal * service / 100;
        double grand    = subtotal + taxAmt + svcAmt;
        double factor   = grand / (subtotal > 0 ? subtotal : 1);

        // Per-person amounts
        double[] personAmts = new double[allBillMembers.size()];
        for (BillItem it : billItems) {
            int assigned = it.assignedKeys.size();
            if (assigned == 0) continue;
            double share = (it.qty * it.price) / assigned;
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
            View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_bill_row, llItems, false);
            EditText etName  = row.findViewById(R.id.etBillItemName);
            EditText etQty   = row.findViewById(R.id.etBillItemQty);
            EditText etPrice = row.findViewById(R.id.etBillItemPrice);
            Button   btnDel  = row.findViewById(R.id.btnDeleteBillItem);
            LinearLayout llAssign = row.findViewById(R.id.llBillAssign);

            // ── FIX: set inputType programmatically on bill item fields too ──
            etQty.setInputType(InputType.TYPE_CLASS_NUMBER);
            etPrice.setInputType(
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

            etName.setText(item.name);
            if (item.qty > 0) etQty.setText(String.valueOf(item.qty));
            if (item.price > 0)
                etPrice.setText(String.format(Locale.getDefault(), "%.2f", item.price));

            final int fIdx = idx;

            // Save on focus-lost instead of per-keystroke to avoid one-char bug
            etName.setOnFocusChangeListener((vv, hasFocus) -> {
                if (!hasFocus) billItems.get(fIdx).name = etName.getText().toString();
            });
            etQty.setOnFocusChangeListener((vv, hasFocus) -> {
                if (!hasFocus) {
                    billItems.get(fIdx).qty = parseIntOr1(etQty.getText().toString());
                    render[0].run();
                }
            });
            etPrice.setOnFocusChangeListener((vv, hasFocus) -> {
                if (!hasFocus) {
                    billItems.get(fIdx).price = parseOr0(etPrice.getText().toString());
                    render[0].run();
                }
            });

            btnDel.setOnClickListener(vv -> {
                billItems.remove(fIdx);
                render[0].run();
            });

            // Assign chips
            llAssign.removeAllViews();
            for (BillMember bm : allBillMembers) {
                String key = billKey(bm);
                Button chip = new Button(this);
                chip.setText(bm.displayName);
                chip.setTextSize(11);
                chip.setPadding(20, 6, 20, 6);
                boolean sel = item.assignedKeys.contains(key);
                chip.setBackgroundResource(sel
                        ? R.drawable.bg_button_primary
                        : R.drawable.bg_btn_secondary);
                chip.setTextColor(getColor(sel ? R.color.white : R.color.muted));
                chip.setStateListAnimator(null);
                chip.setOnClickListener(vv -> {
                    if (item.assignedKeys.contains(key)) {
                        if (item.assignedKeys.size() > 1) item.assignedKeys.remove(key);
                        else toast("At least one person must be assigned");
                    } else {
                        item.assignedKeys.add(key);
                    }
                    render[0].run();
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

        // Per-person totals
        llPersonTotals.removeAllViews();
        for (int pi = 0; pi < allBillMembers.size(); pi++) {
            View prow = LayoutInflater.from(this)
                    .inflate(R.layout.item_bill_person_total, llPersonTotals, false);
            ((TextView) prow.findViewById(R.id.tvBillPersonName))
                    .setText(allBillMembers.get(pi).displayName);
            ((TextView) prow.findViewById(R.id.tvBillPersonAmt))
                    .setText(fmt(personAmts[pi]));
            llPersonTotals.addView(prow);
        }
    }

    private void applyBillToDutch(List<BillMember> allBillMembers,
                                  EditText etTax, EditText etService) {
        // Switch to Dutch
        rgSplitType.check(R.id.rbDutch);
        rebuildSplitRows();

        double subtotal = 0;
        for (BillItem it : billItems) subtotal += it.qty * it.price;
        double tax    = parseOr0(etTax.getText().toString());
        double service= parseOr0(etService.getText().toString());
        double grand  = subtotal * (1 + tax / 100 + service / 100);
        double factor = grand / (subtotal > 0 ? subtotal : 1);

        double[] personAmts = new double[allBillMembers.size()];
        for (BillItem it : billItems) {
            int assigned = it.assignedKeys.size();
            if (assigned == 0) continue;
            for (int pi = 0; pi < allBillMembers.size(); pi++) {
                if (it.assignedKeys.contains(billKey(allBillMembers.get(pi))))
                    personAmts[pi] += (it.qty * it.price) / assigned;
            }
        }
        for (int pi = 0; pi < personAmts.length; pi++) personAmts[pi] *= factor;

        // Set total amount field
        etAmount.setText(String.format(Locale.getDefault(), "%.2f", grand));

        // Fill split rows — payer first, then members (matching rebuildSplitRows order)
        for (int i = 0; i < llSplitRows.getChildCount() && i < personAmts.length; i++) {
            EditText et = llSplitRows.getChildAt(i).findViewById(R.id.etSplitValue);
            if (et != null)
                et.setText(String.format(Locale.getDefault(), "%.2f", personAmts[i]));
        }

        validateSplitInputs();
        toast("Bill amounts applied! " + fmt(grand));
    }

    // ─── Save ─────────────────────────────────────────────────────────────────

    private void setupButtons() {
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void validateAndSave() {
        String amtStr = etAmount.getText().toString().trim();
        String desc   = etDescription.getText().toString().trim();
        if (amtStr.isEmpty()) { toast("Enter amount"); return; }
        double amount;
        try { amount = Double.parseDouble(amtStr); }
        catch (NumberFormatException e) { toast("Invalid amount"); return; }
        if (amount <= 0) { toast("Amount must be > 0"); return; }
        if (desc.isEmpty()) { toast("Enter description"); return; }

        btnSave.setEnabled(false);
        btnSave.setText("Saving…");

        if (members.isEmpty()) savePersonalExpense(amount, desc);
        else saveGroupExpense(amount, desc);
    }

    private void savePersonalExpense(double amount, String desc) {
        ExpenseCreate req = new ExpenseCreate(
                amount, desc, pickedLat, pickedLng, session.getUserId());

        if (NetworkUtils.isOnline(this)) {
            RetrofitClient.getService().addPersonalExpense(req)
                    .enqueue(new Callback<GenericResponse>() {
                        @Override public void onResponse(Call<GenericResponse> call,
                                                         Response<GenericResponse> resp) {
                            if (resp.isSuccessful()) { toast("Expense added! ✓"); finish(); }
                            else { queuePersonalOffline(amount, desc); toast("Queued offline ✓"); finish(); }
                        }
                        @Override public void onFailure(Call<GenericResponse> c, Throwable t) {
                            queuePersonalOffline(amount, desc);
                            toast("Offline — queued ✓"); finish();
                        }
                    });
        } else {
            queuePersonalOffline(amount, desc);
            toast("Offline — will sync when online ✓");
            finish();
        }
    }

    private void queuePersonalOffline(double amount, String desc) {
        new Thread(() -> {
            PendingExpense p = new PendingExpense();
            p.amount = amount; p.description = desc;
            p.latitude = pickedLat; p.longitude = pickedLng;
            p.userId = session.getUserId();
            p.status = "pending"; p.retryCount = 0;
            p.createdAt = System.currentTimeMillis();
            SplitRDatabase.get(AddExpenseActivity.this).pendingExpenseDao().insert(p);
            SyncManager.scheduleSync(AddExpenseActivity.this);
        }).start();
    }

    private void saveGroupExpense(double amount, String desc) {
        int checkedId  = rgSplitType.getCheckedRadioButtonId();
        String splitType = "equal";
        if (checkedId == R.id.rbPercentage) splitType = "percentage";
        else if (checkedId == R.id.rbDutch)  splitType = "dutch";

        List<MemberSplit> splits = splitType.equals("equal") ? null : collectSplits();
        if (splits == null && !splitType.equals("equal")) return;

        List<MemberSpec> specs = new ArrayList<>();
        for (MemberEntry m : members) specs.add(new MemberSpec(m.userId, m.guestName));

        final String       fType   = splitType;
        final List<MemberSplit> fSplits = splits;

        if (NetworkUtils.isOnline(this)) {
            RetrofitClient.getService()
                    .findOrCreateGroup(new FindOrCreateGroupRequest(
                            session.getUserId(), specs, null))
                    .enqueue(new Callback<FindOrCreateGroupResponse>() {
                        @Override public void onResponse(Call<FindOrCreateGroupResponse> call,
                                                         Response<FindOrCreateGroupResponse> resp) {
                            if (!resp.isSuccessful() || resp.body() == null) {
                                queueGroupOffline(amount, desc, fType, fSplits, specs);
                                toast("Queued offline ✓"); finish(); return;
                            }
                            int groupId = resp.body().groupId;
                            RetrofitClient.getService()
                                    .addGroupExpense(new GroupExpenseCreate(
                                            groupId, amount, desc, pickedLat, pickedLng,
                                            session.getUserId(), fType, fSplits))
                                    .enqueue(new Callback<GroupExpenseResponse>() {
                                        @Override public void onResponse(Call<GroupExpenseResponse> c,
                                                                         Response<GroupExpenseResponse> r) {
                                            if (r.isSuccessful()) { toast("Group expense added! ✓"); finish(); }
                                            else { queueGroupOffline(amount, desc, fType, fSplits, specs);
                                                toast("Queued offline ✓"); finish(); }
                                        }
                                        @Override public void onFailure(Call<GroupExpenseResponse> c, Throwable t) {
                                            queueGroupOffline(amount, desc, fType, fSplits, specs);
                                            toast("Offline — queued ✓"); finish();
                                        }
                                    });
                        }
                        @Override public void onFailure(Call<FindOrCreateGroupResponse> c, Throwable t) {
                            queueGroupOffline(amount, desc, fType, fSplits, specs);
                            toast("Offline — queued ✓"); finish();
                        }
                    });
        } else {
            queueGroupOffline(amount, desc, fType, fSplits, specs);
            toast("Offline — will sync when online ✓");
            finish();
        }
    }

    private void queueGroupOffline(double amount, String desc, String splitType,
                                   List<MemberSplit> splits, List<MemberSpec> specs) {
        new Thread(() -> {
            PendingExpense p = new PendingExpense();
            p.amount = amount; p.description = desc;
            p.latitude = pickedLat; p.longitude = pickedLng;
            p.userId = session.getUserId();
            p.splitType  = splitType;
            p.splitsJson = splits != null ? gson.toJson(splits) : null;
            p.membersJson= gson.toJson(specs);
            p.status = "pending"; p.retryCount = 0;
            p.createdAt = System.currentTimeMillis();
            SplitRDatabase.get(AddExpenseActivity.this).pendingExpenseDao().insert(p);
            SyncManager.scheduleSync(AddExpenseActivity.this);
        }).start();
    }

    private List<MemberSplit> collectSplits() {
        boolean isPct = (rgSplitType.getCheckedRadioButtonId() == R.id.rbPercentage);
        List<MemberSplit> splits = new ArrayList<>();
        double total = 0;

        for (int i = 0; i < llSplitRows.getChildCount(); i++) {
            View row  = llSplitRows.getChildAt(i);
            EditText et = row.findViewById(R.id.etSplitValue);
            if (et == null) continue;

            String tag    = (String) et.getTag();
            String[] parts= tag.split("\\|", 2);
            Integer userId = null;
            String guestName = null;
            try { int uid = Integer.parseInt(parts[0]); if (uid > 0) userId = uid; }
            catch (NumberFormatException ignored) {}
            if (parts.length > 1 && !parts[1].isEmpty()) guestName = parts[1];

            double val = parseOr0(et.getText().toString());
            total += val;
            splits.add(new MemberSplit(userId, guestName, val));
        }

        if (isPct && Math.abs(total - 100) > 0.1) {
            toast(String.format(Locale.getDefault(),
                    "Percentages must sum to 100. Got %.1f%%", total));
            resetSave(); return null;
        }
        double amount = getAmountInput();
        if (!isPct && amount > 0 && Math.abs(total - amount) > 0.5) {
            toast(String.format(Locale.getDefault(),
                    "Amounts must sum to ₹%.2f. Got ₹%.2f", amount, total));
            resetSave(); return null;
        }
        return splits;
    }

    private void resetSave() {
        btnSave.setEnabled(true);
        btnSave.setText("Save Expense");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private double parseOr0(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; }
    }

    private int parseIntOr1(String s) {
        try { int v = Integer.parseInt(s.trim()); return v > 0 ? v : 1; }
        catch (Exception e) { return 1; }
    }

    private String fmt(double v) {
        return String.format(Locale.getDefault(), "₹%.2f", v);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ─── Inner data classes ───────────────────────────────────────────────────

    static class MemberEntry {
        Integer userId; String guestName; String displayName; boolean isGuest;
        MemberEntry(Integer u, String g, String d, boolean guest) {
            userId = u; guestName = g; displayName = d; isGuest = guest;
        }
    }

    static class BillMember {
        int userId; String guestName; String displayName; boolean isGuest;
        BillMember(int u, String g, String d, boolean guest) {
            userId = u; guestName = g; displayName = d; isGuest = guest;
        }
    }

    static class BillItem {
        String name; int qty; double price; List<String> assignedKeys;
        BillItem(String n, int q, double p, List<String> keys) {
            name = n; qty = q; price = p;
            assignedKeys = new ArrayList<>(keys);
        }
    }
}
