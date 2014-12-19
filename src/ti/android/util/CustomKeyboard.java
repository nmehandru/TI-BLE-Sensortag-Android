/**************************************************************************************************
  Filename:       CustomKeyboard.java
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
  
 ***********************************************************************
  CREDITS: This class is based on an example designed by Maarten Pennings.
  
  URL: http://www.fampennings.nl/maarten/android/09keyboard/index.htm

 **************************************************************************************************/
package ti.android.util;

/**
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * If you use this software in a product, an acknowledgment in the product
 * documentation would be appreciated but is not required.
 * 
 * NB! This code has been modified for use by the BLE Device Monitor
 */

import android.app.Activity;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.text.Editable;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * When an activity hosts a keyboardView, this class allows several EditText's
 * to register for it.
 * 
 * @author Maarten Pennings
 * @date 2012 December 23
 * 
 */
public class CustomKeyboard {

	/** A link to the KeyboardView that is used to render this CustomKeyboard. */
	private KeyboardView mKeyboardView;
	/** A link to the activity that hosts the {@link #mKeyboardView}. */
	private Activity mHostActivity;
	/** A link to the activity that hosts the {@link #mKeyboardView}. */
	private View mHostView;
	/** A link to the activity that hosts the {@link #mKeyboardView}. */
	private EditText mEditText;

	/** The key (code) handler. */
	private OnKeyboardActionListener mOnKeyboardActionListener = new OnKeyboardActionListener() {

		public final static int CodeDelete = -5; // Keyboard.KEYCODE_DELETE
		public final static int CodeCancel = -3; // Keyboard.KEYCODE_CANCEL
		public final static int CodePrev = 55000;
		public final static int CodeAllLeft = 55001;
		public final static int CodeLeft = 55002;
		public final static int CodeRight = 55003;
		public final static int CodeAllRight = 55004;
		public final static int CodeNext = 55005;
		public final static int CodeClear = 55006;

		public void onKey(int primaryCode, int[] keyCodes) {
			// NOTE We can say '<Key android:codes="49,50" ... >' in the xml file; all
			// codes come in keyCodes, the first in this list in primaryCode
			// Get the EditText and its Editable
			View focusCurrent = mHostActivity.getWindow().getCurrentFocus();
			if (focusCurrent == null || focusCurrent.getClass() != EditText.class)
				return;
			EditText edittext = (EditText) focusCurrent;
			Editable editable = edittext.getText();
			int start = edittext.getSelectionStart();
			if (primaryCode == CodeCancel) {
				hide();
			} else if (primaryCode == CodeDelete) {
				if (editable != null && start > 0)
					editable.delete(start - 1, start);
			} else if (primaryCode == CodeClear) {
				if (editable != null)
					editable.clear();
			} else if (primaryCode == CodeLeft) {
				if (start > 0)
					edittext.setSelection(start - 1);
			} else if (primaryCode == CodeRight) {
				if (start < edittext.length())
					edittext.setSelection(start + 1);
			} else if (primaryCode == CodeAllLeft) {
				edittext.setSelection(0);
			} else if (primaryCode == CodeAllRight) {
				edittext.setSelection(edittext.length());
			} else if (primaryCode == CodePrev) {
				View focusNew = edittext.focusSearch(View.FOCUS_BACKWARD);
				if (focusNew != null)
					focusNew.requestFocus();
			} else if (primaryCode == CodeNext) {
				View focusNew = edittext.focusSearch(View.FOCUS_FORWARD);
				if (focusNew != null)
					focusNew.requestFocus();
			} else { // insert character
				editable.insert(start, Character.toString((char) primaryCode));
			}
		}

		public void onPress(int arg0) {
		}

		public void onRelease(int primaryCode) {
		}

		public void onText(CharSequence text) {
		}

		public void swipeDown() {
		}

		public void swipeLeft() {
		}

		public void swipeRight() {
		}

		public void swipeUp() {
		}
	};

	/**
	 * Create a custom keyboard, that uses the KeyboardView (with resource id
	 * <var>viewid</var>) of the <var>host</var> activity, and load the keyboard
	 * layout from xml file <var>layoutid</var> (see {@link Keyboard} for
	 * description). Note that the <var>host</var> activity must have a
	 * <var>KeyboardView</var> in its layout (typically aligned with the bottom of
	 * the activity). Note that the keyboard layout xml file may include key codes
	 * for navigation; see the constants in this class for their values. Note that
	 * to enable EditText's to use this custom keyboard, call the
	 * {@link #registerEditText(int)}.
	 * 
	 * @param host
	 *          The hosting activity.
	 * @param viewid
	 *          The id of the KeyboardView.
	 * @param layoutid
	 *          The id of the xml file containing the keyboard layout.
	 */
	public CustomKeyboard(Activity host, View v, int viewid, int layoutid) {
		mHostActivity = host;
		mHostView = v;
		mKeyboardView = (KeyboardView) v.findViewById(viewid);
		mKeyboardView.setKeyboard(new Keyboard(mHostActivity, layoutid));
		mKeyboardView.setPreviewEnabled(false); // NOTE Do not show the preview
		                                        // balloons
		mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);
		// Hide the standard keyboard initially
		mHostActivity.getWindow().setSoftInputMode(
		    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	/** Returns whether the CustomKeyboard is visible. */
	public boolean isVisible() {
		return mKeyboardView.getVisibility() == View.VISIBLE;
	}

	/** Make the CustomKeyboard visible, and hide the system keyboard for view v. */
	public void show(View v) {
		mKeyboardView.setVisibility(View.VISIBLE);
		mKeyboardView.setEnabled(true);
		if (v == null)
			v = mHostView;
		((InputMethodManager) mHostActivity
		    .getSystemService(Activity.INPUT_METHOD_SERVICE))
		    .hideSoftInputFromWindow(v.getWindowToken(), 0);
	}

	/** Make the CustomKeyboard invisible. */
	public void hide() {
		mKeyboardView.setVisibility(View.GONE);
		mKeyboardView.setEnabled(false);
	}

	public void setFocus() {
		mEditText.setText("");
		mEditText.setFocusableInTouchMode(true);
		mEditText.requestFocus();
	}

	/**
	 * Register <var>EditText<var> with resource id <var>resid</var> (on the
	 * hosting activity) for using this custom keyboard.
	 * 
	 * @param resid
	 *          The resource id of the EditText that registers to the custom
	 *          keyboard.
	 */
	public void registerEditText(int resid) {
		// Find the EditText 'resid'
		mEditText = (EditText) mHostView.findViewById(resid);

		mEditText.setOnClickListener(new OnClickListener() {
			// NOTE By setting the on click listener, we can show the custom keyboard
			// again, by tapping on an edit box that already had focus (but that had
			// the keyboard hidden).
			public void onClick(View v) {
				show(v);
			}
		});
		// Disable standard keyboard hard way
		// NOTE There is also an easy way:
		// 'edittext.setInputType(InputType.TYPE_NULL)' (but you will not have a
		// cursor, and no 'edittext.setCursorVisible(true)' doesn't work )
		mEditText.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				EditText edittext = (EditText) v;
				int inType = edittext.getInputType(); // Backup the input type
				edittext.setInputType(InputType.TYPE_NULL); // Disable standard keyboard
				edittext.onTouchEvent(event); // Call native handler
				edittext.setInputType(inType); // Restore input type

				Editable editable = edittext.getText();
				editable.clear();

				return true; // Consume touch event
			}
		});
		// Disable spell check (hex strings look like words to Android)
		mEditText.setInputType(mEditText.getInputType()
		    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
	}

}
