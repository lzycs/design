# 校园学习空间综合服务平台

面向高校场景的学习空间综合服务平台，覆盖学习空间预约、扫码签到、教室评价、报修反馈、协作组队、学习计划以及后台管理等功能。

## 技术栈

### 前端
- Vue 3
- Vite 7
- TypeScript
- Vue Router
- Pinia
- Vant
- Axios
- ECharts

### 后端
- Spring Boot 3.1
- MyBatis-Plus
- MySQL 8.x
- Redis
- WebSocket
- Maven

---

## 当前已实现功能

### 1. 首页与基础浏览
- 首页提供校园学习空间服务入口。
- 支持查看教学楼列表与详情。
- 支持楼宇导航与导航详情页。
- 支持按教学楼查看教室列表。

### 2. 学习空间预约
- 支持预约教室与图书馆座位。
- 预约页支持日期切换与资源分类浏览。
- 教室详情页支持查看时段占用情况。
- 支持连续时段预约。
- 支持读取预约规则：
  - 每周预约次数上限
  - 单次预约最长时长
- 支持查看个人预约记录。
- 支持预约签到。
- 支持为预约生成二维码。
- 支持扫码核销预约二维码。
- 支持查询二维码状态。

### 3. 教室评价与反馈
- 支持查询待评价记录。
- 支持提交教室评价。
- 支持查看个人评价记录。
- 支持删除个人评价。
- 教室详情页支持展示审核通过的评价。
- 支持“查看全部评价”页面。
- 支持用户提交报修。
- 支持查看个人报修记录。

### 4. 协作组队
- 支持查看活跃协作小组。
- 支持查看小组详情与成员列表。
- 支持发起协作小组。
- 支持加入小组、退出小组、删除自己创建的小组。
- 支持提交加入申请。
- 支持组长审核申请（通过/拒绝）。
- 支持申请结果查询。
- 支持待处理申请查询。
- 支持协作红点角标提醒。
- 支持进入研讨室协作页面。

### 5. 学习计划
- 支持共享学习计划页。
- 支持按协作小组查询学习计划。
- 支持按预约查询关联学习计划。
- 支持创建“学习计划 + 教室预约”联动记录。
- 支持更新学习计划。
- 支持在计划中关联研讨室、日期、时间段与关键时间节点。

### 6. 资源市场与校园学分
- 支持资源市场广场浏览（按分类、是否免费筛选）。
- 支持发布学习资源与二手资源（含标题、描述、类别、价格、图片、推荐交易地点等）。
- 支持发起交易意向并填写留言、线下交接地点。
- 支持交易双方确认完成交易。
- 支持查看个人发布记录与交易记录。
- 支持查询个人校园学分信息。

### 7. 扫码设备授权
- 管理端支持生成扫码设备配对 token。
- 设备端支持扫码授权绑定设备 UID。
- 管理端支持查看扫码设备列表。
- 管理端支持启用/停用扫码设备。
- 管理端支持删除扫码设备。
- 支持预约扫码核销场景。

### 8. 后台管理
- 支持管理员登录。
- 支持按权限返回后台菜单。
- 支持后台概览统计。
- 支持报修工单管理。
- 支持评价审核。
- 支持资源市场发布内容审核（通过/拒绝）。
- 支持教室管理。
- 支持课程管理。
- 支持教学楼管理。
- 支持预约限制配置管理。
- 支持扫码设备管理。

---

## 主要前端路由

### 用户端
- `/`：首页
- `/classrooms/:buildingId`：教学楼教室列表
- `/building-nav`：楼宇导航
- `/building-nav/:id`：楼宇导航详情
- `/reservation`：预约中心
- `/reservation/building/:buildingId/:type`：按楼宇查看可预约资源
- `/reservation/classroom/:id`：教室详情
- `/reservation/classroom/:id/reviews`：教室全部评价
- `/reservation/room-collab/:reservationId`：研讨室在线协作
- `/collaboration`：协作广场
- `/shared-plan`：共享学习计划
- `/feedback`：反馈/评价
- `/profile`：个人中心
- `/profile/info`：个人信息
- `/profile/reservations`：我的预约
- `/profile/repairs`：我的报修
- `/profile/reviews`：我的评价
- `/profile/teams`：我的协作
- `/profile/plans`：我的计划
- `/resource-market`：资源市场
- `/scan-device`：扫码设备授权页
- `/scan-reservation`：预约扫码页

### 管理端
- `/admin/login`：后台登录
- `/admin`：后台首页
- `/admin/repairs`：报修管理
- `/admin/reviews`：评价审核
- `/admin/classrooms`：教室管理
- `/admin/courses`：课程管理
- `/admin/buildings`：教学楼管理
- `/admin/reservation-limits`：预约规则管理
- `/admin/scan-devices`：扫码设备管理
- `/admin/market-audit`：资源市场审核

---

## 项目结构

