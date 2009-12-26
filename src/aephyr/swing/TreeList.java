package aephyr.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.lang.reflect.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.plaf.*;



import com.sun.java.swing.Painter;
import com.sun.java.swing.plaf.nimbus.*;
//import javax.swing.plaf.nimbus.*;

//import org.pushingpixels.substance.api.*;
//import org.pushingpixels.substance.api.skin.*;
//import org.pushingpixels.substance.internal.ui.*;
//import org.pushingpixels.substance.internal.animation.*;
//import org.pushingpixels.trident.Timeline.TimelineState;


public class TreeList extends JTree {
	
	static boolean isListNode(TreePath path) {
		return path.getLastPathComponent() instanceof ListModel;
	}
	

	
	public TreeList(TreeModel model) {
		this(model, null);
	}
	
	public TreeList(TreeModel model, Class<? extends JList> listClass) {
		super(model);
		if (listClass != null)
			putClientProperty(JList.class, listClass);
		renderer = new Renderer(createDefaultList(listClass));
		editor = new Editor(new Renderer(createDefaultList(listClass)));
		updateUI();
	}
	
	
	JViewport viewportParent;
	
	public void addNotify() {
		Container parent = getParent();
		if (parent instanceof JViewport) {
			viewportParent = (JViewport)parent;
			parent.addComponentListener(handler);
		}
		pathRevalidator = new PathRevalidator(true);
		super.addNotify();
	}
	
	public void removeNotify() {
		if (viewportParent != null) {
			viewportParent.removeComponentListener(handler);
			viewportParent = null;
		}
		super.removeNotify();
	}
	
	
	// TreeList's state fields/methods

	int listFixedCellWidth = -1;
	
	int listFixedCellHeight = -1;
	
	int listHorizontalAlignment = JLabel.LEFT;
	
	int listLayoutOrientation = JList.HORIZONTAL_WRAP;
	
	Handler handler;
	
	Editor editor;
	
	Renderer renderer;
	
