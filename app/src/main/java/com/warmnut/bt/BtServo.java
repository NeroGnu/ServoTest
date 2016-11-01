package com.warmnut.bt;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by Nero on 2016/2/24.
 */
public class BtServo {

    public List<StateCallback> StateCallbacks;

    public boolean isInit = false;
    //belows are members.
    private ConnectState mConnectState = ConnectState.disconnected;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private Handler mHandler;
    private ArrayList<String> mMACList;
    private ArrayList<BluetoothDevice> mDeviceList;

    final static private String SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    final static private String ANGLE_CHARACTERISTIC_UUID = "0000fff1-0000-1000-8000-00805f9b34fb";
    final static private String SPEED_CHARACTERISTIC_UUID = "0000fff2-0000-1000-8000-00805f9b34fb";
    final static private String POWER_CHARACTERISTIC_UUID = "0000fff3-0000-1000-8000-00805f9b34fb";
    final static private String BATTERY_CHARACTERISTIC_UUID = "0000fff4-0000-1000-8000-00805f9b34fb";
//    final static private String MAC_CHARACTERISTIC_UUID = "0000fff5-0000-1000-8000-00805f9b34fb";

    final private UUID mServiceUuid = UUID.fromString(SERVICE_UUID);
    final private UUID AngleCharacteristicUUID = UUID.fromString(ANGLE_CHARACTERISTIC_UUID);
    final private UUID SpeedCharacteristicUUID = UUID.fromString(SPEED_CHARACTERISTIC_UUID);
    final private UUID PowerCharacteristicUUID = UUID.fromString(POWER_CHARACTERISTIC_UUID);
    final private UUID BatteryCharacteristicUUID = UUID.fromString(BATTERY_CHARACTERISTIC_UUID);
//    final private UUID MACCharacteristicUUID = UUID.fromString(MAC_CHARACTERISTIC_UUID);

    private BluetoothGattService DockService;
    private BluetoothGattCharacteristic AngleCharacteristic;
    private BluetoothGattCharacteristic SpeedCharacteristic;
    private BluetoothGattCharacteristic PowerCharacteristic;
    private BluetoothGattCharacteristic BatteryCharacteristic;
//    private BluetoothGattCharacteristic MACCharacteristic;

//    private SynchronousQueue<Boolean> InitializeDone;

    private SynchronousQueue<Boolean> AngleCharacteristicOnWrite;
    private SynchronousQueue<Boolean> SpeedCharacteristicOnWrite;
    private SynchronousQueue<Boolean> PowerCharacteristicOnWrite;
//    private SynchronousQueue<Boolean> ChargeCharacteristicOnWrite;

    private SynchronousQueue<Boolean> AngleCharacteristicOnRead;
    private SynchronousQueue<Boolean> SpeedCharacteristicOnRead;
    private SynchronousQueue<Boolean> PowerCharacteristicOnRead;
//    private SynchronousQueue<Boolean> BatteryCharacteristicOnRead;
//    private SynchronousQueue<Boolean> MACCharacteristicOnRead;

    final static private int REMOTE_ANGLE_CHARACTERISTIC_LENGTH = 2;
    final static private int REMOTE_SPEED_CHARACTERISTIC_LENGTH = 1;
    final static private int REMOTE_POWER_CHARACTERISTIC_LENGTH = 1;
//    final static private int REMOTE_MAC_CHARACTERISTIC_LENGTH = 6;

    private static final double MAX_BATTERY_VOLTAGE = 8.4;
    private static final double MIN_BATTERY_VOLTAGE = 7;

    private static final long SCAN_PERIOD = 10000;
    private static final int PWM_FLOOR = 610;
//    private final static int TURN_INTERVAL = 20;

    private static int mSpeed = 10;
    private static int mAngle = 900;
    private static boolean mPowerState = false;
    private static double BatteryIndicator = 0.0;

    private ExecutorService operationQueue;
    private Context mContext;


