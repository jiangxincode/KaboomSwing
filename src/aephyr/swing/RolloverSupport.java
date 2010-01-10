/*
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package aephyr.swing;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;


public class RolloverSupport extends MouseAdapter implements ComponentListener {

	private static Component rolloverComponent;
	
	private static RolloverSupport rolloverSupport;
	
	private static int registeredCount;
	
	private static AWTEventListener awtListener;
	
	private static void setRolloverComponent(RolloverSupport s, Container p, Component c, Rectangle b) {
		Container oldParent = null;
		if (rolloverComponent != null) {
			oldParent = rolloverComponent.getParent();
			if (oldParent != null)
				repaint(oldParent, rolloverComponent);
		}
		if (c != rolloverComponent) {
			if (rolloverComponent != null && oldParent != null)
				rolloverSupport.hide(rolloverComponent, oldParent != p);
			rolloverComponent = c;
			if (c != null) {
				if (!s.isRegistered)
					registerAWTEventListener(s);
				p.add(c, 0);
			}
		}
		if (c != null) {
			c.setBounds(b);
			repaint(p, c);
			rolloverSupport = s;
		} else if (rolloverSupport == s) {
			rolloverSupport = null;
		}
		p.validate();
	}
	
	private static void registerAWTEventListener(RolloverSupport s) {
		s.isRegistered = true;
		if (registeredCount++ == 0) {
			awtListener = new AWTEventListener() {
				@Override
				public void eventDispatched(AWTEvent event) {
					if (rolloverComponent != null) {
						switch (event.getID()) {
						case MouseEvent.MOUSE_ENTERED:
							if (event.getSource() == rolloverComponent) {
								rolloverSupport.doHide = false;
							} else if (event.getSource() instanceof Component) {
								Component c = (Component)event.getSource();
								for (Container p=c.getParent(); p!=null; p=p.getParent()) {
									if (p == rolloverComponent) {
										rolloverSupport.doHide = false;
										break;
									}
								}
							}
							break;
						case MouseEvent.MOUSE_EXITED:
							if (event.getSource() == rolloverComponent) {
								rolloverSupport.deferHideRolloverComponent();
							}
							break;
						}
					}
				}
			};
			AccessController.doPrivileged(new PrivilegedAction<Void>() {
				public Void run() {
					Toolkit.getDefaultToolkit().addAWTEventListener(
							awtListener, AWTEvent.MOUSE_EVENT_MASK);
					return null;
				}
			});
		}
	}
	
	private static void unregisterAWTEventListener(RolloverSupport s) {
		s.isRegistered = false;
		if (--registeredCount == 0) {
			AccessController.doPrivileged(new PrivilegedAction<Void>() {
				public Void run() {
					Toolkit.getDefaultToolkit().removeAWTEventListener(awtListener);
					return null;
				}
			});
		}
	}

	
	private boolean isRegistered = false;
	
	private boolean isEnabled = true;
	
	private boolean doHide = false;
	
	private boolean doValidate = true;

	public Component getRolloverComponent() {
		return rolloverSupport == this ? rolloverComponent : null;
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	public void setEnabled(boolean enabled) {
		isEnabled = enabled;
		if (!enabled) {
			hideRolloverComponent();
			if (isRegistered)
				unregisterAWTEventListener(this);
		}
	}
	
	public void dispose() {
		if (isEnabled)
			setEnabled(false);
		if (isRegistered)
			unregisterAWTEventListener(this);
	}
	
	public void setRolloverComponent(Container parent, Component rollover, Rectangle bounds) {
		doHide = false;
		setRolloverComponent(this, parent, rollover, bounds);
	}
	
	/**
	 * Called on a mouse exit event. The hide action may not
	 * happen if another event overrides (e.g. a mouse enter event
	 * on one of rollover component's children).
	 * 
	 * @see #hideRolloverComponent()
	 * @see #hide(Component, boolean)
	 */
	public void deferHideRolloverComponent() {
		if (rolloverComponent != null && !doHide) {
			doHide = true;
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					if (doHide)
						hideRolloverComponent();
				}
			});
		}
	}

	/**
	 * Called when the current rollover component should be
	 * hidden and no other rollover component is becoming visible.
	 * 
	 * @see #deferHideRolloverComponent()
	 * @see #hide(Component, boolean)
	 */
	protected void hideRolloverComponent() {
		if (rolloverSupport == this) {
			hide(rolloverComponent, true);
			rolloverComponent = null;
			rolloverSupport = null;
		}
	}
	
	/**
	 * This is the method to override if the hiding behavior should be
	 * changed.
	 * 
	 * @param rolloverComponent - the component to hide
	 * @param validate - true if the parent component should be validated,
	 * 			false if the call is already going to be made
	 * 
	 * @see #deferHideRolloverComponent()
	 * @see #hideRolloverComponent()
	 */
	protected void hide(Component rolloverComponent, boolean validate) {
		Container c = rolloverComponent.getParent();
		if (c != null) {
			repaint(c, rolloverComponent);
			c.remove(rolloverComponent);
			if (validate)
				c.validate();
		}
	}
	
	@Override
	public void mouseExited(MouseEvent e) {
		deferHideRolloverComponent();
	}
	
	@Override
	public void componentHidden(ComponentEvent e) {
		hideRolloverComponent();
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		deferValidate();
	}

	@Override
	public void componentResized(ComponentEvent e) {
		deferValidate();
	}

	@Override
	public void componentShown(ComponentEvent e) {
		deferValidate();
	}
	
	/**
	 * Must be used instead of validate for instances where the cause
	 * of validation comes from an event that the UI may need to process
	 * first.
	 * 
	 * @see #validate()
	 */
	public void deferValidate() {
		if (doValidate && isMouseOverComponent()) {
			doValidate = false;
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					doValidate = true;
					validate();
				}
			});
		}
	}
	
	protected boolean isMouseOverComponent() { return false; }
	
	/**
	 * Validates the current rollover state/component.
	 * 
	 * @see #deferValidate()
	 */
	public void validate() {}
	

	
	public static abstract class Indexed extends RolloverSupport {
		
		private int rolloverIndex = -1; 
		
		public int getRolloverIndex() {
			return rolloverIndex;
		}
		
		protected void setRolloverIndex(int index) {
			if (index != rolloverIndex) {
				onRollover(index);
				rolloverIndex = index;
			}
		}
		
		protected abstract void onRollover(int index);
		
		@Override
		protected void hideRolloverComponent() {
			super.hideRolloverComponent();
			rolloverIndex = -1;
		}
		
	}
	
	public static class List extends Indexed
			implements ListSelectionListener, ListDataListener, PropertyChangeListener {
		
		public List(JList list, ListCellRenderer rolloverRenderer) {
			if (list == null || rolloverRenderer == null)
				throw new NullPointerException();
			this.list = list;
			this.rolloverRenderer = rolloverRenderer;
			addListeners();
		}
		
		protected final JList list;
		
		protected final ListCellRenderer rolloverRenderer;
		
		@Override
		public void mouseMoved(MouseEvent e) {
			setRolloverIndex(e);
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			setRolloverIndex(e);
		}
		
		private void setRolloverIndex(MouseEvent e) {
			setRolloverIndex(locationToIndex(new Point(e.getX(), e.getY())));
		}
		
		protected int locationToIndex(Point location) {
			int index = list.locationToIndex(location);
			if (index < 0)
				return index;
			Rectangle bounds = list.getCellBounds(index, index);
			return bounds.contains(location.x, location.y) ? index : -1;
		}
		
		@Override
		protected void onRollover(int index) {
			if (index >= 0) {
				Component c = rolloverRenderer.getListCellRendererComponent(
						list,
						list.getModel().getElementAt(index),
						index,
						list.isSelectedIndex(index),
						list.hasFocus() && list.getLeadSelectionIndex()==index);
				Rectangle b = c == null ? null : list.getCellBounds(index, index);
				setRolloverComponent(list, c, b);
			} else {
				deferHideRolloverComponent();
			}
		}
		
		protected void addListeners() {
			list.addMouseListener(this);
			list.addMouseMotionListener(this);
			list.addListSelectionListener(this);
			list.getModel().addListDataListener(this);
			list.addPropertyChangeListener(this);
			list.addComponentListener(this);
		}
		
		protected void removeListeners() {
			list.removeMouseListener(this);
			list.removeMouseMotionListener(this);
			list.removeListSelectionListener(this);
			list.getModel().removeListDataListener(this);
			list.removePropertyChangeListener(this);
			list.removeComponentListener(this);
		}

		@Override
		public void setEnabled(boolean enabled) {
			if (enabled != isEnabled()) {
				super.setEnabled(enabled);
				if (enabled) {
					addListeners();
				} else {
					removeListeners();
				}
			}
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (isInRange(getRolloverIndex(), e))
				onRollover(getRolloverIndex());
		}

		@Override
		public void contentsChanged(ListDataEvent e) {
			int index = getRolloverIndex();
			if (index >= 0 && index >= e.getIndex0() && index <= e.getIndex1())
				onRollover(index);
		}

		@Override
		public void intervalAdded(ListDataEvent e) {
			if (getRolloverIndex() >= 0) {
				onRollover(getRolloverIndex());
			} else {
				deferValidate();
			}
		}

		@Override
		public void intervalRemoved(ListDataEvent e) {
			int rollover = getRolloverIndex();
			if (rollover >= e.getIndex0()) {
				if (rollover < list.getModel().getSize()) {
					onRollover(rollover);
				} else {
					hideRolloverComponent();
				}
			}
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName() == "model") {
				ListModel model = (ListModel)evt.getOldValue();
				model.removeListDataListener(this);
				model = (ListModel)evt.getNewValue();
				model.addListDataListener(this);
				deferValidate();
			} else if (evt.getPropertyName() == "enabled") {
				setEnabled((Boolean)evt.getNewValue());
			}
		}
		
		@Override
		protected boolean isMouseOverComponent() {
			return getRolloverIndex() >= 0 || list.getMousePosition(true) != null;
		}

		@Override
		public void validate() {
			Point pt = list.getMousePosition(true);
			if (pt != null) {
				int index = locationToIndex(pt);
				if (index != getRolloverIndex()) {
					setRolloverIndex(index);
				} else if (index >= 0) {
					onRollover(index);
				}
			} else if (getRolloverIndex() >= 0){
				setRolloverIndex(-1);
			}
		}

	}
	
	public static class Tree extends Indexed
			implements TreeSelectionListener, TreeExpansionListener, TreeModelListener, PropertyChangeListener {
		
		public Tree(JTree tree, TreeCellRenderer rolloverRenderer) {
			if (tree == null || rolloverRenderer == null)
				throw new NullPointerException();
			this.tree = tree;
			this.rolloverRenderer = rolloverRenderer;
			addListeners();
		}
		
		protected final JTree tree;
		
		protected final TreeCellRenderer rolloverRenderer;
		
		@Override
		public void mouseMoved(MouseEvent e) {
			setRolloverIndex(tree.getRowForLocation(e.getX(), e.getY()));
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			setRolloverIndex(tree.getRowForLocation(e.getX(), e.getY()));
		}
		
		@Override
		protected void onRollover(int index) {
			if (index >= 0) {
				onRollover(index, tree.getPathForRow(index));
			} else {
				deferHideRolloverComponent();
			}
		}
		
		private void onRollover(int index, TreePath path) {
			Component c = rolloverRenderer.getTreeCellRendererComponent(
					tree,
					path.getLastPathComponent(),
					tree.isRowSelected(index),
					tree.isExpanded(index),
					tree.getModel().isLeaf(path.getLastPathComponent()),
					index,
					tree.hasFocus() && index==tree.getSelectionModel().getLeadSelectionRow());
			Rectangle b = c == null ? null : tree.getRowBounds(index);
			setRolloverComponent(tree, c, b);
		}
		
		protected void addListeners() {
			tree.addMouseListener(this);
			tree.addMouseMotionListener(this);
			tree.addTreeSelectionListener(this);
			tree.addTreeExpansionListener(this);
			tree.getModel().addTreeModelListener(this);
			tree.addPropertyChangeListener(this);
			tree.addComponentListener(this);
		}
		
		protected void removeListeners() {
			tree.removeMouseListener(this);
			tree.removeMouseMotionListener(this);
			tree.removeTreeSelectionListener(this);
			tree.removeTreeExpansionListener(this);
			tree.getModel().removeTreeModelListener(this);
			tree.removePropertyChangeListener(this);
			tree.removeComponentListener(this);
		}

		@Override
		public void setEnabled(boolean enabled) {
			if (enabled != isEnabled()) {
				super.setEnabled(enabled);
				if (enabled) {
					addListeners();
				} else {
					removeListeners();
				}
			}
		}

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			int index = getRolloverIndex();
			if (index >= 0) {
				for (TreePath path : e.getPaths()) {
					if (tree.getRowForPath(path) == index) {
						onRollover(index, path);
						break;
					}
				}
			}
		}

		@Override
		public void treeCollapsed(TreeExpansionEvent e) {
			treeExpansionEvent(e);
		}

		@Override
		public void treeExpanded(TreeExpansionEvent e) {
			treeExpansionEvent(e);
		}
		
		private void treeExpansionEvent(TreeExpansionEvent e) {
			int rollover = getRolloverIndex();
			if (rollover >= 0) {
				int row = tree.getRowForPath(e.getPath());
				if (rollover >= row)
					deferValidate();
			}
		}

		@Override
		public void treeNodesChanged(TreeModelEvent e) {
			int rollover = getRolloverIndex();
			if (rollover >= 0) {
				TreePath parent = e.getTreePath();
				int row = tree.getRowForPath(parent);
				if (rollover >= row) {
					Object[] children = e.getChildren();
					if (children == null) {
						deferValidate();
					} else {
						for (Object child : children) {
							TreePath path = parent.pathByAddingChild(child);
							row = tree.getRowForPath(path);
							if (row == rollover) {
								// must use validation instead of onRollover
								// because the bounds may be stale if this
								// listener is notified before the ui listener
								deferValidate();
								break;
							}
						}
					}
				}
			}
		}

		@Override
		public void treeNodesInserted(TreeModelEvent e) {
			deferValidate();
		}

		@Override
		public void treeNodesRemoved(TreeModelEvent e) {
			deferValidate();
		}

		@Override
		public void treeStructureChanged(TreeModelEvent e) {
			deferValidate();
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName() == "model") {
				TreeModel model = (TreeModel)evt.getOldValue();
				model.removeTreeModelListener(this);
				model = (TreeModel)evt.getNewValue();
				model.addTreeModelListener(this);
				deferValidate();
			} else if (evt.getPropertyName() == "enabled") {
				setEnabled((Boolean)evt.getNewValue());
			}
		}
		
		@Override
		protected boolean isMouseOverComponent() {
			return getRolloverIndex() >= 0 || tree.getMousePosition(true) != null;
		}

		@Override
		public void validate() {
			Point pt = tree.getMousePosition(true);
			if (pt != null) {
				int row = tree.getRowForLocation(pt.x, pt.y);
				if (row != getRolloverIndex()) {
					setRolloverIndex(row);
				} else if (row >= 0) {
					onRollover(row);
				}
			} else if (getRolloverIndex() >= 0) {
				setRolloverIndex(-1);
			}
		}
		
	}
	
	
	public static class Table extends RolloverSupport
			implements ListSelectionListener, TableColumnModelListener,
			PropertyChangeListener, TableModelListener, RowSorterListener {
		
		public Table(JTable table, TableCellRenderer rolloverRenderer) {
			if (table == null || rolloverRenderer == null)
				throw new NullPointerException();
			this.table = table;
			this.rolloverRenderer = rolloverRenderer;
			addListeners();
		}
		
		protected final JTable table;
		
		protected final TableCellRenderer rolloverRenderer;
		
		private int rolloverRow = -1;
		
		private int rolloverColumn = -1;
		
		public int getRolloverRow() {
			return rolloverRow;
		}
		
		public int getRolloverColumn() {
			return rolloverColumn;
		}
		
		protected void setRolloverCoordinates(int row, int column) {
			if (row != rolloverRow || column != rolloverColumn) {
				onRollover(row, column);
				rolloverRow = row;
				rolloverColumn = column;
			}
		}
		
		protected void hideRolloverComponent() {
			super.hideRolloverComponent();
			rolloverRow = rolloverColumn = -1;
		}
		
		protected void addListeners() {
			table.addMouseListener(this);
			table.addMouseMotionListener(this);
			table.addPropertyChangeListener(this);
			table.getModel().addTableModelListener(this);
			table.getSelectionModel().addListSelectionListener(this);
			table.getColumnModel().addColumnModelListener(this);
			table.addComponentListener(this);
			RowSorter<?> sorter = table.getRowSorter();
			if (sorter != null)
				sorter.addRowSorterListener(this);
		}
		
		protected void removeListeners() {
			table.removeMouseListener(this);
			table.removeMouseMotionListener(this);
			table.removePropertyChangeListener(this);
			table.getModel().removeTableModelListener(this);
			table.getSelectionModel().removeListSelectionListener(this);
			table.getColumnModel().removeColumnModelListener(this);
			table.removeComponentListener(this);
			RowSorter<?> sorter = table.getRowSorter();
			if (sorter != null)
				sorter.removeRowSorterListener(this);
		}
		
		@Override
		public void setEnabled(boolean enabled) {
			if (enabled != isEnabled()) {
				super.setEnabled(enabled);
				if (enabled) {
					addListeners();
				} else {
					removeListeners();
				}
			}
		}
		
		@Override
		public void mouseMoved(MouseEvent e) {
			Point pt = new Point(e.getX(), e.getY());
			int row = table.rowAtPoint(pt);
			if (row >= 0) {
				int col = table.columnAtPoint(pt);
				if (col >= 0) {
					setRolloverCoordinates(row, col);
				}
			}
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			Point pt = new Point(e.getX(), e.getY());
			int row = table.rowAtPoint(pt);
			if (row >= 0) {
				int col = table.columnAtPoint(pt);
				if (col >= 0) {
					setRolloverCoordinates(row, col);
				}
			}
		}
		
		protected void onRollover(int row, int column) {
			if (row >= 0 && column >= 0) {
				Component c = rolloverRenderer.getTableCellRendererComponent(
						table,
						table.getValueAt(row, column),
						table.isCellSelected(row, column),
						table.hasFocus()
							&& table.getSelectionModel().getLeadSelectionIndex() == row
							&& table.getColumnModel().getSelectionModel().getLeadSelectionIndex() == column,
						row,
						column);
				Rectangle b = c == null ? null : table.getCellRect(row, column, false);
				setRolloverComponent(table, c, b);
			} else {
				deferHideRolloverComponent();
			}
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (isInRange(getRolloverRow(), e))
				onRollover(getRolloverRow(), getRolloverColumn());
		}
		

		@Override
		public void columnAdded(TableColumnModelEvent e) {
			deferValidate();
		}

		@Override
		public void columnMarginChanged(ChangeEvent e) {
			deferValidate();
		}

		@Override
		public void columnMoved(TableColumnModelEvent e) {
			deferValidate();
		}

		@Override
		public void columnRemoved(TableColumnModelEvent e) {
			deferValidate();
		}

		@Override
		public void columnSelectionChanged(ListSelectionEvent e) {
			if (isInRange(getRolloverColumn(), e))
				onRollover(getRolloverRow(), getRolloverColumn());
		}
		

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			String property = evt.getPropertyName();
			if (property == "model") {
				TableModel oldModel = (TableModel)evt.getOldValue();
				oldModel.removeTableModelListener(this);
				TableModel newModel = (TableModel)evt.getNewValue();
				newModel.addTableModelListener(this);
				deferValidate();
			} else if (property == "columnModel") {
				TableColumnModel oldModel = (TableColumnModel)evt.getOldValue();
				oldModel.removeColumnModelListener(this);
				TableColumnModel newModel = (TableColumnModel)evt.getNewValue();
				newModel.addColumnModelListener(this);
				deferValidate();
			} else if (property == "selectionModel") {
				ListSelectionModel oldModel = (ListSelectionModel)evt.getOldValue();
				oldModel.removeListSelectionListener(this);
				ListSelectionModel newModel = (ListSelectionModel)evt.getNewValue();
				newModel.addListSelectionListener(this);
				deferValidate();
			} else if (property == "sorter") {
				RowSorter<?> sorter = (RowSorter<?>)evt.getOldValue();
				if (sorter != null)
					sorter.removeRowSorterListener(this);
				sorter = (RowSorter<?>)evt.getOldValue();
				if (sorter != null)
					sorter.addRowSorterListener(this);
			} else if (property == "enabled") {
				setEnabled((Boolean)evt.getNewValue());
			}
		}

		@Override
		public void tableChanged(TableModelEvent e) {
			if (e.getType() == TableModelEvent.UPDATE) {
				if (rolloverRow >= 0) {
					int modelRow = table.convertRowIndexToModel(rolloverRow);
					if (modelRow >= e.getFirstRow() && modelRow <= e.getLastRow()
							&& (e.getColumn() == TableModelEvent.ALL_COLUMNS ||
									e.getColumn() == table.convertColumnIndexToModel(rolloverColumn)))
						onRollover(rolloverRow, rolloverColumn);
				}
			} else {
				deferValidate();
			}
		}
		
		@Override
		public void sorterChanged(RowSorterEvent e) {
			deferValidate();
		}
		
		@Override
		protected boolean isMouseOverComponent() {
			return rolloverRow >= 0 || table.getMousePosition(true) != null;
		}

		@Override
		public void validate() {
			Point pt = table.getMousePosition(true);
			if (pt != null) {
				int row = table.rowAtPoint(pt);
				int col = table.columnAtPoint(pt);
				if (row != rolloverRow || col != rolloverColumn) {
					setRolloverCoordinates(row, col);
				} else if (row >= 0) {
					onRollover(row, col);
				}
			} else if (rolloverRow >= 0) {
				setRolloverCoordinates(-1, -1);
			}
		}
	}
	
	private static boolean isInRange(int index, ListSelectionEvent e) {
		return index >= 0 && index >= e.getFirstIndex() && index <= e.getLastIndex();
	}
	
	private static void repaint(Container parent, Component c) {
		parent.repaint(c.getX(), c.getY(), c.getWidth(), c.getHeight());
	}

}
