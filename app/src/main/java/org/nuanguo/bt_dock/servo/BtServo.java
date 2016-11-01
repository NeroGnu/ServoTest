package org.nuanguo.bt_dock.servo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.UUID;

/**
 * Created by Nero on 2016/2/24.
 */
public class BtServo {
    private ConnectState mConnectState = ConnectState.disconnected;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private Handler mHandler;

    final static private String SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    final static private String CHARACTERISTIC1_UUID = "0000fff1-0000-1000-8000-00805f9b34fb";
    final static private String CHARACTERISTIC2_UUID = "0000fff2-0000-1000-8000-00805f9b34fb";
    final static private String CHARACTERISTIC3_UUID = "0000fff3-0000-1000-8000-00805f9b34fb";

    final private UUID mServiceUuid = UUID.fromString(SERVICE_UUID);
    final private UUID mCharacteristic1Uuid = UUID.fromString(CHARACTERISTIC1_UUID);
    final private UUID mCharacteristic2Uuid = UUID.fromString(CHARACTERISTIC2_UUID);
    final private UUID mCharacteristic3Uuid = UUID.fromString(CHARACTERISTIC3_UUID);

    private BluetoothGattService mBluetoothGattService;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic1;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic2;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic3;

    private boolean mCharacteristic1OnWrite = false;
    private boolean mCharacteristic2OnWrite = false;
    private boolean mCharacteristic3OnWrite = false;

    private boolean mCharacteristic1OnRead = false;
    private boolean mCharacteristic2OnRead = false;
    private boolean mCharacteristic3OnRead = false;

    final static private int REMOTE_CHARACTERISTIC1_LENGTH = 2;
    final static private int REMOTE_CHARACTERISTIC2_LENGTH = 1;
    final static private int REMOTE_CHARACTERISTIC3_LENGTH = 1;

    private static final long SCAN_PERIOD = 10000;
    private static final int PWM_FLOOR = 630;
    private final static int TURN_INTERVAL = 20;

    private static int mSpeed = 10;
    private static int mAngle = 1500;

