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

import java.awt.Dimension;


public abstract class AbstractTabRenderer implements TabRenderer {

	protected AbstractTabRenderer(Dimension preferredTabSize, Dimension minimumTabSize) {
		prefSize = preferredTabSize;
		minSize = minimumTabSize;
	}

	boolean closeable;

	Dimension prefSize;

	Dimension minSize;

	Dimension maxSize;
	
	public void setCloseable(boolean closeable) {
		this.closeable = closeable;
	}

	public boolean isCloseable() {
		return closeable;
	}

	public Dimension getTabPreferredSize() {
		return new Dimension(prefSize);
	}

	public Dimension getTabMinimumSize() {
		return new Dimension(minSize);
	}

	public Dimension getTabMaximumSize() {
		if (maxSize != null)
			return new Dimension(maxSize);
		return null;
	}

	public void setPreferredTabSize(Dimension size) {
		prefSize = size;
	}

	public void setMinimumTabSize(Dimension size) {
		minSize = size;
	}

	public void setMaximumTabSize(Dimension size) {
		maxSize = size;
	}

}