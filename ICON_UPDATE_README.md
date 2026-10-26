# ID CaptureX v4.2 — icon ứng dụng

Đã dùng ảnh `id-card-color-icon.png` do bạn cung cấp để tạo icon Android ở các mật độ mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi.

Các thay đổi:
- `app/src/main/AndroidManifest.xml`: thêm `android:icon` và `android:roundIcon`.
- `app/src/main/res/mipmap-*/ic_launcher.png` và `ic_launcher_round.png`: biểu tượng mới.

Để cập nhật GitHub, tải toàn bộ nội dung ZIP lên repository CameraX (giữ cấu trúc thư mục), commit rồi chạy Actions. Nếu chỉ muốn sửa icon, cập nhật manifest và các thư mục `mipmap-*`.

Chưa chạy GitHub Actions hay thử cài đặt trên thiết bị. Một số launcher có thể tự thêm nền hoặc cắt icon thành hình tròn.
