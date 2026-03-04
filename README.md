## 校园学习空间综合服务平台

本项目是一个面向高校的**学习空间综合服务平台**，包含前后端完整实现，支持教室/研讨室/图书馆座位预约、设施报修、评价反馈、学习小组组队、学习计划管理等功能。

- **前端技术栈**：Vue 3 + Vite + TypeScript + Vant Mobile UI  
- **后端技术栈**：Spring Boot 3.x + MyBatis-Plus + MySQL 8 + Redis（可选）  
- **构建工具**：Maven（后端）、npm（前端）

---

## 目录结构

```text
TestA/
├── backend/                      # 后端工程（Spring Boot）
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/campus/learningspace/
│   │   │   │   ├── CampusLearningSpaceApplication.java
│   │   │   │   ├── common/        # 通用响应等（Result）
│   │   │   │   ├── config/        # 全局配置（MyBatis 自动填充、CORS 等）
│   │   │   │   ├── controller/    # 各业务模块控制器
│   │   │   │   ├── entity/        # 实体类（与数据库表对应）
│   │   │   │   ├── mapper/        # MyBatis-Plus Mapper
│   │   │   │   ├── service/       # 业务服务层
│   │   │   │   └── task/          # 定时任务（如每周清空预约次数）
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── sql/init.sql   # 完整建表+测试数据脚本
│   │   └── test/                  # 预留测试目录
│   └── pom.xml
│
├── frontend/                     # 前端工程（Vue 3 + Vite）
│   ├── src/
│   │   ├── api/                  # 与后端接口对应的 TS API 封装
│   │   ├── components/           # 公共组件（如导航等）
│   │   ├── views/                # 页面视图（首页、预约、个人中心等）
│   │   ├── router/               # vue-router 配置
│   │   ├── utils/                # axios 请求封装等
│   │   └── main.ts               # 应用入口
│   ├── index.html
│   ├── vite.config.ts
│   └── package.json
│
└── README.md                     # 总体说明（本文件）
```

---

## 核心功能概览

### 1. 学习空间预约（教室 / 研讨室 / 图书馆）

- **预约中心页面**（`/reservation`，前端 `ReservationView.vue`）
  - 顶部状态切换：**预约 / 待签到 / 已签到 / 已取消**  
    - 所有数据来自后端 `reservation` 表，通过 `GET /api/reservation/user/{userId}` 获取。
  - 日期选择：**今天 / 明天 / 后天**，基于当前日期动态计算。
  - 资源类型切换：
    - **普通教室**：从 `classroom` 表加载可用教室（`GET /api/classroom/available` type=1）。
    - **研讨室**：type=2。
    - **图书馆**：从 `library` 表加载图书馆信息（`GET /api/library/list`）。
  - **标准时间段展示**（类似 8:00-10:00、10:00-12:00 等）：
    - 后端通过 `time_slot` 表保存全局统一的时间段定义。
    - 接口 `GET /api/reservation/classroom/{classroomId}/slots?date=yyyy-MM-dd`
      - 返回该教室在指定日期、按每个时间段的占用状态（可预约/已占用），前端渲染为多个时间块。
    - 用户点击某个蓝色可预约时间块 → 选中变深蓝 → 再点“立即预约” → 创建一条真实预约记录。
  - **图书馆选座**：
    - 支持按楼层查看座位：`GET /api/library-seat/library/{libraryId}/floor/{floor}`。
    - 座位状态来自 `library_seat` 表（可预约/已预约/停用/维修等）。
    - 用户选中座位后，可以一键预约图书馆座位。

- **后端主要表**
  - `classroom`：教学楼教室信息（楼层、容量、设备等）
  - `library`：图书馆信息
  - `library_seat`：图书馆座位（楼层、行列、类型、实时状态等）
  - `time_slot`：标准预约时间段（统一的时间段配置）
  - `reservation`：预约记录  
    - `resource_type`：1-教室，2-图书馆座位  
    - `classroom_id` / `library_seat_id`  
    - `reservation_date`, `start_time`, `end_time`, `duration`, `status` 等

### 2. 个人中心（我的）

- 路由结构：
  - `/profile`：个人中心导航页（“我的信息 / 我的预约 / 我的报修 / 我的评价 / 我的团队 / 学习计划”）
  - `/profile/info`：我的信息详情（用户名、姓名、学号、邮箱、手机号）
  - `/profile/reservations`：我的预约记录（支持查看详情）
  - `/profile/repairs`：我的报修记录
  - `/profile/reviews`：我的评价记录
  - `/profile/teams`：我的团队
  - `/profile/plans`：学习计划

