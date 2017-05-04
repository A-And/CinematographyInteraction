package com.ol.andon.reflex.services;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.ol.andon.reflex.EvalLogger;
import com.ol.andon.reflex.Logger;

/**
 * Created by andon on 27/04/17.
 */

public class RotationalPositionService implements SensorEventListener{


    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Activity parentActivity;
    private static final int SENSOR_DELAY_MICROS = 5 * 1000; // 500ms
    private float[] absolutePositionVector = new float[3];
    private final String TAG = "RotationSensor";
    private Logger logger;

    public RotationalPositionService(Activity activity, Logger argLogger){
        parentActivity = activity;
        mSensorManager = (SensorManager) parentActivity.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        logger = argLogger;

    }
    public void startRotationLogging(){
        mSensorManager.registerListener(this, mSensor, SENSOR_DELAY_MICROS);

    }
    public void stopRotationLogging(){
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] mRotationMatrix = new float[16];
        float[] orientationVals = new float[3];
        // Convert the rotation-vector to a 4x4 matrix.
        SensorManager.getRotationMatrixFromVector(mRotationMatrix,
                event.values);
        SensorManager
                .remapCoordinateSystem(mRotationMatrix,
                        SensorManager.AXIS_X, SensorManager.AXIS_Z,
                        mRotationMatrix);
        SensorManager.getOrientation(mRotationMatrix, orientationVals);

        // Optionally convert the result from radians to degrees
        orientationVals[0] = (float) Math.toDegrees(orientationVals[0]);
        orientationVals[1] = (float) Math.toDegrees(orientationVals[1]);
        orientationVals[2] = (float) Math.toDegrees(orientationVals[2]);

        for(int i = 0; i < orientationVals.length; i++){
            absolutePositionVector[i] += orientationVals[i];
        }
        logger.writeAsync(1 + "," + System.currentTimeMillis() + "," + orientationVals[0] + "," + orientationVals[1] + ","+ orientationVals[2]
        + "," + 0) ;
//        logger.writeAsync("Total| Yaw: " + absolutePositionVector[0] + "\n Pitch: "
//                + absolutePositionVector[1] + "\n Roll (not used): "
//                + absolutePositionVector[2]);
        //EvalLogger.v(parentActivity, TAG, " Yaw: " + orientationVals[0] + "\n Pitch: "
//                + orientationVals[1] + "\n Roll (not used): "
//                + orientationVals[2]);
//        EvalLogger.v(parentActivity, TAG, "Total| Yaw: " + absolutePositionVector[0] + "\n Pitch: "
//                + absolutePositionVector[1] + "\n Roll (not used): "
//                + absolutePositionVector[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
