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
package aephyr.swing.ui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.TransferHandler.TransferSupport;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import java.io.IOException;
import java.util.*;

import aephyr.swing.*;
import aephyr.swing.TabFolder.TabPlacement;
import aephyr.swing.tabfolder.AbstractTabRenderer;
import aephyr.swing.tabfolder.MutableTabModel;
import aephyr.swing.tabfolder.Tab;
import aephyr.swing.tabfolder.TabModel;
import aephyr.swing.tabfolder.TabRenderer;
import aephyr.swing.tabfolder.TabTransferable;


public class BasicTabFolderUI extends TabFolderUI {


	protected TabFolder tabFolder;

	protected TabRenderer tabRenderer;

	protected TabLayoutState tabLayoutState;

	protected TabModel tabModel;

	protected Handler handler;

	protected int selectedIndex;

	protected Component visibleComponent;

	protected AbstractPainter painter;

	protected CellRendererPane rendererPane;

	protected final Rectangle componentBounds = new Rectangle();

	protected boolean validSizeCache = false;

	protected boolean validLayoutPolicy = false;

	// [UN]installation methods

	@Override
	public void installUI(JComponent c) {
		tabFolder = (TabFolder)c;
		tabModel = tabFolder.getModel();
		rendererPane = new CellRendererPane();
		handler = createHandler();
		painter = createPainter();
		tabRenderer = tabFolder.getTabRenderer();
		if (tabRenderer == null) {
			tabRenderer = new Renderer(new Dimension(100, 20), new Dimension(0, 0));
			tabFolder.setTabRenderer(tabRenderer);
		}
		tabFolder.setLayout(new Layout());
		installDefaults();
		installListeners();
		selectedIndex = tabModel.getSelectionIndex();
		if (selectedIndex >= 0) {
			visibleComponent = tabModel.getTabAt(selectedIndex).getComponent();
			if (visibleComponent != null) {
				visibleComponent.setVisible(true);
				if (visibleComponent.getParent() != tabFolder)
					tabFolder.add(visibleComponent);
			}
		}
	}

	@Override
	public void uninstallUI(JComponent c) {
		uninstallListeners();
		uninstallDefaults();
		tabFolder.setLayout(null);
		if (tabFolder.getTabRenderer() instanceof UIResource)
			tabFolder.setTabRenderer(null);
		if (tabLayoutState != null)
			tabLayoutState.uninstall();
	}

	protected void installDefaults() {
		LookAndFeel.installColorsAndFont(tabFolder, "TabbedPane.background",
				"TabbedPane.foreground", "TabbedPane.font");

		Object opaque = UIManager.get("TabbedPane.opaque");
		if (opaque == null)
			opaque = Boolean.FALSE;
		LookAndFeel.installProperty(tabFolder, "opaque", opaque);

		if (tabFolder.getTransferHandler() == null) {
			tabFolder.setTransferHandler(getTabTransferHandler());
			try {
				tabFolder.getDropTarget().addDropTargetListener(getTabTransferHandler());
			} catch (TooManyListenersException e) {
				e.printStackTrace();
			}
		}
	}

	protected void uninstallDefaults() {

		if (tabFolder.getTransferHandler() == getTabTransferHandler()) {
			tabFolder.getDropTarget().removeDropTargetListener(getTabTransferHandler());
			tabFolder.setTransferHandler(null);
		}
	}

	protected void installListeners() {
		tabFolder.addPropertyChangeListener(handler);
		installModelListeners();
	}

	protected void uninstallListeners() {
		uninstallModelListeners();
		tabFolder.removePropertyChangeListener(handler);
	}

	protected void installModelListeners() {
		tabModel.addListDataListener(handler);
		tabModel.addPropertyChangeListener(handler);
	}

	protected void uninstallModelListeners() {
		tabModel.removeListDataListener(handler);
		tabModel.removePropertyChangeListener(handler);
	}

	protected Handler createHandler() {
		return new Handler();
	}

	// TabFolderUI interface

	@Override
	public Rectangle getTabBounds(TabFolder tabFolder, int index) {
		return tabLayoutState.getTabBounds(index);
	}

	@Override
	public int getTabForLocation(TabFolder tabFolder, int x, int y) {
		return tabLayoutState.getTabForLocation(x, y);
	}

	public void paint(Graphics g, JComponent c) {
		painter.paint(g, tabFolder);
	}

	public int getPressedIndex(TabFolder tabFolder) {
		return tabLayoutState.getPressedIndex();
	}

	public int getRolloverIndex(TabFolder tabFolder) {
		return tabLayoutState.getRolloverIndex();
	}




	protected Component trailingCornerComponent;
	protected Component leadingCornerComponent;


	protected void setLeadingCornerComponent(Component c) {
		if (leadingCornerComponent != null)
			tabFolder.remove(leadingCornerComponent);
		leadingCornerComponent = c;
		if (c != null)
			tabFolder.add(c);
	}

	protected void setTrailingCornerComponent(Component c) {
		if (trailingCornerComponent != null)
			tabFolder.remove(trailingCornerComponent);
		trailingCornerComponent = c;
		if (c != null)
			tabFolder.add(c);
	}

	protected void setVisibleComponent(Component c) {
		visibleComponent = c;
		if (c != null) {
			c.setVisible(true);
			if (c.getParent() != tabFolder)
				tabFolder.add(c);
			c.setBounds(componentBounds);
		}
	}

	private void resetComponent(TabState ts, int index) {
		if (index == selectedIndex) {
			if (ts.component != null)
				tabFolder.remove(ts.component);
			setVisibleComponent(ts.tab.getComponent());
			ts.component = visibleComponent;
			tabFolder.validate();
		} else if (ts.component != null && ts.component != visibleComponent) {
			tabFolder.remove(ts.component);
			tabFolder.validate();
		}
	}

	protected class Handler extends ComponentAdapter implements ActionListener, ListDataListener, PropertyChangeListener {

		private JPopupMenu dropDownPopup;

		private boolean validDropDown;

		@Override
		public void actionPerformed(ActionEvent e) {

			if (tabLayoutState.isDropDownAction(e)) {
				if (dropDownPopup == null) {
					dropDownPopup = new JPopupMenu();
				}
				if (!validDropDown) {
					validDropDown = true;
					dropDownPopup.removeAll();
					TabState[] s = tabLayoutState.states;
					for (int i=0; i<s.length; i++) {
						JMenuItem item = new JMenuItem(s[i].tab.getTitle(), s[i].tab.getIcon());
						item.addActionListener(this);
						dropDownPopup.add(item);
					}
				}
				Font font = dropDownPopup.getFont();
				Font highlight;
				if (font.isBold()) {
					highlight = font.isItalic() ? 
							font.deriveFont(Font.PLAIN) : font.deriveFont(Font.ITALIC);
				} else {
					highlight = font.deriveFont(Font.BOLD);
				}
				TabState[] states = tabLayoutState.states;
				int lastVisible = tabLayoutState.lastVisible;
				for (int i=states.length; --i>lastVisible;)
					dropDownPopup.getComponent(i).setFont(highlight);
				boolean checkVisible = tabLayoutState.checkVisible();
				int firstVisible = tabLayoutState.firstVisible;
				for (int i=lastVisible; --i>=firstVisible;)
					dropDownPopup.getComponent(i).setFont(!checkVisible || states[i].visible ? font : highlight);
				for (int i=firstVisible; --i>=0;)
					dropDownPopup.getComponent(i).setFont(highlight);

				Component c = (Component)e.getSource();
				Point pt = c.getLocationOnScreen();
				Dimension sz = dropDownPopup.getPreferredSize();
				GraphicsConfiguration gc = c.getGraphicsConfiguration();
				Insets screenInsets = gc == null ? null : Toolkit.getDefaultToolkit().getScreenInsets(gc);
				Rectangle screenBounds = gc == null ? null : gc.getBounds();
				switch (tabFolder.getTabPlacement()) {
				case TOP:
				case BOTTOM:
					pt.y += c.getHeight();
					pt.x -= sz.getWidth() - c.getWidth();
					if (gc != null) {
						if (pt.y+sz.height > screenBounds.height-screenInsets.bottom) {
							pt.y -= c.getHeight();
							if (pt.y-sz.height < screenBounds.y+screenInsets.top) {
								pt.y = screenBounds.y+screenInsets.top;
							} else {
								pt.y -= sz.height;
							}
						}
						if (pt.x < screenBounds.x+screenInsets.left) {
							pt.x = screenBounds.x+screenInsets.left;
						}
					}
					break;
				case LEFT:
					// TODO
					break;
				case RIGHT:
					// TODO
					break;
				}
				dropDownPopup.setInvoker(tabFolder);
				dropDownPopup.setLocation(pt.x, pt.y);
				dropDownPopup.setVisible(true);

			} else if (e.getSource() instanceof JMenuItem) {
				JMenuItem item = (JMenuItem)e.getSource();
				int idx = dropDownPopup.getComponentIndex(item);
				if (idx != selectedIndex && idx >= 0) {
					tabModel.setSelectedIndex(idx);
				}
			} else if (e.getSource() instanceof JButton) {
				JButton close = (JButton)e.getSource();
				if (tabModel instanceof MutableTabModel) {
					MutableTabModel tm = (MutableTabModel)tabModel;
					TabComponent c = (TabComponent)close.getParent();
					tm.removeTab(c.index);
				}
			}
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			String property = evt.getPropertyName();
			if (evt.getSource() == tabModel) {
				if (property == "selectedIndex") {
					int newIndex = (Integer)evt.getNewValue();
					int oldIndex = selectedIndex;
					tabLayoutState.selectionChanged(newIndex);
					if (visibleComponent != null)
						visibleComponent.setVisible(false);
					selectedIndex = newIndex;
					if (newIndex >= 0) {
						setVisibleComponent(tabModel.getTabAt(newIndex).getComponent());
						tabFolder.validate();
					}
					if (oldIndex < tabLayoutState.states.length)
						tabLayoutState.repaint(oldIndex);
					tabLayoutState.repaint(newIndex);
				} else if (property == "component") {
					IndexedPropertyChangeEvent e = (IndexedPropertyChangeEvent)evt;
					int index = e.getIndex();
					resetComponent(tabLayoutState.states[index], index);
				}
			} else if (evt.getSource() == tabFolder) {
				if (property == "trailingCornerComponent") {
					setTrailingCornerComponent((Component)evt.getNewValue());
					tabFolder.revalidate();
					tabFolder.repaint();
				} else if (property == "leadingCornerComponent") {
					setLeadingCornerComponent((Component)evt.getNewValue());
					tabFolder.revalidate();
					tabFolder.repaint();
				} else if (property == "tabLayoutPolicy") {
					invalidateLayoutPolicy();
				} else if (property == "tabPlacement") {
					if (!validLayoutPolicy)
						return;
					switch ((TabPlacement)evt.getNewValue()) {
					case TOP:
						if (evt.getOldValue() != TabPlacement.BOTTOM)
							invalidateLayoutPolicy();
						break;
					case BOTTOM:
						if (evt.getOldValue() != TabPlacement.TOP)
							invalidateLayoutPolicy();
						break;
					case LEFT:
						if (evt.getOldValue() != TabPlacement.RIGHT)
							invalidateLayoutPolicy();
						break;
					case RIGHT:
						if (evt.getOldValue() != TabPlacement.LEFT)
							invalidateLayoutPolicy();
						break;
					}
				} else if (property == "tabRenderer") {
					tabRenderer = (TabRenderer)evt.getNewValue();
					invalidateLayoutPolicy();
				} else if (property == "model") {
					uninstallModelListeners();
					tabModel = (TabModel)evt.getNewValue();
					installModelListeners();
				} else if (tabLayoutState != null) {
					tabLayoutState.propertyChange(evt);
				}
			}
		}

		@Override
		public void contentsChanged(ListDataEvent e) {
			validDropDown = false;
			tabLayoutState.tabsChanged(e.getIndex0(), e.getIndex1());
		}

		@Override
		public void intervalAdded(ListDataEvent e) {
			validDropDown = false;
			tabLayoutState.tabsAdded(e.getIndex0(), e.getIndex1());
		}

		@Override
		public void intervalRemoved(ListDataEvent e) {
			validDropDown = false;
			tabLayoutState.tabsRemoved(e.getIndex0(), e.getIndex1());
		}

	}

	protected void resetTabLayoutPolicy() {
		if (tabLayoutState != null)
			tabLayoutState.uninstall();
		int layout;
		switch (tabFolder.getTabPlacement()) {
		case NONE: default:
			tabLayoutState = new TabLayoutState(tabLayoutState);
			tabLayoutState.install();
			tabFolder.validate();
			return;
		case TOP: case BOTTOM: layout = SwingConstants.HORIZONTAL; break;
		case LEFT: case RIGHT: layout = SwingConstants.VERTICAL; break;
		}
		switch (tabFolder.getTabLayoutPolicy()) {
		case HORIZONTAL_SCROLL:
			tabLayoutState = layout == SwingConstants.HORIZONTAL ?
					new USTabLayoutState(tabLayoutState, layout) :
						new BSTabLayoutState(tabLayoutState, layout);
					break;
		case VERTICAL_SCROLL:
			tabLayoutState = layout == SwingConstants.HORIZONTAL ?
					new BSTabLayoutState(tabLayoutState, layout) :
						new USTabLayoutState(tabLayoutState, layout);
					break;
		case MRU:
			tabLayoutState = new MRUTabLayoutState(tabLayoutState, layout);
			break;
		}
		tabLayoutState.install();
		tabFolder.validate();

	}

