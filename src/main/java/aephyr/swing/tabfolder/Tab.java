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

public interface Tab {
	
	public interface Serializable extends Tab, java.io.Serializable {}

	public static final DataFlavor localFlavor = DefaultTab.createTabFlavor(DataFlavor.javaJVMLocalObjectMimeType, Tab.class);

	public static final DataFlavor serialFlavor = DefaultTab.createTabFlavor(DataFlavor.javaSerializedObjectMimeType, Serializable.class);
	
	Component getComponent();
	
	Dimension getPreferredComponentSize();
	
	Dimension getMinimumComponentSize();
	
	String getToolTipText();
	
	String getTitle();
	
	Icon getIcon();
	
	boolean isCloseable();
	
	/**
	 * Called by TabFolder when it's UI has been changed.
	 * <p>
	 * Components are only added to a TabFolder when they
	 * have been selected and made visible. Thus, a tab's
	 * component may not be in the TabFolder's component
	 * hierarchy. So it is the responsibility of the tab
	 * implementation to identify those components and call
	 * their updateUI method.
	 * <p>
	 * A typical algorithm would be:
	 * <pre>
	 *     if (component != null
	 *             && component.getParent() == null
	 *             && component instanceof JComponent)
	 *         ((JComponent)component).updateUI();
	 * </pre>
	 */
	void updateTabUI();

	
}
