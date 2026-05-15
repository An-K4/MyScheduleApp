# My Schedule - Ứng dụng Lịch Android từ file ICS

My Schedule là một ứng dụng lịch dành cho Android, cho phép người dùng nhập và quản lý nhiều nguồn lịch từ các file iCalendar (`.ics`). Ứng dụng được xây dựng với mục tiêu giúp người dùng xem thông tin lịch học qua từng học kì một cách trực quan.

Đây là phiên bản **2.0.0** của ứng dụng.

## 📸 Ảnh chụp màn hình

<p align="center">
  <img src="https://github.com/user-attachments/assets/4918d3a5-6e38-48cf-819e-8e0f0a1776e9" width="24%"/>
  <img src="https://github.com/user-attachments/assets/1b3064f1-b2c9-4c34-95d9-d365da053b02" width="24%"/>
  <img src="https://github.com/user-attachments/assets/992caf14-3df8-431a-956e-7c98f68ebf78" width="24%"/>
  <img src="https://github.com/user-attachments/assets/9ec077f9-0dcc-4aaf-a2bd-9ea76f204343" width="24%"/>
</p>

## ✨ Tính năng chính

### Lịch & Sự kiện
- **Hiển thị Lịch theo Tháng:** Giao diện lịch rõ ràng, cho phép cuộn qua lại giữa các tháng.
- **Dấu chấm màu trên ngày:** Mỗi nguồn lịch có màu riêng, các ngày có sự kiện hiển thị chấm màu tương ứng.
- **Hiển thị Sự kiện trong Ngày:** Chọn một ngày để xem toàn bộ sự kiện, thanh màu dọc bên trái mỗi item phân biệt nguồn lịch.
- **Xem Chi tiết Sự kiện:** Nhấn vào sự kiện để xem tiêu đề, địa điểm và mô tả.

### Quản lý Nguồn Lịch
- **Đa nguồn lịch:** Nhập nhiều file `.ics` cùng lúc, mỗi nguồn được gán màu tự động.
- **Màn hình Quản lý Nguồn:** Xem toàn bộ các nguồn lịch đã nhập, với màu nền đặc trưng cho từng nguồn.
- **Filter theo nguồn:** Bật/tắt từng nguồn bằng checkbox — lịch cập nhật realtime.
- **Xóa nguồn:** Xóa nguồn lịch kèm toàn bộ sự kiện liên quan.
- **Chống trùng lặp:** Ứng dụng phát hiện và báo lỗi nếu import cùng một file `.ics` hai lần.

### Giao diện & Tiện ích
- **Dark / Light mode:** Nút chuyển đổi giao diện ngay trên trang chủ, lưu lại lựa chọn.
- **Thông báo Sự kiện:** Tự động gửi thông báo 30 phút trước khi sự kiện bắt đầu.
- **Lưu trữ bền vững:** Toàn bộ dữ liệu lưu trong Room Database, không mất khi khởi động lại.

## 🛠️ Công nghệ sử dụng

| Thành phần | Công nghệ |
|-----------|-----------|
| Ngôn ngữ | [Kotlin](https://kotlinlang.org/) |
| Kiến trúc | MVVM — ViewModel + LiveData + Repository |
| Database | [Room](https://developer.android.com/training/data-storage/room) |
| Giao diện Lịch | [Kizitonwose Calendar View](https://github.com/kizitonwose/Calendar) |
| Phân tích `.ics` | [iCal4j](https://github.com/ical4j/ical4j) |
| Async | Kotlin Coroutines + Flow |
| Thông báo | `AlarmManager` + `BroadcastReceiver` + `NotificationManager` |
| UI | Material Components for Android |

## 📁 Cấu trúc dự án

```
app/src/main/java/com/example/myschedule/
├── data/
│   ├── db/            ← Room DAOs + AppDatabase
│   ├── entity/        ← CalendarSource, CalendarEvent
│   └── repository/    ← CalendarRepository, ImportResult
├── ui/
│   ├── main/          ← MainActivity, EventAdapter
│   └── source/        ← SourceManagerActivity, SourceAdapter
├── viewmodel/         ← MainViewModel, SourceManagerViewModel
└── receiver/          ← NotificationReceiver, NotificationScheduler
```

## 🚀 Hướng dẫn Build

1. Clone repository:
    ```bash
    git clone https://github.com/An-K4/MyScheduleApp.git
    cd myscheduleapp
    ```
2. Mở dự án bằng Android Studio.
3. Đợi Gradle đồng bộ hóa dependencies.
4. Build và chạy trên máy ảo hoặc thiết bị thật (API 26+).

## 🤝 Đóng Góp

1. **Fork** repository này.
2. Tạo nhánh mới (`git checkout -b feature/TinhNangMoi`).
3. Commit thay đổi (`git commit -m 'Thêm tính năng X'`).
4. Push lên nhánh (`git push origin feature/TinhNangMoi`).
5. Tạo **Pull Request**.

## ⭐️ Ủng hộ

Nếu thấy dự án hữu ích, hãy để lại một **Star** ⭐️!

---
Developed by **An_K4**.
