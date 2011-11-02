package org.inodes.gus.demo;

import java.util.Collection;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class Chooser extends ListFragment {
	private final static String TAG = "CocktailFactoryChooser";

	private ArrayAdapter<Drink> mAdapter;
/*
	final private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			new Messenger(service);
			getListView().setEnabled(true);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			getListView().setEnabled(false);
		}
		
	};
	*/

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
Log.d(TAG, "getView for position " + position);
			
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

		mAdapter = new DrinkAdapter(getActivity(), Drink.getDrinks(getActivity(), R.xml.drinks));
		setListAdapter(mAdapter);
		Log.d(TAG, "xml files parsed");
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
	}

	/*
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
	 */

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Drink drink = (Drink)mAdapter.getItem(position);
		Intent intent = new Intent(
				Intent.ACTION_RUN,
				new Uri.Builder().scheme("drink").path(drink.getName()).build(),
				getActivity(), DeviceInterface.class);
		getActivity().startService(intent);
	}
}
