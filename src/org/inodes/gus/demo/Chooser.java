package org.inodes.gus.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class Chooser extends ListFragment {
	private final static String TAG = "Chooser";

	private Messenger mDeviceService = null;
	private ArrayAdapter<Drink> mAdapter;
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
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		List<Drink> drinks = readDrinks(R.xml.drinks);
		mAdapter = new ArrayAdapter<Drink>(getActivity(), android.R.layout.simple_list_item_1, drinks);
		setListAdapter(mAdapter);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		lv.setEnabled(false);  // enabled once we connect to DeviceInterface
	}
	
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
	public void onListItemClick(ListView l, View v, int position, long id) {
		Drink drink = (Drink)mAdapter.getItem(position);
		try {
			Message msg = Message.obtain(null, DeviceInterface.MSG_SEND_COMMAND,
					drink.getCommand());
			mDeviceService.send(msg);
		} catch (RemoteException e) {
			Toast.makeText(getActivity(), R.string.deviceservice_disconnected,
					Toast.LENGTH_SHORT).show();
		}
	}

	public List<Drink> readDrinks(int resource) {
		XmlPullParser parser = getResources().getXml(resource);
		try {
			return Drink.readDrinks(parser);
		} catch (XmlPullParserException e) {
			Log.e(TAG, "Error parsing drinks.xml", e);
			// fixme: toast message so user sees error
		} catch (IOException e) {
			Log.e(TAG, "Error reading drinks.xml", e);
			// fixme: toast message so user sees error
		}
		return new ArrayList<Drink>();
	}
}
