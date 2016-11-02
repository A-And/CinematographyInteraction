package com.ol.andon.reflex;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoCameraPreview;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import java.nio.FloatBuffer;
import java.util.ArrayList;

public class PhotoActivity extends AppCompatActivity  implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG = PhotoActivity.class.getSimpleName();
    private static final String READINGS_READY = TAG + ".READINGS_READY";
    private static final String POSE_READY = TAG + ".POSE_READY";
    private static final String PCL_READY = TAG + ".PCL_READY";
    private Camera mCamera;
    private TangoCameraPreview mCameraView;

    private Tango mTango;
    private TangoConfig mTangoConfig;

    private LocalBroadcastManager broadcastManager;
    private BroadcastReceiver receiver;

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
//        mCamera = getCamera();
        // Example of a call to a native method
        mCameraView = new TangoCameraPreview(this);
        FrameLayout cv = (FrameLayout) findViewById(R.id.camera_view);
        cv.addView(mCameraView);
    }

    @Override
    protected void onResume(){
        super.onResume();

        mTango = new Tango(PhotoActivity.this, new Runnable() {
            @Override
            public void run() {
                synchronized (PhotoActivity.this){
                    mCameraView.connectToTangoCamera(mTango, TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                    mTangoConfig = setupTango(mTango);
                    try{
                        setTangoListeners();
                    }
                    catch(TangoErrorException e){
                        Log.e(TAG, "Tango exception", e);
                    }
                    catch(SecurityException e){
                        Log.e(TAG, "Security exception", e);
                    }

                    try{
                        mTango.connect(mTangoConfig);
                    }
                    catch(TangoOutOfDateException e){
                        Log.e(TAG, "Service out of date exception", e);
                    }
                    catch (TangoErrorException e){
                        Log.e(TAG, "Error exception", e);
                    }
                }
            }
        });
    }
    @Override
    protected void onStart(){
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,new IntentFilter(this.READINGS_READY));
    }
    @Override
    protected void onStop(){
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
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
    private TangoConfig setupTango(Tango tangoInstance){

        TangoConfig config = tangoInstance.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        //config.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);
        //config.putBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY, true);
        return config;
    }
    private void setTangoListeners(){
        final ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE, TangoPoseData.COORDINATE_FRAME_DEVICE));
        mTango.connectListener(framePairs, new Tango.OnTangoUpdateListener() {
            @Override
            public void onPoseAvailable(TangoPoseData tangoPoseData) {
                displayTangoData(tangoPoseData);
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData tangoXyzIjData) {

            }

            @Override
            public void onFrameAvailable(int i) {
                // Check if the frame available is for the camera we want and
                // update its frame on the camera preview.
                if (i == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                    mCameraView.onFrameAvailable();
                }
                else if(i == TangoCameraIntrinsics.TANGO_CAMERA_DEPTH){
                    mCameraView.onFrameAvailable();
                }
            }

            @Override
            public void onTangoEvent(TangoEvent tangoEvent) {

            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData tangoPointCloudData) {
                displayPointCloudData(tangoPointCloudData);
            }
        });
    }

    private void displayTangoData(TangoPoseData poseData){
        float[] orientation = poseData.getRotationAsFloats();
        float[] translation = poseData.getTranslationAsFloats();
        StringBuilder newReadings = new StringBuilder();
        newReadings.append(String.format("Orientation X: %.5f, Y: %.5f, Z:%.5f\n", orientation[0], orientation[1], orientation[2]));
        newReadings.append(String.format("Translation X: %.5f, Y: %.5f, Z:%.5f", translation[0], translation[1], translation[2]));
        final String readingsString = newReadings.toString();
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                TextView readings = (TextView) findViewById(R.id.sensor_readings);
                readings.setText(readingsString);
            }
        } );

    }

    private void displayPointCloudData(TangoPointCloudData pointCloudData){
        FloatBuffer points = pointCloudData.points;
        final String pointString = Integer.toString(pointCloudData.numPoints);
        runOnUiThread(new Runnable (){
            @Override
            public void run(){
                TextView readings = (TextView) findViewById(R.id.pcl_readings);
                readings.setText(pointString);
            }
        });

    }

    private void displayXyzIj(TangoXyzIjData xyzIj){
        final String pointString = xyzIj.toString();
        runOnUiThread(new Runnable (){
            @Override
            public void run(){
                TextView readings = (TextView) findViewById(R.id.pcl_readings);
                readings.setText(pointString);
            }
        });

    }


    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }
}
