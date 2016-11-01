package com.example.nero.servotest;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.warmnut.bt.BtServo;
import com.warmnut.bt.WMBluetoothManager;

public class TestActivity extends AppCompatActivity {
    private TextView mView;
    private EditText mEditTextInt;
    private EditText mEditTextNumEditText;
    private EditText mEditTextRead;
    private TextView mBatteryIndicator;
    private Button mtestButton;

    private static final int REQUEST_ENABLE_BT = 1;
    final private String TAG = "xuxu";

    private WMBluetoothManager wmBluetoothManager;

    private BtServo.StateCallback mStateCallback = new BtServo.StateCallback() {
        @Override
        public void onStateChange(final BtServo.ConnectState state) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (state) {
                        case disconnected:
                            mView.setText(R.string.disconnected);
                            break;
                        case connecting:
                            mView.setText(R.string.connecting);
                            break;
                        case connected:
                            mView.setText(R.string.connected);
                            Log.d(TAG, "connected");
                            new Thread() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "readAngle: " + wmBluetoothManager.btServo.readAngle());
                                    Log.d(TAG, "readSpeed: " + wmBluetoothManager.btServo.readSpeed());
                                    Log.d(TAG, "readPowerOnState: " + wmBluetoothManager.btServo.readPowerState());
                                    wmBluetoothManager.btServo.setBatteryUpdate(true);
                                }
                            }.start();
                            break;
                        default:
                            break;
                    }
                }
            });
        }

        @Override
        public void onDidReadAngle(final float angle) {

        }

        @Override
        public void onDidReadSpeed(final int speed) {

        }

        @Override
        public void onDidReadPowerState(boolean onoff) {

        }

        @Override
        public void didUpdateBattery(final int indcater) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String show = indcater + "%";
                    mBatteryIndicator.setText(show);
                }
            });
        }
    };

    private View.OnClickListener mConnectListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (wmBluetoothManager.btServo.getConnectionState() == BtServo.ConnectState.disconnected) {
                new Thread() {
                    @Override
                    public void run() {
                        wmBluetoothManager.btServo.StateCallbacks.add(mStateCallback);
                        wmBluetoothManager.btServo.initializeConnection();
                    }
                }.start();
            }

            if (wmBluetoothManager.btServo.getConnectionState() == BtServo.ConnectState.connected) {
                new Thread() {
                    @Override
                    public void run() {
                        wmBluetoothManager.btServo.disconnect();
                    }
                }.start();
            }

            }
    };

    private View.OnClickListener mSetAngleListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
                if(mEditTextInt.getText().length() > 0){
                    new Thread() {
                        @Override
                        public void run() {
                            wmBluetoothManager.btServo.writeAngle(Float.parseFloat(mEditTextInt.getText().toString()));
                        }
                    }.start();
                }
            }
    };

    private View.OnClickListener mSetSpeedListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
                if(mEditTextNumEditText.getText().length() > 0){
                    new Thread() {
                        @Override
                        public void run() {
                            wmBluetoothManager.btServo.writeSpeed(Integer.parseInt(mEditTextNumEditText.getText().toString()));
                        }
                    }.start();
                }
        }
    };

    private View.OnClickListener mReadValueListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

                new Thread() {
                    @Override
                    public void run() {

                        Log.d(TAG, "readAngle: " + wmBluetoothManager.btServo.readAngle());
                        Log.d(TAG, "readSpeed: " + wmBluetoothManager.btServo.readSpeed());
                        Log.d(TAG, "readPowerOnState: " + wmBluetoothManager.btServo.readPowerState());
                        Log.d(TAG, "readMAC: " + wmBluetoothManager.btServo.getMAC());
                    }
                }.start();
            }
    };

    private CompoundButton.OnCheckedChangeListener mOnOffListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    new Thread() {
                        @Override
                        public void run() {
                            wmBluetoothManager.btServo.setPowerOn(true);
                        }
                    }.start();

                } else {
                    new Thread() {
                        @Override
                        public void run() {
                            wmBluetoothManager.btServo.setPowerOn(false);
                        }
                    }.start();
                }
            }
    };

    private View.OnClickListener mTestButton = new View.OnClickListener() {
        Thread testThread;
        boolean startFlag = false;
        @Override
        public void onClick(View v) {
            if (null == testThread) {
                startFlag = true;
                testThread = new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        if (wmBluetoothManager.btServo.isInit) {
                            if (startFlag) {
                                wmBluetoothManager.btServo.writeSpeed(20);
                                wmBluetoothManager.btServo.setPowerOn(true);
                            }
                            while (startFlag) {
                                wmBluetoothManager.btServo.writeAngle(0);
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                wmBluetoothManager.btServo.writeAngle(180);
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            wmBluetoothManager.btServo.setPowerOn(false);
                        }
                    }
                };
                testThread.start();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mtestButton.setBackgroundResource(R.color.colorStop);
                        mtestButton.setText("STOP TEST");
                    }
                });
            } else {
                startFlag = false;
                testThread = null;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mtestButton.setBackgroundResource(R.color.colorStart);
                        mtestButton.setText("START TEST");
                    }
                });

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        mView = (TextView) findViewById(R.id.State);
        mView.setOnClickListener(mConnectListener);
        Button mButton = (Button) findViewById(R.id.SetAngle);
        mButton.setOnClickListener(mSetAngleListener);
        mButton = (Button) findViewById(R.id.SetSpeed);
        mButton.setOnClickListener(mSetSpeedListener);
        mButton = (Button) findViewById(R.id.ReadValue);
        mButton.setOnClickListener(mReadValueListener);
        mtestButton = (Button) findViewById(R.id.testbutton);
        mtestButton.setOnClickListener(mTestButton);
        Switch mSwitch = (Switch) findViewById(R.id.switch1);
        mSwitch.setOnCheckedChangeListener(mOnOffListener);
        mEditTextInt = (EditText) findViewById(R.id.editTextInt);
        mEditTextNumEditText = (EditText) findViewById(R.id.editTextNum);
        mEditTextRead = (EditText) findViewById(R.id.textValues);
        mBatteryIndicator = (TextView) findViewById(R.id.BatteryIndcater);

        try {
            wmBluetoothManager = WMBluetoothManager.getManager(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        // Use this check to determine whether BLE is supported on the device.  Then you can
//        // selectively disable BLE-related features.
//        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
//            finish();
//        }
//
//
//
//        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
//        // BluetoothAdapter through BluetoothManager.
//        final BluetoothManager bluetoothManager =
//                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = bluetoothManager.getAdapter();
//
//        // Checks if Bluetooth is supported on the device.
//        if (mBluetoothAdapter == null) {
//            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//       try {
//           mBtServo = new BtServo(mBluetoothAdapter, this);
//       } catch (Exception e) {
//           Log.e(TAG, e.toString());
//       }



//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

    }


    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!wmBluetoothManager.mBluetoothAdapter.isEnabled()) {
            if (!wmBluetoothManager.mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

//        final BluetoothManager bluetoothManager =
//                (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
//        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
//        if (mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

