package org.inodes.gus.demo;

import java.io.BufferedReader;
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
	private static final String TAG = "FooTenderSvc";
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_SEND_COMMAND = 3;
	public static final int MSG_DEVICE_OUTPUT = 4;
	public static final int MSG_DEVICE_ERROR = 5;
	
	private OutputStream mOutputStream;
	private InputStream mInputStream;
	private Writer mOutputStreamWriter;
	private ParcelFileDescriptor mFileDescriptor = null;
	private Collection<Messenger> mClients = new ArrayList<Messenger>();
	final Handler mIncomingHandler = new IncomingHandler();
	final Messenger mMessenger = new Messenger(mIncomingHandler);

	// Reads from (device) input stream, and turns data into messages on the main handler thread 
	private class DeviceReader extends Thread {
		private InputStream mInputStream;

		public DeviceReader(InputStream is) {
			super("DeviceReader");
			mInputStream = is;
		}

		@Override
		public void run() {
			// Blocks on reads from accessory, and passes received messages off to parent handler thread
			BufferedReader reader = new BufferedReader(new InputStreamReader(mInputStream), 100);
			try {
				while (!isInterrupted()) {
					String line = reader.readLine();
					Log.i(TAG, "Read input from device: " + line);
					sendToClients(MSG_DEVICE_OUTPUT, line);
				}
			} catch (IOException e) {
				Log.e(TAG, "Error while reading input from accessory", e);
				notifyDeviceError("Error while reading input from accessory", e);
			}
		}
	}

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			case MSG_SEND_COMMAND:
				writeToDevice((String)msg.obj);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void notifyDeviceError(String text, Exception err) {
		notifyDeviceError(text + err.getMessage());
	}

	private void notifyDeviceError(String text) {
		Log.w(TAG, "Device error: " + text);
		sendToClients(MSG_DEVICE_ERROR, text);
	}

	private void sendToClients(int what, String text) {
		Message msg = Message.obtain(null, what, text);
		msg.replyTo = mMessenger;
		Collection<Messenger> morgue = new ArrayList<Messenger>();
		for (Messenger client : mClients) {
			try {
				client.send(msg);
			} catch (RemoteException e) {
				// Client is dead.  Sniff.
				morgue.add(client);
			}
		}
		mClients.removeAll(morgue);
	}

	private void writeToDevice(String command) {
		Log.i(TAG, "Writing command: " + command);
		if (mOutputStreamWriter == null) {
			notifyDeviceError("No device found");
		} else {
			try {
				mOutputStreamWriter.write(command);
				mOutputStreamWriter.flush();
			} catch (IOException e) {
				notifyDeviceError("Error writing to device", e);
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "StartCommand id=" + startId + " intent=" + intent);
		UsbAccessory accessory = UsbManager.getAccessory(intent);
		openAccessory(accessory);
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	protected void openAccessory(UsbAccessory accessory) {
		Log.d(TAG, "openAccessory: " + accessory);
		UsbManager mUsbManager = UsbManager.getInstance(this);		
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			mOutputStreamWriter = new OutputStreamWriter(mOutputStream);
			Thread thread = new DeviceReader(mInputStream);
			thread.start();
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
}
