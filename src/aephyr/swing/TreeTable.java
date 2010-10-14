package aephyr.swing;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.UIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import aephyr.swing.event.TreeColumnModelEvent;
import aephyr.swing.event.TreeColumnModelListener;
import aephyr.swing.treetable.DefaultTreeColumnModel;
import aephyr.swing.treetable.DefaultTreeTableNode;
import aephyr.swing.treetable.TreeColumnModel;
import aephyr.swing.treetable.TreeTableCellEditor;
import aephyr.swing.treetable.TreeTableCellRenderer;
import aephyr.swing.treetable.TreeTableNode;
import aephyr.swing.ui.BasicTreeTableUI;
import aephyr.swing.ui.TableInterface;
import aephyr.swing.ui.TreeInterface;
import aephyr.swing.ui.TreeTableUI;


public class TreeTable extends JComponent implements Scrollable {

	public TreeTable() {
		this(new DefaultTreeTableNode());
	}

	public TreeTable(TreeTableNode root) {
		this(new DefaultTreeModel(root), new DefaultTreeColumnModel(root));
	}
	
    public TreeTable(TreeModel tm, TreeColumnModel rm) {
    	this(tm, rm, null);
    }
    
	public TreeTable(TreeModel tm, TreeColumnModel rm, TableColumnModel cm) {
		if (tm == null || rm == null)
			throw new NullPointerException();
		treeModel = tm;
		rowModel = rm;
		columnModel = cm;
		adapter = createAdapter(tm.getRoot());
		setFocusable(true);
		updateUI();
		tm.addTreeModelListener(adapter);
		rm.addTreeColumnModelListener(adapter);
	}
	
	private TreeInterface tree;
	
	private TableInterface table;
	
	private TreeModel treeModel;
	
	private TreeColumnModel rowModel;
	
	private TableColumnModel columnModel;
	
	private Adapter adapter;
	
	private TreeTableCellRenderer focusRenderer;
	
	private Icon openIcon;
	
	private Icon closedIcon;
	
	private Icon leafIcon;
	
	private HashMap<Class<?>,TreeTableCellRenderer> defaultRenderers;
	
	private HashMap<Class<?>,TreeTableCellEditor> defaultEditors;
	
	private boolean columnFocusEnabled = true;

	private boolean autoCreateTableHeader = true;
	

	private static final String uiClassID = "TreeTableUI";

	@Override
	public String getUIClassID() {
		return uiClassID;
	}
	
	public void setUI(TreeTableUI ui) {
		super.setUI(ui);
	}
	
	public TreeTableUI getUI() {
		return (TreeTableUI)ui;
	}
	
	@Override
	public void updateUI() {
		if (table != null) {
			table.removePropertyChangeListener(adapter);
			tree.removePropertyChangeListener(adapter);
			getSelectionModel().removeTreeSelectionListener(adapter);
			if (getAutoCreateColumnsFromModel())
				columnModel = null;
		}
		setUI(UIManager.get(getUIClassID()) != null ?
				(TreeTableUI)UIManager.getUI(this) :
				new BasicTreeTableUI());
		tree = getUI().getTreeInterface(this);
		table = getUI().getTableInterface(this);
		if (columnModel == null) {
			columnModel = table.getColumnModel();
			int hc = getRowModel().getHierarchialColumn();
			if (hc >= 0)
				columnModel.getColumn(hc).setPreferredWidth(150);
		}
		getSelectionModel().addTreeSelectionListener(adapter);
		tree.addPropertyChangeListener(adapter);
		table.addPropertyChangeListener(adapter);
	}
	
	
	public void processTreeExpansion(int row, int rowsAdded) {
		adapter.fireTableRowsInserted(row+1, row+rowsAdded);
	}
	
	public void processTreeCollapse(int row, int rowsRemoved) {
		adapter.fireTableRowsDeleted(row+1, row-rowsRemoved);
	}

	
	@Override
	public void addNotify() {
		super.addNotify();
		if (getAutoCreateTableHeader())
			configureEnclosingScrollPane();
	}
	
	@Override
	public void removeNotify() {
		super.removeNotify();
		if (getAutoCreateTableHeader())
			unconfigureEnclosingScrollPane();
	}


