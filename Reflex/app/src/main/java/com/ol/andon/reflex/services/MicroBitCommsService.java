package com.ol.andon.reflex.services;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.ol.andon.reflex.EvalLogger;
import com.ol.andon.reflex.Logger;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by Andon on 21/11/2016.
 */

public class MicroBitCommsService {
//    // MicroBit BLE Specs
//    public static String IOPINSERVICE_SERVICE_UUID = "e95d127b-251d-470a-a062-fa1922dfa9a8";
//    public final static String PINDATA_CHARACTERISTIC_UUID = "e95d8d00-251d-470a-a062-fa1922dfa9a8";
//    public final static  String PINADCONFIGURATION_CHARACTERISTIC_UUID = "e95d5899-251d-470a-a062-fa1922dfa9a8";
//    public final static String PINIOCONFIGURATION_CHARACTERISTIC_UUID = "e95db9fe-251d-470a-a062-fa1922dfa9a8";

    private int currentX = 135;
    private int currentY = 90;
    private int currentZ = 0;


    private int targetX = - 1;
    private int targetY = - 1;

    private int incrementVal = 1;

    private final int xServoIndex = 13;
    private final int yServoIndex = 12;
    private final int ySupportServoIndex = 14;

    private Logger mLogger;
    RotationalPositionService rotationalPositionService;

    public static String UARTSERVICE_SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static String UART_TX_CHARACTERISTIC_UUID ="6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static String UART_RX_CHARACTERISTIC_UUID ="6e400003-b5a3-f393-e0a9-e50e24dcca9e";

    final static String TAG = "MicroBitComms";
    final static long mUpdatePeriod = 20;
    private Timer mTimer;
    private String mMicrobitAddress;
    private Activity activity;
    private boolean mConnected;
    private Handler mHandler;
    private final static int REQUEST_ENABLE_BT = 1;

    private boolean mUpdating = false;


