package com.example.campusguidebackend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NavigationService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    public Map<Long, Node> loadRoadGraph() {
        Map<Long, Node> graph = new HashMap<>();
        List<Map<String, Object>> nodesData = jdbcTemplate.queryForList("SELECT * FROM map_nodes");
        for (Map<String, Object> row : nodesData) {
            Long id = ((Number) row.get("id")).longValue();
            graph.put(id, new Node(id, ((Number) row.get("lat")).doubleValue(), ((Number) row.get("lng")).doubleValue()));
        }
        List<Map<String, Object>> edgesData = jdbcTemplate.queryForList("SELECT * FROM map_edges");
        for (Map<String, Object> row : edgesData) {
            Long sourceId = ((Number) row.get("source_id")).longValue();
            Long targetId = ((Number) row.get("target_id")).longValue();
            double distance = ((Number) row.get("distance")).doubleValue();
            Node sourceNode = graph.get(sourceId);
            Node targetNode = graph.get(targetId);
            if (sourceNode != null && targetNode != null) {
                sourceNode.edges.add(new Edge(sourceNode, targetNode, distance));
            }
        }
        return graph;
    }
    public List<RoadSegment> collectRoadSegments(Map<Long, Node> graph) {
        List<RoadSegment> segments = new ArrayList<>();
        for (Node source : graph.values()) {
            if (source.id < 0) continue;
            for (Edge edge : source.edges) {
                if (edge.target == null || edge.target.id < 0) continue;
                segments.add(new RoadSegment(source, edge.target));
            }
        }
        return segments;
    }
    public List<Node> calculateShortestPath(Map<Long, Node> graph, Long startId, Long endId) {
        Node source = graph.get(startId);
        Node target = graph.get(endId);
        if (source == null || target == null) return new ArrayList<>();
        resetGraphState(graph);
        source.minDistance = 0.0;
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(v -> v.minDistance));
        queue.add(source);
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (current.id.equals(endId)) break;
            for (Edge edge : current.edges) {
                Node next = edge.target;
                double candidateDistance = current.minDistance + edge.weight;
                if (candidateDistance < next.minDistance) {
                    queue.remove(next);
                    next.minDistance = candidateDistance;
                    next.previous = current;
                    queue.add(next);
                }
            }
        }
        return buildPath(target);
    }
    public List<Node> calculateAStarPath(Map<Long, Node> graph, Long startId, Long endId) {
        Node source = graph.get(startId);
        Node target = graph.get(endId);
        if (source == null || target == null) return new ArrayList<>();
        resetGraphState(graph);
        source.minDistance = 0.0;
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(
                node -> node.minDistance + heuristicDistance(node, target)));
        Set<Long> closedSet = new HashSet<>();
        openSet.add(source);
        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            if (current.id.equals(endId)) break;
            if (!closedSet.add(current.id)) continue;
            for (Edge edge : current.edges) {
                Node next = edge.target;
                if (closedSet.contains(next.id)) continue;
                double candidateDistance = current.minDistance + edge.weight;
                if (candidateDistance < next.minDistance) {
                    next.minDistance = candidateDistance;
                    next.previous = current;
                    openSet.remove(next);
                    openSet.add(next);
                }
            }
        }
        return buildPath(target);
    }

    public SnapResult snapPointToNearestRoad(Map<Long, Node> graph, List<RoadSegment> segments, double lat, double lng, long virtualId) {
        Projection best = null;
        RoadSegment bestSegment = null;
        for (RoadSegment segment : segments) {
            Projection projection = projectPointToSegment(lat, lng, segment.source, segment.target);
            if (best == null || projection.distanceToRoad < best.distanceToRoad) {
                best = projection;
                bestSegment = segment;
            }
        }
        if (best == null || bestSegment == null) return null;
        Node virtualNode = new Node(virtualId, best.lat, best.lng);
        graph.put(virtualId, virtualNode);
        connectBidirectional(virtualNode, bestSegment.source, distanceMeters(best.lat, best.lng, bestSegment.source.lat, bestSegment.source.lng));
        connectBidirectional(virtualNode, bestSegment.target, distanceMeters(best.lat, best.lng, bestSegment.target.lat, bestSegment.target.lng));
        return new SnapResult(virtualNode, bestSegment.source.id, bestSegment.target.id, best.distanceToRoad);
    }
    public Projection projectPointToSegment(double lat, double lng, Node a, Node b) {
        double originLat = lat;
        double px = lngToMeters(lng, originLat), py = latToMeters(lat);
        double ax = lngToMeters(a.lng, originLat), ay = latToMeters(a.lat);
        double bx = lngToMeters(b.lng, originLat), by = latToMeters(b.lat);
        double dx = bx - ax, dy = by - ay;
        double len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : ((px - ax) * dx + (py - ay) * dy) / len2;
        t = Math.max(0, Math.min(1, t));
        double projX = ax + t * dx, projY = ay + t * dy;
        return new Projection(metersToLat(projY), metersToLng(projX, originLat), Math.hypot(px - projX, py - projY));
    }
    public void connectBidirectional(Node a, Node b, double distance) {

        a.edges.add(new Edge(a, b, distance));
        b.edges.add(new Edge(b, a, distance));
    }
    public boolean sameSegment(SnapResult a, SnapResult b) {
        return (a.sourceId.equals(b.sourceId) && a.targetId.equals(b.targetId))
                || (a.sourceId.equals(b.targetId) && a.targetId.equals(b.sourceId));
    }
    public double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * earthRadius * Math.asin(Math.sqrt(h));
    }
    private void resetGraphState(Map<Long, Node> graph) {
        for (Node node : graph.values()) {
            node.minDistance = Double.POSITIVE_INFINITY;
            node.previous = null;
        }
    }
    private List<Node> buildPath(Node target) {
        List<Node> path = new ArrayList<>();
        for (Node node = target; node != null; node = node.previous) path.add(node);
        Collections.reverse(path);
        return path;
    }
    private double heuristicDistance(Node a, Node b) {
        return distanceMeters(a.lat, a.lng, b.lat, b.lng);
    }
    private double latToMeters(double lat) { return lat * 110540.0; }
    private double metersToLat(double y) { return y / 110540.0; }
    private double lngToMeters(double lng, double originLat) { return lng * 111320.0 * Math.cos(Math.toRadians(originLat)); }
    private double metersToLng(double x, double originLat) { return x / (111320.0 * Math.cos(Math.toRadians(originLat))); }
    public static class RoadSegment {
        public Node source; public Node target;
        public RoadSegment(Node source, Node target) { this.source = source; this.target = target; }
    }
    public static class Projection {
        public double lat; public double lng; public double distanceToRoad;
        public Projection(double lat, double lng, double distanceToRoad) { this.lat = lat; this.lng = lng; this.distanceToRoad = distanceToRoad; }
    }
    public static class SnapResult {
        public Node node; public Long sourceId; public Long targetId; public double snapDistance;
        public SnapResult(Node node, Long sourceId, Long targetId, double snapDistance) {
            this.node = node; this.sourceId = sourceId; this.targetId = targetId; this.snapDistance = snapDistance;
        }
    }
}