package com.ol.andon.reflex;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.SeekBar;
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
import com.ol.andon.reflex.cv.BlobDetector;
import com.ol.andon.reflex.cv.Camshift;
import com.ol.andon.reflex.cv.FigureDetector;
import com.ol.andon.reflex.cv.ITracker;
import com.ol.andon.reflex.cv.LKOpticalFlow;
import com.ol.andon.reflex.services.CaptureButtonService;
import com.ol.andon.reflex.services.MicroBitCommsService;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class PhotoActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {


    private static final String TAG = PhotoActivity.class.getSimpleName();


    // Blob color tracking Parameters
    private Mat mRgba;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    private Mat mSpectrum;
    private Size SPECTRUM_SIZE;


    // Object detection outline colors
    private static Scalar CONTOUR_COLOR;
    private static Scalar FIGURE_COLOR;
    private static Scalar FACE_COLOR;
    private static Scalar CENTER_COLOR;
    private static Scalar ROI_COLOR;

    private Point currentMainObjCenter;
    private Point mainBoundLockPoint;

    // CameraView Settings
    private static int CAM_HEIGHT = 960;
    private static int CAM_WIDTH = 1280;


    private BlobDetector mDetector;
    private FigureDetector mFigureDetector;
    private TangoCameraPreview mTangoCameraView;
    private CameraBridgeViewBase mOpenCVCameraView;

    // Communication services for the microbit and the bluetooth button
    private MicroBitCommsService mMicroBitPairingService;
    private CaptureButtonService mCaptureButtonService;
    private boolean mColorSelected = false;

    // Tango Setup
    private Tango mTango;
    private TangoConfig mTangoConfig;

    // Current values
    private int mX;
    private int mY;
    private int mZ;
    private int frameCounter;
    private boolean mTrackingObjectSelected = false;

    // Figure tracking parameters
    private MatOfRect mFigureRois;
    private Rect trackedFigureRoi;
    private ITracker mCamshift;

    private int mSessionPictureNum;

    // User settings to help evaluation
    private String mSubjectName;
    private SimpleDateFormat mPictureDateFormat = new SimpleDateFormat("yy_MM_d");

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCVCameraView = (CameraView) findViewById(R.id.opencv_camera_view);
                    mOpenCVCameraView.setCameraIndex(TangoCameraIntrinsics.TANGO_CAMERA_FISHEYE);

                    mOpenCVCameraView.enableView();

                    mOpenCVCameraView.setVisibility(SurfaceView.VISIBLE);


                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private Runnable updateServoValues = new Runnable() {
        @Override
        public void run() {
            if (mMicroBitPairingService != null && mMicroBitPairingService.isConnected())
                mMicroBitPairingService.writeXYZ(mX, mY, 0);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // System.loadLibrary("native-lib");
        Log.i(TAG, "Trying to load OpenCV library");
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mOpenCVCallBack)) {
            Log.e(TAG, "Cannot connect to OpenCV Manager");
        }

        // Set up UI seekbars
        SeekBar xSb = (SeekBar) findViewById(R.id.xServoAngle);
        xSb.setMax(180);

        SeekBar ySb = (SeekBar) findViewById(R.id.xServoAngle);
        ySb.setMax(180);

        SeekBar zSb = (SeekBar) findViewById(R.id.xServoAngle);
        zSb.setMax(180);


        mTangoCameraView = new TangoCameraPreview(this);
        mTango = new Tango(PhotoActivity.this, new Runnable() {
            @Override
            public void run() {
                synchronized (PhotoActivity.this) {
                    mTangoCameraView.connectToTangoCamera(mTango, TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                    mTangoConfig = setupTango(mTango);
                    try {
                        setTangoListeners();
                    } catch (TangoErrorException e) {
                        Log.e(TAG, "Tango exception", e);
                    } catch (SecurityException e) {
                        Log.e(TAG, "Security exception", e);
                    }

                    try {
                        mTango.connect(mTangoConfig);
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, "Service out of date exception", e);
                    } catch (TangoErrorException e) {
                        Log.e(TAG, "Error exception", e);
                    }
                }
            }
        });


        mOpenCVCameraView = (JavaCameraView) findViewById(R.id.opencv_camera_view);
        mOpenCVCameraView.setCvCameraViewListener(this);
        mOpenCVCameraView.setOnTouchListener(this);
        mMicroBitPairingService = new MicroBitCommsService(this);
        mMicroBitPairingService.connect();

        mCaptureButtonService = new CaptureButtonService();
        mCaptureButtonService.connect();

        // Set up number of pictures taken this session
        mSessionPictureNum = 0;

        // Set up subject name dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Subject Name");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSubjectName = input.getText().toString();
            }
        });

        builder.show();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mOpenCVCameraView.enableView();
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mOpenCVCallBack);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mOpenCVCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        mTango = new Tango(PhotoActivity.this, new Runnable() {
            @Override
            public void run() {
                synchronized (PhotoActivity.this) {
                    mTangoCameraView.connectToTangoCamera(mTango, TangoCameraIntrinsics.TANGO_CAMERA_DEPTH);
                    mTangoConfig = setupTango(mTango);
                    try {
                        setTangoListeners();
                    } catch (TangoErrorException e) {
                        Log.e(TAG, "Tango exception", e);
                    } catch (SecurityException e) {
                        Log.e(TAG, "Security exception", e);
                    }

                    try {
                        mTango.connect(mTangoConfig);
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, "Service out of date exception", e);
                    } catch (TangoErrorException e) {
                        Log.e(TAG, "Error exception", e);
                    }
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mMicroBitPairingService != null)
            mMicroBitPairingService.disconnect();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //public native String stringFromJNI(int arg);
    public static Camera getCamera() {
        Camera c = null;
        try {
            c = Camera.open(0);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
        }
        return c;

    }

    private TangoConfig setupTango(Tango tangoInstance) {

        TangoConfig config = tangoInstance.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);
        //config.putBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY, true);
        return config;
    }

    private void setTangoListeners() {
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
                if (i == TangoCameraIntrinsics.TANGO_CAMERA_COLOR || i == TangoCameraIntrinsics.TANGO_CAMERA_DEPTH || i == TangoCameraIntrinsics.TANGO_CAMERA_FISHEYE) {
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

    private void displayTangoData(TangoPoseData poseData) {
        float[] orientation = poseData.getRotationAsFloats();
        float[] translation = poseData.getTranslationAsFloats();
        StringBuilder newReadings = new StringBuilder();
        newReadings.append(String.format("Orientation X: %.5f, Y: %.5f, Z:%.5f\n", orientation[0], orientation[1], orientation[2]));
        newReadings.append(String.format("Translation X: %.5f, Y: %.5f, Z:%.5f", translation[0], translation[1], translation[2]));
        final String readingsString = newReadings.toString();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView readings = (TextView) findViewById(R.id.sensor_readings);
                readings.setText(readingsString);
            }
        });

    }

    private void displayPointCloudData(TangoPointCloudData pointCloudData) {
        FloatBuffer points = pointCloudData.points;
        final String pointString = Integer.toString(pointCloudData.numPoints);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView readings = (TextView) findViewById(R.id.pcl_readings);
                readings.setText(pointString);
            }
        });

    }

    private void displayXyzIj(TangoXyzIjData xyzIj) {
        final String pointString = xyzIj.toString();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView readings = (TextView) findViewById(R.id.pcl_readings);
                readings.setText(pointString);
            }
        });

    }

    private File copyResourceFileToDisk(int rawId, String fileName) {
        File mResFile = null;
        try {
            InputStream is = getResources().openRawResource(rawId);
            File cascadeDir = getDir("cascade", Context.MODE_APPEND);
            mResFile = new File(cascadeDir, fileName);
            FileOutputStream os = new FileOutputStream(mResFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
        } catch (Exception e) {
            Log.e(TAG, "Couldn't load classifier file " + e.getMessage());
        }

        return mResFile;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mDetector = new BlobDetector();

        File mFaceCascadeFile = copyResourceFileToDisk(R.raw.haarcascade_frontalface_default, "frontalFace.xml");
        File mBodyCascadeFile = copyResourceFileToDisk(R.raw.haarcascade_upperbody, "fullBody.xml");
        mFigureDetector = new FigureDetector(mFaceCascadeFile.getAbsolutePath(), mBodyCascadeFile.getAbsolutePath());

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);

        CENTER_COLOR = new Scalar(255, 0, 0, 255);
        CONTOUR_COLOR = new Scalar(0, 0, 255, 255);
        FIGURE_COLOR = new Scalar(255, 0, 176, 12);
        FACE_COLOR = new Scalar(100, 250, 0, 12);
        ROI_COLOR = new Scalar(0, 255, 255, 0);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mColorSelected ) {
            mDetector.detect(mRgba);
            MatOfPoint blob = (MatOfPoint) mDetector.getMainDetected();

            MatOfPoint blurredBounds = new MatOfPoint();
            Imgproc.blur(blob, blurredBounds, new Size(3, 3));
            Rect bound = Imgproc.boundingRect(blurredBounds);

            Imgproc.rectangle(mRgba, new Point(bound.x, bound.y), new Point(bound.x + bound.width, bound.y + bound.height), CONTOUR_COLOR, 10);
            final Point currCenter = new Point(bound.x + bound.width / 2, bound.y + bound.height / 2);
            //mMicroBitPairingService.writeXYZ((byte)angle, (byte)angle, (byte) 0);

            if (mainBoundLockPoint == null) {
                mainBoundLockPoint = currCenter;
            }

            if (currentMainObjCenter == null) {
                currentMainObjCenter = currCenter;
            }

            final double referenceX = mainBoundLockPoint.x;
            final double referenceY = mainBoundLockPoint.y;


            int xOffset = (this.mOpenCVCameraView.getWidth() - mRgba.cols()) / 2;
            int yOffset = (this.mOpenCVCameraView.getHeight() - mRgba.rows()) / 2;

            Log.d(TAG, "Width " + CAM_WIDTH + " X Height: " + CAM_HEIGHT);
            if (currCenter.x != 0)
                mX = (int) (90 + 90 * (referenceX - currCenter.x) / referenceX) ;//- xOffset;
            if (currCenter.y != 0)
                mY = (int) (90 + 90 * (referenceY - currCenter.y) / referenceY) ;//- yOffset;

            final int dispX = mX;
            final int dispY = mY;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView readings = (TextView) findViewById(R.id.tracking_readings);
                    //readings.setText("Center X: " + currCenter.x + " Y: " + currCenter.y + "| Dif X: " + (referenceX - currCenter.x ) + " Y:" + (referenceY- currCenter.y ));
                    readings.setText("Center X: " + currCenter.x + " Y: " + currCenter.y + "| X Angle: " + dispX + " Y Angle: " + dispY);
                }
            });

            currentMainObjCenter = currCenter;
            frameCounter = (frameCounter + 1) % 2;

            if (frameCounter == 0)
                mMicroBitPairingService.writeXYZ(mX, mY, 0);

            Imgproc.drawContours(mRgba, mDetector.getAllDetected(), -1, CONTOUR_COLOR);
            Imgproc.circle(mRgba, currCenter, 10, CENTER_COLOR);
            Imgproc.circle(mRgba, mainBoundLockPoint, 10, CENTER_COLOR);


            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);
            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }

        mFigureDetector.detect(mRgba);

        MatOfRect faces = mFigureDetector.getmFacesDetected();

        drawMatOfRect(mRgba, faces, FACE_COLOR);


        mFigureRois = mFigureDetector.getBodyROIs();
        drawMatOfRect(mRgba, mFigureRois, ROI_COLOR);

        // Tracking figure has been already selected, apply camshift and draw
        if(mTrackingObjectSelected){
            RotatedRect shiftWindow = mCamshift.getROI(mRgba);
            drawRotatedRect(mRgba, shiftWindow, FIGURE_COLOR);
        }

        return mRgba;
    }



    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();
        CAM_HEIGHT = mOpenCVCameraView.getHeight();
        CAM_WIDTH = mOpenCVCameraView.getWidth();

        int xOffset = (this.mOpenCVCameraView.getWidth() - cols) / 2;
        int yOffset = (this.mOpenCVCameraView.getHeight() - rows) / 2;

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;
        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Point touchPoint = new Point(x,y);
        for(Rect figure: mFigureRois.toList()){
            if(figure.contains(touchPoint)){
                Log.i(TAG, "Touched frame: " + figure.toString());
                trackedFigureRoi = figure;
                //mCamshift = new Camshift(mRgba, trackedFigureRoi);
                //mCamshift = new LKOpticalFlow(mRgba, trackedFigureRoi);
                mTrackingObjectSelected = true;
                break;
            }
        }

        return false; // don't need subsequent touch events
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        String key = KeyEvent.keyCodeToString(keyCode);
        Log.i(TAG, "Key pressed: " + key);
        if(key == "KEYCODE_VOLUME_UP"){
            // Lock

            return true;
        }
        else if(key == "KEYCODE_ENTER"){
            if(mRgba != null){
                takePicture();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void mbSendXY(View v) {
        if (!mMicroBitPairingService.isConnected()) mMicroBitPairingService.connect();
        SeekBar xAngle = (SeekBar) findViewById(R.id.xServoAngle);
        SeekBar yAngle = (SeekBar) findViewById(R.id.yServoAngle);

        int x = xAngle.getProgress();

        int y = yAngle.getProgress();


        mMicroBitPairingService.writeXY((x), (y));
    }

    public void mbConnect(View v) {
        mMicroBitPairingService.connect();
    }

    public void mbSendXYZ(View v) {
//        this.mbSendXY(v);

        if (!mMicroBitPairingService.isConnected()) mMicroBitPairingService.connect();
        SeekBar xAngle = (SeekBar) findViewById(R.id.xServoAngle);
        SeekBar yAngle = (SeekBar) findViewById(R.id.yServoAngle);
        SeekBar zForce = (SeekBar) findViewById(R.id.zForce);

        int x = xAngle.getProgress();

        int y = yAngle.getProgress();

        int z = zForce.getProgress();

        mMicroBitPairingService.writeXYZ((x), (y), (z));

    }
    public void takePicture(View v){
        takePicture();
    }


    private Scalar convertScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    private void drawMatOfRect(Mat frame, MatOfRect mat, Scalar color){
        for (Rect detected : mat.toList()) {
            Point uLeft = new Point(detected.x, detected.y);
            Point bRight = new Point(detected.x + detected.width, detected.y + detected.height);
            Imgproc.rectangle(frame, uLeft, bRight, color, 10);
        }
    }

    private void drawRotatedRect(Mat frame, RotatedRect rect, Scalar color){
        Point[] points = new Point[4];
        rect.points(points);
        for(int i = 0; i < 4; i++){
            Imgproc.line(frame, points[i], points[(i+1)%4], color, 3);
        }
    }

    // Pick a color for color blob tracking
    private void pickColor(View v, MotionEvent event){

        int cols = mRgba.cols();
        int rows = mRgba.rows();
        CAM_HEIGHT = mOpenCVCameraView.getHeight();
        CAM_WIDTH = mOpenCVCameraView.getWidth();

        int xOffset = (this.mOpenCVCameraView.getWidth() - cols) / 2;
        int yOffset = (this.mOpenCVCameraView.getHeight() - rows) / 2;

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");


        Rect touchedRect = new Rect();

        touchedRect.x = (x > 4) ? x - 4 : 0;
        touchedRect.y = (y > 4) ? y - 4 : 0;

        touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = convertScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mColorSelected = true;
        mainBoundLockPoint = null;

        //ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        //scheduler.scheduleAtFixedRate(updateServoValues, 0, 5, TimeUnit.SECONDS);

        touchedRegionRgba.release();
        touchedRegionHsv.release();

    }

    private void takePicture(){

        // take picture
        Calendar c = Calendar.getInstance();
        ((CameraView) mOpenCVCameraView).takePicture(mSubjectName + mSessionPictureNum + mPictureDateFormat.format(c.getTime()));

    }

}
