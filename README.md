# My Schedule - Ứng dụng Lịch Android từ file ICS

My Schedule là một ứng dụng lịch dành cho Android, cho phép người dùng nhập và quản lý nhiều nguồn lịch từ các file iCalendar (`.ics`), đồng thời tạo và quản lý sự kiện cá nhân trực tiếp trên ứng dụng. Ứng dụng được xây dựng với mục tiêu giúp người dùng xem thông tin lịch học, lịch làm việc qua nhiều chế độ xem khác nhau một cách trực quan.

Đây là phiên bản **3.0** của ứng dụng.

## 📸 Ảnh chụp màn hình

*(Cập nhật sau)*

## ✨ Tính năng chính

### Chế độ xem Lịch
- **Xem Tháng:** Giao diện lịch tháng rõ ràng, cuộn qua lại giữa các tháng. Mỗi ô ngày hiển thị ngày dương lịch, ngày âm lịch nhỏ phía dưới, và chấm màu sự kiện theo nguồn.
- **Xem Ngày:** Header điều hướng ◀ ▶ chuyển ngày trước/sau, hiển thị ngày âm lịch. Danh sách sự kiện trong ngày với thanh màu dọc phân biệt nguồn lịch.
- **Xem Năm:** Grid 12 mini calendar, highlight tháng hiện tại và ngày hôm nay. Click tháng → chuyển sang Month view của tháng đó.
- **Lịch biểu (Agenda):** HorizontalRecyclerView chọn năm, danh sách sự kiện nhóm theo ngày. Hỗ trợ sự kiện nhiều ngày với thời gian hiển thị điều chỉnh đúng. Auto scroll đến ngày hôm nay khi vào.
- **Chuyển đổi mượt** qua Navigation Drawer, chia sẻ state `selectedDate` giữa tất cả chế độ xem.

### Sự kiện Cá nhân
- **Thêm sự kiện tay:** Form đầy đủ — tên, ngày giờ bắt đầu/kết thúc, toggle cả ngày, địa điểm, mô tả, thông báo. Validate đầu vào trước khi lưu.
- **Xem chi tiết:** Hiển thị đầy đủ thông tin sự kiện kèm tên nguồn lịch.
- **Sửa sự kiện:** Chuyển sang edit mode bằng icon ✏️, sửa tất cả các trường, lưu bằng icon ✓. Alarm tự động reschedule.
- **Xóa sự kiện:** Dialog xác nhận trước khi xóa. Alarm tự động hủy.

### Quản lý Nguồn Lịch
- **Nguồn mặc định "Lịch của tôi":** Tự động tạo khi khởi động, chứa các sự kiện tạo tay. Không thể xóa.
- **Đa nguồn lịch:** Nhập nhiều file `.ics` cùng lúc, mỗi nguồn được gán màu tự động từ palette 8 màu.
- **Filter theo nguồn:** Bật/tắt từng nguồn bằng checkbox — lịch cập nhật realtime.
- **Xóa nguồn:** Xóa nguồn kèm toàn bộ sự kiện và alarm liên quan.
- **Chống trùng lặp:** Phát hiện và thông báo nếu import cùng file `.ics` hai lần.

### Âm Lịch
- **Hiển thị ngày âm** trong ô ngày tháng (Month view) dưới dạng `"15/4"` nhỏ mờ.
- **Hiển thị ngày âm** trong header Day view dưới dạng `"(15/4 âl)"`.
- Thuật toán chuyển đổi dương → âm lịch Việt Nam chuẩn UTC+7, xử lý đúng tháng nhuận.

### Thông báo Sự kiện
- Tự động lên lịch thông báo theo `reminderMinutes` của từng sự kiện.
- Import ICS: đọc `VALARM TRIGGER` để lấy thời gian nhắc, mặc định 30 phút nếu không có VALARM.
- Sự kiện tạo tay: Spinner chọn 5p / 15p / 30p / 1h / 1 ngày / Tắt.
- Dùng `AlarmManager.setExactAndAllowWhileIdle()` với fallback `set()` nếu thiếu permission.

### Giao diện & Tiện ích
- **Navigation Drawer** với 4 chế độ xem: Tháng / Ngày / Năm / Lịch biểu.
- **Dark / Light mode:** Nút chuyển đổi ngay trên toolbar, lưu lại lựa chọn qua SharedPreferences.
- **Lưu trữ bền vững:** Toàn bộ dữ liệu trong Room Database (version 2).

## 🛠️ Công nghệ sử dụng

