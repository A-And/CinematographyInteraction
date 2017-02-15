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

import java.util.List;
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

    private int currentX = 0;
    private int currentY = 0;
    private int currentZ = 0;

    public static String UARTSERVICE_SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static String UART_TX_CHARACTERISTIC_UUID ="6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static String UART_RX_CHARACTERISTIC_UUID ="6e400003-b5a3-f393-e0a9-e50e24dcca9e";

    final static String TAG = "MicroBitComms";

    private String mMicrobitAddress;
    private Activity activity;
    private boolean mConnected;
    private Handler mHandler;
    private final static int REQUEST_ENABLE_BT = 1;

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

    public MicroBitCommsService(Activity activity){
        this.activity = activity;
        this.mHandler = new Handler();
        bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
    }
    public boolean connect(){
        boolean res = false;
         mBluetoothAdapter = bluetoothManager.getAdapter();
        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
            Intent enableBltIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBltIntent, REQUEST_ENABLE_BT);
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
        UUID ioPinUUID = UUID.fromString(UARTSERVICE_SERVICE_UUID);
        for(BluetoothGattService service : services){
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
    }

    public void writeXY(byte x, byte y){
        if(!mConnected) return;
        if(rxCharacteristic == null) return;

        byte[] moveData = {0 , x, 1, y};
        rxCharacteristic.setValue(moveData);
        Log.i(TAG, "BLE write: " + mBluetoothGatt.writeCharacteristic(rxCharacteristic));
        Log.i(TAG, "BLE write: " + mBluetoothGatt.readCharacteristic(rxCharacteristic) +  " " + rxCharacteristic.getValue());

    }

    public void writeXYZ(byte x, byte y, byte z){
        if(!mConnected) return;
        if(rxCharacteristic == null) return;

        byte[] moveData = {0 , x, 1, y, 2, z};
        rxCharacteristic.setValue(moveData);
        Log.i(TAG, "BLE write: " + mBluetoothGatt.writeCharacteristic(rxCharacteristic));
        Log.i(TAG, "BLE write: " + mBluetoothGatt.readCharacteristic(rxCharacteristic) +  " " + rxCharacteristic.getValue());

    }

    public void write0(){
        if(!mConnected) return;
        if(rxCharacteristic == null) return;

        byte[] xData = {0x00, 0x00};
        rxCharacteristic.setValue(xData);
        Log.i(TAG, "BLE write: " + mBluetoothGatt.writeCharacteristic(rxCharacteristic));
        Log.i(TAG, "BLE write: " + mBluetoothGatt.readCharacteristic(rxCharacteristic) +  " " + rxCharacteristic.getValue());
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

        if(microBit != null){
            BluetoothDevice actual = mBluetoothAdapter.getRemoteDevice(microBit.getAddress());
            mBluetoothGatt = actual.connectGatt(this.activity, false, gattCallback);

        }
    }
    public boolean isConnected(){
        return mConnected;
    }


}
