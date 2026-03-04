# 校园学习空间综合服务平台 - 后端项目

## 项目概述

基于实时状态的校园学习空间综合服务平台后端项目，使用 Spring Boot 3.2.0 + MyBatis-Plus + MySQL 8.0 技术栈。

## 技术栈

- **JDK**: 21
- **Spring Boot**: 3.2.0
- **MyBatis-Plus**: 3.5.5
- **MySQL**: 8.0.44
- **Redis**: 用于缓存
- **Lombok**: 简化代码

## 项目结构

```
campus-learning-space/
├── src/main/java/com/campus/learningspace/
│   ├── CampusLearningSpaceApplication.java    # 启动类
│   ├── common/                                 # 通用类
│   │   └── Result.java                        # 统一响应结果
│   ├── config/                                 # 配置类
│   │   ├── CorsConfig.java                    # 跨域配置
│   │   └── MyMetaObjectHandler.java           # 自动填充处理器
│   ├── controller/                             # 控制器层
│   │   ├── BuildingController.java
│   │   ├── ClassroomController.java
│   │   └── ReservationController.java
│   ├── entity/                                 # 实体类
│   │   ├── User.java
│   │   ├── Building.java
│   │   ├── Classroom.java
│   │   ├── Course.java
│   │   ├── Reservation.java
│   │   ├── Repair.java
│   │   ├── Review.java
│   │   ├── TeamRequest.java
│   │   ├── TeamMember.java
│   │   ├── StudyPlan.java
│   │   └── Notification.java
│   ├── mapper/                                 # 数据访问层
│   │   └── *Mapper.java
│   ├── service/                                # 业务逻辑层
│   │   ├── BuildingService.java
│   │   ├── ClassroomService.java
│   │   └── ReservationService.java
│   └── task/                                   # 定时任务
│       └── ReservationTask.java
├── src/main/resources/
│   ├── application.yml                        # 配置文件
│   └── sql/
│       └── init.sql                           # 数据库初始化脚本
└── pom.xml                                    # Maven配置
```

## 快速开始

### 1. 数据库配置

1. 确保已安装 MySQL 8.0.44
2. 使用 Navicat 17 或其他工具执行 `src/main/resources/sql/init.sql` 脚本创建数据库和表
3. 修改 `application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_learning_space?...
    username: root
    password: 你的密码
```

### 2. Redis配置（可选）

如果需要使用Redis缓存功能，请确保Redis服务已启动，并修改配置：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### 3. 运行项目

1. 使用 IntelliJ IDEA 打开项目
2. 等待 Maven 依赖下载完成
3. 运行 `CampusLearningSpaceApplication.java` 主类
4. 项目启动后访问 http://localhost:8080

## 数据库表说明

| 表名 | 说明 |
|------|------|
| user | 用户表 |
| building | 教学楼表 |
| classroom | 教室表 |
| course | 课程表 |
| reservation | 预约记录表 |
| repair | 报修工单表 |
| review | 评价表 |
| team_request | 组队需求表 |
| team_member | 组队成员表 |
| study_plan | 学习计划表 |
| notification | 通知消息表 |

## API接口示例

### 教学楼相关
- `GET /api/building/list` - 获取所有教学楼列表
- `GET /api/building/{id}` - 获取教学楼详情

### 教室相关
- `GET /api/classroom/list` - 获取所有教室
- `GET /api/classroom/building/{buildingId}` - 按教学楼获取教室
- `GET /api/classroom/available` - 获取可用教室

### 预约相关
- `GET /api/reservation/user/{userId}` - 获取用户预约列表
- `GET /api/reservation/classroom/{classroomId}?date=2024-01-01` - 获取教室某日期的预约

### 个人中心（“我的”）相关

为配合前端“我的”页面改为多子页面跳转，并支持点击列表项查看详情，补充了以下详情接口：

- **预约**
  - `GET /api/reservation/{id}` - 获取预约详情
- **报修**
  - `GET /api/repair/user/{userId}` - 获取用户报修列表
  - `GET /api/repair/{id}` - 获取报修详情
- **评价**
  - `GET /api/review/user/{userId}` - 获取用户评价列表
  - `GET /api/review/{id}` - 获取评价详情
- **团队**
  - `GET /api/team/user/{userId}` - 获取用户团队列表
  - `GET /api/team/request/{id}` - 获取团队详情（TeamRequest）
- **学习计划**
  - `GET /api/study-plan/user/{userId}` - 获取用户学习计划列表
  - `GET /api/study-plan/{id}` - 获取学习计划详情

## 初始测试数据

脚本中已包含以下测试用户：
- 管理员: admin / password
- 学生1: student1 / password
- 学生2: student2 / password
- 后勤人员: maintainer / password

## 下一步开发

- 实现用户认证与授权（Spring Security）
- 完善预约冲突检测和时间冲突判断
- 实现二维码生成和签到功能
- 添加WebSocket实时通知
- 实现文件上传功能（报修照片）
- 添加单元测试和集成测试
