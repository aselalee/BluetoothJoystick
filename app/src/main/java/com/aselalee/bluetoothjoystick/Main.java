package com.aselalee.bluetoothjoystick;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Set;

public class Main extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_ID = 99;
    private static final int REQUEST_ENABLE_BT = 98;
    private static final String LOG_TAG = "BTJS[MAIN]";
	RelativeLayout layout_joystick;
	TextView mRcvTxtV, mDirTxtV, mSpeedTxtV, mDevStatusTxtV;
    ScrollView mScrollV;
    Button mConnect, mDisconnect;
	
	JoyStickClass  mJS = null;
    BluetoothClient mBC = null;
    BluetoothAdapter mBluetoothAdapter = null;
    Handler mHandler;
    BluetoothDevice mDevice;
    int mPrevDirection = JoyStickClass.STICK_NONE;
    int mPrevSpeed = JoyStickClass.DISTANCE_STEP_0;
    private enum State {
        STATE_READY,
        STATE_WAIT
    };
    State mDeviceState = State.STATE_WAIT;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mRcvTxtV = (TextView) findViewById(R.id.received_txt);
        mDirTxtV = (TextView) findViewById(R.id.direction);
        mSpeedTxtV = (TextView) findViewById(R.id.speed_step);
        mDevStatusTxtV = (TextView) findViewById(R.id.device_status);
        mScrollV = (ScrollView) findViewById(R.id.scrollView);

        layout_joystick = (RelativeLayout) findViewById(R.id.layout_joystick);

        mJS = new JoyStickClass(getApplicationContext()
                , layout_joystick, R.drawable.image_button);
        mJS.setStickSize(250);
        mJS.setLayoutSize(1000);
        mJS.setOffset(135);
        mJS.setMinimumDistance(50);

        layout_joystick.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                mJS.drawStick(arg1);
                if (arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE) {
                    int dir = mJS.get8Direction();
                    int speed = mJS.get6StepDistance();
                    if (mDeviceState == State.STATE_READY &&
                            (dir != mPrevDirection || speed != mPrevSpeed)) {
                        mPrevDirection = dir;
                        mPrevSpeed = speed;
                        String msg = new String();
                        mSpeedTxtV.setText("Speed: " + mJS.get6StepDistanceAsString());
                        mDirTxtV.setText("Direction: " + mJS.get8DirectionAsSting());
                        if (dir == JoyStickClass.STICK_UP) msg = "F";
                        else if (dir == JoyStickClass.STICK_LEFT) msg = "L";
                        else if (dir == JoyStickClass.STICK_RIGHT) msg = "R";
                        else if (dir == JoyStickClass.STICK_DOWN) msg = "B";
                        else if (dir == JoyStickClass.STICK_NONE) msg = "S";
                        if (!msg.isEmpty()) {
                            mRcvTxtV.append(">" + msg + "\n");
                            mScrollV.fullScroll(View.FOCUS_DOWN);
                            mBC.Write(msg);
                        }
                    }
                } else if (arg1.getAction() == MotionEvent.ACTION_UP) {
                    mSpeedTxtV.setText("Speed: ");
                    mDirTxtV.setText("Direction: ");
                    if (mDeviceState == State.STATE_READY) {
                        String msg = new String("S");
                        mRcvTxtV.append(">" + msg + "\n");
                        mScrollV.fullScroll(View.FOCUS_DOWN);
                        mBC.Write(msg);
                    }
                }
                return true;
            }
        });
        //Request bluetooth permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(LOG_TAG, "Bluetooth permission not available. Requesting permission.");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH},
                    PERMISSION_REQUEST_ID);
        }
        else
        {
            Log.i(LOG_TAG, "Bluetooth permission already available.");
        }
        mConnect = (Button)findViewById(R.id.connect);
        mDisconnect = (Button)findViewById(R.id.disconnect);
        mConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectToBluetoothDevice();
            }
        });
        mDisconnect.setEnabled(false);
        mDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Disconnect() == true) {
                    Log.i(LOG_TAG, "Bluetooth disconnected.");
                    mConnect.setEnabled(true);
                    mDisconnect.setEnabled(false);
                }
            }
        });
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message inputMessage) {

                switch (inputMessage.what) {
                    case MessageConstants.MESSAGE_READ:
                        String msgStr = (String) inputMessage.obj;
                        if (msgStr != null) {
                            mRcvTxtV.append(msgStr);
                            mScrollV.fullScroll(View.FOCUS_DOWN);
                            Log.i(LOG_TAG, "Message read: " + msgStr);
                        }
                        break;
                    case MessageConstants.MESSAGE_READ_ERROR:
                        Log.e(LOG_TAG, "Message read error");
                        mDevStatusTxtV.setText("Status: Read Error");
                        mDeviceState = State.STATE_WAIT;
                        break;
                    case MessageConstants.MESSAGE_WRITTEN:
                        Log.i(LOG_TAG, "Message written.");
                        break;
                    case MessageConstants.MESSAGE_WRITE_ERROR:
                        Log.e(LOG_TAG, "Message write error.");
                        mDevStatusTxtV.setText("Status: Write Error");
                        mDeviceState = State.STATE_WAIT;
                        break;
                    case MessageConstants.MESSAGE_CONNECTED:
                        Log.i(LOG_TAG, "Connected.");
                        mDevStatusTxtV.setText("Status: Connected");
                        mDeviceState = State.STATE_READY;
                        break;
                    case MessageConstants.MESSAGE_CONNECTION_ERROR:
                        Log.e(LOG_TAG, "Connection error.");
                        mDevStatusTxtV.setText("Status: Connection Error");
                        mDeviceState = State.STATE_WAIT;
                        break;
                    case MessageConstants.MESSAGE_DISCONNECTED:
                        Log.i(LOG_TAG, "Disconnected.");
                        mDevStatusTxtV.setText("Status: Disconnected");
                        mDeviceState = State.STATE_WAIT;
                        break;
                }
            }
        };
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Happy days case.
                    Log.i(LOG_TAG, "Bluetooth permission granted.");
                } else {
                    // permission denied, display message and exit.
                    Log.i(LOG_TAG, "Bluetooth permission denied.");
                    new AlertDialog.Builder(this)
                            .setMessage("Need bluetooth permission to connect to device.")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setCancelable(false)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Main.this.finish();
                                }
                            })
                            .show();
                }
                return;
            }
        }
    }
    private void ConnectToBluetoothDevice() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.i(LOG_TAG, "Bluetooth not available.");
            new AlertDialog.Builder(this)
                    .setMessage("Device doesn't support bluetooth.")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //Do nothing.
                        }
                    })
                    .show();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Log.i(LOG_TAG, "Opening enable Bluetooth intent.");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {
            Log.i(LOG_TAG, "Bluetooth on. Try to connect.");
            if (Connect() == true) {
                mConnect.setEnabled(false);
                mDisconnect.setEnabled(true);
            }
        }
    }

    private boolean Connect() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        boolean deviceFound = false;
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                if (deviceName.equals("HC-06")) {
                    mDevice = device;
                    deviceFound = true;
                }
            }
        }
        if (pairedDevices.size() < 1 || deviceFound == false) {
            new AlertDialog.Builder(this)
                    .setMessage("Could not find HC-06 device")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //Do nothing.
                        }
                    })
                    .show();
            return false;
        }
        mBC = new BluetoothClient(mDevice, mHandler);
        mBC.start();
        return true;
    }

    private boolean Disconnect() {
        mDeviceState = State.STATE_WAIT;
        mBC.DisconnectSocket();
        mBC.interrupt();
        return true;
    }

    @Override
    public void onActivityResult (int requestCode,
                                  int resultCode,
                                  Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    if (Connect() == true) {
                        mConnect.setEnabled(false);
                        mDisconnect.setEnabled(true);
                    }
                }
                else
                {
                    new AlertDialog.Builder(this)
                            .setMessage("Enable bluetooth to connect to device.")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setCancelable(false)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    //Do nothing.
                                }
                            })
                            .show();
                }
        }

    }

}
