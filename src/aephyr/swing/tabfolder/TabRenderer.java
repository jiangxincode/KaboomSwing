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

import aephyr.swing.TabFolder;

public interface TabRenderer {

	/**
	 * Used for rendering the contents of a tab. Note that the
	 * background and border are rendered by the UI.
	 * 
	 * @param tabFolder - TabFolder being painted
	 * @param tab - Tab to render
	 * @param index - index of the tab
	 * @param isSelected - selection state of the tab
	 * 
	 * @return component to render the contents of a tab
	 */
	Component getTabRendererComponent(TabFolder tabFolder, Tab tab, int index, boolean isSelected);
	
	/**
	 * When the mouse rolls over a tab, the component returned by this
	 * method will be placed over the tab's cell bounds. This is
	 * useful for providing interactive child components on tabs as
	 * well as custom processing of mouse events on the tab.
	 * <p>
	 * If mouse events need to be processed on tabs, then the only
	 * reliable way is to use a mouse listener on the rollover component
	 * as tabs may be shown in a popup (e.g. MRU layout, when a gap
	 * cell is clicked or hovered over, the hidden tabs represented
	 * by that gap will appear in a popup).
	 * <p>
	 * Note that if a mouse listener is installed on the rollover component,
	 * then the rollover component assumes all responsibilities for tab
	 * selection via the mouse.
	 * 
	 * @param tabFolder - TabFolder being painted
	 * @param tab - Tab to render
	 * @param index - index of the tab
	 * @param isSelected - selection state of the tab
	 * 
	 * @return component to display under the mouse, may be <code>null</code>
	 */
	Component getTabRolloverComponent(TabFolder tabFolder, Tab tab, int index, boolean isSelected);

	/**
	 * Used to help determine the preferred size of the tab folder.
	 * <p>
	 * How the values are used depends on the TabPlacement of the tab folder.
	 * <ul><li>
	 * TOP and BOTTOM:
	 * 		width is multiplied by an unspecified integer greater than 1.
	 * 		height is used to determine the height of the tab area.
	 * </li><li>
	 * LEFT and RIGHT:
	 * 		width is used to determine the width of the tab area.
	 * 		height is multiplied by an unspecified integer greater than 1.
	 * </li></ul>
	 * <p>
	 * Unlike {@link#getMinimumTabSize} and {@link#getMaximumTabSize},
	 * <code>null</code> values are not permitted.
	 * 
	 * @return preferred size for a single tab
	 */
	Dimension getTabPreferredSize();
	
	/**
	 * Ensures that a tab is at least the specified size.
	 * <p>
	 * Also may be used (if non-null) to help determine the minimum
	 * size of the tab folder. Usage is similar to {@link #getTabPreferredSize()}
	 * except that a UI may make the calculation based on displaying
	 * only a single tab.
	 * <p>
	 * A value of <code>null</code> indicates there is no minimum size.
	 * 
	 * @return minimum size a tab is allowed to be, <code>null</code> if there
	 * 		is no minimum
	 */
	Dimension getTabMinimumSize();

	/**
	 * Ensures that a tab is not larger than the specified size.
	 * <p>
	 * A value of <code>null</code> indicates there is no maximum size.
	 * 
	 * @return maximum size a tab is allowed to be, <code>null</code>
	 * 		if there is no maximum
	 */
	Dimension getTabMaximumSize();

}
