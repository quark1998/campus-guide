# 校园导航系统 🗺️

> 自研校园导航与轨迹定位系统 — Private campus navigation & tracking system

全栈校园导航系统，包含自定义路网、Dijkstra/A* 路径规划算法，以及 Android 客户端，支持地图展示、用户管理、失物招领和轨迹追踪等功能。

## 项目结构

```
D:\bestwork/
├── CampusGuideBackend/     # 后端 — Spring Boot (Java 17, Maven)
└── CampusGuide_Android/    # 安卓客户端 (Kotlin, Gradle)
```

---

## 后端 — CampusGuideBackend

**技术栈：** Spring Boot 4.0.3 · Java 17 · MySQL · Maven · JPA

后端负责管理校园私有路网，提供路径规划、用户认证和内容管理等 REST API。

### 核心接口

| 接口 | 说明 |
|---|---|
| `GET /api/nav/route?startId=&endId=&algorithm=` | 按节点 ID 寻路（Dijkstra / A*） |
| `GET /api/nav/routeByLocation?startLat=&startLng=&endLat=&endLng=&algorithm=` | 按真实经纬度寻路，自动吸附到最近路网节点 |
| `POST /api/user/login` | 用户登录 |
| `POST /api/user/register` | 用户注册 |
| `GET /api/biz/notices` | 校园公告列表 |
| `GET /api/biz/lost-found` | 失物招领列表 |

### 数据库

使用 MySQL，需要以下路网表：

```sql
map_nodes(id, name, lat, lng)                  — 路网节点
map_edges(id, source_id, target_id, distance)  — 路网边（带权）
```

后台提供 `road_admin.html` 管理页面，可用于手动绘制校园道路网络。

### 运行方式

```bash
cd CampusGuideBackend
# 修改 application.properties 中的 MySQL 用户名、密码和数据库名
./mvnw spring-boot:run
```

Android 模拟器通过 `http://10.0.2.2:8080` 访问后端接口。

---

## 安卓客户端 — CampusGuide_Android

**技术栈：** Kotlin · Gradle · 高德 3D 地图 SDK · minSdk 24 · targetSdk 34

安卓端负责地图展示、GPS 定位、地标交互和路线绘制。路径规划交由自定义后端完成，不再使用高德路线规划 SDK。

### 功能列表

| 功能 | 说明 |
|---|---|
| 🔐 账户系统 | 登录 / 注册 / 忘记密码 / 编辑个人资料 |
| 🗺️ 校园导航 | 自定义路网寻路 + 实时路线绘制 |
| 📍 轨迹追踪 | GPS 采样、Haversine 球面距离计算、历史轨迹回放 |
| ⭐ 收藏夹 | 保存和管理校园常用地点 |
| 📢 校园公告 | 公告信息展示 |
| 🔍 失物招领 | 发布和浏览失物信息 |
| 🏛️ 校园地标 | 按分类浏览校园地标 |

### 运行方式

1. 先启动后端
2. 用 Android Studio 打开本工程根目录
3. 等待 Gradle 同步完成后运行（模拟器连接 `http://10.0.2.2:8080`）

---

## 核心算法

- **坐标吸附** — 将 GPS 坐标映射到最近的路网节点
- **Dijkstra** — 经典最短路径算法
- **A\*** — 启发式路径规划，用于算法优化对比
- **边投影** — 将起终点吸附到最近道路边的投影点，而非仅吸附到节点


MIT
