/**************************************************************************************************
  Filename:       DeviceActivity.java
  Revised:        $Date: 2014-01-07 16:17:34 +0100 (ti, 07 jan 2014) $
  Revision:       $Revision: 28784 $

  Copyright 2013 Texas Instruments Incorporated. All rights reserved.
 
  IMPORTANT: Your use of this Software is limited to those specific rights
  granted under the terms of a software license agreement between the user
  who downloaded the software, his/her employer (which must be your employer)
  and Texas Instruments Incorporated (the "License").  You may not use this
  Software unless you agree to abide by the terms of the License. 
  The License limits your use, and you acknowledge, that the Software may not be 
  modified, copied or distributed unless used solely and exclusively in conjunction 
  with a Texas Instruments Bluetooth® device. Other than for the foregoing purpose, 
  you may not use, reproduce, copy, prepare derivative works of, modify, distribute, 
  perform, display or sell this Software and/or its documentation for any purpose.
 
  YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
  PROVIDED “AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
  INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
  NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
  TEXAS INSTRUMENTS OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT,
  NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER
  LEGAL EQUITABLE THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES
  INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE
  OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT
  OF SUBSTITUTE GOODS, TECHNOLOGY, SERVICES, OR ANY CLAIMS BY THIRD PARTIES
  (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 
  Should you have any questions regarding your right to use this Software,
  contact Texas Instruments Incorporated at www.TI.com

 **************************************************************************************************/
