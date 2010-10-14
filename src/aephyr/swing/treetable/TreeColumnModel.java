package aephyr.swing.treetable;

import aephyr.swing.event.TreeColumnModelListener;

public interface TreeColumnModel {

	String getColumnName(int column);
	
	Class<?> getColumnClass(int column);
	
	int getColumnCount();
	
	Object getValueAt(Object node, int column);
	
	void setValueAt(Object value, Object node, int column);
	
	boolean isCellEditable(Object node, int column);
	
	int getHierarchialColumn();
	
	void addTreeColumnModelListener(TreeColumnModelListener l);
	
	void removeTreeColumnModelListener(TreeColumnModelListener l);

}
