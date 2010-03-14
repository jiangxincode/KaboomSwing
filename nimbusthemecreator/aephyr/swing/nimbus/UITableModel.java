package aephyr.swing.nimbus;

import java.awt.Font;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;


public class UITableModel extends AbstractTableModel {
	UITableModel(String[] kys) {
		this(kys, new Type[kys.length]);
	}
	UITableModel(String[] kys, Type[] tys) {
		keys = kys;
		types = tys;
	}
	
	private String[] keys;
	private Type[] types;

	@Override
	public int getColumnCount() {
		return 4;
	}
	
	@Override
	public String getColumnName(int col) {
		switch (col) {
		case 0: return "Key";
		case 1: return "Type";
		case 2: return "Value";
		case 3: return "Default";
		}
		throw new IllegalArgumentException();
	}

	@Override
	public Class<?> getColumnClass(int col) {
		switch (col) {
		case 2: return UIDefaults.class;
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
		case 0: return keys[row];
		case 1: return getType(row);
		case 2: return UIManager.get(keys[row]);
		case 3: return !UIManager.getDefaults().containsKey(keys[row]);
		}
		throw new IllegalArgumentException();
	}
	
	Type getType(int row) {
		if (types[row] == null)
			types[row] = Type.getType(UIManager.get(keys[row]));
		return types[row];
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
				UIManager.put(keys[row], null);
			} else {
				UIManager.put(keys[row], aValue);
			}
			fireTableCellUpdated(row, 3);
			break;
		case 3:
			if (aValue == Boolean.TRUE) {
				UIManager.put(keys[row], null);
				fireTableCellUpdated(row, 2);
			}
			break;
		}
	}

}
