package aephyr.swing.nimbus;

import java.awt.Container;
import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

class UITable extends JTable implements ComponentListener {
	
	UITable(TableModel mdl, TableColumnModel clm) {
		super(mdl, clm);
	}
	
	
	@Override
	public void addNotify() {
		super.addNotify();
		Container parent = getParent();
		if (parent instanceof JViewport) {
			parent.addComponentListener(this);
		}
	}
	
	@Override
	public void removeNotify() {
		super.removeNotify();
		Container parent = getParent();
		if (parent instanceof JViewport) {
			parent.removeComponentListener(this);
		}
	}
	
	/**
	 * Overridden to supply hasFocus as false to the renderers
	 * but still allow the table to be focusable.
	 */
	@Override
	public Component prepareRenderer(TableCellRenderer renderer,
			int row, int column) {
		Object value = getValueAt(row, column);
		boolean isSelected = false;
		// Only indicate the selection and focused cell if not printing
		if (!isPaintingForPrint()) {
			isSelected = isCellSelected(row, column);
		}
		return renderer.getTableCellRendererComponent(
				this, value, isSelected, false, row, column);
	}

	@Override
	public void componentHidden(ComponentEvent e) {}

	@Override
	public void componentMoved(ComponentEvent e) {}

	
	@Override
	public void componentResized(ComponentEvent e) {
		JViewport port = (JViewport)e.getSource();
		TableColumnModel columns = getColumnModel();
		int keyColumnIndex = convertColumnIndexToView(UITableModel.KEY_COLUMN_INDEX);
		int width = port.getWidth();
		for (int col=columns.getColumnCount(); --col>=0;) {
			if (col != keyColumnIndex)
				width -= columns.getColumn(col).getWidth();
		}
		if (width < 210)
			width = 210;
		TableColumn col = columns.getColumn(keyColumnIndex);
		if (width != col.getPreferredWidth()) {
			col.setMinWidth(width);
			col.setPreferredWidth(width);
		}
	}

	@Override
	public void componentShown(ComponentEvent e) {}
}
