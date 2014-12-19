/**************************************************************************************************
  Filename:       DeviceView.java
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

import java.util.List;
import java.util.UUID;

import ti.android.ble.common.GattInfo;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

// Fragment for Device View
public class DeviceView extends Fragment {
	private static final String TAG = "DeviceView";

	// GUI
	private TextView mStatus;
	private ProgressBar mProgressBar;
	private ListView mServiceListView;
	private ServiceAdapter mServiceAdapter = null;

	// House-keeping
	private boolean mBusy = false;
	private DeviceActivity mActivity = null;
	private Context mContext;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	    Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");

		// The last two arguments ensure LayoutParams are inflated properly.
		View view = inflater.inflate(R.layout.fragment_device, container, false);

		mActivity = (DeviceActivity) getActivity();
		mContext = mActivity.getApplicationContext();

		// List of services
		mServiceListView = ((ListView) view.findViewById(R.id.service_list));

		// Progress bar
		mProgressBar = (ProgressBar) view.findViewById(R.id.pb_busy);
		mProgressBar.setVisibility(View.INVISIBLE);

		// Status text
		mStatus = (TextView) view.findViewById(R.id.device_status);

		// Notify activity
		mActivity.onDeviceViewReady(view);

		return view;
	}

	ListView getServiceListView() {
		return mServiceListView;
	}

	private void setListAdapter() {
		if (mServiceAdapter == null) {
			mServiceAdapter = new ServiceAdapter(mActivity,
			    mActivity.getServiceList());
		}
		mServiceListView.setAdapter(mServiceAdapter);
	}

	void setStatus(String txt) {
		mStatus.setText(txt);
		mStatus.setTextAppearance(mContext, R.style.statusStyle_Success);
	}

	void setError(String txt) {
		mStatus.setText(txt);
		mStatus.setTextAppearance(mContext, R.style.statusStyle_Failure);
	}

	void setBusy(boolean f) {
		mBusy = f;
		if (mServiceListView == null)
			return;

		mServiceListView.setEnabled(!f);
		if (mProgressBar != null) {
			if (f) {
				mProgressBar.setVisibility(View.VISIBLE);
				mStatus.setTextAppearance(mContext, R.style.statusStyle_Busy);
			} else {
				mProgressBar.setVisibility(View.GONE);
				mStatus.setTextAppearance(mContext, R.style.statusStyle_Success);
			}
		}
	}

	void notifyDataSetChanged() {
		setListAdapter();
		mServiceAdapter.notifyDataSetChanged();
	}

	//
	// CLASS ServiceAdapter: handle service list
	//
	class ServiceAdapter extends BaseAdapter {
		Context mContext;
		List<BluetoothGattService> mServices;
		LayoutInflater mInflater;

		public ServiceAdapter(Context context, List<BluetoothGattService> list) {
			mContext = context;
			mInflater = LayoutInflater.from(context);
			mServices = list;
		}

		public int getCount() {
			return mServices.size();
		}

		public Object getItem(int pos) {
			return mServices.get(pos);
		}

		public long getItemId(int pos) {
			return pos;
		}

		public View getView(int pos, View convertView, ViewGroup parent) {
			ViewGroup vg;

			if (convertView != null) {
				vg = (ViewGroup) convertView;
			} else {
				vg = (ViewGroup) mInflater.inflate(R.layout.element_service, null);
			}

			// Grab service object
			BluetoothGattService service = mServices.get(pos);
			UUID u = service.getUuid();

			int nChr = service.getCharacteristics().size();

			// Handle information
			String infoStr = String.format("%d chrs.", nChr);

			// Show name, UUID and handle information
			TextView nameView = (TextView) vg.findViewById(R.id.name);
			nameView.setText(GattInfo.uuidToName(u));
			((TextView) vg.findViewById(R.id.uuid)).setText(GattInfo.uuidToString(u));
			((TextView) vg.findViewById(R.id.info)).setText(infoStr);

			if (mBusy) {
				nameView.setTextAppearance(mContext, R.style.nameStyle_inactive);
			} else {
				nameView.setTextAppearance(mContext, R.style.nameStyle);
			}

			return vg;
		}
	}

}