	private void invalidateLayoutPolicy() {
		validSizeCache = false;
		validLayoutPolicy = false;
	}


	private void calculateTabRegionSize(Dimension s, boolean hor, Dimension size) {
		if (hor) {
			size.width += s.width;
			if (s.height > size.height)
				size.height = s.height;
		} else {
			size.height += s.height;
			if (s.width > size.width)
				size.width = s.width;
		}
	}

	private void calculateTabRegionSize(Component c, boolean hor) {
		calculateTabRegionSize(c.getMinimumSize(), hor, tabRegionMinSize);
		calculateTabRegionSize(c.getPreferredSize(), hor, tabRegionPrefSize);
	}

	private void calculateTabRegionSize(boolean hor) {
		if (leadingCornerComponent != null)
			calculateTabRegionSize(leadingCornerComponent, hor);
		if (trailingCornerComponent != null)
			calculateTabRegionSize(trailingCornerComponent, hor);
		int tabCount = tabModel.getTabCount();
		switch (tabCount) {
		case 0: // nothing more to do
			return;
		case 1:
			calculateTabRegionSize(tabRenderer.getTabMinimumSize(), hor, tabRegionMinSize);
			calculateTabRegionSize(tabRenderer.getTabPreferredSize(), hor, tabRegionPrefSize);
			break;
		default:
			Dimension tabPref = tabRenderer.getTabPreferredSize();
			Dimension tabMin = tabRenderer.getTabMinimumSize();
			if (tabMin == null)
				tabMin = new Dimension(tabPref);
			Insets insets = getTabAreaInsets();
			if (hor) {
				if (tabCount <= 4) {
					tabPref.width *= tabCount;
				} else {
					tabPref.width *= 4;
				}
				tabMin.width += insets.left + insets.right;
				tabMin.height += insets.top + insets.bottom;
				tabPref.width += insets.left + insets.right;
				tabPref.height += insets.top + insets.bottom;
			} else {
				if (tabCount <= 4) {
					tabPref.height *= tabCount;
				} else {
					tabPref.height *= 4;
				}
				tabMin.height += insets.left + insets.right;
				tabMin.width += insets.top + insets.bottom;
				tabPref.height += insets.left + insets.right;
				tabPref.width += insets.top + insets.bottom;
			}
			calculateTabRegionSize(tabMin, hor, tabRegionMinSize);
			calculateTabRegionSize(tabPref, hor, tabRegionPrefSize);
			tabLayoutState.calculateScrollButtonSize(hor);
			break;
		}
	}

	protected void maybeUpdateSizeCache() {
		if (!validSizeCache) {
			validSizeCache = true;
			if (!validLayoutPolicy) {
				validLayoutPolicy = true;
				resetTabLayoutPolicy();
			}
			tabRegionPrefSize.width = tabRegionPrefSize.height = tabRegionMinSize.width = tabRegionMinSize.height = 0;
			int tabCount = tabModel.getTabCount();
			if (tabCount == 0) {
				prefSize.width = prefSize.height = minSize.width = minSize.height = 0;
			} else {
				int minWidth = 0, minHeight = 0;
				int prefWidth = 0, prefHeight = 0;
				for (int i=tabCount; --i>=0;) {
					Tab tab = tabModel.getTabAt(i);
					Dimension size = tab.getMinimumComponentSize();
					minWidth = Math.max(size.width, minWidth);
					minHeight = Math.max(size.height, minHeight);
					size = tab.getPreferredComponentSize();
					prefWidth = Math.max(size.width, prefWidth);
					prefHeight = Math.max(size.height, prefHeight);
				}
				switch (tabFolder.getTabPlacement()) {
				case TOP: case BOTTOM:
					calculateTabRegionSize(true);
					minSize.width = Math.max(minWidth, tabRegionMinSize.width);
					minSize.height = minHeight + tabRegionMinSize.height;
					prefSize.width = Math.max(prefWidth, tabRegionPrefSize.width);
					prefSize.height = prefHeight + tabRegionPrefSize.height;
					break;
				case LEFT: case RIGHT:
					calculateTabRegionSize(false);
					minSize.height = Math.max(minHeight, tabRegionMinSize.height);
					minSize.width = minHeight + tabRegionMinSize.width;
					prefSize.height = Math.max(prefHeight, tabRegionPrefSize.height);
					prefSize.width = prefWidth + tabRegionPrefSize.width;
					break;
				}
			}
		}
	}

	private final Dimension tabRegionMinSize = new Dimension();
	private final Dimension tabRegionPrefSize = new Dimension();
	private final Rectangle tabRegionBounds = new Rectangle();
	private int tabApex;

	private final Dimension minSize = new Dimension();
	private final Dimension prefSize = new Dimension();

	protected static class TabState extends Rectangle {

		public TabState(Tab tab) {
			this.tab = tab;
		}

		public Tab tab;

		public TabState next;
		public TabState previous;

		public boolean visible = false;

		public boolean validSize = false;

		public Component component;

	}


	protected class TabLayoutState {

		public TabLayoutState(TabLayoutState previous) {
			if (previous != null) {
				firstVisible = previous.firstVisible;
				states = previous.states;
				head = previous.head;
				tail = previous.tail;
			}
		}

		protected TabState[] states;

		protected TabState head;
		protected TabState tail;

		protected boolean validTabLayout;

		// tab indices marking the range of visible tabs
		protected int firstVisible;
		protected int lastVisible;

		public boolean checkVisible() { return false; }

		protected ScrollPane createScrollPane(int layout, int scroll, boolean dropDown) {
			return new ScrollPane(layout, scroll, dropDown);
		}

		protected TabPane createTabPane() {
			return new TabPane();
		}

		public Transferable createTransferable(TabPane tabPane) {
			int idx = tabPane.getDraggedIndex();
			if (idx < 0)
				idx = selectedIndex;
			return idx < 0 ? null : new TabTransferable(tabModel.getTabAt(idx), idx);
		}

		public boolean isPaintingPopup() {
			return false;
		}

		public void selectTab(TabPane tp, int index) {
			if (index >= 0 && index != selectedIndex) {
				tabModel.setSelectedIndex(index);
			}
		}

		public Rectangle getTabBounds(TabPane tp, int index) {
			return states[index].getBounds();
		}

		public Rectangle getTabBounds(int index) {
			return isVisible(index) ? getTabBounds(states[index].getBounds()) : null;
		}

		protected Rectangle getTabBounds(Rectangle r) {
			return r;
		}

		protected Rectangle getTabBounds(ScrollPane s, Rectangle r) {
			Point pt = s.viewport.getViewPosition();
			r.x += s.getX() + s.viewport.getX() - pt.x;
			r.y += s.getY() + s.viewport.getY() - pt.y;
			return r;
		}

		public int getTabForLocation(int x, int y) {
			return -1;
		}

		protected int getTabForLocation(TabPane tp, int x, int y) {
			return getTabForLocation(states, firstVisible, lastVisible, x, y);
		}

		protected int getTabForLocation(ScrollPane sp, int x, int y) {
			Point pt = sp.viewport.getViewPosition();
			x += pt.x - sp.getX();
			y += pt.y - sp.getY();
			return getTabForLocation(states, firstVisible, lastVisible, x, y);
		}

		protected int getTabForLocation(TabState[] s, int i, int j, int x, int y) {
			for (; i<=j; i++)
				if (s[i].contains(x, y))
					return i;
			return Integer.MIN_VALUE;
		}

		public TabPane getTabPane() { return null; }

		public boolean isVisible(int index) {
			return index >= firstVisible && index <= lastVisible;
		}

		public void selectionChanged(int newIndex) {
			if (newIndex < 0)
				return;
			TabState newState = states[newIndex];
			if (newState == head)
				return;
			if (newState == tail)
				tail = newState.previous;
			if (newState.component == null)
				newState.component = newState.tab.getComponent();
			if (newState.previous != null)
				newState.previous.next = newState.next;
			if (newState.next != null)
				newState.next.previous = newState.previous;
			newState.next = head;
			newState.previous = null;
			if (head != null)
				head.previous = newState;
			head = newState;

			TabPane tp = getTabPane();
			if (tp != null) {
				int rollover = tp.getRolloverIndex();
				if (rollover >= 0) {
					if (rollover == selectedIndex || rollover == newIndex) {
						tp.mouseSupport.onRollover(rollover, Integer.MIN_VALUE);
					}
				}
			}
		}

		public void tabsAdded(int firstIndex, int lastIndex) {
			validTabLayout = false;
			int interval = lastIndex - firstIndex + 1;
			TabState[] s = new TabState[states.length + interval];
			System.arraycopy(states, 0, s, 0, firstIndex);
			System.arraycopy(states, firstIndex, s, firstIndex+interval, states.length-firstIndex);
			for (int i=firstIndex; i<=lastIndex; i++) {
				Tab tab = tabModel.getTabAt(i);
				s[i] = new TabState(tab);
				addToLinkList(s[i]);
			}
			states = s;
			update(firstIndex);
		}

		private void update(int firstIndex) {
			TabPane tp = getTabPane();
			if (tp != null)
				update(tp, firstIndex);
		}

		protected void update(TabPane tp, int firstIndex) {
			int idx = tp.getRolloverIndex();
			if (idx < 0 || idx >= firstIndex)
				tp.updateRollover();
			tp.repaint();
		}

		protected void addToLinkList(TabState ts) {
			if (tail == null) {
				head = tail = ts;
			} else {
				tail.next = ts;
				ts.previous = tail;
				tail = ts;
			}
		}

		protected void removeFromLinkList(TabState ts) {
			if (ts == tail)
				tail = ts.previous;
			if (ts.previous != null)
				ts.previous.next = ts.next;
			if (ts.next != null)
				ts.next.previous = ts.previous;
		}

		public void tabsRemoved(int firstIndex, int lastIndex) {
			validTabLayout = false;
			TabPane tp = getTabPane();
			if (tp != null)
				tp.revalidate();
			int interval = lastIndex - firstIndex + 1;
			TabState[] s = new TabState[states.length - interval];
			for (int i=firstIndex; i<=lastIndex; i++) {
				removeFromLinkList(states[i]);
			}
			boolean changeVisibleComponent = selectedIndex >= firstIndex && selectedIndex <= lastIndex;
			Component vc = visibleComponent;
			if (changeVisibleComponent)
				vc = head != null ? head.component : null;
			for (int i=firstIndex; i<=lastIndex; i++) {
				TabState ts = states[i];
				if (ts.component != null && ts.component != vc) {
					tabFolder.remove(ts.component);
				}
			}
			System.arraycopy(states, 0, s, 0, firstIndex);
			System.arraycopy(states, lastIndex+1, s, firstIndex, states.length-lastIndex-1);
			states = s;
			if (changeVisibleComponent) {
				TabState ts = head.next;
				head = ts;
				int index;
				if (ts != null) {
					index = s.length;
					ts.previous = null;
					while (--index >= 0) {
						if (s[index] == ts)
							break;
					}
				} else {
					index = -1;
				}
				tabModel.setSelectedIndex(index);
			}
			if (lastVisible >= s.length)
				lastVisible = s.length-1;
			update(firstIndex);
		}

		public void tabsChanged(int firstIndex, int lastIndex) {
			validTabLayout = false;
			for (int i=firstIndex; i<=lastIndex; i++) {
				TabState ts = states[i];
				Tab tab = tabModel.getTabAt(i);
				if (ts.tab != tab) {
					ts.tab = tab;
					resetComponent(ts, i);
				}
				ts.validSize = false;
			}
			update(firstIndex);
		}

		protected int getPressedIndex(TabPane tp) {
			int idx = tp.getPressedIndex();
			if (idx < 0)
				idx = tp.getDraggedIndex();
			return idx;
		}

		public int getPressedIndex() { return -1; }

		public int getRolloverIndex() { return -1; }

		public boolean isDropDownAction(ActionEvent e) { return false; }

		public void layoutTabs(int startPos, int endPos) {}

		public void onExit(TabPane tabPane) {}

		public void onPress(TabPane tabPane, int index) {
			if (tabPane.getPressedIndex() >= 0)
				tabPane.repaint(getTabBounds(tabPane, tabPane.getPressedIndex()));
			if (index >= 0)
				tabPane.repaint(getTabBounds(tabPane, index));
		}

		public int onRollover(TabPane tabPane, int index, int oldIndex) {
			if (oldIndex >= 0 && oldIndex < states.length)
				tabPane.repaint(getTabBounds(tabPane, oldIndex));
			if (index >= 0)
				tabPane.repaint(getTabBounds(tabPane, index));
			return index;
		}

		public void propertyChange(PropertyChangeEvent evt) {}

		public void leadingScroll() {}

		public void trailingScroll() {}

		public void showDropDownMenu() {}

		public void install() {
			if (states == null) {
				TabModel tm = tabModel;
				TabState[] s = states = new TabState[tm.getTabCount()];
				TabState h = null;
				for (int i=s.length; --i>=0;) {
					TabState ts = s[i] = new TabState(tm.getTabAt(i));
					if (h == null) {
						tail = h = ts;
					} else {
						ts.next = h;
						h.previous = ts;
						h = ts;
					}
				}
				head = h;
				if (selectedIndex >= 0) {
					if (selectedIndex != 0) {
						selectionChanged(selectedIndex);
					} else {
						h.component = h.tab.getComponent();
					}
					firstVisible = selectedIndex;
				} else {
					firstVisible = -1;
				}
			}
		}

		public void uninstall() {}

