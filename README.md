# My Schedule - Ứng dụng Lịch Android từ file ICS

My Schedule là một ứng dụng lịch đơn giản dành cho Android, cho phép người dùng nhập và xem các sự kiện từ một file Lịch iCalendar (`.ics`). Ứng dụng được xây dựng ban đầu với mục tiêu giúp người phát triển xem thông tin lịch học qua từng học kì một cách trực quan.

Đây là phiên bản **1.0.0** của dự án.

## ✨ Tính năng chính

*   **Hiển thị Lịch theo Tháng:** Giao diện lịch rõ ràng, cho phép cuộn qua lại giữa các tháng.
*   **Nhập Sự kiện từ file `.ics`:** Dễ dàng chọn và nhập một file `.ics` từ bộ nhớ của thiết bị để hiển thị tất cả các sự kiện.
*   **Hiển thị Sự kiện trong Ngày:** Khi chọn một ngày cụ thể, ứng dụng sẽ liệt kê tất cả các sự kiện diễn ra trong ngày đó.
*   **Xem Chi tiết Sự kiện:** Nhấn vào một sự kiện để xem chi tiết, bao gồm tiêu đề, địa điểm và mô tả.
*   **Thông báo Sự kiện:** Tự động lên lịch và gửi thông báo cho người dùng 30 phút trước khi một sự kiện bắt đầu.
*   **Lưu trữ Bền vững:** Ứng dụng sẽ ghi nhớ file `.ics` bạn đã chọn lần cuối và tự động tải lại lịch mỗi khi bạn mở ứng dụng.

## 🛠️ Công nghệ sử dụng

*   **Ngôn ngữ:** [Kotlin](https://kotlinlang.org/)
*   **Kiến trúc:** Single Activity, ViewBinding
*   **Giao diện Lịch:** [Kizitonwose Calendar View](https://github.com/kizitonwose/Calendar) - Một thư viện mạnh mẽ và linh hoạt để tạo giao diện lịch tùy chỉnh.
*   **Phân tích `.ics`:** [iCal4j](https://github.com/ical4j/ical4j) - Thư viện Java tiêu chuẩn để đọc và xử lý dữ liệu iCalendar.
*   **Thông báo & Lên lịch:** `AlarmManager`, `BroadcastReceiver`, và `NotificationManager` của Android SDK.
*   **Giao diện người dùng:** Material Components for Android.

## 🚀 Hướng dẫn Build

1.  Clone repository này về máy của bạn.

    ```bash
    git clone https://github.com/An-K4/MyScheduleApp.git
    cd myscheduleapp
    ```

2.  Mở dự án bằng phiên bản Android Studio mới nhất.
3.  Đợi Gradle đồng bộ hóa tất cả các thư viện phụ thuộc.
4.  Build và chạy ứng dụng trên máy ảo hoặc thiết bị thật.

## 📸 Ảnh chụp màn hình

<img src="https://github.com/user-attachments/assets/567afe1c-6fce-4ec5-a796-dbc266ae549b" alt="Giao diện xem lịch" width="600">

## 🤝 Đóng Góp (Contributing)

Nếu bạn muốn tham gia đóng góp:

1.  **Fork** repository này về tài khoản của bạn.
2.  Tạo nhánh mới cho tính năng của bạn (`git checkout -b feature/TinhNangMoi`).
3.  Commit những thay đổi (`git commit -m 'Thêm tính năng X'`).
4.  Push lên nhánh của bạn (`git push origin feature/TinhNangMoi`).
5.  Tạo một **Pull Request** trên GitHub.

## ⭐️ Ủng hộ

Nếu bạn thấy dự án này thú vị hoặc hữu ích, hãy để lại một **Star** ⭐️ để động viên tinh thần cả nhóm nhé!

---
Developed with ❤️ by **An_K4**.