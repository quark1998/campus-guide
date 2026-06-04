package com.example.campusguidebackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class UserController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @PostMapping("/api/user/login")
    public Map<String, Object> login(@RequestBody Map<String, String> user) {
        String sql = "SELECT * FROM users WHERE student_id = ? AND password = ?";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, user.get("studentId"), user.get("password"));
        Map<String, Object> res = new HashMap<>();
        if (!list.isEmpty()) {
            res.put("code", 200);
            res.put("data", list.get(0));
        } else {
            res.put("code", 400);
            res.put("msg", "账号或密码错误");
        }
        return res;
    }
    @PostMapping("/api/user/register")
    public Map<String, Object> register(@RequestBody Map<String, String> user) {
        Map<String, Object> res = new HashMap<>();
        try {
            jdbcTemplate.update("INSERT INTO users (student_id, password, nickname) VALUES (?, ?, ?)",
                    user.get("studentId"), user.get("password"), user.get("nickname"));
            res.put("code", 200);
            res.put("msg", "注册成功");
        } catch (Exception e) {
            res.put("code", 400);
            res.put("msg", "学号已存在或注册失败");
        }
        return res;
    }
    @PostMapping("/api/user/resetPassword")
    public Map<String, Object> resetPassword(@RequestBody Map<String, String> params) {
        Map<String, Object> res = new HashMap<>();
        String studentId = params.get("studentId");
        String nickname = params.get("nickname");
        String newPassword = params.get("newPassword");
        if (studentId == null || nickname == null || newPassword == null) {
            res.put("code", 400); res.put("msg", "参数不完整"); return res;
        }
        try {
            String querySql = "SELECT * FROM users WHERE student_id = ?";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(querySql, studentId);
            if (list.isEmpty()) {
                res.put("code", 404); res.put("msg", "学号不存在"); return res;
            }
            if (!nickname.equals((String) list.get(0).get("nickname"))) {
                res.put("code", 403); res.put("msg", "昵称不匹配"); return res;
            }
            jdbcTemplate.update("UPDATE users SET password = ? WHERE student_id = ?", newPassword, studentId);
            res.put("code", 200); res.put("msg", "密码重置成功");
        } catch (Exception e) {
            res.put("code", 500); res.put("msg", "服务器异常: " + e.getMessage());
        }
        return res;
    }
    @PostMapping("/api/user/update")
    public Map<String, Object> updateProfile(@RequestBody Map<String, String> params) {
        String studentId = params.get("studentId");
        String nickname = params.get("nickname");
        String password = params.get("password");
        String avatar = params.get("avatar");
        try {
            List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT * FROM users WHERE student_id = ?", studentId);
            if (users.isEmpty()) return Map.of("code", 404, "msg", "用户不存在");
            Map<String, Object> currentUser = users.get(0);
            String finalNickname = (nickname == null || nickname.trim().isEmpty()) ? (String) currentUser.get("nickname") : nickname.trim();
            String finalAvatar = (avatar == null || avatar.trim().isEmpty()) ? (String) currentUser.get("avatar") : avatar.trim();
            if (password == null || password.trim().isEmpty()) {
                jdbcTemplate.update("UPDATE users SET nickname = ?, avatar = ? WHERE student_id = ?", finalNickname, finalAvatar, studentId);
            } else {
                jdbcTemplate.update("UPDATE users SET nickname = ?, password = ?, avatar = ? WHERE student_id = ?", finalNickname, password.trim(), finalAvatar, studentId);
            }
            return Map.of("code", 200, "msg", "资料更新成功");
        } catch (Exception e) {
            return Map.of("code", 500, "msg", "更新失败: " + e.getMessage());
        }
    }
    @PostMapping("/api/admin/login")
    public Map<String, Object> adminLogin(@RequestBody Map<String, String> info) {
        Map<String, Object> res = new HashMap<>();
        if ("admin".equals(info.get("username")) && "admin123".equals(info.get("password"))) {
            res.put("code", 200); res.put("token", "ADMIN_OK");
        } else {
            res.put("code", 400); res.put("msg", "管理员账号或密码错误");
        }
        return res;
    }
    @GetMapping("/api/admin/users")
    public Map<String, Object> getAllUsers() {
        return Map.of("code", 200, "data", jdbcTemplate.queryForList("SELECT * FROM users"));
    }
    @DeleteMapping("/api/admin/users/{id}")
    public Map<String, Object> deleteUser(@PathVariable("id") int id) {
        try {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
            return Map.of("code", 200, "msg", "用户删除成功");
        } catch (Exception e) {
            return Map.of("code", 500, "msg", "删除失败: " + e.getMessage());
        }
    }
}