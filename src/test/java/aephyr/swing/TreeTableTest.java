package aephyr.swing;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;

import aephyr.swing.TreeTable;
import aephyr.swing.event.TreeTableSorterEvent;
import aephyr.swing.event.TreeTableSorterListener;
import aephyr.swing.mnemonic.*;
import aephyr.swing.treetable.*;

public class TreeTableTest implements Runnable, ItemListener {
	
	private static final boolean PROPERTY_TABLE = false;
	
	private static final int COLUMN_COUNT = 4;
	
	private static final String LOOK_AND_FEEL = "Look & Feel";
	
	private static final String LEFT_TO_RIGHT = "Left to Right";
	
	private static final String RENDERER_TREE = "A: Tree Renderer";
	
	private static final String RENDERER_VARIABLE_HEIGHT = "C: Variable Row Height Renderer";

	public static void main(String[] args) throws Exception {
		if (printMethods(false))
			return;
		Utilities.setNimbusLookAndFeel();
//		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		SwingUtilities.invokeLater(new TreeTableTest());
	}
	
	private static boolean printMethods(boolean b) {
		if (b) {
			Set<String> treeTableSet = createSet(TreeTable.class);
			Set<String> tableSet = createSet(JTable.class);
			Set<String> treeSet = createSet(JTree.class);
			tableSet.removeAll(treeTableSet);
			treeSet.removeAll(treeTableSet);
			print("JTable:", tableSet);
			print("JTree:", treeSet);
		}
		return b;
	}
	
	private static void print(String title, Set<String> set) {
		System.out.println(title);
		for (String str : set) {
			System.out.print("\t");
			System.out.println(str);
		}
	}
	
	private static Set<String> createSet(Class<?> cls) {
		Method[] methods = cls.getDeclaredMethods();
		Set<String> set = new HashSet<String>(methods.length);
		StringBuilder s = new StringBuilder(200);
		for (Method m : methods) {
			if ((m.getModifiers() & java.lang.reflect.Modifier.PUBLIC) == 0)
				continue;
			s.append(m.getName()).append('(');
			Class<?>[] p = m.getParameterTypes();
			if (p.length > 0) {
				for (Class<?> c : p)
					s.append(c.getName()).append(',');
				s.deleteCharAt(s.length()-1);
			}
			s.append(')');
			set.add(s.toString());
			s.delete(0, s.length());
		}
		return set;
	}
	
	private static Random random = new Random();
	
	private static String createString() {
		char[] c = new char[random.nextInt(5)+4];
		for (int i=c.length; --i>=0;)
			c[i] = (char)('a'+random.nextInt(26));
		return new String(c);
	}
	
	@Override
	public void run() {
		treeTable = PROPERTY_TABLE ? createPropTreeTable() : createTreeTable();
		JScrollPane scroller = new JScrollPane(treeTable);
		JLabel corner = new JLabel(" ");
		corner.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				TreeTableSorter<?,?> sorter = treeTable.getRowSorter();
				if (sorter != null)
					sorter.setSortKeys(null);
			}
		});
		scroller.setCorner(JScrollPane.UPPER_TRAILING_CORNER, corner);
		Actions act = new Actions(Actions.PRINT_LINE);
		treeTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), act);
		treeTable.getActionMap().put(act, act);
		
		table = new JTable(treeTable.getTableModel());
		table.setDropMode(DropMode.INSERT_ROWS);
		table.setDragEnabled(true);
		table.setTransferHandler(new DummyTransferHandler());
		table.setPreferredScrollableViewportSize(new Dimension(100, 100));
		
		tree = new JTree(treeTable.getTreeTableModel());
		tree.setDropMode(DropMode.INSERT);
		tree.setDragEnabled(true);
		tree.setTransferHandler(new DummyTransferHandler());
		JScrollPane treeScroller = new JScrollPane(tree);
		treeScroller.setPreferredSize(new Dimension(200, 500));
		
		frame = new JFrame(getClass().getSimpleName());
		frame.setJMenuBar(createMenuBar());
		frame.add(scroller, BorderLayout.CENTER);
		frame.add(new JScrollPane(table), BorderLayout.SOUTH);
		frame.add(treeScroller, BorderLayout.EAST);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		new MnemonicGenerator().addMnemonics(frame.getRootPane());
		frame.setVisible(true);

	}
	
	private JFrame frame;
	
	private TreeTable treeTable;
	
	private JTable table;
	
	private JTree tree;
	
	private TreeTable createTreeTable() {
		
		DefaultTreeTableNode root = createNode(4, COLUMN_COUNT);
		
		TreeTable treeTable = new TreeTable(root);
		treeTable.setRootVisible(false);
		treeTable.setShowsRootHandles(true);
		// column 0 can't be a String because sometimes the value is wrapped
		// into a Header object so the Renderer knows to make it bold
		root.setValueAt(Object.class, 0);
		
		treeTable.setAutoCreateRowSorter(true);
		((DefaultTreeTableSorter)treeTable.getRowSorter()).setSortsOnUpdates(true);
		
		treeTable.setDragEnabled(true);
		treeTable.setDropMode(DropMode.INSERT_ROWS);
		treeTable.setTransferHandler(new DummyTransferHandler());
		
		treeTable.setAutoCreateRowHeader(true);
		
//		treeTable.setBackground(new Color(220, 245, 230));
//		treeTable.setAlternateRowColor(new Color(240, 255, 240));

		return treeTable;
	}
	
	private static class DummyTransferHandler extends TransferHandler {
		
		@Override
		public boolean canImport(TransferSupport support) {
//			if (support.isDrop()) {
//				return support.getDropLocation().getDropPoint().x < 200;
//			}
			return true;
		}
		
		@Override
		protected Transferable createTransferable(JComponent c) {
			return new StringSelection("dummy");
		}
		
		@Override
        public int getSourceActions(JComponent c) {
    	    return COPY | MOVE;
    	}
	}
	
