package aephyr.swing.treetable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultRowSorter;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import aephyr.swing.event.TreeTableSorterListener;

public interface TreeTableSorter<T extends TreeModel, C extends TreeColumnModel> {
	
	public List<? extends SortKey> getSortKeys();

	public void setSortKeys(List<? extends SortKey> keys);

	public void toggleSortOrder(int column);
	
	/**
	 * Retrieves the RowSorter for the specified path,
	 * creates it if necessary.
	 * 
	 * @param path
	 * @return
	 * @see #getRowSorter(Object)
	 */
	public RowSorter<T> getRowSorter(TreePath path);

	/**
	 * Differs from the TreePath variety as it won't
	 * (lacks the necessary information) create
	 * the row sorter if it doesn't exist.
	 * 
	 * @param node
	 * @return
	 * @see #getRowSorter(TreePath)
	 */
	public RowSorter<T> getRowSorter(Object node);

	public void addTreeTableSorterListener(TreeTableSorterListener l);
	
	public void removeTreeTableSorterListener(TreeTableSorterListener l);
	

	public void setVisible(TreePath path, List<TreePath> subPaths, boolean visible);
	
	public void structureChanged(TreePath path);
	
	public void nodesRemoved(TreePath path, Object[] childNodes);
	
}

