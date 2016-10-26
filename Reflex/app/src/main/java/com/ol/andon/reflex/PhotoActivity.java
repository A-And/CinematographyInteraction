package com.ol.andon.reflex;

import android.Manifest;
import android.hardware.Camera;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;

public class PhotoActivity extends AppCompatActivity {

    private Camera mCamera;
    private CameraView mCameraView;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
        mCamera = getCamera();
        // Example of a call to a native method
        mCameraView = new CameraView(this, mCamera);
        FrameLayout cv = (FrameLayout) findViewById(R.id.camera_view);
        cv.addView(mCameraView);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public static Camera getCamera() {
        Camera c = null;
        try {
            c = Camera.open(0);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
        }
        return c;

    }
}
