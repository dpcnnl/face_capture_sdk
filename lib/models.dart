class FaceSDKConfig {
  final int threadPool;
  final double maskGlassesThreshold;
  final double faceValidThreshold;
  final double faceDirectionThreshold;
  final int imageUnderexposedThreshold;
  final int imageTooBrightThreshold;
  final int imageBlurThreshold;
  final int widthImageThreshold;
  final double lipRatioThreshold;
  final double openEyeThreshold;
  final double thresholdSearch;
  final int distanceEye;
  final int minFaceSize;
  final int maxFaceSize;
  final List<int> regionRectDetect;
  final int previewFps;

  const FaceSDKConfig({
    this.threadPool = 3,
    this.maskGlassesThreshold = 0.5,
    this.faceValidThreshold = 0.5,
    this.faceDirectionThreshold = 0.5,
    this.imageUnderexposedThreshold = 100,
    this.imageTooBrightThreshold = 1500,
    this.imageBlurThreshold = 5,
    this.widthImageThreshold = 720,
    this.lipRatioThreshold = 0.5,
    this.openEyeThreshold =  0.12,
    this.thresholdSearch = 0.83,
    this.distanceEye = 120,
    this.minFaceSize = 80,
    this.maxFaceSize = 240,
    this.regionRectDetect = const [0, 0, 1500, 1800],
    this.previewFps = 15,
  });

  Map<String, dynamic> toMap() {
    return {
      'threadPool': threadPool,
      'maskGlassesThreshold': maskGlassesThreshold,
      'faceValidThreshold': faceValidThreshold,
      'faceDirectionThreshold': faceDirectionThreshold,
      'imageUnderexposedThreshold': imageUnderexposedThreshold,
      'imageTooBrightThreshold': imageTooBrightThreshold,
      'imageBlurThreshold': imageBlurThreshold,
      'widthImageThreshold': widthImageThreshold,
      'lipRatioThreshold': lipRatioThreshold,
      'openEyeThreshold': openEyeThreshold,
      'thresholdSearch': thresholdSearch,
      'distanceEye': distanceEye,
      'minFaceSize': minFaceSize,
      'maxFaceSize': maxFaceSize,
      'regionRectDetect': regionRectDetect,
      'previewFps': previewFps,
    };
  }
}

class SDKResponse<T> {
  final int status;
  final String message;
  final T? data;

  SDKResponse({
    required this.status,
    required this.message,
    this.data,
  });

  factory SDKResponse.success(T data) {
    return SDKResponse(status: 0, message: '', data: data);
  }

  factory SDKResponse.error(String message) {
    return SDKResponse(status: 1, message: message);
  }
}

class Data {
  final String base64Image;
  final String base64ImageBlur;
  final double qualityScore;
  final double livenessScore;

  Data({
    required this.base64Image,
    required this.base64ImageBlur,
    required this.qualityScore,
    required this.livenessScore,
  });

  factory Data.fromMap(Map<dynamic, dynamic> map) {
    return Data(
      base64Image: map['base64Image'] ?? '',
      base64ImageBlur: map['base64ImageBlur'] ?? '',
      qualityScore: map['qualityScore']?.toDouble() ?? 0.0,
      livenessScore: map['livenessScore']?.toDouble() ?? 0.0,
    );
  }
}
