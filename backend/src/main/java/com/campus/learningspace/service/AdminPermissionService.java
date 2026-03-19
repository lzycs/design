package com.campus.learningspace.service;

import com.campus.learningspace.entity.AdminMenu;
import com.campus.learningspace.mapper.AdminMenuMapper;
import com.campus.learningspace.mapper.AdminPermissionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminPermissionService {

    private final AdminPermissionMapper adminPermissionMapper;
    private final AdminMenuMapper adminMenuMapper;

    public AdminPermissionService(AdminPermissionMapper adminPermissionMapper, AdminMenuMapper adminMenuMapper) {
        this.adminPermissionMapper = adminPermissionMapper;
        this.adminMenuMapper = adminMenuMapper;
    }

    public boolean hasPermission(AdminAuthService.AdminSession session, String permKey) {
        if (session == null || permKey == null) return false;
        return adminPermissionMapper.countPermissionForRole(session.adminRoleId(), permKey) > 0;
    }

    public List<AdminMenu> listMenus(AdminAuthService.AdminSession session) {
        if (session == null) return List.of();
        return adminMenuMapper.selectMenusByRole(session.adminRoleId());
    }
}

