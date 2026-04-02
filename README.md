## 1. Hướng dẫn cài đặt

Tích hợp SDK vào dự án Flutter thông qua liên kết Git.
Trong tệp `pubspec.yaml` của ứng dụng, bổ sung cấu trúc sau vào phần `dependencies`:

```yaml
dependencies:
  flutter:
    sdk: flutter
  face_capture_sdk:
    git:
      url: https://github.com/dpcnnl/face_capture_sdk.git
      ref: v1.0.0
```

Sau đó thực thi lệnh tải xuống các cập nhật phụ thuộc:
```bash
flutter pub get
```

## 2. Gọi hàm chức năng

```dart
import 'package:flutter/material.dart';
import 'package:face_capture_sdk/face_capture_sdk.dart';
import 'dart:convert';

// Khởi tạo đối tượng
final _faceCaptureSdkPlugin = FaceCaptureSdk();

// Triển khai chức năng gọi
Future<void> startCapture() async {
  final response = await _faceCaptureSdkPlugin.captureLiveFace(
    timeout: const Duration(seconds: 45), // Thời gian đợi tối đa trước khi hủy bỏ theo dõi
    outputWidth: 720,                     // Kích thước chiều ngang của kết quả cắt (Crop)
    outputHeight: 1280,                   // Kích thước chiều cao của kết quả cắt (Crop)
    
    // Gọi hàm lắng nghe từng hệ khung hình để hiển thị Preview lên giao diện
    onPreview: (String base64Frame) { 
      // Thực hiện cấp phát lại giao diện (Ví dụ gọi hàm setState)
    },
    
    // Thiết lập tùy chọn cấu hình tính toán điểm ảnh nâng cao cho nhân (Core API) Native
    config: const FaceSDKConfig(
      previewFps: 15,
      threadPool: 3,
      regionRectDetect: [0, 0, 1500, 1800],
    ),
  );

  // Xử lý dữ liệu trả về 
  if (response.status == 0 && response.data != null) {
      print("Ảnh góc crop chuẩn [Base64]: ${response.data!.base64Image}");
      print("Ảnh làm dở/mờ [Base64]: ${response.data!.base64ImageBlur}");
      print("Điểm số chất lượng: ${response.data!.qualityScore}");
      print("Xác thực Liveness: ${response.data!.livenessScore}");
  } else {
      print("Phản hồi lỗi phát sinh: ${response.message}");
  }
}
```

## 3. Cấu hình các bộ tham số (`FaceSDKConfig`)

Việc truyền vào `FaceSDKConfig` sẽ cho phép ghi đè các cấu hình tính toán và đánh giá chỉ tiêu của luồng xử lý ảnh sâu trong Android.

| Tham số | Định dạng | Mặc định | Ý nghĩa chức năng |
|----------|--------------|----------|--------|
| `previewFps` | `int` | `15` | Giới hạn tần số chuyển tải khung hình về Dart thông qua callback `onPreview`. |
| `regionRectDetect` | `List<int>` | `[0, 0, 1500, 1800]` | Kích thước khu vực khoanh vùng cho phép quét khuôn mặt nằm dưới dạng hình chữ nhật `[Trái, Trên, Phải, Dưới]`. |
| `minFaceSize` | `int` | `0` | Kích thước tiêu chuẩn nhỏ nhất được công nhận là một khuôn mặt. |
| `maxFaceSize` | `int` | `1000` | Kích thước khoanh vùng khống chế lớn nhất của thuật toán. |
| `maskGlassesThreshold` | `double` | `0.5` | Ngưỡng cho phép khi phát hiện kính hiển vi, khẩu trang hoặc đồ vật che giấu. |
| `distanceEye` | `int` | `120` | Quy định chuẩn cự ly cách biệt của đôi mắt trong bức ảnh tĩnh. |
| `openEyeThreshold` | `double` | `0.12` | Mức đánh giá để lường độ hở của mắt, chống lại hành vi nhắm mắt. |
| `lipRatioThreshold` | `double` | `0.5` | Ngưỡng đánh giá độ dị thường qua tỷ lệ kích thước đôi môi. |
| `imageUnderexposedThreshold`| `int` | `100` | Cán cân phát hiện khung hình chiếu sáng thiếu sáng. Sẽ tự động từ chối nếu ảnh quá mờ/tối. |
| `imageTooBrightThreshold` | `int` | `1500` | Cán cân phát hiện khung hình lộ quá nhiều ánh sáng (Chói lóa/Cháy sáng). |
| `imageBlurThreshold` | `int` | `5` | Chỉ số quy ước chấp thuận tính sắc nét của viền phân giải. |
| `threadPool` | `int` | `3` | Quy định mức độ phân bổ luồng CPU giải quyết toán đồ họa trên thiết bị Android. |

Mọi khung hình bất đồng bộ được trích xuất từ mô-đun CameraX sẽ trải qua toàn bộ bài kiểm tra `FaceSDKConfig` và `Liveness`. Chỉ một hình ảnh hoàn hảo vượt qua xác thực mới kích hoạt trạng thái trả về thành công kèm giải phóng sạch phân luồng của vòng lặp RAM phần cứng. Các sai lệch sẽ tự động được gạt bỏ và bỏ qua để tránh gây lỗi (Crashing).
