package aephyr.swing.treetable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import aephyr.swing.TreeTable;

public class DefaultTreeTableCellRenderer extends JLabel
		implements TreeTableCellRenderer, TableCellRenderer {

	public DefaultTreeTableCellRenderer() {
	}
	
	@Override
	public Component getTreeTableCellRendererComponent(TreeTable treeTable,
			Object value, boolean selected, boolean hasFocus, int row, int column) {
		treeTable.getUI().configureCellRenderer(this, treeTable,
				value, selected, hasFocus, row, column);
		setValue(value);
		return this;
	}

	@Override
	public Component getTreeTableCellRendererComponent(TreeTable treeTable,
			Object value, boolean selected, boolean hasFocus, int row,
			int column, boolean expanded, boolean leaf) {
		treeTable.getUI().configureCellRenderer(this, treeTable,
				value, selected, hasFocus, row, column, expanded, leaf);
		setValue(value);
		return this;
	}
	
	protected void setValue(Object value) {
		setText(value == null ? "" : value.toString());
	}
	

	/**
	 * This class implements the TableCellRenderer interface as a convenience
	 * so that it can be stored as a renderer in a TableColumn.
	 * <p>
	 * This method is not actually implemented.
	 * 
	 * @throws UnsupportedOperationException
	 */
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		throw new UnsupportedOperationException();
	}
	
	// Performance Overrides
	
	@Override
    public void invalidate() {}
	@Override
    public void validate() {}
	@Override
    public void revalidate() {}
	
	@Override
	public void repaint() {}
	@Override
	public void repaint(long tm, int x, int y, int w, int h) {}
	@Override
    public void repaint(Rectangle r) {}

	@Override
	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		if (propertyName=="text"
			|| propertyName == "labelFor"
			|| propertyName == "displayedMnemonic"
			|| ((propertyName == "font" || propertyName == "foreground")
					&& oldValue != newValue
					&& getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey) != null)) {
			super.firePropertyChange(propertyName, oldValue, newValue);
		}
	}
	@Override
	public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}
	@Override
	public void firePropertyChange(String propertyName, char oldValue, char newValue) {}
	@Override
	public void firePropertyChange(String propertyName, short oldValue, short newValue) {}
	@Override
	public void firePropertyChange(String propertyName, int oldValue, int newValue) {}
	@Override
	public void firePropertyChange(String propertyName, long oldValue, long newValue) {}
	@Override
	public void firePropertyChange(String propertyName, float oldValue, float newValue) {}
	@Override
	public void firePropertyChange(String propertyName, double oldValue, double newValue) {}
	@Override
	public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}

}

