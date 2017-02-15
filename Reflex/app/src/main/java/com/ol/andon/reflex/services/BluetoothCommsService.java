package com.ol.andon.reflex.services;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Andon on 08/02/2017.
 */

public class BluetoothCommsService {

    private final static String TAG = "BL Comms Service";
    private Handler mHandler;

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }

    private class ConnectThread extends Thread{
        private final BluetoothSocket mmSocket;

        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.

            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                if(mmDevice != null && mmDevice.getUuids() != null)
                tmp = device.createRfcommSocketToServiceRecord(mmDevice.getUuids()[0].getUuid());
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
           // mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            ConnectedThread connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    private class ConnectedThread extends Thread{

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;

        ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try{
                tempIn = socket.getInputStream();
            }
            catch(IOException e){
                Log.e(TAG, "Error in creating IStream");
            }

            try{
                tempOut= socket.getOutputStream();
            }
            catch(IOException e){
                Log.e(TAG, "Error in creating OStream");
            }

            mmInStream = tempIn;
            mmOutStream = tempOut;
        }

        public void run(){
            mmBuffer = new byte[1024];
            int numBytes;
            while(true){
                try{
                    numBytes = mmInStream.read(mmBuffer);
                    Message readMsg = mHandler.obtainMessage(MessageConstants.MESSAGE_READ, numBytes, 1);
                    readMsg.sendToTarget();
                }
                catch(IOException e){
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}