	// JTable.configureEnclosingScrollPane...
	protected void configureEnclosingScrollPane() {
		Container p = getParent();
		if (p instanceof JViewport) {
			Container gp = p.getParent();
			if (gp instanceof JScrollPane) {
				JScrollPane scrollPane = (JScrollPane)gp;
				// Make certain we are the viewPort's view and not, for
				// example, the rowHeaderView of the scrollPane -
				// an implementor of fixed columns might do this.
				JViewport viewport = scrollPane.getViewport();
				if (viewport == null || viewport.getView() != this) {
					return;
				}
				scrollPane.setColumnHeaderView(getTableHeader());
				//  scrollPane.getViewport().setBackingStoreEnabled(true);
				Border border = scrollPane.getBorder();
				if (border == null || border instanceof UIResource) {
					Border scrollPaneBorder = 
						UIManager.getBorder("Table.scrollPaneBorder");
					if (scrollPaneBorder != null) {
						scrollPane.setBorder(scrollPaneBorder);
					}
				}
			}
		}
	}

	// JTable.unconfigureEnclosingScrollPane...
	protected void unconfigureEnclosingScrollPane() {
		Container p = getParent();
		if (p instanceof JViewport) {
			Container gp = p.getParent();
			if (gp instanceof JScrollPane) {
				JScrollPane scrollPane = (JScrollPane)gp;
				// Make certain we are the viewPort's view and not, for
				// example, the rowHeaderView of the scrollPane -
				// an implementor of fixed columns might do this.
				JViewport viewport = scrollPane.getViewport();
				if (viewport == null || viewport.getView() != this) {
					return;
				}
				scrollPane.setColumnHeaderView(null);
			}
		}
	}
	
	private <T extends EventListener> void addListener(Class<T> cls, T l) {
		if (l == null)
			return;
		int count = listenerList.getListenerCount(cls);
		listenerList.add(cls, l);
		if (count == 0) {
			if (cls == TreeExpansionListener.class) {
				tree.addTreeExpansionListener(adapter);
			} else if (cls == TreeWillExpandListener.class) {
				tree.addTreeWillExpandListener(adapter);
			}
		}
	}
	
	private <T extends EventListener> void removeListener(Class<T> cls, T l) {
		if (l == null)
			return;
		listenerList.remove(cls, l);
		int count = listenerList.getListenerCount(cls);
		if (count == 0) {
			if (cls == TreeExpansionListener.class) {
				tree.removeTreeExpansionListener(adapter);
			} else if (cls == TreeWillExpandListener.class) {
				tree.removeTreeWillExpandListener(adapter);
			}
		}
	}
	
	public void addTreeExpansionListener(TreeExpansionListener l) {
		addListener(TreeExpansionListener.class, l);
	}
	
	public void removeTreeExpansionListener(TreeExpansionListener l) {
		removeListener(TreeExpansionListener.class, l);
	}
	
