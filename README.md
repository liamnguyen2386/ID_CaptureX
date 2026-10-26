# ID CaptureX CameraX Native v4.0 — hướng dẫn cho người mới

## Mục tiêu
- Ứng dụng Android Native Java, không dùng WebView; CameraX chụp liên tục, không có Use Photo / Retake.
- Màn hình đầu: logo Cooler Master (tệp logo.png), Chụp ảnh Căn cước, Bắt đầu chụp ảnh.
- Màn hình camera: 1 nút chụp, các bộ đếm trắng/đỏ/xanh.
- Ảnh JPEG chất lượng 72 (tùy máy, không bảo đảm còn 20%); ghi tệp ngay trong bộ nhớ riêng Android, không chờ mạng.
- WorkManager đồng bộ khi có mạng, kể cả khi giao diện đóng, tùy hạn chế Android.
- Thư mục Drive theo ngày chụp, múi giờ Việt Nam.

## Bước 1: Backend
Sử dụng Code.gs của Web App trước đó có `doPost(e)` nhận form fields: `secret`, `id`, `stamp`, `mime`, `data` và trả JSON `{ok:true}`. KHÔNG dùng Code.gs kiểu JSON `base64` của bản APK cũ. Không thay đổi deployment nếu backend này đã chạy. Dán URL `/exec` và mật khẩu UPLOAD_SECRET ở lần chạy đầu.

## Bước 2: GitHub
Tạo repository MỚI `ID-Capture-CameraX-Native`, tránh ghi đè repo Android WebView đang chạy. Tải lên toàn bộ nội dung thư mục này, giữ cấu trúc `.github/workflows/build.yml`, `app/...`.

## Bước 3: Build
GitHub → Actions → Build ID CaptureX CameraX APK → Run workflow. Khi xanh, vào Artifacts tải `ID-CaptureX-CameraX-Native-APK`, giải nén và cài `app-debug.apk`.

## Bước 4: Chạy thử
Mở ID CaptureX → Bắt đầu chụp ảnh → nhập URL Apps Script `/exec` và UPLOAD_SECRET (lần đầu) → cấp quyền Camera. Chụp 5 ảnh thử KHÔNG nhạy cảm; kiểm tra `Đã chụp`, `Chờ đồng bộ`, `Đã đồng bộ` và Google Drive thư mục theo ngày.

## Lưu ý quan trọng
- `Đã chụp` là số ảnh đã lưu thành công trong phiên mở màn camera; `Đã đồng bộ` là tổng ảnh đồng bộ của ứng dụng từ khi cài, không chỉ phiên.
- Không gỡ app hoặc xóa dữ liệu app khi còn ảnh chờ gửi. Bản này có applicationId mới `com.cm.idcapture.nativecamera` để cài song song với WebView.
- Đây là mã nguồn chưa được biên dịch/kiểm thử trên thiết bị. Nếu build lỗi, gửi GitHub Actions log.
- Mật khẩu upload được lưu trong SharedPreferences của ứng dụng (không mã hóa), phù hợp thử nghiệm, CHƯA đạt mức bảo mật cần thiết cho dữ liệu Căn cước. Backend công khai cần được kiểm soát quyền truy cập, giới hạn tải lên, nhật ký và chính sách lưu trữ trước khi triển khai thực tế.
- Ảnh hiện được chụp JPEG trực tiếp, không tự cắt hoặc xoay theo EXIF; cần kiểm tra hướng ảnh thực tế trên máy. Chất lượng và độ phân giải tùy CameraX/thiết bị.

Tên ứng dụng hiển thị: **ID CaptureX**. Giữ nguyên applicationId để có thể cập nhật phiên bản Native trước (nếu chữ ký tương thích).
