package aephyr.swing.nimbus;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
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
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
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
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;


public class Preview extends WindowAdapter implements ChangeListener {

//	public static void main(final String[] args) throws Exception {
//		File file = new File("Theme.txt");
//		if (file.isFile()) {
//			CodeTransfer.doImport(
//					CodeTransfer.getStatements(file), 
//					new ArrayList<Object>(),
//					UIManager.getDefaults());
//		}
//		Creator.setNimbusLookAndFeel();
//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
//				Preview.initiate(args[0], args[1], args[2]);
//			}
//		});
//	}
	
	static void initiate(String X, String Y, String I) {
		int x = -1, y = -1, tabIndex = 0;
		try {
			x = Integer.parseInt(X);
			y = Integer.parseInt(Y);
			tabIndex = Integer.parseInt(I);
		} catch (NumberFormatException e) {
			x = -1;
		}
		JFrame frame = new JFrame(Preview.class.getSimpleName());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Preview preview = new Preview(tabIndex);
		frame.addWindowListener(preview);
		frame.add(preview.tabs, BorderLayout.CENTER);
		frame.pack();
		if (x >= 0 && y >= 0) {
			frame.setLocation(x, y);
		} else {
			frame.setLocationRelativeTo(null);
		}
		if (preview.defaultButton != null)
			frame.getRootPane().setDefaultButton(preview.defaultButton);
		frame.setVisible(true);
		
	}
	
	public void windowClosing(WindowEvent e) {
		RemoteUIDefaultsImpl.previewClosed();
	}

	private JTabbedPane tabs;
	private boolean[] created;
	private JPanel controls;
	private JSplitPane collections;
	private JPanel options;
	private JSplitPane texts;
	private JPanel fileChooser;
	private JPanel colorChooser;
	private JPanel desktop;
	private JButton defaultButton;
	
