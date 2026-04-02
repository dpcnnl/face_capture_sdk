import 'package:flutter_test/flutter_test.dart';
import 'package:face_capture_sdk/face_capture_sdk.dart';
import 'package:face_capture_sdk/face_capture_sdk_platform_interface.dart';
import 'package:face_capture_sdk/face_capture_sdk_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFaceCaptureSdkPlatform
    with MockPlatformInterfaceMixin
    implements FaceCaptureSdkPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final FaceCaptureSdkPlatform initialPlatform = FaceCaptureSdkPlatform.instance;

  test('$MethodChannelFaceCaptureSdk is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFaceCaptureSdk>());
  });

  test('getPlatformVersion', () async {
    FaceCaptureSdk faceCaptureSdkPlugin = FaceCaptureSdk();
    MockFaceCaptureSdkPlatform fakePlatform = MockFaceCaptureSdkPlatform();
    FaceCaptureSdkPlatform.instance = fakePlatform;

    expect(await faceCaptureSdkPlugin.getPlatformVersion(), '42');
  });
}
