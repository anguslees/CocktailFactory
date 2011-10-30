package org.inodes.gus.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


public class Drink {
	private final String mName;
	private final String mCommand;

	public Drink(String name, String command) {
		mName = name;
		mCommand = command;
	}

	public String getName() {
		return mName;
	}

	public String getCommand() {
		return mCommand;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	public static List<Drink> readDrinks(XmlPullParser parser) throws XmlPullParserException, IOException {
		ArrayList<Drink> drinks = new ArrayList<Drink>();
		String tag;
		String lastName = null;
		String lastCommand = null;
		boolean end = false;
		while (!end) {
			switch (parser.next()) {
			case XmlPullParser.START_TAG:
				tag = parser.getName();
				if (tag.equals("name"))
					lastName = parser.nextText();
				else if (tag.equals("command"))
					lastCommand = parser.nextText();
				break;
			case XmlPullParser.END_TAG:
				tag = parser.getName();
				if (tag.equals("drink"))
					drinks.add(new Drink(lastName, lastCommand));
				break;
			case XmlPullParser.END_DOCUMENT:
				end = true;
				break;
			}
		}
		return drinks;
	}
}