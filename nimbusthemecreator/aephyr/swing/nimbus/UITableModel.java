package aephyr.swing.nimbus;

import java.awt.Font;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Arrays;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.AbstractTableModel;


public class UITableModel extends AbstractTableModel {

	private static class ValueElement {
		
		ValueElement(Object val, Object def) {
			value = val;
			defaultValue = def;
		}
		
		Object value;
		
		Object defaultValue;
		
	}

	static final int KEY_COLUMN_INDEX = 0;
	static final int TYPE_COLUMN_INDEX = 1;
	static final int VALUE_COLUMN_INDEX = 2;
	static final int DEFAULT_COLUMN_INDEX = 3;
	
	UITableModel(String[] kys) {
		this(kys, new Type[kys.length]);
	}
	
	UITableModel(String[] kys, Type[] tys) {
		keys = kys;
		types = tys;
		values = new ValueElement[kys.length];
	}
	
	private String[] keys;
	private Type[] types;
	private ValueElement[] values;

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
		case KEY_COLUMN_INDEX: return "K\u0332ey";
		case TYPE_COLUMN_INDEX: return "T\u0332ype";
		case VALUE_COLUMN_INDEX: return "Value";
		case DEFAULT_COLUMN_INDEX: return "D\u0332efault";
		}
		throw new IllegalArgumentException();
	}

	@Override
	public Class<?> getColumnClass(int col) {
		switch (col) {
		case DEFAULT_COLUMN_INDEX: return Boolean.class;
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
		case KEY_COLUMN_INDEX: return getKey(row);
		case TYPE_COLUMN_INDEX: return getType(row);
		case VALUE_COLUMN_INDEX: return getValue(row);
		case DEFAULT_COLUMN_INDEX: return isDefault(row);
		}
		throw new IllegalArgumentException();
	}
	
	String getKey(int row) {
		return keys[row];
	}
	
	Type getType(int row) {
		if (types[row] == null) {
			RemoteUIDefaults def = Creator.getUIDefaults();
			if (def != null) {
				try {
					types[row] = Type.valueOf(def.getTypeName(keys[row]));
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			if (types[row] == null)
				types[row] = Type.getType(UIManager.get(keys[row]));
		}
		return types[row];
	}
	
	Object getValue(int row) {
		ValueElement element = values[row];
		Object value;
		if (element != null) {
			value = element.value;
		} else {
			RemoteUIDefaults def = Creator.getUIDefaults();
			if (def != null) {
				try {
					Object defaultValue;
					switch (getType(row)) {
					case Color:
						value = def.getColor(keys[row], false);
						defaultValue = def.getColor(keys[row], true);
						break;
					case Painter: case Icon:
						value = def.getImage(keys[row]);
						defaultValue = null;
						break;
					default:
						value = def.get(keys[row], false);
						if (value != null) {
							defaultValue = def.get(keys[row], true);
						} else {
							value = UIManager.get(keys[row]);
							defaultValue = null;
						}
						break;
					}
					values[row] = new ValueElement(value, defaultValue);
				} catch (RemoteException e) {
					e.printStackTrace();
					value = UIManager.get(keys[row]);
				}
			} else {
				value = UIManager.get(keys[row]);
			}
		}
		return value;
	}

	boolean isDefault(int row) {
		Object value = getValue(row);
		ValueElement element = values[row];
		if (element == null || element.defaultValue == null)
			return true;
		return value.equals(element.defaultValue);
	}
	
	@Override
	public boolean isCellEditable(int row, int col) {
		if (col == DEFAULT_COLUMN_INDEX)
			return true;
		if (col == VALUE_COLUMN_INDEX)
			return getType(row).hasChooser();
		return false;
	}
	
	private void updateValue(RemoteUIDefaults def, int row) throws RemoteException {
		ValueElement element = values[row];
		if (element != null) {
			switch (getType(row)) {
			case Color:
				element.value = def.getColor(keys[row], false);
				element.defaultValue = def.getColor(keys[row], true);
				break;
			case Painter: case Icon:
				element.value = def.getImage(keys[row]);
				break;
			default:
				element.value = def.get(keys[row], false);
				break;
			}
		}
	}
	
	@Override
	public void setValueAt(Object aValue, int row, int col) {
		RemoteUIDefaults def = Creator.getUIDefaults();
		if (def != null) {
			try {
				switch (col) {
				case VALUE_COLUMN_INDEX:
					def.put(keys[row], (Serializable)aValue);
					updateValue(def, row);
					fireTableCellUpdated(row, DEFAULT_COLUMN_INDEX);
					break;
				case DEFAULT_COLUMN_INDEX:
					if (aValue == Boolean.TRUE) {
						def.put(keys[row], null);
						updateValue(def, row);
						fireTableCellUpdated(row, VALUE_COLUMN_INDEX);
					}
					break;
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	void clearCache() {
		values = new ValueElement[keys.length];
		fireTableRowsUpdated(0, getRowCount()-1);
	}

}