package ti.android.ble.devicemonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ti.android.ble.common.BluetoothLeService;
import ti.android.ble.common.GattInfo;
import ti.android.ble.common.HelpView;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceActivity extends ViewPagerActivity {
	// Log
	private static String TAG = "DeviceActivity";

	// Activity
	public final static String EXTRA_DEVICE = "EXTRA_DEVICE";
	private static final int SERVICE_ACT_REQ = 0;
	private static final int FWUPDATE_ACT_REQ = 1;

	private static DeviceActivity mInstance = null;
	private DeviceView mDeviceView = null;

	// BLE
	private BluetoothLeService mBtLeService = null;
	private BluetoothDevice mBluetoothDevice = null;
	private BluetoothGatt mBtGatt = null;
	private List<BluetoothGattService> mServiceList = null;
	private BluetoothGattService mActiveService = null;
	private BluetoothGattService mOadService = null;
	private BluetoothGattService mConnControlService = null;

	// Housekeeping
	private boolean mBusy = false;
	private boolean mServicesRdy = false;

	public DeviceActivity() {
		mInstance = this;
		mResourceFragmentPager = R.layout.fragment_pager;
		mResourceIdPager = R.id.pager;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();

		// GUI
		mDeviceView = new DeviceView();
		mSectionsPagerAdapter.addSection(mDeviceView, "Services");
		mSectionsPagerAdapter.addSection(new HelpView("help_device.html",
		    R.layout.fragment_help, R.id.webpage), "Help");

		// BLE
		mBtLeService = BluetoothLeService.getInstance();
		mBluetoothDevice = intent.getParcelableExtra(EXTRA_DEVICE);
		mServiceList = new ArrayList<BluetoothGattService>();

		// GATT database
		Resources res = getResources();
		XmlResourceParser xpp = res.getXml(R.xml.gatt_uuid);
		new GattInfo(xpp);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		finishActivity(SERVICE_ACT_REQ);
		finishActivity(FWUPDATE_ACT_REQ);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.device_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.opt_fwupdate:
			startOadActivity();
			break;
		case R.id.opt_about:
			openAboutDialog();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
		unregisterReceiver(mGattUpdateReceiver);
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		return intentFilter;
	}

	void onDeviceViewReady(View view) {
		Log.d(TAG, "Gatt view ready");

		// Set title bar to device name
		setTitle(mBluetoothDevice.getName());

		// Display device address
		TextView addrView = (TextView) view.findViewById(R.id.device_address);
		addrView.setText("Device address: " + mBluetoothDevice);

		// GATT GUI
		mDeviceView.getServiceListView().setOnItemClickListener(
		    mDeviceClickListener);

		// Create GATT object
		mBtGatt = BluetoothLeService.getBtGatt();

		// Start service discovery
		if (!mServicesRdy && mBtGatt != null) {
			if (mBtLeService.getNumServices() == 0)
				discoverServices();
			else
				displayServices();
		}
	}

	//
	// Application implementation
	//
	BluetoothGattService getOadService() {
		return mOadService;
	}

	BluetoothGattService getConnControlService() {
		return mConnControlService;
	}

	private void startOadActivity() {
		if (mOadService != null && mConnControlService != null) {
			Toast.makeText(this, "OAD service available", Toast.LENGTH_LONG).show();
			Intent i = new Intent(this, FwUpdateActivity.class);
			startActivityForResult(i, FWUPDATE_ACT_REQ);
		} else {
			Toast.makeText(this, "OAD service not available on this device",
			    Toast.LENGTH_LONG).show();
		}
	}

	private void checkOad() {
		// Check if OAD service is present
		mOadService = null;
		mConnControlService = null;

		for (int i = 0; i < mServiceList.size()
		    && (mOadService == null || mConnControlService == null); i++) {
			BluetoothGattService srv = mServiceList.get(i);
			Log.d(TAG, srv.getUuid().toString());
			if (srv.getUuid().equals(GattInfo.OAD_SERVICE_UUID)) {
				mOadService = srv;
			}
			if (srv.getUuid().equals(GattInfo.CC_SERVICE_UUID)) {
				mConnControlService = srv;
			}
		}
	}

	public static DeviceActivity getInstance() {
		return mInstance;
	}

	public List<BluetoothGattService> getServiceList() {
		return mServiceList;
	}

	public BluetoothGattService getActiveService() {
		return mActiveService;
	}

	private void discoverServices() {
		if (mBtGatt.discoverServices()) {
			Log.i(TAG, "START SERVICE DISCOVERY");
			mServiceList.clear();
			mDeviceView.setStatus("Service discovery ...");
			setBusy(true);
		} else {
			mDeviceView.setError("Service discovery failed");
		}
	}

	private void displayServices() {
		setBusy(false);
		mServicesRdy = true;

		try {
			mServiceList = mBtLeService.getSupportedGattServices();
		} catch (Exception e) {
			e.printStackTrace();
			mServicesRdy = false;
		}

		// Characteristics descriptor readout done
		if (mServicesRdy) {
			int n = mServiceList.size();
			mDeviceView.setStatus(n + " services");
			mDeviceView.notifyDataSetChanged();
		} else {
			setError("Failed to read services");
		}
	}

	private void startServiceActivity(BluetoothGattService srv) {
		mActiveService = srv;

		// Start the service activity
		UUID uuid = srv.getUuid();
		Intent i = new Intent(this, ServiceActivity.class);
		i.putExtra(ServiceActivity.EXTRA_NAME, GattInfo.uuidToName(uuid));
		startActivityForResult(i, SERVICE_ACT_REQ);
	}

	private void setBusy(boolean f) {
		mBusy = f;
		mDeviceView.setBusy(mBusy);
	}

	private void setError(String txt) {
		setBusy(false);
		mDeviceView.setError(txt);
	}

	// Listener for clicks in device list
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position,
		    long id) {
			if (!mBusy) {
				BluetoothGattService srv;
				// Basic service info
				srv = mServiceList.get(position);
				startServiceActivity(srv);
			}
		}
	};

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			Log.d(TAG, "action = " + action);

			if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS,
				    BluetoothGatt.GATT_SUCCESS);
				if (status == BluetoothGatt.GATT_SUCCESS) {
					displayServices();
					checkOad();
				} else {
					setError("Service discovery failed");
				}
			}
		}
	};

}
