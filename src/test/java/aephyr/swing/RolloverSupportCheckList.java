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
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import java.util.*;

import aephyr.swing.RolloverSupport;

public class RolloverSupportCheckList implements Runnable, ItemListener, ActionListener {
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new RolloverSupportCheckList());
	}
	
	public void run() {
		list = new JList(java.lang.annotation.ElementType.values());
		checkSelection = new DefaultListSelectionModel();
		list.registerKeyboardAction(this, "Toggle Check Selection",
				KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JComponent.WHEN_FOCUSED);
		list.setCellRenderer(new Renderer());
		Renderer rollover = new Renderer();
		rollover.addItemListener(this);
		rolloverSupport = new RolloverSupport.List(list, rollover);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new JScrollPane(list), BorderLayout.CENTER);
		frame.setSize(200, 500);
		frame.setVisible(true);
	}
	
	JList list;
	ListSelectionModel checkSelection;
	RolloverSupport.List rolloverSupport;
	boolean ignoreSelectionChange = false;

	class Renderer extends JCheckBox implements ListCellRenderer {
		
		Renderer() {
			setFocusPainted(false);
		}
		
		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			setText(value == null ? "" : value.toString());
			ignoreSelectionChange = true;
			setSelected(checkSelection.isSelectedIndex(index));
			ignoreSelectionChange = false;
			setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
			return this;
		}
		
		protected void processMouseEvent(MouseEvent e) {
			if ((e.getModifiers() & (InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK)) != 0) {
				list.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, list));
			} else {
				super.processMouseEvent(e);
			}
		}
	}
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (ignoreSelectionChange)
			return;
		int index = rolloverSupport.getRolloverIndex();
		if (e.getStateChange() == ItemEvent.SELECTED) {
			checkSelection.addSelectionInterval(index, index);
		} else {
			checkSelection.removeSelectionInterval(index, index);
		}
		list.requestFocusInWindow();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == "Toggle Check Selection") {
			ListSelectionModel sel = list.getSelectionModel();
			int max = sel.getMaxSelectionIndex();
			if (max < 0)
				return;
			int min = sel.getMinSelectionIndex();
			for (int i=min; i<=max; i++) {
				if (sel.isSelectedIndex(i)) {
					if (checkSelection.isSelectedIndex(i)) {
						checkSelection.removeSelectionInterval(i, i);
					} else {
						checkSelection.addSelectionInterval(i, i);
					}
				}
			}
			Rectangle r = list.getCellBounds(min, max);
			if (r != null)
				list.repaint(r);
			int index = rolloverSupport.getRolloverIndex();
			if (index >= min && index <= max)
				rolloverSupport.validate();
		}
	}
	
	
}