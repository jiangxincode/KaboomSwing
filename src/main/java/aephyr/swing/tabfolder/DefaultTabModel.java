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
package aephyr.swing.tabfolder;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

public class DefaultTabModel implements MutableTabModel {

	protected EventListenerList listenerList = new EventListenerList();
	
	protected PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
	
	List<DefaultTab> tabs = new ArrayList<DefaultTab>();
	
	int selectedIndex = -1;
	
	public void addListDataListener(ListDataListener l) {
		listenerList.add(ListDataListener.class, l);
	}
	public void removeListDataListener(ListDataListener l) {
		listenerList.remove(ListDataListener.class, l);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener l) {
		changeSupport.addPropertyChangeListener(l);
	}
	public void removePropertyChangeListener(PropertyChangeListener l) {
		changeSupport.removePropertyChangeListener(l);
	}
	
	public Tab getTabAt(int index) {
		return tabs.get(index);
	}
	
	public int getTabCount() {
		return tabs.size();
	}
	
	public void setSelectedIndex(int index) {
		if (index != selectedIndex) {
			int oldValue = selectedIndex;
			selectedIndex = index;
			changeSupport.firePropertyChange("selectedIndex", oldValue, index);
		}
	}
	
	public int getSelectionIndex() {
		return selectedIndex;
	}
	
	public void addTab(String title, Component component) {
		addTab(title, null, component, null);
	}
	
	public void addTab(String title, Icon icon, Component component) {
		addTab(title, icon, component, null);
	}
	
	public void addTab(String title, Icon icon, Component component, String tip) {
		addTab(new DefaultTab(title, icon, component, tip));
	}
	
	public void insertTab(String title, Icon icon, Component component, String tip, int index) {
		insertTab(new DefaultTab(title, icon, component, tip), index);
	}
	
	public void addTab(DefaultTab tab) {
		int index = tabs.size();
		tabs.add(tab);
		fireListDataEvent(ListDataEvent.INTERVAL_ADDED, index, index);
	}
	
	public boolean canInsert(Tab tab) {
		return tab instanceof DefaultTab;
	}
	
	public boolean insertTab(Tab tab, int index) {
		if (tab instanceof DefaultTab) {
			insertTab((DefaultTab)tab, index);
			return true;
		}
		return false;
	}
	
	public void insertTab(DefaultTab tab, int index) {
		if (index < 0 || index > tabs.size())
			index = tabs.size();
		tabs.add(index, tab);
		fireListDataEvent(ListDataEvent.INTERVAL_ADDED, index, index);
		if (index <= selectedIndex)
			setSelectedIndex(selectedIndex+1);
	}
	
	public void removeTab(DefaultTab tab) {
		int index = tabs.indexOf(tab);
		if (index >= 0)
			removeTab(index);
	}
	
	public void removeTab(int index) {
		tabs.remove(index);
		int selected = selectedIndex;
		fireListDataEvent(ListDataEvent.INTERVAL_REMOVED, index, index);
		if (index < selectedIndex && selected == selectedIndex)
			setSelectedIndex(selected-1);
	}

//	public void insertTabs(Collection<DefaultTab> tabs, int index) {
//		if (tabs.size() > 0) {
//			this.tabs.addAll(index, tabs);
//			fireListDataEvent(ListDataEvent.INTERVAL_ADDED, index, index+tabs.size()-1);
//		}
//	}
//	
//	public void addTabs(Collection<DefaultTab> tabs) {
//		if (tabs.size() > 0) {
//			int index = tabs.size();
//			this.tabs.addAll(tabs);
//			fireListDataEvent(ListDataEvent.INTERVAL_ADDED, index, index+tabs.size()-1);
//		}
//	}
	
	
	protected void fireListDataEvent(int type, int index0, int index1) {
		Object[] listeners = listenerList.getListenerList();
		ListDataEvent e = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==ListDataListener.class) {
				if (e == null)
					e = new ListDataEvent(this, type, index0, index1);
				switch (type) {
				case ListDataEvent.CONTENTS_CHANGED:
					((ListDataListener)listeners[i+1]).contentsChanged(e);
					break;
				case ListDataEvent.INTERVAL_ADDED:
					((ListDataListener)listeners[i+1]).intervalAdded(e);
					break;
				case ListDataEvent.INTERVAL_REMOVED:
					((ListDataListener)listeners[i+1]).intervalRemoved(e);
					break;
				}
			}
		}
	}
	
	
	public void setComponentAt(int index, Component component) {
		DefaultTab tab = tabs.get(index);
		Object oldValue = tab.getComponent();
		tab.setComponent(component);
		fireListDataEvent(ListDataEvent.CONTENTS_CHANGED, index, index);
		changeSupport.fireIndexedPropertyChange("component", index, oldValue, component);
	}
	
	public void setIconAt(int index, Icon icon) {
		DefaultTab tab = tabs.get(index);
		Object oldValue = tab.getIcon();
		tab.setIcon(icon);
		fireListDataEvent(ListDataEvent.CONTENTS_CHANGED, index, index);
		changeSupport.fireIndexedPropertyChange("icon", index, oldValue, icon);
	}
	
	public void setTitleAt(int index, String title) {
		DefaultTab tab = tabs.get(index);
		Object oldValue = tab.getTitle();
		tab.setTitle(title);
		fireListDataEvent(ListDataEvent.CONTENTS_CHANGED, index, index);
		changeSupport.fireIndexedPropertyChange("title", index, oldValue, title);
	}
	
	public void setToolTipTextAt(int index, String tip) {
		DefaultTab tab = tabs.get(index);
		Object oldValue = tab.getToolTipText();
		tab.setToolTipText(tip);
		fireListDataEvent(ListDataEvent.CONTENTS_CHANGED, index, index);
		changeSupport.fireIndexedPropertyChange("toolTipText", index, oldValue, tip);
	}
}
