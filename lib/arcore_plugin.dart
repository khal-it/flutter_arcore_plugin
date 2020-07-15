import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/services.dart' show rootBundle;

typedef ArCoreViewCreatedCallback = void Function(
    ArCoreViewController controller);

class ArCoreView extends StatefulWidget {
  const ArCoreView(
      {Key key,
      this.onArCoreViewCreated,
      this.onImageRecognized,
      this.width = 300,
      this.height = 300,
      this.focusBox})
      : super(key: key);

  final double width;
  final double height;
  final Function onImageRecognized;
  final ArCoreViewCreatedCallback onArCoreViewCreated;
  final Widget focusBox;

  @override
  _ArCoreViewState createState() => _ArCoreViewState();
}

class _ArCoreViewState extends State<ArCoreView> with WidgetsBindingObserver {
  @override
  void initState() {
    WidgetsBinding.instance.addObserver(this);
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return Container(
          width: widget.width,
          height: widget.height,
          child: Stack(
            alignment: Alignment.center,
            children: <Widget>[
              AndroidView(
                viewType: 'plugins.peqas.com/arcore_plugin',
                onPlatformViewCreated: _onPlatformViewCreated,
              ),
              if (widget.focusBox != null) widget.focusBox else Container()
            ],
          ));
    }
    return Text(
        '$defaultTargetPlatform is not  supported by the ar_view plugin');
  }

  void _onPlatformViewCreated(int id) {
    if (widget.onArCoreViewCreated == null) {
      return;
    }
    widget.onArCoreViewCreated(
        ArCoreViewController(id, widget.onImageRecognized));
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }
}

class ArCoreViewController {
  ArCoreViewController(int id, this.onImageRecognized) {
    _channel = MethodChannel('plugins.peqas.com/arcore_plugin_$id');
    _channel.setMethodCallHandler(_handleMethodCalls);
  }
  final Function onImageRecognized;

  MethodChannel _channel;

  Future<dynamic> _handleMethodCalls(MethodCall methodCall) async {
    switch (methodCall.method) {
      case 'image_recognized':
        debugPrint(methodCall.arguments.toString());
        final String recognizedImgName = methodCall.arguments.toString();
        onImageRecognized(recognizedImgName);
        return Future<String>.value(recognizedImgName);
    }
  }

  Future<void> resumeImageRecognition() async {
    debugPrint('resuming image recognition');
    return _channel.invokeMethod('resume_image_recognition');
  }

  Future<void> pauseImageRecognition() async {
    debugPrint('pausing image recongition');
    return _channel.invokeMethod('pauseImageRecognition');
  }

  Future<void> getArCoreView() async {
    return _channel.invokeMethod('recognize_images');
  }

  /// example of [tempFilePath] : /data/user/0/<applicationId>/cache/<name_of_imgdb.imgdb>
  Future<void> loadImgdbFromAssets(
      {@required String tempFilePath,
      String imgdbAssetPath = 'assets/image_database.imgdb'}) async {
    final File tempFile = File(tempFilePath);

    // create tempfile
    await tempFile.create();

    final ByteData loadedImgdbByteData = await rootBundle.load(imgdbAssetPath);

    try {
      tempFile.writeAsBytesSync(loadedImgdbByteData.buffer.asUint8List(
          loadedImgdbByteData.offsetInBytes,
          loadedImgdbByteData.lengthInBytes));
    } on Exception catch (e) {
      throw ArCorePluginAssetLoadingException(e.toString());
    }
    return;
  }
}

class ArCorePluginAssetLoadingException implements Exception {
  ArCorePluginAssetLoadingException(this.message);
  final String message;

  @override
  String toString() {
    return 'ArCorePluginAssetLoadingException {message: $message}';
  }
}
