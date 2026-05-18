package com.splitr.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.splitr.app.R;
import com.splitr.app.adapters.LocationExpenseAdapter;
import com.splitr.app.api.RetrofitClient;
import com.splitr.app.models.LocationExpense;
import com.splitr.app.utils.ExpenseClusterer;
import com.splitr.app.utils.MarkerClusterGroup;
import com.splitr.app.utils.SessionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private GoogleMap gMap;
    private FusedLocationProviderClient fusedLocation;
    private SessionManager session;

    private EditText etPlaceSearch;
    private Button btnClearSearch;
    private FloatingActionButton fabAddExpense, fabMyLocation;
    private BottomNavigationView bottomNav;

    // For tapped-pin add expense
    private LatLng pendingLat = null;
    private Marker pendingMarker = null;

    // Current user location
    private LatLng currentLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        session      = new SessionManager(this);
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        etPlaceSearch  = findViewById(R.id.etPlaceSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        fabAddExpense  = findViewById(R.id.fabAddExpense);
        fabMyLocation  = findViewById(R.id.fabMyLocation);
        bottomNav      = findViewById(R.id.bottomNav);

        SupportMapFragment mapFrag = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFrag != null) mapFrag.getMapAsync(this);

        setupSearchBar();
        setupFabs();
        setupBottomNav();
    }

    // ─── Map ready ───────────────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        // Dark style
        try {
            gMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
        } catch (Exception ignored) {}

        gMap.getUiSettings().setMyLocationButtonEnabled(false);
        gMap.getUiSettings().setZoomControlsEnabled(false);

        // Tap on map → set pending location for new expense
        gMap.setOnMapClickListener(latLng -> {
            if (pendingMarker != null) pendingMarker.remove();
            pendingLat = latLng;
            pendingMarker = gMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("New expense here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            Toast.makeText(this, "Tap ＋ to add expense here", Toast.LENGTH_SHORT).show();
        });

        requestLocationAndLoad();
    }

    private void requestLocationAndLoad() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }
        enableMyLocation();
        loadExpenses();
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        gMap.setMyLocationEnabled(true);

        fusedLocation.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 14f));
            }
        });
    }

    // ─── Load and render expenses ─────────────────────────────────────────────

    private void loadExpenses() {
        RetrofitClient.getService()
                .getLocations(session.getUserId())
                .enqueue(new Callback<List<LocationExpense>>() {
                    @Override
                    public void onResponse(Call<List<LocationExpense>> call,
                                           Response<List<LocationExpense>> resp) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            renderExpenses(resp.body());
                        }
                    }
                    @Override
                    public void onFailure(Call<List<LocationExpense>> call, Throwable t) {
                        Toast.makeText(MapActivity.this, "Failed to load map data",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderExpenses(List<LocationExpense> expenses) {
        if (gMap == null) return;
        gMap.clear();
        if (pendingMarker != null) pendingMarker = null;

        // Re-add user blue dot (cleared by gMap.clear())
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            gMap.setMyLocationEnabled(true);
        }

        // Draw heat circle around current location
        if (currentLocation != null && !expenses.isEmpty()) {
            drawHeatCircle(expenses);
        }

        // Cluster nearby expenses (~100m radius)
        List<MarkerClusterGroup> clusters = ExpenseClusterer.cluster(expenses);

        for (MarkerClusterGroup cluster : clusters) {
            LatLng pos = new LatLng(cluster.centerLat, cluster.centerLng);
            BitmapDescriptor icon = makeMarkerIcon(cluster);

            Marker m = gMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .icon(icon)
                    .anchor(0.5f, 0.5f));
            if (m != null) m.setTag(cluster);
        }

        gMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() instanceof MarkerClusterGroup) {
                showClusterSheet((MarkerClusterGroup) marker.getTag());
            }
            return true;
        });
    }

    /**
     * Draws a translucent heat circle around the user's current location,
     * sized to roughly encompass the visible expense area.
     * Shows zoomed-out heat even when individual markers are clustered.
     */
    private void drawHeatCircle(List<LocationExpense> expenses) {
        // Calculate user's total spend in this area
        double myTotal = 0, grandTotal = 0;
        for (LocationExpense e : expenses) {
            grandTotal += e.amount;
            myTotal    += e.myAmount;
        }
        double ratio = grandTotal > 0 ? myTotal / grandTotal : 0;

        // Color based on spend ratio
        int color;
        if (ratio <= 0)       color = Color.argb(40, 59, 130, 246);   // blue
        else if (ratio < 0.33) color = Color.argb(50, 239, 68, 68);   // red
        else if (ratio < 0.66) color = Color.argb(50, 249, 115, 22);  // orange
        else                   color = Color.argb(50, 168, 85, 247);  // purple

        int strokeColor;
        if (ratio <= 0)       strokeColor = Color.argb(120, 59, 130, 246);
        else if (ratio < 0.33) strokeColor = Color.argb(120, 239, 68, 68);
        else if (ratio < 0.66) strokeColor = Color.argb(120, 249, 115, 22);
        else                   strokeColor = Color.argb(120, 168, 85, 247);

        gMap.addCircle(new CircleOptions()
                .center(currentLocation)
                .radius(500)   // 500m radius heat zone
                .fillColor(color)
                .strokeColor(strokeColor)
                .strokeWidth(2f));
    }

    private BitmapDescriptor makeMarkerIcon(MarkerClusterGroup cluster) {
        int size = cluster.expenses.size() > 1 ? 120 : 90;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Shadow
        paint.setColor(Color.argb(60, 0, 0, 0));
        canvas.drawCircle(size / 2f + 3, size / 2f + 3, size / 2f - 6, paint);

        // Fill based on my spend ratio
        double ratio = cluster.totalAmount > 0
                ? cluster.myAmount / cluster.totalAmount : 0;

        if (cluster.myAmount <= 0)      paint.setColor(Color.rgb(59, 130, 246));   // blue
        else if (ratio < 0.33)          paint.setColor(Color.rgb(239, 68, 68));    // red
        else if (ratio < 0.66)          paint.setColor(Color.rgb(249, 115, 22));   // orange
        else                            paint.setColor(Color.rgb(168, 85, 247));   // purple

        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6, paint);

        // White border
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(3f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6, paint);
        paint.setStyle(Paint.Style.FILL);

        // Label
        String label = cluster.expenses.size() > 1
                ? "₹" + formatAmount(cluster.totalAmount)
                : "₹" + formatAmount(cluster.totalAmount);
        paint.setColor(Color.WHITE);
        paint.setTextSize(size > 100 ? 22f : 18f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        float textY = size / 2f - ((paint.descent() + paint.ascent()) / 2f);
        canvas.drawText(label, size / 2f, textY, paint);

        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    private String formatAmount(double amount) {
        if (amount >= 100000) return String.format(Locale.getDefault(), "%.0fL", amount / 100000);
        if (amount >= 1000)   return String.format(Locale.getDefault(), "%.0fK", amount / 1000);
        return String.format(Locale.getDefault(), "%.0f", amount);
    }

    // ─── Cluster bottom sheet ─────────────────────────────────────────────────

    private void showClusterSheet(MarkerClusterGroup cluster) {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View v = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_cluster, null);
        sheet.setContentView(v);

        TextView tvTotal  = v.findViewById(R.id.tvClusterTotal);
        TextView tvMine = v.findViewById(R.id.tvClusterTotal); // use existing view
        RecyclerView rv   = v.findViewById(R.id.rvClusterExpenses);

        tvTotal.setText("Total: ₹" + String.format(Locale.getDefault(), "%.2f", cluster.totalAmount));
        tvMine.setText("Your share: ₹" + String.format(Locale.getDefault(), "%.2f", cluster.myAmount));

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new LocationExpenseAdapter(cluster.expenses, session.getUserId()));

        sheet.show();
    }

    // ─── Place Search ─────────────────────────────────────────────────────────

    private void setupSearchBar() {
        etPlaceSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etPlaceSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                searchPlace(etPlaceSearch.getText().toString().trim());
                hideKeyboard();
                return true;
            }
            return false;
        });

        btnClearSearch.setOnClickListener(v -> {
            etPlaceSearch.setText("");
            btnClearSearch.setVisibility(View.GONE);
        });
    }

    private void searchPlace(String query) {
        if (query.isEmpty()) return;
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocationName(query, 1);
                if (results != null && !results.isEmpty()) {
                    Address addr = results.get(0);
                    LatLng target = new LatLng(addr.getLatitude(), addr.getLongitude());
                    runOnUiThread(() -> {
                        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15f));
                        // Drop a temporary search pin
                        gMap.addMarker(new MarkerOptions()
                                .position(target)
                                .title(addr.getFeatureName() != null
                                        ? addr.getFeatureName() : query)
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_CYAN)));
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this,
                            "Place not found", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Search failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etPlaceSearch.getWindowToken(), 0);
    }

    // ─── FABs ────────────────────────────────────────────────────────────────

    private void setupFabs() {
        fabAddExpense.setOnClickListener(v -> {
            if (pendingLat != null) {
                AddExpenseActivity.launch(this, pendingLat.latitude, pendingLat.longitude);
            } else if (currentLocation != null) {
                AddExpenseActivity.launch(this,
                        currentLocation.latitude, currentLocation.longitude);
            } else {
                AddExpenseActivity.launch(this, null, null);
            }
        });

        fabMyLocation.setOnClickListener(v -> {
            if (currentLocation != null) {
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 14f));
            } else {
                requestLocationAndLoad();
            }
        });
    }

    // ─── Permissions ─────────────────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
            loadExpenses();
        }
    }

    // ─── Bottom Nav ──────────────────────────────────────────────────────────

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navDashboard) {
                startActivity(new Intent(this, DashboardActivity.class)); finish(); return true;
            } else if (id == R.id.navMap) return true;
            else if (id == R.id.navGroups) {
                startActivity(new Intent(this, GroupActivity.class)); finish(); return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.navMap);
    }
}
