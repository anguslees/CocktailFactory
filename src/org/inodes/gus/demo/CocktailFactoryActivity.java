package org.inodes.gus.demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class CocktailFactoryActivity extends FragmentActivity {
	private final static String TAG = "CocktailFactoryActivity";
	private UsbAccessory mUsbAccessory;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Intent intent = getIntent();
		if (intent.getAction().equals(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)) { 
			mUsbAccessory = UsbManager.getAccessory(intent);
			Log.d(TAG, "got accessory attached intent: " + mUsbAccessory);
		}
	}
	
	public UsbAccessory getAccessory() {
		return mUsbAccessory;
	}
	
	public ParcelFileDescriptor getAccessoryFd() {
		UsbManager usbManager = UsbManager.getInstance(this);
		return usbManager.openAccessory(mUsbAccessory);
	}
}
