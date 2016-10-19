package com.ol.andon.tangophotography;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import java.io.IOException;

import static android.content.ContentValues.TAG;

/**
 * Created by Andon on 12/10/2016.
 */

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;

    public CameraView (Context context, Camera camera){
        super(context);
        mCamera = camera;

        // Get notified when the underlying surface is created/destroyed
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try{
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();

        }
        catch(IOException e){
            Log.e(TAG, "Error setting cam view: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera.stopPreview();
        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.release();
    }
}