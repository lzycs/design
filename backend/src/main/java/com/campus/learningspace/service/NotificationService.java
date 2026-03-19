package com.campus.learningspace.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.Notification;
import com.campus.learningspace.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

@Service
public class NotificationService extends ServiceImpl<NotificationMapper, Notification> {
}

