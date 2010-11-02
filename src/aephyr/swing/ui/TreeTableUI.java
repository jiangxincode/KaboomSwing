package aephyr.swing.ui;

import java.awt.Rectangle;

import javax.swing.plaf.ComponentUI;
import javax.swing.tree.TreePath;

import aephyr.swing.TreeTable;
import aephyr.swing.treetable.DefaultTreeTableCellEditor;
import aephyr.swing.treetable.DefaultTreeTableCellRenderer;
import aephyr.swing.treetable.TreeTableCellEditor;
import aephyr.swing.treetable.TreeTableCellRenderer;

public abstract class TreeTableUI extends ComponentUI {
	
	public abstract TreeInterface getTreeInterface(TreeTable treeTable);
	
	public abstract TableInterface getTableInterface(TreeTable treeTable);

	public abstract void configureCellRenderer(
			DefaultTreeTableCellRenderer renderer, TreeTable treeTable,
			Object value, boolean selected, boolean hasFocus,
			int row, int column);
	

	public abstract void configureCellRenderer(
			DefaultTreeTableCellRenderer renderer, TreeTable treeTable,
			Object value, boolean selected, boolean hasFocus,
			int row, int column, boolean expanded, boolean leaf);
	
	public abstract void configureCellEditor(DefaultTreeTableCellEditor editor,
			TreeTable treeTable, Object value, boolean selected, int row, int column);
	
	public abstract void configureCellEditor(DefaultTreeTableCellEditor editor,
			TreeTable treeTable, Object value, boolean selected,
			int row, int column, boolean expanded, boolean leaf);
	
	public abstract TreeTableCellRenderer getDefaultRenderer(TreeTable treeTable, Class<?> columnClass);

	public abstract TreeTableCellEditor getDefaultEditor(TreeTable treeTable, Class<?> columnClass, int column);
	
	public abstract Rectangle getPathBounds(TreeTable treeTable, TreePath path);
	
	public abstract TreePath getPathForLocation(TreeTable treeTable, int x, int y);
	
	public abstract TreePath getClosestPathForLocation(TreeTable treeTable, int x, int y);
	
	public abstract int getTreeHandleWidth(TreeTable treeTable);
	
}
