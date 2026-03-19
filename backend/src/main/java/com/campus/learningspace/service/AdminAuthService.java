package com.campus.learningspace.service;

import com.campus.learningspace.entity.AdminLoginResponse;
import com.campus.learningspace.entity.AdminRole;
import com.campus.learningspace.entity.User;
import com.campus.learningspace.mapper.AdminRoleMapper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAuthService {

    private final Map<String, AdminSession> tokenToSession = new ConcurrentHashMap<>();

    private final UserService userService;
    private final AdminRoleMapper adminRoleMapper;

    public AdminAuthService(UserService userService, AdminRoleMapper adminRoleMapper) {
        this.userService = userService;
        this.adminRoleMapper = adminRoleMapper;
    }

    public record AdminSession(Long adminUserId, Long adminRoleId, Integer roleType, String username) {
    }

    public AdminLoginResponse login(String username, String password) {
        if (username == null || password == null) return null;
        User user = userService.findByUsername(username);
        if (user == null) return null;
        if (!password.equals(user.getPassword())) return null;
        if (user.getStatus() != null && user.getStatus() == 0) return null;

        AdminRole role = adminRoleMapper.selectActiveByUserRole(user.getRole());
        if (role == null) return null;

        String token = UUID.randomUUID().toString();
        AdminSession session = new AdminSession(user.getId(), role.getId(), role.getRoleType(), user.getUsername());
        tokenToSession.put(token, session);

        AdminLoginResponse resp = new AdminLoginResponse();
        resp.setToken(token);
        resp.setAdminUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRoleType(role.getRoleType());
        return resp;
    }

    public AdminSession getSession(String token) {
        if (token == null || token.isBlank()) return null;
        return tokenToSession.get(token);
    }
}

