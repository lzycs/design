[README.md](https://github.com/user-attachments/files/25599523/README.md)
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

## 初始测试数据

脚本中已包含以下测试用户：
- 管理员: admin / password
- 学生1: student1 / password
- 学生2: student2 / password
- 后勤人员: maintainer / password

## 下一步开发

- 实现用户认证与授权（Spring Security）

- [README.md](https://github.com/user-attachments/files/25599532/README.md)# frontend

This template should help get you started developing with Vue 3 in Vite.

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Recommended Browser Setup

- Chromium-based browsers (Chrome, Edge, Brave, etc.):
  - [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
  - [Turn on Custom Object Formatter in Chrome DevTools](http://bit.ly/object-formatters)
- Firefox:
  - [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - [Turn on Custom Object Formatter in Firefox DevTools](https://fxdx.dev/firefox-devtools-custom-object-formatters/)

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript language service aware of `.vue` types.

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm run test:unit
```

### Run End-to-End Tests with [Playwright](https://playwright.dev)

```sh
# Install browsers for the first run
npx playwright install

# When testing on CI, must build the project first
npm run build

# Runs the end-to-end tests
npm run test:e2e
# Runs the tests only on Chromium
npm run test:e2e -- --project=chromium
# Runs the tests of a specific file
npm run test:e2e -- tests/example.spec.ts
# Runs the tests in debug mode
npm run test:e2e -- --debug
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```


- 完善预约冲突检测和时间冲突判断
- 实现二维码生成和签到功能
- 添加WebSocket实时通知
- 实现文件上传功能（报修照片）
- 添加单元测试和集成测试
