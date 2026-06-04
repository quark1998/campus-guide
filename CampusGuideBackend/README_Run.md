# CampusGuideBackend 后端说明

本工程已合入“自研校园导航与轨迹定位”优化，后端负责校园私有路网管理、坐标吸附、Dijkstra/A* 路径规划，并向 Android 端返回可绘制的路径节点序列。

## 主要改动

1. `NavigationService.java`
   - 保留并优化 Dijkstra 最短路径算法。
   - 新增 A* 路径规划算法，可用于论文中的算法优化与对比分析。

2. `CampusGuideBackendApplication.java`
   - `/api/nav/route`：基于起点节点 ID 和终点节点 ID 计算路线。
   - `/api/nav/routeByLocation`：基于真实经纬度计算路线，自动吸附到最近路网节点。
   - 新增 `loadRoadGraph()`、`findNearestNode()`、`distanceMeters()` 等私有方法。
   - 返回字段包含 `distance`、`durationMinute`、`nodeCount`、`algorithm`、吸附节点和吸附距离。

## 关键接口

### 1. 节点寻路

```text
GET /api/nav/route?startId=1&endId=10&algorithm=astar
```

### 2. 坐标寻路

```text
GET /api/nav/routeByLocation?startLat=31.765&startLng=119.923&endLat=31.766&endLng=119.924&algorithm=astar
```

## 数据库要求

需要存在以下路网表：

```sql
map_nodes(id, name, lat, lng)
map_edges(id, source_id, target_id, distance)
```

后台已有路网维护接口与 `road_admin.html` 页面，可继续用于手工绘制校园道路网络。

## 运行方式

1. 修改 `src/main/resources/application.properties` 中的 MySQL 用户名、密码和数据库名。
2. 启动后端：

```bash
./mvnw spring-boot:run
```

3. 后端启动后，Android 模拟器通过 `http://10.0.2.2:8080` 访问接口。

## 论文可写点

- 校园路网图模型：节点、边、边权。
- 坐标吸附算法：把任意定位点映射到最近路网节点。
- Dijkstra 与 A* 路径规划算法。
- 路径长度与预计耗时计算。
- 前后端协同：后端负责算法，Android 负责定位、交互与可视化。
