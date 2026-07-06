# My Schedule — Ứng dụng Lịch Android từ file ICS

My Schedule là ứng dụng lịch cá nhân dành cho Android, cho phép nhập và quản lý nhiều nguồn lịch từ file iCalendar (`.ics`), đồng thời tạo và quản lý sự kiện cá nhân trực tiếp trên ứng dụng. Ứng dụng hỗ trợ bốn chế độ xem lịch, âm lịch Việt Nam, thông báo tùy chỉnh và chuyển đổi dark/light mode.

Đây là phiên bản **3.2** của ứng dụng.

---

## 📸 Ảnh chụp màn hình

<p>
  <img src="https://github.com/user-attachments/assets/322061a9-bc69-433c-af2e-3be5c75f21c8" width="24%"/>
  <img src="https://github.com/user-attachments/assets/8723f269-49ad-408f-88f0-3545805d861a" width="24%"/>
  <img src="https://github.com/user-attachments/assets/d044fe73-c995-45d6-bc43-361b46ed671e" width="24%"/>
  <img src="https://github.com/user-attachments/assets/4ead6c65-a7db-4be8-9686-034fe6a98598" width="24%"/>
</p>

---

## ✨ Tính năng chính

### Chế độ xem Lịch
Chuyển đổi qua **Navigation Drawer**, chia sẻ state `selectedDate` và `currentMonth` giữa tất cả chế độ xem qua `MainViewModel`.

- **Xem Tháng** — Lịch tháng cuộn được, mỗi ô ngày hiển thị ngày dương lịch, ngày âm lịch nhỏ phía dưới và chấm màu phân biệt nguồn lịch. Sự kiện nhiều ngày hiển thị chấm trên tất cả ngày trung gian.
- **Xem Ngày** — Header điều hướng ◀ ▶ chuyển ngày, hiển thị ngày âm lịch. Danh sách sự kiện trong ngày với thanh màu dọc theo nguồn.
- **Xem Năm** — Grid 3×4 gồm 12 mini calendar. Highlight tháng hiện tại, highlight ngày hôm nay, chấm sự kiện trên từng ô. Click tháng → chuyển sang Month view. Mỗi mini calendar vẽ bằng Custom Canvas View (~35% nhanh hơn so với GridLayout cũ).
- **Lịch biểu (Agenda)** — HorizontalRecyclerView chọn năm (±10 năm), danh sách sự kiện nhóm theo ngày. Hỗ trợ sự kiện nhiều ngày với thời gian hiển thị điều chỉnh đúng. Auto scroll đến hôm nay khi vào.

### Sự kiện Cá nhân
- **Thêm sự kiện** — Form đầy đủ: tên, ngày giờ bắt đầu/kết thúc, toggle cả ngày, địa điểm, mô tả, thông báo tùy chỉnh. Validate trước khi lưu.
- **Xem chi tiết** — Hiển thị đầy đủ thông tin kèm tên nguồn lịch.
- **Sửa sự kiện** — Nhấn icon ✏️ → edit mode, nhấn ✓ → lưu. Alarm tự reschedule.
- **Xóa sự kiện** — Dialog xác nhận. Alarm tự hủy.

### Quản lý Nguồn Lịch
- **Nguồn mặc định "Lịch của tôi"** — Tự động seed khi khởi động lần đầu, chứa sự kiện tạo tay. Không thể xóa.
- **Đa nguồn lịch** — Import nhiều file `.ics`, mỗi nguồn gán màu tự động từ palette 8 màu.
- **Filter** — Bật/tắt từng nguồn bằng checkbox, lịch cập nhật realtime.
- **Xóa nguồn** — Xóa kèm toàn bộ sự kiện và alarm liên quan.
- **Chống trùng lặp** — Phát hiện và báo nếu import cùng file hai lần (so sánh URI).

### Thông báo Tùy Chỉnh
- Nhập số + chọn đơn vị (Phút / Giờ / Ngày / Tuần) hoặc tắt hẳn.
- Import ICS: parse `VALARM TRIGGER` lấy thời gian nhắc, mặc định 30 phút nếu không có VALARM.
- Nội dung notification hiển thị đúng "X phút/giờ/ngày trước".
- Nhấn notification → mở thẳng `EventDetailActivity` của sự kiện đó.

### Âm Lịch
- Month view: hiển thị `"15/4"` nhỏ mờ dưới ngày dương.
- Day view: hiển thị `"Ngày 15/4 âm lịch"` trong header.
- Thuật toán chuyển đổi dương → âm lịch Việt Nam (Ho Ngoc Duc, 2004), múi giờ UTC+7.

### Giao diện & Tiện ích
- **Navigation Drawer** — 4 chế độ xem + Nguồn lịch + switch Dark mode.
- **Dark / Light mode** — Switch trong Drawer, lưu lại qua `SharedPreferences`.
- **Nút Hôm nay** — Circle button hiển thị số ngày hôm nay, nhấn → về ngày hiện tại và scroll đúng tháng/năm trên mọi Fragment.
- **Drawer highlight** — Item đang được chọn có background highlight.
- **Tap notification → mở EventDetail** — `PendingIntent` với `eventId` chính xác (kể cả sự kiện từ file ICS), mở `EventDetailActivity` đúng sự kiện.

---

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
| Navigation | Single Activity + Navigation Drawer + Fragment (4 chế độ xem) + 2 Activity riêng |

---

## 📁 Cấu trúc dự án

