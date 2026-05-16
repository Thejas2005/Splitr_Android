package com.splitr.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
import com.google.android.gms.maps.model.LatLng;
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOC_PERM_REQ = 1001;

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocation;
    private SessionManager session;

    // marker → cluster mapping for click handling
    private final Map<Marker, MarkerClusterGroup> markerClusterMap = new HashMap<>();

    private FloatingActionButton fabAdd, fabMyLocation;
    private BottomNavigationView bottomNav;

    // State for location selection mode (when adding an expense)
    private boolean selectingLocation = false;
    private TextView tvSelectHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        session = new SessionManager(this);
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        fabAdd        = findViewById(R.id.fabAdd);
        fabMyLocation = findViewById(R.id.fabMyLocation);
        bottomNav     = findViewById(R.id.bottomNav);
        tvSelectHint  = findViewById(R.id.tvSelectHint);

        SupportMapFragment mapFrag = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFrag != null) mapFrag.getMapAsync(this);

        setupBottomNav();
        setupFabs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) loadAndRenderExpenses();
    }

    // ─── MAP READY ────────────────────────────────────────────────────────────

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMapStyle(
                com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(
                        this, R.raw.map_style_dark));
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);

        enableMyLocation();
        loadAndRenderExpenses();

        map.setOnMarkerClickListener(marker -> {
            MarkerClusterGroup cluster = markerClusterMap.get(marker);
            if (cluster != null) showClusterBottomSheet(cluster);
            return true;
        });

        // Location-select mode: tap map → open add expense with coords
        map.setOnMapClickListener(latLng -> {
            if (selectingLocation) {
                selectingLocation = false;
                tvSelectHint.setVisibility(View.GONE);
                AddExpenseActivity.launch(MapActivity.this, latLng.latitude, latLng.longitude);
            }
        });
    }

    // ─── LOAD & RENDER EXPENSES ───────────────────────────────────────────────

    private void loadAndRenderExpenses() {
        int uid = session.getUserId();
        RetrofitClient.getService().getLocations(uid)
                .enqueue(new Callback<List<LocationExpense>>() {
                    @Override
                    public void onResponse(Call<List<LocationExpense>> call,
                                           Response<List<LocationExpense>> resp) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            renderClusters(resp.body());
                        }
                    }
                    @Override
                    public void onFailure(Call<List<LocationExpense>> call, Throwable t) {
                        Toast.makeText(MapActivity.this, "Failed to load map data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderClusters(List<LocationExpense> expenses) {
        map.clear();
        markerClusterMap.clear();

        List<MarkerClusterGroup> clusters = ExpenseClusterer.cluster(expenses);

        for (MarkerClusterGroup cluster : clusters) {
            LatLng pos = new LatLng(cluster.centerLat, cluster.centerLng);
            String label = cluster.expenses.size() > 1
                    ? "₹" + formatAmount(cluster.totalAmount) + " ×" + cluster.expenses.size()
                    : "₹" + formatAmount(cluster.totalAmount);

            BitmapDescriptor icon = makeMarkerIcon(cluster);

            Marker marker = map.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(label)
                    .icon(icon)
                    .anchor(0.5f, 0.5f));

            if (marker != null) markerClusterMap.put(marker, cluster);
        }
    }

    /**
     * Creates a circular bitmap marker. Color = heat of user spend ratio.
     *
     * Ratio 0    → blue  (no user expense)
     * Ratio 0-33 → red   (low)
     * Ratio 33-66→ orange(medium)
     * Ratio 66+  → purple(high)
     */
    private BitmapDescriptor makeMarkerIcon(MarkerClusterGroup cluster) {
        double ratio = cluster.heatRatio();
        int bgColor;
        if (cluster.myAmount == 0) {
            bgColor = Color.parseColor("#3B82F6"); // blue
        } else if (ratio < 0.33) {
            bgColor = Color.parseColor("#EF4444"); // red
        } else if (ratio < 0.66) {
            bgColor = Color.parseColor("#F97316"); // orange
        } else {
            bgColor = Color.parseColor("#A855F7"); // purple
        }

        int size = cluster.expenses.size() > 1 ? 120 : 90;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // Shadow
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.argb(60, 0, 0, 0));
        shadowPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f + 3, size / 2f + 3, size / 2f - 8, shadowPaint);

        // Main circle
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(bgColor);
        circlePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8, circlePaint);

        // Border
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 10, borderPaint);

        // Label
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(cluster.expenses.size() > 1 ? 22 : 26);
        textPaint.setTextAlign(Paint.Align.CENTER);
        String label = "₹" + formatAmount(cluster.totalAmount);
        canvas.drawText(label, size / 2f, size / 2f + 9, textPaint);

        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    // ─── BOTTOM SHEET ─────────────────────────────────────────────────────────

    private void showClusterBottomSheet(MarkerClusterGroup cluster) {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_cluster, null);
        sheet.setContentView(view);

        TextView tvTotal    = view.findViewById(R.id.tvTotal);
        TextView tvMySpend  = view.findViewById(R.id.tvMySpend);
        TextView tvCount    = view.findViewById(R.id.tvCount);
        RecyclerView rv     = view.findViewById(R.id.rvClusterExpenses);
        Button btnAddHere   = view.findViewById(R.id.btnAddHere);

        tvTotal.setText("₹" + String.format(Locale.getDefault(), "%.2f", cluster.totalAmount));
        tvMySpend.setText("Your spend: ₹" + String.format(Locale.getDefault(), "%.2f", cluster.myAmount));
        tvCount.setText(cluster.expenses.size() + " expense" + (cluster.expenses.size() != 1 ? "s" : ""));

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new LocationExpenseAdapter(cluster.expenses, session.getUserId()));

        btnAddHere.setOnClickListener(v -> {
            sheet.dismiss();
            AddExpenseActivity.launch(this, cluster.centerLat, cluster.centerLng);
        });

        sheet.show();
    }

    // ─── FABs ─────────────────────────────────────────────────────────────────

    private void setupFabs() {
        fabAdd.setOnClickListener(v -> {
            // Enter location-select mode
            selectingLocation = true;
            tvSelectHint.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Tap the map to pin your expense location", Toast.LENGTH_SHORT).show();
        });

        fabMyLocation.setOnClickListener(v -> goToMyLocation());
    }

    private void goToMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOC_PERM_REQ);
            return;
        }
        fusedLocation.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
            }
        });
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            goToMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOC_PERM_REQ);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOC_PERM_REQ && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        }
    }

    // ─── BOTTOM NAV ───────────────────────────────────────────────────────────

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navDashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.navMap) {
                return true; // already here
            } else if (id == R.id.navGroups) {
                startActivity(new Intent(this, GroupActivity.class));
                finish();
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.navMap);
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private String formatAmount(double amount) {
        if (amount >= 1000) return String.format(Locale.getDefault(), "%.1fk", amount / 1000);
        return String.format(Locale.getDefault(), "%.0f", amount);
    }
}