		public void repaint(int index) {
			TabPane tp = getTabPane();
			if (tp != null) {
				if (isVisible(index))
					tp.repaint(states[index].getBounds());
			}
		}

		public void paint(Graphics g, JComponent c) {}

		public void calculateScrollButtonSize(boolean hor) {}

		protected void paint(Graphics g, TabPlacement p, TabState[] s,
				int firstIndex, int lastIndex, int sel, CellRendererPane rp,
				boolean checkVisible, int offsetX, int offsetY, int dragIndex) {
			for (int i=firstIndex; i<=lastIndex; i++) {
				TabState ts = s[i];
				if ((!checkVisible || ts.visible) && i != dragIndex) {
					paint(g, p, rp, ts.tab, i, i == sel,
							ts.x+offsetX, ts.y+offsetY, ts.width, ts.height);
				}
			}
		}

		protected void paint(Graphics g, TabPlacement p, CellRendererPane rp, Tab tab,
				int i, boolean sel, int x, int y, int w, int h) {
			Component c = tabRenderer.getTabRendererComponent(tabFolder, tab, i, sel);
			painter.paintTabBackground(g, i, p, sel, x, y, w, h);
			rp.paintComponent(g, c, tabFolder, x, y, w, h, true);
		}

		public int getDropIndex(TabPane tabPane) {
			Point pt = tabPane.getDropPoint();
			if (pt.x < 0)
				return 0;
			int idx = getTabForLocation(tabPane, pt.x, pt.y);
			if (idx < 0)
				return states.length-1;
			if (tabPane.getDraggedIndex() >= 0 && idx < states.length-1) {
				TabState drag = states[tabPane.getDraggedIndex()];
				switch (tabFolder.getTabPlacement()) {
				case TOP: case BOTTOM:
					if (pt.x > drag.x) {
						int x = pt.x + drag.width - states[idx+1].width;
						idx = getTabForLocation(tabPane, x, pt.y);
					}
					break;
				case LEFT: case RIGHT:
					if (pt.y > drag.y) {
						int y = pt.y + drag.height - states[idx+1].height;
						idx = getTabForLocation(tabPane, pt.x, y);
					}
					break;
				}
				if (idx < 0)
					return states.length-1;
			}
			return idx;
		}


		protected int getDropX(TabPane tp) {
			if (tp.getDraggedIndex() < 0) {
				return tp.getDropPoint().x-tp.getDropTabState().width/2;
			} else {
				return tp.getDropTabState().x - tp.mouseSupport.dndArmedEvent.getX() + tp.getDropPoint().x;
			}
		}

		protected void paint(Graphics g, TabPane tp, TabState[] s, int i, int j,
				int sel, CellRendererPane rp, boolean checkVisible) {
			Rectangle r = g.getClipBounds();
			g.setColor(tabFolder.getBackground());
			g.fillRect(r.x, r.y, r.width, r.height);
			if (sel >= 0) {
				painter.paint(g, tp);
			}
			for (int x=r.x; i<=j; i++)
				if ((!checkVisible || s[i].visible) && s[i].x+s[i].width >= x)
					break;
			for (int x=r.x+r.width; j>=i; j--)
				if ((!checkVisible || s[i].visible) && s[j].x < x)
					break;
			int dropIndex = tp.getDropIndex();
			TabPlacement p = tabFolder.getTabPlacement();
			if (dropIndex < 0) {
				paint(g, p, s, i, j, sel, rp, checkVisible, 0, 0, -1);
			} else if (tp.getDraggedIndex() < 0) {
				TabState ts = tp.getDropTabState();
				if (!ts.validSize)
					setTabSize(rp, ts, tabRenderer.getTabMaximumSize(), tabRenderer.getTabMinimumSize(), -1, true);
				paint(g, p, s, i, dropIndex-1, sel, rp, checkVisible, 0, 0, -1);
				int offsetX = 0, offsetY = 0, dropX = 0, dropY = 0;
				switch (tabFolder.getTabPlacement()) {
				case TOP: case BOTTOM:
					offsetX = ts.width;
					dropX = getDropX(tp);
					if (dropIndex == 0) {
						if (dropX < 0)
							dropX = 0;
					} else if (dropIndex == states.length-1) {
						if (dropX > states[dropIndex].x)
							dropX = states[dropIndex].x;
					}
					dropY = s[dropIndex < s.length ? dropIndex : s.length-1].y;
					break;
				case LEFT: case RIGHT:
					offsetY = ts.height;
					// TODO
					break;
				}
				paint(g, p, s, dropIndex, j, sel, rp, checkVisible, offsetX, offsetY, -1);
				paint(g, p, rp, ts.tab, -1, false, dropX, dropY, ts.width, ts.height);
			} else {
				int dragIndex = tp.getDraggedIndex();
				TabState ts = s[dragIndex];
				// this is done so that the correct TabState is queried
				tp.dropTabState = ts;
				int min = Math.min(dragIndex, dropIndex);
				int max = Math.max(dragIndex, dropIndex);
				paint(g, p, s, i, min-1, sel, rp, checkVisible, 0, 0, -1);
				int offsetX = 0, offsetY = 0, dropX = ts.x, dropY = ts.y;
				switch (tabFolder.getTabPlacement()) {
				case TOP: case BOTTOM:
					if (dropIndex < dragIndex) {
						offsetX = s[dragIndex].width;
					} else  if (dropIndex > dragIndex) {
						offsetX = -s[dragIndex].width;
					}
					dropX = getDropX(tp);
					if (dropIndex == 0) {
						if (dropX < 0)
							dropX = 0;
					} else if (dropIndex == states.length-1) {
						int maxX = states[dropIndex].x + states[dropIndex].width - states[dragIndex].width - painter.getTabOverlap();
						if (dropX > maxX)
							dropX = maxX;
					}
					break;
				case LEFT: case RIGHT:
					if (dropIndex < dragIndex) {
						offsetY = s[dragIndex].height;
					} else  if (dropIndex > dragIndex) {
						offsetY = -s[dragIndex].height;
					}
//					dropY = TODO
					break;
				}
				paint(g, p, s, min, max < j ? max : j, sel, rp, checkVisible, offsetX, offsetY, dragIndex);
				paint(g, p, s, max+1, j, sel, rp, checkVisible, 0, 0, -1);
				paint(g, p, rp, ts.tab, dragIndex, dragIndex == sel, dropX, dropY, ts.width, ts.height);
			}
			rp.removeAll();

		}

		protected void setTabSize(CellRendererPane rp, TabState ts, Dimension max, Dimension min, int index, boolean isSelected) {
			ts.validSize = true;
			Component c = tabRenderer.getTabRendererComponent(
					tabFolder, ts.tab, index, isSelected);
			rp.add(c);
			Dimension sz = c.getPreferredSize();
			if (max != null) {
				if (sz.width > max.width)
					sz.width = max.width;
				if (sz.height > max.height)
					sz.height = max.height;
			}
			if (min != null) {
				if (sz.width < min.width)
					sz.width = min.width;
				if (sz.height < min.height)
					sz.height = min.height;
			}
			ts.width = sz.width;
			ts.height = sz.height;
		}

		protected Dimension layoutHorizontalTabs(
				TabState[] s, int index, int lastIndex, int inc, CellRendererPane rp) {
			int sel = selectedIndex;
			int x = 0;
			Dimension max = tabRenderer.getTabMaximumSize();
			Dimension min = tabRenderer.getTabMinimumSize();
			int maxHeight = -1;
			int overlap = painter.getTabOverlap();
			for (int i=index; i!=lastIndex; i+=inc) {
				TabState ts = s[i];
				if (!ts.validSize)
					setTabSize(rp, ts, max, min, i, i == sel);
				if (i != 0)
					x -= overlap;
				ts.x = x;
				ts.y = 0;
				if (maxHeight < ts.height)
					maxHeight = ts.height;
				x += ts.width;
			}
			return new Dimension(x, maxHeight);
		}
	}

	protected class MouseSupport extends RolloverSupport {

		public MouseSupport(TabPane tp) {
			tabPane = tp;
			tp.addMouseListener(this);
			tp.addMouseMotionListener(this);
		}

		TabPane tabPane;

		int pressedIndex = Integer.MIN_VALUE;

		int rolloverIndex = Integer.MIN_VALUE;


		// drag support fields
		MouseEvent dndArmedEvent;
		boolean isDragging = false;
		int motionThreshold;

		public int getRolloverIndex() {
			return rolloverIndex;
		}

		public int getPressedIndex() {
			return pressedIndex;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				setPressedIndex(tabLayoutState.getTabForLocation(tabPane, e.getX(), e.getY()));
				if (tabFolder.isDragEnabled()) {
					motionThreshold = DragSource.getDragThreshold();
					if (motionThreshold < 10)
						motionThreshold = 10;
					dndArmedEvent = e;
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				if (!isDragging) {
					int index = getTabIndex(e.getX(), e.getY());
					if (index == pressedIndex)
						tabLayoutState.selectTab(tabPane, index);
				}
				setPressedIndex(Integer.MIN_VALUE);
				isDragging = false;
			}
			dndArmedEvent = null;
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			setRolloverIndex(getTabIndex(e.getX(), e.getY()));
		}

		int getTabIndex(int x, int y) {
			return tabLayoutState.getTabForLocation(tabPane, x, y);
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (dndArmedEvent != null) {
				int dx = Math.abs(e.getX() - dndArmedEvent.getX());
				int dy = Math.abs(e.getY() - dndArmedEvent.getY());
				if ((dx > motionThreshold) || (dy > motionThreshold)) {
					TransferHandler th = tabPane.getTransferHandler();
					int actions = th.getSourceActions(tabPane);
					if ((actions & TransferHandler.MOVE) != 0) {
						isDragging = true;
						tabPane.setDraggedIndex(pressedIndex);
						setPressedIndex(Integer.MIN_VALUE);
						hideRolloverComponent();
						th.exportAsDrag(tabPane, dndArmedEvent, TransferHandler.MOVE);
					}
				}
			}
		}

		@Override
		protected void hide(Component c, boolean validate) {
			// TODO, check if it ever is the case that c.getParent is not tabPane
			if (c.getParent() == tabPane)
				super.hide(c, validate);
		}

		@Override
		protected void hideRolloverComponent() {
			super.hideRolloverComponent();
			setRolloverIndex(Integer.MIN_VALUE);
		}

		protected void setPressedIndex(int index) {
			if (index != pressedIndex) {
				onPress(index);
				pressedIndex = index;
			}
		}

		protected void setRolloverIndex(int index) {
			if (index != rolloverIndex) {
				int oldIndex = rolloverIndex;
				rolloverIndex = index;
				onRollover(index, oldIndex);
			}
		}


		protected void onRollover(int index, int oldIndex) {
			int modelIndex = tabLayoutState.onRollover(tabPane, index, oldIndex);
			if (index >= 0) {
				Component c = tabRenderer.getTabRolloverComponent(
						tabFolder, tabModel.getTabAt(modelIndex), modelIndex, modelIndex == selectedIndex);
				Rectangle b = c == null ? null : tabLayoutState.getTabBounds(tabPane, index);
				setRolloverComponent(tabPane, c, b);
			} else {
				deferHideRolloverComponent();
			}

		}

		protected void onPress(int index) {
			tabLayoutState.onPress(tabPane, index);

		}

		public void validate() {
			Point pt = tabPane.getMousePosition(true);
			if (pt != null) {
				int idx = getTabIndex(pt.x, pt.y);
				if (idx == rolloverIndex) {
					if (idx != Integer.MIN_VALUE)
						onRollover(idx, Integer.MIN_VALUE);
				} else {
					setRolloverIndex(idx);
				}
			} else if (rolloverIndex >= 0) {
				setRolloverIndex(Integer.MIN_VALUE);
			}
		}

	}

	static class TabTransferHandler extends TransferHandler implements DropTargetListener, UIResource {

		TabPane tabPane;

		long lastScroll;

		public boolean canImport(TransferSupport support) {
			return tabPane != null;
		}

		public boolean importData(TransferSupport support) {
			if (tabPane != null)
				return tabPane.importData(support);
			return false;
		}

		protected Transferable createTransferable(JComponent c) {
			TabPane tp = getTabPane(c);
			if (tp != null)
				return tp.createTransferable();
			return null;
		}

		public int getSourceActions(JComponent c) {
			return MOVE;
		}

		public void exportDone(JComponent source, Transferable data, int action) {
			TabPane tp = getTabPane(source);
			if (tp != null)
				tp.exportDone(data, action);
			super.exportDone(source, data, action);
		}

		@Override
		public void dragEnter(DropTargetDragEvent dtde) {
			tabPane = getTabPane(dtde.getDropTargetContext().getComponent());
			if (tabPane != null && !tabPane.dragEnter(dtde))
				tabPane = null;

		}

		@Override
		public void dragExit(DropTargetEvent dte) {
			cleanUpDrop(dte);
		}

		@Override
		public void dragOver(DropTargetDragEvent dtde) {
			if (tabPane == null)
				return;

			Component c = dtde.getDropTargetContext().getComponent();
			if (c instanceof TabPane) {
				((TabPane)c).setDropPoint(dtde.getLocation());
			} else if (c instanceof AbstractButton) {
				long t = System.currentTimeMillis();
				if (t-lastScroll > 500) {
					lastScroll = t;
					Container p = c.getParent();
					if (p instanceof ScrollPane) {
						ScrollPane s = (ScrollPane)p;
						s.dndAutoscroll(c);
					}
				}
			} else if (c instanceof TabFolder) {
				TabFolder tf = (TabFolder)c;
				Point pt = dtde.getLocation();
				pt = SwingUtilities.convertPoint(c, pt, tabPane);
				switch (tf.getTabPlacement()) {
				case TOP: pt.y = tabPane.getHeight()-3; break;
				case BOTTOM: pt.y = 3; break;
				case LEFT: pt.x = tabPane.getWidth()-3; break;
				case RIGHT: pt.x = 3; break;
				}
				tabPane.setDropPoint(pt);
			}
		}