    final private String TAG = "BtServo";

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.d(TAG, "find device " + device.getName());
                    mDeviceList.add(device);
                    if (mMACList == null || device.getAddress().equals(mMACList.get(0))) {
                        mBluetoothDevice = device;
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mGattCallback);
                    }
                }
            };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            android.os.Process.setThreadPriority(-20);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                mConnectState = ConnectState.disconnected;
                for (StateCallback cbk : StateCallbacks) {
                    cbk.onStateChange(mConnectState);
                }
            }
        }

        @Override 
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                DockService = mBluetoothGatt.getService(mServiceUuid);
                Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED");


                AngleCharacteristic = DockService.getCharacteristic(AngleCharacteristicUUID);
                SpeedCharacteristic = DockService.getCharacteristic(SpeedCharacteristicUUID);
                PowerCharacteristic = DockService.getCharacteristic(PowerCharacteristicUUID);
                BatteryCharacteristic = DockService.getCharacteristic(BatteryCharacteristicUUID);
//                MACCharacteristic = DockService.getCharacteristic(MACCharacteristicUUID);

                Log.d(TAG, "ACTION_GATT_CHARACTERISTIC_DISCOVERED");

                isInit = true;
                mConnectState = ConnectState.connected;
//                try {
//                    InitializeDone.put(true);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                for (StateCallback cbk : StateCallbacks) {
                    cbk.onStateChange(mConnectState);
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                mConnectState = ConnectState.disconnected;
                for (StateCallback cbk : StateCallbacks) {
                    cbk.onStateChange(mConnectState);
                }
            }
        }

        @SuppressLint("NewApi")
		@Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
        	String uuid=characteristic.getUuid().toString();
        	if(uuid.equals(ANGLE_CHARACTERISTIC_UUID))
        	{
        		  if (status == BluetoothGatt.GATT_SUCCESS) {
                      try {
                          AngleCharacteristicOnRead.put(true);
                      } catch (InterruptedException e) {
                          e.printStackTrace();
                      }
                  } else {
                      try {
                          AngleCharacteristicOnRead.put(false);
                      } catch (InterruptedException e) {
                          e.printStackTrace();
                      }
                  }
        	}
        	else if(uuid.equals(SPEED_CHARACTERISTIC_UUID))
        	{
        		 if (status == BluetoothGatt.GATT_SUCCESS) {
                     try {
                         SpeedCharacteristicOnRead.put(true);
                     } catch (InterruptedException e) {
                         e.printStackTrace();
                     }
                 } else {
                     try {
                         SpeedCharacteristicOnRead.put(false);
                     } catch (InterruptedException e) {
                         e.printStackTrace();
                     }
                 }
        	}
        	else if(uuid.equals(POWER_CHARACTERISTIC_UUID))
        	{
        		  if (status == BluetoothGatt.GATT_SUCCESS) {
                      try {
                          PowerCharacteristicOnRead.put(true);
                      } catch (InterruptedException e) {
                          e.printStackTrace();
                      }
                  } else {
                      try {
                          PowerCharacteristicOnRead.put(false);
                      } catch (InterruptedException e) {
                          e.printStackTrace();
                      }
                  }
        	}
