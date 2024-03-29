package org.inodes.gus.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.util.Log;


public class Drink {
	private final static String TAG = "CocktailFactoryDrink";
	private final String mName;
	private final String mDescription;
	private final List<Ingredient> mIngredients;
	private final int mImageResource;

	public static class Ingredient {
		public final Bottle bottle;
		public final int weight;
		public Ingredient(Bottle bottle, int weight) {
			this.bottle = bottle;
			this.weight = weight;
		}
	}

	private static class RandomDrink extends Drink {
		final private static int NUM_INGREDIENTS = 3;
		final private static int TOTAL_WEIGHT = 60;
		final Bottle[] allBottles;
		final Random mRandom = new Random();
		public RandomDrink(Context context, String name, String desc, int imgResource) {
			super(name, desc, null, imgResource);
			allBottles = Bottle.getBottles(context).toArray(new Bottle[0]);
		}
		@Override
		public Collection<Ingredient> getIngredients() {
			List<Ingredient> ingredients = new ArrayList<Ingredient>();
			for (int i = 0; i < NUM_INGREDIENTS; i++) {
				int bottle_num = mRandom.nextInt(allBottles.length);
				ingredients.add(new Drink.Ingredient(allBottles[bottle_num],
						TOTAL_WEIGHT / NUM_INGREDIENTS));
			}
			return ingredients;
		}
	}

	public Drink(String name, String description, List<Ingredient> ingredients, int imageResource) {
		mName = name;
		mDescription = description;
		mIngredients = ingredients;
		mImageResource = imageResource;
	}

	public String getName() {
		return mName;
	}
	
	public String getDescription() {
		if (!mDescription.equals(""))
			return mDescription;
		
		StringBuilder buf = new StringBuilder();
		for(Ingredient i : getIngredients()) {
			if(buf.length() > 0)
				buf.append(", ");
			buf.append(i.bottle.toString());
		}
		return buf.toString();
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
	public static Drink getDrink(Context context, String name) {
		if (mDrinks == null)
			readDrinks(context);
		Log.d(TAG, "getting drink " + name);
		return mDrinks.get(name);
	}
	public static Collection<Drink> getDrinks(Context context) {
		if (mDrinks == null)
			readDrinks(context);
		Log.d(TAG, "Returning a drinks list of size " + mDrinks.values().size());
		return mDrinks.values();
	}

	private static void readDrinks(Context context) {
		Log.d(TAG, "reading drinks.xml");

		Map<String, Drink> drinks = new LinkedHashMap<String, Drink>();

		Drink randomDrink = new RandomDrink(context, "I'm Feeling Lucky",
				"Let the bartender mix a drink just for you", R.drawable.drink_placeholder);
		drinks.put(randomDrink.getName(), randomDrink);

		try {
			XmlPullParser parser = context.getResources().getXml(R.xml.drinks);
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
						Bottle bottle = Bottle.getBottle(context, parser.getAttributeValue(null, "id"));
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
		} catch (XmlPullParserException e) {
			Log.e(TAG, "Error parsing drinks list", e);
		} catch (IOException e) {
			Log.e(TAG, "Error reading drinks list", e);
		}

		mDrinks = drinks;
	}
}