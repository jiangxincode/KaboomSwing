package aephyr.swing.treetable;

import java.awt.Component;

import javax.swing.CellEditor;

import aephyr.swing.TreeTable;


public interface TreeTableCellEditor extends CellEditor {

	Component getTreeTableCellEditorComponent(TreeTable treeTable,
			Object value, boolean isSelected, int row, int column);

	Component getTreeTableCellEditorComponent(TreeTable treeTable,
			Object value, boolean isSelected, int row, int column,
			boolean expanded, boolean leaf);

}
