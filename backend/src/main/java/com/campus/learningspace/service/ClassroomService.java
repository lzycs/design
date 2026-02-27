package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.Classroom;
import com.campus.learningspace.mapper.ClassroomMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassroomService extends ServiceImpl<ClassroomMapper, Classroom> {

    public List<Classroom> getClassroomsByBuilding(Long buildingId) {
        LambdaQueryWrapper<Classroom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Classroom::getBuildingId, buildingId)
                .eq(Classroom::getStatus, 1);
        return list(wrapper);
    }

    public List<Classroom> getAvailableClassrooms(Integer type) {
        LambdaQueryWrapper<Classroom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Classroom::getStatus, 1);
        if (type != null) {
            wrapper.eq(Classroom::getType, type);
        }
        return list(wrapper);
    }
}
