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
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.UIManager;

import aephyr.swing.ScrollPad;
import aephyr.swing.ScrollPadSupport;

public class ScrollPadTest {
	public static void main(String[] args) throws Exception {
		
//		UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
//		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		
		JList list = new JList(UIManager.getLookAndFeelDefaults().keySet().toArray());
		list.setLayoutOrientation(JList.VERTICAL_WRAP);
		list.setVisibleRowCount(list.getModel().getSize()/5+1);
		ScrollPadSupport scrollPadSupport = new ScrollPadSupport();
		scrollPadSupport.register(list);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JViewport viewport = new JViewport();
		viewport.setView(list);
		frame.add(viewport, BorderLayout.CENTER);
//		frame.add(new JScrollPane(list), BorderLayout.CENTER);
		frame.setBounds(200, 200, 400, 600);
		frame.setVisible(true);
		
	}
}
