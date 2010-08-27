package aephyr.swing.nimbus;

import java.awt.Dimension;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.Icon;

import javax.swing.Painter; // 1.7
//import com.sun.java.swing.Painter; // 1.6

import aephyr.swing.nimbus.ValueChooser.*;

enum Type {
	Color,
	Painter,
	Insets,
	Font,
	Boolean,
	Integer,
	String,
	Icon,
	Dimension,
	Object;
	
	private ValueChooser chooser;
	
	ValueChooser getValueChooser() {
		if (chooser == null) {
			switch (this) {
			case Color: chooser = new ColorChooser(); break;
			case Painter: break;
			case Insets: chooser = new InsetsChooser(); break;
			case Font: chooser = new FontChooser(); break;
			case Boolean: chooser = new BooleanChooser(); break;
			case Integer: chooser = new IntegerChooser(); break;
			case String: chooser = new StringChooser(); break;
			case Icon: break;
			case Dimension: chooser = new DimensionChooser(); break;
			}
		}
		return chooser;
	}
	
	boolean hasChooser() {
		switch (this) {
		case Painter:
		case Icon:
		case Object:
			return false;
		}
		return true;
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
