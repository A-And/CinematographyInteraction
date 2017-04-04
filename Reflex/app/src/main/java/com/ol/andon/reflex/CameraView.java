package com.ol.andon.reflex;

/**
 * Created by Andon on 26/10/2016.
 */

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.android.JavaCameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/** A basic Camera preview class */
public class CameraView extends JavaCameraView implements Camera.PictureCallback{

    private Context mContext;

    private File mExternalStorageDir = Environment.getExternalStorageDirectory()    ;

    private final String TAG = "CameraView";

    private String mPictureFileName;

    public CameraView(Context context, AttributeSet attrs){
        super(context,attrs);
        mContext = context;
    }

    public void takePicture(final String fileName){
        Log.i(TAG, "Taking picture: " + fileName);
        this.mPictureFileName = fileName;

        mCamera.setPreviewCallback(null);
        mCamera.takePicture(null, null, this);
    }
    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving picture to file");
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);
        try{

            File img = new File(mExternalStorageDir, mPictureFileName + ".png");

            FileOutputStream os = new FileOutputStream(img);

            os.write(data);
            os.flush();
            os.close();
            forceScan(img);
//            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//            intent.setData(Uri.fromFile(img));
//            mContext.sendBroadcast(intent);
            Log.i(TAG, "Saved picture to " + img.getAbsolutePath());
        }
        catch(IOException e){
            Log.e(TAG, "Couldn't write picture to file ", e);
        }
    }

    public List<Camera.Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Camera.Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    private void forceScan(File img){
        MediaScannerConnection.scanFile(mContext,new String[] { img.getAbsolutePath() }, null,new MediaScannerConnection.OnScanCompletedListener() {
            public void onScanCompleted(String path, Uri uri) {
                Log.i("ExternalStorage", "Scanned " + path + ":");
                Log.i("ExternalStorage", "-> uri=" + uri);
            }
        });
    }

}
