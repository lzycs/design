-- 创建数据库
CREATE DATABASE IF NOT EXISTS campus_learning_space DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE campus_learning_space;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username` VARCHAR(100) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `real_name` VARCHAR(100) NOT NULL COMMENT '真实姓名',
    `student_id` VARCHAR(50) UNIQUE COMMENT '学号',
    `email` VARCHAR(255) COMMENT '邮箱',
    `phone` VARCHAR(50) COMMENT '手机号',
    `role` TINYINT NOT NULL DEFAULT 1 COMMENT '角色: 1-学生, 2-教师, 3-管理员, 4-后勤人员',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `weekly_reservation_count` INT NOT NULL DEFAULT 0 COMMENT '本周预约次数',
    `violation_count` INT NOT NULL DEFAULT 0 COMMENT '违约次数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_username (`username`),
    INDEX idx_student_id (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 教学楼表
CREATE TABLE IF NOT EXISTS `building` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '教学楼ID',
    `name` VARCHAR(100) NOT NULL COMMENT '教学楼名称',
    `address` VARCHAR(255) COMMENT '地址',
    `floor_count` INT NOT NULL COMMENT '楼层数',
    `description` TEXT COMMENT '描述',
    `latitude` DECIMAL(10, 6) COMMENT '纬度',
    `longitude` DECIMAL(10, 6) COMMENT '经度',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-停用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学楼表';

-- 教室表
CREATE TABLE IF NOT EXISTS `classroom` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '教室ID',
    `building_id` BIGINT NOT NULL COMMENT '教学楼ID',
    `name` VARCHAR(100) NOT NULL COMMENT '教室名称',
    `room_number` VARCHAR(50) NOT NULL COMMENT '教室编号',
    `floor` INT NOT NULL COMMENT '楼层',
    `type` TINYINT NOT NULL COMMENT '类型: 1-普通教室, 2-研讨室/会议室, 3-图书馆固定座位, 4-研修间',
    `capacity` INT NOT NULL COMMENT '容纳人数',
    `equipment` TEXT COMMENT '设备信息(JSON格式)',
    `latitude` DECIMAL(10, 6) COMMENT '纬度',
    `longitude` DECIMAL(10, 6) COMMENT '经度',
    `checkin_radius` INT NOT NULL DEFAULT 50 COMMENT '签到半径(米)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-停用, 1-正常, 2-维修中',
    `real_time_status` TINYINT COMMENT '实时状态: 0-空闲, 1-使用中, 2-已预约',
    `environment_score` DECIMAL(3, 2) DEFAULT 0.00 COMMENT '环境评分',
    `total_reviews` INT DEFAULT 0 COMMENT '评价总数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_building_id (`building_id`),
    INDEX idx_status (`status`),
    FOREIGN KEY (`building_id`) REFERENCES `building`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教室表';

-- 课程表
CREATE TABLE IF NOT EXISTS `course` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '课程ID',
    `classroom_id` BIGINT NOT NULL COMMENT '教室ID',
    `course_name` VARCHAR(100) NOT NULL COMMENT '课程名称',
    `teacher_name` VARCHAR(50) COMMENT '教师姓名',
    `week_day` TINYINT NOT NULL COMMENT '星期: 1-7',
    `start_time` TIME NOT NULL COMMENT '开始时间',
    `end_time` TIME NOT NULL COMMENT '结束时间',
    `start_week` INT NOT NULL COMMENT '开始周',
    `end_week` INT NOT NULL COMMENT '结束周',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_classroom_id (`classroom_id`),
    FOREIGN KEY (`classroom_id`) REFERENCES `classroom`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 预约记录表
CREATE TABLE IF NOT EXISTS `reservation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预约ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `classroom_id` BIGINT NOT NULL COMMENT '教室ID',
    `reservation_date` DATE NOT NULL COMMENT '预约日期',
    `start_time` TIME NOT NULL COMMENT '开始时间',
    `end_time` TIME NOT NULL COMMENT '结束时间',
    `duration` INT NOT NULL COMMENT '时长(分钟)',
    `purpose` TEXT COMMENT '预约用途',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-待签到, 2-已签到, 3-已完成, 4-已取消, 5-已违约',
    `qrcode` VARCHAR(255) COMMENT '二维码内容',
    `checkin_time` DATETIME COMMENT '签到时间',
    `checkin_latitude` DECIMAL(10, 6) COMMENT '签到纬度',
    `checkin_longitude` DECIMAL(10, 6) COMMENT '签到经度',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_user_id (`user_id`),
    INDEX idx_classroom_id (`classroom_id`),
    INDEX idx_reservation_date (`reservation_date`),
    INDEX idx_status (`status`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`classroom_id`) REFERENCES `classroom`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表';

