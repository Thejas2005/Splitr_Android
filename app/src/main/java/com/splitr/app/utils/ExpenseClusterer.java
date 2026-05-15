package com.splitr.app.utils;

import com.splitr.app.models.LocationExpense;
import java.util.ArrayList;
import java.util.List;

/**
 * Groups expenses within ~100 m of each other into MarkerClusterGroups.
 * Uses a simple greedy sweep (O(n²)) — acceptable for typical expense counts.
 */
public class ExpenseClusterer {

    private static final double CLUSTER_RADIUS_M = 100.0;

    public static List<MarkerClusterGroup> cluster(List<LocationExpense> expenses) {
        List<MarkerClusterGroup> groups = new ArrayList<>();

        for (LocationExpense e : expenses) {
            MarkerClusterGroup nearest = null;
            double minDist = Double.MAX_VALUE;

            for (MarkerClusterGroup g : groups) {
                double dist = haversineMeters(e.lat, e.lng, g.centerLat, g.centerLng);
                if (dist < CLUSTER_RADIUS_M && dist < minDist) {
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

    /**
     * Haversine distance in metres between two lat/lng points.
     */
    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6_371_000; // Earth radius in metres
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