| Thành phần | Công nghệ |
|-----------|-----------|
| Ngôn ngữ | [Kotlin](https://kotlinlang.org/) |
| Kiến trúc | MVVM — ViewModel + LiveData + Repository |
| Database | [Room](https://developer.android.com/training/data-storage/room) v2.6.1 |
| Giao diện Lịch | [Kizitonwose Calendar View](https://github.com/kizitonwose/Calendar) v2.0.0 |
| Phân tích `.ics` | [iCal4j](https://github.com/ical4j/ical4j) v3.2.4 |
| Async | Kotlin Coroutines + Flow |
| Thông báo | `AlarmManager` + `BroadcastReceiver` + `NotificationManager` |
| UI | Material Components for Android (Material3) |
| Navigation | Single Activity + Navigation Drawer + Fragment |

## 📁 Cấu trúc dự án

```
app/src/main/java/com/example/myschedule/
├── data/
│   ├── db/            ← Room DAOs + AppDatabase (v2)
│   ├── entity/        ← CalendarSource, CalendarEvent (+reminderMinutes)
│   └── repository/    ← CalendarRepository, ImportResult
├── ui/
│   ├── main/          ← MainActivity (Single Activity + Drawer), EventAdapter
│   ├── month/         ← MonthFragment
│   ├── day/           ← DayFragment
│   ├── year/          ← YearFragment, MiniMonthAdapter
│   ├── agenda/        ← AgendaFragment, AgendaAdapter, AgendaItem, YearAdapter
│   ├── event/         ← AddEditEventFragment, EventDetailFragment
│   └── source/        ← SourceManagerActivity, SourceAdapter
├── viewmodel/         ← MainViewModel, SourceManagerViewModel
├── receiver/          ← NotificationReceiver, NotificationScheduler
└── util/              ← LunarCalendarUtil
```

## 🚀 Hướng dẫn Build

1. Clone repository:
    ```bash
    git clone https://github.com/An-K4/MyScheduleApp.git
    cd myscheduleapp
    ```
2. Mở dự án bằng Android Studio (Hedgehog 2023.1+).
3. Đợi Gradle đồng bộ hóa dependencies.
4. **Xóa app cũ trước khi cài** nếu đang có v2.0.0 (DB version thay đổi từ 1 → 2).
5. Build và chạy trên máy ảo hoặc thiết bị thật (API 26+).

## 📋 Việc cần làm tiếp theo (v3.1)

Sắp xếp theo mức độ ưu tiên:

### 🔴 Cao
- **Fix Fragment backstack** — Các Fragment đang ghi đè nhau khi chuyển qua Drawer, backstack tích lũy không kiểm soát được
- **Fix render chấm multi-day** — Sự kiện nhiều ngày chỉ hiện chấm ở ngày bắt đầu, cần expand `startTime → endTime` để render đúng tất cả ngày

### 🟡 Trung bình
- **Fix "Lịch của tôi" không hiện trong Source Manager** — `ensureDefaultSource()` chưa được gọi từ `SourceManagerViewModel`
- **Fix nguồn lịch sai với sự kiện tạo tay** — Resolve sai `sourceId` trong `EventDetailFragment`
- **Nút quay về ngày hôm nay** — Thêm nút "Hôm nay" trên toolbar hoặc trong Drawer
- **Fix nội dung notification** — Đang hiển thị hardcode "30 phút", cần truyền `reminderMinutes` vào `NotificationReceiver` qua Intent extra

### 🟠 Quan trọng nhưng tốn công
- **Refactor toolbar** — `tvMonthYear` và icon đang đè nhau trên màn hình nhỏ, cần bỏ năm hoặc chuyển icon vào Drawer
- **Thông báo linh hoạt** — Thay Spinner cố định bằng EditText + RadioGroup (phút/giờ/ngày/tuần) giống Calendly

### 🟢 Thấp / Tùy chọn
- **Chia Activity** — Tách EventDetail và AddEditEvent thành Activity riêng
- **Polish padding/margin** — Rà soát toàn bộ layout
- **Tap notification → mở EventDetail** — Thêm PendingIntent với `eventId` trong NotificationReceiver
- **Agenda auto-scroll** — Xem xét thêm nút "Về hôm nay" riêng trong AgendaFragment
- **Import trùng → hỏi user** — Dialog thay Toast, cho phép import đè (rủi ro cao)

## 🔄 Lịch sử phiên bản

| Phiên bản | Nội dung |
|-----------|----------|
| v1.0.0 | MVP ban đầu — import ICS, xem tháng cơ bản, thông báo |
| v2.0.0 | Đa nguồn lịch, chấm màu theo nguồn, dark/light mode, SourceManager |
| v3.0.0 | Single Activity + Drawer, 4 chế độ xem, CRUD sự kiện, âm lịch, parse VALARM |

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