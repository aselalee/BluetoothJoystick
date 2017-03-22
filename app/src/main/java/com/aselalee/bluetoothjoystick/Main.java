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
import android.widget.TextView;

import java.util.Set;

public class Main extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_ID = 99;
    private static final int REQUEST_ENABLE_BT = 98;
    private static final String LOG_TAG = "BTJS[MAIN]";
	RelativeLayout layout_joystick;
	TextView textView1, textView2, textView3, textView4, textView5;
    Button mConnect, mDisconnect;
	
	JoyStickClass  mJS = null;
    BluetoothClient mBC = null;
    BluetoothAdapter mBluetoothAdapter = null;
    Handler mHandler;
    BluetoothDevice mDevice;
    private enum State {
        STATE_READY,
        STATE_WAIT
    };
    State mDeviceState = State.STATE_WAIT;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        textView1 = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        textView3 = (TextView) findViewById(R.id.textView3);
        textView4 = (TextView) findViewById(R.id.textView4);
        textView5 = (TextView) findViewById(R.id.textView5);

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
                    //textView1.setText("X : " + String.valueOf(mJS.getX()));
                    //textView2.setText("Y : " + String.valueOf(mJS.getY()));
                    //textView3.setText("Angle : " + String.valueOf(mJS.getAngle()));
                    textView3.setText("Step Distance : " + mJS.get6StepDistanceAsString());
                    textView4.setText("Distance : " + String.valueOf(mJS.getDistance()));
                    textView5.setText("Direction : " + mJS.get8DirectionAsSting());
                } else if (arg1.getAction() == MotionEvent.ACTION_UP) {
                    //textView1.setText("X :");
                    //textView2.setText("Y :");
                    //textView3.setText("Angle :");
                    textView3.setText("Step Distance :");
                    textView4.setText("Distance :");
                    textView5.setText("Direction :");
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
                        Log.d(LOG_TAG, "Message read.");
                        byte[] buffer = (byte[]) inputMessage.obj;
                        if (buffer != null) {
                            String message = new String(buffer);
                            Log.d(LOG_TAG, message);
                        }
                        break;
                    case MessageConstants.MESSAGE_READ_ERROR:
                        Log.d(LOG_TAG, "Message read error");
                        mDeviceState = State.STATE_WAIT;
                        break;
                    case MessageConstants.MESSAGE_WRITTEN:
                        Log.d(LOG_TAG, "Message written.");
                        break;
                    case MessageConstants.MESSAGE_WRITE_ERROR:
                        Log.d(LOG_TAG, "Message write error.");
                        mDeviceState = State.STATE_WAIT;
                        break;
                    case MessageConstants.MESSAGE_CONNECTED:
                        Log.d(LOG_TAG, "Connected.");
                        mDeviceState = State.STATE_READY;
                        break;
                    case MessageConstants.MESSAGE_CONNECTION_ERROR:
                        Log.d(LOG_TAG, "Connection error.");
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
                    Log.i(LOG_TAG, "Bluetooth permission denied.");
                    // permission denied, display message and exit.
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
