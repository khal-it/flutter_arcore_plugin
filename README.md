# arcore_plugin

A Flutter plugin for Android allowing to recongize images via ARCore 
* USES ARCore version 1.8.0

## Features 
 ### [x] Displaying live ARCamera Feed 
 ### [x] Recognizing images via ARCore (for more information see: https://developers.google.com/ar/develop/c/augmented-images/)
 ### [ ] Placing Objects
 ### [ ] In-App training of the imgdb file 



## Installation

### Android 
* For Setup make sure you migrated your App to AndroidX 
* in app/build.gradle set minSdkVersion to 24 and compileSdkVersion 28 

```
 defaultConfig {
        ...     
        minSdkVersion 24
        ...
    }
```
    and
```
android {
    compileSdkVersion 28
    ...
}

``` 
# Very important! 
    * AS FOR NOW THE PLUGIN DOES NOT SUPPORT IN APP TRAINING OF THE IMGDB FILE
    * The IMGDB File has to be PRETRAINED and saved in a Temp Directory before the ArCoreView is created 
    * The IMGDB File name hast to be image_database.imgdb
     * you can find a pretained imgdb file under example/assets. The images that the imgdb file is trained with are found under example/assets/tester_images
    * for more info about ARCore imgdb files see : https://developers.google.com/ar/develop/c/augmented-images/arcoreimg
   


## Getting Started 
-  For the given Example you have to have a pretrained imgdb saved in your assets folder and added as an asset in your pubspec.yaml file !

```
import 'dart:io';
import 'package:flutter/services.dart' show rootBundle;
import 'package:arcore_plugin/arcore_plugin.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';


void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitDown,
    DeviceOrientation.portraitUp,
  ]);
  final Directory systemTempDir = Directory.systemTemp;
  final File tempFile = File('${systemTempDir.path}/image_database.imgdb');

  // create tempfile
  await tempFile.create();

  rootBundle.load("assets/image_database.imgdb").then((data) {
    tempFile.writeAsBytesSync(
        data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes));

    runApp(MaterialApp(home: TextViewExample()));
  }).catchError((error) {
    throw Exception(error);
  });
}

class TextViewExample extends StatefulWidget {
  @override
  _TextViewExampleState createState() => _TextViewExampleState();
}

class _TextViewExampleState extends State<TextViewExample> {
  String recongizedImage;
  ArCoreViewController arCoreViewController;

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    Size screenSize = MediaQuery.of(context).size;
    return Scaffold(
        backgroundColor: Colors.blue,
        appBar: AppBar(
          title: const Text('ArCoreViewExample'),
          backgroundColor: Colors.black,
          centerTitle: true,
        ),
        body: Center(
            child: ArCoreView(
          focusBox: Container(
            width: screenSize.width * 0.5,
            height: screenSize.width * 0.5,
            decoration: BoxDecoration(
                border: Border.all(width: 1, style: BorderStyle.solid)),
          ),
          width: screenSize.width,
          height: screenSize.height,
          onImageRecognized: _onImageRecognized,
          onArCoreViewCreated: _onTextViewCreated,
        )));
  }

  void _onTextViewCreated(ArCoreViewController controller) {
    arCoreViewController = controller;
    controller.getArCoreView();
  }

  void _onImageRecognized(String recImgName) {
    print("image recongized: $recImgName");

    // you can pause the image recognition via arCoreViewController.pauseImageRecognition();
    // resume it via arCoreViewController.resumeImageRecognition();
  }
}


```



Note: This plugin is still under development, and some APIs might not be available yet. Feedback welcome and Pull Requests are most welcome! 



