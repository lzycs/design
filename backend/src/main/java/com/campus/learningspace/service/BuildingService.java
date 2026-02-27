package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.Building;
import com.campus.learningspace.mapper.BuildingMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BuildingService extends ServiceImpl<BuildingMapper, Building> {

    public List<Building> getAllActiveBuildings() {
        LambdaQueryWrapper<Building> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Building::getStatus, 1);
        return list(wrapper);
    }
}
