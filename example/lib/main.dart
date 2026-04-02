import 'package:flutter/material.dart';
import 'package:face_capture_sdk/face_capture_sdk.dart';
import 'dart:convert';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _faceCaptureSdkPlugin = FaceCaptureSdk();
  String _status = 'Idle';
  String _previewBase64 = '';
  String _resultBase64 = '';
  String _resultBase64Blur = '';

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('FaceSDK Example'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (_previewBase64.isNotEmpty)
                Image.memory(
                  base64Decode(_previewBase64),
                  width: 150,
                  height: 200,
                  fit: BoxFit.cover,
                  gaplessPlayback: true,
                ),
              if (_resultBase64.isNotEmpty)
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Image.memory(
                    base64Decode(_resultBase64),
                    width: 150,
                    height: 200,
                    fit: BoxFit.cover,
                  ),
                ),
              if (_resultBase64Blur.isNotEmpty)
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Image.memory(
                    base64Decode(_resultBase64Blur),
                    width: 150,
                    height: 200,
                    fit: BoxFit.cover,
                  ),
                ),
              Text('Status: $_status\n'),
              ElevatedButton(
                onPressed: () async {
                  setState(() {
                    _status = 'Capturing...';
                    _resultBase64 = '';
                    _previewBase64 = '';
                    _resultBase64Blur = '';
                  });
                  final response = await _faceCaptureSdkPlugin.captureLiveFace(
                    onPreview: (base64) {
                      setState(() {
                        _previewBase64 = base64;
                      });
                    },
                  );
                  setState(() {
                    _status = response.status == 0
                        ? 'Success: Quality ${response.data?.qualityScore}'
                        : 'Error: ${response.message}';
                    if (response.status == 0 && response.data != null) {
                      _resultBase64 = response.data!.base64Image;
                      _resultBase64Blur = response.data!.base64ImageBlur;
                    }
                    _previewBase64 = ''; // Clear preview
                  });
                },
                child: const Text('Capture Live Face'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
