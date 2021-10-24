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

import java.awt.datatransfer.*;
import java.io.*;

public class TabTransferable implements Transferable {

	public TabTransferable(Tab tab) {
		this(tab, -1);
	}
	
	public TabTransferable(Tab tab, int index) {
		this.tab = tab;
		this.index = index;
	}
	
	private Tab tab;
	
	private int index;
	
	public Tab getTab() {
		return tab;
	}
	
	public int getIndex() {
		return index;
	}

	@Override
	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		if (Tab.localFlavor.equals(flavor) ||
				tab instanceof Tab.Serializable && Tab.serialFlavor.equals(flavor)) {
			return tab;
		}
		throw new UnsupportedFlavorException(flavor);
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return tab instanceof Tab.Serializable ?
			new DataFlavor[] { Tab.localFlavor, Tab.serialFlavor } :
			new DataFlavor[] { Tab.localFlavor };
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return Tab.localFlavor.equals(flavor) ||
			tab instanceof Tab.Serializable && Tab.serialFlavor.equals(flavor);
	}
	
}
