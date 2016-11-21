package com.ol.andon.reflex.services;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

/**
 * Created by Andon on 21/11/2016.
 */

public class MicroBitPairingService {
    private static final long SCAN_PERIOD = 10000;
    final BluetoothManager bluetoothManager ;
    private String mMicrobitAddress = "CE:3A:39:FE:53:A6";
    private Activity activity;
    private boolean mScanning;
    private Handler mHandler;
    private final static int REQUEST_ENABLE_BT = 1;
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState!= 0){
                Log.e("Success","Connected");
            }
        }
    } ;

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                               device.connectGatt(activity, true, gattCallback);
                                                    }
                    });
                }
            };
    public MicroBitPairingService(Activity activity){
        this.activity = activity;
        this.mHandler = new Handler();
        bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
    }
    public boolean connect(){
        boolean res = false;
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
            Intent enableBltIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBltIntent, REQUEST_ENABLE_BT);
        }
        scanForMicroBit(mBluetoothAdapter);
        return res;
    }
    private void scanForMicroBit(final BluetoothAdapter mBluetoothAdapter){
        for(BluetoothDevice device: mBluetoothAdapter.getBondedDevices()){
            if(device.getName().contains("micro")){
                mMicrobitAddress = device.getAddress();
            }
        }

        
        mHandler.postDelayed(new Runnable(){
            @Override
            public void run(){
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }, SCAN_PERIOD);
        mScanning = true;
        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }


}
