package aephyr.swing.nimbus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.DefaultRowSorter;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.GroupLayout.Alignment;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.tools.SimpleJavaFileObject;

public class NimbusThemeCreator implements ActionListener, ChangeListener,
		ItemListener, PropertyChangeListener, TableModelListener {
	
	private static final int TABLE_WIDTH = 450;
	private static final int VALUE_WIDTH = 100;
	private static final int DEFAULT_WIDTH = 50;

	public static void main(String[] args) throws Exception {
		if (args.length > 0) {
			File file = new File(args[0]);
			if (file.isFile())
				CodeTransfer.importThemeFromFile(file);
		}
		setNimbusLookAndFeel();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame(NimbusThemeCreator.class.getSimpleName());
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				NimbusThemeCreator creator = new NimbusThemeCreator();
				frame.add(creator.createBody(), BorderLayout.CENTER);
				frame.getRootPane().setDefaultButton(creator.defaultButton);
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
			throw new ClassNotFoundException();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	static String[] painterKeys;
	
	private JButton update;
	private JCheckBox autoUpdate;
	private JTable primaryTable;
	private JTable secondaryTable;
	private JTable otherTable;
	private JComboBox keyFilter;
	private JComboBox keyFilterMethod;
	private JComboBox typeFilter;
	
	private CodeTransfer transfer;
	
	// Default button must be reset as default after each update.
	private JButton defaultButton;

	// Preview tabs : created as needed for startup/update performance.
	private boolean[] created;
	private JSplitPane collections;
	private JPanel options;
	private JSplitPane texts;
	private JPanel fileChooser;
	private JPanel colorChooser;
	private JPanel desktop;

	private NimbusThemeCreator() {
		List<String> primary = new ArrayList<String>();
		List<String> secondary = new ArrayList<String>();
		List<String> other = new ArrayList<String>();
		Set<String> filters = new HashSet<String>();
		List<String> painters = new ArrayList<String>();
		for (Map.Entry<Object,Object> entry : UIManager.getLookAndFeelDefaults().entrySet()) {
			if (!(entry.getKey() instanceof String))
				continue;
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
				if (str.endsWith("Painter"))
					painters.add(str);
				int i = str.indexOf('.');
				if (i < 0)
					continue;
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
		painterKeys = painters.toArray(new String[painters.size()]);
		Arrays.sort(painterKeys);
		primaryTable = createUITable(false, 0, Type.Color, primary);
		primaryTable.getModel().addTableModelListener(this);
		secondaryTable = createUITable(false, 0, Type.Color, secondary);
		otherTable = createUITable(true, 75, null, other);
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
		keyFilterMethod = new JComboBox(
				new Object[]{"Starts With","Ends With","Contains","Regex"});
		keyFilterMethod.addActionListener(this);
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
		
		defaultButton = new JButton("Default");
		defaultButton.setDefaultCapable(true);

	}
	
	
	
	// GUI building
	

	private static JTable createUITable(boolean keyColumnResizable, int typeWidth, Type type,
			List<String> lst) {
		String[] keys = lst.toArray(new String[lst.size()]);
		Arrays.sort(keys);
		TableModel mdl = type == null ? new UITableModel(keys) : new UITypeTableModel(keys, type, true);
		JTable table = new UITable(mdl, null);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setRowHeight(25);
		TableColumnModel columns = table.getColumnModel();
		int keyWidth = TABLE_WIDTH-typeWidth-VALUE_WIDTH-DEFAULT_WIDTH;
		columns.getColumn(0).setMinWidth(keyWidth);
		columns.getColumn(0).setPreferredWidth(keyWidth);
		columns.getColumn(0).setResizable(keyColumnResizable);
		setWidth(columns.getColumn(1), typeWidth);
		TableColumn column = columns.getColumn(2);
		setWidth(column, VALUE_WIDTH);
		column.setCellRenderer(new UIDefaultsRenderer());
		column.setCellEditor(new UIDefaultsEditor());
		setWidth(columns.getColumn(3), DEFAULT_WIDTH);
		return table;
	}
	private static void setWidth(TableColumn column, int width) {
		column.setPreferredWidth(width);
		column.setResizable(false);
		column.setMinWidth(width);
		column.setMaxWidth(width);
	}
	
	JComponent createBody() {
		JScrollPane primary = titled(new JScrollPane(
				primaryTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), "Primary");
		JScrollPane secondary = titled(new JScrollPane(
				secondaryTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), "Secondary");
		
		JPanel colors = new JPanel(new StackedTableLayout(3, 10, true));
		colors.add(primary);
		colors.add(secondary);
		Dimension size = new Dimension(TABLE_WIDTH, primaryTable.getRowHeight()*20);
		otherTable.setPreferredScrollableViewportSize(size);
		
		
		JScrollPane other = new JScrollPane(otherTable);
		other.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		JPanel otherPanel = new JPanel(null);
		
		JPanel filters = new JPanel(new FiltersLayout());
		filters.add(keyFilter);
		filters.add(keyFilterMethod);
		filters.add(typeFilter);
		otherTable.getColumnModel().getColumn(0).addPropertyChangeListener(this);
		
		GroupLayout layout = new GroupLayout(otherPanel);
		otherPanel.setLayout(layout);
		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addGap(2)
				.addGroup(layout.createParallelGroup()
						.addComponent(filters).addComponent(other)));
		final int prf = GroupLayout.PREFERRED_SIZE;
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGap(2).addComponent(other).addComponent(filters, prf, prf, prf));

		JTabbedPane options = new JTabbedPane();
		options.addTab("UI Base", colors);
		options.addTab("UI Controls", otherPanel);
		JComponent preview = createPreview();
		
		JButton imp = new JButton("Import");
		imp.addActionListener(this);
		JButton exp = new JButton("Export");
		exp.addActionListener(this);
		
		JPanel body = new JPanel(null);
		layout = new GroupLayout(body);
		body.setLayout(layout);
		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.LEADING, false)
						.addComponent(options)
						.addGroup(layout.createSequentialGroup()
								.addGap(4)
								.addComponent(imp).addComponent(exp)
								.addGap(0, 100, Short.MAX_VALUE)
								.addComponent(autoUpdate).addGap(5).addComponent(update)))
				.addComponent(preview));
		layout.setVerticalGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
						.addComponent(options)
						.addGroup(layout.createBaselineGroup(false, true)
								.addComponent(imp).addComponent(exp)
								.addComponent(update).addComponent(autoUpdate))
						.addGap(4))
				.addComponent(preview));
		return body;
	}
	
	

	private JComponent createPreview() {
		JLabel label1 = new JLabel("Hover Here for Tooltip");
		label1.setToolTipText("Tooltip");
		JLabel label2 = disabled(new JLabel("Disabled"));
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
		JTextField text1 = new JTextField("Click Here for Popup");
		text1.setComponentPopupMenu(popup);
		JTextField text2 = disabled(new JTextField("Disabled"));
		JSlider slider1 = new JSlider();
		JSlider slider2 = disabled(new JSlider());
		JSlider slider3 = tickedSlider(false);
		JSlider slider4 = disabled(tickedSlider(false));
		JSlider slider5 = tickedSlider(true);
		JSlider slider6 = disabled(tickedSlider(true));
		JSpinner spinner1 = new JSpinner(new SpinnerNumberModel(100, 0, Short.MAX_VALUE, 100));
		JSpinner spinner2 = disabled(new JSpinner(new SpinnerNumberModel(100, 0, Short.MAX_VALUE, 100)));
		JSpinner spinner3 = new JSpinner(new SpinnerDateModel());
		JSpinner spinner4 = disabled(new JSpinner(new SpinnerDateModel()));
		Type[] values = Type.values();
		JSpinner spinner5 = new JSpinner(new SpinnerListModel(values));
		JSpinner spinner6 = disabled(new JSpinner(new SpinnerListModel(values)));
		JComboBox combo1 = new JComboBox(values);
		JComboBox combo2 = disabled(new JComboBox(values));
		JComboBox combo3 = new JComboBox(values);
		combo3.setEditable(true);
		JComboBox combo4 = disabled(new JComboBox(values));
		combo4.setEditable(true);
		JProgressBar prog1 = progress(0, false);
		JProgressBar prog2 = progress(50, false);
		JProgressBar prog3 = progress(100, false);
		JProgressBar progA = progress(0, true);
		JProgressBar progB = progress(50, true);
		JProgressBar progC = progress(100, true);
		final JProgressBar indeterminate = new JProgressBar();
		indeterminate.setIndeterminate(true);
		JCheckBox hide = new JCheckBox("Hide Indeterminate Progress Bar:", false);
		hide.setHorizontalAlignment(SwingConstants.RIGHT);
		hide.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent evt) {
				indeterminate.setVisible(evt.getStateChange() != ItemEvent.SELECTED);
			}
		});
		JPanel other = new JPanel(null);
		GroupLayout layout = new GroupLayout(other);
		other.setLayout(layout);
		final int prf = GroupLayout.PREFERRED_SIZE;

		JPanel toggles = createPanel(JToggleButton.class, 2, 0,
				toggle1, toggle2, toggle3, toggle4);
		JPanel buttons = createPanel(JButton.class, 1, 0,
				defaultButton, button1, button2);
		JPanel combos = createPanel(JComboBox.class, 0, 2,
				combo1, combo2, combo3, combo4);
		JPanel spinners = createPanel(JSpinner.class, 0, 2,
				spinner1, spinner2, spinner3, spinner4, spinner5, spinner6);
		JPanel checks = createPanel(JCheckBox.class, 2, 0,
				check1, check2, check3, check4);
		JPanel radios = createPanel(JRadioButton.class, 2, 0,
				radio1, radio2, radio3, radio4);
		JPanel progs = createPanel(JProgressBar.class, 0, 2,
				prog1, progA, prog2, progB, prog3, progC, hide, indeterminate);
		JPanel texts = createPanel(JTextField.class, 0, 1, text1, text2);
		JPanel labels = createPanel(JLabel.class, 1, 0, label1, label2);
		JPanel sliders = createPanel(JSlider.class, 0, 2,
				slider1, slider2, slider3, slider4, slider5, slider6);
		layout.linkSize(SwingConstants.HORIZONTAL, combos, spinners);
		layout.setHorizontalGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup()
								.addComponent(buttons, prf, prf, prf)
								.addComponent(toggles, prf, prf, prf)
								.addGroup(layout.createSequentialGroup()
										.addGroup(layout.createParallelGroup()
												.addComponent(radios)
												.addComponent(checks))
										.addGap(0, 0, 20)))
						.addGroup(layout.createParallelGroup()
								.addComponent(texts)
								.addComponent(combos, prf, prf, prf)
								.addComponent(spinners, prf, prf, prf)))
				.addComponent(labels)
				.addComponent(sliders)
				.addComponent(progs));
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup()
						.addGroup(layout.createSequentialGroup()
								.addComponent(buttons, prf, prf, prf)
								.addGap(0, 0, Short.MAX_VALUE)
								.addComponent(toggles, prf, prf, prf)
								.addGap(0, 0, Short.MAX_VALUE)
								.addComponent(radios, prf, prf, prf)
								.addGap(0, 0, Short.MAX_VALUE)
								.addComponent(checks, prf, prf, prf))
						.addGroup(layout.createSequentialGroup()
								.addComponent(texts, prf, prf, prf)
								.addGap(0, 0, Short.MAX_VALUE)
								.addComponent(combos, prf, prf, prf)
								.addGap(0, 0, Short.MAX_VALUE)
								.addComponent(spinners, prf, prf, prf)))
				.addGap(0, 0, Short.MAX_VALUE)
				.addComponent(labels, prf, prf, prf)
				.addGap(0, 0, Short.MAX_VALUE)
				.addComponent(sliders, prf, prf, prf)
				.addGap(0, 0, Short.MAX_VALUE)
				.addComponent(progs, prf, prf, prf));
		
		this.texts = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		collections = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		options = new JPanel(new GridLayout(0, 2));
		desktop = new JPanel(new BorderLayout());
		fileChooser = new JPanel(new GridBagLayout());
		colorChooser = new JPanel(new GridBagLayout());
		
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Controls", other);
		tabs.addTab("Collections", collections);
		tabs.addTab("Options", centered(options));
		tabs.addTab("Texts", this.texts);
		tabs.addTab("File Chooser", fileChooser);
		tabs.addTab("Color Chooser", colorChooser);
		tabs.addTab("Desktop Pane", desktop);
		created = new boolean[tabs.getTabCount()];
		created[0] = true;
		tabs.addChangeListener(this);
		return tabs;
	}
	
	static JPanel centered(JComponent c) {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(c);
		return panel;
	}
	
	static <T extends JComponent> T disabled(T c) {
		c.setEnabled(false);
		return c;
	}
	
	static <T extends JComponent> T titled(T c, String title) {
		c.setBorder(BorderFactory.createTitledBorder(title));
		return c;
	}
	
	private static JOptionPane createOptionPane(String message, int type) {
		JOptionPane pane = new JOptionPane(message, type);
		String title = message;
		if (type == JOptionPane.QUESTION_MESSAGE) {
			title = "Question Message";
			pane.setOptionType(JOptionPane.YES_NO_CANCEL_OPTION);
		}
		return titled(pane, title);
	}
	
	private static JProgressBar progress(int value, boolean paint) {
		JProgressBar bar = new JProgressBar();
		bar.setValue(value);
		bar.setStringPainted(paint);
		return bar;
	}
	
	private static JSlider tickedSlider(boolean paintLabels) {
		JSlider s = new JSlider(0, 100);
		s.setMajorTickSpacing(25);
		s.setMinorTickSpacing(5);
		s.setPaintTicks(true);
		s.setPaintLabels(paintLabels);
		return s;
	}
	
	private static JPanel createPanel(Class<?> cls, int rows, int cols, Component...components) {
		JPanel panel = new JPanel(new GridLayout(rows, cols, 5, 0));
		for (Component c : components)
			panel.add(c);
		return titled(panel, cls.getSimpleName());
	}
	
	
	private static String getPrototypeString(int chars) {
		char[] c = new char[chars];
		Arrays.fill(c, 'w');
		return new String(c);
	}
	
	private void createCollections() {
		JList list = new JList(painterKeys);
		list.setPrototypeCellValue(getPrototypeString(50));
		JTree tree = new JTree();
		for (int row=0; row<tree.getRowCount(); row++)
			tree.expandRow(row);
		TableColumnModel columns = new DefaultTableColumnModel();
		TableColumn nameColumn = new TableColumn(0, 300);
		nameColumn.setHeaderValue("Name");
		columns.addColumn(nameColumn);
		TableColumn typeColumn = new TableColumn(1, 100);
		typeColumn.setHeaderValue("Type");
		columns.addColumn(typeColumn);
		JTable table = new JTable(otherTable.getModel(), columns);
		table.setPreferredScrollableViewportSize(new Dimension(400, table.getRowHeight()*15));
		JSplitPane hor = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				titled(new JScrollPane(tree), "JTree"),
				titled(new JScrollPane(list), "JList"));
		collections.setTopComponent(hor);
		collections.setBottomComponent(titled(new JScrollPane(table), JTable.class.getSimpleName()));
		collections.validate();
		collections.setDividerLocation(0.55);
		hor.setDividerLocation(0.35);
	}
	
	
	private void createTexts() {
		JTextArea area = new JTextArea(10, 40);
		Exception ex = new Exception("Little something for the Text Components");
		StringWriter writer = new StringWriter();
		PrintWriter pw = new PrintWriter(writer);
		ex.printStackTrace(pw);
		pw.flush();
		pw.close();
		String str = writer.toString();
		area.setText(str);
		area.select(0, 0);
		final JEditorPane editor = new JEditorPane();
		editor.setText(str);
		texts.setTopComponent(titled(new JScrollPane(area), JTextArea.class.getSimpleName()));
		texts.setBottomComponent(titled(new JScrollPane(editor), JEditorPane.class.getSimpleName()));
		texts.setDividerLocation(0.5);
	}
	
	private void createOptions() {
		options.add(createOptionPane("Plain Message", JOptionPane.PLAIN_MESSAGE));
		options.add(createOptionPane("Error Message", JOptionPane.ERROR_MESSAGE));
		options.add(createOptionPane("Information Message", JOptionPane.INFORMATION_MESSAGE));
		options.add(createOptionPane("Warning Message", JOptionPane.WARNING_MESSAGE));
		options.add(createOptionPane("Want to do something?", JOptionPane.QUESTION_MESSAGE));
		JComboBox choiceCombo = new JComboBox(Type.values());
		options.add(titled(new JOptionPane(choiceCombo,
				JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION), "Question Message"));

	}
	
	private void createDesktop() {
		final JDesktopPane desktop = new JDesktopPane();
		JPopupMenu popup = new JPopupMenu();
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JInternalFrame frame = new JInternalFrame(
						JInternalFrame.class.getSimpleName(), true, true, true, true);
				frame.setVisible(true);
				frame.setBounds(50, 100, 600, 500);
				desktop.add(frame);
				desktop.moveToFront(frame);
				desktop.setSelectedFrame(frame);
			}
		};
		al.actionPerformed(null);
		popup.add("New Internal Frame").addActionListener(al);
		desktop.setComponentPopupMenu(popup);
		this.desktop.add(desktop, BorderLayout.CENTER);
	}
	
	private void createFileChooser() {
		fileChooser.add(titled(
				new JFileChooser(), JFileChooser.class.getSimpleName()));
	}
	
	private void createColorChooser() {
		colorChooser.add((titled(
				new JColorChooser(), JColorChooser.class.getSimpleName())));
	}

	
	
	// ideally this behavior would be contained in a footer for JScrollPane
	// current layout does not match table columns when the columns are reordered.
	private class FiltersLayout implements LayoutManager {

		@Override
		public void addLayoutComponent(String name, Component comp) {}

		@Override
		public void layoutContainer(Container parent) {
			TableColumnModel mdl = otherTable.getColumnModel();
			int cw = mdl.getColumn(0).getWidth();
			Dimension size = keyFilterMethod.getPreferredSize();
			int kfmw = size.width;
			int kfw = cw - kfmw - 10;
			keyFilter.setBounds(0, 0, kfw, size.height);
			keyFilterMethod.setBounds(kfw, 0, kfmw, size.height);
			size = typeFilter.getPreferredSize();
			typeFilter.setBounds(cw, 0, size.width, size.height);
		}

		@Override
		public Dimension minimumLayoutSize(Container parent) {
			return size(300);
		}

		@Override
		public Dimension preferredLayoutSize(Container parent) {
			return size(TABLE_WIDTH);
		}
		
		private Dimension size(int width) {
			Dimension size = keyFilter.getPreferredSize();
			size.width = width;
			return size;
		}

		@Override
		public void removeLayoutComponent(Component comp) {}
		
	}
	
	
	
	
	
	
	
	
	
	
	// Event Handling

	@Override
	public void tableChanged(TableModelEvent e) {
		if (e.getSource() == primaryTable.getModel()) {
			UITableModel mdl = (UITableModel)secondaryTable.getModel();
			mdl.fireTableRowsUpdated(0, mdl.getRowCount()-1);
		}
		if (autoUpdate.isSelected() && updater == null) {
			updater = new Runnable() {
				public void run() {
					updater = null;
					updateUI();
				}
			};
			SwingUtilities.invokeLater(updater);
		}
	}
	private Runnable updater;

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
		boolean b = autoUpdate.isSelected();
		update.setEnabled(!b);
		if (b) {
			secondaryTable.getModel().addTableModelListener(this);
			otherTable.getModel().addTableModelListener(this);
		} else {
			secondaryTable.getModel().removeTableModelListener(this);
			otherTable.getModel().removeTableModelListener(this);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == update) {
			updateUI();
		} else if (e.getSource() == keyFilter ||
				e.getSource() == keyFilterMethod ||
				e.getSource() == typeFilter) {
			updateFilter();
		} else if (e.getActionCommand() == "Import") {
			getCodeTransfer().showImportDialog();
		} else if (e.getActionCommand() == "Export") {
			getCodeTransfer().showExportDialog();
		}
	}
	
	private CodeTransfer getCodeTransfer() {
		if (transfer == null)
			transfer = new CodeTransfer(
				(JFrame)SwingUtilities.getWindowAncestor(defaultButton));
		return transfer;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		JTabbedPane tabs = (JTabbedPane)e.getSource();
		int idx = tabs.getSelectedIndex();
		if (idx >= 0 && !created[idx]) {
			created[idx] = true;
			switch (idx) {
			case 1: createCollections(); break;
			case 2: createOptions(); break;
			case 3: createTexts(); break;
			case 4: createFileChooser(); break;
			case 5: createColorChooser(); break;
			case 6: createDesktop(); break;
			}
			
		}
	}
	
	private void updateUI() {
		for (Window window : Window.getWindows()) {
			SwingUtilities.updateComponentTreeUI(window);
		}
		defaultButton.getRootPane().setDefaultButton(defaultButton);
	}
	
	private void updateFilter() {
		DefaultRowSorter<TableModel,Object> sorter =
			(DefaultRowSorter<TableModel,Object>)otherTable.getRowSorter();
		String key = keyFilter.getSelectedItem().toString();
		RowFilter<TableModel,Object> filter = null;
		if (!key.isEmpty()) {
			Object method = keyFilterMethod.getSelectedItem();
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
			RowFilter<TableModel,Object> typeFilter = RowFilter.regexFilter('^'+type+'$', 1);
			filter = filter == null ? typeFilter :
				RowFilter.<TableModel,Object>andFilter(Arrays.asList(filter, typeFilter));
		}
		sorter.setRowFilter(filter);
	}
	
}
