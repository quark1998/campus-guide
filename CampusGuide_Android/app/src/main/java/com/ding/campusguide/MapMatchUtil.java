package com.ding.campusguide;

import com.amap.api.maps.model.LatLng;
import java.util.List;
public class MapMatchUtil {
    public static LatLng snapToRoute(LatLng rawLocation, List<LatLng> route) {
        if (route == null || route.size() < 2) return rawLocation;
        double minDist = Double.MAX_VALUE;
        LatLng bestSnap = rawLocation;
        for (int i = 0; i < route.size() - 1; i++) {
            LatLng a = route.get(i);
            LatLng b = route.get(i + 1);
            LatLng projected = projectPointOnSegment(rawLocation, a, b);


            float dist = calculateDistanceMeters(rawLocation, projected);
            if (dist < minDist) {
                minDist = dist;
                bestSnap = projected;
            }
        }

        if (minDist > 25.0) return rawLocation;
        return bestSnap;
    }
    private static LatLng projectPointOnSegment(LatLng p, LatLng a, LatLng b) {
        double dx = b.longitude - a.longitude;
        double dy = b.latitude - a.latitude;
        if (dx == 0 && dy == 0) return a;
        double t = ((p.longitude - a.longitude) * dx + (p.latitude - a.latitude) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        return new LatLng(a.latitude + t * dy, a.longitude + t * dx);
    }

    public static float calculateDistanceMeters(LatLng p1, LatLng p2) {
        double earthRadius = 6371000.0;
        double dLat = Math.toRadians(p2.latitude - p1.latitude);
        double dLng = Math.toRadians(p2.longitude - p1.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(p1.latitude)) * Math.cos(Math.toRadians(p2.latitude)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return (float) (2 * earthRadius * Math.asin(Math.sqrt(a)));
    }
}