# Campus Guide 🗺️

> 自研校园导航与轨迹定位系统 — Private campus navigation & tracking system

A full-stack campus navigation system with a custom road network, Dijkstra/A* path planning, and an Android client featuring map display, user management, lost & found, and location tracking.

## Architecture

```
D:\bestwork/
├── CampusGuideBackend/     # Spring Boot backend (Java 17, Maven)
└── CampusGuide_Android/    # Android client (Kotlin, Gradle)
```

---

## Backend — CampusGuideBackend

**Tech stack:** Spring Boot 4.0.3 · Java 17 · MySQL · Maven · JPA

The backend manages the private campus road network and provides REST APIs for route planning, user auth, and content management.

### Key APIs

| Endpoint | Description |
|---|---|
| `GET /api/nav/route?startId=&endId=&algorithm=` | Route by node IDs (Dijkstra/A*) |
| `GET /api/nav/routeByLocation?startLat=&startLng=&endLat=&endLng=&algorithm=` | Route by real coordinates with nearest-node snapping |
| `POST /api/user/login` | User login |
| `POST /api/user/register` | User registration |
| `GET /api/biz/notices` | Campus notices |
| `GET /api/biz/lost-found` | Lost & found items |

### Database

Requires MySQL with road network tables:

```sql
map_nodes(id, name, lat, lng)        — road network nodes
map_edges(id, source_id, target_id, distance)  — road edges with weights
```

A web admin panel is available at `road_admin.html` for manually drawing the campus road network.

### Run

```bash
cd CampusGuideBackend
# Edit application.properties with your MySQL credentials
./mvnw spring-boot:run
```

The Android emulator accesses the backend at `http://10.0.2.2:8080`.

---

## Android — CampusGuide_Android

**Tech stack:** Kotlin · Gradle · AMap 3D Map SDK · minSdk 24 · targetSdk 34

The Android client handles map display, GPS positioning, landmark interaction, and route drawing. Route calculation is delegated to the custom backend — no AMap route planning SDK is used.

### Features

| Feature | Description |
|---|---|
| 🔐 Auth | Login / Register / Forgot password / Profile editing |
| 🗺️ Navigation | Custom road network with real-time route drawing |
| 📍 Tracking | GPS sampling, Haversine distance calculation, history replay |
| ⭐ Favorites | Save and manage favorite campus locations |
| 📢 Notices | Campus announcement board |
| 🔍 Lost & Found | Post and browse lost items |
| 🏛️ Landmarks | Browse campus landmarks by category |

### Run

1. Start the backend first
2. Open in Android Studio
3. Run on emulator (connects to `http://10.0.2.2:8080`)

---

## Key Algorithms

- **Node snapping** — Maps GPS coordinates to the nearest road network node
- **Dijkstra** — Classic shortest path algorithm
- **A\*** — Heuristic path planning for optimized routing
- **Edge projection** — Snaps start/end points to nearest road edge, not just nodes

## License

MIT