		@Override
		public void drop(DropTargetDropEvent dtde) {
			cleanUpDrop(dtde);
		}

		@Override
		public void dropActionChanged(DropTargetDragEvent dtde) {
		}


		private void cleanUpDrop(DropTargetEvent dte) {
			if (tabPane != null) {
				tabPane.cleanUpDrop();
				tabPane = null;
			}
		}

		private TabPane getTabPane(Component c) {
			if (c instanceof TabPane) {
				return (TabPane)c;
			} else if (c instanceof AbstractButton) {
				Container p = c.getParent();
				if (p instanceof ScrollPane)
					return (TabPane)((ScrollPane)p).viewport.getView();
			} else if (c instanceof TabFolder) {
				TabFolderUI ui = ((TabFolder)c).getUI();
				if (ui instanceof BasicTabFolderUI)
					return ((BasicTabFolderUI)ui).tabLayoutState.getTabPane();
			}
			return null;
		}
	}

	private static TabTransferHandler transferHandler;

	protected TabTransferHandler getTabTransferHandler() {
		if (transferHandler == null)
			transferHandler = new TabTransferHandler();
		return transferHandler;
	}

	protected MouseSupport createMouseSupport(TabPane tabPane) {
		return new MouseSupport(tabPane);
	}

	protected class TabPane extends JComponent {

		public TabPane() {
			mouseSupport = createMouseSupport(this);
			setTransferHandler(getTabTransferHandler());
			try {
				getDropTarget().addDropTargetListener(getTabTransferHandler());
			} catch (TooManyListenersException e) {
				e.printStackTrace();
			}
		}

		MouseSupport mouseSupport;

		@Override
		public void paint(Graphics g) {
			tabLayoutState.paint(g, this);
			if (mouseSupport.getRolloverComponent() != null)
				super.paint(g);
		}


		public int getRolloverIndex() {
			return mouseSupport.getRolloverIndex();
		}

		public int getPressedIndex() {
			return mouseSupport.getPressedIndex();
		}

		public void updateRollover() {
			Point pt = getMousePosition(true);
			if (pt != null) {
				int idx = mouseSupport.getTabIndex(pt.x, pt.y);
				if (getRolloverIndex() == idx) {
					mouseSupport.onRollover(idx, Integer.MIN_VALUE);
				} else {
					mouseSupport.setRolloverIndex(idx);
				}
			} else if (getRolloverIndex() >= 0) {
				mouseSupport.setRolloverIndex(Integer.MIN_VALUE);
			}
		}

		public Transferable createTransferable() {
			return tabLayoutState.createTransferable(this);
		}

		public boolean canImport(TransferSupport support) {
			return dropTabState != null;
		}

		private boolean insertTab(MutableTabModel tm, Tab tab, int idx) {
			if (draggedIndex >= 0 && idx > draggedIndex)
				idx++;
			boolean b = tm.insertTab(dropTabState.tab, idx);
			if (b) {
				tm.setSelectedIndex(idx);
			}
			return b;
		}

		public boolean importData(TransferSupport support) {
			if (tabModel instanceof MutableTabModel) {
				MutableTabModel tm = (MutableTabModel)tabModel;
				if (support.isDrop()) {
					if (dropTabState != null) {
						if (tm.canInsert(dropTabState.tab)) {
							int idx = tabLayoutState.getTabForLocation(this, dropPoint.x, dropPoint.y);
							return insertTab(tm, dropTabState.tab, idx);
						}
					}
				} else {
					try {
						if (support.isDataFlavorSupported(Tab.localFlavor)) {
							Tab tab = (Tab)support.getTransferable().getTransferData(Tab.localFlavor);
							return insertTab(tm, tab, -1);
						} else if (support.isDataFlavorSupported(Tab.serialFlavor)) {
							Tab tab = (Tab)support.getTransferable().getTransferData(Tab.serialFlavor);
							return insertTab(tm, tab, -1);
						}
					} catch (IOException e) {
						e.printStackTrace();
					} catch (UnsupportedFlavorException e) {
						e.printStackTrace();
					}
				}
			}
			return false;
		}

		public void exportDone(Transferable data, int action) {
			mouseSupport.dndArmedEvent = null;
			mouseSupport.isDragging = false;
			setDraggedIndex(Integer.MIN_VALUE);
			if (action == TransferHandler.MOVE) {
				if (tabModel instanceof MutableTabModel) {
					MutableTabModel tm = (MutableTabModel)tabModel;
					if (data instanceof TabTransferable) {
						TabTransferable tabData = (TabTransferable)data;
						Tab tab = tabData.getTab();
						int idx = tabData.getIndex();
						TabState[] states = tabLayoutState.states;
						if (states[idx].tab != tab) {
							idx++;
							if (states[idx].tab != tab)
								return;
						}
						tm.removeTab(idx);
					}
				}
			}
		}

		public int getDraggedIndex() {
			return draggedIndex;
		}

		public void setDraggedIndex(int index) {
			draggedIndex = index;
		}

		public Point getDropPoint() {
			return dropPoint;
		}

		public int getDropIndex() {
			return dropIndex;
		}

		public TabState getDropTabState() {
			return dropTabState;
		}

		Point dropPoint;

		TabState dropTabState;

		int dropIndex = -1;

		int draggedIndex = Integer.MIN_VALUE;

		boolean dragEnter(DropTargetDragEvent dtde) {
			if (tabModel instanceof MutableTabModel) {
				MutableTabModel tm = (MutableTabModel)tabModel;
				try {
					if (dtde.isDataFlavorSupported(Tab.localFlavor)) {
						Tab tab = (Tab)dtde.getTransferable().getTransferData(Tab.localFlavor);
						if (tm.canInsert(tab))
							dropTabState = new TabState(tab);
					} else if (dtde.isDataFlavorSupported(Tab.serialFlavor)) {
						Tab tab = (Tab)dtde.getTransferable().getTransferData(Tab.serialFlavor);
						if (tm.canInsert(tab))
							dropTabState = new TabState(tab);
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (UnsupportedFlavorException e) {
					e.printStackTrace();
				}
			}
			return dropTabState != null;
		}

		void setDropPoint(Point pt) {
			if (!pt.equals(dropPoint)) {
				dropPoint = pt;
				dropIndex = tabLayoutState.getDropIndex(this);
				repaint();
			}
		}


		void cleanUpDrop() {
			if (dropTabState != null) {
				dropTabState = null;
				dropPoint = null;
				dropIndex = -1;
				repaint();
			}
		}

	}

	protected class ScrollPane extends JComponent implements ActionListener {

		/**
		 * 
		 * @param layout SwingConstants.HORIZONTAL or SwingConstants.VERTICAL
		 * @param scroll SwingConstants.HORIZONTAL or SwingConstants.VERTICAL
		 * @param viewport
		 */
		public ScrollPane(int layout, int scroll, boolean dropDown) {
			setOpaque(true);
			layoutOrientation = layout;
			scrollOrientation = scroll;
			viewport = new JViewport();
			viewport.setOpaque(false);
			viewport.setFont(null);
			leadingScrollButton = createScrollButton(
					scroll == SwingConstants.HORIZONTAL ?
							tabFolder.getComponentOrientation().isLeftToRight() ?
									SwingConstants.WEST : SwingConstants.EAST : SwingConstants.NORTH);
			leadingScrollButton.addActionListener(this);
			leadingScrollButton.setTransferHandler(getTabTransferHandler());
			try {
				leadingScrollButton.getDropTarget().addDropTargetListener(getTabTransferHandler());
			} catch (TooManyListenersException e) {
				e.printStackTrace();
			}
			trailingScrollButton = createScrollButton(
					scroll == SwingConstants.HORIZONTAL ?
							tabFolder.getComponentOrientation().isLeftToRight() ?
									SwingConstants.EAST : SwingConstants.WEST : SwingConstants.SOUTH);
			trailingScrollButton.addActionListener(this);
			trailingScrollButton.setTransferHandler(getTabTransferHandler());
			try {
				trailingScrollButton.getDropTarget().addDropTargetListener(getTabTransferHandler());
			} catch (TooManyListenersException e) {
				e.printStackTrace();
			}
			if (dropDown) {
				dropDownButton = createDropDownButton();
				dropDownButton.addActionListener(this);
				add(dropDownButton);
			}
			add(leadingScrollButton);
			add(trailingScrollButton);
			add(viewport);
		}

		int layoutOrientation;

		int scrollOrientation;

		JViewport viewport;

		AbstractButton leadingScrollButton;

		AbstractButton trailingScrollButton;

		AbstractButton dropDownButton;

		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == leadingScrollButton) {
				tabLayoutState.leadingScroll();
			} else if (e.getSource() == trailingScrollButton) {
				tabLayoutState.trailingScroll();
			} else if (e.getSource() == dropDownButton) {
				tabLayoutState.showDropDownMenu();
			}
		}

		void dndAutoscroll(Component c) {
			if (c == leadingScrollButton) {
				tabLayoutState.leadingScroll();
				TabPane tp = (TabPane)viewport.getView();
				tp.setDropPoint(viewport.getViewPosition());
			} else if (c == trailingScrollButton) {
				tabLayoutState.trailingScroll();
				TabPane tp = (TabPane)viewport.getView();
				Point pt = viewport.getViewPosition();
				Dimension sz = viewport.getExtentSize();
				pt.x += sz.width;
				pt.y += sz.height;
				tp.setDropPoint(pt);
			}
		}

		private void maximizeViewport() {
			viewport.setBounds(0, 0, getWidth(), getHeight());
			leadingScrollButton.setVisible(false);
			trailingScrollButton.setVisible(false);
			if (dropDownButton != null)
				dropDownButton.setVisible(false);
		}

		public void doLayout() {
			Dimension size = viewport.getViewSize();
			if (layoutOrientation == SwingConstants.HORIZONTAL) {
				if (size.width <= getWidth()) {
					maximizeViewport();
				} else if (scrollOrientation == SwingConstants.HORIZONTAL) {
					boolean ltr = tabFolder.getComponentOrientation().isLeftToRight();
					Rectangle rect = viewport.getViewRect();
					boolean leftVisible = rect.x > 0;
					boolean rightVisible = rect.x+rect.width < size.width;
					int x = 0;
					if (dropDownButton != null) {
						dropDownButton.setVisible(leftVisible || rightVisible);
						if (!ltr) {
							Dimension s = dropDownButton.getPreferredSize();
							dropDownButton.setBounds(x, (size.height-s.height)/2, s.width, s.height);
							x += s.width;
						}
					}
					Component left = ltr ? leadingScrollButton : trailingScrollButton;
					left.setVisible(leftVisible);
					if (leftVisible) {
						Dimension s = left.getPreferredSize();
						left.setBounds(x, 1, s.width, size.height-2);
						x += s.width;
					}
					int w = getWidth() - x;
					if (ltr && dropDownButton != null) {
						Dimension s = dropDownButton.getPreferredSize();
						dropDownButton.setBounds(x+w-s.width, (size.height-s.height)/2, s.width, s.height);
						w -= s.width;
					}
					Component right = ltr ? trailingScrollButton : leadingScrollButton;
					right.setVisible(rightVisible);
					if (rightVisible) {
						Dimension s = right.getPreferredSize();
						right.setBounds(x+w-s.width, 1, s.width, size.height-2);
						w -= s.width;
					}
					viewport.setBounds(x, 0, w, getHeight());
				} else {
					// TODO vertical scroll layout
				}
			} else {
				if (size.height <= getHeight()) {
					maximizeViewport();
				} else if (scrollOrientation == SwingConstants.VERTICAL) {
					// TODO vertical scroll layout
				} else {
					// TODO horizontal scroll layout
				}
			}
			viewport.doLayout();

		}

		void calculateScrollButtonSize(boolean hor) {
			calculateTabRegionSize(leadingScrollButton, hor);
			if (layoutOrientation == scrollOrientation)
				calculateTabRegionSize(trailingScrollButton, hor);
			if (dropDownButton != null) {
				if (hor || scrollOrientation == SwingConstants.VERTICAL)
					calculateTabRegionSize(dropDownButton, hor);
			}
		}


	}

	/** Unit Scroll Tab Layout State
	 * <p>
	 * Used for TabPlacement.TOP|BOTTOM & TabLayoutPolicy.HORIZONTAL_SCROLL
	 * <p>
	 * And for TabPlacement.LEFT|RIGHT & TabLayoutPolicy.VERTICAL_SCROLL
	 *
	 */
	protected class USTabLayoutState extends TabLayoutState {

		ScrollPane scrollPane;

		TabPane tabPane;

		boolean leadIsFlush = true;

		public USTabLayoutState(TabLayoutState previous, int orientation) {
			super(previous);
			scrollPane = createScrollPane(orientation, orientation, true);
			tabPane = createTabPane();

		}

		@Override
		public TabPane getTabPane() {
			return tabPane;
		}

		@Override
		public void install() {
			super.install();
			tabPane.add(rendererPane);
			scrollPane.viewport.setView(tabPane);
			tabFolder.add(scrollPane);
		}

