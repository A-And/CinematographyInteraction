package com.ol.andon.reflex;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
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
import com.ol.andon.reflex.cv.PointCloudBlobDetector;
import com.ol.andon.reflex.services.MicroBitPairingService;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class PhotoActivity extends AppCompatActivity  implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener{
    private static final String TAG = PhotoActivity.class.getSimpleName();
    private static final String READINGS_READY = TAG + ".READINGS_READY";
    private static final String POSE_READY = TAG + ".POSE_READY";
    private static final String PCL_READY = TAG + ".PCL_READY";
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private Mat                  mSpectrum;
    private Size SPECTRUM_SIZE;
    private Scalar CONTOUR_COLOR;

    private PointCloudBlobDetector mDetector;
    private Camera mCamera;
    private TangoCameraPreview mTangoCameraView;
    private CameraBridgeViewBase mOpenCVCameraView;
    private MicroBitPairingService mMicroBitPairingService;

    private Tango mTango;
    private TangoConfig mTangoConfig;

    private LocalBroadcastManager broadcastManager;
    private BroadcastReceiver receiver;

    private boolean mIsColorSelected = false;
    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
//        mCamera = getCamera();

        // Example of a call to a native method
        System.loadLibrary("native-lib");
        Log.i(TAG, "Trying to load OpenCV library");
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mOpenCVCallBack))
        {
            Log.e(TAG, "Cannot connect to OpenCV Manager");
        }
        String cpp = stringFromJNI();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(cpp);
        AlertDialog dialog = builder.create();
        //dialog.show();

        mTangoCameraView = new TangoCameraPreview(this);
        mTango = new Tango(PhotoActivity.this, new Runnable() {
            @Override
            public void run() {
                synchronized (PhotoActivity.this){
                    mTangoCameraView.connectToTangoCamera(mTango, TangoCameraIntrinsics.TANGO_CAMERA_DEPTH);
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

        mOpenCVCameraView = (JavaCameraView) findViewById(R.id.opencv_camera_view);
        mOpenCVCameraView.setCameraIndex(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
        mOpenCVCameraView.enableView();
        mOpenCVCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCVCameraView.setCvCameraViewListener(this);
        mOpenCVCameraView.setOnTouchListener(this);

        //mMicroBitPairingService = new MicroBitPairingService(this);
        //mMicroBitPairingService.connect();

    }

    @Override
    protected void onResume(){
        super.onResume();
        mOpenCVCameraView.enableView();
        mTango = new Tango(PhotoActivity.this, new Runnable() {
            @Override
            public void run() {
                synchronized (PhotoActivity.this){
                    mTangoCameraView.connectToTangoCamera(mTango, TangoCameraIntrinsics.TANGO_CAMERA_DEPTH);
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
                    mTangoCameraView.onFrameAvailable();

                }
                else if(i == TangoCameraIntrinsics.TANGO_CAMERA_DEPTH){
                    mTangoCameraView.onFrameAvailable();
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
        mDetector = new PointCloudBlobDetector();
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);

    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.e(TAG, "Contours count: " + contours.size());
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }

        return mRgba;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (this.mOpenCVCameraView.getWidth() - cols) / 2;
        int yOffset = (this.mOpenCVCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = convertScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }
    private Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

}
