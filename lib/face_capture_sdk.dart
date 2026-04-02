import 'dart:async';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'models.dart';

export 'models.dart';

class FaceCaptureSdk {
  static const MethodChannel _methodChannel = MethodChannel('face_capture_sdk');
  static const EventChannel _eventChannel = EventChannel('face_capture_sdk/preview');

  StreamSubscription? _previewSubscription;

  Future<SDKResponse<Data>> captureLiveFace({
    Duration timeout = const Duration(seconds: 45),
    double outputWidth = 720,
    double outputHeight = 1280,
    FaceSDKConfig config = const FaceSDKConfig(),
    required void Function(String base64) onPreview,
  }) async {
    // Listen to preview frames
    _previewSubscription = _eventChannel.receiveBroadcastStream().listen((event) {
      if (event is String) {
        onPreview(event);
      }
    });

    try {
      // Yêu cầu quyền Camera tự động
      var status = await Permission.camera.status;
      if (!status.isGranted) {
        status = await Permission.camera.request();
        if (!status.isGranted) {
          return SDKResponse.error('Camera permission is required');
        }
      }

      final result = await _methodChannel.invokeMethod('captureLiveFace', {
        'timeoutSeconds': timeout.inSeconds,
        'outputWidth': outputWidth,
        'outputHeight': outputHeight,
        'config': config.toMap(),
      });

      if (result != null && result is Map) {
        if (result['status'] == 0) {
          final data = Data.fromMap(result['data'] as Map);
          return SDKResponse.success(data);
        } else {
          return SDKResponse.error(result['message'] ?? 'Unknown error');
        }
      }
      return SDKResponse.error('Invalid response from native code');
    } on PlatformException catch (e) {
      return SDKResponse.error(e.message ?? 'Platform exception');
    } finally {
      // Clean up subscription
      await _previewSubscription?.cancel();
      _previewSubscription = null;
    }
  }
}