//	private class RowHeader extends JTable {
//		
//		RowHeader(TableModel tm) {
//			super(new RowModel(tm));
//			setAutoCreateColumnsFromModel(false);
//			setRowMargin(0);
//			getColumnModel().setColumnMargin(0);
//			setFocusable(false);
//			updateRowHeight();
//			Handler h = new Handler();
//			if (treeTable.getRowSorter() != null)
//				treeTable.getRowSorter().addTreeTableSorterListener(h);
//			treeTable.addPropertyChangeListener("rowHeight", h);
//			treeTable.addPropertyChangeListener("rowSorter", h);
//		}
//		
//		private boolean variableRowHeights;
//		
//		public void updateUI() {
//			super.updateUI();
//			TableCellRenderer r = getTableHeader().getDefaultRenderer();
//			if (r instanceof JLabel) {
//				JLabel l = (JLabel)r;
//				l.setHorizontalAlignment(JLabel.CENTER);
//			}
//			getColumnModel().getColumn(0).setCellRenderer(r);
//			Dimension size = r.getTableCellRendererComponent(
//					RowHeader.this, "9999", false, false, -1, -1).getPreferredSize();
//			setPreferredScrollableViewportSize(size);
//			repaint();
//		}
//
//		
//		private void updateRowHeight() {
//			int rh = treeTable.getRowHeight();
//			variableRowHeights = rh <= 0;
//			if (variableRowHeights) {
//				updateRowHeights(0, getRowCount()-1);
//			} else {
//				setRowHeight(rh);
//			}
//		}
//		
//		public void tableChanged(TableModelEvent e) {
//			super.tableChanged(e);
//			if (e.getType() != TableModelEvent.DELETE) {
//				updateRowHeights(e.getFirstRow(), e.getLastRow());
//			}
//		}
//		
//		private void updateRowHeights(final int firstRow, final int lastRow) {
//			if (!variableRowHeights || firstRow < 0)
//				return;
//			SwingUtilities.invokeLater(new Runnable() {
//				public void run() {
//					// safety precaution
//					int last = Math.min(lastRow, getRowCount()-1);
//					for (int row=firstRow; row<=last; row++) {
//						setRowHeight(row, treeTable.getRowHeight(row));
//					}
//				}
//			});
//		}
//		
//		class Handler implements PropertyChangeListener, TreeTableSorterListener {
//			
//			@Override
//			public void propertyChange(PropertyChangeEvent evt) {
//				String name = evt.getPropertyName();
//				if (name == "rowHeight") {
//					updateRowHeight();
//				} else if (name == "rowSorter") {
//					TreeTableSorter<?,?> sorter = (TreeTableSorter<?,?>)evt.getOldValue();
//					if (sorter != null)
//						sorter.removeTreeTableSorterListener(this);
//					sorter = (TreeTableSorter<?,?>)evt.getNewValue();
//					if (sorter != null)
//						sorter.addTreeTableSorterListener(this);
//				}
//			}
//			
//			@Override
//			public void sorterChanged(TreeTableSorterEvent e) {
//				if (e.getType() == TreeTableSorterEvent.Type.SORT_ORDER_CHANGED) {
//					updateRowHeights(0, getRowCount()-1);
//				}
//			}
//		}
//		
//	}
//	
//	private static class RowModel extends AbstractTableModel implements TableModelListener {
//		
//		RowModel(TableModel tm) {
//			model = tm;
//			tm.addTableModelListener(this);
//		}
//		
//		private TableModel model;
//
//		@Override
//		public int getColumnCount() {
//			return 1;
//		}
//
//		@Override
//		public int getRowCount() {
//			return model.getRowCount();
//		}
//
//		@Override
//		public Object getValueAt(int rowIndex, int columnIndex) {
//			return rowIndex;
//		}
//
//		@Override
//		public void tableChanged(TableModelEvent e) {
//			fireTableChanged(new TableModelEvent(this,
//					e.getFirstRow(), e.getLastRow(), 0, e.getType()));
//		}
//		
//	}
	
	
	
	private class Actions extends AbstractAction {
		
		static final String PREVIOUS_VALUE = "Previous Value";
		
		static final String NEXT_VALUE = "Next Value";
		
		static final String SORT = "Sort Lead Column";
		
		static final String UNSORT = "Unsort";
		
		static final String PRINT_LINE = "Print Line";
		
		Actions(boolean next) {
			super(next ? NEXT_VALUE : PREVIOUS_VALUE);
		}
		
		Actions(String name) {
			super(name);
		}
		
		public void actionPerformed(ActionEvent e) {
			TreeTable t = treeTable;
			Object name = getValue(Action.NAME);
			if (name == SORT) {
				int col = Math.max(0, t.getColumnModel().getSelectionModel().getLeadSelectionIndex());
				t.getRowSorter().toggleSortOrder(col);
			} else if (name == UNSORT) {
				t.getRowSorter().setSortKeys(null);
			} else if (name == PRINT_LINE) {
				System.out.println();
			} else {
				TreePath p = t.getLeadSelectionPath();
				if (p != null && t.isPathSelected(p) &&
						p.getLastPathComponent() instanceof PropertyNode) {
					PropertyNode pn = (PropertyNode)p.getLastPathComponent();
					if (pn.getType() == "boolean") {
						t.getTreeColumnModel().setValueAt(
								Boolean.FALSE.equals(pn.getValue()), pn, 1);
					} else if (pn.getType() == "int") {
						int value = (Integer)pn.getValue();
						if (getValue(Action.NAME) == NEXT_VALUE) {
							value++;
						} else if (--value < 0) {
							return;
						}
						t.getTreeColumnModel().setValueAt(value, pn, 1);
					}
				}
			}
		}
		
	}
	
	private TreeTable createPropTreeTable() {
		DefaultTreeModel tm = new DefaultTreeModel(null);
		PropertyModel tcm = new PropertyModel(null);
		
		TreeTable treeTable = new TreeTable(tm, tcm);
		treeTable.setAutoCreateColumnsFromModel(false);
		
		TableColumnModel cm = treeTable.getColumnModel();
		cm.getColumn(0).setPreferredWidth(250);
		cm.getColumn(1).setPreferredWidth(200);
		cm.getColumn(2).setPreferredWidth(150);
		
		Node root = new Node(new Header("TreeTable"), null, null, treeTable);
		tcm.setRoot(root);
		tm.setRoot(root);

		InputMap inputs = treeTable.getInputMap();
		ActionMap actions = treeTable.getActionMap();
		Actions nv = new Actions(true);
		inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), nv);
		actions.put(nv, nv);
		Actions pv = new Actions(false);
		inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), pv);
		actions.put(pv, pv);

		cm.getColumn(0).setCellRenderer(new DefaultTreeTableCellRenderer() {
			private Font clsFont;
			
			protected void setValue(Object value) {
				setText(value.toString());
				if (value instanceof Header) {
					if (clsFont == null)
						clsFont = getFont().deriveFont(Font.BOLD | Font.ITALIC);
					setFont(clsFont);
					if (isEnabled()) {
						setIcon(null);
					} else {
						setDisabledIcon(null);
					}
				}
			}
			
			public void updateUI() {
				super.updateUI();
				clsFont = null;
			}
		});
		
		return treeTable;
	}
	
	private static int createInt() {
		return 15 + random.nextInt(25);	
	}
	
	private static DefaultTreeTableNode createNode(int depth, int columns) {
		Object[] rowData = new Object[columns];
		for (int i=0; i<columns-2; i++)
			rowData[i] = createString();
		rowData[columns-2] = createInt();
		rowData[columns-1] = random.nextBoolean();
		DefaultTreeTableNode node = new DefaultTreeTableNode(rowData);
		node.setUserObject(rowData[0]);
		if (--depth >= 0)
			for (int i=3+random.nextInt(5); --i>=0;)
				node.add(createNode(depth, columns));
		return node;
	}
	
	
	private JMenuBar createMenuBar() {
		JMenuBar bar = new JMenuBar();
		JMenu laf = new JMenu(LOOK_AND_FEEL);
		ButtonGroup group = new ButtonGroup();
		String lafClassName = UIManager.getLookAndFeel().getClass().getName();
		ItemListener itemLis = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					updateLookAndFeel((JMenuItem)e.getSource());
				}
			}
		};
		for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(
					info.getName(), lafClassName.equals(info.getClassName()));
			group.add(item);
			item.putClientProperty(LOOK_AND_FEEL, info.getClassName());
			item.addItemListener(itemLis);
			laf.add(item);
		}
		JCheckBoxMenuItem ltr = new JCheckBoxMenuItem("Left to Right", true);
		ltr.addItemListener(this);
		laf.addSeparator();
		laf.add(ltr);
		bar.add(laf);
		
		JMenu properties = new JMenu("Properties");
		JMenu booleans = new JMenu("Booleans");
		JMenu integers = new JMenu("Integers");
		JMenu enumerations = new JMenu("Enumerations");
		itemLis = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				changeProperty(((JMenuItem)e.getSource()).getText(), 
						e.getStateChange() == ItemEvent.SELECTED);
			}
		};
		add(booleans, integers, treeTable, itemLis, Arrays.asList(
				"AutoResizeMode", "CellSelectionEnabled", "DebugGraphicsOptions"));
		integers.addSeparator();
		add(null, integers, treeTable.getColumnModel(), null, Collections.emptyList());
		
		JMenu autoResizeMode = new JMenu("AutoResizeMode");
		ButtonGroup resizeModes = new ButtonGroup();
		itemLis = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					JMenuItem item = (JMenuItem)e.getSource();
					int m = (Integer)getStaticField(
							JTable.class, item.getText());
					treeTable.setAutoResizeMode(m);
					table.setAutoResizeMode(m);
				}
			}
		};
		int resizeMode = treeTable.getAutoResizeMode();
		autoResizeMode.add(createEnumButton("AUTO_RESIZE_ALL_COLUMNS",
				resizeMode == JTable.AUTO_RESIZE_ALL_COLUMNS, resizeModes, itemLis));
		autoResizeMode.add(createEnumButton("AUTO_RESIZE_LAST_COLUMN",
				resizeMode == JTable.AUTO_RESIZE_LAST_COLUMN, resizeModes, itemLis));
		autoResizeMode.add(createEnumButton("AUTO_RESIZE_NEXT_COLUMN",
				resizeMode == JTable.AUTO_RESIZE_NEXT_COLUMN, resizeModes, itemLis));
		autoResizeMode.add(createEnumButton("AUTO_RESIZE_OFF",
				resizeMode == JTable.AUTO_RESIZE_OFF, resizeModes, itemLis));
		autoResizeMode.add(createEnumButton("AUTO_RESIZE_SUBSEQUENT_COLUMNS",
				resizeMode == JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS, resizeModes, itemLis));
		enumerations.add(autoResizeMode);

		JMenu dropMode = new JMenu("DropMode");
		ButtonGroup dropModes = new ButtonGroup();
		itemLis = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					JMenuItem item = (JMenuItem)e.getSource();
					DropMode dm = (DropMode)getStaticField(
							DropMode.class, item.getText());
					treeTable.setDropMode(dm);
					table.setDropMode(dm);
				}
			}
		};
		DropMode mode = treeTable.getDropMode();
		for (DropMode dm : DropMode.values()) {
			if (dm != DropMode.USE_SELECTION)
				dropMode.add(createEnumButton(dm.name(), mode == dm, dropModes, itemLis));
		}
		enumerations.add(dropMode);
		
		properties.add(booleans);
		properties.add(integers);
		properties.add(enumerations);
		bar.add(properties);
		
		if (PROPERTY_TABLE)
			return bar;
		
		JMenu renderers = new JMenu("Cell Renderers");
		JCheckBoxMenuItem a = new JCheckBoxMenuItem(RENDERER_TREE, false);
		a.addItemListener(this);
		renderers.add(a);
		JCheckBoxMenuItem c = new JCheckBoxMenuItem(RENDERER_VARIABLE_HEIGHT, false);
		c.addItemListener(this);
		renderers.add(c);
		bar.add(renderers);
		
		JMenu modify = new JMenu("Modify");
		for (Modifier m : Modifier.values()) {
			String text = m.toString();
			JMenuItem item = new JMenuItem(text);
			item.putClientProperty(TreeTable.class, treeTable);
			item.addActionListener(m);
			item.setAccelerator(KeyStroke.getKeyStroke(
					text.charAt(0), InputEvent.SHIFT_DOWN_MASK));
			modify.add(item);
		}
		bar.add(modify);
		
		
		JMenu sort = new JMenu("Sort");
		sort.add(new Actions(Actions.SORT)).setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		sort.add(new Actions(Actions.UNSORT)).setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK));
		bar.add(sort);
		
		return bar;
	}
	
	static void put(HashMap<String,Object[]> map, String key, Object obj, int idx) {
		Object[] value = map.get(key);
		if (value == null) {
			value = new Object[3];
			map.put(key, value);
		}
		value[idx] = obj;
	}
	
	private static JRadioButtonMenuItem createEnumButton(
			String name, boolean sel, ButtonGroup group, ItemListener lis) {
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(name, sel);
		group.add(item);
		item.addItemListener(lis);
		return item;
	}
	
	private static Object getStaticField(Class<?> cls, String name) {
		try {
			return cls.getDeclaredField(name).get(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static void add(JMenu booleans, JMenu integers, Object obj, ItemListener lis, List<?> ignore) {
		HashMap<String,Object[]> bools = booleans == null ? null : new HashMap<String,Object[]>();
		HashMap<String,Object[]> ints = new HashMap<String,Object[]>();
		Class<?> cls = Node.getRealClass(obj.getClass());
		for (Method m : cls.getMethods()) {
			if ((m.getModifiers() & java.lang.reflect.Modifier.PUBLIC) == 0)
				continue;
			String name = m.getName();
			if (name.startsWith("set")) {
				Class<?>[] t = m.getParameterTypes();
				if (t.length == 1) {
					if (bools != null && t[0] == boolean.class) {
						put(bools, name.substring(3), m, 1);
					} else if (t[0] == int.class) {
						put(ints, name.substring(3), m, 1);
					}
				}
			} else if (name.startsWith("get")) {
				if (m.getParameterTypes().length == 0) {
					Class<?> t = m.getReturnType();
					if (bools != null && t == boolean.class) {
						put(bools, name.substring(3), m, 0);
					} else if (t == int.class) {
						put(ints, name.substring(3), m, 0);
					}
				}
			} else if (bools != null && name.startsWith("is")) {
				if (m.getParameterTypes().length == 0) {
					put(bools, name.substring(2), m, 0);
				}
			}
		}
		for (Map.Entry<String,Object[]> entry : ints.entrySet()) {
			Object[] m = entry.getValue();
			if (m[1] != null && m[0] != null) {
				if (ignore.contains(entry.getKey()))
					continue;
				try {
					Method method = (Method)m[0];
					integers.add(new SpinnerMenuItem(
							entry.getKey(), obj, (Integer)method.invoke(obj), 0, 99));
				} catch (Exception e) {
					System.err.println(entry.getKey());
					e.printStackTrace();
				}
			}
		}
		if (bools == null)
			return;
		List<JMenuItem> secondary = new ArrayList<JMenuItem>();
		for (Map.Entry<String,Object[]> entry : bools.entrySet()) {
			Object[] m = entry.getValue();
			if (m[1] != null && m[0] != null) {
				if (ignore.contains(entry.getKey()))
					continue;
				try {
					Method method = (Method)m[0];
					JMenuItem item = new JCheckBoxMenuItem(entry.getKey(), (Boolean)method.invoke(obj));
					item.addItemListener(lis);
					if (((Method)m[1]).getDeclaringClass() != cls) {
						secondary.add(item);
					} else {
						booleans.add(item);
					}
				} catch (Exception e) {
					System.err.println(entry.getKey());
					e.printStackTrace();
				}
			}
		}
		for (JMenuItem item : secondary)
			booleans.add(item);
	}
	
	private void changeProperty(String name, boolean value) {
		try {
			TreeTable.class.getMethod("set"+name, boolean.class)
					.invoke(treeTable, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean getBooleanValue(String name) {
		return (Boolean)getValue(treeTable, name);
	}
	
	private static Object getValue(Object obj, String name) {
		try {
			return obj.getClass().getMethod("get"+name).invoke(obj);
		} catch (Exception e) {
			try {
				return obj.getClass().getMethod("is"+name).invoke(obj);
			} catch (Exception x) {
				throw new RuntimeException(x);
			}
		}
	}
	
	private void updateLookAndFeel(JMenuItem item) {
		try {
			UIManager.setLookAndFeel((String)item.getClientProperty(LOOK_AND_FEEL));
			SwingUtilities.updateComponentTreeUI(frame);
		} catch (Exception x) {
			x.printStackTrace();
		}
	}
	
	
	
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		String txt = ((JMenuItem)e.getSource()).getText();
		boolean sel = e.getStateChange() == ItemEvent.SELECTED;
		if (txt == LEFT_TO_RIGHT) {
			treeTable.setComponentOrientation(sel ?
					ComponentOrientation.LEFT_TO_RIGHT :
					ComponentOrientation.RIGHT_TO_LEFT);
		} else if (txt == RENDERER_VARIABLE_HEIGHT) {
			setRenderer(treeTable.getColumnModel().getColumnCount() - 2, sel ?
					new VariableRowHeightRenderer() : null);
			treeTable.setRowHeight(-treeTable.getRowHeight());
		} else if (txt == RENDERER_TREE) {
			setRenderer(0, sel ? new TreeRenderer() : null);
		}
	}
	
	private static class TreeRenderer extends DefaultTreeTableCellRenderer {
		private Font bold;
		
		protected void setValue(Object value) {
			setText(value.toString());
			if (value instanceof Header) {
				if (bold == null)
					bold = getFont().deriveFont(Font.BOLD);
				setFont(bold);
			}
		}
	}
	
	private static class TreeEditor extends DefaultTreeTableCellEditor {

		public TreeEditor() {
			super(new JTextField());

		}

//		@Override
//		public Component getTreeTableCellEditorComponent(TreeTable treeTable,
//				Object value, boolean isSelected, int row, int column) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public Component getTreeTableCellEditorComponent(TreeTable treeTable,
//				Object value, boolean isSelected, int row, int column,
//				boolean expanded, boolean leaf) {
//			// TODO Auto-generated method stub
//			return null;
//		}
		
	}
	
	
	
	
	private static class VariableRowHeightRenderer extends DefaultTreeTableCellRenderer {
		
		VariableRowHeightRenderer() {
			setHorizontalAlignment(TRAILING);
		}
		
		public Dimension getPreferredSize() {
			Dimension size = super.getPreferredSize();
			size.height = Integer.parseInt(getText());
			return size;
		}

	}
	
	private void setRenderer(int col, DefaultTreeTableCellRenderer renderer) {
		TableColumn tc = treeTable.getColumnModel().getColumn(
				treeTable.convertColumnIndexToView(col));
		tc.setCellRenderer(renderer);
		treeTable.repaint();
	}
	
	
	private enum Modifier implements ActionListener {
		
		ADD_NODE, REMOVE_NODE, CHANGE_NODE, STRUCTURE_CHANGE;
		
		private Modifier() {
			char[] c = name().toCharArray();
			for (int i=1; i<c.length; i++) {
				if (c[i] == '_') {
					c[i] = ' ';
					i++;
				} else {
					c[i] = Character.toLowerCase(c[i]);
				}
			}
			text = new String(c);
		}
		
		private String text;
		
		public String toString() {
			return text;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			JComponent c = (JComponent)e.getSource();
			TreeTable treeTable = (TreeTable)c.getClientProperty(TreeTable.class);
			if (this == STRUCTURE_CHANGE) {
				changeLeadStructure(treeTable);
			} else {
				modify(treeTable);
			}
			
		}
		
		void changeLeadStructure(TreeTable treeTable) {
			TreePath path = treeTable.getLeadSelectionPath();
			if (path == null) {
				error("No Lead Path.");
				return;
			}
			DefaultTreeModel tm = (DefaultTreeModel)treeTable.getTreeModel();
			DefaultTreeTableNode n = (DefaultTreeTableNode)path.getLastPathComponent();
			n.removeAllChildren();
			DefaultTreeTableNode a = createNode(2, COLUMN_COUNT);
			for (int i=a.getChildCount(); --i>=0;)
				n.add((MutableTreeNode)a.getChildAt(i));
			tm.nodeStructureChanged(n);
		}
		
		void addNodeAfterLead(TreeTable treeTable) {
			TreePath path = treeTable.getLeadSelectionPath();
			if (path == null) {
				error("No Lead Path.");
				return;
			}
			DefaultTreeModel tm = (DefaultTreeModel)treeTable.getTreeModel();
			DefaultTreeTableNode parent;
			int index;
			if (path.getPathCount() == 1) {
				parent = (DefaultTreeTableNode)path.getLastPathComponent();
				index = 0;
			} else {
				parent = (DefaultTreeTableNode)path.getParentPath().getLastPathComponent();
				index = parent.getIndex((TreeNode)path.getLastPathComponent()) + 1;
			}
			tm.insertNodeInto(createNode(1, COLUMN_COUNT), parent, index);
		}
		
		void addNodeToRoot(TreeTable treeTable) {
			DefaultTreeModel tm = (DefaultTreeModel)treeTable.getTreeModel();
			DefaultTreeTableNode parent = (DefaultTreeTableNode)tm.getRoot();
			tm.insertNodeInto(createNode(3, COLUMN_COUNT), parent, parent.getChildCount());
		}
		
		void changeRoot(TreeTable treeTable) {
			DefaultTreeModel tm = (DefaultTreeModel)treeTable.getTreeModel();
			tm.setRoot(createNode(4, COLUMN_COUNT));
		}
		
		void modify(TreeTable treeTable) {
			TreePath[] paths = treeTable.getSelectionPaths();
			if (paths == null || paths.length == 0) {
				switch (this) {
				case ADD_NODE:
					addNodeAfterLead(treeTable);
					return;
				}
				error("No selection.");
				return;
			}
			TreePath parent = paths[0].getParentPath();
			if (parent == null) {
				switch (this) {
				case ADD_NODE:
					addNodeToRoot(treeTable);
					break;
				case REMOVE_NODE:
					error("Cannot remove root.");
					break;
				case CHANGE_NODE:
					changeRoot(treeTable);
					break;
				}
				return;
			}
			int row = treeTable.getMinSelectionRow();
			int i = 0;
			int last = -1;
			DefaultTreeModel tm = (DefaultTreeModel)treeTable.getTreeModel();
			DefaultTreeColumnModel cm = (DefaultTreeColumnModel)treeTable.getTreeColumnModel();
			for (; i<paths.length; i++) {
				TreePath p = paths[i].getParentPath();
				if (!parent.equals(p)) {
					modify(treeTable, tm, cm, parent, paths, i - 1, last);
					last = i - 1;
					parent = p;
				}
			}
			modify(treeTable, tm, cm, parent, paths, i - 1, last);
			if (this == REMOVE_NODE)
				treeTable.setSelectionRow(Math.min(row, treeTable.getRowCount()-1));
		}
		
		void modify(TreeTable treeTable, final DefaultTreeModel tm, final DefaultTreeColumnModel cm,
				TreePath parent, TreePath[] paths, int i, int last) {
			final DefaultTreeTableNode[] childNodes = new DefaultTreeTableNode[i-last];
			int[] childIndices = new int[i-last];
			final MutableTreeNode parentNode = (MutableTreeNode)parent.getLastPathComponent();
			for (int j=i; j>last; j--) {
				int idx = j-last-1;
				Object node = paths[j].getLastPathComponent();
				childIndices[idx] = tm.getIndexOfChild(parentNode, node);
			}
			Arrays.sort(childIndices);
			for (int j=childIndices.length; --j>=0;)
				childNodes[j] = (DefaultTreeTableNode)tm.getChild(parentNode, childIndices[j]);
			switch (this) {
			case ADD_NODE:
				int count = 0;
				for (int j=0; j<childIndices.length; j++) {
					childIndices[j] += count++;
					DefaultTreeTableNode node = createNode(1, COLUMN_COUNT);
					node.setValueAt(new Header(node.getValueAt(0).toString()), 0);
					childNodes[j] = node;
					parentNode.insert(node, childIndices[j]);
				}
				tm.nodesWereInserted(parentNode, childIndices);
				ActionListener lis = new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						for (DefaultTreeTableNode node : childNodes) {
							int idx = tm.getIndexOfChild(parentNode, node);
							if (idx < 0)
								continue;
							cm.setValueAt(cm.getValueAt(node, 0).toString(), node, 0);
						}
					}
				};
				Timer timer = new Timer(600, lis);
				timer.setRepeats(false);
				timer.start();
				break;
			case REMOVE_NODE:
//				System.err.println("remove " + Arrays.toString(childIndices) + " " + string(cm, childNodes));
				for (int j=childIndices.length; --j>=0;)
					parentNode.remove(childIndices[j]);
				tm.nodesWereRemoved(parentNode, childIndices, childNodes);
				break;
			case CHANGE_NODE:
				int col = treeTable.getColumnModel().getSelectionModel().getLeadSelectionIndex();
				col = col < 0 || !treeTable.isColumnFocusEnabled() ? 0 :
					treeTable.convertColumnIndexToModel(col);
				Class<?> cls = cm.getColumnClass(col);
				for (int j=childNodes.length; --j>=0;) {
					childNodes[j].setValueAt(createValue(cls), col);
				}
				tm.nodesChanged(parentNode, childIndices);
				break;
			}

		}
		
		private Object createValue(Class<?> cls) {
			if (cls == Boolean.class)
				return random.nextBoolean();
			if (cls == Integer.class)
				return createInt();
			return createString();
		}
		
		void error(String str) {
			JOptionPane.showConfirmDialog(null, str, str, JOptionPane.OK_CANCEL_OPTION);
		}
		
	}

	public static String string(TreeColumnModel cm, Object ... nodes) {
		StringBuilder s = new StringBuilder().append('[');
		for (int i=0;;) {
			s.append(cm.getValueAt(nodes[i], 0));
			if (++i >= nodes.length)
				break;
			s.append(',');
		}
		return s.append(']').toString();
	}

	
	private static class SpinnerMenuItem extends JMenuItem
			implements MenuKeyListener, ChangeListener {
		
		SpinnerMenuItem(String text, Object obj, int value, int lowerBound, int upperBound) {
			super(text);
			object = obj;
			addMenuKeyListener(this);
			SpinnerNumberModel model = new SpinnerNumberModel(
					value, lowerBound, upperBound, 1);
			model.addChangeListener(this);
			spinner = new JSpinner(model);
			setLayout(new BorderLayout());
			add(spinner, BorderLayout.EAST);
		}
		
		private Object object;
		
		private JSpinner spinner;

		@Override
		public void menuKeyPressed(MenuKeyEvent e) {
			if (e.isControlDown() && isArmed()) {
				Object value;
				switch (e.getKeyCode()) {
				default: return;
				case KeyEvent.VK_DOWN: case KeyEvent.VK_LEFT:
					value = spinner.getPreviousValue(); break;
				case KeyEvent.VK_UP: case KeyEvent.VK_RIGHT:
					value = spinner.getNextValue(); break;
				}
				if (value != null) {
					spinner.setValue(value);
					e.consume();
				}
			}
		}

		@Override
		public void menuKeyReleased(MenuKeyEvent e) {}

		@Override
		public void menuKeyTyped(MenuKeyEvent e) {}
		
		@Override
		public void stateChanged(ChangeEvent e) {
			Integer value = (Integer)spinner.getValue();
			try {
				object.getClass().getMethod("set"+getText(), int.class)
					.invoke(object, value);
			} catch (Exception x) {
				x.printStackTrace();
			}
		}
		
		public Dimension getPreferredSize() {
			Dimension size = super.getPreferredSize();
			Dimension ss = spinner.getPreferredSize();
			size.width += ss.width + 10;
			size.height = Math.max(size.height, ss.height);
			return size;
		}

	}
}




class Header {
	
	Header(String title) {
		this.title = title;
	}
	
	private String title;
	
	public String toString() {
		return title;
	}
}

class PropertyModel extends AbstractTreeColumnModel {

	PropertyModel(Object root) {
		this.root = root;
	}
	
	private Object root;
	
	public void setRoot(Object root) {
		this.root = root;
	}
	
	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {
		case 0: return "Name";
		case 1: return "Value";
		case 2: return "Type";
		}
		throw new IllegalArgumentException();
	}

	@Override
	public Object getValueAt(Object node, int column) {
		Node pn = (Node)node;
		switch (column) {
		case 0: return pn.getName();
		case 1: return pn.getValue();
		case 2: return pn.getType();
		}
		throw new IllegalArgumentException();
	}

	@Override
	public boolean isCellEditable(Object node, int column) {
		return column == 1 && node instanceof PropertyNode;
	}

	@Override
	public void setValueAt(Object value, Object node, int column) {
		PropertyNode pn = (PropertyNode)node;
		if (pn.setValue(value))
			fireTreeColumnChanged(pathToRoot(root, pn), column);
	}
	
}


class Node extends DefaultMutableTreeNode {
	
	Node(String name, Object value) {
		this(name, value, null, null);
	}
	
	Node(Object nam, Object val, Object typ, Object userObject) {
		super(userObject);
		name = nam;
		value = val;
		type = typ;
	}
	
	private Object name;
	
	Object value;
	
	private Object type;
	
	private boolean loaded = false;
	
	Object getName() {
		return name;
	}
	
	Object getValue() {
		return value;
	}
	
	Object getType() {
		return type;
	}
	
	public int getChildCount() {
		if (!loaded) {
			loaded = true;
			load(getUserObject(), null);
		}
		return super.getChildCount();
	}
	
	static Class<?> getRealClass(Class<?> cls) {
		while ("".equals(cls.getSimpleName()))
			cls = cls.getSuperclass();
		return cls;
	}
	
	private void load(Object obj, Class<?> cls) {
		if (obj == null)
			return;
		if (cls == null)
			cls = getRealClass(obj.getClass());
		HashMap<String,Object[]> objs = new HashMap<String,Object[]>();
		HashMap<String,Object[]> bools = new HashMap<String,Object[]>();
		HashMap<String,Object[]> ints = new HashMap<String,Object[]>();
		for (Method m : cls.getDeclaredMethods()) {
			if ((m.getModifiers() & java.lang.reflect.Modifier.PUBLIC) == 0)
				continue;
			String name = m.getName();
			if (name.startsWith("set")) {
				Class<?>[] t = m.getParameterTypes();
				if (t.length == 1) {
					if (bools != null && t[0] == boolean.class) {
						TreeTableTest.put(bools, name.substring(3), m, 1);
					} else if (t[0] == int.class) {
						TreeTableTest.put(ints, name.substring(3), m, 1);
					} else if (Object.class.isAssignableFrom(t[0])) {
						String key = name.substring(3);
						TreeTableTest.put(objs, key, m, 1);
						TreeTableTest.put(objs, key, t[0].getSimpleName(), 2);
					}
				}
			} else if (name.startsWith("get")) {
				if (m.getParameterTypes().length == 0) {
					Class<?> t = m.getReturnType();
					if (bools != null && t == boolean.class) {
						TreeTableTest.put(bools, name.substring(3), m, 0);
					} else if (t == int.class) {
						TreeTableTest.put(ints, name.substring(3), m, 0);
					} else if (Object.class.isAssignableFrom(t)) {
						TreeTableTest.put(objs, name.substring(3), m, 0);
					}
				}
			} else if (name.startsWith("is")) {
				if (m.getParameterTypes().length == 0) {
					TreeTableTest.put(bools, name.substring(2), m, 0);
				}
			}
		}
		Object type = "boolean";
		for (Map.Entry<String,Object[]> entry : bools.entrySet()) {
			Object[] m = entry.getValue();
			if (m[1] != null && m[0] != null) {
				try {
					Method method = (Method)m[0];
					add(new PropertyNode(entry.getKey(), method.invoke(obj), type, (Method)m[1]));
				} catch (Exception e) {
					System.err.println(entry.getKey());
					e.printStackTrace();
				}
			}
		}
		type = "int";
		for (Map.Entry<String,Object[]> entry : ints.entrySet()) {
			Object[] m = entry.getValue();
			if (m[1] != null && m[0] != null) {
				try {
					Method method = (Method)m[0];
					add(new PropertyNode(entry.getKey(), method.invoke(obj), type, (Method)m[1]));
				} catch (Exception e) {
					System.err.println(entry.getKey());
					e.printStackTrace();
				}
			}
		}
		for (Map.Entry<String,Object[]> entry : objs.entrySet()) {
			Object[] m = entry.getValue();
			if (m[1] != null && m[0] != null) {
				try {
					Method method = (Method)m[0];
					Object value = method.invoke(obj);
					if (value != null)
						add(new Node(entry.getKey(),
								getRealClass(value.getClass()).getSimpleName(),
								m[2], value));
				} catch (Exception e) {
					System.err.println(entry.getKey());
					e.printStackTrace();
				}
			}
		}
		cls = cls.getSuperclass();
		if (cls != Object.class) {
			add(new Node(new Header(cls.getSimpleName()), null, null, null));
			load(obj, cls);
		}
	}
	
}

class PropertyNode extends Node {
	
	PropertyNode(String name, Object value, Object type, Method set) {
		super(name, value, type, null);
		setter = set;
		setAllowsChildren(false);
	}
	
	private Method setter;
	
	boolean setValue(Object val) {
		Object oldValue = getValue();
		try {
			val = AbstractTreeColumnModel.convertValue(val, oldValue.getClass());
			if (!oldValue.equals(val)) {
				value = val;
				DefaultMutableTreeNode par = (DefaultMutableTreeNode)parent;
				setter.invoke(par.getUserObject(), val);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public boolean isLeaf() {
		return true;
	}
	
	@Override
	public int getChildCount() {
		return 0;
	}
	
}

