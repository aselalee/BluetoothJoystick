package com.aselalee.bluetoothjoystick;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothClient extends Thread {
    private static final String LOG_TAG = "BTJS[Client]";
    private static final int BUFF_SIZE = 8;
    private BluetoothSocket mSocket;
    private BluetoothDevice mDevice;
    private Handler mMainHandler; // Handler from main thread.
    private InputStream mInStream;
    private OutputStream mOutStream;

    public BluetoothClient(BluetoothDevice device, Handler handler) {
        mDevice = device;
        mMainHandler = handler;
    }


    private synchronized boolean CreateSocket() {
        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            try {
                MY_UUID = mDevice.getUuids()[0].getUuid();
            } catch (Exception e) {
                //Default UUID will be used.
                Log.e(LOG_TAG, "Default UUID is used.", e);
            }
            mSocket = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Socket's create() method failed.", e);
            Message errMsg = mMainHandler.obtainMessage(MessageConstants.MESSAGE_CONNECTION_ERROR);
            errMsg.sendToTarget();
            return false;
        }
        return true;
    }

    private synchronized boolean ConnectSocket() {
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mSocket.connect();
        } catch (IOException connectException) {
            Log.e(LOG_TAG, "Connection error. Could not connect to socket.", connectException);
            // Unable to connect; close the socket and return.
            try {
                mSocket.close();
            } catch (IOException closeException) {
                Log.e(LOG_TAG, "Connection error and could not close the client socket", closeException);
            }
            Message errMsg = mMainHandler.obtainMessage(MessageConstants.MESSAGE_CONNECTION_ERROR);
            errMsg.sendToTarget();
            return false;
        }
        return true;
    }

    public synchronized void DisconnectSocket() {
        if (mSocket.isConnected()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Could not close the client socket", e);
            }
        }
        Message msg = mMainHandler.obtainMessage(MessageConstants.MESSAGE_DISCONNECTED);
        msg.sendToTarget();
    }

    private synchronized boolean CreateStreams() {
        try {
            mInStream = mSocket.getInputStream();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error occurred when creating input stream.", e);
            Message readMsg = mMainHandler.obtainMessage(MessageConstants.MESSAGE_CONNECTION_ERROR);
            readMsg.sendToTarget();
            return false;
        }
        try {
            mOutStream = mSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error occurred when creating output stream.", e);
            Message errMsg = mMainHandler.obtainMessage(MessageConstants.MESSAGE_CONNECTION_ERROR);
            errMsg.sendToTarget();
            return false;
        }
        return true;
    }

    public void run() {
        byte[] buffer = new byte[BUFF_SIZE];
        int numBytes; // bytes returned from read()

        Message msgConnecting = mMainHandler.obtainMessage(MessageConstants.MESSAGE_CONNECTING);
        msgConnecting.sendToTarget();
        if (CreateSocket() == false || ConnectSocket() == false || CreateStreams() == false) {
            Log.e(LOG_TAG, "Could not connect. Exiting run().");
            return;
        }
        Message msgConnected = mMainHandler.obtainMessage(MessageConstants.MESSAGE_CONNECTED);
        msgConnected.sendToTarget();

        // Keep listening to the InputStream until an exception occurs.
        while (!Thread.interrupted()) {
            try {
                // Read from the InputStream.
                numBytes = mInStream.read(buffer, 0, BUFF_SIZE);
                String msgStr = new String(buffer, 0, numBytes);
                Log.d(LOG_TAG, "Read from device: " + msgStr + "Number of bytes: " + msgStr.length());
                // Send the obtained bytes to the UI activity.
                Message readMsg = mMainHandler.obtainMessage(
                        MessageConstants.MESSAGE_READ, msgStr.length(), 0,
                        msgStr);
                readMsg.sendToTarget();
            } catch (IOException e) {
                Log.d(LOG_TAG, "Input stream was disconnected.", e);
                Message readErr = mMainHandler.obtainMessage(MessageConstants.MESSAGE_READ_ERROR);
                readErr.sendToTarget();
                break;
            }
        }
        Log.d(LOG_TAG, "Thread interrupted.");
        if (mSocket.isConnected()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Could not close the client socket", e);
            }
            Message msgDisconnect = mMainHandler.obtainMessage(MessageConstants.MESSAGE_DISCONNECTED);
            msgDisconnect.sendToTarget();
        }
    }

    public synchronized void Write(String message) {
        try {
            mOutStream.write(message.getBytes());
            Message readMsg = mMainHandler.obtainMessage(
                    MessageConstants.MESSAGE_WRITTEN, message.length(), 0,
                    message);
            readMsg.sendToTarget();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error occurred when sending data.", e);
            Message writeErrorMsg = mMainHandler.obtainMessage(MessageConstants.MESSAGE_WRITE_ERROR);
            mMainHandler.sendMessage(writeErrorMsg);
        }
    }
}