package aephyr.swing.nimbus;

import java.util.Arrays;


public class UITypeTableModel extends UITableModel {
	
	UITypeTableModel(String[] keys, Type typ, boolean edt) {
		super(keys, null);
		type = typ;
		editable = edt;
	}
	
	private Type type;
	private boolean editable;

	@Override
	Type getType(int row) {
		return type;
	}
	
	@Override
	public String getColumnName(int col) {
		return col == 2 ? type.name() : super.getColumnName(col);
	}
	
	@Override
	public boolean isCellEditable(int row, int col) {
		return editable ? super.isCellEditable(row, col) : false;
	}
	

}
