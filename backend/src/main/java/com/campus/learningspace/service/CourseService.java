package com.campus.learningspace.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.Course;
import com.campus.learningspace.mapper.CourseMapper;
import org.springframework.stereotype.Service;

@Service
public class CourseService extends ServiceImpl<CourseMapper, Course> {
}