    final BluetoothManager bluetoothManager ;
    BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    BluetoothGattCharacteristic rxCharacteristic;
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState!= 0){
                Log.e("Bluetooth Success","Connected");
                mBluetoothGatt.discoverServices();

            }
            else mConnected = false;
        }
        @Override
        public void onServicesDiscovered( BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS){
                List<BluetoothGattService> serv = gatt.getServices();
                for(BluetoothGattService service : serv)
                    Log.d(TAG, service.getUuid().toString());
                setupRXService();

            }

            else
                Log.i(TAG, "onServicesDiscovered received: " + status);
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
            Log.i(TAG, "Characteristic changed : " + characteristic.getValue());
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            Log.i(TAG, "Characteristic changed; Status " + status);
        }
    } ;

    public MicroBitCommsService(Activity activity, Logger argLogger){
        this.activity = activity;
        this.mHandler = new Handler();
        bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        rotationalPositionService = new RotationalPositionService(activity, argLogger);
        mLogger = argLogger;
    }
    public boolean connect(){
        boolean res = false;
         mBluetoothAdapter = bluetoothManager.getAdapter();
        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
            Intent enableBltIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBltIntent, REQUEST_ENABLE_BT);
            res = true;
        }
        scanForDevice();
        return res;
    }

    private void setupRXService() {
        if (mBluetoothGatt == null){
            Log.e("ERROR", "Can't scan for device, BLE GATT not initialized");
            return;
        }
        List<BluetoothGattService> services = mBluetoothGatt.getServices();

        BluetoothGattService uartService = null;

        for(BluetoothGattService service : services){
            Log.d(TAG, service.getUuid().toString());
            if(service.getUuid().toString().equals(UARTSERVICE_SERVICE_UUID) )
                uartService = service;
        }
        if(uartService != null) {
            List<BluetoothGattCharacteristic> chars = uartService.getCharacteristics();

            // Find services
            for (BluetoothGattCharacteristic characteristic : chars) {
                if(characteristic.getUuid().toString().equals(UART_RX_CHARACTERISTIC_UUID))
                    this.rxCharacteristic = characteristic;
            }


        }
        mConnected = rxCharacteristic != null;
        if(!mConnected){
            Log.d(TAG, "RX characteristic not found");
        }
    }


    public void incrementToXYZ(int x, int y, int z){

        targetX = x;
        targetY = y;

        mLogger.writeAsync(1 +"," + System.currentTimeMillis() + "," + targetX + "," + targetY + "," + z
                + "," + 1) ;
    }

    public void incrementToXY(int x, int y){
        targetX = x;
        targetY = y;
    }

    public void writeXY(int x, int y){
        if(!mConnected) return;
        if(rxCharacteristic == null) return;

        byte[] moveData = {(byte) (x  ), ':', (byte) (y ), ':' };
        rxCharacteristic.setValue(moveData);
        mBluetoothGatt.writeCharacteristic(rxCharacteristic);
        Log.v(TAG, "BLE write: " + new String(moveData));
    }

    private void updateXYZ(){
        if( Math.abs(currentX - targetX) > incrementVal)
            currentX = currentX < targetX? currentX + incrementVal : currentX - incrementVal;

        if( Math.abs(currentY - targetY) > incrementVal)
            currentY = currentY < targetY ? currentY + incrementVal : currentY - incrementVal;

        writeXYZ(currentX, currentY, currentZ);
    }
    public void testMovement(){
        mLogger.writeAsync("Movement test");
        rotationalPositionService.startRotationLogging();
        mLogger.writeAsync("Horizontal");
        int x = 90;
        int y = 90;
        writeXYZ(90, 90, 0);

        writeXYZ(180, 90, 0);

        mLogger.writeAsync("Vertical");
        x = 75;
        y = 0;

        writeXYZ(75, 0, 0);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        writeXYZ(75, 180, 0);

        rotationalPositionService.stopRotationLogging();

    }
    public void writeXYZ(int x, int y, int z){
        //mLogger.writeAsync(TAG +  ": Gyro Vector X: " + x + " Y: " + y);
        Log.d(TAG, "BLE Write: X: " + x + " Y: " + y);
        if(!mConnected){
            Log.d(TAG, "BLE not Connected");
        }
        if(rxCharacteristic == null){
            Log.d(TAG, "Service not Connected");
            return;
        }

        byte[] moveData = {xServoIndex ,(byte) (x & 0xFF) , yServoIndex, (byte) (y  & 0xFF) , ySupportServoIndex, (byte) (z & 0xFF) };
        //byte[] moveData = {(byte) (x  ), ':', (byte) (y ), ':' };

        boolean res = rxCharacteristic.setValue(moveData);
        mBluetoothGatt.writeCharacteristic(rxCharacteristic);
        Log.d(TAG, "BLE write: " + new String(moveData));

    }

    public void write0(){
        if(!mConnected) return;
        if(rxCharacteristic == null) return;

        byte[] xData = {0x00, 0x00};
        rxCharacteristic.setValue(xData);
        Log.i(TAG, "BLE write: " + mBluetoothGatt.writeCharacteristic(rxCharacteristic));
        Log.i(TAG, "BLE write: " + mBluetoothGatt.readCharacteristic(rxCharacteristic) +  " " + rxCharacteristic.getValue().toString());
    }

    public void write1(){
        if(!mConnected) return;
        if(rxCharacteristic == null) return;

        byte[] xData = {0x00, 0x01};
        rxCharacteristic.setValue(xData);
        Log.i(TAG, "BLE write: " + mBluetoothGatt.writeCharacteristic(rxCharacteristic));
        Log.i(TAG, "BLE write: " + mBluetoothGatt.readCharacteristic(rxCharacteristic) +  " " + rxCharacteristic.getValue());
    }

    public void disconnect(){
        if(mConnected)
            mBluetoothGatt.disconnect();

    }

    private void scanForDevice(){
        BluetoothDevice microBit = null;
        if(mBluetoothAdapter == null){
            Log.e("ERROR", "Can't scan for device, BLE adapter not initialized");
            return;
        }
        for(BluetoothDevice device: mBluetoothAdapter.getBondedDevices()){
            // Hacky
            if(device.getName().contains("micro")){
                mMicrobitAddress = device.getAddress();
                microBit = device;
            }

        }

        if(microBit == null)
            Log.d(TAG, "No MicroBit found");

        if(microBit != null){
            BluetoothDevice actual = mBluetoothAdapter.getRemoteDevice(microBit.getAddress());
            mBluetoothGatt = actual.connectGatt(this.activity, false, gattCallback);

        }
    }
    public void startUpdate(){
        mUpdating = true;

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateXYZ();
            }
        }, 0, mUpdatePeriod);
        rotationalPositionService.startRotationLogging();

    }

    public void stopUpdate(){
        mUpdating = false;
        mTimer.cancel();
        rotationalPositionService.stopRotationLogging();

    }
    public boolean isConnected(){
        return mConnected;
    }
    public boolean isUpdating(){return mUpdating;}


}