		@Override
		public void uninstall() {
			super.uninstall();
			tabPane.remove(rendererPane);
			tabFolder.remove(scrollPane);
		}

		@Override
		public void leadingScroll() {
			leadIsFlush = true;
			int index = firstVisible;
			if (!isFirstCropped() && --index < 0)
				return;
			tabPane.scrollRectToVisible(states[index].getBounds());
			scrollPane.revalidate();
		}

		@Override
		public void trailingScroll() {
			leadIsFlush = false;
			int index = lastVisible;
			if (!isLastCropped() && ++index >= states.length)
				return;
			tabPane.scrollRectToVisible(states[index].getBounds());
			scrollPane.revalidate();
		}

		@Override
		public int getTabForLocation(int x, int y) {
			return getTabForLocation(scrollPane, x, y);
		}

		@Override
		protected Rectangle getTabBounds(Rectangle r) {
			return getTabBounds(scrollPane, r);
		}

		int getLeftTab(int i, int j, int inc, int x) {
			TabState[] s = states;
			for (; i!=j; i+=inc)
				if (s[i].x + s[i].width > x)
					return i;
			return Integer.MIN_VALUE;
		}
		int getRightTab(int i, int j, int inc, int x) {
			TabState[] s = states;
			for (; i!=j; i+=inc)
				if (s[i].x < x)
					return i;
			return Integer.MIN_VALUE;
		}

		@Override
		public void layoutTabs(int startPos, int endPos) {
			switch (tabFolder.getTabPlacement()) {
			case TOP: layoutHorizontalTabs(startPos, endPos, true); break;
			case BOTTOM: layoutHorizontalTabs(startPos, endPos, false); break;
			case LEFT:
			case RIGHT:
			}
		}

		void layoutHorizontalTabs(int startPos, int endPos, boolean top) {
			Insets insets = getTabAreaInsets();
			boolean validate = !validTabLayout;
			if (validate) {
				validTabLayout = true;
				Dimension s = tabFolder.getComponentOrientation().isLeftToRight() ? 
						layoutHorizontalTabs(states, 0, states.length, 1, rendererPane) :
							layoutHorizontalTabs(states, states.length-1, -1, -1, rendererPane);
						tabApex = s.height;
						tabPane.setPreferredSize(s);
			}
			if (top) {
				scrollPane.setBounds(
						startPos, tabRegionBounds.y+tabRegionBounds.height-tabApex-insets.bottom,
						endPos-startPos, tabApex);
			} else {
				scrollPane.setBounds(
						startPos, tabRegionBounds.y+insets.bottom,
						endPos-startPos, tabApex);
			}
			scrollPane.doLayout();
			Rectangle r = scrollPane.viewport.getViewRect();
			if (tabFolder.getComponentOrientation().isLeftToRight()) {
				firstVisible = getLeftTab(0, states.length, 1, r.x);
				lastVisible = getRightTab(states.length-1, -1, -1, r.x+r.width);
			} else {
				firstVisible = getRightTab(0, states.length, 1, r.x);
				lastVisible = getLeftTab(states.length-1, -1, -1, r.x+r.width);
			}
			if (validate)
				tabPane.mouseSupport.validate();
		}

		boolean isFirstCropped() {
			return scrollPane.viewport.getViewPosition().x > states[firstVisible].x;
		}

		boolean isLastCropped() {
			Rectangle r = scrollPane.viewport.getViewRect();
			TabState s = states[lastVisible];
			return r.x + r.width < s.x + s.width;
		}

		@Override
		public void calculateScrollButtonSize(boolean hor) {
			scrollPane.calculateScrollButtonSize(hor);
		}

		@Override
		public boolean isDropDownAction(ActionEvent e) {
			return e.getSource() == scrollPane.dropDownButton;
		}

		@Override
		public int getPressedIndex() {
			return getPressedIndex(tabPane);
		}

		@Override
		public int getRolloverIndex() {
			return tabPane.getRolloverIndex();
		}

		@Override
		public void paint(Graphics g, JComponent c) {
			paint(g, tabPane, states, firstVisible, lastVisible, selectedIndex, rendererPane, false);
		}

	}

	/** Block Scroll Tab Layout State
	 * <p>
	 * Used for TabPlacement.TOP|BOTTOM & TabLayoutPolicy.VERTICAL_SCROLL
	 * <p>
	 * And for TabPlacement.LEFT|RIGHT & TabLayoutPolicy.HORIZONTAL_SCROLL
	 *
	 */
	protected class BSTabLayoutState extends TabLayoutState {

		ScrollPane scrollPane;

		public BSTabLayoutState(TabLayoutState previous, int layout) {
			super(previous);

		}

		public int getTabForLocation(int x, int y) {
			return getTabForLocation(scrollPane, x, y);
		}

		protected Rectangle getTabBounds(Rectangle r) {
			return getTabBounds(scrollPane, r);
		}

		public void tabsChanged(int firstIndex, int lastIndex) {

		}

		public void leadingScroll() {

		}

		public void trailingScroll() {

		}

		public void layoutTabs(int startPos, int endPos) {

		}

		public void paint(Graphics g, JComponent c) {

		}


	}

	protected int getMRUPopupDelay() {
		return 300;
	}



	/** Most Recently Used (are visible) Tab Layout State
	 * 
	 * Used for TabLayoutPolicy.MRU
	 *
	 */
	protected class MRUTabLayoutState extends TabLayoutState implements ActionListener, PopupMenuListener {

		protected TabPane tabPane;

		protected AbstractButton dropDownButton;

		protected int[] gapIndices;

		protected JPopupMenu popup;

		protected ScrollPane popupScrollPane;

		protected TabPane popupTabPane;

		protected CellRendererPane popupRendererPane;

		protected TabState[] popupStates;

		protected int popupIndex;

		protected Timer popupTimer;

		private boolean popupFromClick;

		public MRUTabLayoutState(TabLayoutState previous, int layout) {
			super(previous);
			tabPane = createTabPane();
			dropDownButton = createDropDownButton();
			dropDownButton.setSize(dropDownButton.getPreferredSize());
			popup = createPopupMenu();
			popupScrollPane = createScrollPane(layout, layout, false);
			popupTabPane = createTabPane();
			popupScrollPane.viewport.setView(popupTabPane);
			popupRendererPane = new CellRendererPane();
			Object property = tabFolder.getClientProperty("MRUPopupDelay");
			int popupDelay = property != null ? (Integer)property : getMRUPopupDelay();
			popupTimer = new Timer(popupDelay, this);
			popupTimer.setRepeats(false);
			popupTabPane.add(popupRendererPane);
			popup.add(popupScrollPane, BorderLayout.CENTER);
		}

		@Override
		public boolean checkVisible() { return true; }

		@Override
		public boolean isPaintingPopup() {
			return paintingPopup;
		}

		@Override
		public void install() {
			super.install();
			tabPane.add(rendererPane);
			tabFolder.add(tabPane);
			tabFolder.add(dropDownButton);
			installListeners();
		}

		@Override
		public void uninstall() {
			super.uninstall();
			uninstallListeners();
			tabFolder.remove(dropDownButton);
			tabFolder.remove(tabPane);
			tabFolder.remove(rendererPane);
		}

		protected void installListeners() {
			popup.addPopupMenuListener(this);

		}

		protected void uninstallListeners() {
			popup.removePopupMenuListener(this);

		}

		protected JPopupMenu createPopupMenu() {
			JPopupMenu popup = new JPopupMenu();
			popup.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			popup.setLayout(new BorderLayout());
			return popup;
		}

		@Override
		public Transferable createTransferable(TabPane tp) {
			if (tp == tabPane) {
				return super.createTransferable(tp);
			} else if (tp == popupTabPane) {
				int idx = popupTabPane.getDraggedIndex();
				return idx < 0 ? null : new TabTransferable(tabModel.getTabAt(popupIndex+idx), popupIndex+idx);
			}
			return null;
		}

		@Override
		public void selectionChanged(int newIndex) {
			if (newIndex >= 0 && !states[newIndex].visible) {
				validTabLayout = false;
				tabPane.repaint();
			}
			super.selectionChanged(newIndex);
		}

		@Override
		public boolean isDropDownAction(ActionEvent e) {
			return e.getSource() == dropDownButton;
		}

		@Override
		public void tabsChanged(int firstIndex, int lastIndex) {
			super.tabsChanged(firstIndex, lastIndex);
			if (popupStates != null) {
				int idx = firstIndex - popupIndex;
				if (idx >= 0 && idx < popupStates.length)
					update(popupTabPane, idx);
			}
		}

		@Override
		public void tabsRemoved(int firstIndex, int lastIndex) {
			super.tabsRemoved(firstIndex, lastIndex);
			if (popupStates != null) {
				int idx = firstIndex - popupIndex;
				if (idx >= 0 && idx < popupStates.length)
					update(popupTabPane, idx);
			}
		}

		@Override
		public void tabsAdded(int firstIndex, int lastIndex) {
			super.tabsAdded(firstIndex, lastIndex);
			if (popupStates != null) {
				int idx = firstIndex - popupIndex;
				if (idx >= 0 && idx < popupStates.length)
					update(popupTabPane, idx);
			}
		}



		@Override
		public void repaint(int index) {
			if (isVisible(index)) {
				tabPane.repaint(states[index].getBounds());
			} else if (popupStates != null) {
				if (index >= popupIndex && index < popupIndex+popupStates.length) {
					popupTabPane.repaint(states[index].getBounds());
				}
			}
		}

		@Override
		public void popupMenuCanceled(PopupMenuEvent e) {}

		@Override
		public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			popupStates = null;
		}

		@Override
		public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

		@Override
		public void actionPerformed(ActionEvent e) {
			int index = tabPane.getRolloverIndex();
			if (index < 0 && index != Integer.MIN_VALUE) {
				popupFromClick = false;
				showPopup(this.convertGapIndexToFirstHiddenIndex(index));
			} else if (popup.isVisible() && popupTabPane.getRolloverIndex() < 0) {
				popup.setVisible(false);
			}
		}

		@Override
		public int onRollover(TabPane tp, int index, int oldIndex) {
			super.onRollover(tp, index, oldIndex);
			if (popup.isVisible()) {
				if (!popupFromClick && (tp == tabPane || index == Integer.MIN_VALUE))
					popupTimer.restart();
			} else if (index < 0 && index != Integer.MIN_VALUE) {
				popupTimer.restart();
			}
			return index < 0 || tp == tabPane ? index : popupIndex + index;
		}

		@Override
		public void onExit(TabPane tp) {
			if (tp == popupTabPane) {
				popupTimer.restart();
			}
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			String name = evt.getPropertyName();
			if (name == "MRUPopupDelay") {
				Object property = evt.getNewValue();
				popupTimer.setInitialDelay(property != null ? (Integer)property : getMRUPopupDelay());
			}
		}

		@Override
		public void leadingScroll() {
			// TODO, will be called for popupTabPane, as it uses unit scroll
			// when there are more tabs than can be displayed on a single row/col
		}

		@Override
		public void trailingScroll() {
			// TODO, same as above
		}

		@Override
		public TabPane getTabPane() {
			return tabPane;
		}

		int gapIndex(int tabIndex) {
			for (int i=gapIndices.length; --i>=0;) {
				if (gapIndices[i] <= tabIndex) {
					return -i-1;
				}
			}
			return -1;
		}

		@Override
		protected int getTabForLocation(TabPane tp, int x, int y) {
			if (tp == tabPane) {
				TabState[] s = states;
				if (s.length == 0)
					return Integer.MIN_VALUE;
				int gap = getMRULayoutGap();
				for (int i=firstVisible, j=lastVisible; i<=j; i++) {
					TabState ts = s[i];
					if (ts.visible) {
						int tsx = ts.x;
						if (i > 0 && !s[i-1].visible)
							tsx += gap;
						if (x >= tsx) {
							tsx = ts.x+ts.width;
							if (i+1 < s.length && !s[i+1].visible)
								tsx -= gap;
							if (x >= tsx)
								continue;
							return y >= ts.y && y < ts.y+ts.height ? i : Integer.MIN_VALUE;
						}
						return gapIndex(i-1);
					}
				}
				return states.length == 0 || states[states.length-1].visible ?
						Integer.MIN_VALUE : gapIndex(states.length-1);
			} else {
				return getTabForLocation(popupStates, 0, popupStates.length-1, x, y);
			}
		}

		@Override
		public Rectangle getTabBounds(TabPane tp, int index) {
			if (tp != tabPane)
				index += popupIndex;
			return states[index].getBounds();
		}

		@Override
		public int getTabForLocation(int x, int y) {
			x += tabPane.getX();
			y += tabPane.getY();
			return getTabForLocation(tabPane, x, y);
		}

		@Override
		public int getPressedIndex() {
			if (popupStates != null) {
				int idx = getPressedIndex(popupTabPane);
				if (idx >= 0)
					idx += popupIndex;
				return idx;
			} else {
				return getPressedIndex(tabPane);
			}
		}

		@Override
		public int getRolloverIndex() {
			int idx = tabPane.getRolloverIndex();
			if (idx < 0 && popupStates != null) {
				idx = popupTabPane.getRolloverIndex();
				if (idx >= 0)
					idx += popupIndex;
			}
			return idx;
		}

