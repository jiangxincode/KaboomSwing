package aephyr.swing.ui;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.CellEditor;
import javax.swing.CellRendererPane;
import javax.swing.DefaultListSelectionModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.RowSorter;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.sun.java.swing.Painter;

import aephyr.swing.TreeTable;
import aephyr.swing.treetable.DefaultTreeTableCellRenderer;
import aephyr.swing.treetable.TreeColumnModel;
import aephyr.swing.treetable.TreeTableCellEditor;
import aephyr.swing.treetable.TreeTableCellRenderer;

public class BasicTreeTableUI extends TreeTableUI {
	
	public BasicTreeTableUI() {
		
	}

	@Override
	public void installUI(JComponent c) {
		treeTable = (TreeTable)c;
		installDefaults();
		installComponents();
		installListeners();
	}
	
	@Override
	public void uninstallUI(JComponent c) {
		uninstallListeners();
		uninstallComponents();
		uninstallDefaults();
		tree = null;
		table = null;
		treeTable = null;
	}
	
	protected void installDefaults() {
		LookAndFeel.installColorsAndFont(treeTable,
				"Table.background", "Table.foreground", "Table.font");
		
	}
	
	protected void uninstallDefaults() {

	}
	
	protected void installComponents() {
		handler = createHandler();
		treeTableCellRenderer = createCellRenderer();
		treeTableCellEditor = createCellEditor();
		TreeTableCellRenderer focusRenderer = treeTable.getFocusRenderer();
		if (focusRenderer == null || focusRenderer instanceof UIResource)
			treeTable.setFocusRenderer(createFocusRenderer());
		tree = createAndConfigureTree();
		table = createAndConfigureTable();
		
		defaultTreeCellRenderer = tree.getCellRenderer();
		tree.setCellRenderer(treeTableCellRenderer);
		
		if (treeTable.getRowSorter() != null)
			table.setRowSorter(new RowSorterAdapter());
	}
	
	protected void uninstallComponents() {
		unconfigureTree();
		unconfigureTable();
	}
	
	protected void installListeners() {
		keyListener = createKeyListener();
		if (keyListener != null)
			treeTable.addKeyListener(keyListener);
		mouseListener = createMouseListener();
		if (mouseListener != null)
			treeTable.addMouseListener(mouseListener);
		mouseMotionListener = createMouseMotionListener();
		if (mouseMotionListener != null)
			treeTable.addMouseMotionListener(mouseMotionListener);
		
		properties = getProperties();
		if (properties == null) {
			propertyChangeListener = createPropertyChangeListener();
			if (propertyChangeListener != null)
				treeTable.addPropertyChangeListener(propertyChangeListener);
		} else if (!properties.isEmpty()) {
			propertyChangeListener = createPropertyChangeListener();
			if (propertyChangeListener != null)
				for (String name : properties)
					treeTable.addPropertyChangeListener(name, propertyChangeListener);
		}
		if (propertyChangeListener == null)
			properties = Collections.emptyList();
		
	}
	
	
	protected void uninstallListeners() {
		if (keyListener != null) {
			treeTable.removeKeyListener(keyListener);
			keyListener = null;
		}
		if (mouseListener != null) {
			treeTable.removeMouseListener(mouseListener);
			mouseListener = null;
		}
		if (mouseMotionListener != null) {
			treeTable.removeMouseMotionListener(mouseMotionListener);
			mouseMotionListener = null;
		}
		if (propertyChangeListener != null) {
			if (properties == null) {
				treeTable.removePropertyChangeListener(propertyChangeListener);
			} else {
				for (String name : properties)
					treeTable.removePropertyChangeListener(
							name, propertyChangeListener);
			}
			properties = null;
			propertyChangeListener = null;
		}
	}
	
	protected TreeTable treeTable;
	
	private JTree tree;
	
	private JTable table;
	
	protected Renderer treeTableCellRenderer;
	
	protected Editor treeTableCellEditor;
	
	protected TreeCellRenderer defaultTreeCellRenderer;
	
	protected Handler handler;
	
	private KeyListener keyListener;
	
	private MouseListener mouseListener;
	
	private MouseMotionListener mouseMotionListener;
	
	private List<String> properties;
	
	private PropertyChangeListener propertyChangeListener;
	
	
	protected Renderer createCellRenderer() {
		return new Renderer();
	}
	
	protected Editor createCellEditor() {
		return new Editor();
	}
	
	protected TreeTableCellRenderer createFocusRenderer() {
		return new FocusRenderer();
	}
	

	
	protected JTree createAndConfigureTree() {
		JTree tree = createTree(treeTable.getTreeTableModel());
		if (treeTable.getSelectionModel() == null) {
			treeTable.setSelectionModel(tree.getSelectionModel());
		} else {
			tree.setSelectionModel(treeTable.getSelectionModel());
		}
		tree.setOpaque(false);
		tree.setRowHeight(20);
		tree.putClientProperty("JTree.lineStyle", "None");
		if (isNimbus()) {
			UIDefaults map = new UIDefaults();
			
			// Problematic for 1.6 & 1.7 compatibility
			Painter<JComponent> painter = new Painter<JComponent>() {
				public void paint(Graphics2D g, JComponent c, int w, int h) {}
			};
			
			map.put("Tree:TreeCell[Enabled+Selected].backgroundPainter", painter);
			map.put("Tree:TreeCell[Focused+Selected].backgroundPainter", painter);
			tree.putClientProperty("Nimbus.Overrides", map);
		}
		InputMap inputs = tree.getInputMap();
		remap(inputs, KeyEvent.VK_LEFT);
		remap(inputs, KeyEvent.VK_RIGHT);
		return tree;
	}
	
