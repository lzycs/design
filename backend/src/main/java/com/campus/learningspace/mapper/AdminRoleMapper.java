package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.AdminRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminRoleMapper extends BaseMapper<AdminRole> {

    @Select("""
        SELECT * FROM admin_role
        WHERE deleted = 0 AND user_role = #{userRole}
        ORDER BY id DESC
        LIMIT 1
    """)
    AdminRole selectActiveByUserRole(@Param("userRole") Integer userRole);

}

