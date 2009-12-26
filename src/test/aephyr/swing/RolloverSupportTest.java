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
package test.aephyr.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.Random;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;

import aephyr.swing.*;

public class RolloverSupportTest extends MouseAdapter implements Runnable, ActionListener, ItemListener, ChangeListener {
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new RolloverSupportTest());
	}
	
	public void run() {
		list = new JList(createListModel());
		list.setPrototypeCellValue(Long.MAX_VALUE);
		listRollover = new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(
					JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setForeground(Color.RED);
				return this;
			}
		};
		listRolloverSupport = new RolloverSupport.List(list, listRollover);
		
		tree = new JTree(createTreeModel());
		expandTree(tree);
		treeRollover = new DefaultTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(JTree tree,
					Object value, boolean sel, boolean expanded, boolean leaf,
					int row, boolean hasFocus) {
				super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
						row, hasFocus);
				setForeground(Color.RED);
				return this;
			}
		};
		treeRolloverSupport = new RolloverSupport.Tree(tree, treeRollover);
		
		TableColumnModel columns = new DefaultTableColumnModel();
		for (int i=0; i<COLUMNS; i++)
			columns.addColumn(createTableColumn(i));
		
		table = new JTable(createTableModel(), columns);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setAutoCreateRowSorter(true);
		tableRollover = new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				super.getTableCellRendererComponent(
						table, value, isSelected, hasFocus, row, column);
				setForeground(Color.RED);
				return this;
			}
		};
		tableRollover.setHorizontalAlignment(JLabel.RIGHT);
		tableRolloverSupport = new RolloverSupport.Table(table, tableRollover);
		
		listActor = new ListActor();
		tableActor = new TableActor();
		treeActor = new TreeActor();
		
		DragScrollSupport dss = new DragScrollSupport();
		dss.register(list);
		dss.register(table);
		dss.register(tree);
		ScrollPadSupport sps = new ScrollPadSupport();
		sps.register(list);
		sps.register(table);
		sps.register(tree);
		
		
		enabled = new JCheckBoxMenuItem("Enable Rollover Support", true);
		enabled.addItemListener(this);
		simulation = new JCheckBoxMenuItem("Simulation", false);
		simulation.addItemListener(this);
		dragEnabled = new JCheckBoxMenuItem("Drag Enabled", false);
		dragEnabled.addItemListener(this);
		mouseListener = new JCheckBoxMenuItem("MouseListener on Rollover Component", false);
		mouseListener.addItemListener(this);
		mouseMotionListener = new JCheckBoxMenuItem("MouseMotionListener on Rollover Component", false);
		mouseMotionListener.addItemListener(this);
		
		JFrame frame = new JFrame();
		JComponent content = (JComponent)frame.getContentPane();
		
		JMenu options = new JMenu("Options");
		add(enabled, options, content, KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
		add(simulation, options, content, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		options.addSeparator();
		options.add(dragEnabled);
		options.add(mouseListener);
		options.add(mouseMotionListener);
		
		JMenu actions = new JMenu("Actions");
		add(actions, content, "Change Value Under Mouse", KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK));
		add(actions, content, "Add Row", KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK));
		add(actions, content, "Delete Row", KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.ALT_DOWN_MASK));
		add(actions, content, "Change Model", KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_DOWN_MASK));
		
		
		JMenuBar menu = new JMenuBar();
		menu.add(options);
		menu.add(actions);

		Box north = Box.createHorizontalBox();
		simulationModel = new SpinnerNumberModel(100, 100, 5000, 100);
		JSpinner simulationInterval = new JSpinner(simulationModel);
		simulationInterval.setMaximumSize(simulationInterval.getPreferredSize());
		simulationModel.addChangeListener(this);
		north.add(new JLabel("    Simulation Interval: "));
		north.add(simulationInterval);
		north.add(Box.createHorizontalGlue());
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setJMenuBar(menu);
		content.add(north, BorderLayout.NORTH);
		content.add(new JScrollPane(list), BorderLayout.WEST);
		content.add(new JScrollPane(table), BorderLayout.CENTER);
		content.add(new JScrollPane(tree), BorderLayout.EAST);
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
	DefaultListCellRenderer listRollover;
	RolloverSupport.List listRolloverSupport;
	ListActor listActor;
	
	JTable table;
	DefaultTableCellRenderer tableRollover;
	RolloverSupport.Table tableRolloverSupport;
	TableActor tableActor;
	
	JTree tree;
	DefaultTreeCellRenderer treeRollover;
	RolloverSupport.Tree treeRolloverSupport;
	TreeActor treeActor;
	
	JCheckBoxMenuItem enabled;
	JCheckBoxMenuItem simulation;
	JCheckBoxMenuItem dragEnabled;
	JCheckBoxMenuItem mouseListener;
	JCheckBoxMenuItem mouseMotionListener;

	SpinnerNumberModel simulationModel;
	Timer simulationTimer;
	Random random = new Random();
	static final int ROWS = 200;
	static final int COLUMNS = 10;
	
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
			Object[] row = new Object[COLUMNS];
			for (int j=COLUMNS; --j>=0;)
				row[j] = createCellValue();
			model.addRow(row);
		}
		return model;
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
		} else if (e.getSource() == enabled) {
			listRolloverSupport.setEnabled(selected);
			treeRolloverSupport.setEnabled(selected);
			tableRolloverSupport.setEnabled(selected);
		} else if (e.getSource() == dragEnabled) {
			list.setDragEnabled(selected);
			tree.setDragEnabled(selected);
			table.setDragEnabled(selected);
		} else if (e.getSource() == mouseListener) {
			if (selected) {
				listRollover.addMouseListener(this);
				treeRollover.addMouseListener(this);
				tableRollover.addMouseListener(this);
			} else {
				listRollover.removeMouseListener(this);
				treeRollover.removeMouseListener(this);
				tableRollover.removeMouseListener(this);
			}
		} else if (e.getSource() == mouseMotionListener) {
			if (selected) {
				listRollover.addMouseMotionListener(this);
				treeRollover.addMouseMotionListener(this);
				tableRollover.addMouseMotionListener(this);
			} else {
				listRollover.removeMouseMotionListener(this);
				treeRollover.removeMouseMotionListener(this);
				tableRollover.removeMouseMotionListener(this);
			}
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == simulationTimer) {
			Actor actor;
			switch (random.nextInt(3)) {
			case 0: actor = listActor; break;
			case 1: actor = tableActor; break;
			case 2: default: actor = treeActor; break;
			}
			int n = random.nextInt(50);
			if (n < 25) {
				actor.changeValue();
			} else if (n < 35) {
				actor.addRow();
			} else if (n < 45) {
				actor.deleteRow();
			} else {
				actor.changeModel();
			}
		} else {
			String cmd = e.getActionCommand();
			if (cmd == "Change Value Under Mouse") {
				getActor().changeValue();
			} else if (cmd == "Add Row") {
				getActor().addRow();
			} else if (cmd == "Delete Row") {
				getActor().deleteRow();
			} else if (cmd == "Change Model") {
				getActor().changeModel();
			} else if (cmd == "Enable Rollover Support") {
				enabled.setSelected(!enabled.isSelected());
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
		void changeValue();
		void addRow();
		void deleteRow();
		void changeModel();
	}
	
	class ListActor implements Actor {
		public void changeValue() {
			int idx = listRolloverSupport.getRolloverIndex();
			if (idx >= 0)
				((DefaultListModel)list.getModel()).set(idx, createCellValue());
		}
		public void addRow() {
			DefaultListModel mdl = (DefaultListModel)list.getModel();
			mdl.add(random.nextInt(mdl.getSize()+1), createCellValue());
		}
		public void deleteRow() {
			DefaultListModel mdl = (DefaultListModel)list.getModel();
			if (mdl.getSize() > 0)
				mdl.remove(random.nextInt(mdl.getSize()));
		}
		public void changeModel() {
			list.setModel(createListModel());
		}
	}
	
	class TableActor implements Actor {
		public void changeValue() {
			int row = tableRolloverSupport.getRolloverRow();
			int col = tableRolloverSupport.getRolloverColumn();
			if (row >= 0 && col >= 0) {
				row = table.convertRowIndexToModel(row);
				col = table.convertColumnIndexToModel(col);
				DefaultTableModel mdl = (DefaultTableModel)table.getModel();
				mdl.setValueAt(createCellValue(), row, col);
			}
		}
		public void addRow() {
			DefaultTableModel mdl = (DefaultTableModel)table.getModel();
			mdl.insertRow(random.nextInt(mdl.getRowCount()+1), new Object[]{
					createCellValue(), createCellValue(), createCellValue()});
		}
		public void deleteRow() {
			DefaultTableModel mdl = (DefaultTableModel)table.getModel();
			mdl.removeRow(random.nextInt(mdl.getRowCount()));
		}
		public void changeModel() {
			table.setModel(createTableModel());
		}
	}
	
	class TreeActor implements Actor {
		public void changeValue() {
			Point pt = tree.getMousePosition(true);
			if (pt != null) {
				TreePath path = tree.getPathForLocation(pt.x, pt.y);
				if (path != null) {
					DefaultTreeModel mdl = (DefaultTreeModel)tree.getModel();
					DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
					node.setUserObject(createCellValue());
					mdl.nodeChanged(node);
				}
			}
		}
		public void addRow() {
			int row = random.nextInt(tree.getRowCount());
			TreePath path = tree.getPathForRow(row);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			DefaultTreeModel mdl = (DefaultTreeModel)tree.getModel();
			mdl.insertNodeInto(createTreeNode(0), node, 0);
		}
		public void deleteRow() {
			int row = random.nextInt(tree.getRowCount());
			TreePath path = tree.getPathForRow(row);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			node = node.getFirstLeaf();
			DefaultTreeModel mdl = (DefaultTreeModel)tree.getModel();
			mdl.removeNodeFromParent(node);
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