	public void setEditor(Editor editor) {
		this.editor = editor;
	}
	
	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}
	
	public void setHandler(Handler newHandler) {
		if (newHandler == null)
			throw new NullPointerException();
		if (newHandler != handler) {
			Handler oldHandler = handler;
			handler = newHandler;
			if (oldHandler != null) {
				removeTreeSelectionListener(oldHandler);
			}
			addTreeSelectionListener(newHandler);
			if (viewportParent != null) {
				if (oldHandler != null)
					viewportParent.removeComponentListener(oldHandler);
				viewportParent.addComponentListener(newHandler);
			}
			firePropertyChange("selectionHandler", oldHandler, newHandler);
		}
	}
	
	public Handler getHandler() {
		return handler;
	}
	
	/**
	 * @return the fixed cell width for the JList renderer
	 * 
	 * @see #setFixedCellWidth()
	 */
	public int getListFixedCellWidth() {
		return listFixedCellWidth;
	}
	
	/**
	 * Sets the fixed cell width for the JList renderer.
	 * 
	 * @param width the width of cells renderer by JList
	 * 
	 * @see JList#setFixedCellWidth()
	 * @see #getFixedCellWidth()
	 */
	public void setListFixedCellWidth(int width) {
		int oldValue = listFixedCellWidth;
		listFixedCellWidth = width;
		renderer.list.setFixedCellWidth(width);
		editor.renderer.list.setFixedCellWidth(width);
		firePropertyChange("listFixedCellWidth", oldValue, width);
		revalidateLists();
	}
	
	/**
	 * @return the fixed cell height for the JList renderer
	 * 
	 * @see #setFixedCellHeight()
	 */
	public int getListFixedCellHeight() {
		return listFixedCellHeight;
	}
	
	/**
	 * Sets the fixed cell height for the JList renderer.
	 * 
	 * @param height the height of cells renderer by JList
	 * 
	 * @see JList#setFixedCellHeight()
	 * @see #getFixedCellHeight()
	 */
	public void setListFixedCellHeight(int height) {
		int oldValue = listFixedCellHeight;
		listFixedCellHeight = height;
		renderer.list.setFixedCellHeight(height);
		editor.renderer.list.setFixedCellHeight(height);
		firePropertyChange("listFixedCellHeight", oldValue, height);
		revalidateLists();
	}
	
	public int getListHorizontalAlignment() {
		return listHorizontalAlignment;
	}
	
	public void setListHorizontalAlignment(int align) {
		int oldValue = listHorizontalAlignment;
		listHorizontalAlignment = align;
		if (nodeCellRenderer instanceof JLabel) {
			((JLabel) nodeCellRenderer).setHorizontalAlignment(align);
		}
		firePropertyChange("listHorizontalAlignment", oldValue, align);
		revalidateLists();
	}
	
	/**
	 * Sets the layout orientation for the JList renderer.
	 * 
	 * @param orientation JList.HORIZONTAL_WRAP or JList.VERTICAL_WRAP
	 * 
	 * @throws IllegalArgumentException if <code>orientation</code> is not one of the
	 * 		specified values
	 * 
	 * @see JList#setLayoutOrientation(int)
	 * @see #getListLayoutOrientation()
	 */
	public void setListLayoutOrientation(int orientation) {
		if (orientation != JList.HORIZONTAL_WRAP && orientation != JList.VERTICAL_WRAP)
			throw new IllegalArgumentException();
		int oldValue = listLayoutOrientation;
		listLayoutOrientation = orientation;
		renderer.list.setLayoutOrientation(orientation);
		editor.renderer.list.setLayoutOrientation(orientation);
		firePropertyChange("listLayoutOrientation", oldValue, orientation);
		revalidateLists();
	}
	
	public int getListLayoutOrientation() {
		return listLayoutOrientation;
	}
	
	
	public TreeCellEditor getNodeCellEditor() {
		return nodeCellEditor;
	}
	
	public TreeCellRenderer getNodeCellRenderer() {
		return nodeCellRenderer;
	}

	protected JList createDefaultList() {
		Object p = getClientProperty(JList.class);
		Class<? extends JList> c = p instanceof Class<?> ? (Class<? extends JList>)p : null;
		return createDefaultList(c != null && c.isAssignableFrom(JList.class) ? c : null);
	}
	
	private JList createDefaultList(Class<? extends JList> listClass) {
		JList list;
		try {
			list = listClass != null ? listClass.newInstance() : new JList();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		list.setLayoutOrientation(listLayoutOrientation);
		list.setVisibleRowCount(0);
		list.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		list.setOpaque(false);
		list.setInheritsPopupMenu(true);
		return list;
	}
	

	
	
	
	public class Handler extends ComponentAdapter implements TreeSelectionListener {
		
		protected boolean processSelectionAddedPaths = true;
		protected boolean processSelectionClearedPaths = true;
		
		void setProcessSelection(boolean b) {
			processSelectionAddedPaths = processSelectionClearedPaths = b;
		}
		
		public void valueChanged(TreeSelectionEvent e) {
			if (processSelectionClearedPaths || processSelectionAddedPaths) {
				TreePath[] paths = e.getPaths();
				for (int i=paths.length; --i>=0;) {
					if (isListNode(paths[i])) {
						ListHandler hlr = getListHandler(paths[i], null);
						if (e.isAddedPath(i)) {
							if (processSelectionAddedPaths)
								hlr.selectAll(true);
						} else {
							if (processSelectionClearedPaths)
								hlr.clearSelection();
						}
					}
				}
			}
			fireSelectionEvent(e);
		}
		
		public void componentResized(ComponentEvent e) {
			if (viewportParent.getWidth() != lastWidth) {
				lastWidth = viewportParent.getWidth();
				if (pathRevalidator != null) {
					pathRevalidator.revalidateAll = true;
				} else {
					pathRevalidator = new PathRevalidator(true);
				}
			}
		}
		
		protected void processMouseEvent(MouseEvent e) {
			if (isSelectEvent(e)) {
				int row = getRowForLocation(e.getX(), e.getY());
				if (row < 0) {
					row = getClosestRowForLocation(e.getX(), e.getY());
					Rectangle rowBounds = getRowBounds(row);
					if (e.getY() >= rowBounds.y && e.getY() <= rowBounds.y+rowBounds.height) {
						// within the Y coordinates of the row
						if (getComponentOrientation().isLeftToRight() ?
								e.getX() < rowBounds.x :
								e.getX() > rowBounds.x+rowBounds.width) {
							// tree handle and margin space
							return;
						}
					} else {
						row = -1;
					}
				}
				boolean consume = true;
				processSelectionAddedPaths = false;
				if (row >= 0) {
					if (isMultiSelectEvent(e)) {
						TreePath anchorPath = getAnchorSelectionPath();
						int anchorRow = getRowForPath(anchorPath);
						TreePath[] paths = getPathBetweenRows(anchorRow, row);
						if (isToggleSelectionEvent(e)) { // add selection interval from anchor
							addSelectionPaths(paths);
						} else { // set selection interval from anchor
							setSelectionPaths(paths);
						}
						if (anchorRow != row) {
							boolean ascending = anchorRow < row;
							int incr = ascending ? 1 : -1;
							int i = ascending ? 0 : paths.length-1;
							int leadIndex = ascending ? paths.length-1 : 0;
							if (isListNode(anchorPath)) {
								getListHandler(anchorPath, null).extendSelection(ascending);
							}
							while ((i+=incr) != leadIndex) {
								if (isListNode(paths[i])) {
									getListHandler(paths[i], null).selectAll(ascending);
								}
							}
							if (isListNode(paths[leadIndex])) {
								getListHandler(paths[leadIndex], null).prepareSelectionChange(ascending);
								consume = false;
							}
						} else {
							if (isListNode(anchorPath)) {
								consume = false;
							}
						}
					} else if (isToggleSelectionEvent(e)) { // add selection row
						TreePath path = getPathForRow(row);
						if (isListNode(path)) {
							consume = false;
						} else {
							addSelectionPath(path);
						}
					} else { // set selection row
						TreePath path = getPathForRow(row);
						if (isListNode(path)) {
							clearSelection();
							consume = false;
						} else {
							setSelectionPath(path);
						}
					}
				} else { // clear selection
					clearSelection();
				}
				processSelectionAddedPaths = true;
				if (consume) {
					e.consume();
					requestFocusInWindow();
				}
			} else if (isTreeEditable() && isStartEditingEvent(e)) {
				TreePath path = getPathForLocation(e.getX(), e.getY());
				if (path != null && !isListNode(path))
					startEditingAtPath(path);
			}
		}
		
		protected boolean isSelectEvent(MouseEvent e) {
			return e.getID() == MouseEvent.MOUSE_PRESSED && e.getButton() == MouseEvent.BUTTON1;
		}
		
		protected boolean isMultiSelectEvent(MouseEvent e) {
			return e.isShiftDown();
		}
		
		protected boolean isToggleSelectionEvent(MouseEvent e) {
			return e.isControlDown();
		}
		
		protected boolean isStartEditingEvent(MouseEvent e) {
			return e.getID() == MouseEvent.MOUSE_CLICKED
				&& e.getClickCount() == 2
				&& e.getButton() == MouseEvent.BUTTON1;
		}

		protected void processMouseMotionEvent(MouseEvent e) {}
		
	}
	
	
	public boolean isTreeEditable() {
		return super.isEditable();
	}
	
	// Overridden methods from JTree
	
	@Override
	protected void processMouseEvent(MouseEvent e) {
		handler.processMouseEvent(e);
		super.processMouseEvent(e);
	}
	
	@Override
	protected void processMouseMotionEvent(MouseEvent e) {
		handler.processMouseMotionEvent(e);
		super.processMouseMotionEvent(e);
	}
	
	@Override
	protected void processComponentEvent(ComponentEvent e) {
		if (viewportParent == null && e.getID() == ComponentEvent.COMPONENT_RESIZED && getWidth() != lastWidth) {
			lastWidth = getWidth();
			revalidateLists();
		}
		super.processComponentEvent(e);
	}
	
	
	@Override
	public void updateUI() {
		// ignore call from JTree constructor
		if (renderer == null)
			return;
		// save the ui some troubles between now and when
		// super.updateUI() is called. This is also
		// NECESSARY to prevent NPE.
		if (ui != null)
			setUI(null);
		if (nodeCellRenderer != null) {
			if (nodeCellRenderer instanceof UIResource) {
				nodeCellRenderer = null;
				super.setCellRenderer(null);
			}
			renderer.updateUI();
		}
		if (nodeCellEditor != null) {
			if (nodeCellEditor instanceof UIResource) {
				nodeCellEditor = null;
				super.setCellEditor(null);
			}
			editor.updateUI();
		}
		if (UIManager.getLookAndFeel() instanceof NimbusLookAndFeel) {
			UIDefaults map = new UIDefaults();
			map.put("Tree[Enabled+Selected].collapsedIconPainter", UIManager.get("Tree[Enabled].collapsedIconPainter"));
			map.put("Tree[Enabled+Selected].expandedIconPainter", UIManager.get("Tree[Enabled].expandedIconPainter"));
			Painter<JComponent> painter = new Painter<JComponent>() {
				public void paint(Graphics2D g, JComponent c, int w, int h) {}
			};
			map.put("Tree:TreeCell[Enabled+Selected].backgroundPainter", painter);
			map.put("Tree:TreeCell[Focused+Selected].backgroundPainter", painter);
			putClientProperty("Nimbus.Overrides", map);
			if (nodeCellRenderer == null) {
				setCellRenderer(new NimbusCellRenderer());
				super.setCellRenderer(renderer);
			}
		} else {
			if (getClientProperty("Nimbus.Overrides") != null) {
				putClientProperty("Nimbus.Overrides", null);
			}
		}
		// set a model whose sole raison d'être is to
		// steal a reference to the UI's tree model listener
		TreeModel model = getModel();
		setModel(new Interceptor());
		super.updateUI();
		setModel(model);
		if (handler == null)
			setHandler(new Handler());
		super.setCellEditor(editor);
		super.setCellRenderer(renderer);
		if (getRowHeight() != 0)
			setRowHeight(0);
	}
	
	static class NimbusCellRenderer extends DefaultTreeCellRenderer implements UIResource {
		NimbusCellRenderer() {
			focusedPainter = (Painter<JComponent>)UIManager.get("Tree:TreeCell[Enabled+Focused].backgroundPainter");
			selectedPainter = (Painter<JComponent>)UIManager.get("Tree:TreeCell[Enabled+Selected].backgroundPainter");
			selectedFocusedPainter = (Painter<JComponent>)UIManager.get("Tree:TreeCell[Focused+Selected].backgroundPainter");
		}
		Painter<JComponent> focusedPainter;
		Painter<JComponent> selectedPainter;
		Painter<JComponent> selectedFocusedPainter;
		
		public void paint(Graphics g) {
			Painter<JComponent> painter = null;
			if (selected) {
				painter = hasFocus ? selectedFocusedPainter : selectedPainter;
			} else if (hasFocus) {
				painter = focusedPainter;
			}
			if (painter != null) {
				Graphics2D g2 = (Graphics2D)g;
				painter.paint(g2, this, getWidth(), getHeight());
			}
			super.paint(g);
		}
		
		
	}
	
	TreeCellRenderer nodeCellRenderer;
	
	TreeCellEditor nodeCellEditor;
	
	@Override
	public void setCellEditor(TreeCellEditor cellEditor) {
		nodeCellEditor = cellEditor;
		if (getCellEditor() == null)
			super.setCellEditor(cellEditor);
	}
	
	@Override
	public void setCellRenderer(TreeCellRenderer cellRenderer) {
		nodeCellRenderer = cellRenderer;
		if (cellRenderer instanceof JLabel) {
			((JLabel) nodeCellRenderer).setHorizontalAlignment(listHorizontalAlignment);
		}
		if (getCellRenderer() == null)
			super.setCellRenderer(cellRenderer);
	}
	

	@Override
	public boolean isEditable() {
		return true;
	}
	
	@Override
	public boolean isPathEditable(TreePath path) {
		return super.isEditable() || isListNode(path);
	}
	
	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect,
			int orientation, int direction) {
		TreePath path = getClosestPathForLocation(0, visibleRect.y);
		if (path != null && isListNode(path)) {
			int unit = listFixedCellHeight;
			if (unit < 0) {
				//TODO
				unit = 20; // dummy value
			}
			return unit;
		} else {
			return super.getScrollableUnitIncrement(visibleRect, orientation, direction);
		}
	}
	
	@Override
	public void setRootVisible(boolean rootVisible) {
		super.setRootVisible(rootVisible);
		renderer.totalChildIndent = -1;
		editor.renderer.totalChildIndent = -1;
		revalidateLists();
	}
	
	@Override
	public void setShowsRootHandles(boolean newValue) {
		super.setShowsRootHandles(newValue);
		renderer.totalChildIndent = -1;
		editor.renderer.totalChildIndent = -1;
		revalidateLists();
	}
	
	@Override
	public void setEditable(boolean editable) {
		if (editable) {
			boolean wasNull = nodeCellEditor == null;
			if (wasNull)
				super.setCellEditor(null);
			super.setEditable(editable);
			if (wasNull)
				super.setCellEditor(editor);
		} else {
			super.setEditable(editable);
			if (nodeCellEditor instanceof UIResource)
				nodeCellEditor = null;
		}
		
	}
	
	@Override
	public void setComponentOrientation(ComponentOrientation o) {
		super.setComponentOrientation(o);
		revalidateLists();
	}
	
	
	// TreeList's internal maintenance fields/methods
	
	
	int lastWidth = -1;
	
	Map<TreePath,ListHandler> listHandlers = new HashMap<TreePath,ListHandler>();

	PathRevalidator pathRevalidator;
	
	TreeModelListener uiModelListener;
	
	private class Interceptor extends DefaultTreeModel {
		Interceptor() {
			super(new DefaultMutableTreeNode());
		}
		
		public void addTreeModelListener(TreeModelListener l) {
			// the ui listener will be sent last, so this will do it..
			uiModelListener = l;
		}
	}
	
	private class PathRevalidator implements Runnable {
		
		PathRevalidator(boolean all) {
			System.out.println("PathREvalidator "+all);
			revalidateAll = all;
			EventQueue.invokeLater(this);
		}
		
		Set<TreePath> paths;
		
		boolean revalidateAll;
		
		void addPath(TreePath path) {
			if (revalidateAll)
				return;
			if (paths == null)
				paths = new HashSet<TreePath>();
			paths.add(path);
		}
		
		void addPaths(Collection<TreePath> c) {
			if (revalidateAll)
				return;
			if (paths == null) {
				paths = new HashSet<TreePath>(c);
			} else {
				paths.addAll(c);
			}
		}
		
		public void run() {
			pathRevalidator = null;
			TreePath editingPath = getEditingPath();
			if (editingPath != null)
				stopEditing();
			System.out.println("run " + revalidateAll + " " + paths);
			if (revalidateAll) {
				for (int i=0, j=getRowCount(); i<j; i++) {
					TreePath path = getPathForRow(i);
					validatePath(path);
				}
			} else if (paths != null) {
				for (TreePath path : paths) {
					validatePath(path);
				}
			}
			if (editingPath != null)
				startEditingAtPath(editingPath);
		}
	}
	

	private void validatePath(TreePath path) {
		if (isListNode(path)) {
			ListHandler hlr = getListHandler(path, null);
			hlr.size = null;
		}
		if (getRowForPath(path) >= 0) {
			TreePath parent = path.getParentPath();
			Object[] child = { path.getLastPathComponent() };
			int[] idx = { treeModel.getIndexOfChild(parent.getLastPathComponent(), child[0]) };
			uiModelListener.treeNodesChanged(new TreeModelEvent(treeModel, parent, idx, child));
		}
	}
	
	
	protected void revalidateLists() {
		System.out.println("revalidateLists " + pathRevalidator);
		if (pathRevalidator == null)
			pathRevalidator = new PathRevalidator(false);
		pathRevalidator.addPaths(listHandlers.keySet());
	}
	
	
	
	protected void revalidatePath(TreePath path) {
		if (pathRevalidator == null)
			pathRevalidator = new PathRevalidator(false);
		pathRevalidator.addPath(path);
	}
	
	protected void revalidateRow(int row) {
		revalidatePath(getPathForRow(row));
	}

	
	Map<TreePath,PathHandler> pathHandlers = new HashMap<TreePath,PathHandler>();
	
	PathHandler getPathHandler(TreePath path) {
		PathHandler hlr = pathHandlers.get(path);
		if (hlr == null) {
			hlr = new PathHandler(path);
			pathHandlers.put(path, hlr);
		}
		return hlr;
	}
	
	
	protected ListHandler getListHandler(TreePath path, ListModel mdl) {
		ListHandler hlr = listHandlers.get(path);
		if (hlr == null) {
			// mdl should never be null if the hlr isn't initialized
			// if a NPE arises, do not change anything here..
			// the problem is elsewhere
			if (mdl == null)
				return null;
			hlr = new ListHandler(mdl, path);
			listHandlers.put(path, hlr);
			pathHandlers.put(path, hlr);
		}
		return hlr;
	}

	class Actions extends AbstractAction {
		// some action keys defined in BasicListUI
		private static final String SELECT_PREVIOUS_COLUMN 				= "selectPreviousColumn";
		private static final String SELECT_PREVIOUS_COLUMN_EXTEND 		= "selectPreviousColumnExtendSelection";
		private static final String SELECT_PREVIOUS_COLUMN_CHANGE_LEAD 	= "selectPreviousColumnChangeLead";
		private static final String SELECT_NEXT_COLUMN 					= "selectNextColumn";
		private static final String SELECT_NEXT_COLUMN_EXTEND			= "selectNextColumnExtendSelection";
		private static final String SELECT_NEXT_COLUMN_CHANGE_LEAD		= "selectNextColumnChangeLead";
		private static final String SELECT_PREVIOUS_ROW					= "selectPreviousRow";
		private static final String SELECT_PREVIOUS_ROW_EXTEND			= "selectPreviousRowExtendSelection";
		private static final String SELECT_PREVIOUS_ROW_CHANGE_LEAD		= "selectPreviousRowChangeLead";
		private static final String SELECT_NEXT_ROW						= "selectNextRow";
		private static final String SELECT_NEXT_ROW_EXTEND				= "selectNextRowExtendSelection";
		private static final String SELECT_NEXT_ROW_CHANGE_LEAD			= "selectNextRowChangeLead";
		private static final String SELECT_FIRST_ROW					= "selectFirstRow";
		private static final String SELECT_FIRST_ROW_EXTEND				= "selectFirstRowExtendSelection";
		private static final String SELECT_FIRST_ROW_CHANGE_LEAD		= "selectFirstRowChangeLead";
		private static final String SELECT_LAST_ROW						= "selectLastRow";
		private static final String SELECT_LAST_ROW_EXTEND				= "selectLastRowExtendSelection";
		private static final String SELECT_LAST_ROW_CHANGE_LEAD			= "selectLastRowChangeLead";
		
		public Actions(String name, Action parentAction) {
			super(name);
			this.parentAction = parentAction;
		}
		
		Action parentAction;
		
		public void actionPerformed(ActionEvent e) {
			String name = (String)getValue(NAME);
			JList list = (JList)e.getSource();
			System.err.println(name);
			if (name.startsWith("selectPreviousRow")) {
				if (list.getLeadSelectionIndex() == 0) {
					selectPreviousRow(name, e);
					return;
				}
			} else if (name.startsWith("selectNextRow")) {
				if (list.getLeadSelectionIndex() == list.getModel().getSize()-1) {
					selectNextRow(name, e);
					return;
				}
			}
			parentAction.actionPerformed(e);
		}
		
		private int getRow() {
			TreePath path = TreeList.this.getEditingPath();
			return TreeList.this.getRowForPath(path);
		}
		
		private void selectPreviousRow(String name, ActionEvent e) {
			int row = getRow();
			if (row > 0) {
				if (name == SELECT_PREVIOUS_ROW) {
					TreeList.this.setSelectionRow(--row);
				} else if (name == SELECT_PREVIOUS_ROW_EXTEND) {
					parentAction.actionPerformed(e);
					TreeList.this.addSelectionRow(--row);
				} else if (name == SELECT_PREVIOUS_ROW_CHANGE_LEAD) {
					
				}
			}
		}
		
		private void selectNextRow(String name, ActionEvent e) {
			int row = getRow();
			if (row < TreeList.this.getRowCount()-1) {
				if (name == SELECT_NEXT_ROW) {
					TreeList.this.setSelectionRow(++row);
				} else if (name == SELECT_NEXT_ROW_EXTEND) {
					parentAction.actionPerformed(e);
					TreeList.this.addSelectionRow(++row);
				} else if (name == SELECT_NEXT_ROW_CHANGE_LEAD) {
					
				}
			}
		}
		
	}
	
	public class Editor extends AbstractCellEditor implements TreeCellEditor, MouseListener, ListSelectionListener {

		
		public Editor(Renderer renderer) {
			this.renderer = renderer;
			renderer.list.addMouseListener(this);
			renderer.list.addListSelectionListener(this);
			
			ActionMap actions = renderer.list.getActionMap();
			String[] names = {
					Actions.SELECT_PREVIOUS_ROW,
					Actions.SELECT_PREVIOUS_ROW_EXTEND,
					Actions.SELECT_PREVIOUS_ROW_CHANGE_LEAD,
					Actions.SELECT_NEXT_ROW,
					Actions.SELECT_NEXT_ROW_EXTEND,
					Actions.SELECT_NEXT_ROW_CHANGE_LEAD,
			};
			for (String name : names) {
				Action action = actions.get(name);
				actions.put(name, new Actions(name, action));
			}

		}
		
		protected Renderer renderer;

		protected boolean isList;

		
		public Component getTreeCellEditorComponent(JTree tree, Object value,
				boolean isSelected, boolean expanded, boolean leaf, int row) {
			isList = value instanceof ListModel;
			if (isList) {
				return renderer.getTreeCellRendererComponent(tree, value,
						isSelected, expanded, leaf, row, true);
			} else {
				return nodeCellEditor.getTreeCellEditorComponent(tree, value,
						isSelected, expanded, leaf, row);
			}
		}

		public Object getCellEditorValue() {
			if (isList) {
				isList = false;
				return null;
			} else {
				return nodeCellEditor.getCellEditorValue();
			}
		}
		
		public void updateUI() {
			renderer.updateUI();
		}
		
		public boolean shouldSelectCell(EventObject e) {
			return !isList;
		}

		public void mouseClicked(MouseEvent e) {
			if (e.getButton() != MouseEvent.BUTTON1
					|| handler.isMultiSelectEvent(e)
					|| handler.isToggleSelectionEvent(e))
				return;
			TreePath anchorPath = getAnchorSelectionPath();
			final TreePath editingPath = getEditingPath();
			final boolean pathIsAnchor = anchorPath != null && anchorPath.equals(editingPath);
			if (!pathIsAnchor || getMinSelectionRow() != getMaxSelectionRow()) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (getEditingPath() != editingPath)
							return;
						setSelectionPath(editingPath);
						if (!pathIsAnchor)
							setAnchorSelectionPath(editingPath);
						startEditingAtPath(editingPath);
					}
				});
			}
		}

		public void mouseEntered(MouseEvent e) {}

		public void mouseExited(MouseEvent e) {}

		public void mousePressed(MouseEvent e) {}

		public void mouseReleased(MouseEvent e) {}

		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting())
				return;
			TreePath path = getEditingPath();
			if (path != null) {
				getListHandler(path, null).updateSelection();
				startEditingAtPath(path);
				fireSelectionEvent(e, path);
			}
		}
		
	}

	private class RendererContainer extends Container {
		
		Component renderer;
		
		int row;
		
		TreePath path;
		
		void reset(Component c, int r) {
			renderer = c;
			row = r;
			path = getPathForRow(r);
			if (getComponentCount() != 1 || getComponent(0) != c) {
				removeAll();
				if (c != null)
					add(c);
			}
		}
		
		public void doLayout() {
			if (renderer != null) {
				if (row >= 0) {
					PathHandler hlr = getPathHandler(path);
					if (hlr.offset != getX()) {
						hlr.offset = getX();
						validatePath(path);
					}
				}
				renderer.setBounds(getX(), getY(), getWidth(), getHeight());
			}
		}
		
		public void paint(Graphics g) {
			if (renderer != null)
				renderer.paint(g);
		}
		
		public Dimension getPreferredSize() {
			Dimension s = renderer.getPreferredSize();
			PathHandler hlr = getPathHandler(path);
			if (hlr.offset >= 0) {
				int width = viewportParent != null ? viewportParent.getWidth() : TreeList.this.getWidth();
				width -= hlr.offset;
				s.width = width;
				return s;
			}
			return s;
		}
	}
	
	public class Renderer implements TreeCellRenderer, ListCellRenderer {
		
		public Renderer() {
			this(createDefaultList());
			
		}
		
		public Renderer(JList list) {
			this.list = list;
			selection = list.getSelectionModel();
			listCellRenderer = list.getCellRenderer();
			list.setCellRenderer(this);
			container = new RendererContainer();
		}
		
		protected JList list;
		
		protected ListSelectionModel selection;
		
		protected ListCellRenderer listCellRenderer;
		
		protected int listRow;
		
		private RendererContainer container;

		private int depthOffset;
		
		private int totalChildIndent = -1;
		
		protected int getRowX(TreePath path, int row) {
			if (totalChildIndent < 0) {
				if (isRootVisible()) {
					depthOffset = getShowsRootHandles() ? 1 : 0;
				} else {
					depthOffset = getShowsRootHandles() ? 0 : -1;
				}
				TreePath parentPath = path.getParentPath();
				Rectangle bounds = getPathBounds(parentPath);
				int depth = parentPath.getPathCount() - 1;
				if (depth + depthOffset == 0)
					return 0;
				totalChildIndent = bounds.x / (depth + depthOffset);
			}
			int depth = path.getPathCount() - 1;
			return totalChildIndent * (depth + depthOffset);
		}

		protected void prepareListRenderer(JTree tree, ListModel model,
				boolean sel, int row, boolean hasFocus) {
			list.setComponentOrientation(tree.getComponentOrientation());
			list.setFont(getFont());
			listRow = row;
			if (list.getModel() != model) {
				list.setSelectionModel(selection);
				list.setModel(model);
			}
			if (row < 0) {
				new Exception("model: "+model.toString()).printStackTrace();
				return;
			}
			TreePath path = tree.getPathForRow(row);
			if (path == null || path.getLastPathComponent() != model) {
				TreePath parentPath = tree.getPathForRow(row-1);
				path = parentPath.pathByAddingChild(model);
			}
			ListHandler hlr = getListHandler(path, model);
			if (list.getSelectionModel() != hlr.selection)
				list.setSelectionModel(hlr.selection);
			// TODO remove getRowX(TreePath,int) and move sizing responsibilities to container
			if (hlr.size == null) {
				int layout = list.getLayoutOrientation();
				if (layout != JList.HORIZONTAL_WRAP)
					list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
				list.setPreferredSize(null);
				int x = getRowX(path, row);
				int width = viewportParent != null ?
						viewportParent.getWidth() : TreeList.this.getWidth();
				list.setSize(width - x, Short.MAX_VALUE);
				hlr.size = list.getPreferredSize();
				if (layout != JList.HORIZONTAL_WRAP)
					list.setLayoutOrientation(layout);
			}
			list.setPreferredSize(hlr.size);
		}
		
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			if (value instanceof ListModel) {
				prepareListRenderer(tree, (ListModel)value, sel, row, hasFocus);
//				container.reset(list, row);
//				return container;
				return list;
			} else {
				Component c = nodeCellRenderer.getTreeCellRendererComponent(
						tree, value, sel, expanded, leaf, row, hasFocus);
				container.reset(c, row);
				return container;
			}
		}

		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			if (value instanceof Icon) {
				return listCellRenderer.getListCellRendererComponent(
						list, value, index, isSelected, cellHasFocus);
			} else {
				return nodeCellRenderer.getTreeCellRendererComponent(
						TreeList.this, value, isSelected, false, true, listRow, cellHasFocus);
			}
		}
		
		
		public void updateUI() {
			list.setCellRenderer(listCellRenderer);
			list.updateUI();
			listCellRenderer = list.getCellRenderer();
			list.setCellRenderer(this);
		}

	}
	
	
	
	

	public interface Selection {
		TreePath getPath();
		boolean isListPath();
		ListModel getModel();
		ListSelectionModel getSelectionModel();
		boolean isSelected();
	}
	
	class PathHandler implements Selection {
		PathHandler(TreePath p) {
			path = p;
		}
		
		final TreePath path;
		
		int offset = -1;
		
		public TreePath getPath() { return path; }
		
		public boolean isListPath() { return false; }
		
		public ListModel getModel() { throw new IllegalStateException(); }
		
		public ListSelectionModel getSelectionModel() { throw new IllegalStateException(); }
		
		public boolean isSelected() {
			return isPathSelected(path);
		}

	}
	
	class ListHandler extends PathHandler implements ListDataListener {
		
		ListHandler(ListModel m, TreePath p) {
			super(p);
			model = m;
			selection = new DefaultListSelectionModel();
			m.addListDataListener(this);
			
		}
		
		final ListModel model;
		
		final ListSelectionModel selection;
		
		boolean selected = false;
		
		Dimension size;
		
		
		public void contentsChanged(ListDataEvent evt) {
			fireTreeNodeChanged();
		}

		public void intervalAdded(ListDataEvent evt) {
			if (!path.equals(getEditingPath()) && !selection.isSelectionEmpty()) {
				// TODO update selection
			
			}
			fireTreeNodeChanged();
		}

		public void intervalRemoved(ListDataEvent evt) {
			if (!path.equals(getEditingPath()) && !selection.isSelectionEmpty()) {
				// TODO update selection
				
			}
			fireTreeNodeChanged();
		}
		
		void fireTreeNodeChanged() {
			validatePath(path);
			Rectangle bounds = getPathBounds(path);
			if (bounds != null)
				repaint(bounds);
		}
		

		public boolean isListPath() { return true; }
		
		public ListModel getModel() { return model; }
		
		public ListSelectionModel getSelectionModel() { return selection; }
		
		public boolean isSelected() {
			return selected;
		}
		
		void selectAll(boolean ascending) {
			int idx = model.getSize()-1;
			if (idx >= 0)
				selection.setSelectionInterval(ascending ? 0 : idx, ascending ? idx : 0);
			selected = true;
		}
		
		void clearSelection() {
			selection.clearSelection();
			selected = false;
		}
		
		void prepareSelectionChange(boolean ascending) {
			int idx = model.getSize()-1;
			if (idx >= 0) {
				idx = ascending ? 0 : idx;
				selection.setSelectionInterval(idx, idx);
			}
		}
		
		void extendSelection(boolean ascending) {
			int anchor = selection.getAnchorSelectionIndex();
			if (anchor >= 0) {
				int idx = model.getSize()-1;
				if (idx >= 0)
					selection.setSelectionInterval(anchor, ascending ? idx : 0);
			}
			selected = true;
		}
		
		void updateSelection() {
			if (selection.isSelectionEmpty() == selected) {
				selected = !selected;
				handler.setProcessSelection(false);
				if (selected) {
					addSelectionPath(path);
				} else {
					removeSelectionPath(path);
				}
				handler.setProcessSelection(true);
			}
		}
	}
	
	void fireSelectionEvent(TreeSelectionEvent e) {
		Object[] listeners = listenerList.getListenerList();
		SelectionEvent evt = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==SelectionListener.class) {
				if (evt == null)
					evt = new SelectionEvent(this, e);
				((SelectionListener)listeners[i+1]).valueChanged(evt);
			}
		}
	}
	
	void fireSelectionEvent(ListSelectionEvent e, TreePath p) {
		Object[] listeners = listenerList.getListenerList();
		SelectionEvent evt = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==SelectionListener.class) {
				if (evt == null)
					evt = new SelectionEvent(this, e, p);
				((SelectionListener)listeners[i+1]).valueChanged(evt);
			}
		}
	}

	public static class SelectionEvent extends EventObject {
		
		public SelectionEvent(TreeList source, TreeSelectionEvent event) {
			super(source);
			treeEvent = event;
		}
		
		public SelectionEvent(TreeList source, ListSelectionEvent event, TreePath path) {
			super(source);
			listEvent = event;
			listPath = path;
		}
		
		TreeSelectionEvent treeEvent;
		
		TreePath[] treePaths;
		
		ListSelectionEvent listEvent;
		
		TreePath listPath;
		
		public TreeList getTreeList() {
			return (TreeList)getSource();
		}
		
		public int getCount() {
			if (treeEvent != null) {
				if (treePaths == null)
					treePaths = treeEvent.getPaths();
				return treePaths.length;
			}
			return 1;
		}
		
		public Selection getSelection(int index) {
			if (treeEvent != null) {
				if (treePaths == null)
					treePaths = treeEvent.getPaths();
				return getTreeList().getPathHandler(treePaths[index]);
			} else if (index != 0) {
				throw new IndexOutOfBoundsException();
			}
			return getTreeList().getPathHandler(listPath);
		}
		
		public boolean isListEvent() {
			return listEvent != null;
		}
		
		public TreeSelectionEvent getTreeSelectionEvent() {
			return treeEvent;
		}
		
		public ListSelectionEvent getListSelectionEvent() {
			return listEvent;
		}
	}
	
	public interface SelectionListener extends EventListener {
		void valueChanged(SelectionEvent e);
	}


	public void addSelectionListener(SelectionListener l) {
		listenerList.add(SelectionListener.class, l);
	}

	public void removeSelectionListener(SelectionListener l) {
		listenerList.remove(SelectionListener.class, l);
	}

	
}
