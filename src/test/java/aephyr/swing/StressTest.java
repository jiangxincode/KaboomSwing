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
import java.util.*;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;

public class StressTest implements Runnable, ActionListener, ItemListener, ChangeListener {
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new StressTest());
	}
	
	public void run() {
		
		list = new JList(createListModel());
		list.setPrototypeCellValue(Long.MAX_VALUE);
		
		tree = new JTree(createTreeModel());
		tree.setEditable(true);
		expandTree(tree);
		
		TableColumnModel columns = new DefaultTableColumnModel();
		for (int i=0; i<COLUMNS; i++)
			columns.addColumn(createTableColumn(i));
		
		table = new JTable(createTableModel(), columns);
		
		
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setAutoCreateRowSorter(true);
		
		listActor = new ListActor();
		tableActor = new TableActor();
		treeActor = new TreeActor();
		
		simulation = new JCheckBoxMenuItem("Simulation", false);
		simulation.addItemListener(this);
		dragEnabled = new JCheckBoxMenuItem("Drag Enabled", false);
		dragEnabled.addItemListener(this);
		
		JFrame frame = new JFrame();
		JComponent content = (JComponent)frame.getContentPane();
		
		JMenu options = new JMenu("Options");
		add(simulation, options, content, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		options.addSeparator();
		options.add(dragEnabled);
		
		JMenu actions = new JMenu("Actions");
		add(actions, content, "Change Value", KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		add(actions, content, "Change Value Under Mouse", KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK));
		add(actions, content, "Add Row", KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		add(actions, content, "Add Row Under Mouse", KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK));
		add(actions, content, "Delete Row", KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		add(actions, content, "Delete Row Under Mouse", KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.ALT_DOWN_MASK));
		add(actions, content, "Change Model", KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_DOWN_MASK));
		
		JMenuBar menu = new JMenuBar();
		menu.add(options);
		menu.add(actions);

		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 0));
		simulationModel = new SpinnerNumberModel(100, 100, 5000, 100);
		JSpinner simulationInterval = new JSpinner(simulationModel);
		simulationModel.addChangeListener(this);
		north.add(new JLabel("Simulation Interval:"));
		north.add(simulationInterval);

		content.add(north, BorderLayout.NORTH);
		content.add(new JScrollPane(list), BorderLayout.WEST);
		content.add(new JScrollPane(table), BorderLayout.CENTER);
		content.add(new JScrollPane(tree), BorderLayout.EAST);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setJMenuBar(menu);
		frame.pack();
		frame.setVisible(true);
	}
	void add(JMenu menu, JComponent c, String cmd, KeyStroke stroke) {
		JMenuItem item = new JMenuItem(cmd);
		item.addActionListener(this);
		add(item, menu, c, stroke);
	}
	void add(JMenuItem item, JMenu menu, JComponent c, KeyStroke stroke) {
		menu.add(item);
		item.setAccelerator(stroke);
		c.registerKeyboardAction(this, item.getText(), stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}
	
	JList list;
	ListActor listActor;
	
	JTable table;
	TableActor tableActor;
	
	JTree tree;
	TreeActor treeActor;
	
	JCheckBoxMenuItem simulation;
	JCheckBoxMenuItem dragEnabled;

	SpinnerNumberModel simulationModel;
	Timer simulationTimer;
	Random random = new Random();
	static final int ROWS = 200;
	static final int COLUMNS = 20;
	
	ListModel createListModel() {
		DefaultListModel model = new DefaultListModel();
		for (int i=ROWS; --i>=0;)
			model.addElement(createCellValue());
		return model;
	}
	
	TableModel createTableModel() {
		Object[] cols = new Object[COLUMNS];
		for (int i=COLUMNS; --i>=0;)
			cols[i] = Integer.toString(i);
		DefaultTableModel model = new DefaultTableModel(cols, 0) {
			public Class<?> getColumnClass(int col) {
				return Integer.class;
			}
		};
		for (int i=ROWS; --i>=0;) {
			model.addRow(createTableRow());
		}
		return model;
	}
	
	Object[] createTableRow() {
		Object[] row = new Object[COLUMNS];
		for (int j=COLUMNS; --j>=0;)
			row[j] = createCellValue();
		return row;
	}
	
	TreeModel createTreeModel() {
		return new DefaultTreeModel(createTreeNode(3));
	}
	
	MutableTreeNode createTreeNode(int depth) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(createCellValue());
		if (--depth >= 0)
			for (int i=random.nextInt((depth/2+1)*Math.round(ROWS/20f))+1; --i>=0;)
				node.add(createTreeNode(depth));
		return node;
	}
	
	TableColumn createTableColumn(int n) {
		TableColumn column = new TableColumn(n, 100);
		column.setMinWidth(100);
		column.setResizable(true);
		column.setHeaderValue(Integer.toString(n));
		return column;
	}
	
	Integer createCellValue() {
		int d = random.nextInt(10)+1;
		d = d * d * d * d * d;
		return random.nextInt(Integer.MAX_VALUE)/d;		
	}
	
	public void itemStateChanged(ItemEvent e) {
		boolean selected = e.getStateChange() == ItemEvent.SELECTED;
		if (e.getSource() == simulation) {
			if (selected) {
				simulationTimer = new Timer(simulationModel.getNumber().intValue(), this);
				simulationTimer.start();
			} else {
				simulationTimer.stop();
				simulationTimer = null;
			}
		} else if (e.getSource() == dragEnabled) {
			list.setDragEnabled(selected);
			tree.setDragEnabled(selected);
			table.setDragEnabled(selected);
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == simulationTimer) {
			Actor actor;
			switch (random.nextInt(3)) {
			case 0: actor = listActor; break;
			case 1: actor = tableActor; break;
			default: actor = treeActor; break;
			}
			int n = random.nextInt(100);
			if (n < 55) {
				actor.changeValue(n > 20);
			} else if (n < 75) {
				actor.addRow(n > 65);
			} else if (n < 95) {
				actor.deleteRow(n > 85);
			} else {
				actor.changeModel();
			}
		} else {
			String cmd = e.getActionCommand();
			if (cmd == "Change Value Under Mouse") {
				getActor().changeValue(true);
			} else if (cmd == "Add Row Under Mouse") {
				getActor().addRow(true);
			} else if (cmd == "Delete Row Under Mouse") {
				getActor().deleteRow(true);
			} else if (cmd == "Change Value") {
				getActor().changeValue(false);
			} else if (cmd == "Add Row") {
				getActor().addRow(false);
			} else if (cmd == "Delete Row") {
				getActor().deleteRow(false);
			} else if (cmd == "Change Model") {
				getActor().changeModel();
			} else if (cmd == "Simulation") {
				simulation.setSelected(!simulation.isSelected());
			}
		}
	}
	
	Actor getActor() {
		if (list.getMousePosition(true) != null)
			return listActor;
		if (tree.getMousePosition(true) != null)
			return treeActor;
		return tableActor;
	}
	
	public void stateChanged(ChangeEvent e) {
		if (simulationTimer != null)
			simulationTimer.setDelay(simulationModel.getNumber().intValue());
	}
	
	interface Actor {
		void changeValue(boolean underMouse);
		void addRow(boolean underMouse);
		void deleteRow(boolean underMouse);
		void changeModel();
	}
	
	class ListActor implements Actor {
		int getIndexUnderMouse() {
			Point pt = list.getMousePosition();
			return pt == null ? -1 : list.locationToIndex(pt);
		}
		public void changeValue(boolean underMouse) {
			int idx = underMouse ? getIndexUnderMouse() : random.nextInt(list.getModel().getSize());
			if (idx >= 0)
				((DefaultListModel)list.getModel()).set(idx, createCellValue());
		}
		public void addRow(boolean underMouse) {
			int idx = underMouse ? getIndexUnderMouse() : random.nextInt(list.getModel().getSize()+1);
			if (idx >= 0)
				((DefaultListModel)list.getModel()).add(idx, createCellValue());
		}
		public void deleteRow(boolean underMouse) {
			int idx = underMouse ? getIndexUnderMouse() : random.nextInt(list.getModel().getSize());
			if (idx >= 0)
				((DefaultListModel)list.getModel()).remove(idx);
		}
		public void changeModel() {
			list.setModel(createListModel());
		}
	}
	
	class TableActor implements Actor {
		public void changeValue(boolean underMouse) {
			int row, col;
			if (underMouse) {
				Point pt = table.getMousePosition();
				if (pt == null)
					return;
				row = table.rowAtPoint(pt);
				col = table.columnAtPoint(pt);
			} else {
				row = random.nextInt(table.getRowCount());
				col = random.nextInt(table.getColumnCount());
			}
			if (row >= 0 && col >= 0) {
				row = table.convertRowIndexToModel(row);
				col = table.convertColumnIndexToModel(col);
				((DefaultTableModel)table.getModel()).setValueAt(createCellValue(), row, col);
			}
		}
		int getRowUnderMouse() {
			Point pt = table.getMousePosition();
			if (pt == null)
				return -1;
			int row = table.rowAtPoint(pt);
			if (row >= 0)
				row = table.convertRowIndexToModel(row);
			return row;
		}
		public void addRow(boolean underMouse) {
			int row = underMouse ? getRowUnderMouse() : random.nextInt(table.getRowCount()+1);
			if (row >= 0)
				((DefaultTableModel)table.getModel()).insertRow(row, createTableRow());
		}
		public void deleteRow(boolean underMouse) {
			int row = underMouse ? getRowUnderMouse() : random.nextInt(table.getRowCount());
			if (row >= 0)
				((DefaultTableModel)table.getModel()).removeRow(row);
		}
		public void changeModel() {
			table.setModel(createTableModel());
		}
	}
	
	class TreeActor implements Actor {
		TreePath getPath(boolean underMouse) {
			if (underMouse) {
				Point pt = tree.getMousePosition(true);
				return pt == null ? null : tree.getPathForLocation(pt.x, pt.y);
			} else {
				return tree.getPathForRow(random.nextInt(tree.getRowCount()));

			}
		}
		public void changeValue(boolean underMouse) {
			TreePath path = getPath(underMouse);
			if (path != null) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
				node.setUserObject(createCellValue());
				((DefaultTreeModel)tree.getModel()).nodeChanged(node);
			}
		}
		public void addRow(boolean underMouse) {
			TreePath path = getPath(underMouse);
			if (path != null && path.getPathCount() > 1) {
				TreePath parent = path.getParentPath();
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)parent.getLastPathComponent();
				int idx = node.getIndex((TreeNode)path.getLastPathComponent());
				((DefaultTreeModel)tree.getModel()).insertNodeInto(createTreeNode(0), node, idx);
			}
		}
		public void deleteRow(boolean underMouse) {
			TreePath path = getPath(underMouse);
			if (path != null) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
				node = node.getFirstLeaf();
				((DefaultTreeModel)tree.getModel()).removeNodeFromParent(node);
			}
		}
		public void changeModel() {
			tree.setModel(createTreeModel());
			expandTree(tree);
		}
	}
	
	static void expandTree(JTree tree) {
		for (int row=0; row<tree.getRowCount(); row++) {
			tree.expandRow(row);
		}
	}
}

