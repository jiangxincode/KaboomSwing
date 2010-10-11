package aephyr.swing.treetable;

import javax.swing.tree.DefaultMutableTreeNode;

import aephyr.swing.treetable.MutableTreeTableNode;

public class DefaultTreeTableNode extends DefaultMutableTreeNode implements MutableTreeTableNode {
	
	public DefaultTreeTableNode() {
		this("");
	}
	
	public DefaultTreeTableNode(Object ... rowData) {
		if (rowData == null)
			throw new NullPointerException();
		this.rowData = rowData;
	}
	
	private Object[] rowData;

	@Override
	public Object getValueAt(int column) {
		return rowData[column];
	}
	
	@Override
	public void setValueAt(Object value, int column) {
		rowData[column] = value;
	}
	
	public int getColumnCount() {
		return rowData.length;
	}
}
