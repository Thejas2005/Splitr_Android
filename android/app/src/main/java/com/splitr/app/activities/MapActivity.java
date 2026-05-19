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
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // Your existing Maps API key from AndroidManifest.xml
    private static final String PLACES_API_KEY = "AIzaSyBd3gQ7AzQpYQQDvpGYNuD-ux-pTJr1Qlo";

    // Below this zoom level all expenses merge into one growing bubble
    private static final float ZOOM_CLUSTER = 15f;

    private GoogleMap gMap;
    private FusedLocationProviderClient fusedLocation;
    private SessionManager session;

    private AutoCompleteTextView etPlaceSearch;
    private Button btnClearSearch;
    private FloatingActionButton fabAddExpense, fabMyLocation, fabZoomIn, fabZoomOut;
    private BottomNavigationView bottomNav;

    // Autocomplete
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private ArrayAdapter<String> suggestionAdapter;

    // Map from suggestion label → placeId for navigation after selection
    private final java.util.Map<String, String> placeIdMap = new java.util.LinkedHashMap<>();

    private final OkHttpClient httpClient = new OkHttpClient();

    // Pending pin
    private LatLng pendingLat = null;
    private Marker pendingMarker = null;

    // Search result marker (persists across zoom re-renders)
    private Marker searchMarker = null;
    // Opaque circle drawn over the blue dot when an expense cluster sits on current location
    private Circle locationCoverCircle = null;
    // Suppresses zoom re-render while a search navigation is animating
    private boolean suppressZoomRender = false;

    // Current location
    private LatLng currentLocation = null;

    // Cached expenses
    private List<LocationExpense> cachedExpenses = new ArrayList<>();
    private float lastRenderedZoom = -1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        session       = new SessionManager(this);
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        etPlaceSearch  = findViewById(R.id.etPlaceSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        fabAddExpense  = findViewById(R.id.fabAddExpense);
        fabMyLocation  = findViewById(R.id.fabMyLocation);
        fabZoomIn      = findViewById(R.id.fabZoomIn);
        fabZoomOut     = findViewById(R.id.fabZoomOut);
        bottomNav      = findViewById(R.id.bottomNav);

        suggestionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        etPlaceSearch.setAdapter(suggestionAdapter);
        etPlaceSearch.setThreshold(1); // show after 1 char

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

        try {
            gMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
        } catch (Exception ignored) {}

        gMap.getUiSettings().setMyLocationButtonEnabled(false);
        gMap.getUiSettings().setZoomControlsEnabled(false);
        gMap.getUiSettings().setZoomGesturesEnabled(true);
        gMap.getUiSettings().setScrollGesturesEnabled(true);
        gMap.getUiSettings().setTiltGesturesEnabled(true);
        gMap.getUiSettings().setRotateGesturesEnabled(true);

        gMap.setOnMapClickListener(latLng -> {
            if (pendingMarker != null) pendingMarker.remove();
            pendingLat = latLng;
            pendingMarker = gMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("New expense here")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            Toast.makeText(this, "Tap ＋ to add expense here", Toast.LENGTH_SHORT).show();
        });

        gMap.setOnCameraIdleListener(() -> {
            if (suppressZoomRender) {
                suppressZoomRender = false; // reset after the search animation settles
                lastRenderedZoom = gMap.getCameraPosition().zoom;
                return;
            }
            float zoom = gMap.getCameraPosition().zoom;
            if (Math.abs(zoom - lastRenderedZoom) > 0.5f && !cachedExpenses.isEmpty()) {
                lastRenderedZoom = zoom;
                renderExpenses(cachedExpenses);
            }
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

    // ─── Load & render ────────────────────────────────────────────────────────

    private void loadExpenses() {
        RetrofitClient.getService()
                .getLocations(session.getUserId())
                .enqueue(new Callback<List<LocationExpense>>() {
                    @Override
                    public void onResponse(Call<List<LocationExpense>> call,
                                           retrofit2.Response<List<LocationExpense>> resp) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            cachedExpenses = resp.body();
                            lastRenderedZoom = gMap != null ? gMap.getCameraPosition().zoom : 14f;
                            renderExpenses(cachedExpenses);
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
        // Save search marker state before clear
        LatLng searchPos = searchMarker != null ? searchMarker.getPosition() : null;
        String searchTitle = searchMarker != null ? searchMarker.getTitle() : null;
        gMap.clear();
        if (pendingMarker != null) pendingMarker = null;
        searchMarker = null;
        // Re-add search marker if one existed
        if (searchPos != null) {
            searchMarker = gMap.addMarker(new MarkerOptions()
                    .position(searchPos)
                    .title(searchTitle)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            gMap.setMyLocationEnabled(true);
        }

        float zoom = gMap.getCameraPosition().zoom;
        List<MarkerClusterGroup> clusters = ExpenseClusterer.cluster(expenses, zoom);

        for (MarkerClusterGroup cluster : clusters) {
            LatLng pos = new LatLng(cluster.centerLat, cluster.centerLng);

            BitmapDescriptor icon = makeMarkerIcon(cluster, zoom);
            // zIndex > 1 renders above the native My Location blue dot
            Marker m = gMap.addMarker(new MarkerOptions()
                    .position(pos).icon(icon).anchor(0.5f, 0.5f).zIndex(10f));
            if (m != null) m.setTag(cluster);
        }

        // Draw opaque cover circle over the blue My Location dot
        // if the user's location coincides with an expense cluster.
        if (locationCoverCircle != null) {
            locationCoverCircle.remove();
            locationCoverCircle = null;
        }
        if (currentLocation != null) {
            // Find the cluster closest to current location
            MarkerClusterGroup nearest = null;
            double minDist = Double.MAX_VALUE;
            for (MarkerClusterGroup cl : clusters) {
                double d = ExpenseClusterer.haversineMeters(
                        currentLocation.latitude, currentLocation.longitude,
                        cl.centerLat, cl.centerLng);
                if (d < minDist) { minDist = d; nearest = cl; }
            }
            // Cover the dot only if a cluster is within 80m (the dot's visual footprint)
            if (nearest != null && minDist < 80) {
                // Circle radius in metres — small enough to only cover the dot, not the bubble
                // Gets slightly larger at low zoom so the bleed around the bitmap marker is hidden
                double coverRadius = Math.max(8, Math.min(40, (ZOOM_CLUSTER - zoom) * 3 + 8));
                locationCoverCircle = gMap.addCircle(new CircleOptions()
                        .center(new LatLng(nearest.centerLat, nearest.centerLng))
                        .radius(coverRadius)
                        .fillColor(solidHeatColor(nearest.heatRatio(), nearest.myAmount))
                        .strokeWidth(0f)
                        .zIndex(100f)); // above everything including the blue dot layer
            }
        }

        gMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() instanceof MarkerClusterGroup) {
                showClusterSheet((MarkerClusterGroup) marker.getTag());
            }
            return true;
        });
    }


    private BitmapDescriptor makeMarkerIcon(MarkerClusterGroup cluster, float zoom) {
        int count = cluster.expenses.size();
        boolean isCluster = count > 1;

        // Single pin = 90px. Clusters grow log-scale with count,
        // plus a small zoom-out boost so they stay visible when merged.
        int base = isCluster
                ? Math.min(180, 90 + (int)(Math.log(count + 1) / Math.log(2) * 22))
                : 90;
        int zoomBoost = isCluster
                ? (int) Math.min(40, Math.max(0, (ZOOM_CLUSTER - zoom) * 3f))
                : 0;
        int size = base + zoomBoost;

        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Shadow
        paint.setColor(Color.argb(60, 0, 0, 0));
        canvas.drawCircle(size / 2f + 3, size / 2f + 3, size / 2f - 6, paint);

        // Fill colour based on spend ratio
        paint.setColor(solidHeatColor(cluster.heatRatio(), cluster.myAmount));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6, paint);

        // White border
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(3f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6, paint);
        paint.setStyle(Paint.Style.FILL);

        // Amount text — shift up if cluster to make room for count
        String label = "₹" + formatAmount(cluster.totalAmount);
        paint.setColor(Color.WHITE);
        paint.setTextSize(size > 130 ? 24f : size > 100 ? 20f : 16f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        float textY = size / 2f - ((paint.descent() + paint.ascent()) / 2f);
        if (isCluster) textY -= 10f;
        canvas.drawText(label, size / 2f, textY, paint);

        // Count below amount
        if (isCluster) {
            paint.setTextSize(size > 130 ? 14f : 12f);
            paint.setFakeBoldText(false);
            paint.setColor(Color.argb(210, 255, 255, 255));
            canvas.drawText(count + " expenses", size / 2f, textY + (size > 130 ? 20f : 17f), paint);
        }

        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    private int solidHeatColor(double ratio, double myAmount) {
        if (myAmount <= 0) return Color.rgb(59, 130, 246);
        if (ratio < 0.33)  return Color.rgb(239, 68, 68);
        if (ratio < 0.66)  return Color.rgb(249, 115, 22);
        return Color.rgb(168, 85, 247);
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

        TextView tvTotal   = v.findViewById(R.id.tvTotal);
        TextView tvMySpend = v.findViewById(R.id.tvMySpend);
        TextView tvCount   = v.findViewById(R.id.tvCount);
        RecyclerView rv    = v.findViewById(R.id.rvClusterExpenses);
        Button btnAddHere  = v.findViewById(R.id.btnAddHere);

        tvTotal.setText("₹" + String.format(Locale.getDefault(), "%.2f", cluster.totalAmount));
        tvMySpend.setText("Your spend: ₹" + String.format(Locale.getDefault(), "%.2f", cluster.myAmount));
        int n = cluster.expenses.size();
        tvCount.setText(n + " expense" + (n == 1 ? "" : "s"));

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new LocationExpenseAdapter(cluster.expenses, session.getUserId()));

        if (btnAddHere != null) {
            btnAddHere.setOnClickListener(vv -> {
                sheet.dismiss();
                AddExpenseActivity.launch(this, cluster.centerLat, cluster.centerLng);
            });
        }
        sheet.show();
    }

    // ─── Place Search — Google Places Autocomplete ────────────────────────────

    private void setupSearchBar() {
        etPlaceSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                String query = s.toString().trim();
                if (query.length() >= 1) {
                    // Debounce 300 ms — fast enough for typeahead feel
                    searchRunnable = () -> fetchAutocompleteSuggestions(query);
                    searchHandler.postDelayed(searchRunnable, 300);
                } else {
                    suggestionAdapter.clear();
                    suggestionAdapter.notifyDataSetChanged();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etPlaceSearch.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            String placeId  = placeIdMap.get(selected);
            etPlaceSearch.setText(selected);
            hideKeyboard();
            if (placeId != null) {
                navigateToPlaceId(placeId);
            } else {
                searchPlaceByName(selected);
            }
        });

        etPlaceSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = etPlaceSearch.getText().toString().trim();
                // If there's an exact match in our map use it, otherwise geocode
                String placeId = placeIdMap.get(query);
                if (placeId != null) navigateToPlaceId(placeId);
                else searchPlaceByName(query);
                hideKeyboard();
                return true;
            }
            return false;
        });

        btnClearSearch.setOnClickListener(v -> {
            etPlaceSearch.setText("");
            btnClearSearch.setVisibility(View.GONE);
            suggestionAdapter.clear();
            suggestionAdapter.notifyDataSetChanged();
            placeIdMap.clear();
        });
    }

    /**
     * Calls the Places Autocomplete API using the Maps API key already in the manifest.
     * Returns up to 5 suggestions as the user types.
     */
    private void fetchAutocompleteSuggestions(String input) {
        new Thread(() -> {
            try {
                // Bias results toward user's current location if available
                String locationBias = "";
                if (currentLocation != null) {
                    locationBias = "&location=" + currentLocation.latitude
                            + "," + currentLocation.longitude + "&radius=50000";
                }

                String url = "https://maps.googleapis.com/maps/api/place/autocomplete/json"
                        + "?input=" + URLEncoder.encode(input, "UTF-8")
                        + "&key=" + PLACES_API_KEY
                        + "&language=en"
                        + locationBias;

                Request request = new Request.Builder().url(url).build();
                try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) return;

                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONArray predictions = json.optJSONArray("predictions");
                    if (predictions == null) return;

                    List<String> labels = new ArrayList<>();
                    java.util.Map<String, String> newMap = new java.util.LinkedHashMap<>();

                    for (int i = 0; i < Math.min(predictions.length(), 5); i++) {
                        JSONObject pred = predictions.getJSONObject(i);
                        String description = pred.optString("description", "");
                        String placeId     = pred.optString("place_id", "");
                        if (!description.isEmpty()) {
                            labels.add(description);
                            if (!placeId.isEmpty()) newMap.put(description, placeId);
                        }
                    }

                    runOnUiThread(() -> {
                        placeIdMap.clear();
                        placeIdMap.putAll(newMap);
                        suggestionAdapter.clear();
                        suggestionAdapter.addAll(labels);
                        suggestionAdapter.notifyDataSetChanged();
                        if (!labels.isEmpty()) etPlaceSearch.showDropDown();
                    });
                }
            } catch (Exception e) {
                // Silently fail — user can still press search
            }
        }).start();
    }

    /**
     * Once user picks a suggestion we have its placeId.
     * Use Place Details API to get the exact lat/lng and navigate there.
     */
    private void navigateToPlaceId(String placeId) {
        new Thread(() -> {
            try {
                String url = "https://maps.googleapis.com/maps/api/place/details/json"
                        + "?place_id=" + placeId
                        + "&fields=geometry,name"
                        + "&key=" + PLACES_API_KEY;

                Request request = new Request.Builder().url(url).build();
                try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) return;

                    String body = response.body().string();
                    JSONObject json  = new JSONObject(body);
                    JSONObject result = json.optJSONObject("result");
                    if (result == null) return;

                    JSONObject location = result
                            .getJSONObject("geometry")
                            .getJSONObject("location");
                    double lat = location.getDouble("lat");
                    double lng = location.getDouble("lng");
                    String name = result.optString("name", "");

                    runOnUiThread(() -> {
                        LatLng target = new LatLng(lat, lng);
                        suppressZoomRender = true;
                        if (searchMarker != null) searchMarker.remove();
                        searchMarker = gMap.addMarker(new MarkerOptions()
                                .position(target)
                                .title(name.isEmpty() ? etPlaceSearch.getText().toString() : name)
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_RED)));
                        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 16f));
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Navigation failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    /** Fallback geocoder if no placeId available */
    private void searchPlaceByName(String query) {
        if (query.isEmpty()) return;
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocationName(query, 1);
                if (results != null && !results.isEmpty()) {
                    Address addr = results.get(0);
                    LatLng target = new LatLng(addr.getLatitude(), addr.getLongitude());
                    runOnUiThread(() -> {
                        suppressZoomRender = true;
                        if (searchMarker != null) searchMarker.remove();
                        searchMarker = gMap.addMarker(new MarkerOptions()
                                .position(target)
                                .title(addr.getFeatureName() != null ? addr.getFeatureName() : query)
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_RED)));
                        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15f));
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Place not found", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Search failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etPlaceSearch.getWindowToken(), 0);
    }

    // ─── FABs ────────────────────────────────────────────────────────────────

    private void setupFabs() {
        fabAddExpense.setOnClickListener(v -> {
            if (pendingLat != null) {
                AddExpenseActivity.launch(this, pendingLat.latitude, pendingLat.longitude);
            } else if (currentLocation != null) {
                AddExpenseActivity.launch(this, currentLocation.latitude, currentLocation.longitude);
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

        fabZoomIn.setOnClickListener(v -> {
            if (gMap != null) gMap.animateCamera(CameraUpdateFactory.zoomIn());
        });

        fabZoomOut.setOnClickListener(v -> {
            if (gMap != null) gMap.animateCamera(CameraUpdateFactory.zoomOut());
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