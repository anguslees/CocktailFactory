package org.inodes.gus.demo;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.util.Log;

public class Bottle {
	private final static String TAG = "CocktailFactoryBottle";

	private final int mBottleNum;
	private final String mName;

	private static Map<String, Bottle> singletonMap;

	public static Bottle getBottle(Context context, String id) {
		if (singletonMap == null)
			readBottles(context);
		Bottle ret = singletonMap.get(id);
		if (ret == null)
			Log.e(TAG, "Failed to find bottle id=" + id);
		return ret;
	}
	
	public static Collection<Bottle> getBottles(Context context) {
		if (singletonMap == null)
			readBottles(context);
		return singletonMap.values();
	}

	public static void readBottles(Context context) {
		try {
			XmlPullParser parser = context.getResources().getXml(R.xml.bottles);
			Map<String, Bottle> map = new HashMap<String, Bottle>();
			boolean end = false;
			while (!end) {
				Log.d(TAG, "looping over bottles.xml");
				switch (parser.next()) {
				case XmlPullParser.START_TAG:
					String tag = parser.getName();
					Log.d(TAG, "got tag " + tag);
					if (tag.equals("ingredient")) {
						String id = parser.getAttributeValue(null, "id");
						int bottle = Integer.parseInt(parser.getAttributeValue(null, "bottle_num"));
						String name = parser.nextText();
						Log.d(TAG, "Adding bottle name=" + name + " id=" + id + " num=" + bottle);
						map.put(id, new Bottle(id, bottle, name));
					}
					break;
				case XmlPullParser.END_DOCUMENT:
					end = true;
					break;
				}
			}
			Log.d(TAG, "Found bottles list of size " + map.size());
			singletonMap = Collections.unmodifiableMap(map);
		} catch (XmlPullParserException e) {
			Log.e(TAG , "Error parsing bottle list", e);
		} catch (IOException e) {
			Log.e(TAG, "Error reading bottle list", e);
		}
	}

	private Bottle(String id, int bottle, String name) {
		mBottleNum = bottle;
		mName = name;
	}

	@Override
	public String toString() {
		return mName;
	}
	
	public int bottleNum() {
		return mBottleNum;
	}
}
