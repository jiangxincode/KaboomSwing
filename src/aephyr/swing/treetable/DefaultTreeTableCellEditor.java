package aephyr.swing.treetable;

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;

import aephyr.swing.TreeTable;


public class DefaultTreeTableCellEditor extends DefaultCellEditor
		implements TreeTableCellEditor {
	
	public DefaultTreeTableCellEditor(JCheckBox checkBox) {
		super(checkBox);
		// TODO Auto-generated constructor stub
	}

	public DefaultTreeTableCellEditor(JComboBox comboBox) {
		super(comboBox);
		// TODO Auto-generated constructor stub
	}

	public DefaultTreeTableCellEditor(JTextField textField) {
		super(textField);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Component getTreeTableCellEditorComponent(TreeTable treeTable,
			Object value, boolean selected, int row, int column) {
		treeTable.getUI().configureCellEditor(this,
				treeTable, value, selected, row, column);
		return getComponent();
	}

	@Override
	public Component getTreeTableCellEditorComponent(TreeTable treeTable,
			Object value, boolean selected, int row, int column,
			boolean expanded, boolean leaf) {
		treeTable.getUI().configureCellEditor(this,
				treeTable, value, selected, row, column, expanded, leaf);
		return getComponent();
	}
	
	@Override
	public final Component getTableCellEditorComponent(JTable table,
			Object value, boolean selected, int row, int column) {
		return super.getTableCellEditorComponent(
				table, value, selected, row, column);
	}
	
	@Override
	public final Component getTreeCellEditorComponent(JTree tree,
			Object value, boolean selected, boolean expanded, boolean leaf, int row) {
		return super.getTreeCellEditorComponent(
				tree, value, selected, expanded, leaf, row);
	}

}
