# Tài liệu Thiết kế & Kế hoạch Phát triển
## Rolling Icons Live Wallpaper (New Project)

* **Phiên bản:** 1.0
* **Nền tảng:** Android Native
* **Ngôn ngữ:** Kotlin
* **Mục tiêu:** Xây dựng ứng dụng Live Wallpaper mô phỏng các icon ứng dụng chuyển động vật lý chân thực, tối ưu hiệu năng và tiêu thụ pin thấp.

---

## 1. Giới thiệu
### 1.1 Mục tiêu

Rolling Icons là ứng dụng Live Wallpaper mô phỏng các icon ứng dụng hoặc ảnh người dùng lựa chọn chuyển động theo vật lý thực tế. Các icon có thể:

* Va chạm với nhau.
* Va chạm với cạnh màn hình.
* Xoay tự nhiên.
* Chịu tác động của trọng lực.
* Phản ứng theo góc nghiêng điện thoại.
* Có thể kéo và búng bằng ngón tay.

Toàn bộ chuyển động được mô phỏng bằng Box2D và render bằng OpenGL ES thông qua libGDX nhằm đạt hiệu năng cao và mức tiêu thụ pin thấp.

---

## 2. Kiến trúc công nghệ

| Thành phần | Công nghệ | Mục đích |
| :--- | :--- | :--- |
| Ngôn ngữ | Kotlin | Android Native hiện đại |
| UI Settings | XML | Giao diện cấu hình |
| Đồ họa | libGDX | Render OpenGL ES |
| Physics Engine | Box2D | Mô phỏng vật lý |
| Wallpaper | Android Live Wallpaper API | Tích hợp làm hình nền |
| Image Loader | Glide / Coil | Load icon và ảnh |
| Storage | Jetpack DataStore | Lưu cấu hình |
| Sensor | SensorManager | Gia tốc kế |
| Gesture | GestureDetector | Drag/Fling |
| Coroutine | Kotlin Coroutine | Xử lý bất đồng bộ |

---

## 3. Kiến trúc tổng thể

```
                 Settings Activity (XML)
                          │
                          │
                    DataStore Preferences
                          │
                          │
            ┌────────Wallpaper Service────────┐
            │                                 │
            │    RollingWallpaperEngine       │
            │                                 │
            ├───────────┬─────────────────────┤
            │           │
            │           │
      SensorManager   Gesture Handler
            │           │
            └──────┬────┘
                   │
             Physics Engine
                 (Box2D)
                   │
            Render Engine
             (libGDX/OpenGL)
                   │
               Live Wallpaper
```

---

## 4. Công nghệ sử dụng

### 4.1 Kotlin
Là ngôn ngữ chính của toàn bộ dự án.
* **Ưu điểm:** Null Safety, Coroutine, Extension, Dễ bảo trì.

### 4.2 libGDX
Được sử dụng để:
* Render icon, background, animation.
* Quản lý Texture, SpriteBatch.
* *Ghi chú:* libGDX sử dụng OpenGL ES nên hiệu năng rất cao.

### 4.3 Box2D
Mô phỏng vật lý: Gravity, Collision, Rotation, Friction, Density, Restitution.

### 4.4 Android Live Wallpaper API
Cho phép:
* Đặt ứng dụng làm hình nền.
* Nhận callback visibility.
* Pause khi không hiển thị và Resume khi quay về Home.

### 4.5 Glide / Coil
Dùng để tải (load) icon app, background, ảnh người dùng với cơ chế cache tự động.

### 4.6 Jetpack DataStore
Lưu trữ cấu hình: Size icon, Gravity, Friction, Background, FPS, Enable Sensor.

---

## 5. Checklist phát triển

### Giai đoạn 1 – Core Engine
#### 5.1 Wallpaper Service
* **Công việc:** Tạo AndroidLiveWallpaperService, Đăng ký Manifest, Khởi tạo libGDX.
* **Kết quả:** Wallpaper chạy được.

#### 5.2 Box2D World
* **Khởi tạo:** `World(gravity, true)`
* **Tạo:** World, Contact Listener, Debug Renderer.

#### 5.3 Screen Boundary
* **Sinh 4 StaticBody:** Top, Bottom, Left, Right.
* **Mục đích:** Không cho icon rơi ra ngoài.

#### 5.4 Dynamic Icon Body
* **Mỗi icon gồm:** Texture, Body, Sprite, Rotation, Velocity.
* **Các tham số:** Density, Friction, Restitution.

### Giai đoạn 2 – Data Integration
#### 6.1 App Icon Service
* **Đọc PackageManager:** `ACTION_MAIN`, `CATEGORY_LAUNCHER`.
* **Lấy:** Package Name, App Name, Drawable.

#### 6.2 Drawable → Bitmap
* `Drawable` → `Bitmap` → `Texture` → `TextureRegion` → `Sprite` → `Body`

#### 6.3 Cache
* Lưu bitmap vào RAM hoặc Disk Cache giúp giảm thời gian load.

#### 6.4 Photo Picker
* Cho phép chọn từ Gallery/Photo, sau đó: `Crop` → `Resize` → `Bitmap` → `Texture`.

### Giai đoạn 3 – Sensor & Gesture
#### 7.1 Accelerometer
* **Đăng ký:** SensorManager (`SENSOR_DELAY_GAME`).
* **Đọc:** x, y, z.

