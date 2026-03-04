# 校园学习空间综合服务平台

本仓库包含**前端（Vue 3 + Vite + Vant）**与**后端（Spring Boot + MyBatis-Plus + MySQL）**两个子项目。

- 前端目录：`frontend/`
- 后端目录：`backend/`

## 快速开始

### 后端启动

1. 准备 MySQL，并执行初始化脚本：`backend/src/main/resources/sql/init.sql`
2. 修改后端配置：`backend/src/main/resources/application.yml`（数据库账号密码等）
3. 启动 Spring Boot 主类：`backend/src/main/java/com/campus/learningspace/CampusLearningSpaceApplication.java`

### 前端启动

1. 进入 `frontend/`
2. 安装依赖并启动：

```sh
npm install
npm run dev
```

## “我的”页面（个人中心）更新说明

### 页面结构

“我的”页已改为**导航页 + 多子页面跳转**模式：

- 入口页：`/profile`（展示竖向卡片导航 + 底部退出登录按钮）
- 子页面：
  - `/profile/info`：我的信息（基本信息）
  - `/profile/reservations`：我的预约
  - `/profile/repairs`：我的报修
  - `/profile/reviews`：我的评价
  - `/profile/teams`：我的团队
  - `/profile/plans`：学习计划

说明：
- 每个入口项点击后会**跳转到对应页面**，不再使用弹窗承载列表与信息。
- 子页面顶部带返回按钮，返回到 `/profile`。
- **退出登录**按钮仍在 `/profile` 最底部，退出登录逻辑保持不变（清理本地 `currentUser` 并回到未登录态）。

### 前端相关文件

- 导航页：`frontend/src/views/ProfileView.vue`
- 子页面：
  - `frontend/src/views/ProfileInfoView.vue`
  - `frontend/src/views/ProfileReservationView.vue`
  - `frontend/src/views/ProfileRepairView.vue`
  - `frontend/src/views/ProfileReviewView.vue`
  - `frontend/src/views/ProfileTeamView.vue`
  - `frontend/src/views/ProfilePlanView.vue`
- 路由配置：`frontend/src/router/index.ts`

### 后端接口补充（用于子页面详情展示）

除列表接口外，后端补充了按 `id` 获取详情的接口，供子页面点击记录时加载详情（前端会优先请求详情，失败时用列表项数据兜底）：

- 预约
  - `GET /api/reservation/user/{userId}`：用户预约列表
  - `GET /api/reservation/{id}`：预约详情
- 报修
  - `GET /api/repair/user/{userId}`：用户报修列表
  - `GET /api/repair/{id}`：报修详情
- 评价
  - `GET /api/review/user/{userId}`：用户评价列表
  - `GET /api/review/{id}`：评价详情
- 团队
  - `GET /api/team/user/{userId}`：用户团队列表
  - `GET /api/team/request/{id}`：团队详情
- 学习计划
  - `GET /api/study-plan/user/{userId}`：用户学习计划列表
  - `GET /api/study-plan/{id}`：学习计划详情