	private boolean isNimbus() {
		// TODO, change to class path
		return "Nimbus".equals(UIManager.getLookAndFeel().getName());
	}
	
	private void remap(InputMap inputs, int code) {
		Object key = inputs.get(KeyStroke.getKeyStroke(code, 0));
		if (key != null)
			inputs.put(KeyStroke.getKeyStroke(
					code, InputEvent.ALT_DOWN_MASK), key);
	}
	
	protected JTree createTree(TreeModel tm) {
		return new Tree(tm);
	}
	
	protected void unconfigureTree() {
		tree.setCellRenderer(null);
		tree.setModel(new DefaultTreeModel(null));
		tree.setSelectionModel(new DefaultTreeSelectionModel());
		tree.setUI(null);
	}
	
	
	
	protected JTable createAndConfigureTable() {
		TableColumnModel cm = treeTable.getColumnModel();
		JTable table = createTable(treeTable.getTreeTableModel(),
				cm, treeTable.getRowSelectionModel());
		table.setShowHorizontalLines(false);
		table.setRowMargin(0);
		if (cm == null) {
			cm = table.getColumnModel();
			cm.setColumnMargin(0);
		}
		if (cm.getColumnMargin() == 0)
			table.setShowVerticalLines(false);
		table.setRowHeight(20);
		return table;
	}
	
	protected JTable createTable(TableModel tm,
			TableColumnModel cm, ListSelectionModel sm) {
		return new Table(tm, cm, sm);
	}
	
	protected void unconfigureTable() {
		table.setTableHeader(null);
		table.setModel(new DefaultTableModel());
		table.setColumnModel(new DefaultTableColumnModel());
		table.setSelectionModel(new DefaultListSelectionModel());
		table.setUI(null);
	}
	
	protected KeyListener createKeyListener() {
		return handler;
	}
	
	protected MouseListener createMouseListener() {
		return handler;
	}
	
	protected MouseMotionListener createMouseMotionListener() {
		return handler;
	}
	
	/**
	 * If null, the created PropertyChangeListener will be global.
	 * <p>
	 * If Collections.EMPTY_LIST (or any empty list),
	 * a PropertyChangeListener will not be installed.
	 * <p>
	 * Otherwise, only the specified properties will be listened to.
	 * 
	 * @return properties to listen to.
	 */
	protected List<String> getProperties() {
		return java.util.Arrays.asList("componentOrientation", "enabled", "rowSorter", "JTree.lineStyle");
	}
	
	protected PropertyChangeListener createPropertyChangeListener() {
		return handler;
	}
	
	protected Handler createHandler() {
		return new Handler();
	}
	
	
	protected JTable getTable() {
		return table;
	}
	
	protected JTree getTree() {
		return tree;
	}
	
	@Override
	public TreeInterface getTreeInterface(TreeTable treeTable) {
		return (TreeInterface)tree;
	}
	
	@Override
	public TableInterface getTableInterface(TreeTable treeTable) {
		return (TableInterface)table;
	}
	
	@Override
	public Dimension getPreferredSize(JComponent c) {
		Dimension size = tree.getPreferredSize();
		size.width = table.getPreferredSize().width;
		return size;
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		try {
			paintTable(g);
			if (treeTable.getRowCount() > 0) {
				paintTree(g);
				if (!treeTable.isPaintingForPrint())
					paintFocus(g);
			}
		} finally {
			treeTableCellRenderer.clearState();
		}
	}
	
