package com.campus.learningspace.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.AdminOperationLog;
import com.campus.learningspace.mapper.AdminOperationLogMapper;
import org.springframework.stereotype.Service;

@Service
public class AdminOperationLogService extends ServiceImpl<AdminOperationLogMapper, AdminOperationLog> {
}