    private Context mContext;
    private StateCallback mStateCallback;
    final private String TAG = "BtServo";

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.d(TAG, "find device " + device.getName());
                    mBluetoothDevice = device;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mGattCallback);
                }

            };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                mConnectState = ConnectState.disconnected;
                if (mStateCallback != null) {
                    mStateCallback.onStateChange(mConnectState);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBluetoothGattService = mBluetoothGatt.getService(mServiceUuid);
                Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED");

                mBluetoothGattCharacteristic1 = mBluetoothGattService.getCharacteristic(mCharacteristic1Uuid);
                mBluetoothGattCharacteristic2 = mBluetoothGattService.getCharacteristic(mCharacteristic2Uuid);
                mBluetoothGattCharacteristic3 = mBluetoothGattService.getCharacteristic(mCharacteristic3Uuid);

                Log.d(TAG, "ACTION_GATT_CHARACTERISTIC_DISCOVERED");
                mConnectState = ConnectState.connected;
                if (mStateCallback != null) {
                    mStateCallback.onStateChange(mConnectState);
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                mConnectState = ConnectState.disconnected;
                if (mStateCallback != null) {
                    mStateCallback.onStateChange(mConnectState);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            switch (characteristic.getUuid().toString())
            {
                case CHARACTERISTIC1_UUID:
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        mCharacteristic1OnRead = true;
                    }
                    break;
                case CHARACTERISTIC2_UUID:
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        mCharacteristic2OnRead = true;
                    }
                    break;
                case CHARACTERISTIC3_UUID:
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        mCharacteristic3OnRead = true;
                    }
                    break;
                default:
            }
            Log.d(TAG, "onCharacteristicRead: " + status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged");
        }

        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            switch (characteristic.getUuid().toString())
            {
                case CHARACTERISTIC1_UUID:
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        mCharacteristic1OnWrite = true;
                    }
                    break;
                case CHARACTERISTIC2_UUID:
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        mCharacteristic2OnWrite = true;
                    }
                    break;
                case CHARACTERISTIC3_UUID:
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        mCharacteristic3OnWrite = true;
                    }
                    break;
                default:
            }
            Log.d(TAG, "onCharacteristicWrite: " + status);
        }
    };

    public BtServo(final BluetoothAdapter mBluetoothAdapter, Context mContext) throws Exception {
        if (mBluetoothAdapter == null) {
            throw new Exception("none BluetoothAdapter");
        }
        this.mBluetoothAdapter = mBluetoothAdapter;
        this.mContext = mContext;
        this.mHandler = new Handler();
    }

    public void initialize(StateCallback stateCallback) {
        this.mStateCallback = stateCallback;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                if (ConnectState.connecting == mConnectState) {
                    mConnectState = ConnectState.disconnected;
                    if (mStateCallback != null) {
                        mStateCallback.onStateChange(mConnectState);
                    }
                }
            }
        }, SCAN_PERIOD);
        mConnectState = ConnectState.connecting;
        if (mStateCallback != null) {
            mStateCallback.onStateChange(mConnectState);
        }
        UUID uuids[] = new UUID[1];
        uuids[0] = mServiceUuid;
        mBluetoothAdapter.startLeScan(uuids, mLeScanCallback);
    }

    public boolean reConnect() throws Exception {
        if (mBluetoothGatt == null) {
            throw new Exception("none BluetoothDevice");
        }
        if (mBluetoothGatt.connect()) {
            return true;
        } else {
            mConnectState = ConnectState.disconnected;
            if (mStateCallback != null) {
                mStateCallback.onStateChange(mConnectState);
            }
            return false;
        }
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        mConnectState = ConnectState.disconnected;
        if (mStateCallback != null) {
            mStateCallback.onStateChange(mConnectState);
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    private static byte[] u162byte(byte[] b) {
        byte[] b1 = {'0', '0', '0', '0'};
        if (b.length > 4) {
            for (int i = b.length, j = 3; i > b.length - 4; i--, j--) {
                b1[j] = b[i - 1];
            }
        } else {
            for (int i = b.length, j = 3; i > 0; i--, j--) {
                b1[j] = b[i - 1];
            }
        }

        byte[] b2 = new byte[b1.length / 2];
        for (int n = 0; n < b1.length; n += 2) {
            String item = new String(b1, n, 2);
            // 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个进制字节
            b2[n / 2] = (byte) Integer.parseInt(item, 16);
        }
//        b1 = null;
        return b2;
    }

    public long setAngle(float angle) {
        int IntAngle = (int) (angle * 10) + PWM_FLOOR;
        long TurnTime = (long) (Math.ceil((double) (Math.abs(IntAngle - mAngle) / mSpeed)) * TURN_INTERVAL);
        mAngle = IntAngle;
        byte[] WriteBytes = new byte[REMOTE_CHARACTERISTIC1_LENGTH];

        System.arraycopy(u162byte(Integer.toHexString(IntAngle).getBytes()), 0, WriteBytes, 0, 2);
        if (mBluetoothAdapter != null && mBluetoothGatt != null) {
            mBluetoothGattCharacteristic1.setValue(WriteBytes[0],
                    BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            mBluetoothGattCharacteristic1.setValue(WriteBytes);
            mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic1);
            return TurnTime;
        } else {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return 0;
        }
    }

    public long setAngleTrusted(float angle) {
        int IntAngle = (int) (angle * 10) + PWM_FLOOR;
        long TurnTime = (long) (Math.ceil((double) (Math.abs(IntAngle - mAngle) / mSpeed)) * TURN_INTERVAL);
        mAngle = IntAngle;
        byte[] WriteBytes = new byte[REMOTE_CHARACTERISTIC1_LENGTH];

        System.arraycopy(u162byte(Integer.toHexString(IntAngle).getBytes()), 0, WriteBytes, 0, 2);
        if (mBluetoothAdapter != null && mBluetoothGatt != null) {
            mBluetoothGattCharacteristic1.setValue(WriteBytes[0],
                    BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            mBluetoothGattCharacteristic1.setValue(WriteBytes);
            mCharacteristic1OnWrite = false;
            mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic1);
            while (!mCharacteristic1OnWrite) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    Log.e(TAG, "setAngleTrusted error!");
                }
            }
            return TurnTime;
        } else {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return 0;
        }
    }

    public float readAngle() {
        byte[] WriteBytes;
        int IntAngle;
        float angle;

        if (mBluetoothAdapter != null && mBluetoothGatt != null) {
            mCharacteristic1OnRead = false;
            mBluetoothGatt.readCharacteristic(mBluetoothGattCharacteristic1);
            while(!mCharacteristic1OnRead) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    Log.e(TAG, "readAngle error!");
                }
            }
            WriteBytes = mBluetoothGattCharacteristic1.getValue();
        } else {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return -1;
        }
        IntAngle = (((WriteBytes[0] << 8) & 0xFF00) | (WriteBytes[1] & 0xFF)) - PWM_FLOOR;
        angle = (float) IntAngle/10;
        return angle;
    }

    public void setSpeed(int step) {
        byte[] WriteBytes = new byte[REMOTE_CHARACTERISTIC2_LENGTH];
        if (step < 0) {
            WriteBytes[0] = (byte) 0;
        } else if (step > 100){
            WriteBytes[0] = (byte) 100;
        } else {
            WriteBytes[0] = (byte) step;
        }

        mSpeed = (int) WriteBytes[0];

        if (mBluetoothAdapter != null && mBluetoothGatt != null) {
            mBluetoothGattCharacteristic2.setValue(WriteBytes[0],
                    BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            mBluetoothGattCharacteristic2.setValue(WriteBytes);
            mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic2);
        } else {
            Log.w(TAG, "BluetoothAdapter not initialized");
        }
    }

    public boolean setSpeedTrusted(int step) {
        byte[] WriteBytes = new byte[REMOTE_CHARACTERISTIC2_LENGTH];
        if (step < 0) {
            WriteBytes[0] = (byte) 0;
        } else if (step > 100){
            WriteBytes[0] = (byte) 100;
        } else {
            WriteBytes[0] = (byte) step;
        }

        mSpeed = (int) WriteBytes[0];

        if (mBluetoothAdapter != null && mBluetoothGatt != null) {
            mBluetoothGattCharacteristic2.setValue(WriteBytes[0],
                    BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            mBluetoothGattCharacteristic2.setValue(WriteBytes);
            mCharacteristic2OnWrite = false;
            mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic2);
            while (!mCharacteristic2OnWrite) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    Log.e(TAG, "setSpeedTrusted error!");
                }
            }
        } else {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        return true;
    }

    public int readSpeed() {
        byte[] WriteBytes;
        if (mBluetoothAdapter != null && mBluetoothGatt != null) {
            mCharacteristic2OnRead = false;
            mBluetoothGatt.readCharacteristic(mBluetoothGattCharacteristic2);
            while(!mCharacteristic2OnRead) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    Log.e(TAG, "readSpeed error!");
                }
            }
            WriteBytes = mBluetoothGattCharacteristic2.getValue();
        } else {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return -1;
        }
        return WriteBytes[0] & 0xFF;
    }

    public void powerOnOff(boolean onoff) {
        byte[] WriteBytes = new byte[REMOTE_CHARACTERISTIC3_LENGTH];
        if (onoff) {
            WriteBytes[0] = (byte) 0x01;
        } else {
            WriteBytes[0] = (byte) 0x00;
        }
        if (mBluetoothAdapter != null && mBluetoothGatt != null) {
            mBluetoothGattCharacteristic3.setValue(WriteBytes[0],
                    BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            mBluetoothGattCharacteristic3.setValue(WriteBytes);
            mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic3);
        } else {
            Log.w(TAG, "BluetoothAdapter not initialized");
        }
    }

    public boolean powerOnOffTrusted(boolean onoff) {
        byte[] WriteBytes = new byte[REMOTE_CHARACTERISTIC3_LENGTH];
        if (onoff) {
            WriteBytes[0] = (byte) 0x01;
        } else {
            WriteBytes[0] = (byte) 0x00;
        }
        if (mBluetoothAdapter != null && mBluetoothGatt != null) {
            mBluetoothGattCharacteristic3.setValue(WriteBytes[0],
                    BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            mBluetoothGattCharacteristic3.setValue(WriteBytes);
            mCharacteristic3OnWrite = false;
            mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic3);
            while (!mCharacteristic3OnWrite) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    Log.e(TAG, "powerOnOffTrusted error!");
                }
            }
        } else {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        return true;
    }

    public boolean readPowerOnState() {
        byte[] WriteBytes;
        if (mBluetoothAdapter != null && mBluetoothGatt != null) {
            mCharacteristic3OnRead = false;
            mBluetoothGatt.readCharacteristic(mBluetoothGattCharacteristic3);
            while(!mCharacteristic3OnRead) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    Log.e(TAG, "readPowerOnState error!");
                }
            }
            WriteBytes = mBluetoothGattCharacteristic3.getValue();
        } else {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        return !(0 == WriteBytes[0]);
    }

}
