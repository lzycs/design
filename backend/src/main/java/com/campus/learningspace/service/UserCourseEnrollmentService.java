package com.campus.learningspace.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.UserCourseEnrollment;
import com.campus.learningspace.mapper.UserCourseEnrollmentMapper;
import org.springframework.stereotype.Service;

@Service
public class UserCourseEnrollmentService extends ServiceImpl<UserCourseEnrollmentMapper, UserCourseEnrollment> {
}
