package org.inodes.gus.demo;

import java.util.Collection;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Chooser extends ListFragment {
	private final static String TAG = "CocktailFactoryChooser";

	private ArrayAdapter<Drink> mAdapter;
	private Messenger mDeviceService = null;

	final private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mDeviceService = new Messenger(service);
			getListView().setEnabled(true);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			getListView().setEnabled(false);
		}
		
	};

	final private Messenger mMessenger = new Messenger(new MessageHandler());
	private class MessageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DeviceInterface.MSG_DEVICE_READY:
				getListView().setEnabled(true);
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	}

	private class DrinkAdapter extends ArrayAdapter<Drink> {
		public DrinkAdapter(Context context, Collection<Drink> collection) {
			super(context, R.layout.drink_view, 0);
			// addAll(Collection<>) doesn't exist until API 11
			for (Drink d : collection)
				add(d);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final Drink drink = getItem(position);
			
			View v = convertView;
			if (v == null)
				v = View.inflate(getContext(), R.layout.drink_view, null);

			ImageView img = (ImageView)v.findViewById(R.id.drink_image);
			img.setImageResource(drink.getImageResource());

			TextView drink_name = (TextView)v.findViewById(R.id.drink_name);
			drink_name.setText(drink.getName());

			TextView desc = (TextView)v.findViewById(R.id.drink_desc);
			desc.setText(drink.getDescription());

			return v;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAdapter = new DrinkAdapter(getActivity(), Drink.getDrinks(getActivity()));
		setListAdapter(mAdapter);
		Log.d(TAG, "xml files parsed");
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		lv.setEnabled(false);  // enabled once we connect to DeviceInterface service
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "binding to service");
		getActivity().bindService(new Intent(getActivity(), DeviceInterface.class),
				mConnection, Context.BIND_AUTO_CREATE);
		Log.d(TAG, "bound to service");
	}

	@Override
	public void onStop() {
		getActivity().unbindService(mConnection);
		super.onStop();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		CocktailFactoryActivity activity = (CocktailFactoryActivity)getActivity();
		Drink drink = (Drink)mAdapter.getItem(position);
		try {
			Message msg = Message.obtain(null, DeviceInterface.MSG_MAKE_DRINK,
					drink.getName());
			msg.replyTo = mMessenger;
			Bundle bundle = new Bundle();
			bundle.putParcelable(DeviceInterface.DATA_ACCESSORYFD, activity.getAccessoryFd());
			msg.setData(bundle);
			mDeviceService.send(msg);

			getListView().setEnabled(false);  // reenabled once finished
		} catch (RemoteException e) {
			Toast.makeText(getActivity(), R.string.deviceservice_disconnected,
					Toast.LENGTH_SHORT).show();
		}
	}
}
