package com.splitr.app.utils;

import com.splitr.app.models.LocationExpense;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a cluster of geographically nearby expenses (~100m radius).
 * Used by the map to aggregate markers and compute heat-coloring.
 */
public class MarkerClusterGroup {

    public double centerLat;
    public double centerLng;
    public List<LocationExpense> expenses = new ArrayList<>();

    // Computed totals
    public double totalAmount  = 0;
    public double myAmount     = 0;

    public MarkerClusterGroup(double lat, double lng) {
        this.centerLat = lat;
        this.centerLng = lng;
    }

    public void add(LocationExpense e) {
        expenses.add(e);
        totalAmount += e.amount;
        myAmount    += e.myAmount;

        // Recompute centroid
        centerLat = 0;
        centerLng = 0;
        for (LocationExpense ex : expenses) {
            centerLat += ex.lat;
            centerLng += ex.lng;
        }
        centerLat /= expenses.size();
        centerLng /= expenses.size();
    }

    /** Ratio of user spend vs total — drives marker heat color */
    public double heatRatio() {
        if (totalAmount == 0) return 0;
        return myAmount / totalAmount;
    }
}
