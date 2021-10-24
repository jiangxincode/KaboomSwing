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

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.MouseEvent;

import javax.swing.*;

import aephyr.swing.*;

public class DragScrollTest {
	
	public static void main(String[] args) {

		JList list = new JList(UIManager.getLookAndFeelDefaults().keySet().toArray());
		list.setLayoutOrientation(JList.VERTICAL_WRAP);
		list.setVisibleRowCount(list.getModel().getSize()/5+1);
		list.putClientProperty("List.isFileList", true);
		
		DragScrollSupport support = new DragScrollSupport();
//		support.setDragButton(MouseEvent.BUTTON2);
		support.register(list);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new JScrollPane(list), BorderLayout.CENTER);
		frame.setBounds(200, 200, 400, 600);
		frame.setVisible(true);

	}
	
	
}
