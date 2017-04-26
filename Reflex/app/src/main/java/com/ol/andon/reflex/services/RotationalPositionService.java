package com.ol.andon.reflex.services;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

/**
 * Created by andon on 27/04/17.
 */

public class RotationalPositionService {


    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Activity parentActivity;

    public RotationalPositionService(Activity activity){
        parentActivity = activity;
        mSensorManager = (SensorManager) parentActivity.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

}
