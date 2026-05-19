package com.splitr.app.utils;

import com.splitr.app.models.LocationExpense;
import java.util.ArrayList;
import java.util.List;

/**
 * Groups expenses into clusters whose radius grows as zoom level decreases,
 * so zooming out merges nearby pins into heatmap blobs.
 *
 * Zoom ~20 (street) → 80 m radius   (individual pins)
 * Zoom ~15 (district) → 500 m
 * Zoom ~12 (city)     → 2 km
 * Zoom ~10 (region)   → 8 km
 * Zoom  ~8 (country)  → 30 km
 */
public class ExpenseClusterer {

    /**
     * Returns cluster radius in metres for a given Google Maps zoom level.
     * Formula: radius doubles every ~2 zoom levels as you zoom out.
     */
    public static double clusterRadiusForZoom(float zoom) {
        // At zoom 20 → ~80 m; each step down doubles radius
        return 80.0 * Math.pow(2.0, 20 - zoom);
    }

    public static List<MarkerClusterGroup> cluster(List<LocationExpense> expenses, float zoom) {
        double radius = clusterRadiusForZoom(zoom);
        List<MarkerClusterGroup> groups = new ArrayList<>();

        for (LocationExpense e : expenses) {
            MarkerClusterGroup nearest = null;
            double minDist = Double.MAX_VALUE;

            for (MarkerClusterGroup g : groups) {
                double dist = haversineMeters(e.lat, e.lng, g.centerLat, g.centerLng);
                if (dist < radius && dist < minDist) {
                    minDist = dist;
                    nearest = g;
                }
            }

            if (nearest != null) {
                nearest.add(e);
            } else {
                MarkerClusterGroup ng = new MarkerClusterGroup(e.lat, e.lng);
                ng.add(e);
                groups.add(ng);
            }
        }
        return groups;
    }

    /** Kept for backward-compat; uses 100 m default. */
    public static List<MarkerClusterGroup> cluster(List<LocationExpense> expenses) {
        return cluster(expenses, 15f);
    }

    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}