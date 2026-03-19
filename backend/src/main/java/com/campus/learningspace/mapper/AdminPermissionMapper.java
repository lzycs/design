package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.AdminPermission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminPermissionMapper extends BaseMapper<AdminPermission> {

    @Select("""
        SELECT COUNT(1)
        FROM admin_role_permission rp
        JOIN admin_permission p ON rp.permission_id = p.id
        WHERE rp.deleted = 0 AND p.deleted = 0
          AND rp.role_id = #{roleId}
          AND p.perm_key = #{permKey}
    """)
    long countPermissionForRole(@Param("roleId") Long roleId, @Param("permKey") String permKey);
}

