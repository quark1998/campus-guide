# CampusGuide Android 端说明

本工程已合入“自研校园导航与轨迹定位”优化，Android 端只负责地图展示、定位、地标交互和路线绘制；路线规划结果来自后端私有校园路网接口，不再使用高德路线规划/搜索 SDK。

## 主要改动

1. `MainActivity.java`
   - 删除高德 `RouteSearch` 路线规划调用。
   - 地标导航、收藏页/分类页返回导航统一调用 `requestCustomRouteTo()`。
   - 新增 `drawCustomRouteFromResponse()`，解析后端返回的节点路径并绘制 Polyline。
   - 轨迹距离计算改为 Haversine 球面距离公式，不再依赖 `AMapUtils.calculateLineDistance()`。

2. `CoordinateConverter.java`
   - 新增 `gcj02ToWgs84()` 与 `outOfChina()`。
   - Android 高德定位/地标坐标可转换为后端私有路网坐标，再交给后端吸附到最近节点。

3. `app/build.gradle.kts`
   - 移除高德搜索/路线规划 SDK 依赖。
   - 保留高德 3D Map 作为地图展示与定位底图；核心路径生成由自研后端算法完成。

## 运行前准备

1. 先启动后端工程，确保接口地址为：
   `http://10.0.2.2:8080`

2. 在后台管理端完成校园路网维护：
   - 添加 `map_nodes` 路网节点。
   - 添加 `map_edges` 路网边。
   - 确保目标地标附近有可吸附的路网节点。

3. 用 Android Studio 打开本工程根目录，等待 Gradle 同步后运行。

## 论文可写点

- 私有校园路网建模：节点表 `map_nodes`、边表 `map_edges`。
- 坐标吸附：当前位置和目标点自动匹配最近路网节点。
- 路径规划：后端 Dijkstra/A* 最短路径算法。
- 路径可视化：Android 端根据后端节点序列绘制校园路线。
- 轨迹定位：定位采样点保存、Haversine 距离累加、历史轨迹回放。
