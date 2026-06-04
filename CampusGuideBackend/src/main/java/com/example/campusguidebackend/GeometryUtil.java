package com.example.campusguidebackend;
import java.util.List;
public class GeometryUtil {

    public static class ProjectionResult {
        public double lat;
        public double lng;
        public double distanceToP;
        public Edge closestEdge;
    }

    public static ProjectionResult findClosestProjectedPoint(double pLat, double pLng, List<Edge> allEdges) {
        ProjectionResult bestMatch = new ProjectionResult();
        bestMatch.distanceToP = Double.MAX_VALUE;

        for (Edge edge : allEdges) {
            Node a = edge.source;
            Node b = edge.target;

            double ab_lat = b.lat - a.lat;
            double ab_lng = b.lng - a.lng;
            double ap_lat = pLat - a.lat;
            double ap_lng = pLng - a.lng;
            double dotProduct = ap_lng * ab_lng + ap_lat * ab_lat;
            double abSquared = ab_lng * ab_lng + ab_lat * ab_lat;
            double t = -1;
            if (abSquared != 0) {
                t = dotProduct / abSquared;
            }
            double projLat, projLng;

            if (t < 0) {
                projLat = a.lat;
                projLng = a.lng;
            } else if (t > 1) {
                projLat = b.lat;
                projLng = b.lng;
            } else {
                projLat = a.lat + t * ab_lat;
                projLng = a.lng + t * ab_lng;
            }

            double currentDist = calculateDistanceMeters(pLat, pLng, projLat, projLng);

            if (currentDist < bestMatch.distanceToP) {
                bestMatch.distanceToP = currentDist;
                bestMatch.lat = projLat;
                bestMatch.lng = projLng;
                bestMatch.closestEdge = edge;
            }
        }
        return bestMatch;
    }

    public static double calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * earthRadius * Math.asin(Math.sqrt(a));
    }
}