		@Override
		public void selectTab(TabPane source, int index) {
			if (source == tabPane) {
				if (index >= 0) {
					if (index != selectedIndex)
						tabModel.setSelectedIndex(index);
				} else if (index != Integer.MIN_VALUE) {
					popupFromClick = true;
					if (!popup.isVisible())
						showPopup(convertGapIndexToFirstHiddenIndex(index));
				}			
			} else {
				if (index >= 0)
					tabModel.setSelectedIndex(index + popupIndex);
				popup.setVisible(false);
			}

		}

		public void showPopup(int index) {
//			index = convertGapIndexToFirstHiddenIndex(index);
			int count = 0;
			Dimension max = tabRenderer.getTabMaximumSize();
			Dimension min = tabRenderer.getTabMinimumSize();
			CellRendererPane rp = popupRendererPane;
			for (int i=index, j=tabModel.getTabCount(); i<j; i++) {
				if (states[i].visible)
					break;
				if (!states[i].validSize)
					setTabSize(rp, states[i], max, min, i, false);
				count++;
			}
			TabState[] ps = Arrays.copyOfRange(states, index, index+count);
			popupStates = ps;
			popupIndex = index;
			Dimension size = layoutHorizontalTabs(
					popupStates, 0, popupStates.length, 1, popupRendererPane);
			popupTabPane.setPreferredSize(new Dimension(size));
			Insets insets = popup.getInsets();

			//TODO, the following is only for TabPlacement.TOP
			int x;
			size.width += insets.left + insets.right;
			size.height += insets.top + insets.bottom;
			boolean scrollVisible = size.width > tabFolder.getWidth();
			if (scrollVisible) {
				x = 0;
				size.width = tabFolder.getWidth();
			} else {
				if (index > 0) {
					TabState ts = states[index-1];
					x = ts.x+ts.width + getMRULayoutGap()/2 - size.width/2;
					if (x < 0)
						x = 0;
					else if (x+size.width > tabFolder.getWidth())
						x = tabFolder.getWidth() - size.width;
				} else {
					x = 0;
				}
			}
			popup.setPopupSize(size);
			popup.setBackground(tabFolder.getBackground());
			popup.show(tabFolder, x, tabRegionBounds.height-1);
		}

		@Override
		public boolean isVisible(int index) {
			return index >= 0 && index < states.length && states[index].visible;
		}


		@Override
		public void layoutTabs(int startPos, int endPos) {
			int x=0, y=0, w, h;
			TabPlacement p = tabFolder.getTabPlacement();
			switch (p) {
			default: return;
			case TOP: case BOTTOM:
				x = startPos;
				w = endPos-startPos;
				h = tabRegionBounds.height;
				break;
			case LEFT: case RIGHT:
				y = startPos;
				w = tabRegionBounds.width;
				h = endPos-startPos;
				break;
			}
			switch (p) {
			default: return;
			case TOP:
				y = 0;
				layoutHorizontalTabs(true, endPos - startPos);
				break;
			case BOTTOM:
				y = tabRegionBounds.y;
				layoutHorizontalTabs(false, endPos - startPos);
				break;
			case LEFT:
				x = 0;
				layoutVerticalTabs(true, endPos - startPos);
				break;
			case RIGHT:
				x = tabRegionBounds.x;
				layoutVerticalTabs(false, endPos - startPos);
				break;
			}
			if (dropDownButton.isVisible()) {
				int b_w = dropDownButton.getWidth();
				int b_h = dropDownButton.getHeight();
				switch (p) {
				case TOP: case BOTTOM:
					if (tabFolder.getComponentOrientation().isLeftToRight()) {
						w -= b_w;
						dropDownButton.setLocation(x+w, y+(h-b_h)/2);
					} else {
						dropDownButton.setLocation(x, y+(h-b_h)/2);
						x += b_w;
						w -= b_w;
					}
					break;
				case LEFT: case RIGHT:
					break;
				}
			}
			tabPane.setBounds(x, y, w, h);
		}

		private int findLastVisible(TabState[] s, int idx) {
			for (; idx>=0; idx--) {
				if (s[idx].visible)
					return idx;
			}
			return -1;
		}

		private int findFirstVisible(TabState[] s, int idx) {
			for (; idx<s.length; idx++) {
				if (s[idx].visible)
					return idx;
			}
			return Integer.MIN_VALUE;
		}

		private int convertGapIndexToFirstHiddenIndex(int gapIndex) {
			return gapIndices[-gapIndex-1]+1;
		}


		void layoutVerticalTabs(boolean left, int width) {
			// TODO
		}

		void layoutHorizontalTabs(boolean top, int width) {
			if (!validTabLayout || span != tabFolder.getWidth()) {
				validTabLayout = true;
				span = tabFolder.getWidth();
				TabState[] s = states;
				if (s.length == 0)
					return;
				int height = tabRegionBounds.height;
				int gap = getMRULayoutGap();
				// iterate over tab states, mark as !visible and validate size
				int sel = selectedIndex;
				Dimension max = tabRenderer.getTabMaximumSize();
				Dimension min = tabRenderer.getTabMinimumSize();
				CellRendererPane rp = rendererPane;
				int maxHeight = 0;
				for (int i=s.length; --i>=0;) {
					s[i].visible = false;
					if (!s[i].validSize)
						setTabSize(rp, s[i], max, min, i, i == sel);
					if (s[i].height > maxHeight)
						maxHeight = s[i].height;
				}
				tabApex = maxHeight;
				// iterate over link list, mark as visible until allowed width is consumed
				for (TabState ts=head; ts!=null; ts=ts.next) {
					if (width - ts.width < 0)
						break;
					width -= ts.width;
					ts.visible = true;
				}
				if (tail == null || tail.visible) {
					dropDownButton.setVisible(false);
				} else {
					dropDownButton.setVisible(true);
					width -= dropDownButton.getWidth();
				}

				// insert gaps / set positions
				int idx = firstVisible = findFirstVisible(s, 0);
				int x = 0;
				int[] gaps = new int[s.length];
				int gaps_idx = 0;
				int overlap = painter.getTabOverlap();
				for (int lstIdx=-1; idx<s.length; idx++) {
					TabState ts = s[idx];
					if (ts.visible) {
						if (lstIdx != idx-1) {
							gaps[gaps_idx++] = lstIdx;
							x += gap;
						} else if (idx != firstVisible) {
							x -= overlap;
						}
						ts.x = x;
						ts.y = top ? height - ts.height : 0;
						x += ts.width;
						lstIdx = idx;
					}
				}
				lastVisible = findLastVisible(s, s.length-1);
				// check if gaps pushed any tabs off the available width
				width -= gaps_idx * gap;
				boolean endVisible = lastVisible == s.length-1;
				if (!endVisible)
					width -= gap;
				while (width < 0) {
					TabState ts = s[lastVisible];
					ts.visible = false;
					width += ts.width;
					if (--lastVisible < 0)
						break;
					ts = s[lastVisible];
					if (!ts.visible) {
						findLastVisible(s, lastVisible);
						gaps_idx--;
						width += gap;
					}
					if (endVisible) {
						width -= gap;
						endVisible = false;
					}
				}
				if (!endVisible) {
					gaps[gaps_idx++] = lastVisible;
				}
				gapIndices = Arrays.copyOf(gaps, gaps_idx);

				// rollover validation
				if (popupStates != null) {
					if (popupTabPane.getRolloverIndex() >= 0) {
						popupTabPane.mouseSupport.validate();
						return;
					}
				}
				tabPane.mouseSupport.validate();
			}
		}


		protected boolean paintingPopup;

		private int span;

		protected void paint(Graphics g, TabPlacement p, CellRendererPane rp, Tab tab,
				int i, boolean sel, int x, int y, int w, int h) {
			if (rp == popupRendererPane) {
				i += popupIndex;
				sel = false;
			}
			super.paint(g, p, rp, tab, i, sel, x, y, w, h);
		}



		public int getDropIndex(TabPane tp) {
//			if (tp == popupTabPane) TODO
			Point pt = tabPane.getDropPoint();
			if (pt.x < 0)
				return 0;
			int idx = getTabForLocation(tp, pt.x, pt.y);
			if (idx < 0) {
				if (idx == Integer.MIN_VALUE)
					return lastVisible;
				idx = convertGapIndexToFirstHiddenIndex(idx);
				idx = findFirstVisible(states, idx);
			}
			if (tp.getDraggedIndex() >= 0 && idx < lastVisible) {
				TabState drag = states[tabPane.getDraggedIndex()];
				switch (tabFolder.getTabPlacement()) {
				case TOP: case BOTTOM:
					if (pt.x > drag.x) {
						int x = pt.x + drag.width - states[idx].width;
						idx = getTabForLocation(tabPane, x, pt.y);
					}
					break;
				case LEFT: case RIGHT:
					if (pt.y > drag.y) {
						int y = pt.y + drag.height - states[idx].height;
						idx = getTabForLocation(tabPane, pt.x, y);
					}
					break;
				}
				if (idx < 0) {
					if (idx == Integer.MIN_VALUE)
						return lastVisible;
					idx = convertGapIndexToFirstHiddenIndex(idx);
					idx = findFirstVisible(states, idx);
				}
			}
//			if (idx > 0 && !states[idx-1].visible) {
//				int i = findLastVisible(states, idx-1)+1;
//				System.out.println(idx + " " + i);
//				if (popupIndex != i)
//					showPopup(i);
//			} else if (popup.isVisible()) {
//				popup.setVisible(false);
//				popupIndex = -1;
//			}
			return idx;
		}

		protected void paint(Graphics g, TabPlacement p, TabState[] s, int firstIndex, int lastIndex, int sel,
				CellRendererPane rp, boolean checkVisible, int offsetX, int offsetY, int dragIndex) {
			// TODO, this will be a task requiring a deal of coordination...
			// if drag is over a tab that borders a gap boundary,
			// then the gap moves based on where the current dragX or dragY is
			// also, if the drag hovers over a gap, then the popup for that
			// gap should appear per normal hover behavior

			if (rp == rendererPane && firstIndex <= lastIndex && gapIndices.length > 0) {
				int dropIndex = tabPane.getDropIndex();
				System.out.println("dropIndex: " + dropIndex);
				if (dragIndex >= 0 && dragIndex < dropIndex)
					dropIndex++;

				int[] gaps = gapIndices;
				int i = gaps.length;
				if (lastIndex == lastVisible && gaps[i-1] == lastIndex) {
					i--;
					TabState ts = s[lastIndex];
					switch (p) {
					case TOP: case BOTTOM:
						painter.paintEndGap(g, p, ts.x+ts.width+offsetX, false); break;
					case LEFT: case RIGHT:
						painter.paintEndGap(g, p, ts.y+ts.height+offsetY, false); break;
					}
				}
				while (--i>=0) {
					int idx = gaps[i];
					if (idx <= lastIndex) {
						if (idx < firstIndex) {
							if (idx < 0 && firstIndex == firstVisible) {
								switch (p) {
								case TOP: case BOTTOM:
									painter.paintEndGap(g, p, 0, true); break;
								case LEFT: case RIGHT:
									painter.paintEndGap(g, p, 0, true); break;
								}
							}

							break;
						}
						if (dropIndex >= 0) {
							int firstGap = idx + 1;
							int lastGap = firstGap;
							for (; lastGap<s.length; lastGap++) {
								if (s[lastGap].visible)
									break;
							}
							lastGap--;
							if (dropIndex == firstGap || dropIndex == lastGap+1) {
								int dropX = getDropX(tabPane);
								int gapX = s[idx].x + s[idx].width + offsetX;
								if (dropX < gapX + getMRULayoutGap()/2) {
									gapX += tabPane.getDropTabState().width;
								}
								painter.paintGap(g, p, gapX);
								continue;
							}
						}
						painter.paintGap(g, p, s[idx].x+s[idx].width+offsetX);
					}

				}
			}
			super.paint(g, p, s, firstIndex, lastIndex, sel, rp, checkVisible, offsetX, offsetY, dragIndex);
		}

