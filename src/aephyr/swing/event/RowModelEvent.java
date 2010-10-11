package aephyr.swing.event;

import java.util.EventObject;

import javax.swing.event.TableModelEvent;
import javax.swing.tree.TreePath;

import aephyr.swing.treetable.RowModel;

public class RowModelEvent extends EventObject {

	public static final int ALL_COLUMNS = TableModelEvent.ALL_COLUMNS;
	
	public RowModelEvent(RowModel source, TreePath path, int column) {
		super(source);
		this.path = path;
		this.column = column;
	}
	
	private TreePath path;
	
	private int column;
	
	public TreePath getTreePath() {
		return path;
	}
	
	public int getColumn() {
		return column;
	}

}
