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
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    UNIQUE KEY uk_building_name (`name`)
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
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    UNIQUE KEY uk_library_name (`name`)
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
    UNIQUE KEY uk_classroom_room (`building_id`, `room_number`),
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

-- 标准时间段表（用于前端展示每个教室的可预约时间段）
CREATE TABLE IF NOT EXISTS `time_slot` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '时间段ID',
    `label` VARCHAR(50) NOT NULL COMMENT '时间段标签，如 8:00-10:00',
    `start_time` TIME NOT NULL COMMENT '开始时间',
    `end_time` TIME NOT NULL COMMENT '结束时间',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准预约时间段表';

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
    `images` MEDIUMTEXT COMMENT '图片路径(JSON数组，可存 base64)',
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

-- 若表已存在且 images 为 TEXT，改为 MEDIUMTEXT 以支持 base64 图片（避免 500 错误）
ALTER TABLE `repair` MODIFY COLUMN `images` MEDIUMTEXT COMMENT '图片路径(JSON数组，可存 base64)';

-- 评价表 (可评价教室或图书馆座位) - 供“我的评价”等通用场景使用
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

-- 教室反馈表（用于反馈页：环境评分、设备评分、评价内容等）
CREATE TABLE IF NOT EXISTS `classroom_feedback` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '反馈ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `classroom_id` BIGINT NOT NULL COMMENT '教室ID',
    `reservation_id` BIGINT COMMENT '关联预约ID（可选）',
    `env_score` TINYINT NULL COMMENT '整体环境评分 1-5（待评价时可为空）',
    `equip_score` TINYINT NULL COMMENT '设备设施评分 1-5（待评价时可为空）',
    `content` TEXT COMMENT '评价内容',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-待评价(预留), 2-已评价',
    `used_start_time` DATETIME COMMENT '使用开始时间',
    `used_end_time` DATETIME COMMENT '使用结束时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_user_id (`user_id`),
    INDEX idx_classroom_id (`classroom_id`),
    INDEX idx_status (`status`),
    UNIQUE KEY uk_user_reservation (`user_id`, `reservation_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`classroom_id`) REFERENCES `classroom`(`id`),
    FOREIGN KEY (`reservation_id`) REFERENCES `reservation`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教室反馈表';

-- 兼容已存在表结构：确保待评价时评分字段允许为空
ALTER TABLE `classroom_feedback` MODIFY COLUMN `env_score` TINYINT NULL;
ALTER TABLE `classroom_feedback` MODIFY COLUMN `equip_score` TINYINT NULL;

-- 学习计划表：添加关键时间节点字段（若已存在可忽略）
SET @sp_kn_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'study_plan' AND COLUMN_NAME = 'key_time_nodes');
SET @sp_kn_sql := IF(@sp_kn_exists = 0, 'ALTER TABLE `study_plan` ADD COLUMN `key_time_nodes` TEXT COMMENT ''关键时间节点''', 'SELECT 1');
PREPARE sp_kn_stmt FROM @sp_kn_sql;
EXECUTE sp_kn_stmt;
DEALLOCATE PREPARE sp_kn_stmt;

-- 组队需求表
CREATE TABLE IF NOT EXISTS `team_request` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '需求ID',
    `user_id` BIGINT NOT NULL COMMENT '发布人ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `description` TEXT COMMENT '描述',
    `tags` VARCHAR(255) COMMENT '标签(JSON数组)',
    `expected_count` INT NOT NULL COMMENT '期望人数',
    `current_count` INT NOT NULL DEFAULT 1 COMMENT '当前人数',
    `start_time` DATETIME COMMENT '小组开始时间',
    `end_time` DATETIME COMMENT '小组结束时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-已关闭, 1-招募中, 2-已满员',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_user_id (`user_id`),
    INDEX idx_status (`status`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组队需求表';

-- 兼容已存在表结构：补齐小组时间范围字段
SET @__team_start_time_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'team_request'
    AND COLUMN_NAME = 'start_time'
);
SET @__team_start_time_sql := IF(
  @__team_start_time_exists = 0,
  'ALTER TABLE `team_request` ADD COLUMN `start_time` DATETIME COMMENT ''小组开始时间''',
  'SELECT 1'
);
PREPARE __stmt FROM @__team_start_time_sql;
EXECUTE __stmt;
DEALLOCATE PREPARE __stmt;

SET @__team_end_time_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'team_request'
    AND COLUMN_NAME = 'end_time'
);
SET @__team_end_time_sql := IF(
  @__team_end_time_exists = 0,
  'ALTER TABLE `team_request` ADD COLUMN `end_time` DATETIME COMMENT ''小组结束时间''',
  'SELECT 1'
);
PREPARE __stmt2 FROM @__team_end_time_sql;
EXECUTE __stmt2;
DEALLOCATE PREPARE __stmt2;

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

-- 组队消息表（用于小组聊天记录）
CREATE TABLE IF NOT EXISTS `team_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    `team_request_id` BIGINT NOT NULL COMMENT '组队需求ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者用户ID',
    `sender_name` VARCHAR(100) COMMENT '发送者名称（冗余）',
    `type` TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型: 1-文本, 2-文件',
    `content` TEXT COMMENT '消息内容或文件URL',
    `file_name` VARCHAR(255) COMMENT '文件名',
    `file_size` BIGINT COMMENT '文件大小（字节）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_team_request_id (`team_request_id`),
    INDEX idx_sender_id (`sender_id`),
    FOREIGN KEY (`team_request_id`) REFERENCES `team_request`(`id`),
    FOREIGN KEY (`sender_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组队消息表';

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
    `key_time_nodes` TEXT COMMENT '关键时间节点，格式：节点名|日期时间,节点名|日期时间',
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

-- 插入用户数据（含维修人员：张师傅 role=4 后勤人员）
INSERT IGNORE INTO `user` (`username`, `password`, `real_name`, `student_id`, `email`, `phone`, `role`, `status`) VALUES
('zhangsan', '123456', '张三', '20220001', 'zhangsan@example.com', '13800138001', 1, 1),
('lisi', '123456', '李四', '20220002', 'lisi@example.com', '13800138002', 1, 1),
('wanglaoshi', '123456', '王老师', NULL, 'wang@edu.com', '13800138003', 2, 1),
('admin', '123456', '管理员', NULL, 'admin@system.com', '13800138000', 3, 1),
('zhangshifu', '123456', '张师傅', NULL, NULL, '13800138000', 4, 1),
('wangwu', '123456', '王五', NULL, NULL, '13800138006', 1, 1);

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
-- 注意：course/time_slot/reservation 等表默认无唯一约束且未指定 id，会导致每次启动重复插入
INSERT INTO `course` (`id`, `classroom_id`, `course_name`, `teacher_name`, `week_day`, `start_time`, `end_time`, `start_week`, `end_week`) VALUES
(1, 1, '高等数学', '王老师', 2, '08:00:00', '09:40:00', 1, 16),
(2, 3, '小组讨论-数据结构', '李老师', 4, '10:00:00', '11:40:00', 1, 8)
ON DUPLICATE KEY UPDATE
  `classroom_id` = VALUES(`classroom_id`),
  `course_name`  = VALUES(`course_name`),
  `teacher_name` = VALUES(`teacher_name`),
  `week_day`     = VALUES(`week_day`),
  `start_time`   = VALUES(`start_time`),
  `end_time`     = VALUES(`end_time`),
  `start_week`   = VALUES(`start_week`),
  `end_week`     = VALUES(`end_week`);

-- 插入标准时间段数据
INSERT INTO `time_slot` (`id`, `label`, `start_time`, `end_time`, `sort_order`) VALUES
(1, '8:00-10:00',  '08:00:00', '10:00:00', 1),
(2, '10:00-12:00', '10:00:00', '12:00:00', 2),
(3, '12:00-14:00', '12:00:00', '14:00:00', 3),
(4, '14:00-16:00', '14:00:00', '16:00:00', 4),
(5, '16:00-18:00', '16:00:00', '18:00:00', 5)
ON DUPLICATE KEY UPDATE
  `label`      = VALUES(`label`),
  `start_time` = VALUES(`start_time`),
  `end_time`   = VALUES(`end_time`),
  `sort_order` = VALUES(`sort_order`);

-- 插入预约记录 (预约教室)
INSERT INTO `reservation` (
  `id`, `user_id`, `resource_type`, `classroom_id`, `reservation_date`,
  `start_time`, `end_time`, `duration`, `purpose`, `status`, `checkin_time`
) VALUES
-- 张三：待签到（教室）
(1, 1, 1, 2, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00:00', '16:00:00', 120, '自习', 1, NULL),
-- 张三：已签到（教室）
(2, 1, 1, 1, CURDATE(), '08:00:00', '10:00:00', 120, '早自习', 2, DATE_ADD(NOW(), INTERVAL -10 MINUTE)),
-- 张三：已完成（教室）
(3, 1, 1, 3, DATE_ADD(CURDATE(), INTERVAL -1 DAY), '10:00:00', '12:00:00', 120, '小组讨论', 3, DATE_ADD(NOW(), INTERVAL -1 DAY)),
-- 张三：已取消（教室）
(4, 1, 1, 4, DATE_ADD(CURDATE(), INTERVAL -2 DAY), '16:00:00', '18:00:00', 120, '临时有事取消', 4, NULL),
-- 李四：待签到（教室）
(5, 2, 1, 6, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '14:00:00', '16:00:00', 120, '会议', 1, NULL)
ON DUPLICATE KEY UPDATE
  `user_id`          = VALUES(`user_id`),
  `resource_type`    = VALUES(`resource_type`),
  `classroom_id`     = VALUES(`classroom_id`),
  `reservation_date` = VALUES(`reservation_date`),
  `start_time`       = VALUES(`start_time`),
  `end_time`         = VALUES(`end_time`),
  `duration`         = VALUES(`duration`),
  `purpose`          = VALUES(`purpose`),
  `status`           = VALUES(`status`),
  `checkin_time`     = VALUES(`checkin_time`);

-- 插入预约记录 (预约图书馆座位)
INSERT INTO `reservation` (
  `id`, `user_id`, `resource_type`, `library_seat_id`, `reservation_date`,
  `start_time`, `end_time`, `duration`, `purpose`, `status`, `checkin_time`
) VALUES
-- 李四：已签到（图书馆座位）
(6, 2, 2, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '19:00:00', '21:00:00', 120, '复习备考', 2, DATE_ADD(NOW(), INTERVAL -30 MINUTE)),
-- 李四：已完成（图书馆座位）
(7, 2, 2, 2, DATE_ADD(CURDATE(), INTERVAL -1 DAY), '08:00:00', '12:00:00', 240, '图书馆自习', 3, DATE_ADD(NOW(), INTERVAL -1 DAY)),
-- 李四：已取消（图书馆座位）
(8, 2, 2, 3, DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:00:00', '16:00:00', 120, '行程变更', 4, NULL),
-- 张三：已违约（图书馆座位）
(9, 1, 2, 4, DATE_ADD(CURDATE(), INTERVAL -4 DAY), '10:00:00', '12:00:00', 120, '未按时到场', 5, NULL)
ON DUPLICATE KEY UPDATE
  `user_id`          = VALUES(`user_id`),
  `resource_type`    = VALUES(`resource_type`),
  `library_seat_id`  = VALUES(`library_seat_id`),
  `reservation_date` = VALUES(`reservation_date`),
  `start_time`       = VALUES(`start_time`),
  `end_time`         = VALUES(`end_time`),
  `duration`         = VALUES(`duration`),
  `purpose`          = VALUES(`purpose`),
  `status`           = VALUES(`status`),
  `checkin_time`     = VALUES(`checkin_time`);

-- 插入报修工单（与“我的报修”页面对应：待处理/处理中/已完成各一条，类型 1-照明 2-空调 3-桌椅 4-多媒体 5-网络 6-其他，状态 1-待处理 2-处理中 3-已修复）
INSERT INTO `repair` (`id`, `user_id`, `resource_type`, `classroom_id`, `title`, `description`, `type`, `priority`, `status`, `handler_id`, `handle_time`, `create_time`) VALUES
(1, 1, 1, 1, '第一教学楼-101', '教室投影仪无法正常开启，开机后屏幕显示蓝屏，影响正常教学使用，请尽快处理。', 4, 2, 1, NULL, NULL, '2026-03-05 09:15:00'),
(2, 1, 1, 3, '第一教学楼-201A', '研讨室其中一把椅子的靠背松动，存在安全隐患，需要加固或更换。', 3, 2, 2, 5, NULL, '2026-03-04 16:30:00'),
(3, 1, 1, 4, '第二教学楼-202B', '教室空调遥控器失灵，无法调节温度，现在天气较热，影响学习环境。', 2, 2, 3, 5, '2026-03-03 17:45:00', '2026-03-03 14:20:00')
ON DUPLICATE KEY UPDATE
  `user_id`       = VALUES(`user_id`),
  `resource_type` = VALUES(`resource_type`),
  `classroom_id`  = VALUES(`classroom_id`),
  `title`         = VALUES(`title`),
  `description`   = VALUES(`description`),
  `type`          = VALUES(`type`),
  `priority`      = VALUES(`priority`),
  `status`        = VALUES(`status`),
  `handler_id`    = VALUES(`handler_id`),
  `handle_time`   = VALUES(`handle_time`),
  `create_time`   = VALUES(`create_time`);

-- 插入评价
INSERT INTO `review` (`id`, `user_id`, `resource_type`, `classroom_id`, `reservation_id`, `score`, `content`, `tags`) VALUES
(1, 1, 1, 3, 1, 5, '研讨室很安静，设备齐全，适合小组讨论。', '["安静", "设备好"]')
ON DUPLICATE KEY UPDATE
  `user_id`        = VALUES(`user_id`),
  `resource_type`  = VALUES(`resource_type`),
  `classroom_id`   = VALUES(`classroom_id`),
  `reservation_id` = VALUES(`reservation_id`),
  `score`          = VALUES(`score`),
  `content`        = VALUES(`content`),
  `tags`           = VALUES(`tags`);

-- 插入组队需求（与「我的协作」页面对应：项目需求评审、UI设计稿确认、接口联调测试；status 0-已关闭 1-招募中 2-已满员，前端展示为 进行中/已完成）
-- 使用 ON DUPLICATE KEY UPDATE 避免 REPLACE 删除行时触发 study_plan 外键约束
INSERT INTO `team_request` (`id`, `user_id`, `title`, `description`, `tags`, `expected_count`, `current_count`, `start_time`, `end_time`, `status`, `create_time`, `update_time`) VALUES
(1, 1, '项目需求评审', '完成项目需求文档评审，确定核心功能和优先级，输出评审意见和修改建议', NULL, 5, 3, '2026-03-10 10:00:00', '2026-03-20 18:00:00', 1, '2026-03-10 10:00:00', '2026-03-10 12:00:00'),
(2, 2, 'UI设计稿确认', '确认首页、详情页、表单页的UI设计稿，反馈修改意见，最终定稿', NULL, 3, 3, '2026-03-08 10:00:00', '2026-03-12 18:00:00', 0, '2026-03-08 10:00:00', '2026-03-08 12:00:00'),
(3, 2, '接口联调测试', '前后端接口联调，测试各接口的可用性和数据准确性，修复联调中发现的问题', NULL, 4, 2, '2026-03-09 10:00:00', '2026-03-18 18:00:00', 1, '2026-03-09 10:00:00', '2026-03-09 12:00:00'),
-- 协作广场示例小组：考研英语复习 / XX项目开发 / 数据库学习
(10, 1, '考研英语复习', '每天背单词 + 真题精读，互相打卡监督，目标上岸！', '["考研","英语","真题","打卡"]', 6, 2, '2026-03-06 09:00:00', '2026-06-01 18:00:00', 1, '2026-03-06 09:00:00', '2026-03-06 09:00:00'),
(11, 2, 'XX项目开发', '一起完成“校园学习空间”项目，分工前后端/测试/文档，定期开会同步进度。', '["项目开发","前端","后端","联调"]', 5, 5, '2026-03-06 14:00:00', '2026-04-10 18:00:00', 2, '2026-03-06 14:00:00', '2026-03-07 18:00:00'),
(12, 6, '数据库学习', '从 SQL 基础到索引与事务，一起刷题复盘，每周至少 2 次讨论。', '["数据库","SQL","MySQL","刷题"]', 4, 2, '2026-03-07 10:00:00', '2026-05-01 18:00:00', 1, '2026-03-07 10:00:00', '2026-03-07 10:00:00')
ON DUPLICATE KEY UPDATE
  `user_id` = VALUES(`user_id`),
  `title` = VALUES(`title`),
  `description` = VALUES(`description`),
  `tags` = VALUES(`tags`),
  `expected_count` = VALUES(`expected_count`),
  `current_count` = VALUES(`current_count`),
  `start_time` = VALUES(`start_time`),
  `end_time` = VALUES(`end_time`),
  `status` = VALUES(`status`),
  `create_time` = VALUES(`create_time`),
  `update_time` = VALUES(`update_time`);

-- 插入组队成员（1=创建者 2=成员；项目需求评审：张三/李四/王五；UI设计稿确认：李四/张三/王五；接口联调测试：李四/张三）
INSERT INTO `team_member` (`id`, `team_request_id`, `user_id`, `role`, `join_time`) VALUES
(1, 1, 1, 1, '2026-03-10 10:00:00'),
(2, 1, 2, 2, '2026-03-10 10:30:00'),
(3, 1, 6, 2, '2026-03-10 11:00:00'),
(4, 2, 2, 1, '2026-03-08 10:00:00'),
(5, 2, 1, 2, '2026-03-08 10:30:00'),
(6, 2, 6, 2, '2026-03-08 11:00:00'),
(7, 3, 2, 1, '2026-03-09 10:00:00'),
(8, 3, 1, 2, '2026-03-09 10:30:00'),
-- 协作广场示例小组成员
(101, 10, 1, 1, '2026-03-06 09:00:00'),
(102, 10, 2, 2, '2026-03-06 09:10:00'),
(111, 11, 2, 1, '2026-03-06 14:00:00'),
(112, 11, 1, 2, '2026-03-06 14:20:00'),
(113, 11, 6, 2, '2026-03-06 14:30:00'),
(114, 11, 3, 2, '2026-03-06 14:40:00'),
(115, 11, 4, 2, '2026-03-06 14:50:00'),
(121, 12, 6, 1, '2026-03-07 10:00:00'),
(122, 12, 1, 2, '2026-03-07 10:15:00')
ON DUPLICATE KEY UPDATE
  `team_request_id` = VALUES(`team_request_id`),
  `user_id`         = VALUES(`user_id`),
  `role`            = VALUES(`role`),
  `join_time`       = VALUES(`join_time`);

-- 插入小组聊天消息（接口联调测试 id=3）
INSERT INTO `team_message` (`id`, `team_request_id`, `sender_id`, `sender_name`, `type`, `content`) VALUES
(1, 3, 2, '李四', 1, '大家好，本周三下午3点研讨室讨论项目需求，记得准时参加！'),
(2, 3, 2, '李四', 1, '我整理了一些SpringBoot的学习资料，等下发给大家'),
(3, 3, 2, '李四', 2, 'SpringBoot教程.pdf'),
(4, 3, 1, '张三', 1, '收到！我这边负责前端部分，已经开始搭建框架了')
ON DUPLICATE KEY UPDATE
  `team_request_id` = VALUES(`team_request_id`),
  `sender_id`       = VALUES(`sender_id`),
  `sender_name`     = VALUES(`sender_name`),
  `type`            = VALUES(`type`),
  `content`         = VALUES(`content`);

-- 共享计划关联的研讨室预约（reservation_id 10、11 供 study_plan 使用，需在 study_plan 之前插入）
INSERT INTO `reservation` (
  `id`, `user_id`, `resource_type`, `classroom_id`, `reservation_date`,
  `start_time`, `end_time`, `duration`, `purpose`, `status`
) VALUES
(10, 2, 1, 3, DATE_ADD(CURDATE(), INTERVAL 4 DAY), '14:00:00', '16:00:00', 120, '共享学习计划-研讨室', 1),
(11, 6, 1, 4, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '10:00:00', '12:00:00', 120, '共享学习计划-研讨室', 1)
ON DUPLICATE KEY UPDATE
  `user_id`          = VALUES(`user_id`),
  `resource_type`    = VALUES(`resource_type`),
  `classroom_id`     = VALUES(`classroom_id`),
  `reservation_date` = VALUES(`reservation_date`),
  `start_time`       = VALUES(`start_time`),
  `end_time`         = VALUES(`end_time`),
  `duration`         = VALUES(`duration`),
  `purpose`          = VALUES(`purpose`),
  `status`           = VALUES(`status`);

-- 插入学习计划（含共享学习计划测试数据，key_time_nodes 格式：节点名|日期时间,节点名|日期时间）
INSERT INTO `study_plan` (`id`, `team_request_id`, `user_id`, `reservation_id`, `title`, `description`, `plan_date`, `start_time`, `end_time`, `key_time_nodes`, `status`) VALUES
(1, 1, 1, NULL, '第三章树和图的学习', NULL, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '15:00:00', '17:00:00', NULL, 1),
(2, 11, 2, 10, 'Java项目实战计划', '完成SpringBoot项目开发，包含用户管理、权限控制等核心模块', DATE_ADD(CURDATE(), INTERVAL 4 DAY), '14:00:00', '16:00:00', '需求分析完成|2026-03-15 18:00,核心功能开发|2026-03-25 18:00,项目测试验收|2026-04-08 18:00', 1),
(3, 10, 1, NULL, '考研英语复习计划', '每天背单词+真题精读，互相打卡监督，目标上岸！', DATE_ADD(CURDATE(), INTERVAL 3 DAY), '09:00:00', '11:00:00', '单词一轮完成|2026-03-20 18:00,真题一刷完成|2026-04-05 18:00', 1),
(4, 12, 6, 11, '数据库学习计划', '从SQL基础到索引与事务，一起刷题复盘，每周至少2次讨论', DATE_ADD(CURDATE(), INTERVAL 5 DAY), '10:00:00', '12:00:00', 'SQL基础完成|2026-03-18 18:00,索引与事务|2026-03-28 18:00', 1),
(5, 10, 2, NULL, '高数真题刷题计划', '完成近10年考研数学真题刷题，整理错题本，针对性复习', DATE_ADD(CURDATE(), INTERVAL -10 DAY), '18:00:00', '20:00:00', '真题一刷|2026-02-25 18:00,错题整理|2026-03-05 18:00', 2)
ON DUPLICATE KEY UPDATE
  `team_request_id` = VALUES(`team_request_id`),
  `user_id`         = VALUES(`user_id`),
  `reservation_id`  = VALUES(`reservation_id`),
  `title`           = VALUES(`title`),
  `description`     = VALUES(`description`),
  `plan_date`       = VALUES(`plan_date`),
  `start_time`      = VALUES(`start_time`),
  `end_time`        = VALUES(`end_time`),
  `key_time_nodes`  = VALUES(`key_time_nodes`),
  `status`          = VALUES(`status`);

-- 插入通知消息
INSERT INTO `notification` (`id`, `user_id`, `type`, `title`, `content`, `related_id`) VALUES
(1, 1, 2, '预约即将开始提醒', '您预约的教室将在30分钟后开始，请准时签到。', 1)
ON DUPLICATE KEY UPDATE
  `user_id`     = VALUES(`user_id`),
  `type`        = VALUES(`type`),
  `title`       = VALUES(`title`),
  `content`     = VALUES(`content`),
  `related_id`  = VALUES(`related_id`);

-- 小组加入申请表
CREATE TABLE IF NOT EXISTS `team_join_application` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '申请ID',
    `team_request_id` BIGINT NOT NULL COMMENT '申请加入的小组ID',
    `applicant_id` BIGINT NOT NULL COMMENT '申请人用户ID',
    `reason` TEXT COMMENT '申请理由',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已拒绝',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    INDEX idx_team_request_id (`team_request_id`),
    INDEX idx_applicant_id (`applicant_id`),
    INDEX idx_status (`status`),
    FOREIGN KEY (`team_request_id`) REFERENCES `team_request`(`id`),
    FOREIGN KEY (`applicant_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小组加入申请表';

-- 插入加入申请测试数据
-- 王五(id=6)申请加入「项目需求评审」(id=1，组长=张三 id=1)
-- 王老师(id=3)申请加入「接口联调测试」(id=3，组长=李四 id=2)
INSERT INTO `team_join_application` (`id`, `team_request_id`, `applicant_id`, `reason`, `status`) VALUES
(1, 1, 6, '我对项目需求分析很感兴趣，希望能加入小组一起学习', 0),
(2, 3, 3, '我是后端开发，有丰富的接口联调经验，希望参与', 0),
(3, 3, 4, '我想学习接口联调的相关知识，请批准我加入', 0)
ON DUPLICATE KEY UPDATE
  `team_request_id` = VALUES(`team_request_id`),
  `applicant_id`    = VALUES(`applicant_id`),
  `reason`          = VALUES(`reason`),
  `status`          = VALUES(`status`);

SELECT '测试数据插入完成。' AS result; 