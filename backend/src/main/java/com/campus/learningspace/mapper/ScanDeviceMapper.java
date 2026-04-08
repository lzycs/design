package com.campus.learningspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learningspace.entity.ScanDevice;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface ScanDeviceMapper extends BaseMapper<ScanDevice> {
    @Delete("DELETE FROM scan_device WHERE id = #{id}")
    int hardDeleteById(@Param("id") Long id);
}

