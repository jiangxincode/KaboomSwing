package aephyr.swing.treetable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultRowSorter;
import javax.swing.tree.TreeModel;


public class TreeTableSorter<T extends TreeModel, C extends TreeColumnModel, I>
		extends DefaultRowSorter<T, I> {
	
	public TreeTableSorter(T tm, C cm) {
		this(null, tm, cm, tm.getRoot());
	}

	public TreeTableSorter(TreeTableSorter<T,C,I> par, T tm, C cm, Object node) {
		parent = par;
		setModelWrapper(new TreeTableWrapper<T,C,I>(tm, cm, node));
		children = createChildren();
	}
	
	private TreeTableSorter<T,C,I> parent;

	private Map<Object,TreeTableSorter<T,C,I>> children;
	
	private boolean visible;
	
	protected Map<Object,TreeTableSorter<T,C,I>> createChildren() {
		return new IdentityHashMap<Object,TreeTableSorter<T,C,I>>(
				getModel().getChildCount(getNode()));
	}
	
	public TreeTableSorter<T,C,I> getChildSorter(Object node) {
		TreeTableSorter<T,C,I> s = children.get(node);
		if (s == null) {
			s = new TreeTableSorter<T,C,I>(this, getModel(), getColumnModel(), node);
			children.put(node, s);
		}
		return s;
	}
	
	protected TreeTableWrapper<T,C,I> getTreeTableModelWrapper() {
		return (TreeTableWrapper<T,C,I>)getModelWrapper();
	}
	
	public Object getNode() {
		return getTreeTableModelWrapper().getNode();
	}
	
	public C getColumnModel() {
		return getTreeTableModelWrapper().getColumnModel();
	}
	
	@Override
	public Comparator<?> getComparator(int column) {
		return parent == null ? super.getComparator(column)
				: parent.getComparator(column);
	}

	@Override
	public List<? extends SortKey> getSortKeys() {
		return parent == null ? super.getSortKeys()
				: parent.getSortKeys();
	}

	@Override
	public int getMaxSortKeys() {
		return parent == null ? super.getMaxSortKeys() :
			parent.getMaxSortKeys();

	}

	@Override
	public boolean getSortsOnUpdates() {
		return parent == null ? super.getSortsOnUpdates() :
			parent.getSortsOnUpdates();
	}

	
	@Override
	protected void fireRowSorterChanged(int[] lastRowIndexToModel) {
		if (children != null) {
			for (TreeTableSorter<T,C,I> sorter : children.values()) {
				sorter.sort();
			}
		}
		super.fireRowSorterChanged(lastRowIndexToModel);
	}
	
	public void allRowsChanged() {
		getTreeTableModelWrapper().updateRowCount();
		super.allRowsChanged();
	}
	
	public void rowsDeleted(int firstRow, int endRow) {
		getTreeTableModelWrapper().updateRowCount();
		super.rowsDeleted(firstRow, endRow);
	}
	
	public void rowsInserted(int firstRow, int endRow) {
		getTreeTableModelWrapper().updateRowCount();
		super.rowsInserted(firstRow, endRow);
	}
	

	public void setVisible(boolean vis) {
		if (visible != vis) {
			visible = vis;
			if (vis)
				sort();
		}
	}

	public boolean isVisible() {
		return visible;
	}

	/**
	 * Notifies the TreeTableSorter that it should discard the specified children.
	 * 
	 * @param childNodes the nodes to remove
	 * @return a list of all [great...] grand children
	 */
	public List<Object> nodesRemoved(Object[] childNodes) {
		if (children != null) {
			ArrayList<Object> list = new ArrayList<Object>();
			for (Object node : childNodes) {
				TreeTableSorter<T,C,I> sorter = children.remove(node);
				if (sorter != null)
					list.addAll(sorter.removeAllChildren());
			}
			return list;
		}
		return Collections.emptyList();
	}

	/**
	 * Notifies the TreeTableSorter that it should discard all nested children.
	 * 
	 * @return list of all nested children
	 */
	public List<Object> removeAllChildren() {
		if (children != null) {
			ArrayList<Object> list = new ArrayList<Object>(children.size());
			for (Map.Entry<Object,TreeTableSorter<T,C,I>> entry : children.entrySet()) {
				list.add(entry.getKey());
				list.addAll(entry.getValue().removeAllChildren());
			}
			children = null;
			return list;
		}
		return Collections.emptyList();
	}
	

	protected static class TreeTableWrapper<T extends TreeModel, C extends TreeColumnModel, I> extends ModelWrapper<T, I> {

		TreeTableWrapper(T tm, C cm, Object n) {
			treeModel = tm;
			columnModel = cm;
			node = n;
			updateRowCount();
		}

		private T treeModel;

		private C columnModel;

		private Object node;
		
		private int rowCount;
		
		public Object getNode() {
			return node;
		}
		
		public C getColumnModel() {
			return columnModel;
		}

		@Override
		public int getColumnCount() {
			return columnModel.getColumnCount();
		}

		@Override
		public I getIdentifier(int row) {
			return (I)treeModel.getChild(node, row);
		}

		@Override
		public T getModel() {
			return treeModel;
		}

		@Override
		public int getRowCount() {
			return rowCount;
		}
		
		/**
		 * The last row count must be cached until
		 * this method is called to update it.
		 */
		public void updateRowCount() {
			rowCount = treeModel.getChildCount(node);
		}

		@Override
		public Object getValueAt(int row, int column) {
			return columnModel.getValueAt(treeModel.getChild(node, row), column);
		}

	}

	
}
