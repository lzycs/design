package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.AdminMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AdminMenuMapper extends BaseMapper<AdminMenu> {

    @Select("""
        SELECT m.*
        FROM admin_menu m
        WHERE m.deleted = 0
          AND (m.permission_key IS NULL OR EXISTS (
              SELECT 1
              FROM admin_role_permission rp
              JOIN admin_permission p ON rp.permission_id = p.id
              WHERE rp.deleted = 0
                AND p.deleted = 0
                AND rp.role_id = #{roleId}
                AND p.perm_key = m.permission_key
          ))
        ORDER BY m.sort_order ASC, m.id ASC
    """)
    List<AdminMenu> selectMenusByRole(@Param("roleId") Long roleId);
}

