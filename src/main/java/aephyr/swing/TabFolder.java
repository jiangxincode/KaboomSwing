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

import aephyr.swing.tabfolder.*;
import aephyr.swing.ui.BasicTabFolderUI;
import aephyr.swing.ui.TabFolderUI;

import java.awt.*;
import javax.swing.*;

public class TabFolder extends JComponent {
	
	public TabFolder(TabModel model) {
		setModel(model);
		updateUI();
	}
	
	private TabLayoutPolicy tabLayoutPolicy;
	
	private TabPlacement tabPlacement;
	
	private TabRenderer tabRenderer;
	
	private TabModel tabModel;
	
	private boolean dragEnabled;
	
	public enum TabLayoutPolicy {
		WRAP,
		HORIZONTAL_SCROLL,
		VERTICAL_SCROLL,
		MRU
	}
	
	public enum TabPlacement {
		TOP,
		BOTTOM,
		LEFT,
		RIGHT,
		NONE
	}
	
	public void setTabPlacement(TabPlacement placement) {
		if (placement == null)
			throw new NullPointerException();
		TabPlacement oldValue = tabPlacement;
		tabPlacement = placement;
		firePropertyChange("tabPlacement", oldValue, placement);
		revalidate();
		repaint();
	}
	
	public TabPlacement getTabPlacement() {
		return tabPlacement;
	}
	
	public void setTabLayoutPolicy(TabLayoutPolicy policy) {
		if (policy == null)
			throw new NullPointerException();
		TabLayoutPolicy oldValue = tabLayoutPolicy;
		tabLayoutPolicy = policy;
		firePropertyChange("tabLayoutPolicy", oldValue, policy);
		revalidate();
		repaint();
	}
	
	public TabLayoutPolicy getTabLayoutPolicy() {
		return tabLayoutPolicy;
	}
	
	public void setTabRenderer(TabRenderer renderer) {
//		if (renderer == null)
//			throw new NullPointerException();
		TabRenderer oldValue = tabRenderer;
		tabRenderer = renderer;
		firePropertyChange("tabRenderer", oldValue, renderer);
	}
	
	public TabRenderer getTabRenderer() {
		return tabRenderer;
	}
	
	
	public void setLeadingCornerComponent(Component c) {
		putClientProperty("leadingCornerComponent", c);
		revalidate();
		repaint();
	}
	
	public Component getLeadingCornerComponent() {
		return (Component)getClientProperty("leadingCornerComponent");
	}
	
	public void setTrailingCornerComponent(Component c) {
		putClientProperty("trailingCornerComponent", c);
		revalidate();
		repaint();
	}
	
	public Component getTrailingCornerComponent() {
		return (Component)getClientProperty("trailingCornerComponent");
	}
	
	public void setModel(TabModel model) {
		if (model == null)
			throw new NullPointerException();
		if (model.getSelectionIndex() < 0 && model.getTabCount() > 0)
			model.setSelectedIndex(0);
		TabModel oldValue = tabModel;
		tabModel = model;
		firePropertyChange("model", oldValue, model);
	}
	
	public TabModel getModel() {
		return tabModel;
	}

	public Rectangle getBoundsForTab(int index) {
		if (ui != null)
			return getUI().getTabBounds(this, index);
		return null;
	}
	
	public int getTabForLocation(int x, int y) {
		if (ui != null)
			return getUI().getTabForLocation(this, x, y);
		return -1;
	}
	
	public void setDragEnabled(boolean enabled) {
		dragEnabled = enabled;
	}
	
	public boolean isDragEnabled() {
		return dragEnabled;
	}
	
	public int getPressedIndex() {
		return ui != null ? getUI().getPressedIndex(this) : -1;
	}
	
	public int getRolloverIndex() {
		return ui != null ? getUI().getRolloverIndex(this) : -1;
	}
	
	// UI interface

	private static final String uiClassID = "TabFolderUI";

    public void setUI(TabFolderUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
    	if (tabPlacement == null) {
    		// these values are set here instead of the constructor so that
    		// it can be detected when this call comes from the constructor
    		tabPlacement = TabPlacement.TOP;
    		tabLayoutPolicy = TabLayoutPolicy.HORIZONTAL_SCROLL;
    	} else {
    		// runtime UI change, notify tabs...
        	for (int i=tabModel.getTabCount(); --i>=0;)
        		tabModel.getTabAt(i).updateTabUI();
    		
    	}
        if (UIManager.get(getUIClassID()) != null) {
            setUI((TabFolderUI)UIManager.getUI(this));
        } else {
            setUI(new BasicTabFolderUI());
        }
    }

    public TabFolderUI getUI() {
        return (TabFolderUI)ui;
    }

    public String getUIClassID() {
        return uiClassID;
    }
    

}


