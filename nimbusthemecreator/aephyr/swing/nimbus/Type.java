package aephyr.swing.nimbus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.Icon;
//import javax.swing.Painter; // 1.7
import com.sun.java.swing.Painter; // 1.6


enum Type {
	Color(ValueChooser.ColorChooser.class),
	Painter(null),
	Insets(ValueChooser.InsetsChooser.class),
	Font(ValueChooser.FontChooser.class),
	Boolean(ValueChooser.BooleanChooser.class),
	Integer(ValueChooser.IntegerChooser.class),
	String(ValueChooser.StringChooser.class),
	Icon(null),
	Dimension(ValueChooser.DimensionChooser.class),
	Object(null);
	
	private Type(Class<? extends ValueChooser> cls) {
		chooserClass = cls;
	}
	
	private ValueChooser chooser;
	private Class<? extends ValueChooser> chooserClass;
	
	ValueChooser getValueChooser() {
		if (chooser == null) {
			if (chooserClass == null)
				return null;
			try {
				chooser = chooserClass.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				chooserClass = null;
				return null;
			}
		}
		return chooser;
	}
	
	
	static Type getType(Object obj) {
		if (obj instanceof Color) {
			return Color;
		} else if (obj instanceof Painter<?>) {
			return Painter;
		} else if (obj instanceof Insets) {
			return Insets;
		} else if (obj instanceof Font) {
			return Font;
		} else if (obj instanceof Boolean) {
			return Boolean;
		} else if (obj instanceof Integer) {
			return Integer;
		} else if (obj instanceof Icon) {
			return Icon;
		} else if (obj instanceof String) {
			return String;
		} else if (obj instanceof Dimension) {
			return Dimension;
		} else {
			return Object;
		}

	}
}
