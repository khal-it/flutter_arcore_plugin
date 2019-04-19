import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

typedef void ArCoreViewCreatedCallback(ArCoreViewController controller);

class ArCoreView extends StatefulWidget {
  final double width;
  final double height;
  final Function onImageRecognized;
  final ArCoreViewCreatedCallback onArCoreViewCreated;
  final Widget focusBox;

  const ArCoreView(
      {Key key,
      this.onArCoreViewCreated,
      this.onImageRecognized,
      this.width = 300,
      this.height = 300,
      this.focusBox})
      : super(key: key);

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
              widget.focusBox != null ? widget.focusBox : Container()
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
        new ArCoreViewController(id, widget.onImageRecognized));
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }
}

class ArCoreViewController {
  final Function onImageRecognized;
  ArCoreViewController(int id, this.onImageRecognized) {
    _channel = new MethodChannel('plugins.peqas.com/arcore_plugin_$id');
    _channel.setMethodCallHandler(_handleMethodCalls);
  }
  MethodChannel _channel;

  Future<dynamic> _handleMethodCalls(MethodCall methodCall) async {
    switch (methodCall.method) {
      case "image_recognized":
        print(methodCall.arguments);
        String recognizedImgName = methodCall.arguments.toString();
        this.onImageRecognized(recognizedImgName);
        return Future.value(recognizedImgName);
    }
  }

  Future<void> resumeImageRecognition() async {
    print("resuming image recognition");
    return _channel.invokeMethod('resume_image_recognition');
  }

  Future<void> pauseImageRecognition() async {
    print("pausing image recongition");
    return _channel.invokeMethod('pauseImageRecognition');
  }

  Future<void> getArCoreView() async {
    return _channel.invokeMethod('recognize_images');
  }
}
