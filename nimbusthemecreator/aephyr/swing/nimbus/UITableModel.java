package aephyr.swing.nimbus;

import java.awt.Font;
import java.util.Arrays;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;

public class UITableModel extends AbstractTableModel {
	
	static final int VALUE_COLUMN = 2;
	
	UITableModel(String[] kys) {
		this(kys, new Type[kys.length]);
	}
	UITableModel(String[] kys, Type[] tys) {
		keys = kys;
		types = tys;
		values = new Object[kys.length];
	}
	
	private String[] keys;
	private Type[] types;
	private Object[] values;

	int indexOfKey(Object key) {
		String[] k = keys;
		for (int i=k.length; --i>=0;)
			if (key.equals(k[i]))
				return i;
		return -1;
	}

	@Override
	public int getColumnCount() {
		return 4;
	}
	
	@Override
	public String getColumnName(int col) {
		switch (col) {
		case 0: return "Key";
		case 1: return "Type";
		case VALUE_COLUMN: return "Value";
		case 3: return "Default";
		}
		throw new IllegalArgumentException();
	}

	@Override
	public Class<?> getColumnClass(int col) {
		switch (col) {
		case 3: return Boolean.class;
		}
		return Object.class;
	}

	@Override
	public int getRowCount() {
		return keys.length;
	}

	@Override
	public Object getValueAt(int row, int col) {
		switch (col) {
		case 0: return getKey(row);
		case 1: return getType(row);
		case VALUE_COLUMN: return getValue(row);
		case 3: return isDefault(row);
		}
		throw new IllegalArgumentException();
	}
	
	String getKey(int row) {
		return keys[row];
	}
	
	Type getType(int row) {
		if (types[row] == null)
			types[row] = Type.getType(UIManager.get(keys[row]));
		return types[row];
	}
	
	Object getValue(int row) {
		if (values[row] == null)
			values[row] = UIManager.get(keys[row]);
		return values[row];
	}

	boolean isDefault(int row) {
		if (values[row] == null)
			return true;
		return values[row].equals(
				UIManager.getLookAndFeelDefaults().get(keys[row]));
	}
	
	@Override
	public boolean isCellEditable(int row, int col) {
		return col == 2 || col == 3;
	}
	
	// Font.equals() is too sensitive, so use this.
	private boolean fontEquals(Font a, Font b) {
		return a.getSize2D() == b.getSize2D() &&
			a.getStyle() == b.getStyle() && 
			a.getFamily().equals(b.getFamily());
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		switch (col) {
		case 2:
			Object def = UIManager.getLookAndFeel().getDefaults().get(keys[row]);
			if ((getType(row) == Type.Font && fontEquals((Font)aValue, (Font)def)) || aValue.equals(def)) {
				values[row] = def;
			} else {
				values[row] = aValue;
			}
			fireTableCellUpdated(row, 3);
			break;
		case 3:
			if (aValue == Boolean.TRUE) {
				values[row] = UIManager.getLookAndFeelDefaults().get(keys[row]);
				fireTableCellUpdated(row, 2);
			}
			break;
		}
	}

}