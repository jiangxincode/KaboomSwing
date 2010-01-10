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
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;
import java.util.*;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.table.*;

import aephyr.swing.*;

// NOTE: there is a conflict between DragScrollSupport and Drag & Drop
//		Drag Scroll will initiate immediately on drag whereas dnd will wait until the drag crosses a certain
//		threshold and then take over in such a way that Drag Scroll never receives a mouseReleased event
//		to clean up after itself. Probably should make a note in the DragScrollSupport class that care should
//		be taken to ensure that DragScrollSupport is not initiated by the same button as dnd when applicable.

public class RolloverSupportTest extends MouseAdapter implements Runnable, ActionListener, ItemListener, ChangeListener {
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new RolloverSupportTest());
	}
	
	public void run() {
		UIManager.LookAndFeelInfo[] lafArray = UIManager.getInstalledLookAndFeels();
		String[] lafNames = new String[lafArray.length];
		boolean useNimbus = false;
		for (int i=lafArray.length; --i>=0;) {
			lafNames[i] = lafArray[i].getName();
			if (useNimbus && "Nimbus".equals(lafNames[i])) {
				try {
					UIManager.setLookAndFeel(lafArray[i].getClassName());
				} catch (Exception x) {
					x.printStackTrace();
				}
			}
		}
		
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
		tree.setEditable(true);
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
		
		list.setTransferHandler(listActor);
		table.setTransferHandler(tableActor);
		tree.setTransferHandler(treeActor);
		
		list.setDropMode(DropMode.INSERT);
		table.setDropMode(DropMode.INSERT);
		tree.setDropMode(DropMode.INSERT);
		
		DragScrollSupport dss = new DragScrollSupport();
		dss.register(list);
		dss.register(table);
		dss.register(tree);
		ScrollPadSupport sps = new ScrollPadSupport();
		sps.register(list);
		sps.register(table);
		sps.register(tree);
		
		
		rtol = new JCheckBoxMenuItem("Right to Left", false);
		rtol.addItemListener(this);
		
		JMenu laf = new JMenu("Look & Feel");
		ButtonGroup group = new ButtonGroup();
		String lafName = UIManager.getLookAndFeel().getName();
		for (String n : lafNames) {
			JRadioButtonMenuItem r = new JRadioButtonMenuItem(n, n.equals(lafName));
			group.add(r);
			r.addItemListener(this);
			laf.add(r);
		}

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
		add(simulation, options, content, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		options.addSeparator();
		options.add(dragEnabled);
		options.add(rtol);
		options.add(laf);
		options.addSeparator();
		add(enabled, options, content, KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
		options.add(mouseListener);
		options.add(mouseMotionListener);
		
		JMenu actions = new JMenu("Actions");
		add(actions, content, "Change Value", KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		add(actions, content, "Change Value Under Mouse", KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK));
		add(actions, content, "Add Row", KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		add(actions, content, "Add Row Under Mouse", KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK));
		add(actions, content, "Delete Row", KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		add(actions, content, "Delete Row Under Mouse", KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.ALT_DOWN_MASK));
		add(actions, content, "Change Model", KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_DOWN_MASK));
		add(actions, content, "Hide", KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK));
		actions.addSeparator();
		add(actions, content, "List: Show", KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.ALT_DOWN_MASK));
		actions.addSeparator();
		add(actions, content, "Table: Show", KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.ALT_DOWN_MASK));
		add(actions, content, "Table: Sort Column", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK));
		actions.addSeparator();
		add(actions, content, "Tree: Show", KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.ALT_DOWN_MASK));
		add(actions, content, "Tree: Collapse Node", KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK));
		add(actions, content, "Tree: Expand Node", KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK));
		
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
	
	JCheckBoxMenuItem rtol;
	JCheckBoxMenuItem enabled;
	JCheckBoxMenuItem simulation;
	JCheckBoxMenuItem dragEnabled;
	JCheckBoxMenuItem mouseListener;
	JCheckBoxMenuItem mouseMotionListener;

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
		} else if (e.getSource() == rtol) {
			ComponentOrientation o = selected ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT;
			list.setComponentOrientation(o);
			tree.setComponentOrientation(o);
			table.setComponentOrientation(o);
		} else if (e.getSource() instanceof JRadioButtonMenuItem) {
			JRadioButtonMenuItem r = (JRadioButtonMenuItem)e.getSource();
			final String name = r.getText();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
							if (name.equals(info.getName())) {
								UIManager.setLookAndFeel(info.getClassName());
								SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(list));
								break;
							}
						}
					} catch (Exception x) {
						x.printStackTrace();
					}
				}
			});
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
			if (n < 50) {
				actor.changeValue(n > 20);
			} else if (n < 70) {
				actor.addRow(n > 60);
			} else if (n < 90) {
				actor.deleteRow(n > 80);
			} else if (n < 92) {
				actor.changeModel();
			} else {
				actor.setVisible(n > 95);
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
			} else if (cmd == "Enable Rollover Support") {
				enabled.setSelected(!enabled.isSelected());
			} else if (cmd == "Simulation") {
				simulation.setSelected(!simulation.isSelected());
			} else if (cmd == "Tree: Collapse Node") {
				treeActor.collapse();
			} else if (cmd == "Tree: Expand Node") {
				treeActor.expand();
			} else if (cmd == "Table: Sort Column") {
				tableActor.sort();
			} else if (cmd == "Hide") {
				getActor().setVisible(false);
			} else if (cmd == "List: Show") {
				listActor.setVisible(true);
			} else if (cmd == "Tree: Show") {
				treeActor.setVisible(true);
			} else if (cmd == "Table: Show") {
				tableActor.setVisible(true);
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
		void setVisible(boolean visible);
	}
	
	class ListActor extends TransferHandler implements Actor {
		int getIndexUnderMouse() {
			int idx;
			if (listRolloverSupport.isEnabled()) {
				idx = listRolloverSupport.getRolloverIndex();
			} else {
				Point pt = list.getMousePosition();
				idx = pt == null ? -1 : list.locationToIndex(pt);
			}
			return idx;
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
		public void setVisible(boolean visible) {
			list.getParent().getParent().setVisible(visible);
		}

		public boolean canImport(TransferSupport support) {
			return support.isDataFlavorSupported(DataFlavor.stringFlavor);
		}
		protected Transferable createTransferable(JComponent c) {
			Object[] o = list.getSelectedValues();
			switch (o.length) {
			case 0: return null;
			case 1: return new StringSelection(o[0].toString());
			}
			StringBuilder s = new StringBuilder(o.length*10);
			for (int i=0;;) {
				s.append(o[i].toString());
				if (++i>=o.length)
					break;
				s.append('\n');
			}
			return new StringSelection(s.toString());
		}
		public int getSourceActions(JComponent c) {
			return COPY;
		}
		public boolean importData(TransferSupport support) {
			if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				try {
					String str = (String)support.getTransferable().getTransferData(DataFlavor.stringFlavor);
					String[] array = str.split("\n");
					int idx;
					if (support.isDrop()) {
						idx = ((JList.DropLocation)support.getDropLocation()).getIndex();
					} else {
						idx = list.getLeadSelectionIndex();
					}
					DefaultListModel mdl = (DefaultListModel)list.getModel();
					if (idx < 0)
						idx = mdl.getSize();
					for (int i=array.length; --i>=0;)
						mdl.insertElementAt(array[i], idx);
					return true;
				} catch (IOException e) {
					e.printStackTrace();
				} catch (UnsupportedFlavorException e) {
					e.printStackTrace();
				}
			}
			return false;
		}
	}

	static class TableTransferable implements Transferable {
		static final DataFlavor flavor = createFlavor();
		static DataFlavor createFlavor() {
			try {
				return new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType+";class=java.util.List");
			} catch (Exception e) {
				return null;
			}
		}
		
		TableTransferable(List<Object[]> rows) {
			list = rows;
		}
		
		List<Object[]> list;
		
		@Override
		public Object getTransferData(DataFlavor flavor)
				throws UnsupportedFlavorException, IOException {
			if (TableTransferable.flavor.equals(flavor))
				return list;
			throw new UnsupportedFlavorException(flavor);
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { flavor };
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return TableTransferable.flavor.equals(flavor);
		}
	}
	class TableActor extends TransferHandler implements Actor {
		public void changeValue(boolean underMouse) {
			int row, col;
			if (underMouse) {
				if (tableRolloverSupport.isEnabled()) {
					row = tableRolloverSupport.getRolloverRow();
					col = tableRolloverSupport.getRolloverColumn();
				} else {
					Point pt = table.getMousePosition();
					if (pt == null)
						return;
					row = table.rowAtPoint(pt);
					col = table.columnAtPoint(pt);
				}
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
		int getColUnderMouse() {
			Point pt = table.getMousePosition();
			if (pt == null)
				return -1;
			int col = table.columnAtPoint(pt);
			if (col >= 0)
				col = table.convertColumnIndexToModel(col);
			return col;
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
		public void setVisible(boolean visible) {
			table.getParent().getParent().setVisible(visible);
		}
		void sort() {
			int col = getColUnderMouse();
			if (col >= 0)
				table.getRowSorter().toggleSortOrder(col);
		}
		
		public boolean canImport(TransferSupport support) {
			return support.isDataFlavorSupported(TableTransferable.flavor);
		}
		protected Transferable createTransferable(JComponent c) {
			ListSelectionModel sel = table.getSelectionModel();
			int max = sel.getMaxSelectionIndex();
			if (max < 0)
				return null;
			int min = sel.getMinSelectionIndex();
			List<Object[]> rows = new ArrayList<Object[]>(max-min+1);
			DefaultTableModel mdl = (DefaultTableModel)table.getModel();
			Vector<Vector<?>> rowVector = (Vector<Vector<?>>)mdl.getDataVector();
			for (int i=min; ;) {
				if (sel.isSelectedIndex(i))
					rows.add(rowVector.get(i).toArray());
				if (++i>=max)
					break;
			}
			return new TableTransferable(rows);
		}
		public int getSourceActions(JComponent c) {
			return COPY;
		}
		public boolean importData(TransferSupport support) {
			if (support.isDataFlavorSupported(TableTransferable.flavor)) {
				try {
					List<Object[]> rows = (List<Object[]>)support.getTransferable().getTransferData(TableTransferable.flavor);
					int row;
					if (support.isDrop()) {
						row = ((JTable.DropLocation)support.getDropLocation()).getRow();
					} else {
						row = table.getSelectionModel().getLeadSelectionIndex();
					}
					if (row < 0)
						row = table.getRowCount();
					DefaultTableModel mdl = (DefaultTableModel)table.getModel();
					for (Object[] rowData : rows)
						mdl.insertRow(row, rowData);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (UnsupportedFlavorException e) {
					e.printStackTrace();
				}
			}
			return false;
		}
	}
	
	class TreeActor extends TransferHandler implements Actor {
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
		public void setVisible(boolean visible) {
			tree.getParent().getParent().setVisible(visible);
		}
		void collapse() {
			TreePath path = getPath(true);
			if (path != null && path.getPathCount() > 1) {
				TreePath parent = path.getParentPath();
				tree.collapsePath(parent);
			}
		}
		void expand() {
			TreePath path = getPath(true);
			if (path != null)
				tree.expandPath(path);
		}

		public boolean canImport(TransferSupport support) {
			return support.isDataFlavorSupported(DataFlavor.stringFlavor);
		}
		protected Transferable createTransferable(JComponent c) {
			TreePath[] paths = tree.getSelectionPaths();
			if (paths == null)
				return null;
			if (paths.length == 1)
				return new StringSelection(paths[0].getLastPathComponent().toString());
			StringBuilder s = new StringBuilder(paths.length*10);
			for (int i=0;;) {
				s.append(paths[i].getLastPathComponent().toString());
				if (++i>=paths.length)
					break;
				s.append('\n');
			}
			return new StringSelection(s.toString());
		}
		public int getSourceActions(JComponent c) {
			return COPY;
		}
		public boolean importData(TransferSupport support) {
			if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				try {
					String str = (String)support.getTransferable().getTransferData(DataFlavor.stringFlavor);
					String[] nodes = str.split("\n");
					TreePath path;
					int idx;
					if (support.isDrop()) {
						JTree.DropLocation loc = (JTree.DropLocation)support.getDropLocation();
						path = loc.getPath();
						idx = loc.getChildIndex();
					} else {
						path = tree.getLeadSelectionPath();
						if (path.getPathCount() > 1) {
							Object child = path.getLastPathComponent();
							path = path.getParentPath();
							idx = tree.getModel().getIndexOfChild(path.getLastPathComponent(), child);
						} else {
							idx = tree.getModel().getChildCount(path.getLastPathComponent());
						}
					}
					if (idx < 0)
						idx = 0;
					DefaultTreeModel mdl = (DefaultTreeModel)tree.getModel();
					MutableTreeNode par = (MutableTreeNode)path.getLastPathComponent();
					for (int i=nodes.length; --i>=0;)
						mdl.insertNodeInto(new DefaultMutableTreeNode(nodes[i]), par, idx);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (UnsupportedFlavorException e) {
					e.printStackTrace();
				}
			}
			return false;
		}
	}
	
	static void expandTree(JTree tree) {
		for (int row=0; row<tree.getRowCount(); row++) {
			tree.expandRow(row);
		}
	}
	
}

