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

-- 教学楼表（与后端 Building 实体的 building 表对应）
CREATE TABLE IF NOT EXISTS `building` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '教学楼ID',
    `name` VARCHAR(100) NOT NULL COMMENT '教学楼名称 (如：第一教学楼)',
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

-- 图书馆表
CREATE TABLE IF NOT EXISTS `library` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '图书馆ID',
    `name` VARCHAR(100) NOT NULL COMMENT '图书馆名称 (如：中心图书馆)',
    `address` VARCHAR(255) COMMENT '地址',
    `floor_count` INT NOT NULL COMMENT '楼层数',
    `description` TEXT COMMENT '描述',
    `latitude` DECIMAL(10, 6) COMMENT '纬度',
    `longitude` DECIMAL(10, 6) COMMENT '经度',
    `opening_hours` VARCHAR(100) COMMENT '开放时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-停用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书馆表';

-- 教室表 (仅属于教学楼)
CREATE TABLE IF NOT EXISTS `classroom` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '教室ID',
    `building_id` BIGINT NOT NULL COMMENT '教学楼ID',
    `name` VARCHAR(100) NOT NULL COMMENT '教室名称',
    `room_number` VARCHAR(50) NOT NULL COMMENT '教室编号 (如：101, 201A)',
    `floor` INT NOT NULL COMMENT '楼层',
    `type` TINYINT NOT NULL COMMENT '类型: 1-普通教室, 2-研讨室/会议室',
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

-- 图书馆座位表
CREATE TABLE IF NOT EXISTS `library_seat` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '座位ID',
    `library_id` BIGINT NOT NULL COMMENT '图书馆ID',
    `seat_label` VARCHAR(50) NOT NULL COMMENT '座位标签/编号 (如：A-01)',
    `floor` INT NOT NULL COMMENT '所在楼层',
    `row_num` INT NOT NULL COMMENT '行号',
    `col_num` INT NOT NULL COMMENT '列号',
    `seat_type` TINYINT NOT NULL DEFAULT 1 COMMENT '座位类型: 1-普通座位, 2-带插座, 3-静音区座位, 4-小组研讨座',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-停用, 1-可预约, 2-维修中',
    `real_time_status` TINYINT COMMENT '实时状态: 0-空闲, 1-使用中, 2-已预约',
    `equipment` TEXT COMMENT '设备信息(JSON格式，如是否有台灯、网络)',
    `environment_score` DECIMAL(3, 2) DEFAULT 0.00 COMMENT '环境评分',
    `total_reviews` INT DEFAULT 0 COMMENT '评价总数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    UNIQUE KEY uk_library_seat (`library_id`, `floor`, `row_num`, `col_num`),
    INDEX idx_library_id (`library_id`),
    INDEX idx_status (`status`),
    INDEX idx_seat_type (`seat_type`),
    FOREIGN KEY (`library_id`) REFERENCES `library`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书馆座位表';

-- 课程表 (课程安排在教学楼的教室中)
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

-- 预约记录表 (可预约教室或图书馆座位)
CREATE TABLE IF NOT EXISTS `reservation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预约ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `resource_type` TINYINT NOT NULL COMMENT '资源类型: 1-教室, 2-图书馆座位',
    `classroom_id` BIGINT COMMENT '教室ID (当resource_type=1时)',
    `library_seat_id` BIGINT COMMENT '图书馆座位ID (当resource_type=2时)',
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
    INDEX idx_resource (`resource_type`, `classroom_id`, `library_seat_id`),
    INDEX idx_reservation_date (`reservation_date`),
    INDEX idx_status (`status`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`classroom_id`) REFERENCES `classroom`(`id`),
    FOREIGN KEY (`library_seat_id`) REFERENCES `library_seat`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表';

-- 报修工单表 (可报修教室或图书馆座位)
CREATE TABLE IF NOT EXISTS `repair` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '工单ID',
    `user_id` BIGINT NOT NULL COMMENT '报修人ID',
    `resource_type` TINYINT NOT NULL COMMENT '资源类型: 1-教室, 2-图书馆座位',
    `classroom_id` BIGINT COMMENT '教室ID (当resource_type=1时)',
    `library_seat_id` BIGINT COMMENT '图书馆座位ID (当resource_type=2时)',
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
    INDEX idx_resource (`resource_type`, `classroom_id`, `library_seat_id`),
    INDEX idx_status (`status`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`classroom_id`) REFERENCES `classroom`(`id`),
    FOREIGN KEY (`library_seat_id`) REFERENCES `library_seat`(`id`),
    FOREIGN KEY (`handler_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修工单表';

