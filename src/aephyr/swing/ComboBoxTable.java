package aephyr.swing;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

public class ComboBoxTable extends JTable {
	
	public ComboBoxTable(TableModel model, int rows) {
		rowCount = rows;
		int [] vtm = new int[Math.min(rows, model.getRowCount())];
		for (int i=vtm.length; --i>=0;)
			vtm[i] = i;
		viewToModel = vtm;
		comboEditor = new DefaultCellEditor(new JComboBox(new Model()));
	}
	
	private int[] viewToModel;
	
	private int rowCount;
	
	private int comboColumn;
	
	public void setRowCount(int rows) {
		rowCount = rows;
	}
	
	@Override
	public int getRowCount() {
		System.out.println("getRowCount " + viewToModel.length);
		return viewToModel.length;
	}
	
	@Override
	public int convertRowIndexToView(int row) {
		System.out.println("converRowIndexToView " + row);
		return row;
	}
	
	@Override
	public int convertRowIndexToModel(int row) {
		System.out.println("converRowIndexToModel " + row);
		if (row < 0 || row >= viewToModel.length)
			return -1;
		return viewToModel[row];
	}
	
	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		if (convertColumnIndexToModel(column) == comboColumn) {
			return comboEditor;
		}
		return super.getCellEditor(row, column);
	}
	
	@Override
	public boolean isCellEditable(int row, int column) {
		if (convertColumnIndexToModel(column) == comboColumn)
			return true;
		return super.isCellEditable(row, column);
	}
	
	@Override
	public void setAutoCreateRowSorter(boolean val) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setRowSorter(RowSorter<? extends TableModel> sorter) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void tableChanged(TableModelEvent e) {
		super.tableChanged(e);
	}
	
	public Object getValueAt(int row, int col) {
		System.out.println("getValueAt " + row + " " + col);
		return super.getValueAt(row, col);
	}
	
	
	private DefaultCellEditor comboEditor;
	
	private class Model extends AbstractListModel implements ComboBoxModel {

		Object selected;
		
		@Override
		public Object getSelectedItem() {
			return selected;
		}

		@Override
		public void setSelectedItem(Object anItem) {
			selected = anItem;
			System.out.println("set selected item " + anItem);
		}

		@Override
		public Object getElementAt(int index) {
			return dataModel.getValueAt(index, comboColumn);
		}

		@Override
		public int getSize() {
			return dataModel.getRowCount();
		}
		
	}
}