CREATE TABLE IF NOT EXISTS `reservation_limit_config` (
    `id` TINYINT NOT NULL PRIMARY KEY DEFAULT 1,
    `max_per_week` INT NOT NULL DEFAULT 4,
    `max_duration_minutes` INT NOT NULL DEFAULT 240,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约上限配置';

INSERT IGNORE INTO `reservation_limit_config` (`id`, `max_per_week`, `max_duration_minutes`) VALUES (1, 4, 240);

-- 报修工单表
CREATE TABLE IF NOT EXISTS `repair` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '工单ID',
    `user_id` BIGINT NOT NULL COMMENT '报修人ID',
    `classroom_id` BIGINT NOT NULL COMMENT '教室ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `description` TEXT NOT NULL COMMENT '描述',
    `images` TEXT COMMENT '图片路径(JSON数组)',
    `type` TINYINT NOT NULL COMMENT '类型: 1-照明, 2-空调, 3-桌椅, 4-多媒体设备, 5-网络, 6-其他',
    `priority` TINYINT NOT NULL DEFAULT 2 COMMENT '优先级: 1-紧急, 2-普通, 3-低',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-待处理, 2-处理中, 3-已修复, 4-已关闭',
    `handler_id` BIGINT COMMENT '处理人ID',
    `handle_time` DATETIME COMMENT '处理时间',
    `handle_remark` TEXT COMMENT '处理备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_user_id (`user_id`),
    INDEX idx_classroom_id (`classroom_id`),
    INDEX idx_status (`status`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`classroom_id`) REFERENCES `classroom`(`id`),
    FOREIGN KEY (`handler_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修工单表';

-- 评价表
CREATE TABLE IF NOT EXISTS `review` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评价ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `classroom_id` BIGINT NOT NULL COMMENT '教室ID',
    `reservation_id` BIGINT COMMENT '预约ID',
    `score` TINYINT NOT NULL COMMENT '评分: 1-5',
    `content` TEXT COMMENT '评价内容',
    `tags` VARCHAR(255) COMMENT '标签(JSON数组)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-待审核, 1-已展示, 2-已屏蔽',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_user_id (`user_id`),
    INDEX idx_classroom_id (`classroom_id`),
    INDEX idx_reservation_id (`reservation_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`classroom_id`) REFERENCES `classroom`(`id`),
    FOREIGN KEY (`reservation_id`) REFERENCES `reservation`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- 组队需求表
CREATE TABLE IF NOT EXISTS `team_request` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '需求ID',
    `user_id` BIGINT NOT NULL COMMENT '发布人ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `description` TEXT COMMENT '描述',
    `tags` VARCHAR(255) COMMENT '标签(JSON数组)',
    `expected_count` INT NOT NULL COMMENT '期望人数',
    `current_count` INT NOT NULL DEFAULT 1 COMMENT '当前人数',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-已关闭, 1-招募中, 2-已满员',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_user_id (`user_id`),
    INDEX idx_status (`status`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组队需求表';

-- 组队成员表
CREATE TABLE IF NOT EXISTS `team_member` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    `team_request_id` BIGINT NOT NULL COMMENT '组队需求ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` TINYINT NOT NULL DEFAULT 2 COMMENT '角色: 1-创建者, 2-成员',
    `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    UNIQUE KEY uk_team_user (`team_request_id`, `user_id`),
    INDEX idx_team_request_id (`team_request_id`),
    INDEX idx_user_id (`user_id`),
    FOREIGN KEY (`team_request_id`) REFERENCES `team_request`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组队成员表';

-- 学习计划表
CREATE TABLE IF NOT EXISTS `study_plan` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '计划ID',
    `team_request_id` BIGINT COMMENT '组队需求ID',
    `user_id` BIGINT NOT NULL COMMENT '创建人ID',
    `reservation_id` BIGINT COMMENT '关联预约ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `description` TEXT COMMENT '描述',
    `plan_date` DATE COMMENT '计划日期',
    `start_time` TIME COMMENT '开始时间',
    `end_time` TIME COMMENT '结束时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-已取消, 1-进行中, 2-已完成',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_team_request_id (`team_request_id`),
    INDEX idx_user_id (`user_id`),
    FOREIGN KEY (`team_request_id`) REFERENCES `team_request`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`reservation_id`) REFERENCES `reservation`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划表';

-- 通知消息表
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
    `type` TINYINT NOT NULL COMMENT '类型: 1-系统通知, 2-预约提醒, 3-报修通知, 4-组队消息',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `related_id` BIGINT COMMENT '关联业务ID',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
    `read_time` DATETIME COMMENT '阅读时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_user_id (`user_id`),
    INDEX idx_is_read (`is_read`),
    INDEX idx_create_time (`create_time`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息表';
