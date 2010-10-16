package aephyr.swing.treetable;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;


public abstract class AbstractTreeModel implements TreeModel {
	
	protected EventListenerList listenerList = new EventListenerList();

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		listenerList.add(TreeModelListener.class, l);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		listenerList.remove(TreeModelListener.class, l);
	}


	public void fireNodeChanged(TreePath parentPath, int childIndex, Object childNode) {
		fireNodesChanged(listenerList, this, parentPath,
				new int[]{childIndex}, new Object[]{childNode});
	}
	
	public void fireNodesChanged(TreePath parentPath, int[] childIndices, Object[] childNodes) {
		fireNodesChanged(listenerList, this, parentPath,
				childIndices, childNodes);
	}
	
	public void fireNodeInserted(TreePath parentPath, int childIndex, Object childNode) {
		fireNodesInserted(listenerList, this, parentPath,
				new int[]{childIndex}, new Object[]{childNode});
	}
	
	public void fireNodesInserted(TreePath parentPath, int[] childIndices, Object[] childNodes) {
		fireNodesInserted(listenerList, this, parentPath,
				childIndices, childNodes);
	}
	
	public void fireNodeRemoved(TreePath parentPath, int childIndex, Object childNode) {
		fireNodesRemoved(listenerList, this, parentPath,
				new int[]{childIndex}, new Object[]{childNode});
	}
	
	public void fireNodesRemoved(TreePath parentPath, int[] childIndices, Object[] childNodes) {
		fireNodesRemoved(listenerList, this, parentPath,
				childIndices, childNodes);
	}
	
	public void fireTreeStructureChanged(TreePath parentPath) {
		fireTreeStructureChanged(listenerList, this, parentPath);
	}
	
	
	
	public static void fireNodesChanged(EventListenerList listenerList,
			TreeModel source, TreePath path, int[] childIndices, Object[] childNodes) {
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==TreeModelListener.class) {
				if (e == null)
					e = new TreeModelEvent(source, path, childIndices, childNodes);
				((TreeModelListener)listeners[i+1]).treeNodesChanged(e);
			}
		}
	}
	
	public static void fireNodesInserted(EventListenerList listenerList,
			TreeModel source, TreePath path, int[] childIndices, Object[] childNodes) {
		if (childIndices == null || childNodes == null)
			throw new NullPointerException();
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==TreeModelListener.class) {
				if (e == null)
					e = new TreeModelEvent(source, path, childIndices, childNodes);
				((TreeModelListener)listeners[i+1]).treeNodesInserted(e);
			}
		}
	}
	
	public static void fireNodesRemoved(EventListenerList listenerList,
			TreeModel source, TreePath path, int[] childIndices, Object[] childNodes) {
		if (childIndices == null || childNodes == null)
			throw new NullPointerException();
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==TreeModelListener.class) {
				if (e == null)
					e = new TreeModelEvent(source, path, childIndices, childNodes);
				((TreeModelListener)listeners[i+1]).treeNodesRemoved(e);
			}
		}
	}
	
	public static void fireTreeStructureChanged(EventListenerList listenerList,
			TreeModel source, TreePath path) {
		Object[] listeners = listenerList.getListenerList();
		TreeModelEvent e = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==TreeModelListener.class) {
				if (e == null)
					e = new TreeModelEvent(source, path, null, null);
				((TreeModelListener)listeners[i+1]).treeStructureChanged(e);
			}
		}
	}

}
