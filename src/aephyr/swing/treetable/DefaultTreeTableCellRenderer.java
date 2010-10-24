package aephyr.swing.treetable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import aephyr.swing.TreeTable;
import aephyr.swing.TreeTable.DropLocation;

public class DefaultTreeTableCellRenderer extends JLabel
		implements TreeTableCellRenderer, TableCellRenderer {

	public DefaultTreeTableCellRenderer() {
	}
	
	private TreeTable treeTable;
	
	@Override
	public Component getTreeTableCellRendererComponent(TreeTable treeTable,
			Object value, boolean selected, boolean hasFocus, int row, int column) {
		this.treeTable = treeTable;
		treeTable.getUI().configureCellRenderer(this, treeTable,
				value, selected, hasFocus, row, column);
		setValue(value);
		return this;
	}

	@Override
	public Component getTreeTableCellRendererComponent(TreeTable treeTable,
			Object value, boolean selected, boolean hasFocus, int row,
			int column, boolean expanded, boolean leaf) {
		this.treeTable = treeTable;
		treeTable.getUI().configureCellRenderer(this, treeTable,
				value, selected, hasFocus, row, column, expanded, leaf);
		setValue(value);
		return this;
	}
	
	protected void setValue(Object value) {
		setText(value == null ? "" : value.toString());
	}
	
	public Color getTextSelectionColor() {
		return treeTable.getSelectionForeground();
	}
	
	public Color getTextNonSelectionColor() {
		return treeTable.getForeground();
	}
	
	public Color getBackgroundSelectionColor() {
		return treeTable.getSelectionBackground();
	}
	
	public Color getBackgroundNonSelectionColor() {
		return treeTable.getBackground();
	}
	
	
//	private Color unselectedForeground;
//	
//	private Color unselectedBackground;
//	
//	public void setForeground(Color fg) {
//		super.setForeground(fg);
//		unselectedForeground = fg;
//	}
//	
//	public void setBackground(Color bg) {
//		super.setBackground(bg);
//		unselectedBackground = bg;
//	}

	
//	private static Border ltrBorder;
//	
//	private static Border rtlBorder;
//	
//	private static Border getLTRBorder() {
//		if (ltrBorder == null)
//			ltrBorder = new EmptyBorder(1, 0, 1, 1);
//		return ltrBorder;
//	}
//	
//	private static Border getRTLBorder() {
//		if (rtlBorder == null)
//			rtlBorder = new EmptyBorder(1, 1, 1, 0);
//		return rtlBorder;
//	}
//	
//	private static Border noFocusBorder;
//	
//	private static Border getNoFocusBorder() {
//		if (noFocusBorder == null)
//			noFocusBorder = new EmptyBorder(1, 1, 1, 1);
//		return noFocusBorder;
//	}
	
	
//	@Override
//	public final Component getTreeCellRendererComponent(JTree tree, Object value,
//			boolean selected, boolean expanded, boolean leaf, int row,
//			boolean hasFocus) {
//		if (tree != null) {
//			setOpaque(false);
//			
//			Color fg;
//			if (selected) {
//				fg = getTextSelectionColor();
//			} else {
//				fg = getTextNonSelectionColor();
//			}
//			super.setForeground(fg);
//
//			Icon icon = treeTable.getIconForRow(row, expanded, leaf);
//			if (!tree.isEnabled()) {
//				setEnabled(false);
//				setDisabledIcon(icon);
//			}
//			else {
//				setEnabled(true);
//				setIcon(icon);
//			}
//			setComponentOrientation(tree.getComponentOrientation());
//			setBorder(getComponentOrientation().isLeftToRight() ?
//					getLTRBorder() : getRTLBorder());
//			setFont(treeTable.getFont());
//		}
//		setValue(value);
//		return this;
//	}
	
//	@Override
//	public final Component getTableCellRendererComponent(JTable table,
//			Object value, boolean isSelected, boolean hasFocus,
//			int row, int column) {
//		if (table != null) {
//			setOpaque(true);
//			setIcon(null);
//			setDisabledIcon(null);
//
//			Color fg = null;
//			Color bg = null;
//
//			DropLocation dropLocation = treeTable.getDropLocation();
//			if (dropLocation != null
//					&& !dropLocation.isInsertRow()
//					&& !dropLocation.isInsertColumn()
//					&& dropLocation.getRow() == row
//					&& dropLocation.getColumn() == column) {
//
//				fg = UIManager.getColor("Table.dropCellForeground");
//				bg = UIManager.getColor("Table.dropCellBackground");
//
//				isSelected = true;
//			}
//
//			if (isSelected) {
//				super.setForeground(fg == null ?
//						table.getSelectionForeground() : fg);
//				super.setBackground(bg == null ?
//						table.getSelectionBackground() : bg);
//			} else {
//				super.setForeground(unselectedForeground != null
//						? unselectedForeground
//								: table.getForeground());
//				super.setBackground(unselectedBackground != null
//						? unselectedBackground
//								: table.getBackground());
//			}
//
//			setFont(table.getFont());
//
//			if (hasFocus) {
//				Border border = null;
//				if (isSelected) {
//					border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");
//				}
//				if (border == null) {
//					border = UIManager.getBorder("Table.focusCellHighlightBorder");
//				}
//				setBorder(border);
//
//				if (!isSelected && table.isCellEditable(row, column)) {
//					Color col;
//					col = UIManager.getColor("Table.focusCellForeground");
//					if (col != null) {
//						super.setForeground(col);
//					}
//					col = UIManager.getColor("Table.focusCellBackground");
//					if (col != null) {
//						super.setBackground(col);
//					}
//				}
//			} else {
//				setBorder(getNoFocusBorder());
//			}
//		}
////		setValue(value);
//		return this;
//	}
	
	
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

}

