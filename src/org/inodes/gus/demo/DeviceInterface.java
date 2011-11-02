package org.inodes.gus.demo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class DeviceInterface extends Service {
	private static final String TAG = "CocktailFactorySvc";
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_DEVICE_READY = 3;
	public static final int MSG_MAKE_DRINK = 4;
	public static final int MSG_RESET = 5;
	public static final int MSG_STATUS = 6;
	
	private static final int REQ_WAIT = 119;     // 'w'
	private static final int REQ_DISPENSE = 100; // 'd'
	private static final int RESP_CONTINUE = 10;
	private static final int RESP_OK = 20;
	private static final int RESP_CLIENT_ERR = 40;
	private static final int RESP_SERVER_ERR = 50;

	private OutputStream mOutputStream;
	private InputStream mInputStream;
	private Writer mOutputStreamWriter;
	private ParcelFileDescriptor mFileDescriptor = null;

	class DrinkMaker extends Thread {
		private final Drink mDrink;
		private List<Drink.Ingredient> mRemainingIngredients;
		private final Messenger mReplyTo;
		private DataOutputStream mOut;
		private DataInputStream mIn;
		private final int mStartId;
		
		public DrinkMaker(Drink drink, Messenger replyTo, int startId) {
			mDrink = drink;
			mRemainingIngredients = new ArrayList<Drink.Ingredient>(drink.getIngredients());
			mReplyTo = replyTo;
			mStartId = startId;
			mOut = new DataOutputStream(new BufferedOutputStream(mOutputStream, 4));
			mIn = new DataInputStream(new BufferedInputStream(mInputStream, 16384));
		}

		private void writeCommand(int command, int target, int value) throws IOException {
			Log.d(TAG, "Writing command {" + command + ", " + target + ", " + value + "}");
			mOut.writeByte(command);
			mOut.writeByte(target);
			mOut.writeByte(value & 0xff);
			mOut.writeByte((value >>> 8) & 0xff);
			mOut.flush();
		}

		private void waitForDevice() throws IOException {
			int status;
			int progress;
			do {
				Log.d(TAG, "About to read from device");
				status = mIn.readUnsignedByte();
				Log.d(TAG, "Read byte: " + status);
				progress = mIn.readUnsignedByte() | (mIn.readUnsignedByte() << 8);
				Log.d(TAG, "Read response {" + status + ", " + progress + "}");
				if (mReplyTo != null && status != RESP_OK) {
					Message msg = Message.obtain(null, status, progress);
					try {
						mReplyTo.send(msg);
					} catch (RemoteException e) {
						Log.w(TAG, "Unable to send response to requestor", e);
					}
				}
			} while (status == RESP_CONTINUE);
		}

		@Override
		public void run() {
			try {
				// reset/zero weight measurements
				writeCommand(REQ_WAIT, 0, 0);
				waitForDevice();

				for (Drink.Ingredient ingredient : mDrink.getIngredients()) {
					Log.d(TAG, "ingredient=" + ingredient);
					Log.d(TAG, "bottle=" + ingredient.bottle);
					Log.d(TAG, "weight=" + ingredient.weight);
					Log.d(TAG, "bottlenum=" + ingredient.bottle.bottleNum());
					writeCommand(REQ_DISPENSE, ingredient.bottle.bottleNum(), ingredient.weight);
					waitForDevice();
				}
			} catch (IOException e) {
				Log.e(TAG, "IO error talking to device, aborting", e);
			}
			stopSelfResult(mStartId);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "StartCommand id=" + startId + " intent=" + intent);
		//UsbAccessory accessory = UsbManager.getAccessory(intent);
		UsbAccessory accessory = UsbManager.getInstance(this).getAccessoryList()[0];
		openAccessory(accessory);

		Drink drink = Drink.getDrink(this, R.xml.drinks, intent.getData().getPath().substring(1));
		assert drink != null;
		Thread t = new DrinkMaker(drink, null, startId);
		t.start();

		return START_NOT_STICKY;
	}

	protected void openAccessory(UsbAccessory accessory) {
		Log.d(TAG, "openAccessory: " + accessory);
		UsbManager usbManager = UsbManager.getInstance(this);		
		mFileDescriptor = usbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			mOutputStreamWriter = new OutputStreamWriter(mOutputStream);
		}
	}
	
	@Override
	public void onDestroy() {
		close();
	}

	private void close() {
		if (mFileDescriptor != null) {
			try {
				mOutputStream.close();
				mInputStream.close();
				mFileDescriptor.close();
				mFileDescriptor = null;
			} catch (IOException e) {
				Log.i(TAG, "Ignoring IOError while closing stream", e);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}
