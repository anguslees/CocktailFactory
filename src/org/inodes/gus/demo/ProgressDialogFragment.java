package org.inodes.gus.demo;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ProgressDialogFragment extends DialogFragment {
	public static ProgressDialogFragment newSpinnerInstance(String message) {
		ProgressDialogFragment f = new ProgressDialogFragment();

		Bundle args = new Bundle();
		args.putInt("style", ProgressDialog.STYLE_SPINNER);
		args.putString("message", message);
		f.setArguments(args);
		
		return f;
	}

	public static ProgressDialogFragment newHorizInstance() {
		ProgressDialogFragment f = new ProgressDialogFragment();

		Bundle args = new Bundle();
		args.putInt("style", ProgressDialog.STYLE_HORIZONTAL);
		f.setArguments(args);
		
		return f;
	}

	public void updateProgress(int progress, int max) {
		ProgressDialog d = (ProgressDialog)getDialog();
		if (d != null) {
			d.setProgress(progress);
			d.setMax(max);
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		ProgressDialog d = new ProgressDialog(getActivity());
		d.setTitle("Mixing your drink");
		d.setCancelable(false);
		d.setProgressStyle(args.getInt("style"));
		d.setMessage(args.getString("message"));
		return d;
	}
}
