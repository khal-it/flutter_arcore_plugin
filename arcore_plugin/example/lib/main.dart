import 'package:arcore_plugin/arcore_plugin.dart';
import 'package:flutter/material.dart';

void main() => runApp(MaterialApp(home: TextViewExample()));

class TextViewExample extends StatefulWidget {
  @override
  _TextViewExampleState createState() => _TextViewExampleState();
}

class _TextViewExampleState extends State<TextViewExample> {

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
            child: Container(
                width: screenSize.width,
                height: screenSize.height,
                child: ArCoreView(
                  onArCoreViewCreated: _onTextViewCreated,
                ))));
  }

  void _onTextViewCreated(ArCoreViewController controller) {
    controller.getArCoreView();
  }
}
