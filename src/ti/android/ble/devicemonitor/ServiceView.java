/**************************************************************************************************
  Filename:       ServiceView.java
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

import ti.android.ble.common.BluetoothLeService;
import ti.android.ble.common.GattInfo;
import ti.android.util.Conversion;
import ti.android.util.CustomKeyboard;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class ServiceView extends Fragment implements OnClickListener {
	// Log
	private static String TAG = "ServiceView";

	// Housekeeping
	private ServiceActivity mActivity = null;
	private Context mContext;

	// GUI
	private TextView mStatus = null;
	private EditText mData;
	private CharAdapter mCharAdapter;
	private ListView mCharListView;
	private Button mBtnWrite;
	private Button mBtnRead;
	private CheckBox mBtnNotify;
	private CustomKeyboard mKeyboard;
	private int mSelectedItem = 0;

	// GATT
	private static BluetoothGatt mBtGatt;
	private static BluetoothLeService mGattService;
	private List<BluetoothGattCharacteristic> mCharList;
	private BluetoothGattCharacteristic mChar;
	private int mProperty = 0;
	private boolean mNotifyEnabled = false;

	public ServiceView() {
		Log.i(TAG, "construct");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	    Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");

		// The last two arguments ensure LayoutParams are inflated properly.
		View v = inflater.inflate(R.layout.fragment_service, container, false);
		mActivity = (ServiceActivity) getActivity();
		mContext = mActivity.getApplicationContext();

		// Get GATT object and active service object
		mBtGatt = BluetoothLeService.getBtGatt();
		mGattService = BluetoothLeService.getInstance();
		mChar = null;

		// GUI initialization
		mData = (EditText) v.findViewById(R.id.txt_data);
		mData.setEnabled(false);
		mData.setHint("");

		mStatus = (TextView) v.findViewById(R.id.txt_status);

		mBtnRead = (Button) v.findViewById(R.id.btn_read);
		mBtnRead.setOnClickListener(this);

		mBtnWrite = (Button) v.findViewById(R.id.btn_write);
		mBtnWrite.setOnClickListener(this);

		mBtnNotify = (CheckBox) v.findViewById(R.id.btn_notify);
		mBtnNotify.setOnClickListener(this);

		// Characteristics list
		mCharList = (List<BluetoothGattCharacteristic>) ((ServiceActivity) mActivity)
		    .getCharList();
		mCharListView = (ListView) v.findViewById(R.id.char_list);
		mCharListView.setOnItemClickListener(mCharClickListener);
		mCharAdapter = new CharAdapter(mActivity, mCharList);
		mCharListView.setAdapter(mCharAdapter);

		// Hex keyboard
		mKeyboard = new CustomKeyboard(mActivity, v, R.id.keyboardview,
		    R.xml.hexkbd);
		mKeyboard.registerEditText(R.id.txt_data);

		// Notify activity that the view has been expanded
		mActivity.onServiceViewReady(v);
		return v;
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");

		mChar = null;
		mStatus = null;
		mCharAdapter = null;

		super.onDestroy();
	}

	public boolean onBackPressed() {
		boolean visible;

		visible = mKeyboard.isVisible();
		if (visible) {
			mKeyboard.hide();
		}

		return visible;
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_read:
			onBtnRead(v);
			break;

		case R.id.btn_write:
			onBtnWrite(v);
			break;

		case R.id.btn_notify:
			onBtnNotify(v);
			break;

		default:
			break;
		}
	}

	public void onBtnRead(View view) {
		mBtGatt.readCharacteristic(mChar);
	}

	public void onBtnNotify(View view) {
		mNotifyEnabled = mBtnNotify.isChecked();
		mGattService.setCharacteristicNotification(mChar, mNotifyEnabled);
	}

	public void onBtnWrite(View view) {

		if (mKeyboard.isVisible()) {
			mKeyboard.hide();
			String str;
			int len;

			str = mData.getText().toString();
			len = str.length();

			switch (len) {
			case 0:
				break;
			case 1:
			case 2:
				byte[] data = new byte[1];

				data[0] = (byte) Integer.parseInt(str, 16);
				mChar.setValue(data);
				mBtGatt.writeCharacteristic(mChar);
				break;
			case 3:
			case 4:
				data = new byte[2];
				short v;

				v = (short) Integer.parseInt(str, 16);
				data[0] = Conversion.loUint16(v);
				data[1] = Conversion.hiUint16(v);
				mChar.setValue(data);
				mBtGatt.writeCharacteristic(mChar);
				break;
			default:
				// Multibyte input
				byte[] value = new byte[len / 2];
				Log.d(TAG, str + " " + len);
				for (int i = 0; i < (len / 2); i++) {
					int o = i * 2;
					String t = str.substring(o, o + 2);
					Log.d(TAG, "Str: " + t);
					value[i] = (byte) Integer.parseInt(t, 16);
					Log.d(TAG, "Value: " + value[i]);
				}
				mChar.setValue(value);
				mBtGatt.writeCharacteristic(mChar);
				break;
			}
		} else {
			mKeyboard.show(null);
			mKeyboard.setFocus();
		}
	}

	void notifyDataSetChanged() {
		setAdapter();
		mCharAdapter.notifyDataSetChanged();
	}

	void updateView() {
		mCharList.clear();
		mCharAdapter.notifyDataSetChanged();
	}

	void selectCurrentItem() {
		setItem(mSelectedItem);
	}

	void setItem(int pos) {
		boolean fHasRead;
		boolean fHasNotification;
		boolean fHasWrite;
		BluetoothGattDescriptor clientConfig;

		mChar = mCharList.get(pos);
		mProperty = mChar.getProperties();

		fHasRead = (mProperty & BluetoothGattCharacteristic.PROPERTY_READ) > 0;
		fHasNotification = (mProperty & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0;
		fHasWrite = (mProperty & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;

		// Activate input widgets according to char. property
		mBtnRead.setEnabled(fHasRead);
		mBtnWrite.setEnabled(fHasWrite);
		mData.setEnabled(fHasWrite);
		mBtnNotify.setEnabled(fHasNotification);

		if (fHasNotification) {
			mBtnNotify.setTextAppearance(mContext, R.style.checkboxStyle);
			mBtnNotify.setChecked(mGattService.isNotificationEnabled(mChar));
		} else
			mBtnNotify.setTextAppearance(mContext, R.style.checkboxStyle_Disabled);

		if (fHasWrite) {
			mData.setHint(R.string.write_hint);
		} else {
			mData.setHint("");
		}

		clientConfig = mChar.getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
		if (clientConfig != null) {
			int perm = clientConfig.getPermissions();
			Log.d(TAG, "perm= " + perm);
		}
		setStatus(GattInfo.uuidToName(mChar.getUuid()));

		// Read the characteristic (if applicable)
		if (fHasRead) {
			mBtGatt.readCharacteristic(mChar);
		}

		// Make sure selection is highlighted
		mCharAdapter.setSelectedPosition(pos);
		mActivity.onSelectionUpdate(mChar);
	}

	void setStatus(String txt) {
		if (mStatus != null) {
			mStatus.setText(timeNowStr() + txt);
			setStatusStyle(R.style.statusStyle_Busy);
		}
	}

	void setError(String txt) {
		if (mStatus != null) {
			mStatus.setText(timeNowStr() + txt);
			setStatusStyle(R.style.statusStyle_Failure);
		}
	}

	void displayData(byte[] value, byte len) {
		String txt = new String(value, 0, len - 1);
		boolean isPrint = Conversion.isAsciiPrintable(txt);

		if (!isPrint || len <= 4)
			txt = Conversion.BytetohexString(value, len);
		mData.setText(txt);
	}

	void displayNotif(byte[] value, byte len) {
		if (mChar != null) {
			if ((mProperty & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
				// Only show data if selected characteristic has notifications
				displayData(value, len);
			}
		}
	}

	private void setAdapter() {
		mCharList = (List<BluetoothGattCharacteristic>) (mActivity).getCharList();
		mCharAdapter = new CharAdapter(mActivity, mCharList);
		mCharListView.setAdapter(mCharAdapter);
	}

	private String timeNowStr() {
		Time now = new Time(Time.getCurrentTimezone());
		now.setToNow();
		return now.format("%k:%M:%S") + ": ";
	}

	private void setStatusStyle(int style) {
		if (mStatus != null)
			mStatus.setTextAppearance(mContext, style);
	}

	private String getPropertyDescription(int prop) {
		String str = new String();

		if ((prop & BluetoothGattCharacteristic.PROPERTY_READ) > 0)
			str += "R";

		if ((prop & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0)
			str += "W";

		if ((prop & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0)
			str += "N";

		if ((prop & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0)
			str += "I";

		if ((prop & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0)
			str += "*";

		if ((prop & BluetoothGattCharacteristic.PROPERTY_BROADCAST) > 0)
			str += "B";

		if ((prop & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) > 0)
			str += "E";

		if ((prop & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) > 0)
			str += "S";

		return str;
	}

	// Listener for characteristic click
	private OnItemClickListener mCharClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {

			// Hide keyboard when selecting a characteristic
			if (mKeyboard.isVisible())
				mKeyboard.hide();

			// A characteristic item has been selected
			mSelectedItem = pos;
			setItem(pos);
		}
	};

	//
	// CLASS ServiceAdapter: handle characteristics list
	//
	class CharAdapter extends BaseAdapter {
		Context mContext;
		List<BluetoothGattCharacteristic> mChars;
		LayoutInflater mInflater;
		int mSelectedPos;

		public CharAdapter(Context context, List<BluetoothGattCharacteristic> chars) {
			mInflater = LayoutInflater.from(context);
			mContext = context;
			mChars = chars;
			mSelectedPos = 0;
		}

		public int getCount() {
			return mChars.size();
		}

		public Object getItem(int pos) {
			return mChars.get(pos);
		}

		public long getItemId(int pos) {
			return pos;
		}

		public void setSelectedPosition(int pos) {
			mSelectedPos = pos;
			notifyDataSetChanged();
		}

		public int getSelectedPosition() {
			return mSelectedPos;
		}

		public View getView(int pos, View view, ViewGroup parent) {
			ViewGroup vg;

			if (view != null) {
				vg = (ViewGroup) view;
			} else {
				vg = (ViewGroup) mInflater.inflate(R.layout.element_characteristic,
				    null);
			}

			// Grab characteristic object
			BluetoothGattCharacteristic charac = mChars.get(pos);
			int prop = charac.getProperties();
			UUID u = charac.getUuid();

			// Show name, UUID and properties
			TextView twName = (TextView) vg.findViewById(R.id.name);
			twName.setText(GattInfo.uuidToName(u));
			((TextView) vg.findViewById(R.id.uuid)).setText(GattInfo.uuidToString(u));
			((TextView) vg.findViewById(R.id.info))
			    .setText(getPropertyDescription(prop));

			// Highlight selected object
			if (pos == mSelectedPos) {
				twName.setTextAppearance(mContext, R.style.nameStyleSelected);
			} else {
				twName.setTextAppearance(mContext, R.style.nameStyle);
			}

			return vg;
		}
	}

}
