package com.aselalee.bluetoothjoystick;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

public class BluetoothClient extends Thread{
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private static final String LOG_TAG = "BTJS[Client]";
    private Handler mHandler; // handler that gets info from Bluetooth service
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream

    public BluetoothClient(BluetoothDevice device, Handler handler) {
        mmDevice = device;
        mHandler = handler;
    }

    private boolean CreateSocket() {
        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            try {
                MY_UUID = mmDevice.getUuids()[0].getUuid();
            } catch (Exception e) {
                //Default UUID will be used.
                Log.e(LOG_TAG, "Default UUID is used.", e);
            }
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Socket's create() method failed.", e);
            Message errMsg = mHandler.obtainMessage(MessageConstants.MESSAGE_CONNECTION_ERROR);
            errMsg.sendToTarget();
            return false;
        }
        return true;
    }

    private boolean ConnectSocket() {
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
        } catch (IOException connectException) {
            Log.e(LOG_TAG, "Connection error. Could not connect to socket.", connectException);
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(LOG_TAG, "Connection error and could not close the client socket", closeException);
            }
            Message errMsg = mHandler.obtainMessage(MessageConstants.MESSAGE_CONNECTION_ERROR);
            errMsg.sendToTarget();
            return false;
        }
        return true;
    }

    public boolean CreateStreams() {
        try {
            mmInStream = mmSocket.getInputStream();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error occurred when creating input stream.", e);
            Message readMsg = mHandler.obtainMessage(MessageConstants.MESSAGE_CONNECTION_ERROR);
            readMsg.sendToTarget();
            return false;
        }
        try {
            mmOutStream = mmSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error occurred when creating output stream.", e);
            Message errMsg = mHandler.obtainMessage(MessageConstants.MESSAGE_CONNECTION_ERROR);
            errMsg.sendToTarget();
            return false;
        }
        return true;
    }

    public void run() {
        mmBuffer = new byte[1024];
        int numBytes; // bytes returned from read()
        if (CreateSocket() == false || ConnectSocket() == false || CreateStreams() == false) {
            Log.e(LOG_TAG, "Could not connect. Exiting run().");
            return;
        }
        Message msg = mHandler.obtainMessage(MessageConstants.MESSAGE_CONNECTED);
        msg.sendToTarget();

        // Keep listening to the InputStream until an exception occurs.
        while (!Thread.interrupted()) {
            try {
                // Read from the InputStream.
                numBytes = mmInStream.read(mmBuffer);
                String msgStr = new String(mmBuffer, 0, numBytes);
                Log.i(LOG_TAG, "Bytes as string: " + msgStr);
                // Send the obtained bytes to the UI activity.
                Message readMsg = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_READ, numBytes, 0,
                        msgStr);
                readMsg.sendToTarget();
            } catch (IOException e) {
                Log.d(LOG_TAG, "Input stream was disconnected.", e);
                Message readErr = mHandler.obtainMessage(MessageConstants.MESSAGE_READ_ERROR);
                readErr.sendToTarget();
                break;
            }
        }
        Log.d(LOG_TAG, "Thread interrupted.");
    }

    public void DisconnectSocket() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Could not close the client socket", e);
        }
    }

    // Call this from the main activity to send data to the remote device.
    public void Write(String message) {
        try {
            mmOutStream.write(message.getBytes());
            Message writtenMsg = mHandler.obtainMessage(MessageConstants.MESSAGE_WRITTEN);
            writtenMsg.sendToTarget();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error occurred when sending data.", e);
            Message writeErrorMsg = mHandler.obtainMessage(MessageConstants.MESSAGE_WRITE_ERROR);
            mHandler.sendMessage(writeErrorMsg);
        }
    }
}