		@Override
		public void paint(Graphics g, JComponent c) {
			if (c == popupTabPane) {
				paintingPopup = true;
				paint(g, popupTabPane, popupStates, 0, popupStates.length-1, -1, popupRendererPane, false);
			} else {
				paintingPopup = false;
				paint(g, tabPane, states, firstVisible, lastVisible, selectedIndex, rendererPane, true);
			}
		}
	}

	private static int doLayout(Component c, TabPlacement p, int baseInset, int pos, int apex, boolean leftEdge) {
		Dimension size = c.getPreferredSize();
		switch (p) {
		case TOP:
			if (!leftEdge)
				pos -= size.width;
			c.setBounds(pos, apex-size.height-baseInset, size.width, size.height);
			if (leftEdge)
				pos += size.width;
			break;
		case BOTTOM:
			if (!leftEdge)
				pos -= size.width;
			c.setBounds(pos, baseInset, size.width, size.height);
			if (leftEdge)
				pos += size.width;
			break;
		case LEFT:
			if (!leftEdge)
				pos -= size.height;
			c.setBounds(apex-size.width-baseInset, pos, size.width, size.height);
			if (leftEdge)
				pos += size.height;
			break;
		case RIGHT:
			if (!leftEdge)
				pos -= size.height;
			c.setBounds(baseInset, pos, size.width, size.height);
			if (leftEdge)
				pos += size.height;
			break;
		}
		return pos;
	}



	class Layout implements LayoutManager {

		@Override
		public void addLayoutComponent(String name, Component comp) {}
		@Override
		public void removeLayoutComponent(Component comp) {}

		@Override
		public void layoutContainer(Container parent) {
			maybeUpdateSizeCache();
			int width = parent.getWidth();
			int height = parent.getHeight();
			// layout tab area
			TabPlacement placement = tabFolder.getTabPlacement();
			Insets insets = getTabAreaInsets();
			int apex;
			boolean ltr = true;
			switch (placement) {
			case TOP: case BOTTOM:
				tabRegionBounds.x = 0;
				tabRegionBounds.width = width;
				if (height >= prefSize.height) {
					tabRegionBounds.height = tabRegionPrefSize.height;
				} else {
					int prefContentHeight = prefSize.height - tabRegionPrefSize.height;
					if (prefContentHeight + tabRegionMinSize.height <= height) {
						tabRegionBounds.height = height - prefContentHeight;
					} else {
						tabRegionBounds.height = tabRegionMinSize.height;
					}
				}
				ltr = tabFolder.getComponentOrientation().isLeftToRight();
				apex = tabRegionBounds.height;
				break;
			case LEFT: case RIGHT:
				tabRegionBounds.y = 0;
				tabRegionBounds.height = height;
				if (width >= prefSize.width) {
					tabRegionBounds.width = tabRegionPrefSize.width;
				} else {
					int prefContentWidth = prefSize.width - tabRegionPrefSize.width;
					if (prefContentWidth + tabRegionMinSize.width <= width) {
						tabRegionBounds.width = width - prefContentWidth;
					} else {
						tabRegionBounds.width = tabRegionMinSize.width;
					}
				}
				apex = tabRegionBounds.width;
				break;
			default: case NONE:
				setVisibleComponentBounds(0, 0, width, height);
				return;
			}
			int baseInset = insets.bottom;
			int startPos, endPos;
			int pos = 0;
			if (ltr) {
				if (leadingCornerComponent != null && leadingCornerComponent.isVisible())
					pos = BasicTabFolderUI.doLayout(
							leadingCornerComponent, placement, baseInset, pos, apex, true);
				startPos = pos += insets.left;
				pos = placement == TabPlacement.TOP || placement == TabPlacement.BOTTOM ? width : height;
				if (trailingCornerComponent != null && trailingCornerComponent.isVisible())
					pos = BasicTabFolderUI.doLayout(
							trailingCornerComponent, placement, baseInset, pos, apex, false);
				endPos = pos -= insets.right;
			} else {
				if (trailingCornerComponent != null && trailingCornerComponent.isVisible())
					pos = BasicTabFolderUI.doLayout(
							trailingCornerComponent, placement, baseInset, pos, apex, true);
				endPos = pos += insets.left;
				pos = width;
				if (leadingCornerComponent != null && leadingCornerComponent.isVisible())
					pos = BasicTabFolderUI.doLayout(
							leadingCornerComponent, placement, baseInset, pos, apex, false);
				startPos = pos -= insets.right;
			}

			switch (placement) {
			case TOP:
				tabRegionBounds.y = 0;
				setVisibleComponentBounds(0, tabRegionBounds.height, width, height - tabRegionBounds.height);
				break;
			case BOTTOM:
				tabRegionBounds.y = height - tabRegionBounds.height;
				setVisibleComponentBounds(0, 0, width, height - tabRegionBounds.height);
				break;
			case LEFT:
				tabRegionBounds.x = 0;
				setVisibleComponentBounds(0, tabRegionBounds.width, width - tabRegionBounds.width, height);
				break;
			case RIGHT:
				tabRegionBounds.x = width - tabRegionBounds.width;
				setVisibleComponentBounds(0, 0, width - tabRegionBounds.width, height);
				break;
			}
			tabLayoutState.layoutTabs(startPos, endPos);

		}


		private void setVisibleComponentBounds(int x, int y, int w, int h) {
			if (w != componentBounds.width
					|| h != componentBounds.height
					|| x != componentBounds.x
					|| y != componentBounds.y) {
				componentBounds.setBounds(x, y, w, h);
				if (visibleComponent != null)
					visibleComponent.setBounds(x, y, w, h);
			}
		}

		@Override
		public Dimension minimumLayoutSize(Container parent) {
			maybeUpdateSizeCache();
			return new Dimension(minSize);
		}

		@Override
		public Dimension preferredLayoutSize(Container parent) {
			maybeUpdateSizeCache();
			return new Dimension(prefSize);
		}

	}

	protected class Renderer extends AbstractTabRenderer implements UIResource {

		protected Renderer(Dimension size) {
			this(size, size);
		}

		protected Renderer(Dimension preferredTabSize, Dimension minimumTabSize) {
			super(preferredTabSize, minimumTabSize);
			tabRendererComponent = createTabRendererComponent();
			tabRolloverComponent = createTabRolloverComponent();

		}

		TabComponent tabRendererComponent;

		TabComponent tabRolloverComponent;


		public Component getTabRendererComponent(TabFolder tabFolder, Tab tab, int index, boolean isSelected) {
			return tabRendererComponent.getTabComponent(tabFolder, tab, index, isSelected);
		}

		public Component getTabRolloverComponent(TabFolder tabFolder, Tab tab, int index, boolean isSelected) {
			return tabRolloverComponent == null ? null :
				tabRolloverComponent.getTabComponent(tabFolder, tab, index, isSelected);
		}


	}

	protected AbstractPainter createPainter() {
		return new Painter();
	}

	protected TabComponent createTabRolloverComponent() {
		JButton close = new JButton(painter.getCloseIcon());
		close.addActionListener(handler);
		close.setContentAreaFilled(false);
		close.setBorderPainted(false);
		close.setMargin(new Insets(0, 0, 0, 0));
		close.setBorder(null);
		return new TabComponent(painter, close);
	}

	protected TabComponent createTabRendererComponent() {
		return new TabComponent(painter, null);
	}

	protected AbstractButton createScrollButton(int direction) {
		return new ArrowButton(direction);
	}

	protected AbstractButton createDropDownButton() {
		ArrowButton button = new ArrowButton(SwingConstants.SOUTH);
		button.addActionListener(handler);
		return button;
	}

	static class ArrowButton extends JButton {

		private static final GeneralPath arrow;

		static {
			arrow = new GeneralPath();
			arrow.moveTo(-3.5, -2);
			arrow.lineTo(3.5, -2);
			arrow.lineTo(0, 2);
			arrow.closePath();
		}

		ArrowButton(int dir) {
			switch (dir) {
			case NORTH: theta = Math.PI; break;
			case WEST: theta = Math.PI/2; break;
			case SOUTH: theta = 0.0; break;
			case EAST: theta = -Math.PI/2; break;
			}
			foreground = UIManager.getColor("controlText");
			highlight = UIManager.getColor("nimbusBase");
			if (highlight == null)
				highlight = UIManager.getColor("activeCaptionBorder");
		}

		private double theta;
		private Color foreground;
		private Color highlight;

		public void paint(Graphics g) {
			paint((Graphics2D)g);
		}

		private void paint(Graphics2D g) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			if (theta != 0.0)
				arrow.transform(AffineTransform.getRotateInstance(theta));
			g.translate(getWidth()/2, getHeight()/2);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			if (getModel().isPressed()) {
				arrow.transform(AffineTransform.getTranslateInstance(1.0, 1.0));
				paintArrow(g, highlight);
				arrow.transform(AffineTransform.getTranslateInstance(-1.0, -1.0));
			} else if (getModel().isRollover()) {
				paintArrow(g, highlight);
			} else {
				paintArrow(g, foreground);
			}
			if (theta != 0.0)
				arrow.transform(AffineTransform.getRotateInstance(-theta));
		}

		private void paintArrow(Graphics2D g, Color c) {
			g.setColor(c);
			g.fill(arrow);
		}

		public Dimension getPreferredSize() {
			return new Dimension(16, 16);
		}

	}

	protected Insets getTabAreaInsets() {
		return painter.getTabAreaInsets();
	}

	private static float RATIO = (float)((1 + Math.sqrt(5.0))/2);


	protected int getMRULayoutGap() {
		return 5;
	}

	protected int getMRULayoutGapRise() {
		return 1;
	}


	protected static abstract class AbstractPainter {
		public abstract void paint(Graphics g, TabFolder tabFolder);
		public abstract void paint(Graphics g, TabPane tabPane);
		public abstract void paint(Graphics g, TabComponent tabComponent);
		public abstract Dimension getPreferredSize(TabComponent tabComponent);
		public abstract Insets getTabAreaInsets();

		public abstract void paintGap(Graphics g, TabPlacement p, int pos);
		public abstract void paintEndGap(Graphics g, TabPlacement p, int pos, boolean leading);
		public abstract void paintTabBackground(Graphics g, int index, TabPlacement p, boolean sel,
				int x, int y, int w, int h);

		public int getTabOverlap() {
			return 0;
		}

		public abstract Icon getCloseIcon();
	}


	protected static class TabComponent extends Container {

		protected TabComponent(AbstractPainter painter, AbstractButton close) {
			tabPainter = painter;
			if (close != null) {
				closeButton = close;
				close.setSize(close.getPreferredSize());
				add(close);
			}
		}

		protected AbstractPainter tabPainter;

		protected AbstractButton closeButton;

		protected TabFolder tabFolder;

		protected Tab tab;

		protected int index;

		protected boolean isSelected;

		protected Component getTabComponent(
				TabFolder tabFolder, Tab tab, int index, boolean isSelected) {
			this.tabFolder = tabFolder;
			this.tab = tab;
			this.index = index;
			this.isSelected = isSelected;
			return this;
		}

		public void paint(Graphics g) {
			tabPainter.paint(g, this);
			super.paint(g);
		}

		public Dimension getPreferredSize() {
			return tabPainter.getPreferredSize(this);
		}

		public Tab getTab() {
			return tab;
		}

		public TabFolder getTabFolder() {
			return tabFolder;
		}

		public int getIndex() {
			return index;
		}

		public boolean isSelected() {
			return isSelected;
		}

	}




	protected class Painter extends AbstractPainter {
		protected Painter() {

			tabAreaBackground = UIManager.getColor("TabbedPane.tabAreaBackground");

			borderBase = UIManager.getColor("nimbusBase");
			if (borderBase == null)
				borderBase = UIManager.getColor("TabbedPane.borderHighlightColor");
			if (borderBase == null)
				borderBase = UIManager.getColor("TabbedPane.darkShadow");

			borderApex = UIManager.getColor("TabbedPane.borderHighlightColor");
			if (borderApex == null)
				borderApex = UIManager.getColor("TabbedPane.shadow");

			shadow = UIManager.getColor("nimbusBlueGrey");
			if (shadow == null)
				shadow = UIManager.getColor("TabbedPane.shadow");

			backgroundBase = UIManager.getColor("nimbusSelectionBackground");
			if (backgroundBase != null) {
				backgroundBase = new Color(
						backgroundBase.getRed()+(255-backgroundBase.getRed())*2/3,
						backgroundBase.getGreen()+(255-backgroundBase.getGreen())*2/3,
						backgroundBase.getBlue()+(255-backgroundBase.getBlue())*3/4);
			} else {
				backgroundBase = UIManager.getColor("TabbedPane.contentAreaColor");
				if (backgroundBase == null)
					backgroundBase = UIManager.getColor("TabbedPane.background");
			}

			backgroundApex = UIManager.getColor("TabbedPane.highlight");

			contentOpaque = UIManager.getBoolean("TabbedPane.contentOpaque");
			textIconGap = UIManager.getInt("TabbedPane.textIconGap");

			tabAreaInsets = new Insets(getMRULayoutGapRise()+1, 0, 0, 0);
			tabInsets = new Insets(2, 8, 1, 8);


		}

		protected Color tabAreaBackground;

		protected Color borderBase;

		protected Color borderApex;

		protected Color backgroundBase;

		protected Color backgroundApex;

		protected Insets tabAreaInsets;

		protected Insets tabInsets;

		boolean tabsOverlapBorder = true;

		boolean contentOpaque;

		protected int textIconGap;

		protected Color shadow;


		public Insets getTabAreaInsets() {
			return tabAreaInsets;
		}

		public int getTabOverlap() {
			return 1;
		}

		public void paint(Graphics g, TabComponent tabComponent) {
			paintTabContents((Graphics2D)g, tabComponent);
		}

		public void paint(Graphics g, TabPane tabPane) {
			// the idea was to put an extended background behind the selected
			// tab, but it doesn't work based on how the current repaint bounds
			// are sent and also it doesn't look that great as implemented below

			//			if (selectedIndex >= 0) {
			//				Rectangle r = tabLayoutState.getTabBounds(tabPane, selectedIndex);
			//				if (g.getClipBounds().contains(r)) {
			//					Graphics2D g2 = (Graphics2D)g.create();
			//					switch (tabFolder.getTabPlacement()) {
			//					case TOP:
			//						GeneralPath path = new GeneralPath();
			//						path.moveTo(r.x, r.y+r.height);
			//						path.lineTo(r.x, r.y+3);
			//						path.quadTo(r.x, r.y-1, r.x+8, r.y-1);
			//						path.lineTo(r.x+r.width-8, r.y-1);
			//						path.quadTo(r.x+r.width, r.y-1, r.x+r.width, r.y+3);
			//						path.lineTo(r.x+r.width, r.y+r.height);
			//						path.closePath();
			//						g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			//						g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			//						System.out.println("selectedTabExtended " + r);
			//						g2.setColor(borderBase);
			//						g2.fill(path);
			//						break;
			//					}
			//				}
			//			}


			TabPlacement p = tabFolder.getTabPlacement();
			int w = tabPane.getWidth();
			int h = tabPane.getHeight();
			switch (p) {
			case TOP: paintContentBottomBorder(g, p, 0, 0, w, h); break;
			case BOTTOM: paintContentTopBorder(g, p, 0, 0, w, h); break;
			case LEFT: paintContentRightBorder(g, p, 0, 0, w, h); break;
			case RIGHT: paintContentLeftBorder(g, p, 0, 0, w, h); break;
			}
		}

		public void paintTabBackground(Graphics graphics, int index, TabPlacement p, 
				boolean sel, int x, int y, int w, int h) {
			Graphics2D g = (Graphics2D)graphics.create();
			int pressedIndex = tabLayoutState.getPressedIndex();
			boolean rollover = index >= 0 && tabLayoutState.getRolloverIndex() == index;
			boolean pressed = index >= 0 && pressedIndex == index;
			if (!sel && !pressed && index >= 0 && (selectedIndex >= 0 || pressedIndex >= 0)
					&& !tabLayoutState.isPaintingPopup()) {
				// this code block ensures that selected and pressed tabs are not
				// painted over from tab overlapping
				int draggedIndex = tabLayoutState.getTabPane().draggedIndex;
				switch (p) {
				case TOP: case BOTTOM:
					if (tabFolder.getComponentOrientation().isLeftToRight()) {
						if ((index == selectedIndex+1 || index == pressedIndex+1)
								&& (draggedIndex < 0 || index != draggedIndex+1)) {
							Rectangle r = g.getClipBounds();
							if (r.x < x+getTabOverlap()) {
								r.width -= getTabOverlap() - r.x;
								r.x = x + getTabOverlap();
								g.setClip(r.x, r.y, r.width, r.height);
							}
//						} else if ((index == selectedIndex-1) || (index == pressedIndex-1)) {
//							Rectangle r = g.getClipBounds();
//							if (r.x+r.width > x + w - getTabOverlap()) {
//								r.width = w - r.x - getTabOverlap();
//								g.setClip(r.x, r.y, r.width, r.height);
//							}
						}
					} else {
						// TODO
					} break;
				case LEFT: case RIGHT:
					// TODO
					break;
				}
			}
			if (tabLayoutState.isPaintingPopup()) {
				// invert for popup
				switch (p) {
				case TOP: p = TabPlacement.BOTTOM; break;
				case BOTTOM: p = TabPlacement.TOP; break;
				case LEFT: p = TabPlacement.RIGHT; break;
				case RIGHT: p = TabPlacement.LEFT; break;
				}
			}
			Paint back, border;
			GeneralPath path;
			switch (p) {
			case TOP:
				if (pressed) {
					back = new GradientPaint(x, y+1, backgroundApex, x, y+h, backgroundBase);
				} else if (rollover) {
					back = sel ?
							new GradientPaint(x, y+1, backgroundApex, x, y+h, backgroundBase) :
								new GradientPaint(x, y+1, backgroundApex, x, y+h*3, shadow);
				} else {
					back = sel ?
							new GradientPaint(x, y+1, backgroundApex, x, y+h/RATIO, backgroundBase) :
								new GradientPaint(x, y+1, backgroundApex, x, y+h*RATIO, shadow);
				}
				border = sel || pressed ?
						borderBase :
							new GradientPaint(x, y, shadow, x, y+h-3, borderBase);
				x += 1;
				w -= 2;
				y += 1;
				h -= sel ? 1 : 2;
				path = new GeneralPath();
				path.moveTo(x, y+h);
				path.lineTo(x, y+4);
				path.quadTo(x, y, x+8, y);
				path.lineTo(x+w-8, y);
				path.quadTo(x+w, y, x+w, y+4);
				path.lineTo(x+w, y+h);
				break;
			case BOTTOM:
				if (pressed) {
					back = new GradientPaint(x, y+1, backgroundApex, x, y+h, backgroundBase);
				} else if (rollover) {
					back = sel ?
							new GradientPaint(x, y+1, backgroundApex, x, y+h, backgroundBase) :
								new GradientPaint(x, y+1, backgroundApex, x, y+h*3, shadow);
				} else {
					back = sel ?
							new GradientPaint(x, y+1, backgroundApex, x, y+h/RATIO, backgroundBase) :
								new GradientPaint(x, y+1, backgroundApex, x, y+h*RATIO, shadow);
				}
				border = borderBase;
				x += 1;
				w -= 2;
				y += sel ? 0 : 1;
				h -= sel ? 1 : 2;
				path = new GeneralPath();
				path.moveTo(x, y);
				path.lineTo(x, y+h-4);
				path.quadTo(x, y+h, x+8, y+h);
				path.lineTo(x+w-8, y+h);
				path.quadTo(x+w, y+h, x+w, y+h-4);
				path.lineTo(x+w, y);
				break;
			case LEFT:
				// TODO
//				break;
			case RIGHT:
				// TODO
//				break;
			default: return;
			}
			if (!sel)
				path.closePath();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setPaint(border);
			g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
			g.draw(path);
			g.setPaint(back);
			g.fill(path);
		}

		protected void paintTabContents(Graphics g, TabComponent tabComponent) {
			Tab tab = tabComponent.getTab();
			TabFolder tabFolder = tabComponent.getTabFolder();
			String title = tab.getTitle();
			Icon icon = tab.getIcon();
			int width = tabComponent.getWidth();
			int height = tabComponent.getHeight();
			boolean ltr = tabFolder.getComponentOrientation().isLeftToRight();
			int x = ltr ? tabInsets.left + 1 : width - tabInsets.right - 1;
			int y = tabInsets.top + 1;
			int index = tabComponent.getIndex();
			boolean pressed = index >= 0 && tabLayoutState.getPressedIndex() == index;
			if (pressed) {
				//				y += 1;
			}
			if (icon != null) {
				if (!ltr)
					x -= icon.getIconWidth();
				icon.paintIcon(tabComponent, g, x, y + height - icon.getIconHeight());
				if (ltr) {
					x += icon.getIconWidth() + textIconGap;
				} else {
					x -= textIconGap;
				}
			}
			if (title != null && !title.isEmpty()) {
				Font font = tabComponent.getFont();
				FontMetrics metrics = tabComponent.getFontMetrics(font);
				g.setColor(tabComponent.getForeground());
				int w = metrics.stringWidth(title);
				if (!ltr)
					x -= w;
				g.drawString(title, x, y + metrics.getAscent());
				if (ltr)
					x += w;
			}

			if (tab.isCloseable() && tabModel instanceof MutableTabModel) {
				Icon closeIcon = getCloseIcon();
				int tabH = tabComponent.getHeight() - tabInsets.top - tabInsets.bottom - 1;
				int h = closeIcon.getIconHeight();
				int closeY = tabInsets.top + (tabH-h)/2;
				AbstractButton b = tabComponent.closeButton;
				if (b == null) {
					closeIcon.paintIcon(tabComponent, g, x+4, closeY);
				} else {
					b.setLocation(x+4, closeY);
					tabComponent.closeButton.setVisible(true);
				}
			} else if (tabComponent.closeButton != null) {
				tabComponent.closeButton.setVisible(false);
			}
		}



		public void paint(Graphics g, TabFolder tabFolder) {
			TabPlacement p = tabFolder.getTabPlacement();

			int width = tabFolder.getWidth();
			int height = tabFolder.getHeight();

			if (tabFolder.isOpaque()) {
				g.setColor(tabAreaBackground);
				g.fillRect(0, 0, width, height);
			}

			Insets insets = tabFolder.getInsets();

			int x = insets.left;
			int y = insets.top;
			int w = width - insets.right - insets.left;
			int h = height - insets.top - insets.bottom;

			switch(p) {
			case TOP:
				y += tabRegionBounds.height;
				if (tabsOverlapBorder)
					y -= tabAreaInsets.bottom;
				h -= (y - insets.top);
				break;
			case BOTTOM: 
				h -= tabRegionBounds.height;
				if (tabsOverlapBorder)
					h += tabAreaInsets.top;
				break;
			case LEFT:
				x += tabRegionBounds.width;
				if (tabsOverlapBorder)
					x -= tabAreaInsets.right;
				w -= (x - insets.left);
				break;
			case RIGHT:
				w -= tabRegionBounds.width;
				if (tabsOverlapBorder)
					w += tabAreaInsets.left;
				break;
			} 
			paintContentBackground(g, tabFolder, x, y, w, h);
			paintContentTopBorder(g, p, x, y, w, h);
			paintContentLeftBorder(g, p, x, y, w, h);
			paintContentBottomBorder(g, p, x, y, w, h);
			paintContentRightBorder(g, p, x, y, w, h);

		}


		protected void paintContentBackground(Graphics g, TabFolder tabFolder, int x, int y, int w, int h) {
			if (tabModel.getTabCount() > 0 && (contentOpaque || tabFolder.isOpaque()) ) {
				g.setColor(backgroundBase);
				g.fillRect(x,y,w,h);
			}
		}

		protected Color getContentBorderColor() {
			return borderBase; 
		}

		protected void paintContentTopBorder(Graphics g, TabPlacement p, int x, int y, int w, int h) {
			if (p == TabPlacement.TOP)
				y -= 1;
			g.setColor(getContentBorderColor());
			g.drawLine(x, y, x+w, y);
		}

		protected void paintContentBottomBorder(Graphics g, TabPlacement p, int x, int y, int w, int h) {
			if (p == TabPlacement.BOTTOM)
				y += 1;
			g.setColor(getContentBorderColor());
			g.drawLine(x, y+h-1, x+w-1, y+h-1);
		}

		protected void paintContentLeftBorder(Graphics g, TabPlacement p, int x, int y, int w, int h) {
			if (p == TabPlacement.LEFT)
				x -= 1;
			g.setColor(getContentBorderColor());
			g.drawLine(x, y, x, y+h-1);
		}

		protected void paintContentRightBorder(Graphics g, TabPlacement p, int x, int y, int w, int h) {
			if (p == TabPlacement.RIGHT)
				x += 1;
			g.setColor(getContentBorderColor());
			g.drawLine(x+w-1, y, x+w-1, y+h-1);
		}

		// TODO or delete
		protected void paintTabFocus(Graphics g, TabComponent tabComponent) {

		}

		public void paintGap(Graphics g, TabPlacement p, int pos) {
			int rise = getMRULayoutGapRise();
			int gap = getMRULayoutGap();
			switch (p) {
			case TOP:
				paintTabBackground(g, -1, p, false, pos-gap,
						tabRegionBounds.height-tabApex-rise, gap*3, tabApex+rise);
				break;
			case BOTTOM:
				paintTabBackground(g, -1, p, false, pos-gap,
						tabAreaInsets.bottom, gap*3, tabApex+rise);
				break;
			case LEFT:
				// TODO
				break;
			case RIGHT:
				// TODO
				break;
			}
		}

		public void paintEndGap(Graphics g, TabPlacement p, int pos, boolean leading) {
			int rise = getMRULayoutGapRise();
			int gap = getMRULayoutGap();
			switch (p) {
			case TOP:
				paintTabBackground(g, -1, p, false, pos - (leading ? 0 : gap*2),
						tabRegionBounds.height-tabApex-rise, gap*3, tabApex+rise);
				break;
			case BOTTOM:
				paintTabBackground(g, -1, p, false, pos - (leading ? 0 : gap*2),
						tabAreaInsets.bottom, gap*3, tabApex+rise);
				break;
			case LEFT:
				// TODO
				break;
			case RIGHT:
				// TODO
				break;
			}
		}

		public Dimension getPreferredSize(TabComponent tabComponent) {
			int width = tabInsets.left + tabInsets.right + 3;
			int height = 0;
			Tab tab = tabComponent.getTab();
			String title = tab.getTitle();
			Icon icon = tab.getIcon();
			if (icon != null) {
				width += icon.getIconWidth() + textIconGap;
				height = icon.getIconHeight();
			}
			if (title != null && !title.isEmpty()) {
				Font font = tabFolder.getFont();
				FontMetrics metrics = tabFolder.getFontMetrics(font);
				int w = metrics.stringWidth(title);
				int h = metrics.getAscent() + metrics.getDescent();
				width += w;
				if (h > height)
					height = h;
			}
			if (tab.isCloseable() && tabModel instanceof MutableTabModel) {
				width += 16;
			}
			height += tabInsets.top + tabInsets.bottom + 3;
			return new Dimension(width, height);
		}

		public Icon getCloseIcon() {
			return closeIcon;
		}

		Icon closeIcon = new CloseIcon();

		class CloseIcon implements Icon {

			CloseIcon() {
				float w = getIconWidth();
				float h = getIconHeight();
				xPath = new GeneralPath();
				xPath.moveTo(3, 3);
				xPath.lineTo(w-3, h-3);
				xPath.moveTo(3, h-3);
				xPath.lineTo(w-3, 3);
			}

			private final GeneralPath xPath;

			@Override
			public int getIconHeight() {
				return 12;
			}

			@Override
			public int getIconWidth() {
				return 12;
			}

			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				Color center = backgroundApex;
				Color border = borderBase;
				float strokeWidth = 2.5f;
				if (c instanceof AbstractButton) {
					ButtonModel bm = ((AbstractButton)c).getModel();
					if (bm.isPressed()) {
						strokeWidth = 0.75f;
					} else if (bm.isRollover()) {
						strokeWidth = 1.5f;
					}
				}

				Graphics2D g2 = (Graphics2D)g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
				g2.setColor(border);
				g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g2.translate(x, y);
				g2.draw(xPath);
				g2.setColor(center);
				g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g2.draw(xPath);
				g2.translate(-x, -y);
			}
		}

	}

}



