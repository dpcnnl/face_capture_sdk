package com.example.face_capture_sdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.etc.facesdkfull.core.facedetect.Detection;
import com.etc.facesdkfull.core.facedetect.FaceDetectionMode;
import com.etc.facesdkfull.core.facedetect.FaceQualityResult;
import com.etc.facesdkfull.core.facedetect.FaceSDK;
import com.etc.facesdkfull.core.facedetect.SDKSetting;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class FaceCaptureSdkPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, EventChannel.StreamHandler {
    private MethodChannel methodChannel;
    private EventChannel eventChannel;
    private Context context;
    private Activity activity;
    private EventChannel.EventSink eventSink;

    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;

    private FaceSDK faceSDK;
    private boolean isCapturing = false;
    private Result pendingResult;

    private int captureTimeoutSeconds = 45;
    private double outputWidth = 720;
    private double outputHeight = 1280;
    private int previewFps = 15;

    private long lastFrameTime = 0;

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (isCapturing) {
                stopCaptureAndReturn(1, "Timeout", null);
            }
        }
    };

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        context = flutterPluginBinding.getApplicationContext();
        methodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "face_capture_sdk");
        methodChannel.setMethodCallHandler(this);

        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "face_capture_sdk/preview");
        eventChannel.setStreamHandler(this);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("captureLiveFace")) {
            if (isCapturing) {
                result.error("BUSY", "Already capturing", null);
                return;
            }

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, 100);
                result.error("PERMISSION_DENIED", "Camera permission is required", null);
                return;
            }

            captureTimeoutSeconds = call.argument("timeoutSeconds");
            outputWidth = call.argument("outputWidth");
            outputHeight = call.argument("outputHeight");
            Map<String, Object> configMap = call.argument("config");
            if (configMap != null && configMap.containsKey("previewFps")) {
                previewFps = (int) configMap.get("previewFps");
            } else {
                previewFps = 15;
            }
            initFaceSDK(configMap);
            startCamera(result);
        } else {
            result.notImplemented();
        }
    }

    private void initFaceSDK(Map<String, Object> configMap) {
        try {
            int threadPool = (int) configMap.get("threadPool");
            double maskGlassesThreshold = (double) configMap.get("maskGlassesThreshold");
            double faceValidThreshold = (double) configMap.get("faceValidThreshold");
            double faceDirectionThreshold = (double) configMap.get("faceDirectionThreshold");
            int imageUnderexposedThreshold = (int) configMap.get("imageUnderexposedThreshold");
            int imageTooBrightThreshold = (int) configMap.get("imageTooBrightThreshold");
            int imageBlurThreshold = (int) configMap.get("imageBlurThreshold");
            int widthImageThreshold = (int) configMap.get("widthImageThreshold");
            double lipRatioThreshold = (double) configMap.get("lipRatioThreshold");
            double openEyeThreshold = (double) configMap.get("openEyeThreshold");
            double thresholdSearch = (double) configMap.get("thresholdSearch");
            int distanceEye = (int) configMap.get("distanceEye");
            int minFaceSize = (int) configMap.get("minFaceSize");
            int maxFaceSize = (int) configMap.get("maxFaceSize");

            Rect regionRectDetect = new Rect(0, 0, 1500, 1800);
            if (configMap.containsKey("regionRectDetect")) {
                List<Integer> rectList = (List<Integer>) configMap.get("regionRectDetect");
                if (rectList != null && rectList.size() == 4) {
                    regionRectDetect = new Rect(rectList.get(0), rectList.get(1), rectList.get(2), rectList.get(3));
                }
            }

            SDKSetting option = new SDKSetting(
                    threadPool,
                    (float)maskGlassesThreshold,
                    (float)faceValidThreshold,
                    (float)imageUnderexposedThreshold,
                    (float)imageTooBrightThreshold,
                    (float)imageBlurThreshold,
                    (float)widthImageThreshold,
                    (float)distanceEye,
                    (float)lipRatioThreshold,
                    (float)openEyeThreshold,
                    (float)thresholdSearch,
                    minFaceSize,
                    maxFaceSize,
                    regionRectDetect,
                    FaceDetectionMode.NORMAL
            );

            faceSDK = new FaceSDK(context, activity.getAssets(), option);
        } catch (Exception e) {
            Log.e("FaceSDK", "Error initializing FaceSDK", e);
        }
    }

    private void startCamera(Result result) {
        if (activity == null || !(activity instanceof LifecycleOwner)) {
            result.error("NO_ACTIVITY", "Activity is not a LifecycleOwner", null);
            return;
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
        isCapturing = true;
        pendingResult = result;

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                imageAnalysis = new ImageAnalysis.Builder()
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @OptIn(markerClass = ExperimentalGetImage.class)
                    @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        try {
                            if (!isCapturing) {
                                imageProxy.close();
                                return;
                            }
                            processFrame(imageProxy);
                        } finally {
                            imageProxy.close();
                        }
                    }
                });

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                cameraProvider.bindToLifecycle((LifecycleOwner) activity, cameraSelector, imageAnalysis);

                mainHandler.postDelayed(timeoutRunnable, captureTimeoutSeconds * 1000L);

            } catch (Exception e) {
                stopCaptureAndReturn(1, "Camera bind error: " + e.getMessage(), null);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void processFrame(ImageProxy imageProxy) {
        Bitmap bitmap = imageProxy.toBitmap();
        if (bitmap == null) return;

        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
        if (rotationDegrees != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);
            matrix.preScale(-1.0f, 1.0f);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            bitmap = rotatedBitmap;
        } else {
            Matrix matrix = new Matrix();
            matrix.preScale(-1.0f, 1.0f);
            Bitmap flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            bitmap = flippedBitmap;
        }

        long now = System.currentTimeMillis();
        // Giới hạn FPS
        int delayMs = previewFps > 0 ? 1000 / previewFps : 0;
        if (eventSink != null && (now - lastFrameTime) > delayMs) {
            sendPreviewFrame(bitmap);
            lastFrameTime = now;
        }

        if (faceSDK != null) {
            List<Detection> detections = faceSDK.getFaces(bitmap);
            if (detections != null && !detections.isEmpty()) {
                Detection target = detections.get(0);
                
                // Tránh crash nếu SDK lấy location sai lệch / lọt vào sát mép ảnh cạnh góc -> width/height cắt mắt <= 0
                Rect loc = target.getLocation();
                if (loc != null && loc.width() > 20 && loc.height() > 20) {
                    try {
                        FaceQualityResult qualityResult = faceSDK.checkFaceQualityResult(target);
                        if (qualityResult != null && !Boolean.TRUE.equals(qualityResult.isTooBigFace) &&
                                !Boolean.TRUE.equals(qualityResult.isTooSmallFace) &&
                                // !Boolean.TRUE.equals(qualityResult.eyesDistanceTooSmall) &&
                                // !Boolean.TRUE.equals(qualityResult.rightEyeIsClosed) &&
                                // !Boolean.TRUE.equals(qualityResult.leftEyeIsClosed) &&
                                // !Boolean.TRUE.equals(qualityResult.notMeetLipRatio) &&
                                !Boolean.TRUE.equals(qualityResult.faceNotStraight)
                                // !Boolean.TRUE.equals(qualityResult.faceNotValid) &&
                                // !Boolean.TRUE.equals(qualityResult.isLeftEyeBlur) &&
                                // !Boolean.TRUE.equals(qualityResult.isRightEyeBlur) &&
                                // !Boolean.TRUE.equals(qualityResult.isMouthBlur)
                            ) {

                                Boolean wearMaskGlasses = faceSDK.checkMaskGlasses(target);
                                if (!Boolean.TRUE.equals(wearMaskGlasses)) {
                                    Boolean isLiveness = faceSDK.checkFaceLiveness(target);
                                    if (Boolean.TRUE.equals(isLiveness)) {
                                        successCapture(bitmap, target);
                                        return;
                                    }
                                }
                        }
                    } catch (Exception e) {
                        Log.w("FaceCaptureSdk", "Bỏ qua frame do lỗi bên trong SDK khi tính toán quality/liveness (Width <= 0)", e);
                    }
                }
            }
        }
        
        bitmap.recycle();
        System.gc();
    }

    private void successCapture(Bitmap originalBitmap, Detection detection) {
        Bitmap rawCropped = detection.getBitmap(1.5f);
        Bitmap rawBlurred = detection.getBlurBitmap(1.7f, context);

        Bitmap cropped = Bitmap.createScaledBitmap(rawCropped, (int)outputWidth, (int)outputHeight, true);
        Bitmap blurred = Bitmap.createScaledBitmap(rawBlurred, (int)outputWidth, (int)outputHeight, true);

        if (rawCropped != cropped) rawCropped.recycle();
        if (rawBlurred != blurred) rawBlurred.recycle();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        cropped.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] b = baos.toByteArray();
        String base64 = Base64.encodeToString(b, Base64.NO_WRAP);

        ByteArrayOutputStream baosBlur = new ByteArrayOutputStream();
        blurred.compress(Bitmap.CompressFormat.JPEG, 90, baosBlur);
        String base64Blur = Base64.encodeToString(baosBlur.toByteArray(), Base64.NO_WRAP);
        
        blurred.recycle();
        cropped.recycle();
        originalBitmap.recycle();
        
        Map<String, Object> data = new HashMap<>();
        data.put("base64Image", base64);
        data.put("base64ImageBlur", base64Blur);
        data.put("qualityScore", 1.0);
        data.put("livenessScore", 1.0);

        stopCaptureAndReturn(0, "", data);
    }

    private void sendPreviewFrame(Bitmap originalBitmap) {
        Matrix matrix = new Matrix();
        float scale = 320f / Math.max(originalBitmap.getWidth(), originalBitmap.getHeight());
        matrix.postScale(scale, scale);
        
        Bitmap previewBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
        final String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        previewBitmap.recycle();
        
        mainHandler.post(() -> {
            if (eventSink != null) {
                eventSink.success(base64);
            }
        });
    }

    private void stopCaptureAndReturn(int status, String message, Map<String, Object> data) {
        if (!isCapturing) return;
        isCapturing = false;
        
        mainHandler.removeCallbacks(timeoutRunnable);
        
        if (cameraProvider != null) {
            mainHandler.post(() -> {
               cameraProvider.unbindAll(); 
               cameraProvider = null;
            });
        }
        
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
            cameraExecutor = null;
        }

        if (faceSDK != null) {
            faceSDK = null; // Clear SDK ref if needed, or leave initialized
        }

        if (pendingResult != null) {
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("status", status);
            resultMap.put("message", message);
            if (data != null) {
                resultMap.put("data", data);
            }
            final Result rs = pendingResult;
            pendingResult = null;
            mainHandler.post(() -> rs.success(resultMap));
        }

        System.gc(); // Clear memory
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        this.eventSink = events;
    }

    @Override
    public void onCancel(Object arguments) {
        this.eventSink = null;
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        methodChannel.setMethodCallHandler(null);
        eventChannel.setStreamHandler(null);
    }
}
