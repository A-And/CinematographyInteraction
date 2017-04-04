package com.ol.andon.reflex.services;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.Set;

/**
 * Created by andon on 23/03/17.
 */

public class CaptureButtonService {

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    String TAG = "CaptureButtonService";
    String buttonAddress = "00:1D:AE:51:A3:FB";
    BluetoothDevice button;
    public CaptureButtonService(){
        if(mBluetoothAdapter == null){
            Log.e(TAG, "Device doesn't support bluetooth");
        }

    }

    public void connect(){
        pairButton();
    }

    private void pairButton(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            for(BluetoothDevice device: pairedDevices){
                Log.i(TAG, "Device :" + device.getName() + " " + device.getAddress());
                if(device.getAddress().equals(buttonAddress)){
                    button = device;
                    Log.i(TAG, "Button found");
                }
            }

        }
    }
}
