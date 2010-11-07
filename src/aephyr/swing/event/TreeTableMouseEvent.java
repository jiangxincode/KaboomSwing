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
	
	private int treePosition = Integer.MIN_VALUE;
	
	@Override
	public Point getPoint() {
		return new Point(getX(), getY());
	}
	
	/**
	 * @return the TreeTable source
	 */
	public TreeTable getTreeTable() {
		return (TreeTable)getSource();
	}
	
	/**
	 * @return the row for the location of this event
	 */
	public int getRow() {
		if (row == Integer.MIN_VALUE) {
			row = getTreeTable().rowAtPoint(getPoint());
		}
		return row;
	}
	
	/**
	 * @return the column for the location of this event
	 */
	public int getColumn() {
		if (column == Integer.MIN_VALUE) {
			column = getTreeTable().columnAtPoint(getPoint());
		}
		return column;
	}
	
	/**
	 * @return the path for the location of this event
	 */
	public TreePath getTreePath() {
		if (path == null) {
			path = getTreeTable().getClosestPathForLocation(getX(), getY());
		}
		return path;
	}
	
	/**
	 * Calculates the x distance from the tree handle. If the tree 
	 * handle isn't present, it is the distance from the start of the
	 * path bounds.
	 * <p>
	 * A return value of 0 means the location is over the tree handle.
	 * <p>
	 * The return value will be negative for x locations that
	 * fall in the leading region and positive for x locations
	 * that fall in the trailing region.
	 * 
	 * @return distance from the tree handle
	 */
	public int getRelativeTreePosition() {
		if (treePosition == Integer.MIN_VALUE) {
			TreeTable treeTable = getTreeTable();
			TreePath path = getTreePath();
			Rectangle nb = treeTable.getPathBounds(path);
			int x = getX();
			boolean ltr = treeTable.getComponentOrientation().isLeftToRight();
			if (ltr ? x < nb.x : x > nb.x + nb.width) {
				// leading margin/columns
				// Check if the node has a tree handle
				TreeModel tm = treeTable.getTreeTableModel();
				Object node = path.getLastPathComponent();
				boolean hasTreeHandle = !(tm.isLeaf(node) || (tm.getChildCount(node) <= 0
						&& !treeTable.hasBeenExpanded(path)));

				// Check if the event location falls over the tree handle.
				int thw = treeTable.getUI().getTreeHandleWidth(treeTable);
				if (hasTreeHandle && ltr ?
						x < nb.x && (thw < 0 || x > nb.x - thw) :
							x > nb.x + nb.width && (thw < 0
									|| x < nb.x + nb.width + thw)) {
					// over tree handle
					treePosition = 0;
				} else {
					treePosition = ltr ?
						x - nb.x :
						nb.x + nb.width - x;
					if (hasTreeHandle)
						treePosition += thw;
				}
			} else {
				// node & trailing margin/columns
				treePosition = ltr ?
					x - nb.x :
					nb.x + nb.width - x;
			}
		}
		return treePosition;
	}
	
	
	/**
	 * @return true if the location if over the leading margin
	 * 		of the node bounds and not over the tree handle
	 */
	public boolean isOverTreeMargin() {
		return getColumn() == getTreeTable().getHierarchicalColumn()
			&& getRelativeTreePosition() < 0;
	}
	
	/**
	 * @return true if the location is over the tree handle
	 */
	public boolean isOverTreeHandle() {
		return getRelativeTreePosition() == 0;
	}
	
	/**
	 * @return true if the location is over the path's bounds
	 */
	public boolean isOverTreeNode() {
		int pos = getRelativeTreePosition();
		if (pos <= 0)
			return false;
		Rectangle nb = getTreeTable().getPathBounds(getTreePath());
		return pos < nb.width;
	}


}