	protected void paintTable(Graphics g) {
		treeTableCellRenderer.prepareForTable();
		Graphics cg = g.create(0, 0, treeTable.getWidth(), treeTable.getHeight());
		try {
			table.paint(cg);
		} finally {
			cg.dispose();
		}
	}
	
	
	protected void paintTree(Graphics g) {
		Shape clip = g.getClip();
		if (tree.getWidth() <= 0 || !clip.intersects(tree.getBounds()))
			return;
		treeTableCellRenderer.prepareForTree();
		JTableHeader header = table.getTableHeader();
		int x = tree.getX();
		int clipX = 0;
		int clipW = tree.getWidth();
		if (header != null) {
			TableColumn dc = header.getDraggedColumn();
			if (dc != null) {
				if (dc.getModelIndex() == treeTable.getTreeColumnModel().getHierarchialColumn()) {
					// shift x distance for painting tree
					x += header.getDraggedDistance();
				} else {
					// see if dragged column overlaps tree
					// if so, adjust clipX and clipW
					int col = table.convertColumnIndexToView(dc.getModelIndex());
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
		Graphics cg = g.create(x, 0, tree.getWidth(), tree.getHeight());
		try {
			cg.clipRect(clipX, 0, clipW, tree.getHeight());
			tree.paint(cg);
		} finally {
			cg.dispose();
		}
	}
	
	protected void paintFocus(Graphics g) {
		// TODO, need to use CellRendererPane for focusRenderer?
		// TODO, drag clipping is too simplistic, i.e. wrong.
		// it is possible for a dragged column to be "land locked"
		// on top of another if the bottom column has a large enough width
		TreeTableCellRenderer focusRenderer = treeTable.getFocusRenderer();
		if (treeTable.isColumnFocusEnabled()) {
			int lead = table.getColumnModel().getSelectionModel()
					.getLeadSelectionIndex();
			if (lead < 0 || ((focusRenderer instanceof UIResource)
					&& lead != treeTable.getHierarchialColumn()))
				return;
			int row = tree.getRowForPath(tree.getLeadSelectionPath());
			Rectangle r = table.getCellRect(row, lead, false);
			Rectangle clipR = null;
			JTableHeader header = table.getTableHeader();
			if (header != null) {
				TableColumn dc = header.getDraggedColumn();
				if (dc != null && header.getDraggedDistance() != 0) {
					if (dc.getModelIndex() == treeTable.getTreeColumnModel().getHierarchialColumn()) {
						r.x += header.getDraggedDistance();
					} else {
						int col = table.convertColumnIndexToView(dc.getModelIndex());
						Rectangle dr = table.getCellRect(row, col, true);
						dr.x += header.getDraggedDistance();
						clipR = r.intersection(dr);
						clipR.x -= r.x;
					}
				}
			}
			if (!g.getClip().intersects(r))
				return;
			Component c = focusRenderer.getTreeTableCellRendererComponent(
					treeTable, "", false, true, row, lead);
			c.setBounds(0, 0, r.width, r.height);
			Graphics cg = g.create(r.x, r.y, r.width, r.height);
			try {
				if (clipR != null) {
					// clipR is actually the inverse of the "clip"
					if (clipR.x == 0) {
						cg.setClip(clipR.width, 0, r.width, r.height);
					} else {
						cg.setClip(0, 0, clipR.x, r.height);
					}
				}
				c.paint(cg);
			} finally {
				cg.dispose();
			}
		} else {
			// paint focus around lead row
			int row = tree.getRowForPath(tree.getLeadSelectionPath());
			int columns = table.getColumnModel().getColumnCount();
			if (row >= 0 && columns > 0) {
				Rectangle r = table.getCellRect(row, 0, true);
				if (columns > 1)
					r.add(table.getCellRect(row, columns-1, true));
				if (g.getClip().intersects(r)) {
					Component c = focusRenderer.getTreeTableCellRendererComponent(
							treeTable, "", false, true, row, -1);
					c.setBounds(0, 0, r.width, r.height);
					Graphics cg = g.create(r.x, r.y, r.width, r.height);
					try {
						boolean paintFullRow = true;
						JTableHeader header = table.getTableHeader();
						if (header != null) {
							TableColumn dc = header.getDraggedColumn();
							if (dc != null && header.getDraggedDistance() != 0) {
								// only paint focus over valid column bounds
								paintFullRow = false;
								int col = table.convertColumnIndexToView(dc.getModelIndex());
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
	
	
	
	
	protected void layoutTable() {
		table.setBounds(0, 0, treeTable.getWidth(), treeTable.getHeight());
	}

	protected void layoutTree() {
		int hier = treeTable.getHierarchialColumn();
		if (hier < 0) {
			tree.setBounds(0, 0, 0, 0);
		} else {
			Rectangle r = table.getCellRect(-1, hier, true);
			// subtract the column margin here since getCellRect()
			// won't (might not) do it when the cell isn't valid
			r.width -= treeTable.getColumnModel().getColumnMargin();
			r.height = table.getHeight();
			tree.setBounds(r);
		}
	}

	
	
	
	
	@Override
	public void configureCellRenderer(DefaultTreeTableCellRenderer renderer,
			TreeTable treeTable, Object value, boolean selected,
			boolean hasFocus, int row, int column) {
		renderer.getTableCellRendererComponent(
				table, value, selected, hasFocus, row, column);
	}

	@Override
	public void configureCellRenderer(DefaultTreeTableCellRenderer renderer,
			TreeTable treeTable, Object value, boolean selected,
			boolean hasFocus, int row, int column, boolean expanded,
			boolean leaf) {
		renderer.getTreeCellRendererComponent(
				tree, value, selected, expanded, leaf, row, hasFocus);
	}
	
	
	@Override
	public TreeTableCellRenderer getDefaultRenderer(Class<?> columnClass) {
		return treeTableCellRenderer;
	}
	
	@Override
	public TreeTableCellEditor getDefaultEditor(Class<?> columnClass) {
		return treeTableCellEditor;
	}
	
	
	
	/**
 	 * @param path expanded or collapsed path
	 * @param interval number of rows to add or remove
	 */
	protected void updateTableAfterExpansion(TreePath path, int interval) {
		if (interval < 0) {
			treeTable.processTreeCollapse(path, -interval);
		} else if (interval > 0) {
			treeTable.processTreeExpansion(path, interval);
		}
	}

	
	
	protected void scrollToVisible(Rectangle r, int x, int y) {
		r.x += x;
		r.y += y;
		treeTable.scrollRectToVisible(r);
		r.x -= x;
		r.y -= y;
	}
	
	protected interface ProcessKeyBinding {
		boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed);
	}
	
	private class Tree extends JTree implements TreeInterface, ProcessKeyBinding {
		
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
		
		@Override
		public boolean processKeyBinding(KeyStroke ks,
				KeyEvent e, int condition, boolean pressed) {
			return super.processKeyBinding(ks, e, condition, pressed);
		}
		
		public Container getParent() {
			return treeTable.getParent();
		}
		
		public boolean hasFocus() {
			return false;
		}
		
		public void computeVisibleRect(Rectangle visibleRect) {
			treeTable.computeVisibleRect(visibleRect);
			Rectangle2D.intersect(visibleRect, getBounds(), visibleRect);
			visibleRect.x -= getX();
			visibleRect.y -= getY();
		}
		
		public void repaint(long tm, int x, int y, int width, int height) {
			treeTable.repaint(tm, x+getX(), y+getY(), width, height);
		}
		
		public void paintImmediately(int x, int y, int w, int h) {
			treeTable.paintImmediately(x+getX(), y+getY(), w, h);
		}
		
		public void revalidate() {
			treeTable.revalidate();
		}
		
		public void doLayout() {
			layoutTree();
			super.doLayout();
		}
		
		public void scrollRectToVisible(Rectangle r) {
			scrollToVisible(r, getX(), getY());
		}
		
		@Override
		protected void setExpandedState(TreePath path, boolean state) {
//			if (isExpanded(path) == state)
//				return;
			TreePath updatePath = path;
			if (state) {
				for (;;) {
					TreePath par = updatePath.getParentPath();
					if (par == null || isExpanded(par))
						break;
					updatePath = par;
				}
			}
			int rowCount = getRowCount();
			super.setExpandedState(path, state);
			updateTableAfterExpansion(updatePath, getRowCount() - rowCount);
		}
		
		@Override
		public void setUI(TreeUI ui) {
			super.setUI(ui);
			if (ui instanceof BasicTreeUI) {
				BasicTreeUI bui = (BasicTreeUI)ui;
				treeHandleWidth = bui.getLeftChildIndent() + bui.getRightChildIndent();
			} else {
				treeHandleWidth = -1;
			}
		}
		
	}
	
	private class Table extends JTable implements TableInterface, ProcessKeyBinding {
		
		Table(TableModel tm, TableColumnModel cm, ListSelectionModel sm) {
			super(tm, cm, sm);
		}
		
		/**
		 * Override to safe-guard against changing the model.
		 */
//		public void setModel(TableModel model) {
//			if (dataModel != null)
//				throw new UnsupportedOperationException();
//			super.setModel(model);
//		}
		
		/**
		 * Override to safe-guard against changing the selection model.
		 */
//		public void setSelectionModel(ListSelectionModel model) {
//			if (selectionModel != null)
//				throw new UnsupportedOperationException();
//			super.setSelectionModel(model);
//		}
		
		@Override
		public boolean processKeyBinding(KeyStroke ks,
				KeyEvent e, int condition, boolean pressed) {
			return super.processKeyBinding(ks, e, condition, pressed);
		}

		
		public Container getParent() {
			return treeTable.getParent();
		}
		
		public void computeVisibleRect(Rectangle visibleRect) {
			treeTable.computeVisibleRect(visibleRect);
		}
		
		public void repaint(long tm, int x, int y, int width, int height) {
			treeTable.repaint(tm, x+getX(), y+getY(), width, height);
		}
		
		public void paintImmediately(int x, int y, int w, int h) {
			treeTable.paintImmediately(x+getX(), y+getY(), w, h);
		}
		
		public void revalidate() {
			treeTable.revalidate();
		}
		
		public void doLayout() {
			layoutTable();
			super.doLayout();
		}
		
		public void scrollRectToVisible(Rectangle r) {
			scrollToVisible(r, getX(), getY());
		}
		
		public boolean hasFocus() {
			return treeTable.hasFocus();
		}
		
		public TableCellEditor getCellEditor(int row, int column) {
			treeTableCellEditor.loadEditor(row, column);
			return treeTableCellEditor;
		}
		
		public boolean editCellAt(int row, int col, EventObject e) {
			return super.editCellAt(row, col, e);
		}
		
		public Component prepareEditor(TableCellEditor editor, int row, int col) {
			return super.prepareEditor(editor, row, col);
			
		}
		
		
		protected void addImpl(Component comp, Object constraints, int index) {
			if (comp instanceof CellRendererPane) {
				super.addImpl(comp, constraints, index);
			} else {
				treeTable.add(comp, constraints, index);
			}
		}
		
		public void remove(Component comp) {
			if (comp instanceof CellRendererPane) {
				super.remove(comp);
			} else {
				treeTable.remove(comp);
			}
		}
		
		
		public TableCellRenderer getCellRenderer(int row, int column) {
			return treeTableCellRenderer;
		}
		
		public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
			boolean hier = column == treeTable.getHierarchialColumn();
			Object value = hier ? "" : getValueAt(row, column);
			boolean isSelected = false;
			boolean hasFocus = false;
			if (!isPaintingForPrint()) {
				isSelected = isCellSelected(row, column);
				if (!hier && treeTable.getFocusRenderer() instanceof UIResource &&
						(treeTable.isColumnFocusEnabled() || getColumnCount() == 1)) {
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
			if (table == null) { // constructor specialty
				if (getAutoCreateColumnsFromModel())
					createDefaultColumnsFromModel();
			} else {
				super.tableChanged(e);
			}
		}
		
		
		public void columnAdded(TableColumnModelEvent e) {
			super.columnAdded(e);
			if (tree.getRowHeight() <= 0)
				treeTable.invalidateAllRows();
		}
		
		public void columnMoved(TableColumnModelEvent e) {
			super.columnMoved(e);
			if (e.getFromIndex() != e.getToIndex())
				tree.doLayout();
		}
		
		public void columnRemoved(TableColumnModelEvent e) {
			super.columnRemoved(e);
			if (tree.getRowHeight() <= 0)
				treeTable.invalidateAllRows();
		}
		
		public void createDefaultColumnsFromModel() {
			TableModel m = getModel();
			if (m != null) {
				// Remove any current columns
				TableColumnModel cm = getColumnModel();
				while (cm.getColumnCount() > 0)
					cm.removeColumn(cm.getColumn(0));

				// Create new columns from the data model info
				for (int i = 0; i < m.getColumnCount(); i++)
					addColumn(new TreeTableColumn(i));
			}
		}
		
		// bypass JTable's SortManager
		private RowSorter<? extends TableModel> rowSorter;
		
		public RowSorter<? extends TableModel> getRowSorter() {
			return rowSorter;
		}
		
		public void setRowSorter(RowSorter<? extends TableModel> rowSorter) {
			RowSorter<?> oldValue = getRowSorter();
			this.rowSorter = rowSorter;
			firePropertyChange("rowSorter", oldValue, rowSorter);
		}
		
	}

	/**
	 * Type-checking TableColumn.
	 * <p>
	 * Requires TableCellRenderer/TableCellEditor properties
	 * to implement TreeTableCellRenderer/TreeTableCellEditor.
	 */
	protected static class TreeTableColumn extends TableColumn {
		
		public TreeTableColumn(int modelIndex) {
			super(modelIndex);
		}
		
		@Override
		public void setCellRenderer(TableCellRenderer renderer) {
			if (renderer != null && !(renderer instanceof TreeTableCellRenderer))
				throw new IllegalArgumentException("renderer must implement TreeTableCellRenderer");
			super.setCellRenderer(renderer);
		}
		
		@Override
		public void setCellEditor(TableCellEditor editor) {
			if (editor != null && !(editor instanceof TreeTableCellEditor))
				throw new IllegalArgumentException("editor must implement TreeTableCellEditor");
			super.setCellEditor(editor);
		}
	}
	
	private class RowSorterAdapter extends RowSorter<TableModel> {
		
		@Override
		public int convertRowIndexToModel(int index) {
			return index;
		}

		@Override
		public int convertRowIndexToView(int index) {
			return index;
		}

		@Override
		public TableModel getModel() {
			return getTable().getModel();
		}

		@Override
		public int getModelRowCount() {
			return getModel().getRowCount();
		}
		
		@Override
		public int getViewRowCount() {
			return getModelRowCount();
		}


		@Override
		public List<? extends SortKey> getSortKeys() {
			return treeTable.getRowSorter().getSortKeys();
		}

		@Override
		public void setSortKeys(List<? extends SortKey> keys) {
			treeTable.getRowSorter().setSortKeys(keys);
		}

		@Override
		public void toggleSortOrder(int column) {
			treeTable.getRowSorter().toggleSortOrder(column);
		}

		@Override
		public void allRowsChanged() {}

		@Override
		public void modelStructureChanged() {}

		@Override
		public void rowsDeleted(int firstRow, int endRow) {}

		@Override
		public void rowsInserted(int firstRow, int endRow) {}

		@Override
		public void rowsUpdated(int firstRow, int endRow) {}

		@Override
		public void rowsUpdated(int firstRow, int endRow, int column) {}
		
		@Override
		public void addRowSorterListener(RowSorterListener l) {}

		@Override
		public void removeRowSorterListener(RowSorterListener l) {}

	}
	
	
	
	protected class Renderer extends CellRendererPane implements
			TreeCellRenderer, TableCellRenderer, TreeTableCellRenderer {

		public Renderer() {}

		private Component component;

		private boolean tableColumn;
		
		private int treeColumn;
		
		private boolean treeColumnSelected;
		
		private boolean rowSelectionAllowed;
		
		private Object node;

		private int row;
		
		public void prepareForTable() {
		}
		
		public void prepareForTree() {
			rowSelectionAllowed = table.getRowSelectionAllowed();
			treeColumnSelected = !rowSelectionAllowed && table.getColumnSelectionAllowed()
					&& table.isColumnSelected(treeColumn);
		}

		public void clearState() {
			component = null;
			node = null;
			removeAll();
		}
		
		@Override
		public Component getTreeTableCellRendererComponent(TreeTable treeTable,
				Object val, boolean sel, boolean foc, int row, int col) {
			int modelColumn = treeTable.convertColumnIndexToModel(col);
			TableCellRenderer r = table.getDefaultRenderer(
					treeTable.getTreeColumnModel().getColumnClass(modelColumn));
			return r.getTableCellRendererComponent(
					table, val, sel, foc, row, col);
		}
		
		@Override
		public Component getTreeTableCellRendererComponent(TreeTable treeTable,
				Object val, boolean sel, boolean foc, int row, int col, boolean exp, boolean leaf) {
			return defaultTreeCellRenderer.getTreeCellRendererComponent(
					tree, val, sel, exp, leaf, row, foc);
		}
				

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object val, boolean sel, boolean foc, int row, int col) {
			tableColumn = true;
			treeColumn = -1;
			component = getTableComponent(table, val, sel, foc, row, col);
			return this;
		}

		private Component getTableComponent(JTable table, Object val,
				boolean sel, boolean foc, int row, int col) {
			return treeTable.getCellRenderer(row, col).getTreeTableCellRendererComponent(
					treeTable, val, sel, foc, row, col);
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object val,
				boolean sel, boolean exp, boolean leaf, int row, boolean foc) {
			tableColumn = false;
			if (treeColumn < 0)
				treeColumn = treeTable.getHierarchialColumn();
			
			node = val;
			this.row = row;
			
			// implement JTable's selection idioms.
			sel = treeColumnSelected || sel && rowSelectionAllowed;

			// Discrepancy between how JTree and JTable paints focus on some
			//  L&F's so use focusRenderer to paint tree's focus same as table.
			foc = false;

			TreeColumnModel model = treeTable.getTreeColumnModel();
			val = model.getValueAt(val, model.getHierarchialColumn());
			component = getTreeComponent(tree, val,
					sel, foc, row, treeColumn, exp, leaf);
			return this;
		}

		private Component getTreeComponent(JTree tree, Object val, boolean sel,
				boolean foc, int row, int col, boolean exp, boolean leaf) {
			return treeTable.getCellRenderer(row, col).getTreeTableCellRendererComponent(
					treeTable, val, sel, foc, row, col, exp, leaf);
		}

		@Override
		public Dimension getPreferredSize() {
			if (tableColumn || tree.getRowHeight() > 0)
				return component.getPreferredSize();
			int margin = treeTable.getRowMargin();
			Dimension size;
			int tc = treeColumn;
			if (tc >= 0) {
				size = component.getPreferredSize();
				size.height += margin;
			} else {
				size = new Dimension(1, margin);
			}
			JTable tbl = table;
			TableColumnModel cm = tbl.getColumnModel();
			TreeColumnModel rm = treeTable.getTreeColumnModel();
			Object nod = node;
			// TODO (TBD) use row -1 because this can be called
			// before the row is valid in the table?
			//int row = -1;
			for (int col=cm.getColumnCount(); --col>=0;) {
				if (col == tc)
					continue;
				Object val = rm.getValueAt(nod, cm.getColumn(col).getModelIndex());
				Component c = getTableComponent(tbl, val, false, false, row, col);
				size.height = Math.max(size.height, c.getPreferredSize().height + margin);
			}
			return size;
		}
		
		@Override
		public void paint(Graphics g) {
			if (tableColumn) {
				paintTableComponent(g, component);
			} else {
				paintTreeComponent(g, component);
			}
		}
		
		protected void paintTableComponent(Graphics g, Component c) {
			paintComponent(g, c, table, 0, 0, getWidth(), getHeight(), true);
		}
		
		protected void paintTreeComponent(Graphics g, Component c) {
			int x = 0;
			int y = 0;
			int w = getWidth();
			int h = getHeight();
			if (getX() + w > tree.getWidth())
				w = tree.getWidth() - getX();
			component.setBounds(0, 0, w, h);
			
			int margin = treeTable.getRowMargin();
			int mod = margin % 2;
			margin /= 2;
			y += margin;
			h -= margin + mod;
			
			if (c.getParent() != this)
				add(c);
			boolean wasDoubleBuffered = false;
			if ((c instanceof JComponent) && ((JComponent)c).isDoubleBuffered()) {
				wasDoubleBuffered = true;
				((JComponent)c).setDoubleBuffered(false);
			}
			Graphics cg = g.create(x, y, w, h);
			try {
				cg.translate(0, -margin - mod);
				cg.clipRect(0, 0, w, h);
				c.paint(cg);
			} finally {
				cg.dispose();
			}
			if (wasDoubleBuffered && (c instanceof JComponent))
				((JComponent)c).setDoubleBuffered(true);
			c.setBounds(-w, -h, 0, 0);
		}
		
		/**
		 * Report the TreeTable's opacity since renderers will
		 * assume this component is this TreeTable.
		 */
		@Override
		public boolean isOpaque() {
			Container p = getParent();
			return p != null ? p.isOpaque() : super.isOpaque();
		}
		
		
		@Override
		public void repaint() {}
		@Override
		public void repaint(long tm, int x, int y, int w, int h) {}

		@Override
		protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
		@Override
		public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}
		@Override
		public void firePropertyChange(String propertyName, char oldValue, char newValue) {}
		@Override
		public void firePropertyChange(String propertyName, short oldValue, short newValue) {}
		@Override
		public void firePropertyChange(String propertyName, int oldValue, int newValue) {}
		@Override
		public void firePropertyChange(String propertyName, long oldValue, long newValue) {}
		@Override
		public void firePropertyChange(String propertyName, float oldValue, float newValue) {}
		@Override
		public void firePropertyChange(String propertyName, double oldValue, double newValue) {}
		@Override
		public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}

	}
	
	
	
	private class TreeEditor extends DefaultTreeCellEditor implements TableCellEditor {

		public TreeEditor() {
			super(getTree(), (DefaultTreeCellRenderer)defaultTreeCellRenderer);
		}
		
		@Override
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean selected, int row, int column) {
			selected = table.isRowSelected(row);
			TreePath path = tree.getPathForRow(row);
			boolean expanded = tree.isExpanded(path);
			boolean leaf = tree.getModel().isLeaf(path.getLastPathComponent());
			return getTreeCellEditorComponent(tree, value, selected, expanded, leaf, row);
		}
		
		public boolean isCellEditable(EventObject obj) {
			// TODO ...
			if (obj instanceof MouseEvent) {
				MouseEvent e = (MouseEvent)obj;
				return e.getClickCount() == 2;
			}
			return false;
		}
		
	}
	
	private class TreeEditorContainer extends Container {
		
		TreeEditorContainer() {
		}
		
		private Component component;
		
		private int row;
		
		private int column;
		
		public void setBounds(int x, int y, int w, int h) {
			Rectangle node = tree.getRowBounds(row);
			if (tree.getComponentOrientation().isLeftToRight()) {
				x = node.x;
				w -= node.x;
				component.setBounds(0, 0, w, h);
			} else {
				w = node.x + node.width;
				component.setBounds(0, 0, w, h);
			}
			super.setBounds(x, y, w, h);
		}
		
		void setState(Component c, int row, int column) {
			add(c);
			component = c;
			this.row = row;
			this.column = column;
		}
		
		void clearState() {
			if (component != null) {
				remove(component);
				component = null;
			}
		}

	}
	
	protected class Editor extends AbstractCellEditor implements
			TableCellEditor, CellEditorListener, TreeTableCellEditor {

		private TreeTableCellEditor treeTableEditor;
		
		private TableCellEditor tableEditor;
		
		private TreeEditor defaultTreeEditor;
		
		private TreeEditorContainer treeEditorContainer;
		
		public void loadEditor(int row, int col) {
			treeTableEditor = treeTable.getCellEditor(row, col);
			if (treeTableEditor == this) {
				if (col == treeTable.getHierarchialColumn()) {
					if (defaultTreeEditor == null)
						defaultTreeEditor = new TreeEditor();
					tableEditor = defaultTreeEditor;
				} else {
					tableEditor = table.getDefaultEditor(table.getColumnClass(col));
				}
			}
		}
		
		@Override
		public Component getTreeTableCellEditorComponent(TreeTable treeTable,
				Object val, boolean sel, int row, int col) {
			return tableEditor.getTableCellEditorComponent(
					table, val, sel, row, col);
		}
		
		@Override
		public Component getTreeTableCellEditorComponent(TreeTable treeTable,
				Object val, boolean sel, int row, int col, boolean exp, boolean leaf) {
			return defaultTreeEditor.getTreeCellEditorComponent(
					tree, val, sel, exp, leaf, row);
		}


		
		@Override
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {
			Component c;
			boolean treeColumn = column == treeTable.getHierarchialColumn();
			if (treeColumn) {
				TreePath path = tree.getPathForRow(row);
				boolean expanded = tree.isExpanded(path);
				boolean leaf = tree.getModel().isLeaf(path.getLastPathComponent());
				c = treeTableEditor.getTreeTableCellEditorComponent(
						treeTable, value, isSelected, row, column, expanded, leaf);	
			} else {
				c = treeTableEditor.getTreeTableCellEditorComponent(
						treeTable, value, isSelected, row, column);
			}
			if (treeColumn) {
				if (treeEditorContainer == null)
					treeEditorContainer = new TreeEditorContainer();
				treeEditorContainer.setState(c, row, column);
				c = treeEditorContainer;
			}
			getEditor().addCellEditorListener(this);
			return c;
		}
		
		private CellEditor getEditor() {
			return tableEditor != null ? tableEditor : treeTableEditor;
		}

		@Override
		public void cancelCellEditing() {
			getEditor().cancelCellEditing();
		}

		@Override
		public Object getCellEditorValue() {
			return getEditor().getCellEditorValue();
		}

		@Override
		public boolean isCellEditable(EventObject anEvent) {
			return getEditor().isCellEditable(anEvent);
		}

		@Override
		public boolean shouldSelectCell(EventObject anEvent) {
			return getEditor().shouldSelectCell(anEvent);
		}

		@Override
		public boolean stopCellEditing() {
			return getEditor().stopCellEditing();
		}

		@Override
		public void editingCanceled(ChangeEvent e) {
			fireEditingCanceled();
			clearState();
		}

		@Override
		public void editingStopped(ChangeEvent e) {
			fireEditingStopped();
			clearState();
		}
		
		public void clearState() {
			if (treeTableEditor != null) {
				getEditor().removeCellEditorListener(this);
				treeTableEditor = null;
				tableEditor = null;
				if (treeEditorContainer != null)
					treeEditorContainer.clearState();
			}
		}
		
	}
	
	

	private class FocusRenderer extends DefaultTableCellRenderer
			implements TreeTableCellRenderer, UIResource {

		@Override
		public Component getTreeTableCellRendererComponent(TreeTable treeTable,
				Object value, boolean selected, boolean hasFocus, int row,
				int column) {
			return super.getTableCellRendererComponent(
					table, value, selected, hasFocus, row, column);
		}

		@Override
		public Component getTreeTableCellRendererComponent(TreeTable treeTable,
				Object value, boolean selected, boolean hasFocus, int row,
				int column, boolean expanded, boolean leaf) {
			return super.getTableCellRendererComponent(
					table, value, selected, hasFocus, row, column);
		}
		
		@Override
		public boolean isOpaque() { return false; }

	}

	
	private int treeHandleWidth = -1;

	/**
	 * If a negative number is returned, then all events that occur in the
	 * leading margin will be forwarded to the tree and consumed.
	 * 
	 * @return the width of the tree handle if it can be determined, else -1
	 */
	protected int getTreeHandleWidth() {
		return treeHandleWidth;
	}
	
	protected void processKeyBinding(JComponent c, KeyStroke ks,
			KeyEvent e, int condition, boolean pressed) {
		if (c instanceof ProcessKeyBinding)
			((ProcessKeyBinding)c).processKeyBinding(
					ks, e, condition, pressed);
	}
	
	protected class Handler extends MouseAdapter
			implements KeyListener, PropertyChangeListener {
		
		public Handler() {
		}

		@Override
		public void keyPressed(KeyEvent e) {
			processKeyEvent(e);
		}

		@Override
		public void keyReleased(KeyEvent e) {
			processKeyEvent(e);
		}

		@Override
		public void keyTyped(KeyEvent e) {
			processKeyEvent(e);
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			processMouseEvent(e);
		}
		
		@Override
		public void mouseReleased(MouseEvent e) {
			processMouseEvent(e);
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			processMouseEvent(e);
		}
		
		/**
		 * Implementation of Key Binding order:
		 * 		treeTable WHEN_FOCUS
		 * 		treeTable WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
		 * 		table or tree WHEN_FOCUSED
		 * 		table or tree WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
		 * 		treeTable WHEN_IN_FOCUS_WINDOW
		 * 
		 * KeyListeners are notified before the Key Binding mechanism.
		 * 
		 * TODO Note: JTable's default (crude) implementation of
		 * editing upon any key press is circumvented with this
		 * implementation (what a shame). A similar (better) operation
		 * needs to be conducted.
		 */
		protected void processKeyEvent(KeyEvent e) {
			KeyStroke ks = KeyStroke.getKeyStrokeForEvent(e);
			int condition = treeTable.getConditionForKeyStroke(ks);
			if (condition == JComponent.WHEN_FOCUSED ||
					condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
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
				return (treeTable.isColumnFocusEnabled() || treeTable.getColumnSelectionAllowed()) &&
						table.getColumnModel().getColumnCount() > 1;
			}
			return true;
		}
		
		private boolean dispatchKeyEvent(KeyEvent e, KeyStroke ks, JComponent c) {
			int condition = c.getConditionForKeyStroke(ks);
			if (condition == JComponent.WHEN_FOCUSED ||
					condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
				processKeyBinding(c, ks, e, condition, !ks.isOnKeyRelease());
				e.consume();
				return true;
			}
			return false;
		}
		
		/**
		 * Dispatch the MouseEvent to the table or tree.
		 */
		protected void processMouseEvent(MouseEvent e) {
			if (e.isConsumed() || e.isPopupTrigger())
				return;
			if (!dispatchToTree(e))
				dispatchMouseEvent(e, table);
		}

		private boolean dispatchToTree(MouseEvent e) {
			switch (e.getID()) {
			case MouseEvent.MOUSE_ENTERED:
			case MouseEvent.MOUSE_EXITED:
				return false;
			}
			int hier = treeTable.getHierarchialColumn();
			if (hier < 0)
				return false;
			Point pt = e.getPoint();
			int col = table.columnAtPoint(pt);
			if (col != hier)
				return false;
			int row = table.rowAtPoint(pt);
			if (row < 0)
				return false;
			TreeModel tm = tree.getModel();
			TreePath path = tree.getPathForRow(row);
			Object node = path.getLastPathComponent();
			// Check if the node has a tree handle
			if (tm.isLeaf(node) || (tm.getChildCount(node) <= 0
					&& !tree.hasBeenExpanded(path)))
				return false;
			// Check if the event location falls over the tree handle.
			int x = e.getX() - tree.getX();
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
		public void propertyChange(PropertyChangeEvent evt) {
			String name = evt.getPropertyName();
			if (name == "rowSorter") {
				table.setRowSorter(evt.getNewValue() == null ? null : new RowSorterAdapter());
			} else if (name == "enabled") {
				boolean enabled = (Boolean)evt.getNewValue();
				table.setEnabled(enabled);
				tree.setEnabled(enabled);
			} else if (name == "componentOrientation") {
				ComponentOrientation o = (ComponentOrientation)evt.getNewValue();
				table.setComponentOrientation(o);
				tree.setComponentOrientation(o);
				treeTable.revalidate();
				treeTable.repaint();
			} else if ("JTree.lineStyle".equals(name)) {
				tree.putClientProperty("JTree.lineStyle", evt.getNewValue());
			}
		}
		
		
	}
}
