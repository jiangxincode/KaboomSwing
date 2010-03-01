package test.aephyr.swing;

import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.*;
import javax.swing.GroupLayout.Alignment;

import com.sun.java.swing.Painter;

public class NimbusThemeCreator implements
		ActionListener, ItemListener, PropertyChangeListener, TableModelListener {
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setNimbusLookAndFeel();
				JFrame frame = new JFrame(NimbusThemeCreator.class.getSimpleName());
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.add(new NimbusThemeCreator().createBody(), BorderLayout.CENTER);
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});
	}
	
	private static void setNimbusLookAndFeel() {
		try {
			for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					return;
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		throw new IllegalStateException();
	}
	
	private JComponent createBody() {
		UIDefaults def = UIManager.getLookAndFeel().getDefaults();
		List<String> primary = new ArrayList<String>();
		List<String> secondary = new ArrayList<String>();
		List<String> other = new ArrayList<String>();
		Set<String> filters = new HashSet<String>();
		for (Map.Entry<Object,Object> entry : def.entrySet()) {
			if (entry.getKey() instanceof String) {
				String str = (String)entry.getKey();
				if (Character.isLowerCase(str.charAt(0))) {
					if (entry.getValue() instanceof Color) {
						if (entry.getValue() instanceof ColorUIResource) {
							primary.add(str);
						} else {
							secondary.add(str);
						}
					} else {
						other.add(str);
					}
				} else {
					int i = str.indexOf('.');
					if (i >= 0) {
						other.add(str);
						if (Character.isLetter(str.charAt(0))) {
							int j = str.indexOf('[');
							if (j >= 0 && j < i)
								i = j;
							j = str.indexOf(':');
							if (j >= 0 && j < i)
								i = j;
							filters.add(str.substring(0, i));
						}
					}
				}
			}
		}
		TableCellRenderer renderer = new UIDefaultsRenderer();
		TableCellEditor editor = new UIDefaultsEditor();
		primaryTable = createUITable(0, "Color", primary, renderer, editor);
		primaryTable.getModel().addTableModelListener(this);
		secondaryTable = createUITable(0, "Color", secondary, renderer, editor);
		otherTable = createUITable(75, "Value", other, renderer, editor);
		otherTable.setAutoCreateRowSorter(true);
		DefaultRowSorter<?,?> sorter = (DefaultRowSorter<?,?>)otherTable.getRowSorter();
		sorter.setSortable(2, false);

		String[] filterArray = filters.toArray(new String[filters.size()+1]);
		filterArray[filterArray.length-1] = "";
		Arrays.sort(filterArray);
		keyFilter = new JComboBox(filterArray);
		keyFilter.setToolTipText("Filter Key Column");
		keyFilter.setEditable(true);
		keyFilter.addActionListener(this);
		keyFilterType = new JComboBox(new Object[]{"Starts With","Ends With","Contains","Regex"});
		keyFilterType.addActionListener(this);
		Object[] types = Type.values();
		Object[] typeArray = new Object[types.length+1];
		System.arraycopy(types, 0, typeArray, 1, types.length);
		typeArray[0] = "";
		typeFilter = new JComboBox(typeArray);
		typeFilter.setToolTipText("Filter Type Column");
		typeFilter.addActionListener(this);
		
		update = new JButton("Update UI");
		update.addActionListener(this);
		autoUpdate = new JCheckBox("Auto Update", false);
		autoUpdate.addItemListener(this);
		
		return createBodyLayout();
	}
	
	
	private JComponent createBodyLayout() {
		JScrollPane primary = new JScrollPane(primaryTable);
		primary.setBorder(BorderFactory.createTitledBorder("Primary"));
		JScrollPane secondary = new JScrollPane(secondaryTable);
		secondary.setBorder(BorderFactory.createTitledBorder("Secondary"));
		// GroupLayout is used because vertical Box behaves poorly (what a surprise)
		// when the horizontal scroll bar appears/disappears for the tables
		JPanel colors = new JPanel(null);
		GroupLayout layout = new GroupLayout(colors);
		colors.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(primary).addComponent(secondary));
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(primary).addComponent(secondary));

		JScrollPane other = new JScrollPane(otherTable);
		other.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		JPanel otherPanel = new JPanel(null);
		
		JPanel filters = new JPanel(new FiltersLayout());
		filters.add(keyFilter);
		filters.add(keyFilterType);
		filters.add(typeFilter);
		otherTable.getColumnModel().getColumn(0).addPropertyChangeListener(this);
		
		layout = new GroupLayout(otherPanel);
		otherPanel.setLayout(layout);
		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addGap(2)
				.addGroup(layout.createParallelGroup()
						.addComponent(filters).addComponent(other)));
		final int sz = GroupLayout.PREFERRED_SIZE;
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGap(2).addComponent(other).addComponent(filters, sz, sz, sz));

		JTabbedPane options = new JTabbedPane();
		options.addTab("UI Base", colors);
		options.addTab("UI Controls", otherPanel);
		JComponent preview = createPreview();
		
		JPanel body = new JPanel(null);
		layout = new GroupLayout(body);
		body.setLayout(layout);
		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup()
						.addComponent(options)
						.addGroup(layout.createSequentialGroup()
								.addGap(4)
								.addComponent(update)
								.addComponent(autoUpdate)))
				.addComponent(preview));
		layout.setVerticalGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
						.addComponent(options)
						.addGroup(layout.createBaselineGroup(false, true)
								.addComponent(update)
								.addComponent(autoUpdate))
						.addGap(4))
				.addComponent(preview));
		return body;
	}
	
	private JComponent createPreview() {
		List<String> keys = new ArrayList<String>();
		String prototype = null;
		for (Object key : UIManager.getLookAndFeel().getDefaults().keySet()) {
			if (key instanceof String) {
				String str = (String)key;
				if (prototype == null && str.length() > 50 && str.length() < 60)
					prototype = str;
				keys.add(str);
			}
		}
		String[] array = keys.toArray(new String[keys.size()]);
		Arrays.sort(array);
		JList list = new JList(array);
		list.setPrototypeCellValue(prototype);
		JTree tree = new JTree();
		for (int row=0; row<tree.getRowCount(); row++)
			tree.expandRow(row);
		JSplitPane collections = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(list), new JScrollPane(tree));
		
		JTextArea area = new JTextArea(10, 40);
		JTextPane editor = new JTextPane();
		JSplitPane textTab = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(area), new JScrollPane(editor));
		
		JLabel label1 = new JLabel("Hover Here for Tooltip:");
		label1.setToolTipText("Tooltip");
		JLabel label2 = disabled(new JLabel("Disabled Label:"));
		JButton button1 = new JButton("Button");
		JButton button2 = disabled(new JButton("Disabled"));
		JToggleButton toggle1 = new JToggleButton("Toggle", true);
		JToggleButton toggle2 = new JToggleButton("Toggle", false);
		JToggleButton toggle3 = disabled(new JToggleButton("Disabled", true));
		JToggleButton toggle4 = disabled(new JToggleButton("Disabled", false));
		JRadioButton radio1 = new JRadioButton("Radio", true);
		JRadioButton radio2 = new JRadioButton("Radio", false);
		JRadioButton radio3 = disabled(new JRadioButton("Disabled", true));
		JRadioButton radio4 = disabled(new JRadioButton("Disabled", false));
		JCheckBox check1 = new JCheckBox("Check", true);
		JCheckBox check2 = new JCheckBox("Check", false);
		JCheckBox check3 = disabled(new JCheckBox("Disabled", true));
		JCheckBox check4 = disabled(new JCheckBox("Disabled", false));
		JPopupMenu popup = new JPopupMenu();
		JMenu menu = new JMenu("Menu");
		menu.add("Item");
		popup.add(menu);
		popup.add(new JMenuItem("Item"));
		JMenuItem item1 = new JMenuItem("Accelerator");
		item1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
		popup.add(item1);
		popup.add(disabled(new JMenuItem("Disabled")));
		popup.addSeparator();
		popup.add(new JRadioButtonMenuItem("Radio", true));
		popup.add(new JRadioButtonMenuItem("Radio", false));
		popup.add(disabled(new JRadioButtonMenuItem("Disabled", false)));
		popup.addSeparator();
		popup.add(new JCheckBoxMenuItem("Check", true));
		popup.add(new JCheckBoxMenuItem("Check", false));
		popup.add(disabled(new JCheckBoxMenuItem("Disabled", false)));
		JSlider slider1 = new JSlider();
		JSlider slider2 = disabled(new JSlider());
		JSlider slider3 = tickedSlider(false);
		JSlider slider4 = disabled(tickedSlider(false));
		JSlider slider5 = tickedSlider(true);
		JSlider slider6 = disabled(tickedSlider(true));
		JTextField text1 = new JTextField("Click Here for Popup");
		text1.setComponentPopupMenu(popup);
		JTextField text2 = disabled(new JTextField("Disabled TextField"));
		JSpinner spinner1 = new JSpinner(new SpinnerNumberModel(100, 0, Short.MAX_VALUE, 100));
		JSpinner spinner2 = disabled(new JSpinner(new SpinnerNumberModel(100, 0, Short.MAX_VALUE, 100)));
		JSpinner spinner3 = new JSpinner(new SpinnerDateModel());
		JSpinner spinner4 = disabled(new JSpinner(new SpinnerDateModel()));
		JSpinner spinner5 = new JSpinner(new SpinnerListModel(Type.values()));
		JSpinner spinner6 = disabled(new JSpinner(new SpinnerListModel(Type.values())));
		JComboBox combo1 = new JComboBox(Type.values());
		JComboBox combo2 = disabled(new JComboBox(Type.values()));
		JComboBox combo3 = new JComboBox(Type.values());
		combo3.setEditable(true);
		JComboBox combo4 = disabled(new JComboBox(Type.values()));
		combo4.setEditable(true);
		JProgressBar prog1 = new JProgressBar();
		JProgressBar prog2 = new JProgressBar();
		prog2.setValue(50);
		JProgressBar prog3 = new JProgressBar();
		prog3.setValue(100);
		JProgressBar progA = new JProgressBar();
		progA.setStringPainted(true);
		JProgressBar progB = new JProgressBar();
		progB.setStringPainted(true);
		progB.setValue(50);
		JProgressBar progC = new JProgressBar();
		progC.setStringPainted(true);
		progC.setValue(100);
		final JProgressBar indeterminate = new JProgressBar();
		indeterminate.setIndeterminate(true);
		JCheckBox hide = new JCheckBox("Hide Indeterminate Progress Bar:", false);
		hide.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent evt) {
				indeterminate.setVisible(evt.getStateChange() != ItemEvent.SELECTED);
			}
		});
		JPanel other = new JPanel(null);
		GroupLayout layout = new GroupLayout(other);
		other.setLayout(layout);
		final int prf = GroupLayout.PREFERRED_SIZE;
		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup()
						.addComponent(label1, Alignment.TRAILING).addComponent(label2, Alignment.TRAILING)
						.addComponent(toggle1).addComponent(toggle2).addComponent(toggle3).addComponent(toggle4)
						.addComponent(combo1, prf, prf, prf).addComponent(combo2, prf, prf, prf)
						.addComponent(combo3, prf, prf, prf).addComponent(combo4, prf, prf, prf)
						.addComponent(check1).addComponent(check2).addComponent(check3).addComponent(check4)
						.addComponent(slider1).addComponent(slider3).addComponent(slider5)
						.addComponent(prog1).addComponent(prog2).addComponent(prog3)
						.addComponent(hide, Alignment.TRAILING))
				.addGap(3)
				.addGroup(layout.createParallelGroup()
						.addComponent(text1).addComponent(text2)
						.addComponent(button1).addComponent(button2)
						.addComponent(spinner1, prf, prf, prf).addComponent(spinner2, prf, prf, prf)
						.addComponent(spinner3, prf, prf, prf).addComponent(spinner4, prf, prf, prf)
						.addComponent(spinner5).addComponent(spinner6)
						.addComponent(radio1).addComponent(radio2).addComponent(radio3).addComponent(radio4)
						.addComponent(slider2).addComponent(slider4).addComponent(slider6)
						.addComponent(progA).addComponent(progB).addComponent(progC)
						.addComponent(indeterminate))
				.addGap(3));
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(label1).addComponent(text1))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(label2).addComponent(text2))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(toggle1).addComponent(button1))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(toggle2).addComponent(button2))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(toggle3).addComponent(spinner1))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(toggle4).addComponent(spinner2))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(combo1).addComponent(spinner3))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(combo2).addComponent(spinner4))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(combo3).addComponent(spinner5))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(combo4).addComponent(spinner6))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(radio1).addComponent(check1))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(radio2).addComponent(check2))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(radio3).addComponent(check3))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(radio4).addComponent(check4))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(slider1).addComponent(slider2))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(slider3).addComponent(slider4))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(slider5).addComponent(slider6))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(prog1).addComponent(progA))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(prog2).addComponent(progB))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(prog3).addComponent(progC))
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(hide).addComponent(indeterminate)));

					
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Controls", other);
		tabs.addTab("Collections", collections);
		tabs.addTab("Text", textTab);
		return tabs;
	}
	
	JButton update;
	JCheckBox autoUpdate;
	JTable primaryTable;
	JTable secondaryTable;
	JTable otherTable;
	JComboBox keyFilter;
	JComboBox keyFilterType;
	JComboBox typeFilter;

	private class FiltersLayout implements LayoutManager {

		@Override
		public void addLayoutComponent(String name, Component comp) {}

		@Override
		public void layoutContainer(Container parent) {
			TableColumnModel mdl = otherTable.getColumnModel();
			int cw = mdl.getColumn(0).getWidth();
			Dimension size = keyFilterType.getPreferredSize();
			int kftw = size.width;
			int kfw = cw - kftw - 10;
			if (kfw < 100) {
				kfw = 100;
				kftw = cw - 110;
			}
			keyFilter.setBounds(0, 0, kfw, size.height);
			keyFilterType.setBounds(kfw, 0, kftw, size.height);
			size = typeFilter.getPreferredSize();
			typeFilter.setBounds(cw, 0, size.width, size.height);
		}

		@Override
		public Dimension minimumLayoutSize(Container parent) {
			return new Dimension(0, 0);
		}

		@Override
		public Dimension preferredLayoutSize(Container parent) {
			Dimension size = keyFilter.getPreferredSize();
			size.width = TABLE_WIDTH;
			return size;
		}

		@Override
		public void removeLayoutComponent(Component comp) {}
		
	}
	
	private static class Table extends JTable implements ComponentListener {
		Table(TableModel mdl) {
			super(mdl);
		}
		
		
		@Override
		public void addNotify() {
			super.addNotify();
			Container parent = getParent();
			if (parent instanceof JViewport) {
				parent.addComponentListener(this);
			}
		}
		
		@Override
		public void removeNotify() {
			super.removeNotify();
			Container parent = getParent();
			if (parent instanceof JViewport) {
				parent.removeComponentListener(this);
			}
		}
		
		/**
		 * Overridden to supply hasFocus as false to the renderers
		 * but still allow the table to be focusable.
		 */
		@Override
		public Component prepareRenderer(TableCellRenderer renderer,
				int row, int column) {
			Object value = getValueAt(row, column);
			boolean isSelected = false;
			// Only indicate the selection and focused cell if not printing
			if (!isPaintingForPrint()) {
				isSelected = isCellSelected(row, column);
			}
			return renderer.getTableCellRendererComponent(
					this, value, isSelected, false, row, column);
		}

		@Override
		public void componentHidden(ComponentEvent e) {}

		@Override
		public void componentMoved(ComponentEvent e) {}

		
		@Override
		public void componentResized(ComponentEvent e) {
			JViewport port = (JViewport)e.getSource();
			TableColumnModel columns = getColumnModel();
			int width = port.getWidth();
			Insets in = port.getInsets();
			width -= in.left + in.right;
			for (int i=columns.getColumnCount(); --i>0;)
				width -= columns.getColumn(i).getWidth();
			if (width < 200)
				width = 200;
			TableColumn col = columns.getColumn(0);
			if (width != col.getPreferredWidth()) {
				col.setMinWidth(width);
				col.setPreferredWidth(width);
			}
		}

		@Override
		public void componentShown(ComponentEvent e) {}
	    
	}
	
	private static JSlider tickedSlider(boolean paintLabels) {
		JSlider s = new JSlider(0, 100);
		s.setMajorTickSpacing(25);
		s.setMinorTickSpacing(5);
		s.setPaintTicks(true);
		s.setPaintLabels(paintLabels);
		return s;
	}
	
	
	private static <T extends JComponent> T disabled(T c) {
		c.setEnabled(false);
		return c;
	}
	
	private static final int TABLE_WIDTH = 500;
	private static final int VALUE_WIDTH = 100;
	private static final int DEFAULT_WIDTH = 50;
	private static JTable createUITable(int typeWidth, String valueHeader,
			List<String> lst, TableCellRenderer renderer, TableCellEditor editor) {
		String[] keys = lst.toArray(new String[lst.size()]);
		Arrays.sort(keys);
		JTable table = new Table(new UITableModel(keys, valueHeader));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setRowHeight(25);
		table.setPreferredScrollableViewportSize(new Dimension(TABLE_WIDTH, table.getRowHeight()*10));
		TableColumnModel columns = table.getColumnModel();
		int keyWidth = TABLE_WIDTH-typeWidth-VALUE_WIDTH-DEFAULT_WIDTH;
		columns.getColumn(0).setMinWidth(keyWidth);
		columns.getColumn(0).setPreferredWidth(keyWidth);
		setWidth(columns.getColumn(1), typeWidth);
		TableColumn column = columns.getColumn(2);
		setWidth(column, VALUE_WIDTH);
		column.setCellRenderer(renderer);
		column.setCellEditor(editor);
		setWidth(columns.getColumn(3), DEFAULT_WIDTH);
		return table;
	}
	private static void setWidth(TableColumn column, int width) {
		column.setPreferredWidth(width);
		column.setResizable(false);
		column.setMinWidth(width);
		column.setMaxWidth(width);
	}

	@Override
	public void tableChanged(TableModelEvent e) {
		UITableModel mdl = (UITableModel)secondaryTable.getModel();
		mdl.fireTableRowsUpdated(0, mdl.getRowCount()-1);
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if ("width".equals(e.getPropertyName())) {
			JComponent c = (JComponent)keyFilter.getParent();
			if (c != null) {
				c.revalidate();
				c.repaint();
			}
		}
	}
	
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		update.setEnabled(!autoUpdate.isSelected());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == update) {
			updateUI();
		} else if (e.getSource() == keyFilter || e.getSource() == keyFilterType || e.getSource() == typeFilter) {
			updateFilter();
		}
	}
	
	private static void updateUI() {
		for (Window window : Window.getWindows()) {
			SwingUtilities.updateComponentTreeUI(window);
		}
	}
	
	private void updateFilter() {
		DefaultRowSorter<TableModel,String> sorter = (DefaultRowSorter<TableModel,String>)otherTable.getRowSorter();
		String key = keyFilter.getSelectedItem().toString();
		RowFilter<TableModel,String> filter = null;
		if (!key.isEmpty()) {
			Object method = keyFilterType.getSelectedItem();
			if (method == "Starts With") {
				filter = RowFilter.regexFilter('^'+Pattern.quote(key), 0);
			} else if (method == "Ends With") {
				filter = RowFilter.regexFilter(Pattern.quote(key)+'$', 0);
			} else if (method == "Contains") {
				filter = RowFilter.regexFilter(Pattern.quote(key), 0);
			} else {
				filter = RowFilter.regexFilter(key, 0);
			}
		}
		String type = typeFilter.getSelectedItem().toString();
		if (!type.isEmpty()) {
			RowFilter<TableModel,String> typeFilter = RowFilter.regexFilter('^'+type+'$', 1);
			filter = filter == null ? typeFilter : RowFilter.<TableModel,String>andFilter(Arrays.asList(filter, typeFilter));
		}
		sorter.setRowFilter(filter);
	}
	
	
	private enum Type {
		Color(ColorChooser.class),
		Painter(null),
		Insets(InsetsChooser.class),
		Font(FontChooser.class),
		Boolean(BooleanChooser.class),
		Integer(IntegerChooser.class),
		String(StringChooser.class),
		Icon(null),
		Dimension(DimensionChooser.class),
		Object(null);
		
		Type(Class<? extends ValueChooser> cls) {
			chooserClass = cls;
		}
		
		ValueChooser chooser;
		Class<? extends ValueChooser> chooserClass;
		
		ValueChooser getValueChooser() {
			if (chooser == null) {
				if (chooserClass == null)
					return null;
				try {
					chooser = chooserClass.newInstance();
				} catch (Exception e) {
					e.printStackTrace();
					chooserClass = null;
					return null;
				}
			}
			return chooser;
		}
		
		static Type getType(Object obj) {
			if (obj instanceof Color) {
				return Color;
			} else if (obj instanceof Painter<?>) {
				return Painter;
			} else if (obj instanceof Insets) {
				return Insets;
			} else if (obj instanceof Font) {
				return Font;
			} else if (obj instanceof Boolean) {
				return Boolean;
			} else if (obj instanceof Integer) {
				return Integer;
			} else if (obj instanceof Icon) {
				return Icon;
			} else if (obj instanceof String) {
				return String;
			} else if (obj instanceof Dimension) {
				return Dimension;
			} else {
				return Object;
			}

		}
		
	}
	
	private static class UITableModel extends AbstractTableModel {
		UITableModel(String[] kys, String valHdr) {
			keys = kys;
			types = new Type[kys.length];
			valueHeader = valHdr;
		}
		
		private String[] keys;
		private Type[] types;
		private String valueHeader;

		@Override
		public int getColumnCount() {
			return 4;
		}
		
		@Override
		public String getColumnName(int col) {
			switch (col) {
			case 0: return "Key";
			case 1: return "Type";
			case 2: return valueHeader;
			case 3: return "Default";
			}
			throw new IllegalArgumentException();
		}

		@Override
		public Class<?> getColumnClass(int col) {
			switch (col) {
			case 2: return UIDefaults.class;
			case 3: return Boolean.class;
			}
			return Object.class;
		}

		@Override
		public int getRowCount() {
			return keys.length;
		}

		@Override
		public Object getValueAt(int row, int col) {
			switch (col) {
			case 0: return keys[row];
			case 1: return getType(row);
			case 2: return UIManager.get(keys[row]);
			case 3: return !UIManager.getDefaults().containsKey(keys[row]);
			}
			throw new IllegalArgumentException();
		}
		
		Type getType(int row) {
			if (types[row] == null)
				types[row] = Type.getType(UIManager.get(keys[row]));
			return types[row];
		}
		
		@Override
		public boolean isCellEditable(int row, int col) {
			return col == 2 || col == 3;
		}
		
		// Font.equals() is too sensitive, so use this.
		private boolean fontEquals(Font a, Font b) {
			return a.getSize2D() == b.getSize2D() &&
				a.getStyle() == b.getStyle() && 
				a.getFamily().equals(b.getFamily());
		}

		@Override
		public void setValueAt(Object aValue, int row, int col) {
			switch (col) {
			case 2:
				Object def = UIManager.getLookAndFeel().getDefaults().get(keys[row]);
				if ((getType(row) == Type.Font && fontEquals((Font)aValue, (Font)def)) || aValue.equals(def)) {
					UIManager.put(keys[row], null);
				} else {
					UIManager.put(keys[row], aValue);
				}
				fireTableCellUpdated(row, 3);
				break;
			case 3:
				if (aValue == Boolean.TRUE) {
					UIManager.put(keys[row], null);
					fireTableCellUpdated(row, 2);
				}
				break;
			}
		}

	}

	

	private static class UIDefaultsRenderer extends JComponent implements TableCellRenderer {
		private static final Font BOOLEAN_FONT = Font.decode("sansserif-bold");
		
		boolean selected;
		Object value;
		Type type;
		
		@Override
		public Component getTableCellRendererComponent(JTable tbl,
				Object val, boolean isSelected, boolean hasFocus, int row,
				int column) {
			UITableModel mdl = (UITableModel)tbl.getModel();
			type = mdl.getType(tbl.convertRowIndexToModel(row));
			value = val;
			selected = isSelected;
			return this;
		}
		
		protected void paintComponent(Graphics g) {
			if (selected) {
				g.setColor(UIManager.getColor("Table[Enabled+Selected].textBackground"));
				g.fillRect(0, 0, getWidth(), getHeight());
			}
			switch (type) {
			case Color: {
				Color col = (Color)value;
				g.setColor(col);
				Graphics2D g2 = (Graphics2D)g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 10, 10);
			} break;
			case Painter: {
				Painter<JComponent> painter = (Painter<JComponent>)value;
				g.translate((getWidth()-getHeight())/2, 0);
				painter.paint((Graphics2D)g, this, getHeight(), getHeight());
			} break;
			case Insets: {
				Insets in = (Insets)value;
				g.setColor(Color.BLACK);
				g.drawRect(2, 2, getWidth()-4, getHeight()-4);
				g.setColor(Color.GRAY);
				g.drawRect(3+in.left, 3+in.top, getWidth()-6-in.right-in.left, getHeight()-6-in.bottom-in.top);
			} break;
			case Font: {
				Font font = (Font)value;
				drawString(g, font.getFamily(), font);
			} break;
			case Boolean:
				drawString(g, value.toString(), BOOLEAN_FONT);
				break;
			case Integer: case String:
				drawString(g, value.toString(), getFont());
				break;
			case Icon: {
				Icon icn = (Icon)value;
				int x = (getWidth()-icn.getIconWidth())/2;
				int y = (getHeight()-icn.getIconHeight())/2;
				icn.paintIcon(this, g, x, y);
			} break;
			case Dimension: {
				Dimension d = (Dimension)value;
				if (d.width < getWidth()-2 && d.height < getHeight()-2) {
					g.setColor(Color.GRAY);
					g.drawRect((getWidth()-d.width)/2, (getHeight()-d.height)/2, d.width, d.height);
				} else {
					drawString(g, d.width+" x "+d.height, getFont());
				}
			} break;
			case Object: {
				System.out.println(value.getClass());
			} break;
			}
		}
		
		private void drawString(Graphics g, String str, Font font) {
			g.setColor(selected ?
					UIManager.getColor("Table[Enabled+Selected].textForeground") :
					UIManager.getColor("Table.textForeground"));
			g.setFont(font);
			((Graphics2D)g).setRenderingHint(
					RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			FontMetrics metrics = g.getFontMetrics();
			int w = metrics.stringWidth(str);
			int y =(getHeight()-metrics.getHeight())/2+metrics.getAscent();
			int x;
			int cw = getWidth();
			if (w > cw) {
				w = metrics.charWidth('.')*3;
				int i = 0;
				while (w < cw)
					w += metrics.charWidth(str.charAt(i++));
				str = str.substring(0, i-1).concat("...");
				x = 0;
			} else {
				x = (cw-w)/2;
			}
			g.drawString(str, x, y);
		}
	}

	private static class UIDefaultsEditor extends AbstractCellEditor implements TableCellEditor, ActionListener, MouseListener {
		private static final String OK = "OK";
		private static final String CANCEL = "Cancel";
		
		public UIDefaultsEditor() {
			renderer = new UIDefaultsRenderer();
			renderer.addMouseListener(this);
			popup = new JPopupMenu();
			popup.setLayout(new BorderLayout());
			JButton ok = new JButton(OK);
			ok.addActionListener(this);
			JButton cancel = new JButton(CANCEL);
			cancel.addActionListener(this);
			JPanel buttons = new JPanel(null);
			GroupLayout layout = new GroupLayout(buttons);
			buttons.setLayout(layout);
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER, true)
					.addGroup(layout.createSequentialGroup()
							.addGap(8).addComponent(ok).addGap(5).addComponent(cancel).addGap(8))
					.addGap(100, 100, Short.MAX_VALUE));
			layout.setVerticalGroup(layout.createBaselineGroup(false, true)
					.addComponent(ok).addComponent(cancel));
			layout.linkSize(SwingUtilities.HORIZONTAL, ok, cancel);
			
			popup.add(buttons, BorderLayout.SOUTH);
		}
		
		UIDefaultsRenderer renderer;
		JPopupMenu popup;
		ValueChooser currentChooser;
		
		@Override
		public Object getCellEditorValue() {
			return renderer.value;
		}

		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {
			return renderer.getTableCellRendererComponent(table, value, true, false, row, column);
		}
		
		@Override
		public boolean isCellEditable(EventObject e) {
			if (e instanceof MouseEvent) {
				MouseEvent me = (MouseEvent)e;
				return (me.getModifiersEx() & (
						InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK |
						InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)) == 0;
			}
			return super.isCellEditable(e);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand() == OK) {
				Object value = currentChooser.getValue();
				if (!value.equals(renderer.value))
					renderer.value = value;
				hidePopup();
			} else if (e.getActionCommand() == CANCEL) {
				hidePopup();
			}
		}
		
		private void hidePopup() {
			currentChooser = null;
			popup.setVisible(false);
			fireEditingStopped();
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}
		
		@Override
		public void mouseReleased(MouseEvent evt) {
			currentChooser = renderer.type.getValueChooser();
			if (currentChooser == null)
				return;
			currentChooser.setValue(renderer.value);
			BorderLayout layout = (BorderLayout)popup.getLayout();
			Component cur = layout.getLayoutComponent(BorderLayout.CENTER);
			if (cur != currentChooser.getComponent()) {
				if (cur != null)
					popup.remove(cur);
				popup.add(currentChooser.getComponent(), BorderLayout.CENTER);
			}
			popup.show(renderer, 0, renderer.getHeight());
		}

	}
	
	private static abstract class ValueChooser {
		
		abstract JComponent getComponent();
		abstract void setValue(Object value);
		abstract Object getValue();
		
	}
	
	private static class BooleanChooser extends ValueChooser {
		@SuppressWarnings("unused")
		BooleanChooser() {
			tru = new JRadioButton(Boolean.TRUE.toString());
			tru.setFont(UIDefaultsRenderer.BOOLEAN_FONT);
			fal = new JRadioButton(Boolean.FALSE.toString());
			fal.setFont(UIDefaultsRenderer.BOOLEAN_FONT);
			ButtonGroup group = new ButtonGroup();
			group.add(tru);
			group.add(fal);
			pane = new JPanel(null);
			GroupLayout layout = new GroupLayout(pane);
			pane.setLayout(layout);
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER, true)
					.addGap(100, 100, Short.MAX_VALUE)
					.addGroup(layout.createSequentialGroup()
							.addGap(8)
							.addGroup(layout.createParallelGroup(Alignment.LEADING, false)
									.addComponent(tru).addComponent(fal))
							.addGap(8)));
			layout.setVerticalGroup(layout.createSequentialGroup()
							.addGap(2).addComponent(tru).addComponent(fal).addGap(4));
		}
		JComponent pane;
		JRadioButton tru;
		JRadioButton fal;

		JComponent getComponent() {
			return pane;
		}
		
		void setValue(Object value) {
			if (Boolean.TRUE.equals(value)) {
				tru.setSelected(true);
			} else {
				fal.setSelected(true);
			}
		}
		
		Object getValue() {
			return Boolean.valueOf(tru.isSelected());
		}
	}
	
	private static class StringChooser extends ValueChooser {
		
		@SuppressWarnings("unused")
		StringChooser() {
			FlowLayout layout = new FlowLayout(FlowLayout.CENTER, 2, 0);
			pane = new JPanel(layout);
			pane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
			text = new JTextField(40);
			pane.add(text);
		}
		
		JComponent pane;
		JTextField text;
		
		JComponent getComponent() {
			return pane;
		}
		
		void setValue(Object value) {
			text.setText(value.toString());
		}
		
		Object getValue() {
			return text.getText();
		}
		
	}
	
	private static class ColorChooser extends ValueChooser {
		
		@SuppressWarnings("unused")
		ColorChooser() {
			chooser = new JColorChooser();
		}
		
		JColorChooser chooser;
		
		JComponent getComponent() {
			return chooser;
		}
		
		void setValue(Object value) {
			chooser.setColor((Color)value);
		}
		
		Object getValue() {
			return chooser.getColor();
		}
	}
	
	
	private static class IntegerChooser extends ValueChooser {
		@SuppressWarnings("unused")
		IntegerChooser() {
			chooser = new NumberChooser(null, -10, 100);
			pane = NumberChooser.createComponent(null, -1, -1, -1, -1, chooser);
		}
		JComponent pane;
		NumberChooser chooser;
		
		JComponent getComponent() {
			return pane;
		}
		
		void setValue(Object value) {
			chooser.setValue((Integer)value);
		}
		
		Object getValue() {
			return chooser.getValue();
		}
	}
	
	private static class DimensionChooser extends ValueChooser implements ChangeListener {
		@SuppressWarnings("unused")
		DimensionChooser() {
			width = new NumberChooser("Width:", 0, 2000);
			height = new NumberChooser("Height:", 0, 2000);
			renderer = new UIDefaultsRenderer();
			renderer.type = Type.Dimension;
			width.addChangeListener(this);
			height.addChangeListener(this);
			pane = NumberChooser.createComponent(renderer, 200, Short.MAX_VALUE, 200, 200, width, height);
		}
		
		JComponent pane;
		NumberChooser width;
		NumberChooser height;
		UIDefaultsRenderer renderer;
		
		JComponent getComponent() {
			return pane;
		}
		
		void setValue(Object value) {
			Dimension d = (Dimension)value;
			renderer.value = null;
			width.setValue(d.width);
			height.setValue(d.height);
			renderer.value = value;
		}
		
		Object getValue() {
			return renderer.value;
		}
		
		public void stateChanged(ChangeEvent evt) {
			if (renderer.value != null) {
				renderer.value = new Dimension(width.getValue(), height.getValue());
				renderer.repaint();
			}
		}

	}
	
	private static class InsetsChooser extends ValueChooser implements ChangeListener {
		
		@SuppressWarnings("unused")
		InsetsChooser() {
			top = new NumberChooser("Top:", 0, 20);
			left = new NumberChooser("Left:", 0, 20);
			bottom = new NumberChooser("Bottom:", 0, 20);
			right = new NumberChooser("Right:", 0, 20);
			renderer = new UIDefaultsRenderer();
			renderer.type = Type.Insets;
			top.addChangeListener(this);
			left.addChangeListener(this);
			bottom.addChangeListener(this);
			right.addChangeListener(this);
			
			pane = NumberChooser.createComponent(renderer, 120, 120, 50, 50, top, left, bottom, right);
		}
		
		JComponent pane;
		NumberChooser top;
		NumberChooser left;
		NumberChooser bottom;
		NumberChooser right;
		UIDefaultsRenderer renderer;
		
		JComponent getComponent() {
			return pane;
		}
		
		void setValue(Object value) {
			Insets i = (Insets)value;
			renderer.value = null;
			top.setValue(i.top);
			left.setValue(i.left);
			bottom.setValue(i.bottom);
			right.setValue(i.right);
			renderer.value = i;
			renderer.repaint();
		}
		
		Object getValue() {
			return renderer.value;
		}
		
		public void stateChanged(ChangeEvent evt) {
			if (renderer.value != null) {
				renderer.value = new Insets(top.getValue(), left.getValue(), bottom.getValue(), right.getValue());
				renderer.repaint();
			}
		}
		
	}

	private static class NumberChooser implements ChangeListener {
		NumberChooser(String nam, int min, int max) {
			name = nam;
			spin = new SpinnerNumberModel(min, min, max, 1);
			spin.addChangeListener(this);
			slide = new JSlider(min, max);
			slide.setMinorTickSpacing((max-min)/10);
			slide.setMajorTickSpacing((max-min)/2);
			slide.setPaintTicks(true);
			slide.addChangeListener(this);
		}
		String name;
		SpinnerNumberModel spin;
		JSlider slide;
		
		@Override
		public void stateChanged(ChangeEvent e) {
			if (e.getSource() == slide) {
				if (spin.getNumber().intValue() != slide.getValue())
					spin.setValue(slide.getValue());
			} else {
				if (slide.getValue() != spin.getNumber().intValue())
					slide.setValue(spin.getNumber().intValue());
			}
		}
		
		int getValue() {
			return slide.getValue();
		}
		
		void setValue(int value) {
			slide.setValue(value);
		}
		
		void addChangeListener(ChangeListener l) {
			slide.addChangeListener(l);
		}
		

		static JComponent createComponent(JComponent preview, int prefW, int maxW, int prefH, int maxH, NumberChooser ... choosers) {
			JComponent pane = new JPanel(null);
			GroupLayout layout = new GroupLayout(pane);
			pane.setLayout(layout);
			GroupLayout.ParallelGroup labelX = null;
			GroupLayout.ParallelGroup spinX = layout.createParallelGroup(Alignment.LEADING, false);
			GroupLayout.ParallelGroup slideX = layout.createParallelGroup(Alignment.LEADING, false);
			GroupLayout.SequentialGroup y = layout.createSequentialGroup().addGap(2);
			for (NumberChooser chooser : choosers) {
				JLabel label = chooser.name == null ? null : new JLabel(chooser.name);
				JSpinner spin = new JSpinner(chooser.spin);
				GroupLayout.ParallelGroup baseline = layout.createBaselineGroup(false, true);
				y.addGroup(baseline);
				if (label != null) {
					if (labelX == null)
						labelX = layout.createParallelGroup(Alignment.TRAILING, false);
					labelX.addComponent(label);
					baseline.addComponent(label);
				}
				spinX.addComponent(spin);
				slideX.addComponent(chooser.slide);
				baseline.addComponent(spin).addComponent(chooser.slide);
			}
			GroupLayout.Group x = layout.createSequentialGroup().addGap(8);
			if (labelX != null)
				x.addGroup(labelX).addGap(2);
			x.addGroup(spinX).addGap(2).addGroup(slideX).addGap(8);
			y.addGap(4);
			if (preview != null) {
				y.addComponent(preview, prefH, prefH, maxH);
				y.addGap(4);
				x = layout.createParallelGroup(Alignment.CENTER, false)
						.addGroup(x).addComponent(preview, prefW, prefW, maxW);
			}
			layout.setHorizontalGroup(x);
			layout.setVerticalGroup(y);
			return pane;
		}
	}
	
	private static class FontChooser extends ValueChooser implements ChangeListener {
		@SuppressWarnings("unused")
		FontChooser() {
			family = new SpinnerListModel(new Object[]{
				Font.DIALOG, Font.DIALOG_INPUT, Font.MONOSPACED, Font.SANS_SERIF, Font.SERIF	
			});
			family.addChangeListener(this);
			JSpinner familySpin = new JSpinner(family);
			size = new SpinnerNumberModel(32f, 8f, 32f, 2f);
			size.addChangeListener(this);
			JSpinner sizeSpin = new JSpinner(size);
			bold = new JToggleButton("B");
			bold.addChangeListener(this);
			bold.setFont(bold.getFont().deriveFont(Font.BOLD));
			italic = new JToggleButton("I");
			italic.addChangeListener(this);
			italic.setFont(italic.getFont().deriveFont(Font.ITALIC));
			renderer = new UIDefaultsRenderer();
			renderer.type = Type.Font;
			pane = new JPanel(null);
			GroupLayout layout = new GroupLayout(pane);
			pane.setLayout(layout);
			layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
					.addGroup(layout.createSequentialGroup()
							.addGap(8).addComponent(familySpin, 150, 150, 150).addGap(2)
							.addComponent(sizeSpin).addGap(2).addComponent(bold)
							.addGap(2).addComponent(italic).addGap(12))
					.addComponent(renderer));
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addGap(2)
					.addGroup(layout.createBaselineGroup(false, true)
							.addComponent(familySpin).addComponent(sizeSpin)
							.addComponent(bold).addComponent(italic))
					.addComponent(renderer, 50, 50, 50));
		}
		JComponent pane;
		SpinnerListModel family;
		SpinnerNumberModel size;
		JToggleButton bold;
		JToggleButton italic;
		UIDefaultsRenderer renderer;
		
		JComponent getComponent() {
			return pane;
		}
		
		void setValue(Object value) {
			Font font = (Font)value;
			renderer.value = null;
			family.setValue(font.getFamily());
			size.setValue(font.getSize2D());
			bold.setSelected(font.isBold());
			italic.setSelected(font.isItalic());
			renderer.value = value;
		}
		
		Object getValue() {
			return renderer.value;
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			Font font = (Font)renderer.value;
			if (e.getSource() == size) {
				renderer.value = font.deriveFont(size.getNumber().floatValue());
			} else if (e.getSource() == bold) {
				renderer.value = font.deriveFont(bold.isSelected() ?
						font.getStyle() | Font.BOLD : font.getStyle() & ~Font.BOLD);
			} else if (e.getSource() == italic) {
				renderer.value = font.deriveFont(italic.isSelected() ?
						font.getStyle() | Font.ITALIC : font.getStyle() & ~Font.ITALIC);
			} else if (e.getSource() == family) {
				font = Font.decode(family.getValue().toString()+' '+size.getNumber().intValue());
				int style = 0;
				if (bold.isSelected())
					style |= Font.BOLD;
				if (italic.isSelected())
					style |= Font.ITALIC;
				if (style != 0)
					font = font.deriveFont(style);
				renderer.value = font;
			}
			renderer.repaint();
		}
		
	}

}
