package aephyr.swing.ui;

import javax.swing.plaf.ComponentUI;
import javax.swing.tree.TreePath;

import aephyr.swing.TreeTable;
import aephyr.swing.treetable.DefaultTreeTableCellRenderer;

public abstract class TreeTableUI extends ComponentUI {
	
	public abstract TreeInterface getTreeInterface(TreeTable treeTable);
	
	public abstract TableInterface getTableInterface(TreeTable treeTable);

	public abstract void invalidatePathBounds(TreeTable treeTable, TreePath path);
	
	public abstract void configureCellRenderer(
			DefaultTreeTableCellRenderer renderer, TreeTable treeTable,
			Object value, boolean selected, boolean hasFocus,
			int row, int column);
	

	public abstract void configureCellRenderer(
			DefaultTreeTableCellRenderer renderer, TreeTable treeTable,
			Object value, boolean selected, boolean hasFocus,
			int row, int column, boolean expanded, boolean leaf);
	
}
