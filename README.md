## 校园学习空间综合服务平台

面向高校场景的学习空间平台，提供预约、报修、评价、协作组队、学习计划和后台管理等能力。

- 前端：Vue 3 + Vite + TypeScript + Vant
- 后端：Spring Boot + MyBatis-Plus + MySQL
- 构建：npm（前端）、Maven（后端）

---

## 当前功能（按模块）

### 1) 学习空间预约

- 支持普通教室、研讨室、图书馆座位预约。
- 预约页支持日期切换（今天/明天/后天）和时间段选择。
- 教室详情支持连续时段合并预约，并根据预约规则校验时长上限。
- 预约相关状态流转：待签到、已签到、已完成、已取消等。

### 2) 教室评价体系

- 仅展示“审核通过”的教室评价。
- 教室详情页在“综合评分”后提供 `查看全部评价` 入口。
- 新增“全部评价页”（路由：`/reservation/classroom/:id/reviews`），只展示当前教室已审核评价。
- 无评价时入口仍显示，点击给出“暂无评价”提示，不跳转。

### 3) 报修与反馈

- 用户可提交教室报修单，查看个人报修记录。
- 后台支持报修单处理与状态更新。
- 评价与反馈走审核流程，前台展示受审核状态控制。

### 4) 协作广场与小组审批

- 协作广场可浏览小组、发起小组、查看成员并进入小组聊天。
- 用户可申请加入小组（支持加入多个小组，不做总数量限制）。
- 同一用户对同一小组“待审批”申请不可重复提交。
- 组长可在“我的协作-审核页”审批申请（通过/拒绝，拒绝原因可选）。
- 审批后状态会同步到申请人侧，并在“我的协作”入口显示红点/角标。

### 5) 学习计划

- 支持创建学习计划并关联小组。
- 组长可对计划执行“关联研讨室”，并配置关键时间节点。
- 学习计划页支持筛选（全部、进行中、已完成、已过期、我的创建）。
- 说明：当前页面已移除无效的“编辑/查看详情”按钮，仅保留可用操作。

### 6) 个人中心与后台

- 个人中心包含：我的预约、我的评价、我的报修、我的协作等入口。
- 后台支持：登录、报修管理、评价审核、教室/课程/教学楼管理、预约上限配置等。

---

## 关键前端路由

- `/reservation`：预约中心
- `/reservation/classroom/:id`：教室详情
- `/reservation/classroom/:id/reviews`：全部评价页
- `/collaboration`：协作广场
- `/profile`：个人中心
- `/profile/teams`：我的协作（含申请审核页）
- `/shared-plan`：学习计划
- `/admin/login`、`/admin/*`：后台相关页面

---

## 项目结构

```text
TestA/
├── backend/
│   ├── src/main/java/com/campus/learningspace/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── mapper/
│   │   ├── entity/
│   │   └── common/
│   ├── src/main/resources/sql/init.sql
│   └── pom.xml
├── frontend/
│   ├── src/views/
│   ├── src/api/
│   ├── src/router/
│   └── package.json
└── README.md
```

---

## 本地启动

### 1. 后端

1) 准备环境：JDK 21、MySQL 8.x  
2) 执行数据库脚本：`backend/src/main/resources/sql/init.sql`  
3) 配置 `backend/src/main/resources/application.yml` 数据库连接  
4) 启动：

```bash
cd backend
mvn spring-boot:run
```

后端默认端口：`8080`

### 2. 前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

---

## 测试账号（init.sql 内置）

- 学生：`zhangsan / 123456`、`lisi / 123456`、`wangwu / 123456`
- 教师：`wanglaoshi / 123456`
- 管理员：`admin / 123456`
- 维修人员：`zhangshifu / 123456`
- 评论审核员：`commentadmin / 123456`

---

## 开发约定（简要）

- 前端统一使用 `src/api/*` 封装接口，页面在 `src/views/*`。
- 登录态通过 `localStorage.currentUser` 维护。
- 后端统一返回 `Result<T>`，数据表普遍使用逻辑删除字段 `deleted`。

