package com.peqas.arcoreplugin;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.RequestManager;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import common.helpers.DisplayRotationHelper;
import common.rendering.BackgroundRenderer;
import io.flutter.app.FlutterApplication;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;


public class FlutterArcoreView implements PlatformView, MethodChannel.MethodCallHandler, GLSurfaceView.Renderer {
    private static final int CAMERA_PERMISSION_CODE = 0;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final String TAG = FlutterArcoreView.class.getSimpleName();
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    // Augmented image configuration and rendering.
    // Load a single image (true) or a pre-generated image database (false).
    private final boolean useSingleImage = false;
    // Augmented image and its associated center pose anchor, keyed by index of the augmented image in
    // the
    // database.
    private final Map<Integer, Pair<AugmentedImage, Anchor>> augmentedImageMap = new HashMap<>();
    private final Activity activity;
    private final MethodChannel methodChannel;

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;
    private ImageView fitToScanView;
    private RequestManager glideRequestManager;
    private boolean installRequested;
    private boolean requestingPermission;
    private Session session;
    private DisplayRotationHelper displayRotationHelper;
    private boolean shouldConfigureSession = false;
    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks;
    private double _tick = 0;
    private int recognized_image_index = -1;
    private boolean activity_paused = false;
    private BinaryMessenger image_recognition_messenger;
    private boolean recognize_images = true;


    public FlutterArcoreView(Context context, BinaryMessenger messenger, int id) {
        surfaceView = new GLSurfaceView(context);
        this.activity = ((FlutterApplication) context.getApplicationContext()).getCurrentActivity();
        methodChannel = new MethodChannel(messenger, "plugins.peqas.com/arcore_plugin_" + id);
        methodChannel.setMethodCallHandler(this);
        displayRotationHelper = new DisplayRotationHelper(/*context=*/ context);
        image_recognition_messenger = messenger;


        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);


        this.activityLifecycleCallbacks =
                new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    }

                    @Override
                    public void onActivityStarted(Activity activity) {
                    }

                    @Override
                    public void onActivityResumed(Activity activity) {
                        activity_paused = false;
                        _onResume();
                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                        activity_paused = true;
                        _onPause();
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {
                        _onPause();
                    }

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                    }
                };

        ((FlutterApplication) context.getApplicationContext()).getCurrentActivity().getApplication()
                .registerActivityLifecycleCallbacks(this.activityLifecycleCallbacks);

        installRequested = false;

        requestingPermission = false;

        try {
            if (hasCameraPermission(activity)) {
                _onResume();
            } else {
                requestCameraPermission(activity);
            }
        } catch (Exception e) {
            return;
        }
    }

    public static boolean hasCameraPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity, new String[]{CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
    }

    private void _onPause() {
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            //surfaceView.onPause();
            session.pause();
        }
    }

    protected void _onResume() {

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {

                // request to install arcore if not already done
                switch (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;

                }

                // request camera permission if not already requested
                if (!hasCameraPermission(activity)) {
                    requestCameraPermission(activity);
                }

                // create new Session
                this.session = new Session(activity);
                Log.i(TAG, "Session created man");

            } catch (UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }
            if (message != null) {

                Log.e(TAG, "message is " + message);

                return;
            }
            shouldConfigureSession = true;


        }

        if (shouldConfigureSession) {
            configureSession();
            Log.i(TAG, "i got out of here! wow.");
            shouldConfigureSession = false;
        }
        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            session = null;
            return;
        }
        surfaceView.onResume();
        displayRotationHelper.onResume();

        //fitToScanView.setVisibility(View.VISIBLE); //TODO fix problem later

    }

    private void configureSession() {

        Config config = new Config(session);
        config.setFocusMode(Config.FocusMode.AUTO);

        if (!setupAugmentedImageDatabase(config)) {
            Log.e(TAG, "Could not setup augmented image database");

        }
        session.configure(config);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(/*context=*/ activity);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);
        if (!activity_paused) {
            try {
                session.setCameraTextureName(backgroundRenderer.getTextureId());


                // Obtain the current frame from ARSession. When the configuration is set to
                // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
                // camera framerate.

                Frame frame = session.update();
                Camera camera = frame.getCamera();



                // Draw background.
                backgroundRenderer.draw(frame);

                // If not tracking, don't draw 3d objects.
                //if (camera.getTrackingState() == TrackingState.PAUSED) {
                //    return;
                //}

                // Get projection matrix.
                float[] projmtx = new float[16];
                camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

                // Get camera matrix and draw.
                float[] viewmtx = new float[16];
                camera.getViewMatrix(viewmtx, 0);

                // Compute lighting from average intensity of the image.
                final float[] colorCorrectionRgba = new float[4];
                frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

                // recognize images.
                if (recognize_images) {
                    recognizeImages(frame, projmtx, viewmtx, colorCorrectionRgba);
                }
            } catch (Throwable t) {
                // Avoid crashing the application due to unhandled exceptions.
                Log.e(TAG, "Exception on the OpenGL thread", t);
            }

        }
    }

    private void recognizeImages(

            Frame frame, float[] projmtx, float[] viewmtx, float[] colorCorrectionRgba) {
        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);

        // Iterate to update augmentedImageMap, remove elements we cannot draw.
        for (AugmentedImage augmentedImage : updatedAugmentedImages) {
            switch (augmentedImage.getTrackingState()) {
                case PAUSED:

                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                    methodChannel.invokeMethod("image_recognized", augmentedImage.getName());
                    recognize_images = false;
                    augmentedImageMap.clear();


                    recognized_image_index = augmentedImage.getIndex();

                case TRACKING:
                    methodChannel.invokeMethod("image_recognized", augmentedImage.getName());
                    recognize_images = false;
                    augmentedImageMap.clear();


                    break;

                case STOPPED:
                    augmentedImageMap.clear();
                    recognize_images = false;
                    methodChannel.invokeMethod("image_recognized", augmentedImage.getName());
                    break;

                default:
                    break;
            }
        }

    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {

        if (methodCall.method.equals("recognize_images")) {
            result.success(null);
        }
        if (methodCall.method.equals("resume_image_recognition")) {
            System.out.println("android: image recognition resumed ");
            recognize_images = true;
        }
        if (methodCall.method.equals("pauseImageRecognition")) {
            System.out.println("android: need to stop image recognition!!!!!");
            recognize_images = false;
            System.out.println("recognize_images = ");
            System.out.println(recognize_images);
        }

    }

    @Override
    public View getView() {
        return surfaceView;
    }

    @Override
    public void dispose() {

    }

    private boolean setupAugmentedImageDatabase(Config config) {
        AugmentedImageDatabase augmentedImageDatabase;

        // There are two ways to configure an AugmentedImageDatabase:
        // 1. Add Bitmap to DB directly
        // 2. Load a pre-built AugmentedImageDatabase
        // Option 2) has
        // * shorter setup time
        // * doesn't require images to be packaged in apk.

        // This is an alternative way to initialize an AugmentedImageDatabase instance,
        // load a pre-existing augmented image database.
        File imgDBFile = new File(activity.getCacheDir(), "image_database.imgdb");

        try (InputStream is = new FileInputStream(imgDBFile)) {
            augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, is);
        } catch (Exception e) {
            System.out.println("ARCORE PLUGIN: Couldnt open IMGDB" + imgDBFile.getPath().toString());
            Log.e(TAG, "IO exception loading augmented image database.", e);
            return false;
        }


        config.setAugmentedImageDatabase(augmentedImageDatabase);
        return true;
    }
}