```text
TestA/
├── backend/                         # Spring Boot 后端
│   ├── src/main/java/com/campus/learningspace/
│   │   ├── common/                  # 通用返回、异常
│   │   ├── config/                  # 跨域、MyBatis、配置项
│   │   ├── controller/              # 接口控制器
│   │   ├── dto/                     # DTO
│   │   ├── entity/                  # 实体/VO
│   │   ├── mapper/                  # MyBatis-Plus Mapper
│   │   ├── service/                 # 业务服务
│   │   └── task/                    # 定时任务
│   ├── src/main/resources/
│   │   ├── application.yml          # 后端配置
│   │   └── sql/init.sql             # 初始化脚本
│   └── pom.xml
├── frontend/                        # Vue 前端
│   ├── src/api/                     # 前端接口封装
│   ├── src/router/                  # 路由配置
│   ├── src/utils/                   # 请求与公共地址工具
│   ├── src/views/                   # 页面视图
│   ├── .env.example                 # 前端环境变量示例
│   ├── package.json
│   └── vite.config.ts
├── UIDesign/                        # UI 原型/设计稿
└── README.md
```

---

## 本地开发环境

### 必需环境
- Node.js 20+
- npm
- JDK 21
- Maven 3.9+
- MySQL 8.x
- Redis 6+

---

## 启动方式

### 1. 初始化数据库
1. 创建数据库：`campus_learning_space`
2. 执行初始化脚本：`backend/src/main/resources/sql/init.sql`
3. 修改 `backend/src/main/resources/application.yml` 中的数据库连接信息

### 2. 启动 Redis
后端当前依赖 Redis，请先确保本地 Redis 已启动，默认配置为：
- Host: `localhost`
- Port: `6379`
- DB: `0`

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`
接口基址默认：`http://localhost:8080/api`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

如果需要让同一局域网内的手机或其他设备访问前端，可使用：

```bash
npm run dev:lan
```

该命令会以 `0.0.0.0:5173` 启动前端。

### 5. 打包 Windows 桌面端（Electron）

先分别构建后端与安装前端依赖：

```bash
cd backend
mvn clean package

cd ../frontend
npm install
```

生成桌面端安装包：

```bash
npm run desktop:dist
```

生成结果位于 `frontend/release/`。

说明：
- 该命令会先使用 `frontend/.env.desktop` 构建前端。
- 再自动复制 `backend/target` 下的可执行 JAR 到打包资源中。
- 打开的桌面程序会自动尝试启动本地 Spring Boot 服务（默认 `127.0.0.1:8080`）。
- 目标机器需要可用的 Java 21 运行环境，并且数据库、Redis 配置仍需可访问。

---

## 前端环境变量

`frontend/.env.example` 当前包含：

```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_PUBLIC_WEB_ORIGIN=http://localhost:5173
```

说明：
- `VITE_API_BASE_URL`：前端请求后端接口的基础地址。
- `VITE_PUBLIC_WEB_ORIGIN`：用于二维码、跨设备访问等场景生成外部可访问地址。

如果要让手机访问二维码链接，建议把 `VITE_PUBLIC_WEB_ORIGIN` 改成你电脑的局域网地址，例如：

```env
VITE_PUBLIC_WEB_ORIGIN=http://192.168.1.100:5173
```

如果前端也需要让同一 Wi‑Fi 下的其他设备访问，通常还需要把 `VITE_API_BASE_URL` 一并改成后端所在电脑的局域网地址，例如：

```env
VITE_API_BASE_URL=http://192.168.1.100:8080/api
```

### 桌面端打包环境变量

桌面端打包默认读取 `frontend/.env.desktop`：

```env
VITE_API_BASE_URL=http://127.0.0.1:8080/api
VITE_PUBLIC_WEB_ORIGIN=http://127.0.0.1:4173
```

说明：
- `VITE_API_BASE_URL` 指向本机 Spring Boot 服务。
- `VITE_PUBLIC_WEB_ORIGIN` 指向 Electron 内部静态服务地址。
- 若需修改桌面端接口地址，可调整 `frontend/.env.desktop` 后重新执行桌面打包。

---

## 默认配置说明

### 后端默认端口
- 应用端口：`8080`

### 前端默认端口
- 开发端口：`5173`

### 预约规则默认值
当数据库中没有预约规则配置时，后端默认使用：
- 每周最多预约：`4` 次
- 单次最长预约：`240` 分钟

---

## 测试账号

初始化脚本内置以下账号：

- 学生：`zhangsan / 123456`
- 学生：`lisi / 123456`
- 学生：`wangwu / 123456`
- 教师：`wanglaoshi / 123456`
- 管理员：`admin / 123456`
- 维修人员：`zhangshifu / 123456`
- 评论审核员：`commentadmin / 123456`

---

## 开发说明

- 前端接口统一封装在 `frontend/src/api/*`。
- 前端请求实例位于 `frontend/src/utils/request.ts`。
- 前端支持通过 `VITE_PUBLIC_WEB_ORIGIN` 生成跨设备可访问链接。
- 管理端 token 通过请求头 `X-Admin-Token` 传递。
- 后端统一返回 `Result<T>`。
- 数据表普遍使用逻辑删除字段 `deleted`。
- 资源市场接口见 `frontend/src/api/resourceMarket.ts`，后台审核接口见 `frontend/src/api/admin.ts`。

---

## 当前状态说明

本项目已具备完整的用户端 + 管理端基础业务闭环，包含预约、评价、报修、协作、学习计划、资源市场、扫码设备与后台管理等核心模块，可继续在此基础上完善权限、消息通知、部署与生产环境配置。