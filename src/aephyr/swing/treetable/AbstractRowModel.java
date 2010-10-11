package aephyr.swing.treetable;

import javax.swing.event.EventListenerList;
import javax.swing.tree.TreePath;

import aephyr.swing.treetable.RowModel;
import aephyr.swing.event.RowModelEvent;
import aephyr.swing.event.RowModelListener;

public abstract class AbstractRowModel implements RowModel {

	protected EventListenerList listenerList = new EventListenerList();
	
	@Override
	public String getColumnName(int column) {
		String str = Character.toString((char)('A' + (column % 26)));
		while (column > 25) {
			column = column / 26 - 1;
			str += (char)('A' + (column % 26));
		}
		return str;
	}
	
	@Override
	public Class<?> getColumnClass(int column) {
		return Object.class;
	}
	
	@Override
	public void setValueAt(Object value, Object node, int column) {}
	
	@Override
	public boolean isCellEditable(Object node, int column) {
		return false;
	}
	
	@Override
	public int getHierarchialColumn() {
		return 0;
	}
	
	@Override
	public void addRowModelListener(RowModelListener l) {
		listenerList.add(RowModelListener.class, l);
	}

	@Override
	public void removeRowModelListener(RowModelListener l) {
		listenerList.remove(RowModelListener.class, l);
	}
	
	protected void fireRowChanged(TreePath path, int column) {
		fireRowChanged(listenerList, this, path, column);
	}
	
	public static void fireRowChanged(EventListenerList listenerList,
			RowModel source, TreePath path, int column) {
		Object[] listeners = listenerList.getListenerList();
		RowModelEvent e = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==RowModelListener.class) {
				if (e == null)
					e = new RowModelEvent(source, path, column);
				((RowModelListener)listeners[i+1]).rowChanged(e);
			}
		}
	}

}
