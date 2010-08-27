package aephyr.swing.nimbus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultRowSorter;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;



public class Creator extends WindowAdapter implements ActionListener, ChangeListener,
		TableModelListener, TableColumnModelListener, PropertyChangeListener {


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
		final Creator creator = new Creator();
		Creator.openRemoteUIDefaults(file);
		synchronized (Creator.class) {
			if (defaults == null)
				Creator.class.wait();
		}
		Creator.setNimbusLookAndFeel();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = creator.createFrame();
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
	private JCheckBox preview;
	private int previewIndex;

	private JTable primaryTable;
	private JTable secondaryTable;
	
	private JTable otherTable;
	private JComboBox keyFilter;
	private JComboBox keyFilterMethod;
	private JComboBox typeFilter;
	private JViewport filtersViewport;

	private CodeTransfer transfer;
	
	private RemoteController controllerImpl;

	
	private Creator() throws RemoteException {
		Registry registry = LocateRegistry.createRegistry(1099);
		controllerImpl = new RemoteController() {
			@Override
			public void previewClosed() throws RemoteException {
				preview.setSelected(false);
			}
			@Override
			public void ready() throws RemoteException {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						initiateRemoteUIDefaults();
					}
				});
			}
			@Override
			public void setTabIndex(int index) throws RemoteException {
				previewIndex = index;
			}
		};
		RemoteController controller = (RemoteController)UnicastRemoteObject.exportObject(
				controllerImpl, 0);
		registry.rebind(RemoteController.class.getName(), controller);
	}
	
	private void init() {
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

		Actions editAction = new Actions(Actions.EDIT);
		primaryTable = Creator.createUITable(false, 0, Type.Color, primary, editAction);
		primaryTable.getModel().addTableModelListener(this);
		secondaryTable = Creator.createUITable(false, 0, Type.Color, secondary, editAction);
		secondaryTable.getModel().addTableModelListener(this);
		otherTable = Creator.createUITable(true, 75, null, other, editAction);

		String[] filterArray = filters.toArray(new String[filters.size()+1]);
		filterArray[filterArray.length-1] = "";
		Arrays.sort(filterArray);
		keyFilter = new JComboBox(filterArray);
		keyFilter.setToolTipText("Filter Key Column");
		keyFilter.setEditable(true);
		keyFilter.addActionListener(this);
		String[] methods = {STARTS_WITH, ENDS_WITH, CONTAINS, REGEX};
		keyFilterMethod = new JComboBox(methods);
		for (String method : methods) {
			int idx = method.indexOf('\u0332');
			if (idx > 0) {
				Actions act = new Actions(Actions.MNEMONIC);
				act.putValue(Actions.MNEMONIC, method);
				int mnemonic = (int)Character.toUpperCase(method.charAt(idx-1));
				act.registerMnemonic(keyFilterMethod, mnemonic, false);
			}
		}
		keyFilterMethod.addActionListener(this);
		Object[] types = Type.values();
		Object[] typeArray = new Object[types.length+1];
		System.arraycopy(types, 0, typeArray, 1, types.length);
		typeArray[0] = "";
		typeFilter = new JComboBox(typeArray);
		typeFilter.setToolTipText("Filter Type Column");
		typeFilter.addActionListener(this);

		preview = new JCheckBox("Show Preview", false);
		preview.setMnemonic(KeyEvent.VK_H);
		update = new JButton("Update");
		update.addActionListener(this);
		update.setMnemonic(KeyEvent.VK_U);
		
		baseDirty = false;
	}
	
	private static class Actions extends AbstractAction {
		
		static final String MNEMONIC = "Mnemonic";
		
		static final String EDIT = "Edit";
		
		static final String SORT = "Sort";
		
		static final String CLOSE = "Close";
			
		Actions(String name) {
			super(name);
		}
		
		public void actionPerformed(ActionEvent e) {
			Object value = getValue(NAME);
			if (value == MNEMONIC) {
				JComponent c = (JComponent)e.getSource();
				c.requestFocusInWindow();
				value = getValue(MNEMONIC);
				if (value != null) {
					JComboBox combo = (JComboBox)c;
					combo.setSelectedItem(value);
				}
			} else if (value == EDIT) {
				JTable table = (JTable)e.getSource();
				int row = table.getSelectionModel().getLeadSelectionIndex();
				if (row < 0 || row >= table.getRowCount())
					return;
				int col = UITableModel.VALUE_COLUMN_INDEX;
				table.editCellAt(row, table.convertColumnIndexToView(col));
			} else if (value == SORT) {
				value = getValue(SORT);
				JTable table = (JTable)e.getSource();
				for (int col=table.getColumnCount(); --col>=0;) {
					if (value.equals(table.getColumnName(col))) {
						table.getRowSorter().toggleSortOrder(
								table.convertColumnIndexToModel(col));
						break;
					}
				}
			} else if (value == CLOSE) {
				SwingUtilities.getWindowAncestor((Component)e.getSource()).dispose();
			}
		}
		
		void registerMnemonic(JComponent c, int mnemonic, boolean ctrl) {
			int mod = InputEvent.ALT_DOWN_MASK;
			if (ctrl)
				mod |= InputEvent.CTRL_DOWN_MASK;
			c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
					KeyStroke.getKeyStroke(mnemonic, mod), this);
			c.getActionMap().put(this, this);
			
		}
	}
	
	static void registerCloseAction(JComponent c) {
		Actions act = new Actions(Actions.CLOSE);
		c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), act);
		c.getActionMap().put(act, act);
	}
	
	private static void registerSortMnemonics(JTable t) {
		for (int col=t.getColumnCount(); --col>=0;) {
			String name = t.getColumnName(col);
			int idx = name.indexOf('\u0332');
			if (idx > 0) {
				int mnemonic = (int)Character.toUpperCase(name.charAt(idx-1));
				Actions act = new Actions(Actions.SORT);
				act.putValue(Actions.SORT, name);
				t.getInputMap(JComponent.WHEN_FOCUSED).put(
						KeyStroke.getKeyStroke(mnemonic, InputEvent.ALT_DOWN_MASK), act);
				t.getActionMap().put(act, act);
			}
		}
	}

	private JFrame createFrame() {
		init();
		JScrollPane primary = Preview.innerTitled(new JScrollPane(
				primaryTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), "P\u0332rimary");
		JScrollPane secondary = Preview.innerTitled(new JScrollPane(
				secondaryTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), "S\u0332econdary");
		Actions mnemonicAction = new Actions(Actions.MNEMONIC);
		mnemonicAction.registerMnemonic(primaryTable, KeyEvent.VK_P, false);
		mnemonicAction.registerMnemonic(secondaryTable, KeyEvent.VK_S, false);
		mnemonicAction.registerMnemonic(otherTable, KeyEvent.VK_A, false);
		mnemonicAction.registerMnemonic(keyFilter, KeyEvent.VK_K, true);
		mnemonicAction.registerMnemonic(typeFilter, KeyEvent.VK_T, true);
		registerSortMnemonics(primaryTable);
		registerSortMnemonics(secondaryTable);
		registerSortMnemonics(otherTable);
		
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
		TableColumnModel model = otherTable.getColumnModel();
		model.getColumn(UITableModel.KEY_COLUMN_INDEX).addPropertyChangeListener(this);
		model.addColumnModelListener(this);
		filtersViewport = new JViewport();
		filtersViewport.setView(filters);
		other.getViewport().addChangeListener(this);
		
		GroupLayout layout = new GroupLayout(otherPanel);
		otherPanel.setLayout(layout);
		layout.setHorizontalGroup(layout.createSequentialGroup()
			.addGap(2)
			.addGroup(layout.createParallelGroup()
				.addComponent(filtersViewport).addComponent(other)));
		final int prf = GroupLayout.PREFERRED_SIZE;
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGap(2).addComponent(other)
			.addComponent(filtersViewport, prf, prf, prf));


		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("UI Base", colors);
		tabs.setMnemonicAt(0, KeyEvent.VK_B);
		tabs.addTab("UI Controls", otherPanel);
		tabs.setMnemonicAt(1, KeyEvent.VK_C);
		tabs.addChangeListener(this);

		JButton imp = new JButton("Import");
		imp.setMnemonic(KeyEvent.VK_M);
		imp.addActionListener(this);
		JButton exp = new JButton("Export");
		exp.setMnemonic(KeyEvent.VK_X);
		exp.addActionListener(this);

		Box south = Box.createHorizontalBox();
		south.setBorder(BorderFactory.createEmptyBorder(0, 3, 3, 3));
		south.add(imp);
		south.add(exp);
		south.add(Box.createHorizontalGlue());
		south.add(preview);
		south.add(Box.createHorizontalStrut(3));
		south.add(update);

		frame = new JFrame(getClass().getSimpleName());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(this);
		frame.add(tabs, BorderLayout.CENTER);
		frame.add(south, BorderLayout.SOUTH);
		return frame;
	}


	private class FiltersLayout implements LayoutManager {

		@Override
		public void addLayoutComponent(String name, Component comp) {}

		@Override
		public void layoutContainer(Container parent) {
			int keyViewIndex = otherTable.convertColumnIndexToView(
					UITableModel.KEY_COLUMN_INDEX);
			int typeViewIndex = otherTable.convertColumnIndexToView(
					UITableModel.TYPE_COLUMN_INDEX);
			TableColumnModel mdl = otherTable.getColumnModel();
			int keyColWidth = mdl.getColumn(keyViewIndex).getWidth();
			Dimension methodSize = keyFilterMethod.getPreferredSize();
			int keyWidth = keyColWidth - methodSize.width - 10;
			Dimension typeSize = typeFilter.getPreferredSize();
			
			int x = 0;
			if (keyViewIndex != 0) {
				for (int col=keyViewIndex; --col>=0;)
					x += mdl.getColumn(col).getWidth();
			}
			if (typeViewIndex == keyViewIndex-1) {
				x += typeSize.width - mdl.getColumn(typeViewIndex).getWidth();
			}
			keyFilter.setBounds(x, 0, keyWidth, methodSize.height);
			keyFilterMethod.setBounds(x+keyWidth, 0, methodSize.width, methodSize.height);
			
			if (typeViewIndex == keyViewIndex+1) {
				x += keyColWidth;
			} else {
				x = 0;
				for (int col=typeViewIndex; --col>=0;) {
					x += mdl.getColumn(col).getWidth();
				}
			}
			typeFilter.setBounds(x, 0, typeSize.width, typeSize.height);
		}

		@Override
		public Dimension minimumLayoutSize(Container parent) {
			return size(otherTable.getWidth());
		}

		@Override
		public Dimension preferredLayoutSize(Container parent) {
			return size(otherTable.getWidth());
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
			List<String> lst, Actions editAction) {
		String[] keys = lst.toArray(new String[lst.size()]);
		Arrays.sort(keys);
		TableModel mdl = type == null ? new UITableModel(keys) : new UITypeTableModel(keys, type, true);
		JTable table = new UITable(mdl, null);
		table.putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setRowHeight(25);
		TableColumnModel columns = table.getColumnModel();
		int keyWidth = TABLE_WIDTH-typeWidth-VALUE_WIDTH-DEFAULT_WIDTH;
		columns.getColumn(UITableModel.KEY_COLUMN_INDEX).setMinWidth(keyWidth);
		columns.getColumn(UITableModel.KEY_COLUMN_INDEX).setPreferredWidth(keyWidth);
		columns.getColumn(UITableModel.KEY_COLUMN_INDEX).setResizable(keyColumnResizable);
		Creator.setWidth(columns.getColumn(UITableModel.TYPE_COLUMN_INDEX), typeWidth);
		TableColumn column = columns.getColumn(UITableModel.VALUE_COLUMN_INDEX);
		Creator.setWidth(column, VALUE_WIDTH);
		column.setCellRenderer(new UIDefaultsRenderer());
		column.setCellEditor(new UIDefaultsEditor());
		Creator.setWidth(columns.getColumn(UITableModel.DEFAULT_COLUMN_INDEX), DEFAULT_WIDTH);
		table.setAutoCreateRowSorter(true);
		DefaultRowSorter<?,?> sorter = (DefaultRowSorter<?,?>)table.getRowSorter();
		sorter.setSortable(UITableModel.VALUE_COLUMN_INDEX, false);
		table.getInputMap(JComponent.WHEN_FOCUSED).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), editAction);
		table.getActionMap().put(editAction, editAction);
		return table;
	}
	private static void setWidth(TableColumn column, int width) {
		column.setPreferredWidth(width);
		column.setResizable(false);
		column.setMinWidth(width);
		column.setMaxWidth(width);
	}



	private boolean baseDirty;


	// Event Handling

	@Override
	public void tableChanged(TableModelEvent e) {
		if (e.getSource() == primaryTable.getModel()) {
			((UITableModel)secondaryTable.getModel()).clearCache();
		}
		baseDirty = true;
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if ("width".equals(e.getPropertyName())) {
			revalidateFilterLayout();
		}
	}
	
	private void revalidateFilterLayout() {
		JComponent c = (JComponent)keyFilter.getParent();
		if (c != null) {
			c.revalidate();
			c.repaint();
		}
	}

	@Override
	public void columnAdded(TableColumnModelEvent e) {
		revalidateFilterLayout();
	}

	@Override
	public void columnMarginChanged(ChangeEvent e) {
		revalidateFilterLayout();
	}

	@Override
	public void columnMoved(TableColumnModelEvent e) {
		revalidateFilterLayout();
	}

	@Override
	public void columnRemoved(TableColumnModelEvent e) {
		revalidateFilterLayout();
	}

	@Override
	public void columnSelectionChanged(ListSelectionEvent e) {}

	

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == update) {
//			showPreview = true;
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
	

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() instanceof JViewport) {
			JViewport port = (JViewport)e.getSource();
			Point tablePos = port.getViewPosition();
			Point filtersPos = filtersViewport.getViewPosition();
			if (filtersPos.x != tablePos.x) {
				filtersPos.x = tablePos.x;
				filtersViewport.setViewPosition(filtersPos);
			}
		} else if (e.getSource() instanceof JTabbedPane) {
			JTabbedPane tabs = (JTabbedPane)e.getSource();
			if (baseDirty && tabs.getSelectedIndex() != 0) {
				UITableModel mdl = (UITableModel)otherTable.getModel();
				mdl.clearCache();
			}
		}
	}

	@Override
	public void windowClosing(WindowEvent e) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				closeRemoteUIDefaults(false);
				try {
					Registry registry = LocateRegistry.getRegistry();
					registry.unbind(RemoteController.class.getName());
					UnicastRemoteObject.unexportObject(controllerImpl, true);
				} catch (RemoteException x) {
					x.printStackTrace();
				} catch (NotBoundException x) {
					x.printStackTrace();
				}
				if (CodeTransfer.THEME_FILE.isFile())
					CodeTransfer.THEME_FILE.delete();
			}
		});
		thread.start();
	}

	private CodeTransfer getCodeTransfer() {
		if (transfer == null)
			transfer = new CodeTransfer();
		return transfer;
	}

	private static final String STARTS_WITH = "S\u0332tarts With";
	
	private static final String ENDS_WITH = "E\u0332nds With";
	
	private static final String CONTAINS = "Co\u0332ntains";
	
	private static final String REGEX = "R\u0332egex";

	private void updateFilter() {
		DefaultRowSorter<TableModel,Object> sorter =
			(DefaultRowSorter<TableModel,Object>)otherTable.getRowSorter();
		String key = keyFilter.getSelectedItem().toString();
		RowFilter<TableModel,Object> filter = null;
		if (!key.isEmpty()) {
			Object method = keyFilterMethod.getSelectedItem();
			if (method == STARTS_WITH) {
				filter = RowFilter.regexFilter(
						'^'+Pattern.quote(key), UITableModel.KEY_COLUMN_INDEX);
			} else if (method == ENDS_WITH) {
				filter = RowFilter.regexFilter(
						Pattern.quote(key)+'$', UITableModel.KEY_COLUMN_INDEX);
			} else if (method == CONTAINS) {
				filter = RowFilter.regexFilter(
						Pattern.quote(key), UITableModel.KEY_COLUMN_INDEX);
			} else {
				filter = RowFilter.regexFilter(
						key, UITableModel.KEY_COLUMN_INDEX);
			}
		}
		String type = typeFilter.getSelectedItem().toString();
		if (!type.isEmpty()) {
			RowFilter<TableModel,Object> typeFilter = RowFilter.regexFilter(
					'^'+type+'$', UITableModel.TYPE_COLUMN_INDEX);
			filter = filter == null ? typeFilter :
				RowFilter.<TableModel,Object>andFilter(Arrays.asList(filter, typeFilter));
		}
		sorter.setRowFilter(filter);
	}



	static RemoteUIDefaults getUIDefaults() {
		return defaults;
	}
	
	private static RemoteUIDefaults defaults;
	
	private static volatile Process defaultsProcess;
	
	private static void closeRemoteUIDefaults(boolean doExport) {
		Process process = defaultsProcess;
		if (process != null) {
			defaultsProcess = null;
			try {
				if (doExport)
					defaults.exportTo(CodeTransfer.THEME_FILE, null, null, null, false);
				OutputStream out = process.getOutputStream();
				out.write(0);
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				process.waitFor();
			} catch (InterruptedException e) {}
		}
	}
	
	private void updateUI() {
		if (preview.isSelected()) {
			openRemoteUIDefaults(
				"java", RemoteUIDefaultsImpl.class.getName(),
				CodeTransfer.THEME_FILE.getPath(),
				Integer.toString(frame.getX()+frame.getWidth()),
				Integer.toString(frame.getY()),
				Integer.toString(previewIndex));
		} else {
			openRemoteUIDefaults(CodeTransfer.THEME_FILE);
		}
	}
	
	private static void openRemoteUIDefaults(File theme) {
		openRemoteUIDefaults(
			"java", RemoteUIDefaultsImpl.class.getName(),
			theme == null ? "" : theme.getPath());
	}
	
	private static void openRemoteUIDefaults(String...cmd) {
		closeRemoteUIDefaults(true);
		try {
			Process process = Runtime.getRuntime().exec(cmd, null, new File("."));
			defaultsProcess = process;
			startStreamReader(
				new StreamReader(process.getInputStream(), System.out));
			startStreamReader(
				new StreamReader(process.getErrorStream(), System.err));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initiateRemoteUIDefaults() {
		RemoteUIDefaults def = null;
		try {
			Registry registry = LocateRegistry.getRegistry();
			def = (RemoteUIDefaults)registry.lookup(RemoteUIDefaults.class.getName());
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		synchronized (Creator.class) {
			defaults = def;
			Creator.class.notifyAll();
		}
		if (primaryTable != null) {
			((UITableModel)primaryTable.getModel()).clearCache();
			((UITableModel)secondaryTable.getModel()).clearCache();
			((UITableModel)otherTable.getModel()).clearCache();
		}
	}
	

	private static void startStreamReader(StreamReader reader) {
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
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		void processLine(String line) {
			print.println(line);
		}
		
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
