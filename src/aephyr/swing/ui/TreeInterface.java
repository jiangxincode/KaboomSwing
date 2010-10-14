package aephyr.swing.ui;

import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;

import javax.swing.Scrollable;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public interface TreeInterface extends Scrollable {

	public void setModel(TreeModel model);
	
	
	
	public Enumeration<TreePath> getExpandedDescendants(TreePath parent);
	
	public void collapsePath(TreePath path);
	
	public void collapseRow(int row);
	
	public void expandPath(TreePath path);
	
	public void expandRow(int row);
	
	

	public Rectangle getPathBounds(TreePath path);

	public TreePath getPathForLocation(int x, int y);

	public TreePath getPathForRow(int row);

	public Rectangle getRowBounds(int row);

	public int getRowCount();

	public int getRowForLocation(int x, int y);

	public int getRowForPath(TreePath path);
	
	public boolean isCollapsed(int row);

	public boolean isCollapsed(TreePath path);

	public boolean isExpanded(int row);

	public boolean isExpanded(TreePath path);

	public boolean isFixedRowHeight();
	
	public boolean isLargeModel();
	
	public void setLargeModel(boolean largeModel);
	
	public boolean isRootVisible();

	public void setRootVisible(boolean rootVisible);

	public boolean getScrollsOnExpand();

	public boolean getShowsRootHandles();

	public void setShowsRootHandles(boolean newValue);

	public void setToggleClickCount(int clickCount);
	
	public int getToggleClickCount();

	public int getVisibleRowCount();

	public void setVisibleRowCount(int newCount);

	public int getRowHeight();

	public void setRowHeight(int rowHeight);
	
		
	public TreeSelectionModel getSelectionModel();

	public void setSelectionModel(TreeSelectionModel selectionModel);
	
	public void clearSelection();
	
	public boolean isSelectionEmpty();
	
	public int getSelectionCount();
	
	public int getMaxSelectionRow();

	public int getMinSelectionRow();

	public boolean isPathSelected(TreePath path);

	public boolean isRowSelected(int row);
	
	public TreePath getAnchorSelectionPath();
	
	public void setAnchorSelectionPath(TreePath newPath);
	
	public TreePath getLeadSelectionPath();

	public void setLeadSelectionPath(TreePath newPath);

	public int getLeadSelectionRow();
	
	public boolean getExpandsSelectedPaths();

	public void setExpandsSelectedPaths(boolean newValue);

	public TreePath getSelectionPath();
	
	public void addSelectionInterval(int index0, int index1);
	
	public void addSelectionPath(TreePath path);
	
	public void addSelectionPaths(TreePath[] paths);
	
	public void addSelectionRow(int row);
	
	public void addSelectionRows(int[] rows);
	
	public void removeSelectionInterval(int index0, int index1);
	
	public void removeSelectionPath(TreePath path);
	
	public void removeSelectionPaths(TreePath[] paths);
	
	public void removeSelectionRow(int row);
	
	public void removeSelectionRows(int[] rows);

	public void setSelectionPath(TreePath path);

	public TreePath[] getSelectionPaths();

	public void setSelectionPaths(TreePath[] paths);

	public int[] getSelectionRows();
	
	public void setSelectionRows(int[] rows);

	public void setSelectionRow(int row);

	public void setSelectionInterval(int index0, int index1);
	

	public void addPropertyChangeListener(PropertyChangeListener l);
	
	public void removePropertyChangeListener(PropertyChangeListener l);
	
	public void addTreeExpansionListener(TreeExpansionListener l);
	
	public void removeTreeExpansionListener(TreeExpansionListener l);
	
	public void addTreeWillExpandListener(TreeWillExpandListener l);
	
	public void removeTreeWillExpandListener(TreeWillExpandListener l);
	

	public void doLayout();
}
