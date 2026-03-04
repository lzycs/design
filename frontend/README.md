# 前端项目（Vue 3 + Vite + Vant）

## 启动方式

进入 `frontend/` 目录后执行：

```sh
npm install
npm run dev
```

## 路由说明

底部 TabBar：
- `/`：首页
- `/reservation`：预约
- `/profile`：我的（个人中心入口页）

个人中心（“我的”）相关路由：
- `/profile`：个人中心导航页（竖向卡片导航 + 底部“退出登录”按钮）
- `/profile/info`：我的信息（基本信息详情页）
- `/profile/reservations`：我的预约
- `/profile/repairs`：我的报修
- `/profile/reviews`：我的评价
- `/profile/teams`：我的团队
- `/profile/plans`：学习计划

## “我的”页面说明

### 页面结构

`/profile` 只作为导航入口，点击各菜单项后跳转到对应页面展示内容，不使用弹窗承载列表。

### 退出登录

退出登录按钮位于 `/profile` 页面底部，逻辑保持不变：
- 清理本地 `localStorage.currentUser`
- 清空页面内状态并回到未登录态

## 相关文件

- 路由：`src/router/index.ts`
- 个人中心入口：`src/views/ProfileView.vue`
- 个人中心子页面：
  - `src/views/ProfileInfoView.vue`
  - `src/views/ProfileReservationView.vue`
  - `src/views/ProfileRepairView.vue`
  - `src/views/ProfileReviewView.vue`
  - `src/views/ProfileTeamView.vue`
  - `src/views/ProfilePlanView.vue`

