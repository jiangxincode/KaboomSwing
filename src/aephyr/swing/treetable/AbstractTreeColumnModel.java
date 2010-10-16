package aephyr.swing.treetable;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.swing.event.EventListenerList;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import aephyr.swing.treetable.TreeColumnModel;
import aephyr.swing.event.TreeColumnModelEvent;
import aephyr.swing.event.TreeColumnModelListener;

public abstract class AbstractTreeColumnModel implements TreeColumnModel {

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
	
	protected Object convertValue(Object value, Object node, int column) {
		try {
			return convertValue(value, getColumnClass(column));
		} catch (NumberFormatException e) {
			return convertValue(value, node, column, e);
		}
	}
	
	protected Object convertValue(Object value, Object node, int column,
			NumberFormatException e) {
		return value;
	}
	
	@Override
	public boolean isCellEditable(Object node, int column) {
		return false;
	}
	
	@Override
	public int getHierarchialColumn() {
		return 0;
	}
	
	@Override
	public void addTreeColumnModelListener(TreeColumnModelListener l) {
		listenerList.add(TreeColumnModelListener.class, l);
	}

	@Override
	public void removeTreeColumnModelListener(TreeColumnModelListener l) {
		listenerList.remove(TreeColumnModelListener.class, l);
	}
	
	protected void fireRowChanged(TreePath path, int column) {
		fireRowChanged(listenerList, this, path, column);
	}
	
	public static void fireRowChanged(EventListenerList listenerList,
			TreeColumnModel source, TreePath path, int column) {
		Object[] listeners = listenerList.getListenerList();
		TreeColumnModelEvent e = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==TreeColumnModelListener.class) {
				if (e == null)
					e = new TreeColumnModelEvent(source, path, column);
				((TreeColumnModelListener)listeners[i+1]).treeColumnChanged(e);
			}
		}
	}
	
	
	public static TreePath pathToRoot(Object root, TreeNode node) {
		if (node == root)
			return new TreePath(node);
		return pathToRoot(root, node.getParent()).pathByAddingChild(node);
	}
	
	public static Object convertValue(Object value, Class<?> cls)
			throws NumberFormatException {
		// short-circuit String columns
		if (cls == Object.class || cls == String.class)
			return value;
		if (cls == Boolean.class) {
			if (value instanceof Boolean)
				return value;
			return value == null ? Boolean.FALSE :
					Boolean.valueOf(value.toString());
		} else if (Number.class.isAssignableFrom(cls)) {
			if (value instanceof String) {
				if (cls == Integer.class) {
					value = Integer.valueOf((String)value);
				} else if (cls == Long.class) {
					value = Long.valueOf((String)value);
				} else if (cls == Float.class) {
					value = Float.valueOf((String)value);
				} else if (cls == Double.class) {
					value = Double.valueOf((String)value);
				} else if (cls == BigInteger.class) {
					return new BigInteger((String)value);
				} else if (cls == BigDecimal.class) {
					return new BigDecimal((String)value);
				} else if (cls == Short.class) {
					value = Short.valueOf((String)value);
				} else if (cls == Byte.class) {
					value = Byte.valueOf((String)value);
				}
			}
		}
		return value;
	}
	


}
