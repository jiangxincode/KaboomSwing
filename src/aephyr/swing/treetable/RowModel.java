package aephyr.swing.treetable;

import aephyr.swing.event.RowModelListener;

public interface RowModel {

	String getColumnName(int column);
	
	Class<?> getColumnClass(int column);
	
	int getColumnCount();
	
	Object getValueAt(Object node, int column);
	
	void setValueAt(Object value, Object node, int column);
	
	boolean isCellEditable(Object node, int column);
	
	int getHierarchialColumn();
	
	void addRowModelListener(RowModelListener l);
	
	void removeRowModelListener(RowModelListener l);

}
