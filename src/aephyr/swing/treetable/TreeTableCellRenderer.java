package aephyr.swing.treetable;

import java.awt.Component;

import aephyr.swing.TreeTable;

public interface TreeTableCellRenderer {

	Component getTreeTableCellRendererComponent(TreeTable treeTable,
			Object value, boolean selected, boolean hasFocus,
			int row, int column);
	
	Component getTreeTableCellRendererComponent(TreeTable treeTable,
			Object value, boolean selected, boolean hasFocus,
			int row, int column, boolean expanded, boolean leaf);
	
}
