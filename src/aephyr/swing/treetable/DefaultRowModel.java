package aephyr.swing.treetable;

import java.util.Collections;
import java.util.List;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import aephyr.swing.treetable.AbstractRowModel;
import aephyr.swing.treetable.MutableTreeTableNode;
import aephyr.swing.treetable.TreeTableNode;

public class DefaultRowModel extends AbstractRowModel {
	
	public DefaultRowModel(int columns) {
		this(null, columns);
	}
	
	public DefaultRowModel(TreeTableNode root, int columns) {
		this(root, Collections.nCopies(columns, null));
	}
	
	public DefaultRowModel(TreeTableNode root, List<?> columnNames) {
		this.root = root;
		this.columnNames = columnNames;
	}
	
	private List<?> columnNames;
	
	private int editableColumns = -1;
	
	private int hierarchialColumn;
	
	private TreeTableNode root;
	
	@Override
	public String getColumnName(int column) {
		Object name = columnNames.get(column);
		String str = name == null ? null : name.toString();
		return str != null ? str : super.getColumnName(column);
	}
	
	@Override
	public Class<?> getColumnClass(int column) {
		if (root == null)
			return Object.class;
		Object value = root.getValueAt(column);
		if (value == null)
			return Object.class;
		if (value instanceof Class<?>)
			return (Class<?>)value;
		return value.getClass();
	}
	
	@Override
	public int getColumnCount() {
		return columnNames.size();
	}
	
	@Override
	public Object getValueAt(Object node, int column) {
		return ((TreeTableNode)node).getValueAt(column);
	}
	
	@Override
	public void setValueAt(Object value, Object node, int column) {
		((MutableTreeTableNode)node).setValueAt(value, column);
		fireRowChanged(pathToRoot((TreeNode)node), column);
	}
	
	@Override
	public boolean isCellEditable(Object node, int column) {
		return (editableColumns & (1 << column)) != 0;
	}
	
	public void setEditable(int column, boolean editable) {
		if (column > 32)
			throw new IllegalArgumentException();
		if (editable) {
			editableColumns |= 1 << column;
		} else {
			editableColumns &= ~(1 << column);
		}
	}
	
	@Override
	public int getHierarchialColumn() {
		return hierarchialColumn;
	}
	
	public void setHierarchialColumn(int column) {
		hierarchialColumn = column;
	}

	public TreePath pathToRoot(TreeNode node) {
		if (node == root)
			return new TreePath(node);
		return pathToRoot(node.getParent()).pathByAddingChild(node);
	}
}
