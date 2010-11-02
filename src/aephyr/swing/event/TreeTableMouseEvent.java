package aephyr.swing.event;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import aephyr.swing.TreeTable;

public class TreeTableMouseEvent extends MouseEvent {

	public TreeTableMouseEvent(TreeTable source, MouseEvent e) {
		super(source, e.getID(), e.getWhen(), e.getModifiers(),
				e.getX(), e.getY(), e.getXOnScreen(), e.getYOnScreen(),
				e.getClickCount(), e.isPopupTrigger(), e.getButton());
	}
	
	private TreePath path = null;
	
	private int row = Integer.MIN_VALUE;
	
	private int column = Integer.MIN_VALUE;
	
	private int treeLocation = Integer.MIN_VALUE;
	
	@Override
	public Point getPoint() {
		return new Point(getX(), getY());
	}
	
	public TreeTable getTreeTable() {
		return (TreeTable)getSource();
	}
	
	public int getRow() {
		if (row == Integer.MIN_VALUE) {
			row = getTreeTable().rowAtPoint(getPoint());
		}
		return row;
	}
	
	public int getColumn() {
		if (column == Integer.MIN_VALUE) {
			column = getTreeTable().columnAtPoint(getPoint());
		}
		return column;
	}
	
	public TreePath getTreePath() {
		if (path == null) {
			path = getTreeTable().getClosestPathForLocation(getX(), getY());
		}
		return path;
	}
	
	private int getTreeLocation() {
		if (treeLocation == Integer.MIN_VALUE) {
			treeLocation = -1;
			TreeTable treeTable = getTreeTable();
			if (getColumn() == treeTable.getHierarchialColumn()) {
				Rectangle nodeBounds = treeTable.getPathBounds(getTreePath());
				if (nodeBounds.contains(getX(), getY())) {
					treeLocation = 1;
				} else if (overTreeHandle(treeTable, getTreePath(), nodeBounds, getX())) {
					treeLocation = 0;
				}
			}
		}
		return treeLocation;
	}
	
	public boolean isOverTreeHandle() {
		return getTreeLocation() == 0;
	}
	
	public boolean isOverTreeNode() {
		return getTreeLocation() == 1;
	}

	
	private static boolean overTreeHandle(TreeTable treeTable,
			TreePath path, Rectangle nodeBounds, int x) {
		TreeModel tm = treeTable.getTreeTableModel();
		Object node = path.getLastPathComponent();
		// Check if the node has a tree handle
		if (tm.isLeaf(node) || (tm.getChildCount(node) <= 0
				&& !treeTable.hasBeenExpanded(path)))
			return false;
		// Check if the event location falls over the tree handle.
		Rectangle nb = nodeBounds;
		int thw = treeTable.getUI().getTreeHandleWidth(treeTable);
		return treeTable.getComponentOrientation().isLeftToRight() ?
				x < nb.x && (thw < 0 || x > nb.x - thw) :
					x > nb.x + nb.width && (thw < 0
							|| x < nb.x + nb.width + thw);
	}
	
}
