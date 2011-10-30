package org.inodes.gus.demo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public class DeviceLog extends Fragment {
	private TextView mTextView = null;
	private Messenger mDeviceService = null;

	class DeviceMessageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DeviceInterface.MSG_DEVICE_ERROR:
			case DeviceInterface.MSG_DEVICE_OUTPUT:
				String text = (String) msg.obj;
				mTextView.append(text + "\n");
				//fixme: mTextView.scrollTo show new text
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	final Messenger mMessenger = new Messenger(new DeviceMessageHandler());
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mDeviceService = new Messenger(service);
			Message msg = Message.obtain(null, DeviceInterface.MSG_REGISTER_CLIENT);
			msg.replyTo = mMessenger;
			try {
				mDeviceService.send(msg);
			} catch (RemoteException e) {
				// Error sending registration message.  Nothing better to do.
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	};

	@Override
	public void onStart() {
		super.onStart();
		getActivity().bindService(new Intent(getActivity(), DeviceInterface.class),
				mConnection, Context.BIND_AUTO_CREATE);		
	}

	@Override
	public void onStop() {
		getActivity().unbindService(mConnection);
		super.onStop();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstance) {
		ScrollView scroller = new ScrollView(getActivity());
		mTextView = new TextView(getActivity());
		scroller.addView(mTextView);
		return scroller;
	}
}