#### 7.2 Gravity Update
* *Ví dụ:* Điện thoại nghiêng trái → `gravity = (-9.8, 0)` → `World.setGravity()`. Icon sẽ lăn theo hướng nghiêng.

#### 7.3 Drag
* `Touch Down` → `QueryAABB` → `Find Body` → `MouseJoint` → `Move`.

#### 7.4 Fling
* Tính `VelocityX`, `VelocityY` → `ApplyLinearImpulse()` → Body bay đi.

### Giai đoạn 4 – Settings
#### 8.1 Icon Size
* Thanh trượt điều chỉnh kích thước (ví dụ: 20dp, 40dp, 60dp, 80dp).

#### 8.2 Physics
* Các Slider điều chỉnh: Density, Friction, Restitution (cập nhật realtime).

#### 8.3 Background
* Cho phép tùy chỉnh: Màu sắc, Gradient, Ảnh nền, Blur.

#### 8.4 Sensor
* Switch bật/tắt: `Enable Gravity`. Nếu OFF, trọng lực sẽ được cố định.

#### 8.5 FPS
* Tùy chọn giới hạn khung hình: 30 FPS, 60 FPS, 90 FPS.

---

## 6. Luồng hoạt động

### A. Luồng tải icon ứng dụng
```
Khởi động Wallpaper
        │
        ▼
Đọc danh sách ứng dụng
        │
        ▼
Lấy Icon Drawable
        │
        ▼
Drawable → Bitmap
        │
        ▼
Bitmap → Texture
        │
        ▼
Tạo Physics Body
        │
        ▼
Spawn vào World
        │
        ▼
Render lên màn hình
```

### B. Luồng xử lý cảm biến
```
SensorManager
        │
        ▼
Accelerometer Event
        │
        ▼
Đọc X,Y,Z
        │
        ▼
Tính Gravity Vector
        │
        ▼
World.setGravity()
        │
        ▼
Body Update
        │
        ▼
Render Frame
```

### C. Luồng Drag & Fling
```
Touch Down
        │
        ▼
QueryAABB
        │
        ▼
Có Body?
      /     \
    Không    Có
      │       │
      ▼       ▼
   Bỏ qua  Mouse Joint
              │
              ▼
          Drag Icon
              │
              ▼
        Touch Release
              │
              ▼
         Tính Velocity
              │
              ▼
      Apply Linear Impulse
              │
              ▼
          Icon Bay Đi
```

### D. Luồng tối ưu pin
```
Wallpaper Visible?
      │
 ┌────┴────┐
 │         │
Yes        No
 │         │
 ▼         ▼
Render   Pause Render
Physics  Pause Physics
Sensor   Unregister Sensor
60FPS    Release Resource
```

---

## 7. Cấu trúc dự án đề xuất

```
app/
│
├── wallpaper/
│   ├── RollingWallpaperService.kt
│   ├── RollingWallpaperEngine.kt
│
├── render/
│   ├── GameRenderer.kt
│   ├── TextureManager.kt
│
├── physics/
│   ├── PhysicsWorld.kt
│   ├── IconBody.kt
│   ├── CollisionManager.kt
│
├── sensor/
│   ├── GravitySensor.kt
│
├── gesture/
│   ├── GestureController.kt
│
├── data/
│   ├── AppRepository.kt
│   ├── IconLoader.kt
│   ├── PreferenceRepository.kt
│
├── ui/
│   ├── SettingsActivity.kt
│   ├── adapter/
│   ├── fragment/
│
├── util/
│
└── model/
```

---

## 8. Kế hoạch triển khai

| Giai đoạn | Nội dung | Kết quả mong đợi |
| :--- | :--- | :--- |
| **Phase 1** | Tích hợp Live Wallpaper + libGDX + Box2D | Wallpaper hiển thị và mô phỏng vật lý cơ bản |
| **Phase 2** | Đọc icon ứng dụng, chuyển Drawable → Texture | Icon ứng dụng xuất hiện và va chạm |
| **Phase 3** | Tích hợp cảm biến gia tốc và thao tác Drag/Fling | Điều khiển icon bằng nghiêng máy và cử chỉ chạm |
| **Phase 4** | Xây dựng màn hình Settings, tối ưu hiệu năng và pin | Hoàn thiện tính năng, cấu hình linh hoạt và tối ưu tài nguyên |
| **Phase 5** | Kiểm thử, sửa lỗi, tối ưu trải nghiệm người dùng | Phiên bản ổn định sẵn sàng phát hành |

---

## 9. Mục tiêu kỹ thuật
* **Hiệu năng:** Duy trì 60 FPS ổn định trên đa số thiết bị, hỗ trợ 90 FPS trên màn hình tần số quét cao.
* **Tiêu thụ pin:** Tạm dừng hoàn toàn render, mô phỏng vật lý và cảm biến khi wallpaper không hiển thị.
* **Khả năng mở rộng:** Kiến trúc module hóa, dễ bổ sung hiệu ứng, loại icon, background và các tính năng mới.
* **Khả năng bảo trì:** Tách biệt rõ các tầng UI, Physics, Rendering, Data và Sensor để thuận tiện phát triển lâu dài.
