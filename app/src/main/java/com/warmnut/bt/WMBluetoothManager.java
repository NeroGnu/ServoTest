package com.warmnut.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

public class WMBluetoothManager {

	private String TAG = "WMBluetoothManager";
	public BtServo btServo;
	static private WMBluetoothManager manager;
	public BluetoothAdapter mBluetoothAdapter;

	private WMBluetoothManager (Context context) throws Exception{
		if (context == null)
			return;

		if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
//			finish();
			throw new Exception("BLE not support!");
		}
		// Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
		// BluetoothAdapter through BluetoothManager.
		final BluetoothManager bluetoothManager =
				(BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			throw new Exception("bluetooth_not_supported");
//			Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
//			finish();
//			return;
		}


		btServo = new BtServo(mBluetoothAdapter, context);

	}
	
	static public WMBluetoothManager getManager(Context context) throws Exception{
		if(manager != null)
			return manager;
		synchronized (WMBluetoothManager.class) {
			if(manager == null)
				manager = new WMBluetoothManager(context);
		}
		return manager;
	}

	static public WMBluetoothManager getManager() {
		return manager;
	}
	
}
