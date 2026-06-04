package com.example.campusguidebackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class BizController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @PostMapping("/api/track/upload")
    public Map<String, Object> uploadTrack(@RequestBody Map<String, Object> record) {
        try {
            jdbcTemplate.update("INSERT INTO track_record (studentId, distance, points, createTime) VALUES (?, ?, ?, NOW())",
                    record.get("studentId"), record.get("distance"), record.get("points"));
            return Map.of("code", 200, "msg", "轨迹上传成功");
        } catch (Exception e) {
            return Map.of("code", 500, "msg", "轨迹上传失败");
        }
    }

    @GetMapping("/api/track/history")
    public Map<String, Object> getHistory(@RequestParam String studentId) {
        String sql = "admin".equals(studentId) ? "SELECT * FROM track_record ORDER BY id DESC" : "SELECT * FROM track_record WHERE studentId = ? ORDER BY id DESC";
        List<Map<String, Object>> data = "admin".equals(studentId) ? jdbcTemplate.queryForList(sql) : jdbcTemplate.queryForList(sql, studentId);
        return Map.of("code", 200, "data", data);
    }

    @GetMapping("/api/admin/tracks")
    public Map<String, Object> getAllAdminTracks() {
        return Map.of("code", 200, "data", jdbcTemplate.queryForList("SELECT * FROM track_record ORDER BY id DESC"));
    }
    @GetMapping({"/api/markers", "/api/markers/all"})
    public Map<String, Object> getAllMarkers() {
        return Map.of("code", 200, "data", jdbcTemplate.queryForList("SELECT * FROM map_markers ORDER BY category"));
    }

    @GetMapping("/api/markers/search")
    public Map<String, Object> searchMarkers(@RequestParam String keyword) {
        String key = "%" + keyword + "%";
        return Map.of("code", 200, "data", jdbcTemplate.queryForList("SELECT * FROM map_markers WHERE name LIKE ? OR snippet LIKE ? OR category LIKE ?", key, key, key));
    }

    @PostMapping("/api/admin/markers")
    public Map<String, Object> addMarker(@RequestBody Map<String, Object> m) {
        String cat = m.get("category") != null ? m.get("category").toString() : "未分类";
        jdbcTemplate.update("INSERT INTO map_markers (name, snippet, latitude, longitude, category) VALUES (?, ?, ?, ?, ?)", m.get("name"), m.get("snippet"), m.get("lat"), m.get("lng"), cat);
        return Map.of("code", 200, "msg", "地标添加成功");
    }

    @PutMapping("/api/admin/markers/{id}")
    public Map<String, Object> updateMarker(@PathVariable int id, @RequestBody Map<String, Object> m) {
        String cat = m.get("category") != null ? m.get("category").toString() : "未分类";
        jdbcTemplate.update("UPDATE map_markers SET name = ?, snippet = ?, latitude = ?, longitude = ?, category = ? WHERE id = ?", m.get("name"), m.get("snippet"), m.get("lat"), m.get("lng"), cat, id);
        return Map.of("code", 200, "msg", "地标更新成功");
    }

    @DeleteMapping("/api/admin/markers/{id}")
    public Map<String, Object> deleteMarker(@PathVariable("id") int id) {
        jdbcTemplate.update("DELETE FROM map_markers WHERE id = ?", id);
        return Map.of("code", 200, "msg", "删除成功");
    }

    @PostMapping("/api/favorites/add")
    public Map<String, Object> addFavorite(@RequestBody Map<String, String> params) {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM favorites WHERE student_id = ? AND marker_id = ?", Integer.class, params.get("studentId"), params.get("markerId"));
        if (count != null && count > 0) return Map.of("code", 400, "msg", "已经收藏过啦");
        jdbcTemplate.update("INSERT INTO favorites (student_id, marker_id) VALUES (?, ?)", params.get("studentId"), params.get("markerId"));
        return Map.of("code", 200, "msg", "收藏成功");
    }

    @GetMapping("/api/favorites")
    public Map<String, Object> getFavorites(@RequestParam String studentId) {
        return Map.of("code", 200, "data", jdbcTemplate.queryForList("SELECT m.*, f.create_time FROM map_markers m JOIN favorites f ON m.id = f.marker_id WHERE f.student_id = ? ORDER BY f.id DESC", studentId));
    }

    @PostMapping("/api/favorites/delete")
    public Map<String, Object> deleteFavorite(@RequestBody Map<String, String> params) {
        jdbcTemplate.update("DELETE FROM favorites WHERE student_id = ? AND marker_id = ?", params.get("studentId"), params.get("markerId"));
        return Map.of("code", 200, "msg", "取消收藏成功");
    }
    @GetMapping("/api/lostfound")
    public Map<String, Object> getLostFound() {
        return Map.of("code", 200, "data", jdbcTemplate.queryForList("SELECT * FROM lost_found ORDER BY id DESC"));
    }
    @PostMapping("/api/lostfound")
    public Map<String, Object> addLostFound(@RequestBody Map<String, String> params) {
        jdbcTemplate.update("INSERT INTO lost_found (student_id, type, title, content, contact) VALUES (?, ?, ?, ?, ?)", params.get("studentId"), params.get("type"), params.get("title"), params.get("content"), params.get("contact"));
        return Map.of("code", 200, "msg", "发布成功");
    }
    @PutMapping("/api/admin/lostfound/status/{id}")
    public Map<String, Object> updateLostFoundStatus(@PathVariable int id, @RequestParam int status) {
        jdbcTemplate.update("UPDATE lost_found SET status = ? WHERE id = ?", status, id);
        return Map.of("code", 200, "msg", "状态更新成功");
    }
    @DeleteMapping("/api/lostfound/{id}")
    public Map<String, Object> deleteLostFound(@PathVariable("id") int id, @RequestParam(required = false) String studentId) {
        if ("admin".equals(studentId)) {
            jdbcTemplate.update("DELETE FROM lost_found WHERE id = ?", id);
        } else {
            jdbcTemplate.update("DELETE FROM lost_found WHERE id = ? AND student_id = ?", id, studentId);
        }
        return Map.of("code", 200, "msg", "删除成功");
    }

    @GetMapping("/api/notices")
    public Map<String, Object> getNotices() {
        return Map.of("code", 200, "data", jdbcTemplate.queryForList("SELECT * FROM notices ORDER BY id DESC"));
    }

    @PostMapping("/api/admin/notices")
    public Map<String, Object> addNotice(@RequestBody Map<String, String> params) {
        jdbcTemplate.update("INSERT INTO notices (title, content) VALUES (?, ?)", params.get("title"), params.get("content"));
        return Map.of("code", 200, "msg", "发布成功");
    }

    @PutMapping("/api/admin/notices/{id}")
    public Map<String, Object> updateNotice(@PathVariable int id, @RequestBody Map<String, String> params) {
        jdbcTemplate.update("UPDATE notices SET title = ?, content = ? WHERE id = ?", params.get("title"), params.get("content"), id);
        return Map.of("code", 200, "msg", "更新成功");
    }

    @DeleteMapping("/api/admin/notices/{id}")
    public Map<String, Object> deleteNotice(@PathVariable int id) {
        jdbcTemplate.update("DELETE FROM notices WHERE id = ?", id);
        return Map.of("code", 200, "msg", "删除成功");
    }
    @GetMapping("/api/config/all")
    public Map<String, Object> getAllConfigs() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM system_config");
        Map<String, String> configMap = new HashMap<>();
        for (Map<String, Object> row : list) {
            configMap.put((String)row.get("config_key"), (String)row.get("config_value"));
        }
        return Map.of("code", 200, "data", configMap);
    }

    @PostMapping("/api/admin/config/update")
    public Map<String, Object> updateConfig(@RequestBody Map<String, String> params) {
        params.forEach((key, value) -> jdbcTemplate.update("INSERT INTO system_config (config_key, config_value) VALUES (?, ?) ON DUPLICATE KEY UPDATE config_value = ?", key, value, value));
        return Map.of("code", 200, "msg", "配置更新成功");
    }
    @GetMapping("/api/admin/map/nodes")
    public Map<String, Object> getMapNodes() {
        return Map.of("code", 200, "data", jdbcTemplate.queryForList("SELECT * FROM map_nodes"));
    }

    @PostMapping("/api/admin/map/node")
    public Map<String, Object> addMapNode(@RequestBody Map<String, Object> params) {
        jdbcTemplate.update("INSERT INTO map_nodes (name, lat, lng) VALUES (?, ?, ?)", params.get("name"), params.get("lat"), params.get("lng"));
        return Map.of("code", 200, "msg", "节点添加成功");
    }

    @GetMapping("/api/admin/map/edges")
    public Map<String, Object> getMapEdges() {
        String sql = "SELECT e.*, n1.lat as lat1, n1.lng as lng1, n2.lat as lat2, n2.lng as lng2 FROM map_edges e JOIN map_nodes n1 ON e.source_id = n1.id JOIN map_nodes n2 ON e.target_id = n2.id";
        return Map.of("code", 200, "data", jdbcTemplate.queryForList(sql));
    }

    @PostMapping("/api/admin/map/edge")
    public Map<String, Object> addMapEdge(@RequestBody Map<String, Object> params) {
        Long sourceId = Long.valueOf(params.get("sourceId").toString());
        Long targetId = Long.valueOf(params.get("targetId").toString());
        Double distance = Double.valueOf(params.get("distance").toString());
        jdbcTemplate.update("INSERT INTO map_edges (source_id, target_id, distance) VALUES (?, ?, ?)", sourceId, targetId, distance);
        jdbcTemplate.update("INSERT INTO map_edges (source_id, target_id, distance) VALUES (?, ?, ?)", targetId, sourceId, distance);
        return Map.of("code", 200, "msg", "路线连接成功");
    }

    @DeleteMapping("/api/admin/map/node/{id}")
    public Map<String, Object> deleteMapNode(@PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM map_edges WHERE source_id = ? OR target_id = ?", id, id);
        jdbcTemplate.update("DELETE FROM map_nodes WHERE id = ?", id);
        return Map.of("code", 200, "msg", "节点及相关道路彻底删除成功");
    }
    @PostMapping("/api/upload")
    public Map<String, Object> uploadFile(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String dirPath = System.getProperty("user.dir") + "/uploads/";
            java.io.File dir = new java.io.File(dirPath);
            if (!dir.exists()) dir.mkdirs();
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            file.transferTo(new java.io.File(dir, fileName));
            String url = "http://10.0.2.2:8080/uploads/" + fileName;
            return Map.of("code", 200, "data", url, "msg", "头像上传成功");
        } catch (Exception e) {
            return Map.of("code", 500, "msg", "头像上传失败: " + e.getMessage());
        }
    }
}