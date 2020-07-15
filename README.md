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


- In the main function you have to you need to ensure that the binding is initialized before calling 

```
  /// ensure that the binding is initialized 
  WidgetsFlutterBinding.ensureInitialized();
  ...
```
- you also have to load file from asset folder and save it into cache 
```
Future<void> _onArCoreViewCreated(ArCoreViewController controller) async {
    arCoreViewController = controller;
    await arCoreViewController.loadImgdbFromAssets(
        tempFilePath:
            '/data/user/0/com.peqas.arcorepluginexample/cache/image_database.imgdb');
    await controller.getArCoreView();
  }
```

For more information checkout the [Example](https://github.com/khalithartmann/flutter_arcore_plugin/tree/master/example) project

``` 
# Very important! 
    * AS FOR NOW THE PLUGIN DOES NOT SUPPORT IN APP TRAINING OF THE IMGDB FILE
    * The IMGDB File has to be PRETRAINED and saved in a Temp Directory before the ArCoreView is created 
    * The IMGDB File name hast to be image_database.imgdb
     * you can find a pretained imgdb file under example/assets. The images that the imgdb file is trained with are found under example/assets/tester_images
    * for more info about ARCore imgdb files see : https://developers.google.com/ar/develop/c/augmented-images/arcoreimg
   



Note: This plugin is still under development, and some APIs might not be available yet. Feedback welcome and Pull Requests are most welcome! 



