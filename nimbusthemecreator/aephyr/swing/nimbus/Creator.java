package aephyr.swing.nimbus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
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
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.GroupLayout.Alignment;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;


public class Creator extends WindowAdapter implements ActionListener, TableModelListener, PropertyChangeListener {
	

	private static final int TABLE_WIDTH = 450;
	private static final int VALUE_WIDTH = 100;
	private static final int DEFAULT_WIDTH = 50;
	
	public static void main(String[] args) throws Exception {
		File file = null;
		if (args.length > 0) {
			file = new File(args[0]);
			if (!file.isFile())
				file = null;
		}
		final File theme = file;
		Creator.setNimbusLookAndFeel();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new Creator(theme).createFrame();
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setLocation(0, frame.getY());
				frame.setVisible(true);
			}
		});
	}
	
	static void setNimbusLookAndFeel() {
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

	private JFrame frame;
	private JButton update;
	private JTable primaryTable;
	private JTable secondaryTable;
	private JTable otherTable;
	private JComboBox keyFilter;
	private JComboBox keyFilterMethod;
	private JComboBox typeFilter;
	
	private CodeTransfer transfer;
	
	private Creator(File theme) {
		List<String> primary = new ArrayList<String>();
		List<String> secondary = new ArrayList<String>();
		List<String> other = new ArrayList<String>();
		Set<String> filters = new HashSet<String>();
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
		primaryTable = Creator.createUITable(false, 0, Type.Color, primary);
		primaryTable.getModel().addTableModelListener(this);
		secondaryTable = Creator.createUITable(false, 0, Type.Color, secondary);
		otherTable = Creator.createUITable(true, 75, null, other);
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
		
		update = new JButton("Preview");
		update.addActionListener(this);
		
		if (theme != null) {
			try {
				getCodeTransfer().doImport(CodeTransfer.getStatements(theme));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	
	private JFrame createFrame() {
		JScrollPane primary = Preview.titled(new JScrollPane(
				primaryTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), "Primary");
		JScrollPane secondary = Preview.titled(new JScrollPane(
				secondaryTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), "Secondary");
		
		JPanel colors = new JPanel(new StackedTableLayout(3, 10, true));
		colors.add(primary);
		colors.add(secondary);
		Dimension size = new Dimension(Creator.TABLE_WIDTH, primaryTable.getRowHeight()*20);
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
		
		JButton imp = new JButton("Import");
		imp.addActionListener(this);
		JButton exp = new JButton("Export");
		exp.addActionListener(this);
		
		JPanel body = new JPanel(null);
		layout = new GroupLayout(body);
		body.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING, false)
			.addComponent(options)
			.addGroup(layout.createSequentialGroup()
				.addGap(4)
				.addComponent(imp).addComponent(exp)
				.addGap(0, 100, Short.MAX_VALUE)
				.addComponent(update)));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(options)
			.addGroup(layout.createBaselineGroup(false, true)
				.addComponent(imp).addComponent(exp)
				.addComponent(update))
			.addGap(4));
		
		frame = new JFrame(getClass().getSimpleName());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(this);
		frame.add(body, BorderLayout.CENTER);
		return frame;
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
			return size(Creator.TABLE_WIDTH);
		}
		
		private Dimension size(int width) {
			Dimension size = keyFilter.getPreferredSize();
			size.width = width;
			return size;
		}

		@Override
		public void removeLayoutComponent(Component comp) {}
		
	}
	

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
		Creator.setWidth(columns.getColumn(1), typeWidth);
		TableColumn column = columns.getColumn(2);
		Creator.setWidth(column, VALUE_WIDTH);
		column.setCellRenderer(new UIDefaultsRenderer());
		column.setCellEditor(new UIDefaultsEditor());
		Creator.setWidth(columns.getColumn(3), DEFAULT_WIDTH);
		return table;
	}
	private static void setWidth(TableColumn column, int width) {
		column.setPreferredWidth(width);
		column.setResizable(false);
		column.setMinWidth(width);
		column.setMaxWidth(width);
	}
	

	
	
	

	// Event Handling

	@Override
	public void tableChanged(TableModelEvent e) {
		if (e.getSource() == primaryTable.getModel()) {
			UITableModel mdl = (UITableModel)secondaryTable.getModel();
			mdl.fireTableRowsUpdated(0, mdl.getRowCount()-1);
		}
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
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == update) {
			updateUI();
		} else if (e.getSource() == keyFilter ||
				e.getSource() == keyFilterMethod ||
				e.getSource() == typeFilter) {
			updateFilter();
		} else if (e.getActionCommand() == "Import") {
			getCodeTransfer().showImportDialog(frame);
		} else if (e.getActionCommand() == "Export") {
			getCodeTransfer().showExportDialog(frame);
		}
	}
	
	private CodeTransfer getCodeTransfer() {
		if (transfer == null)
			transfer = new CodeTransfer(
					(UITableModel)primaryTable.getModel(),
					(UITableModel)secondaryTable.getModel(),
					(UITableModel)otherTable.getModel());
		return transfer;
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
	
	private Process process;
	private int previewIndex;
	private void updateUI() {
		if (process != null)
			process.destroy();
		try {
			File file = new File("Theme.txt");
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			getCodeTransfer().doExport(writer, null);
			writer.close();
			String[] cmd = {
					"java",
					Preview.class.getName(),
					Integer.toString(frame.getX()+frame.getWidth()),
					Integer.toString(frame.getY()),
					Integer.toString(previewIndex)
					
			};
			process = Runtime.getRuntime().exec(cmd, null, new File("."));
			startStreamReader(
				new StreamReader(process.getInputStream(), System.out) {
					@Override
					void processLine(String line) {
						if (line.startsWith("TabIndex:")) {
							previewIndex = Integer.parseInt(line.substring(9));
						} else {
							super.processLine(line);
						}
					}
				});
			startStreamReader(
				new StreamReader(process.getErrorStream(), System.err));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void startStreamReader(StreamReader reader) {
		Thread thread = new Thread(reader);
		thread.setDaemon(true);
		thread.start();
	}
	
	private static class StreamReader implements Runnable {
		
		StreamReader(InputStream stream, PrintStream print) {
			this.stream = stream;
			this.print = print;
		}
		
		private InputStream stream;
		private PrintStream print;
		
		@Override
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
				for (String line; (line=reader.readLine())!=null;)
					processLine(line);
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		void processLine(String line) {
			print.println(line);
		}
	}
	
	@Override
	public void windowClosing(WindowEvent e) {
		if (process != null)
			process.destroy();
	}
	
	
	
	static void getKeys(List<String> all, List<String> painters) {
		for (Map.Entry<Object,Object> entry : UIManager.getLookAndFeelDefaults().entrySet()) {
			if (!(entry.getKey() instanceof String))
				continue;
			String str = (String)entry.getKey();
			if (Character.isUpperCase(str.charAt(0))) {
				int i = str.indexOf('.');
				if (i < 0)
					continue;
				if (painters != null && str.endsWith("Painter"))
					painters.add(str);
				if (all != null)
					all.add(str);
			}
		}
	}

	
}