//            else if(uuid.equals(MAC_CHARACTERISTIC_UUID))
//            {
//                if (status == BluetoothGatt.GATT_SUCCESS) {
//                    try {
//                        MACCharacteristicOnRead.put(true);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    try {
//                        MACCharacteristicOnRead.put(false);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }

            Log.d(TAG, "onCharacteristicRead: " + status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            String uuid = characteristic.getUuid().toString();
            if (uuid.equals(BATTERY_CHARACTERISTIC_UUID)) {
                byte[] changeBytes;
                changeBytes = BatteryCharacteristic.getValue();

                int t = (((changeBytes[1] << 8) & 0xFF00) | (changeBytes[0] & 0xFF));
                t = t >> 4;
                double ind =  (double) t * 9.9 / 2047;
                ind = ((ind - MIN_BATTERY_VOLTAGE) / (MAX_BATTERY_VOLTAGE - MIN_BATTERY_VOLTAGE)) * 100;
                if (ind < 0) {
                    ind = 0.0;
                }
                if (ind > 100) {
                    ind = 100.0;
                }
                BatteryIndicator = ind;

                for (StateCallback cbk : StateCallbacks) {
                    cbk.didUpdateBattery((int) BatteryIndicator);
                }
            }
//            Log.d(TAG, "onCharacteristicChanged");
        }

        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
        	String uuid=characteristic.getUuid().toString();
            //for JDK 1.6
        	if(uuid.equals(ANGLE_CHARACTERISTIC_UUID))
        	{
        	    if (status == BluetoothGatt.GATT_SUCCESS) {
                    try {
                        AngleCharacteristicOnWrite.put(true);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        AngleCharacteristicOnWrite.put(false);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        	}
        	else if(uuid.equals(SPEED_CHARACTERISTIC_UUID))
        	{
        		  if (status == BluetoothGatt.GATT_SUCCESS) {
                      try {
                          SpeedCharacteristicOnWrite.put(true);
                      } catch (InterruptedException e) {
                          e.printStackTrace();
                      }
                  } else {
                      try {
                          SpeedCharacteristicOnWrite.put(false);
                      } catch (InterruptedException e) {
                          e.printStackTrace();
                      }
                  }
        	}
        	else if(uuid.equals(POWER_CHARACTERISTIC_UUID))
        	{
        		if (status == BluetoothGatt.GATT_SUCCESS) {
                    try {
                        PowerCharacteristicOnWrite.put(true);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        PowerCharacteristicOnWrite.put(false);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        	}
            Log.d(TAG, "onCharacteristicWrite: " + status);
        }


    };

    //belows are internal class and interface.
    public enum ConnectState {
        disconnected,
        connecting,
        connected
    }

    public interface StateCallback {
        void onStateChange(final ConnectState state);
        void onDidReadAngle(final float angle);
        void onDidReadSpeed(final int speed);
        void onDidReadPowerState(final boolean onoff);
        void didUpdateBattery(final int indcater);
    }

    //belows are constructor and methods.
    protected BtServo(final BluetoothAdapter mBluetoothAdapter, Context mContext) throws Exception {
        if (mBluetoothAdapter == null) {
            throw new Exception("none BluetoothAdapter");
        }
        this.mBluetoothAdapter = mBluetoothAdapter;
        this.mContext = mContext;
        this.mHandler = new Handler();
        this.StateCallbacks = new ArrayList<>(2);
        this.isInit = false;
        this.mDeviceList = new ArrayList<>();
//        this.InitializeDone = new SynchronousQueue<>();
        this.AngleCharacteristicOnWrite = new SynchronousQueue<>();
        this.SpeedCharacteristicOnWrite = new SynchronousQueue<>();
        this.PowerCharacteristicOnWrite = new SynchronousQueue<>();

        this.AngleCharacteristicOnRead = new SynchronousQueue<>();
        this.SpeedCharacteristicOnRead = new SynchronousQueue<>();
        this.PowerCharacteristicOnRead = new SynchronousQueue<>();
//        this.MACCharacteristicOnRead = new SynchronousQueue<>();

        operationQueue = Executors.newSingleThreadExecutor();

    }

    public void initializeConnection() {
//        this.mStateCallback = stateCallback;

        if (mConnectState == ConnectState.disconnected) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    if (ConnectState.connecting == mConnectState) {
                        mConnectState = ConnectState.disconnected;
                        for (StateCallback cbk : StateCallbacks) {
                            cbk.onStateChange(mConnectState);
                        }
                    }
                }
            }, SCAN_PERIOD);
            mConnectState = ConnectState.connecting;
            for (StateCallback cbk : StateCallbacks) {
                cbk.onStateChange(mConnectState);
            }
            UUID uuids[] = new UUID[1];
            uuids[0] = mServiceUuid;
            mBluetoothAdapter.startLeScan(uuids, mLeScanCallback);
        }
    }

    public void initializeConnection(final ArrayList<String> macList) {
        mMACList = macList;

        if (mConnectState == ConnectState.disconnected) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    if (null == mBluetoothDevice || ConnectState.connecting == mConnectState) {
                        for (String tempMAC : macList) {
                            for (BluetoothDevice tempBluetoothDevice : mDeviceList) {
                                if (tempMAC.equals(tempBluetoothDevice.getAddress())) {
                                    mBluetoothDevice = tempBluetoothDevice;
                                    mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mGattCallback);
                                    break;
                                }
                            }
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (ConnectState.connecting == mConnectState) {
                        mConnectState = ConnectState.disconnected;
                        for (StateCallback cbk : StateCallbacks) {
                            cbk.onStateChange(mConnectState);
                        }
                    }
                }
            }, SCAN_PERIOD);
            mConnectState = ConnectState.connecting;
            for (StateCallback cbk : StateCallbacks) {
                cbk.onStateChange(mConnectState);
            }
            UUID uuids[] = new UUID[1];
            uuids[0] = mServiceUuid;
            mBluetoothAdapter.startLeScan(uuids, mLeScanCallback);
        }
    }

    public boolean reConnect() throws Exception {
        if (mBluetoothGatt == null) {
            throw new Exception("none BluetoothDevice");
        }
        this.mConnectState = ConnectState.connecting;
        for (StateCallback cbk : StateCallbacks) {
            cbk.onStateChange(mConnectState);
        }

        if (mBluetoothGatt.connect()) {
            return true;
        } else {
            mConnectState = ConnectState.disconnected;
            for (StateCallback cbk : StateCallbacks) {
                cbk.onStateChange(mConnectState);
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
        mConnectState = ConnectState.connecting;
        for (StateCallback cbk : StateCallbacks) {
            cbk.didUpdateBattery(0);
            cbk.onStateChange(mConnectState);
        }

        mBluetoothGatt.disconnect();

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

    public ConnectState getConnectionState()
    {
        return this.mConnectState;
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
        return b2;
    }

    public void writeAngle(final float angle) {
        if (angle > 180 || angle < 0) {
            return;
        }

        final SynchronousQueue<Boolean> didRead = new SynchronousQueue<>();
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {

                int IntAngle = (int) (angle * 10) + PWM_FLOOR;

                byte[] WriteBytes = new byte[REMOTE_ANGLE_CHARACTERISTIC_LENGTH];

                System.arraycopy(u162byte(Integer.toHexString(IntAngle).getBytes()), 0, WriteBytes, 0, 2);
                if (mBluetoothAdapter != null && mBluetoothGatt != null && ConnectState.connected == mConnectState && mPowerState == true) {

                    mAngle = (int) (angle * 10);

                    AngleCharacteristic.setValue(WriteBytes[0],
                            BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    AngleCharacteristic.setValue(WriteBytes);
                    long ts = System.currentTimeMillis();
                    mBluetoothGatt.writeCharacteristic(AngleCharacteristic);
                    try {
                        AngleCharacteristicOnWrite.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    long te = System.currentTimeMillis();
                    try {
                        didRead.put(true);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Log.d(TAG, "writeAngle time: " + (te - ts));
                } else {
                    Log.w(TAG, "BluetoothAdapter not initialized");
                }
            }
        });

        try {
            didRead.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public float readAngle() {
        float angle;

        final SynchronousQueue<Boolean> didRead = new SynchronousQueue<>();

        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                byte[] WriteBytes;

                if (mBluetoothAdapter != null && mBluetoothGatt != null && ConnectState.connected == mConnectState) {
                    long ts = System.currentTimeMillis();
                    mBluetoothGatt.readCharacteristic(AngleCharacteristic);

                    try {
                        AngleCharacteristicOnRead.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    long te = System.currentTimeMillis();

                    WriteBytes = AngleCharacteristic.getValue();

                    mAngle = (((WriteBytes[0] << 8) & 0xFF00) | (WriteBytes[1] & 0xFF)) - PWM_FLOOR;

                    try {
                        didRead.put(true);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    for (StateCallback cbk : StateCallbacks) {
                        cbk.onDidReadAngle(mAngle / 10);
                    }

                    Log.d(TAG, "readAngle time: " + (te - ts));

                } else {
                    Log.w(TAG, "BluetoothAdapter not initialized");
                }
            }
        });

        try {
            didRead.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        angle = (float) mAngle/10;
        return angle;
    }

    public void writeSpeed(final int step) {

        final SynchronousQueue<Boolean> didRead = new SynchronousQueue<>();

        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                byte[] WriteBytes = new byte[REMOTE_SPEED_CHARACTERISTIC_LENGTH];


                if (mBluetoothAdapter != null && mBluetoothGatt != null && ConnectState.connected == mConnectState) {

                    mSpeed = step;
                    if (mSpeed < 0) {
                        mSpeed = 0;
                    } else if (mSpeed > 100){
                        mSpeed = 100;
                    }
                    WriteBytes[0] = (byte) mSpeed;

                    SpeedCharacteristic.setValue(WriteBytes[0],
                            BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    SpeedCharacteristic.setValue(WriteBytes);
                    long ts = System.currentTimeMillis();
                    mBluetoothGatt.writeCharacteristic(SpeedCharacteristic);
                    try {
                        SpeedCharacteristicOnWrite.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    long te = System.currentTimeMillis();
                    try {
                        didRead.put(true);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "writeSpeed time: " + (te - ts));
                } else {
                    Log.w(TAG, "BluetoothAdapter not initialized1");
                }
            }
        });

        try {
            didRead.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int readSpeed() {

        final SynchronousQueue<Boolean> didRead = new SynchronousQueue<>();

        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                byte[] WriteBytes;
                if (mBluetoothAdapter != null && mBluetoothGatt != null && ConnectState.connected == mConnectState) {
                    long ts = System.currentTimeMillis();
                    mBluetoothGatt.readCharacteristic(SpeedCharacteristic);
                    try {
                        SpeedCharacteristicOnRead.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    long te = System.currentTimeMillis();
                    WriteBytes = SpeedCharacteristic.getValue();

                    mSpeed = WriteBytes[0] & 0xFF;

                    try {
                        didRead.put(true);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    for (StateCallback cbk : StateCallbacks) {
                        cbk.onDidReadSpeed(mSpeed);
                    }
                    Log.d(TAG, "readSpeed time: " + (te - ts));
                } else {
                    Log.w(TAG, "BluetoothAdapter not initialized3");
                }
            }
        });

        try {
            didRead.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return mSpeed;
    }


    public void setPowerOn(final boolean onoff) {
        final SynchronousQueue<Boolean> didRead = new SynchronousQueue<>();

        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                byte[] WriteBytes = new byte[REMOTE_POWER_CHARACTERISTIC_LENGTH];
                if (onoff) {
                    WriteBytes[0] = (byte) 0x01;
                } else {
                    WriteBytes[0] = (byte) 0x00;
                }
                if (mBluetoothAdapter != null && mBluetoothGatt != null && ConnectState.connected == mConnectState) {
                    
                    mPowerState = onoff;

                    PowerCharacteristic.setValue(WriteBytes[0],
                            BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    PowerCharacteristic.setValue(WriteBytes);
                    long ts = System.currentTimeMillis();
                    mBluetoothGatt.writeCharacteristic(PowerCharacteristic);
                    try {
                        PowerCharacteristicOnWrite.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    long te = System.currentTimeMillis();
                    try {
                        didRead.put(true);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "setPowerOn time: " + (te - ts));
                } else {
                    Log.w(TAG, "BluetoothAdapter not initialized4");
                }
            }
        });
        try {
            didRead.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean readPowerState() {
        final SynchronousQueue<Boolean> didRead = new SynchronousQueue<>();
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                byte[] WriteBytes;
                if (mBluetoothAdapter != null && mBluetoothGatt != null && ConnectState.connected == mConnectState) {
                    long ts = System.currentTimeMillis();
                    mBluetoothGatt.readCharacteristic(PowerCharacteristic);
                    try {
                        PowerCharacteristicOnRead.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    long te = System.currentTimeMillis();
                    WriteBytes = PowerCharacteristic.getValue();

                    if (0 == WriteBytes[0]) {
                        mPowerState = false;
                    } else {
                        mPowerState = true;
                    }

                    try {
                        didRead.put(true);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    for (StateCallback cbk : StateCallbacks) {
                        cbk.onDidReadPowerState(mPowerState);
                    }
                    Log.d(TAG, "readPowerState time: " + (te - ts));
                } else {
                    Log.w(TAG, "BluetoothAdapter not initialized6");
                }
            }
        });

        try {
            didRead.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return mPowerState;
    }

    public boolean setBatteryUpdate(boolean enable) {
        if (this.mConnectState == ConnectState.connected) {
            mBluetoothGatt.setCharacteristicNotification(BatteryCharacteristic, enable);

            BluetoothGattDescriptor descriptor = BatteryCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            if (true == enable) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }

            mBluetoothGatt.writeDescriptor(descriptor);
            return true;
        } else {
            return false;
        }
    }


    public int getSpeed() {
        return mSpeed;
    }

    public float getAngle() {
        return mAngle / 10;
    }

    public boolean getPowerState() {
        return mPowerState;
    }

    public double getBatteryIndcator() {
        return BatteryIndicator;
    }

    public String getMAC() {
        if (null != mBluetoothDevice) {
            return mBluetoothDevice.getAddress();
        } else {
            return null;
        }
    }
}
