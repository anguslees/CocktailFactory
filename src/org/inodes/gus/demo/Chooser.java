package org.inodes.gus.demo;

import java.util.Collection;

import android.app.ProgressDialog;
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
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentTransaction;
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
	
	private final static boolean ENABLE_SPEECH = true;

	private ArrayAdapter<Drink> mAdapter;
	private Messenger mDeviceService = null;
	private TextToSpeech mTts = null;

	final private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			mDeviceService = new Messenger(service);
			getListView().setEnabled(true);
		}

		public void onServiceDisconnected(ComponentName name) {
			getListView().setEnabled(false);
		}
		
	};

	final private Messenger mMessenger = new Messenger(new MessageHandler());
	private class MessageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "handleMessage: " + msg);
			switch (msg.what) {
			case DeviceInterface.MSG_DONE: {
				onDrinkCompleted();
				break;
			}
			case DeviceInterface.MSG_STATUS: {
				onDrinkProgress(msg.arg1, msg.arg2);
				break;
			}
			case DeviceInterface.MSG_ERROR: {
				onDrinkError((String)msg.obj);
				break;
			}
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

	protected void onDrinkChosen(Drink drink) {
		ProgressDialogFragment f = ProgressDialogFragment.newSpinnerInstance(
				"Place your glass on the scales");
		f.show(getFragmentManager(), "dialog");

		getListView().setEnabled(false);  // reenabled once finished
		
		if (mTts != null) {
			mTts.speak("An excellent choice!", TextToSpeech.QUEUE_FLUSH, null);
			mTts.speak("Just place your glass on the scales.", TextToSpeech.QUEUE_ADD, null);
		}
	}
	
	protected void onDrinkProgress(int progress, int max) {
		Log.d(TAG, "onDrinkProgress(" + progress + "/" + max + ")");
		ProgressDialogFragment f = (ProgressDialogFragment)getFragmentManager().findFragmentByTag("dialog");
		Log.d(TAG, "Found dialog fragment: " + f);
		if (f != null) {
			ProgressDialog d = (ProgressDialog)f.getDialog();
			if (d.isIndeterminate()) {
				Log.d(TAG, "dialog is indeterminate - switching to horizontal");
				// currently showing spinner - time to switch to horizontal
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				f.dismiss();
				f = ProgressDialogFragment.newHorizInstance();
				ft.add(f, "dialog");
				ft.commit();
			}
			f.updateProgress(progress, max);		
		}
	}

	protected void onDrinkCompleted() {
		getListView().setEnabled(true);

		ProgressDialogFragment f = (ProgressDialogFragment)getFragmentManager().findFragmentByTag("dialog");
		if (f != null)
			f.dismiss();

		if (mTts != null) {
			mTts.speak("Enjoy!", TextToSpeech.QUEUE_ADD, null);
		}
	}

	protected void onDrinkError(String err) {
		getListView().setEnabled(true);

		ProgressDialogFragment f = (ProgressDialogFragment)getFragmentManager().findFragmentByTag("dialog");
		if (f != null)
			f.dismiss();

		if (mTts != null) {
			mTts.speak("Oops, sorry about that!", TextToSpeech.QUEUE_FLUSH, null);
		}
		
		Toast.makeText(getActivity(), "Error: " + err, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAdapter = new DrinkAdapter(getActivity(), Drink.getDrinks(getActivity()));
		setListAdapter(mAdapter);
		Log.d(TAG, "xml files parsed");

		if (ENABLE_SPEECH) {
			mTts = new TextToSpeech(getActivity(), new TextToSpeech.OnInitListener() {
				public void onInit(int status) {
					if (status == TextToSpeech.ERROR) {
						Log.e(TAG, "Error initialising text-to-speech - disabling");
						mTts = null;
					}
				}
			});
		}
	}

	@Override
	public void onDestroy() {
		if (mTts != null) mTts.shutdown();
		super.onDestroy();
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

			onDrinkChosen(drink);
		} catch (RemoteException e) {
			Toast.makeText(getActivity(), R.string.deviceservice_disconnected,
					Toast.LENGTH_SHORT).show();
		}
	}
}
