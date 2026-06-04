package com.example.campusguidebackend.controller;

import com.example.campusguidebackend.NavigationService;
import com.example.campusguidebackend.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nav")
public class NavigationController {
    @Autowired
    private NavigationService navService;
    @GetMapping("/route")
    public Map<String, Object> getRoute(@RequestParam Long startId,
                                        @RequestParam Long endId,
                                        @RequestParam(defaultValue = "dijkstra") String algorithm) {
        try {
            Map<Long, Node> graph = navService.loadRoadGraph();
            return buildRouteResponse(graph, startId, endId, algorithm, "自主导航路线计算成功！");
        } catch (Exception e) {
            return Map.of("code", 500, "msg", "计算失败: " + e.getMessage());
        }
    }
    @GetMapping("/routeByLocation")
    public Map<String, Object> getRouteByLocation(@RequestParam double startLat, @RequestParam double startLng,
                                                  @RequestParam double endLat, @RequestParam double endLng,
                                                  @RequestParam(defaultValue = "astar") String algorithm) {
        try {
            Map<Long, Node> graph = navService.loadRoadGraph();
            if (graph.isEmpty()) return Map.of("code", 404, "msg", "地图中还没有绘制道路！");

            List<NavigationService.RoadSegment> segments = navService.collectRoadSegments(graph);
            if (segments.isEmpty()) return Map.of("code", 404, "msg", "校园路网尚未建立！");

            NavigationService.SnapResult startSnap = navService.snapPointToNearestRoad(graph, segments, startLat, startLng, -1000001L);
            NavigationService.SnapResult endSnap = navService.snapPointToNearestRoad(graph, segments, endLat, endLng, -1000002L);

            if (startSnap == null || endSnap == null) return Map.of("code", 404, "msg", "无法将坐标吸附到路网");

            if (navService.sameSegment(startSnap, endSnap)) {
                double directDistance = navService.distanceMeters(startSnap.node.lat, startSnap.node.lng, endSnap.node.lat, endSnap.node.lng);
                navService.connectBidirectional(startSnap.node, endSnap.node, directDistance);
            }
            Map<String, Object> res = buildRouteResponse(graph, startSnap.node.id, endSnap.node.id, algorithm, "智能吸附导航计算成功！");
            if (!Integer.valueOf(200).equals(res.get("code"))) return res;
            @SuppressWarnings("unchecked")
            List<Node> roadPath = (List<Node>) res.get("data");
            List<Map<String, Object>> displayPath = buildDisplayPath(startLat, startLng, endLat, endLng, roadPath);
            double roadDistance = ((Number) res.get("distance")).doubleValue();
            double totalDistance = roadDistance + startSnap.snapDistance + endSnap.snapDistance;
            res.put("data", displayPath);
            res.put("distance", totalDistance);
            res.put("durationMinute", Math.max(1, (int) Math.ceil(totalDistance / 80.0)));
            res.put("startSnapDistance", startSnap.snapDistance);
            res.put("endSnapDistance", endSnap.snapDistance);
            return res;
        } catch (Exception e) {
            return Map.of("code", 500, "msg", "导航计算异常: " + e.getMessage());
        }
    }
    private Map<String, Object> buildRouteResponse(Map<Long, Node> graph, Long startId, Long endId, String algorithm, String msg) {
        Map<String, Object> res = new HashMap<>();
        if (!graph.containsKey(startId) || !graph.containsKey(endId)) {
            return Map.of("code", 404, "msg", "找不到起点或终点节点");
        }
        String selectedAlgorithm = "astar".equalsIgnoreCase(algorithm) ? "astar" : "dijkstra";
        List<Node> shortestPath = "astar".equals(selectedAlgorithm)
                ? navService.calculateAStarPath(graph, startId, endId)
                : navService.calculateShortestPath(graph, startId, endId);

        if (shortestPath.isEmpty() || !shortestPath.get(0).id.equals(startId)) {
            return Map.of("code", 404, "msg", "这两点之间无法通行（路网未连通）");
        }
        res.put("code", 200);
        res.put("data", shortestPath);
        res.put("distance", graph.get(endId).minDistance);
        res.put("msg", msg);
        return res;
    }

    private List<Map<String, Object>> buildDisplayPath(double sLat, double sLng, double eLat, double eLng, List<Node> roadPath) {
        List<Map<String, Object>> path = new ArrayList<>();
        addPoint(path, -1L, sLat, sLng, "actualStart");
        for (Node node : roadPath) addPoint(path, node.id, node.lat, node.lng, node.id < 0 ? "snap" : "road");
        addPoint(path, -2L, eLat, eLng, "actualEnd");
        return path;
    }

    private void addPoint(List<Map<String, Object>> path, Long id, double lat, double lng, String type) {
        if (!path.isEmpty()) {
            Map<String, Object> last = path.get(path.size() - 1);
            if (navService.distanceMeters((double)last.get("lat"), (double)last.get("lng"), lat, lng) <= 0.8) return;
        }
        path.add(Map.of("id", id, "lat", lat, "lng", lng, "type", type));
    }
}