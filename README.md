# Hướng dẫn tích hợp face_caputure_sdk

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
    outputWidth: 720,                     // Kích thước chiều ngang output
    outputHeight: 1280,                   // Kích thước chiều cao output

    // Gọi hàm lắng nghe từng hệ khung hình để hiển thị Preview lên giao diện
    onPreview: (String base64Frame) {
      // Thực hiện cấp phát lại giao diện (Ví dụ gọi hàm setState)
    },

    // Thiết lập tùy chọn cấu hình
    config: const FaceSDKConfig(
      previewFps: 15,
      threadPool: 3,
      regionRectDetect: [0, 0, 1500, 1800],
    ),
  );

  // Xử lý dữ liệu trả về
  if (response.status == 0 && response.data != null) {
      print("Ảnh crop: ${response.data!.base64Image}");
      print("Ảnh crop có làm mờ xung quanh: ${response.data!.base64ImageBlur}");
      print("Điểm chất lượng ảnh: ${response.data!.qualityScore}");
      print("Điểm liveness: ${response.data!.livenessScore}");
  } else {
      print("Thông báo lỗi: ${response.message}");
  }
}
```

## 3. Cấu hình các bộ tham số (`FaceSDKConfig`)

| Tham số                      | Định dạng   | Mặc định             | Ý nghĩa chức năng                                                                                              |
| ---------------------------- | ----------- | -------------------- | -------------------------------------------------------------------------------------------------------------- |
| `previewFps`                 | `int`       | `15`                 | Giới hạn tần số chuyển tải khung hình về Dart thông qua callback `onPreview`.                                  |
| `regionRectDetect`           | `List<int>` | `[0, 0, 1500, 1800]` | Kích thước khu vực khoanh vùng cho phép quét khuôn mặt nằm dưới dạng hình chữ nhật `[Trái, Trên, Phải, Dưới]`. |
| `minFaceSize`                | `int`       | `0`                  | Kích thước khuôn mặt bé nhất.                                                                                  |
| `maxFaceSize`                | `int`       | `1000`               | Kích thước khuôn mặt lớn nhất.                                                                                 |
| `maskGlassesThreshold`       | `double`    | `0.5`                | Ngưỡng cho phép khi phát hiện kính, khẩu trang.                                                                |
| `distanceEye`                | `int`       | `120`                | Khoảng cách chấp nhận giữa 2 mắt.                                                                              |
| `openEyeThreshold`           | `double`    | `0.12`               | Ngưỡng chấp nhận mở mắt.                                                                                       |
| `lipRatioThreshold`          | `double`    | `0.5`                | Ngưỡng chấp nhận mở môi.                                                                                       |
| `imageUnderexposedThreshold` | `int`       | `100`                | Ngưỡng ảnh tối.                                                                                                |
| `imageTooBrightThreshold`    | `int`       | `1500`               | Ngưỡng ảnh sáng.                                                                                               |
| `imageBlurThreshold`         | `int`       | `5`                  | Ngưỡng kiểm tra blur.                                                                                          |
| `threadPool`                 | `int`       | `3`                  | Số luồng xử lý.                                                                                                |