	private Preview(int tabIndex) {
		
		controls = new JPanel(null);
		texts = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		collections = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		options = new JPanel(new GridLayout(0, 2));
		desktop = new JPanel(new BorderLayout());
		fileChooser = new JPanel(new GridBagLayout());
		colorChooser = new JPanel(new GridBagLayout());
		
		tabs = new JTabbedPane();
		tabs.addTab("Controls", controls);
		tabs.addTab("Collections", collections);
		tabs.addTab("Options", centered(options));
		tabs.addTab("Texts", this.texts);
		tabs.addTab("File Chooser", fileChooser);
		tabs.addTab("Color Chooser", colorChooser);
		tabs.addTab("Desktop Pane", desktop);
		created = new boolean[tabs.getTabCount()];
		if (tabIndex < 0 || tabIndex >= tabs.getTabCount())
			tabIndex = 0;
		loadTab(tabIndex);
		tabs.setSelectedIndex(tabIndex);
		tabs.addChangeListener(this);

	}
	
		
	private void createControls() {
		
		defaultButton = new JButton("Default");
		defaultButton.setDefaultCapable(true);
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
		Type[] values = Type.values();
		JSpinner[] spins = new JSpinner[] {
			new JSpinner(new SpinnerNumberModel(100, 0, Short.MAX_VALUE, 100)),
			disabled(new JSpinner(new SpinnerNumberModel(100, 0, Short.MAX_VALUE, 100))),
			new JSpinner(new SpinnerDateModel()),
			disabled(new JSpinner(new SpinnerDateModel())),
			new JSpinner(new SpinnerListModel(values)),
			disabled(new JSpinner(new SpinnerListModel(values)))
		};
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
		
		JMenuBar menubar = new JMenuBar();
		JMenu menu1 = new JMenu("Menu");
		menu1.add("Item");
		JMenuItem item = new JMenuItem("Acceslerator");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK));
		menu1.add(item);
		menu1.add(new JRadioButtonMenuItem("Radio", true));
		menu1.add(new JRadioButtonMenuItem("Radio", false));
		menu1.add(disabled(new JRadioButtonMenuItem("Disabled", false)));
		menu1.addSeparator();
		menu1.add(new JCheckBoxMenuItem("Check", true));
		menu1.add(new JCheckBoxMenuItem("Check", false));
		menu1.add(disabled(new JCheckBoxMenuItem("Disabled", false)));
		JMenu menu2 = disabled(new JMenu("Disabled"));
		menubar.add(menu1);
		menubar.add(menu2);

		JPanel toggles = createPanel(JToggleButton.class, 2, 0,
				toggle1, toggle2, toggle3, toggle4);
		JPanel buttons = createPanel(JButton.class, 1, 0,
				defaultButton, button1, button2);
		JPanel combos = createPanel(JComboBox.class, 0, 2,
				combo1, combo2, combo3, combo4);
		JPanel spinners = createPanel(JSpinner.class, 0, 2, spins);
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
		JPanel menus = createPanel(JMenuBar.class, 0, 1, menubar);

		
		GroupLayout layout = new GroupLayout(controls);
		controls.setLayout(layout);
		final int prf = GroupLayout.PREFERRED_SIZE;
		layout.linkSize(SwingConstants.HORIZONTAL, combos, spinners);
		layout.setHorizontalGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup()
								.addComponent(buttons, prf, prf, prf)
								.addComponent(toggles, prf, prf, prf)
								.addComponent(radios, prf, prf, prf)
								.addComponent(checks, prf, prf, prf)
								.addComponent(labels, prf, prf, prf))
						.addGap(0, 0, Short.MAX_VALUE)
						.addGroup(layout.createParallelGroup()
								.addComponent(texts)
								.addComponent(combos, prf, prf, prf)
								.addComponent(spinners, prf, prf, prf)
								.addComponent(menus)))
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
								.addComponent(checks, prf, prf, prf)
								.addGap(0, 0, Short.MAX_VALUE)
								.addComponent(labels, prf, prf, prf))
						.addGroup(layout.createSequentialGroup()
								.addComponent(texts, prf, prf, prf)
								.addGap(0, 0, Short.MAX_VALUE)
								.addComponent(combos, prf, prf, prf)
								.addGap(0, 0, Short.MAX_VALUE)
								.addComponent(spinners, prf, prf, prf)
								.addGap(0, 0, Short.MAX_VALUE)
								.addComponent(menus, prf, prf, prf)))
				.addGap(0, 0, Short.MAX_VALUE)
				.addComponent(sliders, prf, prf, prf)
				.addGap(0, 0, Short.MAX_VALUE)
				.addComponent(progs, prf, prf, prf));
		JRootPane root = tabs.getRootPane();
		if (root != null)
			root.setDefaultButton(defaultButton);
	}
	

	private void createCollections() {

		List<String> other = new ArrayList<String>();
		List<String> painters = new ArrayList<String>();
		Creator.getKeys(other, painters);
		String[] painterKeys = painters.toArray(new String[painters.size()]);
		Arrays.sort(painterKeys);
		String[] keys = other.toArray(new String[other.size()]);
		Arrays.sort(keys);
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
		JTable table = new JTable(new UITableModel(keys), columns);
		table.setPreferredScrollableViewportSize(new Dimension(400, table.getRowHeight()*15));
		JSplitPane hor = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				innerTitled(new JScrollPane(tree), "JTree"),
				innerTitled(new JScrollPane(list), "JList"));
		collections.setTopComponent(hor);
		collections.setBottomComponent(innerTitled(new JScrollPane(table), JTable.class.getSimpleName()));
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
		texts.setTopComponent(innerTitled(new JScrollPane(area), JTextArea.class.getSimpleName()));
		texts.setBottomComponent(innerTitled(new JScrollPane(editor), JEditorPane.class.getSimpleName()));
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

	


	@Override
	public void stateChanged(ChangeEvent e) {
		int idx = tabs.getSelectedIndex();
		RemoteUIDefaultsImpl.setTabIndex(idx);
		loadTab(idx);
	}
	
	private void loadTab(int tabIndex) {
		if (tabIndex >= 0 && !created[tabIndex]) {
			created[tabIndex] = true;
			switch (tabIndex) {
			case 0: createControls(); break;
			case 1: createCollections(); break;
			case 2: createOptions(); break;
			case 3: createTexts(); break;
			case 4: createFileChooser(); break;
			case 5: createColorChooser(); break;
			case 6: createDesktop(); break;
			}
		}
	}
	
	
	
	private static class RoundedBorder extends AbstractBorder {
		RoundedBorder(int topInset) {
			this.topInset = topInset;
		}
		int topInset;
		
		@Override
		public Insets getBorderInsets(Component c, Insets insets) {
			insets.left = insets.right = insets.bottom = 5;
			insets.top = topInset;
			return insets;
		}
		@Override
		public Insets getBorderInsets(Component c) {
			return getBorderInsets(c, new Insets(0, 0, 0, 0));
		}
		@Override
		public void paintBorder(Component c, Graphics g, int x, int y,
				int width, int height) {
			Graphics2D g2 = (Graphics2D)g;
			g2.setColor(UIManager.getColor("nimbusBorder"));
			Object hintValue = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
			if (hintValue != RenderingHints.VALUE_ANTIALIAS_ON)
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.drawRoundRect(x+1, y+1, width-2, height-2, 10, 10);
			if (hintValue != RenderingHints.VALUE_ANTIALIAS_ON)
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, hintValue);
		}
	}
	
	private static Border innerBorder = new RoundedBorder(1);
	
	private static Border outerBorder = new RoundedBorder(5);
	
	private static Font titleFont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
	
	static <T extends JComponent> T innerTitled(T c, String title) {
		c.setBorder(createTitledBorder(true, title));
		return c;
	}
	
	static <T extends JComponent> T titled(T c, String title) {
		c.setBorder(createTitledBorder(false, title));
		return c;
	}
	
	static TitledBorder createTitledBorder(boolean inner, String title) {
		Border b = inner ? innerBorder : outerBorder;
		int pos = inner ? TitledBorder.BELOW_TOP : TitledBorder.ABOVE_TOP;
		return new TitledBorder(b, title, TitledBorder.LEADING, pos, titleFont);
	}
	