- 对应后端接口示例：
  - 用户信息：`/api/user/login` 后返回的用户对象 + 本地 `localStorage` 缓存。
  - 我的预约：`GET /api/reservation/user/{userId}`
  - 我的报修：`GET /api/repair/user/{userId}`
  - 我的评价：`GET /api/review/user/{userId}`
  - 我的团队：`GET /api/team/user/{userId}`
  - 学习计划：`GET /api/study-plan/user/{userId}`

- 详情接口（子页面点击记录时调用）：
  - `GET /api/reservation/{id}`
  - `GET /api/repair/{id}`
  - `GET /api/review/{id}`
  - `GET /api/team/request/{id}`
  - `GET /api/study-plan/{id}`

- 退出登录：
  - 前端在 `/profile` 底部提供“退出登录”按钮，清理 `localStorage.currentUser` 并重置内存状态。

### 3. 报修反馈

- 用户可以对教室或图书馆座位提交报修工单：
  - 表：`repair`  
    - `resource_type`：1-教室，2-座位  
    - `classroom_id` / `library_seat_id`
    - `title`，`description`，`type`（照明/空调/桌椅等），`priority`，`status`（待处理/处理中/已修复/已关闭）等。
- 后端接口：
  - `POST /api/repair`：创建报修
  - `GET /api/repair/user/{userId}`：我的报修列表
  - `PUT /api/repair/{id}/status`：运营/后勤人员更新处理状态与备注

### 4. 评价系统

- 对已完成的预约，用户可以提交评价：
  - 表：`review`  
    - 支持教室与图书馆座位（`resource_type`）  
    - 与 `reservation_id` 关联  
    - 评分、内容、标签、多状态（待审核/展示/屏蔽）
- 相关接口：
  - `GET /api/review/user/{userId}`：我的评价
  - `GET /api/review/classroom/{classroomId}`：某教室全部评价
  - `POST /api/review`：创建评价

### 5. 组队与学习计划

- 组队需求（`team_request`）：  
  - 发布/招募学习小组（标题、描述、标签、期望人数、招募状态）。
- 组队成员（`team_member`）：  
  - 记录用户加入的队伍及角色（创建者/成员）。
- 学习计划（`study_plan`）：  
  - 与队伍、预约关联，记录计划的时间段与状态。
- 后端通过 `TeamController`、`StudyPlanController` 提供相应 CRUD 接口，前端在“我的团队”“学习计划”页面展示相关数据。

---

## 启动说明

### 后端（backend）

1. 确保本地已安装：
   - JDK 21
   - MySQL 8.x
2. 在 MySQL 中执行 `backend/src/main/resources/sql/init.sql`：  
   - 创建所有表（`user`, `building`, `classroom`, `library`, `library_seat`, `reservation`, `repair`, `review`, `team_request`, `team_member`, `study_plan`, `notification` 等）。  
   - 自动插入一批测试数据（包括测试用户、样例教室/图书馆/座位、样例预约/报修/评价/组队等）。
3. 修改 `backend/src/main/resources/application.yml` 中的数据库配置：
   - 数据库 URL、用户名、密码。
4. 在 IDE（如 IntelliJ IDEA）中运行 `CampusLearningSpaceApplication`，或命令行：

```sh
cd backend
mvn spring-boot:run
```

5. 后端默认监听 `http://localhost:8080`。

### 前端（frontend）

1. 安装依赖并启动开发服务器：

```sh
cd frontend
npm install
npm run dev
```

2. 默认访问地址通常为 `http://localhost:5173`（以控制台输出为准）。
3. 使用初始化脚本中的测试用户登录（如 `student1 / password`），即可体验预约、报修、评价、组队等完整流程。

---

## 代码风格与约定

### 前端

- 使用 TypeScript + `<script setup>` 语法，组件拆分以页面为主（`views/`），接口统一放在 `api/`。
- 所有与后端交互通过 `src/utils/request.ts` 包装后的 axios 完成，返回统一 Result 结构。
- 登录态缓存在 `localStorage.currentUser`，并在 `ProfileView` / `ReservationView` 中按需读取。

### 后端

- 使用 MyBatis-Plus 进行 CRUD 与查询封装（`ServiceImpl` + `LambdaQueryWrapper`）。
- 所有接口统一使用 `Result<T>` 包裹返回值（`code` / `message` / `data`）。
- 软删除字段使用 `@TableLogic` 管理（`deleted`）。

---

如果后续需要，可以在 README 基础上再补充“技术实现细节”（比如预约冲突算法、签到与二维码逻辑等）。

