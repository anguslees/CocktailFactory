package org.inodes.gus.demo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

public class DeviceInterface extends Service {
	private static final String TAG = "CocktailFactorySvc";
	
	// Scale down drinks for demos - set to 1 for "release"
	private final static int WEIGHT_FACTOR = 5;

	public static final String DATA_ACCESSORYFD = "AccessoryFd";

	public static final int MSG_DONE = 2;
	public static final int MSG_DEVICE_READY = 3;
	public static final int MSG_MAKE_DRINK = 4;
	public static final int MSG_RESET = 5;
	public static final int MSG_STATUS = 6;
	public static final int MSG_ERROR = 7;
	
	private static final int REQ_WAIT = 119;     // 'w'
	private static final int REQ_DISPENSE = 100; // 'd'
	private static final int RESP_CONTINUE = 10;
	private static final int RESP_OK = 20;
	private static final int RESP_CLIENT_ERR = 40;
	private static final int RESP_SERVER_ERR = 50;

	private Messenger mMessenger;

	@Override
	public void onCreate() {
		mDrinkMakerThread.start();
		mMessenger = new Messenger(new DrinkMakerHandler(mDrinkMakerThread.getLooper()));
	}

	@Override
	public void onDestroy() {
		mDrinkMakerThread.quit();
	}

	final HandlerThread mDrinkMakerThread = new HandlerThread("DrinkMaker");
	class DrinkMakerHandler extends Handler {
		public DrinkMakerHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "handleMessage " + msg);
			switch (msg.what) {
			case MSG_MAKE_DRINK:
				Bundle data = msg.getData();
				Log.d(TAG, "Got data=" + data);
				String drinkName = (String)msg.obj;
				ParcelFileDescriptor pfd = (ParcelFileDescriptor)data.getParcelable(DATA_ACCESSORYFD); 
				Log.d(TAG, "Got pfd=" + pfd);

				MixDrinksTask t = new MixDrinksTask(
						Drink.getDrink(DeviceInterface.this, drinkName),
						msg.replyTo,
						pfd.getFileDescriptor());
				t.run();
				
				try {
					pfd.close();
				} catch (IOException e) {
				}

				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	}

	// This class only exists to avoid passing around various related options
	class MixDrinksTask implements Runnable {
		final private Drink mDrink;
		final private Messenger mReplyTo;
		final private DataOutputStream mOut;
		final private DataInputStream mIn;
		final private int mProgressMax;
		private int mPreviousProgress;
		
		public MixDrinksTask(Drink drink, Messenger replyTo, FileDescriptor fd) {
			mDrink = drink;
			mReplyTo = replyTo;
			mOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fd), 4));
			mIn = new DataInputStream(new BufferedInputStream(new FileInputStream(fd), 16384));

			int max = 0;
			for (Drink.Ingredient i : drink.getIngredients())
				max += i.weight / WEIGHT_FACTOR;
			mProgressMax = max;
			mPreviousProgress = 0;
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
				status = mIn.readUnsignedByte();
				progress = mIn.readUnsignedByte() | (mIn.readUnsignedByte() << 8);
				Log.d(TAG, "Read response {" + status + ", " + progress + "}");
				if (mReplyTo != null) {
					switch (status) {
					case RESP_OK:
					case RESP_CONTINUE:
						Message msg = Message.obtain(null, MSG_STATUS,
								mPreviousProgress + progress, mProgressMax);
						try {
							mReplyTo.send(msg);
						} catch (RemoteException e) {
							Log.w(TAG, "Unable to send response to requestor", e);
						}
						break;
					case RESP_CLIENT_ERR:
					case RESP_SERVER_ERR:
						throw new IOException("Error from device: status=" + status);
					default:
						throw new IOException("Unexpected response from device: " + status);
					}
				}
			} while (status == RESP_CONTINUE);
		}

		private void doCommand(int command, int target, int value) throws IOException {
			writeCommand(command, target, value);
			waitForDevice();
		}

		@Override
		public void run() {
			try {
				// reset/zero weight measurements
				doCommand(REQ_WAIT, 0, 0);

				for (Drink.Ingredient ingredient : mDrink.getIngredients()) {
					int weight = ingredient.weight / WEIGHT_FACTOR;
					doCommand(REQ_DISPENSE, ingredient.bottle.bottleNum(), weight);
					mPreviousProgress += weight;
				}
				
				Message msg = Message.obtain(null, MSG_DONE);
				mReplyTo.send(msg);
			} catch (IOException e) {
				Log.e(TAG, "IO error talking to device, aborting", e);
				try {
					Message msg = Message.obtain(null, MSG_ERROR, e.getMessage());
					mReplyTo.send(msg);
				} catch (RemoteException e1) {
					// Ignore
				}
			} catch (RemoteException e) {
				Log.e(TAG, "Error sending IPC response", e);
			}
		}
	}


	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}
}