//	private static <T extends JComponent> T titled(Border b, int p, T c, String t) {
//		c.setBorder(new TitledBorder(b, t, TitledBorder.LEADING, p, titleFont));
//		return c;
//	}

	static JPanel centered(JComponent c) {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(c);
		return panel;
	}
	
	static <T extends JComponent> T disabled(T c) {
		c.setEnabled(false);
		return c;
	}
	
	static JOptionPane createOptionPane(String message, int type) {
		JOptionPane pane = new JOptionPane(message, type);
		String title = message;
		if (type == JOptionPane.QUESTION_MESSAGE) {
			title = "Question Message";
			pane.setOptionType(JOptionPane.YES_NO_CANCEL_OPTION);
		}
		return titled(pane, title);
	}
	
	static JProgressBar progress(int value, boolean paint) {
		JProgressBar bar = new JProgressBar();
		bar.setValue(value);
		bar.setStringPainted(paint);
		return bar;
	}
	
	static JSlider tickedSlider(boolean paintLabels) {
		JSlider s = new JSlider(0, 100);
		s.setMajorTickSpacing(25);
		s.setMinorTickSpacing(5);
		s.setPaintTicks(true);
		s.setPaintLabels(paintLabels);
		return s;
	}
	
	static JPanel createPanel(Class<?> cls, int rows, int cols, Component...components) {
		JPanel panel = new JPanel(new GridLayout(rows, cols, 5, 0));
		for (Component c : components)
			panel.add(c);
		return titled(panel, cls.getSimpleName());
	}
	
	
	static String getPrototypeString(int chars) {
		char[] c = new char[chars];
		Arrays.fill(c, 'w');
		return new String(c);
	}
}