```
app/src/main/java/com/example/myschedule/
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt             ← Room singleton (version 2)
│   │   ├── CalendarSourceDao.kt       ← DAO nguồn lịch
│   │   ├── CalendarEventDao.kt        ← DAO sự kiện
│   │   └── EventTimeWithSource.kt     ← Projection (startTime, endTime, sourceId)
│   ├── entity/
│   │   ├── CalendarSource.kt          ← Entity nguồn lịch
│   │   └── CalendarEvent.kt           ← Entity sự kiện (+reminderMinutes)
│   └── repository/
│       ├── CalendarRepository.kt      ← Business logic chính
│       └── ImportResult.kt            ← Sealed class kết quả import
│
├── ui/
│   ├── BaseActivity.kt                ← Abstract class xử lý theme chung
│   ├── MainActivity.kt                ← Single Activity container + Drawer
│   ├── calendar/
│   │   ├── MonthFragment.kt           ← Chế độ xem tháng
│   │   ├── DayFragment.kt             ← Chế độ xem ngày
│   │   ├── YearFragment.kt            ← Chế độ xem năm (12 mini cal)
│   │   ├── MiniMonthAdapter.kt        ← Adapter cho YearFragment
│   │   ├── MiniMonthView.kt           ← Lịch mini month vẽ bằng Canvas cho YearFragment
│   │   └── EventAdapter.kt            ← Adapter sự kiện (dùng chung)
│   ├── event/
│   │   ├── AddEventActivity.kt        ← Activity thêm sự kiện mới
│   │   └── EventDetailActivity.kt     ← Activity xem/sửa/xóa sự kiện
│   ├── agenda/
│   │   ├── AgendaFragment.kt          ← Chế độ xem lịch biểu
│   │   ├── AgendaAdapter.kt           ← Multiple ViewType: header + event
│   │   ├── AgendaItem.kt              ← Data class item agenda
│   │   └── YearAdapter.kt             ← Adapter year selector
│   └── source/
│       ├── SourceManagerActivity.kt   ← Quản lý nguồn lịch
│       └── SourceAdapter.kt           ← Adapter nguồn (ẩn xóa nguồn mặc định)
│
├── viewmodel/
│   ├── MainViewModel.kt               ← State dùng chung (4 Fragment + MainActivity)
│   ├── SourceManagerViewModel.kt      ← State SourceManagerActivity
│   ├── AddEventViewModel.kt           ← State AddEventActivity
│   └── EventDetailViewModel.kt        ← State EventDetailActivity
│
├── receiver/
│   ├── NotificationReceiver.kt        ← BroadcastReceiver: nhận alarm → show notification
│   └── NotificationScheduler.kt       ← Wrapper AlarmManager: set/cancel alarm
│
└── util/
    └── LunarCalendarUtil.kt           ← Chuyển đổi dương → âm lịch Việt Nam
```

---

## 🚀 Hướng dẫn Build

1. Clone repository:
   ```bash
   git clone https://github.com/An-K4/MyScheduleApp.git
   cd myscheduleapp
   ```
2. Mở dự án bằng **Android Studio** (Hedgehog 2023.1+).
3. Đợi Gradle đồng bộ hóa dependencies.
4. **Xóa app cũ trước khi cài** nếu đang có phiên bản < v3.0 (DB version thay đổi, dùng `fallbackToDestructiveMigration`).
5. Build và chạy trên máy ảo hoặc thiết bị thật (API 26+).

---

## 🔄 Lịch sử phiên bản

| Phiên bản | Nội dung                                                                                                                                                                                                                                       |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| v1.0.0 | MVP ban đầu — import ICS, xem tháng cơ bản, thông báo cố định 30 phút                                                                                                                                                                          |
| v2.0.0 | Đa nguồn lịch, chấm màu theo nguồn, dark/light mode, SourceManagerActivity                                                                                                                                                                     |
| v3.0.0 | Single Activity + Drawer, 4 chế độ xem (Month/Day/Year/Agenda), CRUD sự kiện tay, âm lịch, parse VALARM                                                                                                                                        |
| v3.1.0 | Fix backstack, fix chấm multi-day, tách EventDetail/AddEvent thành Activity, BaseActivity, tap notification → EventDetail, nút Hôm nay, thông báo linh hoạt (số + đơn vị), toolbar gọn, drawer highlight, fix AgendaFragment multiple observer |
| v3.2.0 | Fix tap notification sự kiện (eventId=0), tối ưu Year View ~35% bằng Custom Canvas (`MiniMonthView`) thay GridLayout, fix auto-scroll không hoạt động nếu hôm nay không có sự kiện                                                             |

---

## 📋 Việc cần làm tiếp theo (tương lai)

- **Custom Canvas MonthFragment** — thay `KizitonwoseCalendarView` bằng Custom View vẽ Canvas (tương tự `MiniMonthView`); hướng duy nhất giải quyết frame time ~46ms hiện tại (GPU-bound)
- **Chia Activity hoàn toàn** — xem xét chuyển toàn bộ sang Navigation Component
- **Polish padding/margin** — rà soát toàn bộ layout trên nhiều kích thước màn hình
- **Agenda "Về hôm nay"** — thêm nút riêng trong AgendaFragment
- **Sync Google Calendar** — tích hợp CalDAV hoặc Google Calendar API
- **Widget màn hình chính** — hiển thị sự kiện hôm nay ngay ngoài home screen
- **Tháng nhuận âm lịch** — hiển thị ký hiệu "(Nhuận)" cho tháng nhuận

---

## 🤝 Đóng Góp

1. **Fork** repository này.
2. Tạo nhánh mới: `git checkout -b feature/TinhNangMoi`
3. Commit thay đổi: `git commit -m 'feat: thêm tính năng X'`
4. Push: `git push origin feature/TinhNangMoi`
5. Tạo **Pull Request**.

## ⭐️ Ủng hộ

Nếu thấy dự án hữu ích, hãy để lại một **Star** ⭐️!

---

*Developed by **An_K4**.*