	private void fireTreeExpansionEvent(TreePath path, boolean exp) {
		TreeExpansionEvent e = null;
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==TreeExpansionListener.class) {
				if (e == null)
					e = new TreeExpansionEvent(this, path);
				TreeExpansionListener lis = (TreeExpansionListener)listeners[i+1];
				if (exp) {
					lis.treeExpanded(e);
				} else {
					lis.treeCollapsed(e);
				}
			}
		}
	}
	
	public void addTreeWillExpandListener(TreeWillExpandListener l) {
		addListener(TreeWillExpandListener.class, l);
	}
	
	public void removeTreeWillExpandListener(TreeWillExpandListener l) {
		removeListener(TreeWillExpandListener.class, l);
	}
	
	private void fireTreeWillExpandEvent(TreePath path, boolean exp) throws ExpandVetoException {
		TreeExpansionEvent e = new TreeExpansionEvent(this, path);
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==TreeWillExpandListener.class) {
				TreeWillExpandListener lis = (TreeWillExpandListener)listeners[i+1];
				if (exp) {
					lis.treeWillExpand(e);
				} else {
					lis.treeWillCollapse(e);
				}
			}
		}
	}
	
	
	protected void resizeAndRepaint() {
		revalidate();
		repaint();
	}
	
	@Override
	public void doLayout() {
		table.doLayout();
		tree.doLayout();
		super.doLayout();
	}
	
	
	
	public boolean getAutoCreateTableHeader() {
		return autoCreateTableHeader;
	}

	public void setAutoCreateTableHeader(boolean autoCreateTableHeader) {
		boolean oldValue = getAutoCreateTableHeader();
		this.autoCreateTableHeader = autoCreateTableHeader;
		firePropertyChange("autoCreateTableHeader", oldValue, getAutoCreateTableHeader());
	}
	
	
	
	
	public TreeTableCellRenderer getFocusRenderer() {
		return focusRenderer;
	}
	
	public void setFocusRenderer(TreeTableCellRenderer renderer) {
		TreeTableCellRenderer oldValue = getFocusRenderer();
		focusRenderer = renderer;
		this.firePropertyChange("focusRenderer", oldValue, getFocusRenderer());
		if (isValid())
			repaint(getLeadSelectionPath());
	}
	
	
	
	
	public TableColumnModel getColumnModel() {
		return columnModel;
	}

	public void setColumnModel(TableColumnModel columnModel) {
		TableColumnModel oldValue = getColumnModel();
		this.columnModel = columnModel;
		table.setColumnModel(getColumnModel());
		firePropertyChange("columnModel", oldValue, getColumnModel());
	}

	
	
	public TreeModel getTreeModel() {
		return treeModel;
	}
	
	public void setTreeModel(TreeModel treeModel) {
		TreeModel oldValue = getTreeModel();
		this.treeModel = treeModel;
		tree.setModel(getTreeModel());
		firePropertyChange("treeModel", oldValue, getTreeModel());
	}
	
	public TreeColumnModel getRowModel() {
		return rowModel;
	}
	
	public void setRowModel(TreeColumnModel rowModel) {
		TreeColumnModel oldValue = getRowModel();
		this.rowModel = rowModel;
		adapter.fireTableStructureChanged();
		firePropertyChange("rowModel", oldValue, getRowModel());
	}

	
	/**
	 * Changes to the TreeModel/RowModel as well as row insertion/removal
	 * due to tree expansion/collapse can be listened to in terms of a 
	 * TableModelListener by adding the TableModelListener to this TableModel.
	 * 
	 * @return TableModel view of the TreeModel/RowModel combination.
	 * @see #getTreeModel()
	 * @see #getRowModel()
	 */
	public TableModel getTableModel() {
		return adapter;
	}


	/**
	 * @return columnFocusEnabed
	 * @see #setColumnFocusEnabled(boolean)
	 */
	public boolean isColumnFocusEnabled() {
		return columnFocusEnabled;
	}
	
	/**
	 * If false, the focus is draw around the entire focused row.
	 * 
	 * @param columnFocusEnabled
	 */
	public void setColumnFocusEnabled(boolean columnFocusEnabled) {
		boolean oldValue = isColumnFocusEnabled();
		this.columnFocusEnabled = columnFocusEnabled;
		firePropertyChange("columnFocusEnabled", oldValue, isColumnFocusEnabled());
		repaint(getLeadSelectionPath());
	}
	
	
	
	
	public void setDefaultRenderer(Class<?> columnClass, TreeTableCellRenderer renderer) {
		defaultRenderers = putDefault(columnClass, renderer, defaultRenderers);
	}

	public TreeTableCellRenderer getDefaultRenderer(Class<?> columnClass) {
		return defaultRenderers == null ? null :
			getDefault(columnClass, defaultRenderers);
	}
	
	public void setDefaultEditor(Class<?> columnClass, TreeTableCellEditor editor) {
		defaultEditors = putDefault(columnClass, editor, defaultEditors);
	}
	
	public TreeTableCellEditor getDefaultEditor(Class<?> columnClass) {
		return defaultEditors == null ? null :
			getDefault(columnClass, defaultEditors);
	}
	
	private static <T> HashMap<Class<?>,T> putDefault(
			Class<?> key, T value, HashMap<Class<?>,T> defaults) {
		if (defaults == null) {
			if (value == null)
				return null;
			defaults = new HashMap<Class<?>,T>(8);
		}
		if (value != null) {
			defaults.put(key, value);
		} else {
			defaults.remove(key);
		}
		return defaults;
	}
	
	private static <T> T getDefault(Class<?> cls, HashMap<Class<?>,T> defaults) {
		if (cls == null)
			return null;
		T def = defaults.get(cls);
		return def != null ? def : getDefault(cls.getSuperclass(), defaults);
	}


	public TreeTableCellRenderer getCellRenderer(int row, int column) {
		if (column < 0 || column >= getColumnModel().getColumnCount())
			return null;
		TableCellRenderer renderer = getColumnModel()
				.getColumn(column).getCellRenderer();
		if (renderer instanceof TreeTableCellRenderer)
			return (TreeTableCellRenderer)renderer;
		return defaultRenderers == null ? null :
			getDefaultRenderer(getRowModel().getColumnClass(
				convertColumnIndexToModel(column)));
	}
	
	public TreeTableCellEditor getCellEditor(int row, int column) {
		if (column < 0 || column >= getColumnModel().getColumnCount())
			return null;
		TableCellEditor editor = getColumnModel()
			.getColumn(column).getCellEditor();
		if (editor instanceof TreeTableCellEditor)
			return (TreeTableCellEditor)editor;
		return defaultEditors == null ? null :
			getDefaultEditor(getRowModel().getColumnClass(
					convertColumnIndexToModel(column)));
	}

	
	/**
	 * @return the hierarchial column in view coordinates
	 */
	public int getHierarchialColumn() {
		return convertColumnIndexToView(
				getRowModel().getHierarchialColumn());
	}
	
	public int convertColumnIndexToView(int modelColumnIndex) {
		return table.convertColumnIndexToView(modelColumnIndex);
	}
	
	public int convertColumnIndexToModel(int viewColumnIndex) {
		return table.convertColumnIndexToModel(viewColumnIndex);
	}
	
	

	public Icon getLeafIcon() {
		return leafIcon;
	}
	
	public void setLeafIcon(Icon leafIcon) {
		Icon oldValue = getLeafIcon();
		this.leafIcon = leafIcon;
		firePropertyChange("leafIcon", oldValue, getLeafIcon());
		repaintColumn(getHierarchialColumn());
	}
	
	public Icon getOpenIcon() {
		return openIcon;
	}
	
	public void setOpenIcon(Icon openIcon) {
		Icon oldValue = getOpenIcon();
		this.openIcon = openIcon;
		firePropertyChange("openIcon", oldValue, getOpenIcon());
		repaintColumn(getHierarchialColumn());
	}
	
	public Icon getClosedIcon() {
		return closedIcon;
	}
	
	public void setClosedIcon(Icon closedIcon) {
		Icon oldValue = getClosedIcon();
		this.closedIcon = closedIcon;
		firePropertyChange("closedIcon", oldValue, getClosedIcon());
		repaintColumn(getHierarchialColumn());
	}
	
	private void repaintColumn(int col) {
		if (col < 0 || getRowCount() == 0)
			return;
		Rectangle r = table.getCellRect(0, col, true);
		r.height = getHeight();
		repaint(r);
	}
	
	
	
    public boolean getAutoCreateColumnsFromModel() {
		return table.getAutoCreateColumnsFromModel();
	}

	public void setAutoCreateColumnsFromModel(boolean autoCreateColumnsFromModel) {
		table.setAutoCreateColumnsFromModel(autoCreateColumnsFromModel);
	}

	public int getAutoResizeMode() {
		return table.getAutoResizeMode();
	}

	public void setAutoResizeMode(int mode) {
		table.setAutoResizeMode(mode);
	}

	public boolean getCellSelectionEnabled() {
		return table.getCellSelectionEnabled();
	}

	public void setCellSelectionEnabled(boolean cellSelectionEnabled) {
		table.setCellSelectionEnabled(cellSelectionEnabled);
	}

	public boolean getColumnSelectionAllowed() {
		return table.getColumnSelectionAllowed();
	}

	public void setColumnSelectionAllowed(boolean columnSelectionAllowed) {
		table.setColumnSelectionAllowed(columnSelectionAllowed);
	}

	public Color getGridColor() {
		return table.getGridColor();
	}

	public void setGridColor(Color gridColor) {
		table.setGridColor(gridColor);
	}

	public Dimension getIntercellSpacing() {
		return table.getIntercellSpacing();
	}

	public void setIntercellSpacing(Dimension intercellSpacing) {
		table.setIntercellSpacing(intercellSpacing);
	}

	public int getRowMargin() {
		return table.getRowMargin();
	}
	
	public void setRowMargin(int rowMargin) {
		table.setRowMargin(rowMargin);
	}

	public boolean getRowSelectionAllowed() {
		return table.getRowSelectionAllowed();
	}

	public void setRowSelectionAllowed(boolean rowSelectionAllowed) {
		table.setRowSelectionAllowed(rowSelectionAllowed);
	}

	public boolean getShowHorizontalLines() {
		return table.getShowHorizontalLines();
	}

	public void setShowHorizontalLines(boolean showHorizontalLines) {
		table.setShowHorizontalLines(showHorizontalLines);
	}
	
	public boolean getShowVerticalLines() {
		return table.getShowVerticalLines();
	}

	public void setShowVerticalLines(boolean showVerticalLines) {
		table.setShowVerticalLines(showVerticalLines);
	}
	
	public void setShowGrid(boolean showGrid) {
		table.setShowGrid(showGrid);
	}

	public JTableHeader getTableHeader() {
		return table.getTableHeader();
	}

	public void setTableHeader(JTableHeader tableHeader) {
		table.setTableHeader(tableHeader);
	}
	
	
	
	
	
	
	
	
	public Enumeration<TreePath> getExpandedDescendants(TreePath parent) {
		return tree.getExpandedDescendants(parent);
	}
	
	public void collapsePath(TreePath path) {
		tree.collapsePath(path);
	}
	
	public void collapseRow(int row) {
		tree.collapseRow(row);
	}
	
	public void expandPath(TreePath path) {
		tree.expandPath(path);
	}
	
	public void expandRow(int row) {
		tree.expandRow(row);
	}
	
	

	public Rectangle getPathBounds(TreePath path) {
		return tree.getPathBounds(path);
	}

	public TreePath getPathForLocation(int x, int y) {
		return tree.getPathForLocation(x, y);
	}

	public TreePath getPathForRow(int row) {
		return tree.getPathForRow(row);
	}

	public Rectangle getRowBounds(int row) {
		return tree.getRowBounds(row);
	}

	public int getRowCount() {
		return tree.getRowCount();
	}

	public int getRowForLocation(int x, int y) {
		return tree.getRowForLocation(x, y);
	}

	public int getRowForPath(TreePath path) {
		return tree.getRowForPath(path);
	}
	
	public boolean isCollapsed(int row) {
		return tree.isCollapsed(row);
	}

	public boolean isCollapsed(TreePath path) {
		return tree.isCollapsed(path);
	}

	public boolean isExpanded(int row) {
		return tree.isExpanded(row);
	}

	public boolean isExpanded(TreePath path) {
		return tree.isExpanded(path);
	}

	public boolean isFixedRowHeight() {
		return tree.isFixedRowHeight();
	}
	
	public boolean isLargeModel() {
		return tree.isLargeModel();
	}
	
	public void setLargeModel(boolean largeModel) {
		tree.setLargeModel(largeModel);
	}
	
	public boolean isRootVisible() {
		return tree.isRootVisible();
	}

	public void setRootVisible(boolean rootVisible) {
		tree.setRootVisible(rootVisible);
	}

	public boolean getScrollsOnExpand() {
		return tree.getScrollsOnExpand();
	}

	public boolean getShowsRootHandles() {
		return tree.getShowsRootHandles();
	}

	public void setShowsRootHandles(boolean newValue) {
		tree.setShowsRootHandles(newValue);
	}

	public void setToggleClickCount(int clickCount) {
		tree.setToggleClickCount(clickCount);
	}
	public int getToggleClickCount() {
		return tree.getToggleClickCount();
	}

	public int getVisibleRowCount() {
		return tree.getVisibleRowCount();
	}

	public void setVisibleRowCount(int newCount) {
		tree.setVisibleRowCount(newCount);
	}

	public int getRowHeight() {
		return tree.getRowHeight();
	}

	public void setRowHeight(int rowHeight) {
		tree.setRowHeight(rowHeight);
	}


	
	public TreeSelectionModel getSelectionModel() {
		return tree == null ? null : tree.getSelectionModel();
	}

	public void setSelectionModel(TreeSelectionModel selectionModel) {
		if (tree != null)
			tree.setSelectionModel(selectionModel);
	}
	
	/**
	 * Changes to the TreeSelectionModel can be listened to
	 * in terms of a ListSelectionModel by adding a 
	 * ListSelectionListener to this ListSelectionModel.
	 * 
	 * @return ListSelectionModel view of the TreeSelectionModel.
	 * @see #getSelectionModel()
	 */
	public ListSelectionModel getRowSelectionModel() {
		return adapter;
	}
	
	public void clearSelection() {
		tree.clearSelection();
	}
	
	public boolean isSelectionEmpty() {
		return tree.isSelectionEmpty();
	}
	
	public int getSelectionCount() {
		return tree.getSelectionCount();
	}
	
	public int getMaxSelectionRow() {
		return tree.getMaxSelectionRow();
	}

	public int getMinSelectionRow() {
		return tree.getMinSelectionRow();
	}

	public boolean isPathSelected(TreePath path) {
		return tree.isPathSelected(path);
	}

	public boolean isRowSelected(int row) {
		return tree.isRowSelected(row);
	}
	
	public TreePath getAnchorSelectionPath() {
		return tree.getAnchorSelectionPath();
	}
	
	public void setAnchorSelectionPath(TreePath newPath) {
		tree.setAnchorSelectionPath(newPath);
	}
	
	public TreePath getLeadSelectionPath() {
		return tree.getLeadSelectionPath();
	}

	public void setLeadSelectionPath(TreePath newPath) {
		tree.setLeadSelectionPath(newPath);
	}

	public int getLeadSelectionRow() {
		return tree.getLeadSelectionRow();
	}
	
	public boolean getExpandsSelectedPaths() {
		return tree.getExpandsSelectedPaths();
	}

	public void setExpandsSelectedPaths(boolean newValue) {
		tree.setExpandsSelectedPaths(newValue);
	}

	public TreePath getSelectionPath() {
		return tree.getSelectionPath();
	}

	public void setSelectionPath(TreePath path) {
		tree.setSelectionPath(path);
	}

	public TreePath[] getSelectionPaths() {
		return tree.getSelectionPaths();
	}

	public void setSelectionPaths(TreePath[] paths) {
		tree.setSelectionPaths(paths);
	}

	public int[] getSelectionRows() {
		return tree.getSelectionRows();
	}
	
	public void addSelectionInterval(int index0, int index1) {
		tree.addSelectionInterval(index0, index1);
	}
	
	public void addSelectionPath(TreePath path) {
		tree.addSelectionPath(path);
	}
	
	public void addSelectionPath(TreePath[] paths) {
		tree.addSelectionPaths(paths);
	}
	
	public void addSelectionRow(int row) {
		tree.addSelectionRow(row);
	}
	
	public void addSelectionRows(int[] rows) {
		tree.addSelectionRows(rows);
	}
	
	public void removeSelectionInterval(int index0, int index1) {
		tree.removeSelectionInterval(index0, index1);
	}
	
	public void removeSelectionPath(TreePath path) {
		tree.removeSelectionPath(path);
	}
	
	public void removeSelectionPath(TreePath[] paths) {
		tree.removeSelectionPaths(paths);
	}
	
	public void removeSelectionRow(int row) {
		tree.removeSelectionRow(row);
	}
	
	public void removeSelectionRows(int[] rows) {
		tree.removeSelectionRows(rows);
	}
	
	public void setSelectionRows(int[] rows) {
		tree.setSelectionRows(rows);
	}

	public void setSelectionRow(int row) {
		tree.setSelectionRow(row);
	}

	public void setSelectionInterval(int index0, int index1) {
		tree.setSelectionInterval(index0, index1);
	}
	
	public void changeSelection(int row, int column, boolean toggle, boolean extend) {
		table.changeSelection(row, column, toggle, extend);
	}
	
	public Color getSelectionForeground() {
		return table.getSelectionForeground();
	}
	
	public void setSelectionForeground(Color selectionForeground) {
		table.setSelectionForeground(selectionForeground);
	}
	
	public Color getSelectionBackground() {
		return table.getSelectionBackground();
	}
	
	public void setSelectionBackground(Color selectionBackground) {
		table.setSelectionBackground(selectionBackground);
	}


	
	
	
	
	// Scrollable interface

	public Dimension getPreferredScrollableViewportSize() {
		Dimension size = tree.getPreferredScrollableViewportSize();
		TableColumnModel cm = getColumnModel();
		int width = 0;
		for (int col=cm.getColumnCount(); --col>=0;)
			width += cm.getColumn(col).getPreferredWidth();
		size.width = width + cm.getColumnMargin() * cm.getColumnCount();
		return size;
	}
	
	/**
	 * JTree and JTable have different scrolling behavior,
	 * so scroll as if a JTree for vertical scrolls and
	 * as a JTable for horizontal scrolls.
	 * 
	 * @param orientation VERTICAL or HORIZONTAL
	 * @return tree for VERTICAL, table for HORIZONTAL
	 */
	private Scrollable getScrollable(int orientation) {
		return orientation == SwingConstants.VERTICAL ? tree : table;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect,
			int orientation, int direction) {
		return getScrollable(orientation).getScrollableBlockIncrement(
					visibleRect, orientation, direction);
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return table.getScrollableTracksViewportHeight();
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return table.getScrollableTracksViewportWidth();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect,
			int orientation, int direction) {
		return getScrollable(orientation).getScrollableUnitIncrement(
				visibleRect, orientation, direction);
	}

	
	
	
	
	void repaint(TreePath path, int col) {
		if (col < 0) {
			repaint(path);
		} else if (path != null) {
			repaint(table.getCellRect(getRowForPath(path), col, false));
		}
	}
	
	void repaint(TreePath path) {
		if (path == null)
			return;
		Rectangle r = tree.getPathBounds(path);
		if (r == null)
			return;
		r.x = 0;
		r.width = getWidth();
		repaint(r);
	}
	
	
	
	
	protected Adapter createAdapter(Object root) {
		return new Adapter(root);
	}
	
	protected class Adapter extends AbstractTableModel implements
			ListSelectionModel, PropertyChangeListener,	TreeColumnModelListener,
			TreeExpansionListener, TreeModelListener, TreeSelectionListener,
			TreeWillExpandListener {

		public Adapter(Object root) {
			treeRoot = root;
		}

		/**
		 * Used to determine when TreeModel's root has changed.
		 */
		protected Object treeRoot;

		// TableModel interface

		@Override
		public int getColumnCount() {
			return getRowModel().getColumnCount();
		}

		@Override
		public String getColumnName(int column) {
			return getRowModel().getColumnName(column);
		}

		@Override
		public Class<?> getColumnClass(int column) {
			return getRowModel().getColumnClass(column);
		}

		@Override
		public int getRowCount() {
			return tree.getRowCount();
		}

		private Object getNode(int row) {
			return getPathForRow(row).getLastPathComponent();
		}

		@Override
		public Object getValueAt(int row, int column) {
			return getRowModel().getValueAt(getNode(row), column);
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			getRowModel().setValueAt(value, getNode(row), column);
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return getRowModel().isCellEditable(getNode(row), column);
		}

		// RowModelListener interface

		@Override
		public void treeColumnChanged(TreeColumnModelEvent e) {
			int row = getRowForPath(e.getTreePath());
			if (row < 0)
				return;
			if (e.getColumn() == TreeColumnModelEvent.ALL_COLUMNS) {
				fireTableRowsUpdated(row, row);
			} else {
				fireTableCellUpdated(row, e.getColumn());
			}
			if (getRowHeight() <= 0 && ui != null)
				getUI().invalidatePathBounds(TreeTable.this, e.getTreePath());
		}

		// TreeModelListener interface

		@Override
		public void treeNodesChanged(TreeModelEvent e) {
			if (e.getChildren() == null) {
				if (isRootVisible())
					updateTable(0, 0, TableModelEvent.UPDATE);
			} else {
				updateTable(e, TableModelEvent.UPDATE);
			}
		}

		@Override
		public void treeNodesInserted(TreeModelEvent e) {
			updateTable(e, TableModelEvent.INSERT);
		}

		@Override
		public void treeNodesRemoved(TreeModelEvent e) {
			updateTable(e, TableModelEvent.DELETE);
		}

		@Override
		public void treeStructureChanged(TreeModelEvent e) {
			TreePath path = e.getTreePath();
			if (path.getPathCount() == 1 && path.getLastPathComponent() != treeRoot) {
				treeRoot = path.getLastPathComponent();
				fireTableStructureChanged();
			} else {
				fireTableDataChanged();
			}
		}

		private void updateTable(TreeModelEvent e, int eventType) {
			int row = getRowForPath(e.getTreePath());
			if (row < 0)
				return;
			int[] children = e.getChildIndices();
			if (children == null)
				return; // invalid TreeModelEvent
			int lastRow = -1;
			int firstRow = -1;
			for (int i=children.length; --i>=0;) {
				int idx = children[i];
				if (firstRow < 0) {
					firstRow = lastRow = idx;
				} else if (idx == lastRow - 1) {
					firstRow = idx;
				} else {
					updateTable(row+firstRow, row+lastRow, eventType);
					firstRow = lastRow = idx;
				}
			}
			updateTable(row+firstRow, row+lastRow, eventType);
		}

		private void updateTable(int firstRow, int lastRow, int eventType) {
			fireTableChanged(new TableModelEvent(this, firstRow,
					lastRow, TableModelEvent.ALL_COLUMNS, eventType));
		}


		// TreeExpansionListener interface

		@Override
		public void treeExpanded(TreeExpansionEvent e) {
			fireTreeExpansionEvent(e.getPath(), true);
		}

		@Override
		public void treeCollapsed(TreeExpansionEvent e) {
			fireTreeExpansionEvent(e.getPath(), false);
		}

		// TreeWillExpandListener interface

		@Override
		public void treeWillExpand(TreeExpansionEvent e)
				throws ExpandVetoException {
			fireTreeWillExpandEvent(e.getPath(), true);
		}

		@Override
		public void treeWillCollapse(TreeExpansionEvent e)
				throws ExpandVetoException {
			fireTreeWillExpandEvent(e.getPath(), false);
		}

		// PropertyChangeListener interface

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			String name = evt.getPropertyName();
			if (name == JTree.LEAD_SELECTION_PATH_PROPERTY ||
					name == JTree.ANCHOR_SELECTION_PATH_PROPERTY) {
				int col = !isColumnFocusEnabled() ? -1 :
					getColumnModel().getSelectionModel().getLeadSelectionIndex();
				repaint((TreePath)evt.getOldValue(), col);
 				repaint((TreePath)evt.getNewValue(), col);
			} else if (name == "model") {
				return;
			} else if (name == "rowHeight") {
				if (evt.getSource() != tree)
					return; // only fire once
			} else if (name == "componentOrientation") {
				return; // don't fire
			}
			firePropertyChange(name, evt.getOldValue(), evt.getNewValue());
		}

		// ListSelectionModel interface

		@Override
		public void addListSelectionListener(ListSelectionListener l) {
			listenerList.add(ListSelectionListener.class, l);
		}

		@Override
		public void removeListSelectionListener(ListSelectionListener l) {
			listenerList.remove(ListSelectionListener.class, l);
		}

		@Override
		public void addSelectionInterval(int index0, int index1) {
			tree.addSelectionInterval(index0, index1);
			
		}

		@Override
		public void clearSelection() {
			getSelectionModel().clearSelection();
		}

		@Override
		public int getAnchorSelectionIndex() {
			return getRowForPath(getAnchorSelectionPath());
		}

		@Override
		public int getLeadSelectionIndex() {
			return getRowForPath(getLeadSelectionPath());
		}

		@Override
		public int getMaxSelectionIndex() {
			return getSelectionModel().getMaxSelectionRow();
		}

		@Override
		public int getMinSelectionIndex() {
			return getSelectionModel().getMinSelectionRow();
		}

		@Override
		public boolean getValueIsAdjusting() {
			return false;
		}

		@Override
		public void insertIndexInterval(int index, int length, boolean before) {}

		@Override
		public boolean isSelectedIndex(int index) {
			return getSelectionModel().isRowSelected(index);
		}

		@Override
		public boolean isSelectionEmpty() {
			return getSelectionModel().isSelectionEmpty();
		}

		@Override
		public void removeIndexInterval(int index0, int index1) {
		}

		@Override
		public void removeSelectionInterval(int index0, int index1) {
			TreePath anchor = getAnchorSelectionPath();
			tree.removeSelectionInterval(index0, index1);
			setAnchorSelectionPath(anchor);
			setLeadSelectionPath(getPathForRow(index1));
		}

		@Override
		public void setAnchorSelectionIndex(int index) {
			setAnchorSelectionPath(getPathForRow(index));
		}

		@Override
		public void setLeadSelectionIndex(int index) {
			setLeadSelectionPath(getPathForRow(index));
		}

		@Override
		public void setSelectionInterval(int index0, int index1) {
			TreeTable.this.setSelectionInterval(index0, index1);
			setAnchorSelectionPath(getPathForRow(index0));
			setLeadSelectionPath(getPathForRow(index1));
		}

		@Override
		public int getSelectionMode() {
			switch (getSelectionModel().getSelectionMode()) {
			case TreeSelectionModel.CONTIGUOUS_TREE_SELECTION:
				return ListSelectionModel.SINGLE_INTERVAL_SELECTION;
			case TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION:
				return ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
			case TreeSelectionModel.SINGLE_TREE_SELECTION:
				return ListSelectionModel.SINGLE_SELECTION;
			}
			return ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
		}

		@Override
		public void setSelectionMode(int mode) {
			switch (mode) {
			default: return;
			case ListSelectionModel.SINGLE_SELECTION:
				mode = TreeSelectionModel.SINGLE_TREE_SELECTION;
				break;
			case ListSelectionModel.SINGLE_INTERVAL_SELECTION:
				mode = TreeSelectionModel.CONTIGUOUS_TREE_SELECTION;
				break;
			case ListSelectionModel.MULTIPLE_INTERVAL_SELECTION:
				mode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION;
				break;
			}
			getSelectionModel().setSelectionMode(mode);
		}

		@Override
		public void setValueIsAdjusting(boolean valueIsAdjusting) {}

		// TreeSelectionListener interface

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			int minRow = Integer.MAX_VALUE;
			int maxRow = 0;
			minRow = min(minRow, e.getNewLeadSelectionPath());
			maxRow = max(maxRow, e.getNewLeadSelectionPath());
			TreePath[] paths = e.getPaths();
			if (paths != null) {
				for (TreePath path : paths) {
					minRow = min(minRow, path);
					maxRow = max(maxRow, path);
				}
			}
			fireSelectionChanged(minRow, maxRow);
		}

		private int min(int row, TreePath path) {
			return Math.min(row, getRowForPath(path));
		}

		private int max(int row, TreePath path) {
			return Math.max(row, getRowForPath(path));
		}

		private void fireSelectionChanged(int firstRow, int lastRow) {
			ListSelectionEvent e = new ListSelectionEvent(this, firstRow, lastRow, false);
			Object[] listeners = listenerList.getListenerList();
			for (int i = listeners.length-2; i>=0; i-=2)
				if (listeners[i]==ListSelectionListener.class)
					((ListSelectionListener)listeners[i+1]).valueChanged(e);
		}

	}



}
