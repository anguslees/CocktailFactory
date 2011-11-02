package org.inodes.gus.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.util.Log;


public class Drink {
	private final String mName;
	private final String mDescription;
	private final List<Ingredient> mIngredients;
	private final int mImageResource;

	public static class Ingredient {
		public final Bottle bottle;
		public final int weight;
		private Ingredient(Bottle bottle, int weight) {
			assert bottle != null;
			Log.d("Bottle", "bottle=" + bottle);
			this.bottle = bottle;
			this.weight = weight;
		}
	}

	private Drink(String name, String description, List<Ingredient> ingredients, int imageResource) {
		mName = name;
		mDescription = description;
		mIngredients = ingredients;
		mImageResource = imageResource;
	}

	public String getName() {
		return mName;
	}
	
	public String getDescription() {
		return mDescription;
	}

	public int getImageResource() {
		return mImageResource;
	}

	public Collection<Ingredient> getIngredients() {
		return mIngredients;
	}

	@Override
	public String toString() {
		return getName();
	}
	
	private static Map<String, Drink> mDrinks = null;
	private static String TAG = "Drink";
	public static Drink getDrink(Context context, int resource, String name) {
		if (mDrinks == null)
			readDrinks(context, resource);
		Log.d(TAG, "getting drink " + name);
		return mDrinks.get(name);
	}
	public static Collection<Drink> getDrinks(Context context, int resource) {
		if (mDrinks == null)
			readDrinks(context, resource);
		Log.d(TAG, "Returning a drinks list of size " + mDrinks.values().size());
		return mDrinks.values();
	}

	private static void readDrinks(Context context, int resource) {
		Log.d(TAG, "reading drinks.xml");
		try {
			XmlPullParser parser = context.getResources().getXml(resource);
			Map<String, Drink> drinks = new HashMap<String, Drink>();
			String name = null;
			String description = null;
			List<Ingredient> ingredients = new ArrayList<Ingredient>();
			int image = -1;
			boolean end = false;
			while (!end) {
				String tag;
				switch (parser.next()) {
				case XmlPullParser.START_TAG:
					tag = parser.getName();
					if (tag.equals("drink")) {
						name = null;
						description = null;
						image = -1;
						ingredients = new ArrayList<Ingredient>();
					} else if (tag.equals("name"))
						name = parser.nextText();
					else if (tag.equals("description"))
						description = parser.nextText();
					else if (tag.equals("image"))
						image = context.getResources().getIdentifier(
								parser.getAttributeValue(null, "res"),
								"drawable", Chooser.class.getPackage().getName());
					else if (tag.equals("ingredient")) {
						Bottle bottle = Bottle.getBottle(context, R.xml.bottles, parser.getAttributeValue(null, "id"));
						int weight = Integer.parseInt(parser.getAttributeValue(null, "weight"));
						ingredients.add(new Ingredient(bottle, weight));
					}
					break;
				case XmlPullParser.END_TAG:
					tag = parser.getName();
					if (tag.equals("drink")) {
						drinks.put(name, new Drink(name, description, ingredients, image));
					}
					break;
				case XmlPullParser.END_DOCUMENT:
					end = true;
					break;
				}
			}
			mDrinks = drinks;
		} catch (XmlPullParserException e) {
			Log.e(TAG, "Error parsing drinks list", e);
		} catch (IOException e) {
			Log.e(TAG, "Error reading drinks list", e);
		}
	}
}