package org.inodes.gus.demo.test;

import java.util.List;

import org.inodes.gus.demo.Chooser;
import org.inodes.gus.demo.Drink;
import org.inodes.gus.demo.FooTenderActivity;
import org.inodes.gus.demo.R;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListAdapter;

public class ChooserTest extends ActivityInstrumentationTestCase2<FooTenderActivity> {
	FooTenderActivity mActivity;
	Chooser mChooser;

	public ChooserTest() {
		super(FooTenderActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mActivity = (FooTenderActivity)getActivity();
		assertNotNull(mActivity);
		mChooser = (Chooser) mActivity.getSupportFragmentManager().findFragmentById(R.id.chooser);
		assertNotNull(mChooser);
	}
	
	public void testPreconditions() {
		assertNotNull(mActivity);
		assertNotNull(mChooser);
		//assertNotNull(mActivity.getOnItemSelectedListener());
	}
	
	public void testReadDrinks() {
		List<Drink> drinks = mChooser.readDrinks(org.inodes.gus.demo.R.xml.drinks);
		assertNotNull(drinks);
		assertTrue(drinks.size() > 0);
	}

	public void testAdapter() {
		ListAdapter adapter = mChooser.getListAdapter();
		assertNotNull(adapter);
		assertTrue(adapter.getCount() > 0);
		assertTrue(adapter.getViewTypeCount() == 1);
	}
}
