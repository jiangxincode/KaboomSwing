package aephyr.swing.event;

import java.util.EventObject;
import javax.swing.tree.TreePath;
import aephyr.swing.treetable.TreeTableSorter;

public class TreeTableSorterEvent extends EventObject {
	
	public enum Type {
		SORT_ORDER_CHANGED, SORTED, NODE_SORTED
	}
	
	public TreeTableSorterEvent(TreeTableSorter<?,?> source) {
		super(source);
		type = Type.SORT_ORDER_CHANGED;
	}
	
	public TreeTableSorterEvent(TreeTableSorter<?,?> source, TreePath path) {
		super(source);
		type = path == null ? Type.SORTED : Type.NODE_SORTED;
		this.path = path;
	}
	
	private Type type;
	
	private TreePath path;
	
	public Type getType() {
		return type;
	}
	
	public TreePath getTreePath() {
		return path;
	}
	
	public TreeTableSorter<?,?> getSource() {
		return (TreeTableSorter<?,?>)super.getSource();
	}
	
}
