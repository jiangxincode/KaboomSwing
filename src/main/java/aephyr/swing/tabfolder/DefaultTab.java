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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;

import javax.swing.Icon;
import javax.swing.JComponent;

public class DefaultTab implements Tab.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1851072360725662945L;

	static DataFlavor createTabFlavor(String mime, Class<?> cls) {
		try {
			return new DataFlavor(mime+";class="+cls.getName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	public DefaultTab(String title, Icon icon, Component component, String toolTipText) {
		this.title = title;
		this.icon = icon;
		this.component = component;
		this.toolTipText = toolTipText;
	}

	String title;
	Icon icon;
	Component component;
	String toolTipText;
	

	public void setComponent(Component component) {
		this.component = component;
	}
	
	@Override
	public Component getComponent() {
		return component;
	}
	
	@Override
	public Dimension getPreferredComponentSize() {
		return component == null ? null : component.getPreferredSize();
	}
	
	@Override
	public Dimension getMinimumComponentSize() {
		return component == null ? null : component.getMinimumSize();
	}

	@Override
	public String getToolTipText() {
		return toolTipText;
	}
	
	public void setToolTipText(String toolTipText) {
		this.toolTipText = toolTipText;
	}

	@Override
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}


	@Override
	public Icon getIcon() {
		return icon;
	}

	public void setIcon(Icon icon) {
		this.icon = icon;
	}
	
	@Override
	public boolean isCloseable() {
		return true;
	}

	@Override
	public void updateTabUI() {
		if (component != null
				&& component.getParent() == null
				&& component instanceof JComponent)
			((JComponent)component).updateUI();
	}
	
	
}