-- 评价表 (可评价教室或图书馆座位)
CREATE TABLE IF NOT EXISTS `review` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评价ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `resource_type` TINYINT NOT NULL COMMENT '资源类型: 1-教室, 2-图书馆座位',
    `classroom_id` BIGINT COMMENT '教室ID (当resource_type=1时)',
    `library_seat_id` BIGINT COMMENT '图书馆座位ID (当resource_type=2时)',
    `reservation_id` BIGINT COMMENT '预约ID',
    `score` TINYINT NOT NULL COMMENT '评分: 1-5',
    `content` TEXT COMMENT '评价内容',
    `tags` VARCHAR(255) COMMENT '标签(JSON数组)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-待审核, 1-已展示, 2-已屏蔽',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_user_id (`user_id`),
    INDEX idx_resource (`resource_type`, `classroom_id`, `library_seat_id`),
    INDEX idx_reservation_id (`reservation_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`classroom_id`) REFERENCES `classroom`(`id`),
    FOREIGN KEY (`library_seat_id`) REFERENCES `library_seat`(`id`),
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

-- ----------------------------
-- 插入测试数据
-- ----------------------------

-- 插入用户数据
INSERT IGNORE INTO `user` (`username`, `password`, `real_name`, `student_id`, `email`, `phone`, `role`, `status`) VALUES
('zhangsan', '$2a$10$exampleHash', '张三', '20220001', 'zhangsan@example.com', '13800138001', 1, 1),
('lisi', '$2a$10$exampleHash', '李四', '20220002', 'lisi@example.com', '13800138002', 1, 1),
('wanglaoshi', '$2a$10$exampleHash', '王老师', NULL, 'wang@edu.com', '13800138003', 2, 1),
('admin', '$2a$10$exampleHash', '管理员', NULL, 'admin@system.com', '13800138000', 3, 1);

-- 插入教学楼数据
INSERT IGNORE INTO `building` (`name`, `address`, `floor_count`, `description`, `latitude`, `longitude`) VALUES
('第一教学楼', '学校东区主路1号', 5, '主教学楼，设施较新。', 39.904989, 116.405285),
('第二教学楼', '学校西区科技路2号', 6, '以理工科实验室为主。', 39.905500, 116.406000);

-- 插入图书馆数据
INSERT IGNORE INTO `library` (`name`, `address`, `floor_count`, `description`, `latitude`, `longitude`, `opening_hours`) VALUES
('中心图书馆', '校园中心区', 8, '馆藏丰富，学习环境优越。', 39.903500, 116.404000, '08:00-22:00'),
('科技分馆', '西区科研楼旁', 4, '侧重科技类书籍和期刊。', 39.906000, 116.407000, '09:00-21:00');

-- 插入教室数据 (属于第一教学楼)
INSERT IGNORE INTO `classroom` (`building_id`, `name`, `room_number`, `floor`, `type`, `capacity`, `equipment`) VALUES
(1, '101多媒体教室', '101', 1, 1, 60, '["投影仪", "电脑", "音响"]'),
(1, '102普通教室', '102', 1, 1, 50, '["白板"]'),
(1, '201研讨室A', '201A', 2, 2, 10, '["会议桌", "白板", "电视"]'),
(1, '202研讨室B', '202B', 2, 2, 8, '["会议桌", "白板"]');

-- 插入教室数据 (属于第二教学楼)
INSERT IGNORE INTO `classroom` (`building_id`, `name`, `room_number`, `floor`, `type`, `capacity`, `equipment`) VALUES
(2, '301实验室', '301', 3, 1, 30, '["实验台", "电脑", "专用仪器"]'),
(2, '401会议室', '401', 4, 2, 20, '["大会议桌", "投影仪", "电话会议系统"]');

-- 插入图书馆座位数据 (中心图书馆1-2层示例)
INSERT IGNORE INTO `library_seat` (`library_id`, `seat_label`, `floor`, `row_num`, `col_num`, `seat_type`, `equipment`) VALUES
-- 1楼座位
(1, 'A-01', 1, 1, 1, 2, '["插座", "台灯"]'),
(1, 'A-02', 1, 1, 2, 1, '[]'),
(1, 'A-03', 1, 1, 3, 2, '["插座"]'),
(1, 'B-01', 1, 2, 1, 3, '["隔板", "静音区"]'),
(1, 'B-02', 1, 2, 2, 3, '["隔板", "静音区"]'),
-- 2楼座位
(1, 'C-01', 2, 1, 1, 4, '["小组桌", "白板"]'),
(1, 'C-02', 2, 1, 2, 4, '["小组桌", "插座"]');

-- 插入图书馆座位数据 (科技分馆示例)
INSERT IGNORE INTO `library_seat` (`library_id`, `seat_label`, `floor`, `row_num`, `col_num`, `seat_type`) VALUES
(2, 'S-01', 1, 1, 1, 2),
(2, 'S-02', 1, 1, 2, 1),
(2, 'S-03', 2, 1, 1, 3);

-- 插入课程数据
INSERT IGNORE INTO `course` (`classroom_id`, `course_name`, `teacher_name`, `week_day`, `start_time`, `end_time`, `start_week`, `end_week`) VALUES
(1, '高等数学', '王老师', 2, '08:00:00', '09:40:00', 1, 16),
(3, '小组讨论-数据结构', '李老师', 4, '10:00:00', '11:40:00', 1, 8);

-- 插入预约记录 (预约教室)
INSERT IGNORE INTO `reservation` (`user_id`, `resource_type`, `classroom_id`, `reservation_date`, `start_time`, `end_time`, `duration`, `purpose`, `status`) VALUES
(1, 1, 2, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00:00', '16:00:00', 120, '自习', 1);

-- 插入预约记录 (预约图书馆座位)
INSERT IGNORE INTO `reservation` (`user_id`, `resource_type`, `library_seat_id`, `reservation_date`, `start_time`, `end_time`, `duration`, `purpose`, `status`) VALUES
(2, 2, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '19:00:00', '21:00:00', 120, '复习备考', 2);

-- 插入报修工单
INSERT IGNORE INTO `repair` (`user_id`, `resource_type`, `classroom_id`, `title`, `description`, `type`, `priority`) VALUES
(1, 1, 1, '101教室投影仪无法开启', '下午上课时发现投影仪无法启动。', 4, 1);

-- 插入评价
INSERT IGNORE INTO `review` (`user_id`, `resource_type`, `classroom_id`, `reservation_id`, `score`, `content`, `tags`) VALUES
(1, 1, 3, 1, 5, '研讨室很安静，设备齐全，适合小组讨论。', '["安静", "设备好"]');

-- 插入组队需求
INSERT IGNORE INTO `team_request` (`user_id`, `title`, `description`, `expected_count`, `current_count`) VALUES
(1, '寻找数据结构课程学习伙伴', '希望找2-3位同学一起学习和完成数据结构作业。', 3, 1);

-- 插入组队成员
INSERT IGNORE INTO `team_member` (`team_request_id`, `user_id`, `role`) VALUES
(1, 1, 1);

-- 插入学习计划
INSERT IGNORE INTO `study_plan` (`team_request_id`, `user_id`, `reservation_id`, `title`, `plan_date`, `start_time`, `end_time`) VALUES
(1, 1, NULL, '第三章树和图的学习', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '15:00:00', '17:00:00');

-- 插入通知消息
INSERT IGNORE INTO `notification` (`user_id`, `type`, `title`, `content`, `related_id`) VALUES
(1, 2, '预约即将开始提醒', '您预约的教室将在30分钟后开始，请准时签到。', 1);

SELECT '测试数据插入完成。' AS result;