package aephyr.swing;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.EventListener;

import javax.swing.CellRendererPane;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import aephyr.swing.event.RowModelEvent;
import aephyr.swing.event.RowModelListener;
import aephyr.swing.treetable.DefaultRowModel;
import aephyr.swing.treetable.DefaultTreeTableNode;
import aephyr.swing.treetable.RowModel;


public class TreeTable extends JComponent implements Scrollable {

	public TreeTable() {
		this(new DefaultTreeTableNode());
	}

	public TreeTable(DefaultTreeTableNode root) {
		this(new DefaultTreeModel(root), new DefaultRowModel(root, root.getColumnCount()));
	}
	
    public TreeTable(TreeModel tm, RowModel rm) {
    	this(tm, rm, null);
    }
    
	public TreeTable(TreeModel tm, RowModel rm, TableColumnModel cm) {
		rowModel = rm;
		focusRenderer = new FocusRenderer();
		Adapter adapter = new Adapter(tm.getRoot());
		
		// Intercepter hack-trick to get the UI TreeModelListeners
		DefaultTreeModel intercepter = tm instanceof DefaultTreeModel ?
				(DefaultTreeModel)tm : new DefaultTreeModel(null);
		tree = new Tree(intercepter);
		uiTreeModelListeners = intercepter.getTreeModelListeners();
		if (tm != intercepter)
			tree.setModel(tm);
		
		table = new Table(adapter, cm, adapter);
		
		// this must be set after table construction
		this.adapter = adapter;
		
		if (cm == null) {
			cm = table.getColumnModel();
			int hc = rm.getHierarchialColumn();
			if (hc >= 0)
				cm.getColumn(hc).setPreferredWidth(150);
		}
		cm.addColumnModelListener(adapter);
		tm.addTreeModelListener(adapter);
		rm.addRowModelListener(adapter);
		tree.getSelectionModel().addTreeSelectionListener(adapter);
		tree.setCellRenderer(new Renderer(tree.getCellRenderer()));
		tree.addPropertyChangeListener(adapter);
		table.addPropertyChangeListener(adapter);
		setFocusable(true);
		enableEvents(AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
		updateProperties();
		
	}
	
	private Tree tree;
	
	private Table table;
	
	private Adapter adapter;
	
	private RowModel rowModel;
	
	private TableCellRenderer focusRenderer;
	
	private boolean columnFocusEnabled = true;

	private boolean autoCreateTableHeader = true;
	
	private TreeModelListener[] uiTreeModelListeners;
	
	
	@Override
	public void updateUI() {
		Renderer renderer = (Renderer)tree.getCellRenderer();
		boolean updateTreeCellRenderer = renderer.delegate instanceof UIResource;
		if (updateTreeCellRenderer) {
			tree.setCellRenderer(null);
		}
		tree.updateUI();
		if (updateTreeCellRenderer) {
			renderer.delegate = tree.getCellRenderer();
			tree.setCellRenderer(renderer);
		} else if (renderer.delegate instanceof JComponent) {
			((JComponent)renderer.delegate).updateUI();
		}
		table.updateUI();
		updateProperties();
	}
	
	private void updateProperties() {
		tree.setOpaque(false);
		table.setShowHorizontalLines(false);
		setRowHeight(20);
		InputMap inputs = tree.getInputMap();
		update(inputs, KeyEvent.VK_LEFT);
		update(inputs, KeyEvent.VK_RIGHT);
	}

	private void update(InputMap inputs, int code) {
		Object key = inputs.get(KeyStroke.getKeyStroke(code, 0));
		if (key != null)
			inputs.put(KeyStroke.getKeyStroke(
					code, InputEvent.ALT_DOWN_MASK), key);
	}


//	private static final String uiClassID = "TreeTableUI";
//
//	@Override
//	public String getUIClassID() {
//		return uiClassID;
//	}
//	
//	public void setUI(TreeTableUI ui) {
//		super.setUI(ui);
//	}
//	
//	public TreeTableUI getUI() {
//		return (TreeTableUI)ui;
//	}
	
	
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
	
	public boolean getAutoCreateTableHeader() {
		return autoCreateTableHeader;
	}

	public void setAutoCreateTableHeader(boolean autoCreateTableHeader) {
		boolean oldValue = getAutoCreateTableHeader();
		this.autoCreateTableHeader = autoCreateTableHeader;
		firePropertyChange("autoCreateTableHeader", oldValue, getAutoCreateTableHeader());
	}
	
	
	/**
	 * Overridden to implement Key Binding order:
	 * 		this WHEN_FOCUS
	 * 		this WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
	 * 		table or tree WHEN_FOCUSED
	 * 		table or tree WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
	 * 		this WHEN_IN_FOCUS_WINDOW
	 * 
	 * TODO Note: JTable's default (crude) implementation of
	 * editing upon any key press is circumvented with this
	 * implementation (what a shame). A similar (better) operation
	 * needs to be conducted.
	 */
	@Override
	protected void processComponentKeyEvent(KeyEvent e) {
		KeyStroke ks = KeyStroke.getKeyStrokeForEvent(e);
		int condition = getConditionForKeyStroke(ks);
		if (condition == WHEN_FOCUSED || condition == WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
			return;
		if ((!dispatchToTable(e) || !dispatchKeyEvent(e, ks, table)))
			dispatchKeyEvent(e, ks, tree);
	}
	
	private boolean dispatchToTable(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_DOWN: case KeyEvent.VK_UP:
		case KeyEvent.VK_END: case KeyEvent.VK_HOME:
		case KeyEvent.VK_PAGE_DOWN: case KeyEvent.VK_PAGE_UP:
			return false;
		case KeyEvent.VK_LEFT: case KeyEvent.VK_RIGHT:
			return isColumnFocusEnabled();
		}
		return true;
	}
	
	private boolean dispatchKeyEvent(KeyEvent e, KeyStroke ks, JComponent c) {
		int condition = c.getConditionForKeyStroke(ks);
		if (condition == WHEN_FOCUSED || condition == WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
			if (c == table) {
				table.processKeyBinding(ks, e, condition, !ks.isOnKeyRelease());
			} else {
				tree.processKeyBinding(ks, e, condition, !ks.isOnKeyRelease());
			}
			e.consume();
			return true;
		}
		return false;
	}
	
	/**
	 * Overridden to dispatch the MouseEvent to the table or tree.
	 */
	@Override
	protected void processMouseEvent(MouseEvent e) {
		super.processMouseEvent(e);
		if (e.isConsumed() || e.isPopupTrigger())
			return;
		if (!dispatchToTree(e))
			dispatchMouseEvent(e, table);
	}

	/**
	 * If a negative number is returned, then all events that occur in the
	 * leading margin will be forwarded to the tree and consumed.
	 * 
	 * @return the width of the tree handle if it can be determined, else -1
	 */
	protected int getTreeHandleWidth() {
		if (tree.getUI() instanceof BasicTreeUI) {
			BasicTreeUI ui = (BasicTreeUI) tree.getUI();
			return ui.getLeftChildIndent() + ui.getRightChildIndent();
		} else {
			return -1;
		}
	}

	private boolean dispatchToTree(MouseEvent e) {
		switch (e.getID()) {
		case MouseEvent.MOUSE_ENTERED:
		case MouseEvent.MOUSE_EXITED:
			return false;
		}
		int hier = getHierarchialColumn();
		if (hier < 0)
			return false;
		Point pt = e.getPoint();
		int col = table.columnAtPoint(pt);
		if (col != hier)
			return false;
		int row = table.rowAtPoint(pt);
		if (row < 0)
			return false;
		TreePath path = tree.getPathForRow(row);
		Object node = path.getLastPathComponent();
		// Check if the node has a tree handle
		if (getTreeModel().isLeaf(node) ||
				(getTreeModel().getChildCount(node) <= 0 &&
						!tree.hasBeenExpanded(path)))
			return false;
		// Check if the event location falls over the tree handle.
		Rectangle cellBounds = table.getCellRect(row, col, false);
		int x = e.getX() - cellBounds.x;
		Rectangle nb = tree.getRowBounds(row);
		int thw = getTreeHandleWidth();
		if (tree.getComponentOrientation().isLeftToRight() ?
				x < nb.x && (thw < 0 || x > nb.x - thw) :
					x > nb.x + nb.width && (thw < 0
							|| x < nb.x + nb.width + thw)) {
			dispatchMouseEvent(e, tree);
			return true;
		}
		return false;
	}
	
	private void dispatchMouseEvent(MouseEvent e, JComponent c) {
		MouseEvent me = new MouseEvent(c, e.getID(), e.getWhen(), e.getModifiers(),
				e.getX()-c.getX(), e.getY()-c.getY(), e.getXOnScreen(), e.getYOnScreen(),
				e.getClickCount(), false, e.getButton());
		c.dispatchEvent(me);
		if (me.isConsumed())
			e.consume();
	}
	
	@Override
	public Dimension getPreferredSize() {
		Dimension size = tree.getPreferredSize();
		Dimension tableSize = table.getPreferredSize();
		size.width = tableSize.width;
		return size;
	}
	
	@Override
	public void doLayout() {
		layoutTable();
		layoutTree();
		super.doLayout();
	}
	
	private void layoutTable() {
		table.setBounds(0, 0, getWidth(), getHeight());
		table.doLayout();
	}

	private void layoutTree() {
		int hier = getHierarchialColumn();
		if (hier < 0) {
			tree.setBounds(0, 0, 0, 0);
		} else {
			Rectangle r = table.getCellRect(-1, hier, true);
			// subtract the column margin here since getCellRect()
			// won't (might not) do it when the cell isn't valid
			r.width -= getColumnModel().getColumnMargin();
			r.height = table.getHeight();
			tree.setBounds(r);
			tree.doLayout();
		}
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		// first paint the table
		Graphics cg = g.create(0, 0, getWidth(), getHeight());
		try {
			table.paint(cg);
		} finally {
			cg.dispose();
		}
		// no need to paint the tree or focus row if there are no rows
		if (getRowCount() == 0)
			return;
		Shape clip = g.getClip();
		if (tree.getWidth() > 0 && clip.intersects(tree.getBounds())) {
			JTableHeader header = getTableHeader();
			int x = tree.getX();
			int clipX = 0;
			int clipW = tree.getWidth();
			if (header != null) {
				TableColumn dc = header.getDraggedColumn();
				if (dc != null) {
					if (dc.getModelIndex() == getRowModel().getHierarchialColumn()) {
						// shift x distance for painting tree
						x += header.getDraggedDistance();
					} else {
						// see if dragged column overlaps tree
						// if so, adjust clipX and clipW
						int col = convertColumnIndexToView(dc.getModelIndex());
						Rectangle r = table.getCellRect(0, col, true);
						int dragX0 = r.x + header.getDraggedDistance();
						int dragX1 = dragX0 + r.width;
						if (x >= dragX0 && x < dragX1) {
							clipX = dragX1 - x;
							clipW -= clipX;
						} else if (x < dragX0 && dragX0 < x+tree.getWidth()) {
							clipW -= x + tree.getWidth() - dragX0;
						}
					}
				}
			}
			cg = g.create(x, 0, tree.getWidth(), tree.getHeight());
			try {
				cg.clipRect(clipX, 0, clipW, tree.getHeight());
				tree.paint(cg);
			} finally {
				cg.dispose();
			}
		}
		if (isPaintingForPrint())
			return;
		// TODO, need to use CellRendererPane for focusRenderer?
		// TODO, drag clipping is too simplistic, i.e. wrong.
		// it is possible for a dragged column to be "land locked"
		// on top of another if the bottom column has a large enough width
		TableCellRenderer focusRenderer = getFocusRenderer();
		if (isColumnFocusEnabled()) {
			int lead = getColumnModel().getSelectionModel()
					.getLeadSelectionIndex();
			if (lead < 0 || ((focusRenderer instanceof UIResource)
					&& lead != getHierarchialColumn()))
				return;
			int row = getRowForPath(getLeadSelectionPath());
			Rectangle r = table.getCellRect(row, lead, false);
			Rectangle clipR = null;
			JTableHeader header = getTableHeader();
			if (header != null) {
				TableColumn dc = header.getDraggedColumn();
				if (dc != null && header.getDraggedDistance() != 0) {
					if (dc.getModelIndex() == getRowModel().getHierarchialColumn()) {
						r.x += header.getDraggedDistance();
					} else {
						int col = convertColumnIndexToView(dc.getModelIndex());
						Rectangle dr = table.getCellRect(row, col, true);
						dr.x += header.getDraggedDistance();
						clipR = r.intersection(dr);
						clipR.x -= r.x;
					}
				}
			}
			if (!clip.intersects(r))
				return;
			Component c = focusRenderer.getTableCellRendererComponent(
					table, "", false, true, row, lead);
			c.setBounds(0, 0, r.width, r.height);
			cg = g.create(r.x, r.y, r.width, r.height);
			if (clipR != null) {
				// clipR is actually the inverse of the "clip"
				if (clipR.x == 0) {
					cg.setClip(clipR.width, 0, r.width, r.height);
				} else {
					cg.setClip(0, 0, clipR.x, r.height);
				}
			}
			try {
				c.paint(cg);
			} finally {
				cg.dispose();
			}
		} else {
			// paint focus around lead row
			int row = getRowForPath(getLeadSelectionPath());
			int columns = getColumnModel().getColumnCount();
			if (row >= 0 && columns > 0) {
				Rectangle r = table.getCellRect(row, 0, true);
				if (columns > 1)
					r.add(table.getCellRect(row, columns-1, true));
				if (clip.intersects(r)) {
					Component c = focusRenderer.getTableCellRendererComponent(
							table, "", false, true, row, -1);
					c.setBounds(0, 0, r.width, r.height);
					cg = g.create(r.x, r.y, r.width, r.height);
					try {
						boolean paintFullRow = true;
						JTableHeader header = getTableHeader();
						if (header != null) {
							TableColumn dc = header.getDraggedColumn();
							if (dc != null && header.getDraggedDistance() != 0) {
								// only paint focus over valid column bounds
								paintFullRow = false;
								int col = convertColumnIndexToView(dc.getModelIndex());
								Rectangle dr = table.getCellRect(row, col, true);
								if (header.getDraggedDistance() < 0) {
									int w = dr.x + dr.width + header.getDraggedDistance();
									cg.setClip(0, 0, w, r.height);
									c.paint(cg);
									int x = dr.x + dr.width;
									cg.setClip(x, 0, r.width - x, r.height);
									c.paint(cg);
								} else {
									cg.setClip(0, 0, dr.x, r.height);
									c.paint(cg);
									int x = dr.x + header.getDraggedDistance();
									cg.setClip(x, 0, r.width - x, r.height);
									c.paint(cg);
								}
							}
						}
						if (paintFullRow)
							c.paint(cg);
					} finally {
						cg.dispose();
					}
				}
			}
		}
	}
	
	
	public TableCellRenderer getFocusRenderer() {
		return focusRenderer;
	}
	
	public void setFocusRenderer(TableCellRenderer renderer) {
		if (renderer == null)
			renderer = new FocusRenderer();
		TableCellRenderer oldValue = getFocusRenderer();
		focusRenderer = renderer;
		this.firePropertyChange("focusRenderer", oldValue, getFocusRenderer());
		repaint(getLeadSelectionPath());
	}

	// Marked as UIResource for when moved to BasicTreeTableUI
	// as the default will be a UIResource to be changed upon updateUI
	private static class FocusRenderer extends DefaultTableCellRenderer
			implements UIResource {
		public boolean isOpaque() { return false; }
	}
	
	
	public TreeModel getTreeModel() {
		return tree.getModel();
	}
	
	public void setTreeModel(TreeModel treeModel) {
		tree.setModel(treeModel);
	}
	
	public RowModel getRowModel() {
		return rowModel;
	}
	
	public void setRowModel(RowModel rowModel) {
		RowModel oldValue = rowModel;
		this.rowModel = rowModel;
		adapter.fireTableStructureChanged();
		firePropertyChange("rowModel", oldValue, getRowModel());
	}
	
	/**
	 * Changes to the TreeModel/RowModel as well as row insertion/removal
	 * due to tree expansion/collapse can be listened to in terms of a 
	 * TableModelListener by adding the TableModelListener to this TableModel.
	 * 
	 * @return TableModel view of the TreeModel and RowModel combination.
	 * @see #getTreeModel()
	 * @see #getRowModel()
	 */
	public TableModel getTableModel() {
		return adapter;
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
	
	protected void resizeAndRepaint() {
		revalidate();
		repaint();
	}
	
	/**
	 * @return the hierarchial column in view coordinates
	 */
	public int getHierarchialColumn() {
		int col = getRowModel().getHierarchialColumn();
		return col < 0 ? -1 : convertColumnIndexToView(col);
	}
	
	public int convertColumnIndexToView(int modelColumnIndex) {
		return table.convertColumnIndexToView(modelColumnIndex);
	}
	
	public int convertColumnIndexToModel(int viewColumnIndex) {
		return table.convertColumnIndexToModel(viewColumnIndex);
	}
	
	


	
	
	
	public void setTreeCellRenderer(TreeCellRenderer renderer) {
		Renderer r = (Renderer)tree.getCellRenderer();
		TreeCellRenderer oldValue = r.delegate;
		r.delegate = renderer;
		firePropertyChange("treeCellRenderer", oldValue, renderer);
	}
	
	public TreeCellRenderer getTreeCellRenderer() {
		return ((Renderer)tree.getCellRenderer()).delegate;
	}
	
	public void setDefaultRenderer(Class<?> columnClass, TableCellRenderer renderer) {
		table.setDefaultRenderer(columnClass, renderer);
	}
	
	public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
		return table.getDefaultRenderer(columnClass);
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

	public TableColumnModel getColumnModel() {
		return table.getColumnModel();
	}

	public void setColumnModel(TableColumnModel columnModel) {
		table.setColumnModel(columnModel);
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
		int oldRowHeight = tree.getRowHeight();
		if (rowHeight > 0)
			table.setRowHeight(rowHeight);
		tree.setRowHeight(rowHeight);
		if (rowHeight <= 0 && oldRowHeight > 0)
			updateTableRowHeights();
	}
	
	/**
	 * @param path expanded or collapsed path
	 * @param interval number of rows to add or remove
	 */
	void updateTableAfterExpansion(TreePath path, int interval) {
		if (interval < 0) {
			int row = getRowForPath(path);
			adapter.fireTableRowsDeleted(row+1, row-interval);
		} else if (interval > 0) {
			int row = getRowForPath(path);
			adapter.fireTableRowsInserted(row+1, row+interval);
			if (getRowHeight() <= 0)
				updateTableRowHeights(row+1, row+interval+1);
		}
	}

	/**
	 * For variable row heights, sync table row height to 
	 * corresponding tree row height.
	 */
	void updateTableRowHeights() {
		updateTableRowHeights(0, getRowCount());
	}
	
	/**
	 * Sync table row heights to corresponding tree row height
	 * for rows <code>fromRow</code> (inclusive) to
	 * <code>toRow</code> exclusive.
	 * 
	 * @param fromRow 
	 * @param toRow 
	 */
	void updateTableRowHeights(int fromRow, int toRow) {
		assert (getRowHeight() <= 0);
		JTable tbl = table;
		JTree tre = tree;
		for (int row=toRow; --row>=fromRow;)
			tbl.setRowHeight(row, tre.getRowBounds(row).height);
	}
	

	
	public TreeSelectionModel getSelectionModel() {
		return tree.getSelectionModel();
	}

	public void setSelectionModel(TreeSelectionModel selectionModel) {
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
	



	


	public void setComponentOrientation(ComponentOrientation o) {
		super.setComponentOrientation(o);
		tree.setComponentOrientation(o);
		table.setComponentOrientation(o);
	}
	
	
	
	
	// Scrollable interface

	public Dimension getPreferredScrollableViewportSize() {
		Dimension size = tree.getPreferredScrollableViewportSize();
		TableColumnModel cm = getColumnModel();
		int width = 0;
		for (int col=cm.getColumnCount(); --col>=0;)
			width += cm.getColumn(col).getPreferredWidth();
		size.width = width;
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

	
	
	private class Renderer extends CellRendererPane implements TreeCellRenderer {
		
		Renderer(TreeCellRenderer r) {
			delegate = r;
		}
		
		private TreeCellRenderer delegate;
		
		public Component getTreeCellRendererComponent(JTree tree, Object val,
				boolean sel, boolean exp, boolean leaf, int row, boolean foc) {
			// Discrepancy between how JTree and JTable paints focus on some
			//  L&F's so use focusRenderer to paint tree's focus same as table.
			foc = false;
			component = delegate.getTreeCellRendererComponent(
					tree, val, sel, exp, leaf, row, foc);
			node = val;
			this.row = row;
			return this;
		}
		
		private Component component;
		
		private Object node;
		
		private int row;
		
		public Dimension getPreferredSize() {
			int hier = getHierarchialColumn();
			Dimension size;
			if (hier >= 0) {
				size = component.getPreferredSize();
				size.height += getRowMargin();
			} else {
				size = new Dimension(1, 0);
			}
			TableColumnModel cm = getColumnModel();
			// TODO (TBD) use row -1 because this can be called
			// before the row is valid in the table?
			//int row = -1;
			for (int col=cm.getColumnCount(); --col>=0;) {
				if (col == hier)
					continue;
				TableCellRenderer r = table.getCellRenderer(row, col);
				Object val = getRowModel().getValueAt(node, cm.getColumn(col).getModelIndex());
				Component c = r.getTableCellRendererComponent(
						table, val, false, false, row, col);
				size.height = Math.max(size.height, c.getPreferredSize().height);
			}
			return size;
		}
		
		public void paint(Graphics g) {
			int w = getWidth();
			if (getX() + w > tree.getWidth())
				w = tree.getWidth() - getX();
			paintComponent(g, component, tree,
					0, 0, w, getHeight() - getRowMargin(), false);
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
	
	private class Adapter extends AbstractTableModel implements
			ListSelectionModel, PropertyChangeListener,	RowModelListener,
			TableColumnModelListener, TreeExpansionListener,
			TreeModelListener, TreeSelectionListener, TreeWillExpandListener {

		Adapter(Object root) {
			treeRoot = root;
		}

		/**
		 * Used to determine when TreeModel's root has changed.
		 */
		private Object treeRoot;

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
		public void rowChanged(RowModelEvent e) {
			int row = getRowForPath(e.getTreePath());
			if (row < 0)
				return;
			if (e.getColumn() == RowModelEvent.ALL_COLUMNS) {
				fireTableRowsUpdated(row, row);
			} else {
				fireTableCellUpdated(row, e.getColumn());
			}
			if (getRowHeight() <= 0)
				updateTreeRowHeight(e.getTreePath(), row);
		}
		
		/**
		 * Quite hackish... BasicTreeUI has to be sent a
		 * treeNodesChanged event to revalidate a node.
		 * Maybe there is a better way?
		 * 
		 * @param row
		 */
		private void updateTreeRowHeight(TreePath path, int row) {
			int[] children;
			Object[] childNodes;
			if (path.getParentPath() == null) {
				children = null;
				childNodes = null;
			} else {
				Object node = path.getLastPathComponent();
				path = path.getParentPath();
				Object parentNode = path.getLastPathComponent();
				int index = getTreeModel().getIndexOfChild(
						parentNode, node);
				children = new int[] { index };
				childNodes = new Object[] { node };
			}
			TreeModelEvent te = new TreeModelEvent(
					getTreeModel(), path, children, childNodes);
			for (TreeModelListener l : uiTreeModelListeners) {
				l.treeNodesChanged(te);
			}
			updateTableRowHeights(row, row+1);

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

		// TableColumnListener interface

		@Override
		public void columnAdded(TableColumnModelEvent e) {
			resizeAndRepaint();
		}

		@Override
		public void columnMarginChanged(ChangeEvent e) {
			resizeAndRepaint();
		}

		@Override
		public void columnMoved(TableColumnModelEvent e) {
			repaint();
			if (e.getFromIndex() != e.getToIndex())
				layoutTree();
		}

		@Override
		public void columnRemoved(TableColumnModelEvent e) {
			resizeAndRepaint();
		}

		@Override
		public void columnSelectionChanged(ListSelectionEvent e) {
			repaint();
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
				if (!isColumnFocusEnabled()) {
					repaint((TreePath)evt.getOldValue());
					repaint((TreePath)evt.getNewValue());
				}
			} else if (name == "model") {
				if (evt.getSource() == tree) {
					TreeModel tm = (TreeModel)evt.getOldValue();
					if (tm != null)
						tm.removeTreeModelListener(this);
					tm = (TreeModel)evt.getNewValue();
					if (tm != null)
						tm.addTreeModelListener(this);
					fireTableDataChanged();
					name = "treeModel";
				} else {
					// Hopefully table's model isn't being swapped out. Yikes.
					return;
				}
			} else if (name == "rowHeight") {
				if (evt.getSource() != tree)
					return; // only fire once
			} else if (name == "columnModel") {
				TableColumnModel cm = (TableColumnModel)evt.getOldValue();
				if (cm != null)
					cm.removeColumnModelListener(this);
				cm = (TableColumnModel)evt.getNewValue();
				if (cm != null)
					cm.addColumnModelListener(this);
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

	
	
	void scrollToVisible(Rectangle r, int x, int y) {
		r.x += x;
		r.y += y;
		scrollRectToVisible(r);
		r.x -= x;
		r.y -= y;
	}
	
	private class Tree extends JTree {
		
		Tree(TreeModel model) {
			super(model);
		}
		
		/**
		 * Overridden to safe-guard against external changing of the
		 * cell renderer. Should be done @ TreeTable.setTreeCellRenderer. TODO create said method
		 */
//		public void setCellRenderer(TreeCellRenderer renderer) {
//			if (renderer != null && getCellRenderer() != null && (renderer == null || !(renderer instanceof Renderer)))
//				throw new UnsupportedOperationException();
//			super.setCellRenderer(renderer);
//		}
		
		public boolean processKeyBinding(KeyStroke ks,
				KeyEvent e, int condition, boolean pressed) {
			return super.processKeyBinding(ks, e, condition, pressed);
		}
		
		public Container getParent() {
			return TreeTable.this.getParent();
		}
		
		public boolean hasFocus() {
			return false;
		}
		
		public void repaint(long tm, int x, int y, int width, int height) {
			TreeTable.this.repaint(tm, x+getX(), y+getY(), width, height);
		}
		
		public void paintImmediately(int x, int y, int w, int h) {
			TreeTable.this.paintImmediately(x+getX(), y+getY(), w, h);
		}
		
		public void revalidate() {
			TreeTable.this.revalidate();
		}
		
		public void scrollRectToVisible(Rectangle r) {
			scrollToVisible(r, getX(), getY());
		}
		
		@Override
		public String convertValueToText(Object value, boolean sel,
				boolean exp, boolean leaf, int row, boolean foc) {
			RowModel model = getRowModel();
			value = model.getValueAt(value, model.getHierarchialColumn());
			return super.convertValueToText(value, sel, exp, leaf, row, foc);
		}
		
		@Override
		protected void setExpandedState(TreePath path, boolean state) {
			int rowCount = getRowCount();
			super.setExpandedState(path, state);
			updateTableAfterExpansion(path, getRowCount() - rowCount);
		}
		
		protected void addImpl(Component comp, Object constraints, int index) {
			if (comp instanceof CellRendererPane) {
				super.addImpl(comp, constraints, index);
			} else {
				TreeTable.this.addImpl(comp, constraints, index);
			}
		}

	}
	
	private class Table extends JTable {
		
		Table(TableModel tm, TableColumnModel cm, ListSelectionModel sm) {
			super(tm, cm, sm);
		}
		
		/**
		 * Override to safe-guard against changing the model.
		 */
		public void setModel(TableModel model) {
			if (dataModel != null)
				throw new UnsupportedOperationException();
			super.setModel(model);
		}
		
		@Override
		public boolean processKeyBinding(KeyStroke ks,
				KeyEvent e, int condition, boolean pressed) {
			return super.processKeyBinding(ks, e, condition, pressed);
		}

		
		public void changeSelection(int row, int col, boolean tog, boolean exp) {
			super.changeSelection(row, col, tog, exp);
		}
		
		public Container getParent() {
			return TreeTable.this.getParent();
		}
		
		public void repaint(long tm, int x, int y, int width, int height) {
			TreeTable.this.repaint(tm, x+getX(), y+getY(), width, height);
		}
		
		public void paintImmediately(int x, int y, int w, int h) {
			TreeTable.this.paintImmediately(x+getX(), y+getY(), w, h);
		}
		
		public void revalidate() {
			TreeTable.this.revalidate();
		}
		
		public void scrollRectToVisible(Rectangle r) {
			scrollToVisible(r, getX(), getY());
		}
		
		public boolean hasFocus() {
			return TreeTable.this.hasFocus();
		}
		
		protected void addImpl(Component comp, Object constraints, int index) {
			if (comp instanceof CellRendererPane) {
				super.addImpl(comp, constraints, index);
			} else {
				TreeTable.this.addImpl(comp, constraints, index);
			}
		}
		
		public void remove(Component comp) {
			if (comp instanceof CellRendererPane) {
				super.remove(comp);
			} else {
				TreeTable.this.remove(comp);
			}
		}

		public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
			boolean hier = column == getHierarchialColumn();
			Object value = hier ? "" : getValueAt(row, column);
			boolean isSelected = false;
			boolean hasFocus = false;
			if (!isPaintingForPrint()) {
				isSelected = isCellSelected(row, column);
				if (!hier && getFocusRenderer() instanceof UIResource &&
						(isColumnFocusEnabled() || getColumnCount() == 1)) {
					boolean rowIsLead =
						(selectionModel.getLeadSelectionIndex() == row);
					boolean colIsLead =
						(columnModel.getSelectionModel().getLeadSelectionIndex() == column);
					hasFocus = (rowIsLead && colIsLead) && isFocusOwner();
				}
			}
			return renderer.getTableCellRendererComponent(
					this, value, isSelected, hasFocus, row, column);
		}
		
		public void tableChanged(TableModelEvent e) {
			super.tableChanged(e);
			if (adapter == null)
				return; // in constructor, abort
			if (TreeTable.this.getRowHeight() <= 0 &&
					(e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW
							|| e.getLastRow() == Integer.MAX_VALUE))
				updateTableRowHeights();
		}
		
	}



}
