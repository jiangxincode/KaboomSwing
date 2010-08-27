package aephyr.swing.nimbus;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.GroupLayout.Alignment;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.FontUIResource;
import javax.tools.SimpleJavaFileObject;


class CodeTransfer implements ActionListener, ChangeListener {
	
	static final File THEME_FILE = new File("Theme.txt");
	
	private static final String LOCATION_EXPORT = "Save to Directory";
	private static final String LOCATION_IMPORT = "Open File";
	private static final String DIALOG_EXPORT = "Export";
	private static final String DIALOG_IMPORT = "Import";
	private static final String TAB_FILE_EXPORT = "Export to File";
	private static final String TAB_TEXT_EXPORT = "Export to Text";
	private static final String TAB_FILE_IMPORT = "Import from File";
	private static final String TAB_TEXT_IMPORT = "Import from Text";
	private static final int FILE_TAB = 0;
	private static final int TEXT_TAB = 1;
	
	CodeTransfer() {
	}
	
	
	private JDialog dialog;
	
	private JTabbedPane tabs;
	
	private boolean isExport;
	
	private JComponent options;
	private JTextField packageField;
	private JTextField classField;
	private JTextField methodField;
	private JRadioButton indentTabs;
	private JRadioButton indentJava;
	
	private JTextField location;
	private TitledBorder locationBorder;
	
	private JButton ok;
	
	private JTextArea text;
	private boolean validTextArea;
	
	private JFileChooser browse;
	
	void showImportDialog(JFrame frame) {
		maybeInitializeDialog(frame);
		isExport = false;
		location.setText(null);
		locationBorder.setTitle(LOCATION_IMPORT);
		setTitle(FILE_TAB, TAB_FILE_IMPORT);
		setTitle(TEXT_TAB, TAB_TEXT_IMPORT);
		tabs.setSelectedIndex(FILE_TAB);
		options.setVisible(false);
		text.setEditable(true);
		dialog.setTitle(DIALOG_IMPORT);
		ok.getRootPane().setDefaultButton(ok);
		dialog.setVisible(true);
	}
	
	void showExportDialog(JFrame frame) {
		maybeInitializeDialog(frame);
		isExport = true;
		location.setText(null);
		locationBorder.setTitle(LOCATION_EXPORT);
		setTitle(FILE_TAB, TAB_FILE_EXPORT);
		setTitle(TEXT_TAB, TAB_TEXT_EXPORT);
		tabs.setSelectedIndex(FILE_TAB);
		options.setVisible(true);
		text.setEditable(false);
		validTextArea = false;
		dialog.setTitle(DIALOG_EXPORT);
		ok.getRootPane().setDefaultButton(ok);
		dialog.setVisible(true);
	}
	
	private void setTitle(int tab, String title) {
		int mnemonic = tabs.getMnemonicAt(tab);
		tabs.setTitleAt(tab, title);
		int index = title.indexOf(mnemonic);
		if (index >= 0)
			tabs.setDisplayedMnemonicIndexAt(tab, index);
	}
	

	private void setMnemonic(JLabel lab, JComponent c, int mnemonic) {
		lab.setDisplayedMnemonic(mnemonic);
		lab.setLabelFor(c);
	}
	
	private void maybeInitializeDialog(JFrame frame) {
		if (dialog != null)
			return;
		JLabel pkgLabel = new JLabel("Package Name:");
		JLabel clsLabel = new JLabel("Class Name:");
		JLabel mtdLabel = new JLabel("Method Name:");
		JLabel indLabel = new JLabel("Indentation:");
		packageField = new JTextField();
		classField = new JTextField("NimbusTheme");
		methodField = new JTextField("loadTheme");
		indentTabs = new JRadioButton("Tabs", true);
		indentTabs.setMnemonic(KeyEvent.VK_T);
		indentJava = new JRadioButton("Java Convention", false);
		indentJava.setMnemonic(KeyEvent.VK_J);
		setMnemonic(pkgLabel, packageField, KeyEvent.VK_P);
		setMnemonic(clsLabel, classField, KeyEvent.VK_C);
		setMnemonic(mtdLabel, methodField, KeyEvent.VK_M);
		ButtonGroup group = new ButtonGroup();
		group.add(indentTabs);
		group.add(indentJava);
		location = new JTextField(25);
		JButton browse = new JButton("Browse...");
		browse.addActionListener(this);
		browse.setMnemonic(KeyEvent.VK_B);

		options = Preview.titled(new JPanel(null), "Options");
		GroupLayout layout = new GroupLayout(options);
		options.setLayout(layout);
		layout.setHorizontalGroup(layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup(Alignment.TRAILING, false)
				.addComponent(pkgLabel).addComponent(clsLabel).addComponent(mtdLabel).addComponent(indLabel))
			.addGap(5)
			.addGroup(layout.createParallelGroup()
				.addComponent(packageField).addComponent(classField).addComponent(methodField)
				.addGroup(layout.createSequentialGroup()
						.addComponent(indentTabs)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(indentJava))));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGroup(layout.createBaselineGroup(false, true)
				.addComponent(pkgLabel).addComponent(packageField))
			.addGroup(layout.createBaselineGroup(false, true)
				.addComponent(clsLabel).addComponent(classField))
			.addGroup(layout.createBaselineGroup(false, true)
				.addComponent(mtdLabel).addComponent(methodField))
			.addGap(3).addGroup(layout.createBaselineGroup(false, true)
				.addComponent(indLabel).addComponent(indentTabs).addComponent(indentJava)));
		
		JPanel locationPanel = new JPanel(null);
		locationBorder = Preview.createTitledBorder(false, LOCATION_EXPORT);
		locationPanel.setBorder(locationBorder);
		layout = new GroupLayout(locationPanel);
		locationPanel.setLayout(layout);
		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addComponent(location).addComponent(browse));
		int prf = GroupLayout.PREFERRED_SIZE;
		layout.setVerticalGroup(layout.createBaselineGroup(false, true)
				.addComponent(location, prf, prf, prf)
				.addComponent(browse, prf, prf, prf));
		
		JPanel file = new JPanel(null);
		layout = new GroupLayout(file);
		file.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup()
			.addComponent(options).addComponent(locationPanel));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(options, prf, prf, prf).addComponent(locationPanel, prf, prf, prf));
		
		text = new JTextArea();
		text.setEditable(false);
		
		tabs = new JTabbedPane();
		tabs.addChangeListener(this);
		tabs.addTab(TAB_FILE_EXPORT, file);
		tabs.addTab(TAB_TEXT_EXPORT, new JScrollPane(text,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, 
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		tabs.setMnemonicAt(FILE_TAB, KeyEvent.VK_F);
		tabs.setMnemonicAt(TEXT_TAB, KeyEvent.VK_T);
		
		ok = new JButton("OK");
		ok.addActionListener(this);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(this);
		
		JPanel content = new JPanel(null);
		layout = new GroupLayout(content);
		layout.setAutoCreateContainerGaps(true);
		content.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup()
			.addComponent(tabs).addGroup(layout.createSequentialGroup()
				.addGap(0, 200, Short.MAX_VALUE).addComponent(ok)
				.addGap(3).addComponent(cancel).addGap(5)));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(tabs, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
			.addGroup(layout.createBaselineGroup(false, true)
				.addComponent(ok).addComponent(cancel))
			.addGap(5));
		layout.linkSize(SwingConstants.HORIZONTAL, ok, cancel);
		Creator.registerCloseAction(content);
		
		dialog = new JDialog(frame, true);
		dialog.setContentPane(content);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
	}
	
	
	@Override
	public void stateChanged(ChangeEvent e) {
		if (isExport && !validTextArea && tabs.getSelectedIndex() == TEXT_TAB) {
			validTextArea = true;
			try {
				RemoteUIDefaults def = Creator.getUIDefaults();
				if (def != null)
					text.setText(def.export());
			} catch (RemoteException x) {
				x.printStackTrace();
			}
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "Browse...") {
			if (browse == null) {
				browse = new JFileChooser();
				browse.setMultiSelectionEnabled(false);
			}
			browse.setFileSelectionMode(isExport ?
					JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
			if (JFileChooser.APPROVE_OPTION == (isExport ? 
					browse.showSaveDialog(null) : browse.showOpenDialog(null))) {
				File file = browse.getSelectedFile();
				location.setText(file.getPath());
			}
		} else if (e.getActionCommand() == "OK") {
			if (isExport ? doExport() : doImport())
				dispose();
		} else if (e.getActionCommand() == "Cancel") {
			dispose();
		}
	}
	
	private void dispose() {
		text.setText(null);
		dialog.dispose();
	}
	
	/**
	 * @return true if exportation was successful
	 */
	private boolean doExport() {
		RemoteUIDefaults def = Creator.getUIDefaults();
		if (def == null)
			throw new IllegalStateException();
		if (tabs.getSelectedIndex() == FILE_TAB) {
			String pkg = packageField.getText();
			String cls = classField.getText();
			String mtd = methodField.getText();
			File dir = new File(location.getText());
			try {
				if (!dir.isDirectory()) {
					if (dir.isFile())
						return CodeTransfer.error(
								"Invalid location:\n\t" +
								dir.getCanonicalPath() +
								"\nLocation must be a directory.");
					if (!CodeTransfer.confirm(
							"Directory does not exist:\n\t" +
							dir.getCanonicalPath() +
							"\nCreate?"))
						return false;
					dir.mkdirs();
					if (!dir.isDirectory())
						return CodeTransfer.error(
								"Unable to create directory:\n\t" +
								dir.getCanonicalPath());
				}
				File file = new File(dir, cls.concat(".java"));
				if (file.exists()) {
					if (!CodeTransfer.confirm(
							"File already exists:\n\t" +
							file.getCanonicalPath() +
							"\nOverwrite?"))
						return false;
				}
				def.exportTo(file, pkg, cls, mtd, indentTabs.isSelected());
			} catch (IOException x) {
				return CodeTransfer.error("IOException: "+x.getMessage());
			}
		}
		return true;
	}
	
	
	/**
	 * @return true if importation was successful
	 */
	private boolean doImport() {
		String[] statements;
		if (tabs.getSelectedIndex() == FILE_TAB) {
			try {
				File file = new File(location.getText());
				if (!file.isFile())
					return CodeTransfer.error("Invalid File:\n\t" + file.getCanonicalPath());
				statements = getStatements(file);
			} catch (IOException x) {
				return CodeTransfer.error("IOException: "+x.getMessage());
			}
		} else if (tabs.getSelectedIndex() == TEXT_TAB) {
			statements = text.getText().split(";");
		} else {
			throw new IllegalStateException();
		}
		try {
			return Creator.getUIDefaults().importStatements(statements);
		} catch (RemoteException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	static String[] getStatements(File file) throws IOException {
		FileReader reader = new FileReader(file);
		int len = (int)file.length();
		char[] c = new char[len+1];
		len = 0;
		try {
			for (int r, l=c.length; (r=reader.read(c, len, l))>=0;) {
				len += r;
				l -= r;
			}
		} finally {
			reader.close();
		}
		String java = new String(c, 0, len);
		Matcher matcher = Pattern.compile("UIManager\\.put[^;]+").matcher(java);
		ArrayList<String> statements = new ArrayList<String>();
		while (matcher.find())
			statements.add(matcher.group());
		return statements.toArray(new String[statements.size()]);
	}
	

	/**
	 * @param statements an array of statements to interpret
	 * @return true if all statements were successfully interpreted
	 */
	static boolean doImport(String[] statements) {
		ArrayList<Object> error = new ArrayList<Object>();
		LinkedHashMap<Object,Object> map = new LinkedHashMap<Object,Object>();
		doImport(statements, error, map);
		if (map.isEmpty())
			return error("No valid statements were found.");
		if (!error.isEmpty()) {
			if (!dialog(
					"The following statements were not recognized:",
					error,
					"Continue importing the recognized statements?", true))
				return false;
		}
		for (Entry<Object,Object> entry : map.entrySet())
			UIManager.put(entry.getKey(), entry.getValue());
		return true;
	}
	
	static void doImport(String[] statements, List<Object> error, Map<Object,Object> map) {
		Matcher matcher = Pattern.compile(
			"\\QUIManager.put(\\E\"([^\"]+)\",\\s*(.+)\\s*\\)$").matcher("");
		for (String statement : statements) {
			matcher.reset(statement);
			if (matcher.find()) {
				String key = matcher.group(1);
				String value = matcher.group(2);
				try {
					if (value.startsWith("new ")) {
						int idx = value.indexOf('(');
						String name = value.substring(4, idx);
						Class<?> cls;
						if (name.equals("Color")) {
							cls = Color.class;
						} else if (name.equals("Insets")) {
							cls = Insets.class;
						} else if (name.equals("Integer")) {
							cls = Integer.class;
						} else if (name.equals("Font")) {
							cls = Font.class;
						} else if (name.equals("Dimension")) {
							cls = Dimension.class;
						} else {
							throw new Exception();
						}
						String[] params = value.substring(idx+1, value.length()-1).split(",");
						Class[] types = new Class[params.length];
						Object[] args = new Object[params.length];
						for (int i=params.length; --i>=0;) {
							String p = params[i].trim();
							if (Character.isDigit(p.charAt(0))) {
								types[i] = int.class;
								args[i] = new Integer(p.startsWith("0x") ?
										Integer.parseInt(p.substring(2), 16) :
										Integer.parseInt(p));
							} else if (p.charAt(0) == '"') {
								if (p.charAt(p.length()-1) != '"')
									throw new Exception();
								types[i] = String.class;
								// create a new string to tear off the baggage
								// from the statement's char array
								args[i] = new String(p.substring(1, p.length()-1));
							} else if (p.charAt(0) == 'F') {
								p = p.replace("\\s+", "");
								types[i] = int.class;
								if (p.equals("Font.PLAIN")) {
									args[i] = new Integer(Font.PLAIN);
								} else if (p.equals("Font.BOLD")) {
									args[i] = new Integer(Font.BOLD);
								} else if (p.equals("Font.ITALIC")) {
									args[i] = new Integer(Font.ITALIC);
								} else if (p.equals("Font.BOLD|Font.ITALIC") || p.equals("Font.ITALIC|Font.BOLD")) {
									args[i] = new Integer(Font.BOLD | Font.ITALIC);
								} else {
									types[i] = String.class;
									if (p.equals("Font.DIALOG")) {
										args[i] = Font.DIALOG;
									} else if (p.equals("Font.DIALOG_INPUT")) {
										args[i] = Font.DIALOG_INPUT;
									} else if (p.equals("Font.MONOSPACED")) {
										args[i] = Font.MONOSPACED;
									} else if (p.equals("Font.SANS_SERIF")) {
										args[i] = Font.SANS_SERIF;
									} else if (p.equals("Font.SERIF")) {
										args[i] = Font.SERIF;
									} else {
										throw new Exception();
									}
								}
							} else {
								throw new Exception();
							}
						}
						map.put(key, cls.getConstructor(types).newInstance(args));
					} else if (value.charAt(0) == 'B') {
						if (value.equals("Boolean.TRUE")) {
							map.put(key, Boolean.TRUE);
						} else if (value.equals("Boolean.FALSE")) {
							map.put(key, Boolean.FALSE);
						} else {
							throw new Exception();
						}
					} else if (value.charAt(0) == '"' && value.charAt(value.length()-1) == '"') {
						map.put(key, new String(value.substring(1, value.length()-1)));
					} else {
						throw new Exception();
					}
					continue;
				} catch (Exception x) {
					x.printStackTrace(System.out);
					// any exceptions (e.g NumberFormatException or the jungle of exceptions thrown by newInstance)
					// should be caught and the statement added to the error list
					// also, plain Exceptions are thrown above if a statement isn't recognized
				}
			} else {
				if (statement.trim().isEmpty())
					continue;
			}
			error.add(statement);
		}
	}
	
	private static boolean dialog(String north, List<Object> error, String south, boolean confirm) {
		JPanel message = new JPanel(new BorderLayout());
		if (north != null)
			message.add(new JLabel(north), BorderLayout.NORTH);
		message.add(new JScrollPane(new JList(error.toArray())), BorderLayout.CENTER);
		if (south != null)
			message.add(new JLabel(south), BorderLayout.SOUTH);
		return confirm ? confirm(message) : error(message);
	}

	/**
	 * @param msg confirmation message
	 * @return true if the user confirmed
	 */
	private static boolean confirm(Object msg) {
		return JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
				null, msg, "Confirm",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
	}

	/**
	 * @param msg error message
	 * @return always returns false
	 */
	private static boolean error(Object msg) {
		JOptionPane.showMessageDialog(
				null, msg, "Error", JOptionPane.ERROR_MESSAGE);
		return false;
	}
	
	

	/**
	 * Implementation from JavaDoc for {@link javax.tools.JavaCompiler} (JavaSourceFromString)
	 */
	static class MemoryFileObject extends SimpleJavaFileObject {
		final String code;

		MemoryFileObject(String name, String code) {
			super(URI.create("string:///" + name.replace('.','/') + Kind.SOURCE.extension),Kind.SOURCE);
			this.code = code;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return code;
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	static void doExport(File file, String pkg, String cls, String mtd, boolean tabs) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		if (pkg != null) {
			writer.write("package ");
			writer.write(pkg);
			writer.write(';');
			writer.newLine();
			writer.newLine();
		}
		String prefix = null;
		if (cls != null) {
			writer.write("import javax.swing.*;");
			writer.newLine();
			writer.write("import java.awt.*;");
			writer.newLine();
			writer.newLine();
			writer.write("public class ");
			writer.write(cls);
			writer.write(" {");
			writer.newLine();
			writer.write(tabs ? "\t" : "    ");
			writer.write("public static void ");
			writer.write(mtd);
			writer.write("() {");
			writer.newLine();
			prefix = tabs ? "\t\t" : "\t";
		}
		doExport(writer, prefix);
		if (cls != null) {
			writer.write(tabs ? "\t" : "    ");
			writer.write('}');
			writer.newLine();
			writer.write('}');
		}
		writer.close();
	}
	
	static void doExport(Writer writer, String prefix) throws IOException {
		BufferedWriter buf = writer instanceof BufferedWriter ? (BufferedWriter)writer : null;
		for (Object ky : UIManager.getDefaults().keySet()) {
			if (!(ky instanceof String))
				continue;
			String key = (String)ky;
			Object value = UIManager.get(ky);
			Type type = Type.getType(value);
				switch (type) {
				// unsupported types
				case Painter: case Icon: case Object:
					continue;
				}
				if (prefix != null)
					writer.write(prefix);
				writer.write("UIManager.put(\"");
				writer.write(key.toString());
				switch (type) {
				case Color:
					Color color = (Color)value;
//				writer.write("\", new ColorUIResource(0x");
					writer.write("\", new Color(0x");
					writer.write(Integer.toHexString(color.getRGB() & 0xffffff));
					writer.write("));");
					break;
				case Painter:
					throw new IllegalStateException();
				case Insets:
					Insets insets = (Insets)value;
//				writer.write("\", new InsetsUIResource(");
					writer.write("\", new Insets(");
					writer.write(Integer.toString(insets.top));
					writer.write(", ");
					writer.write(Integer.toString(insets.left));
					writer.write(", ");
					writer.write(Integer.toString(insets.bottom));
					writer.write(", ");
					writer.write(Integer.toString(insets.right));
					writer.write("));");
					break;
				case Font:
					Font font = (Font)value;
					//writer.write("\", new FontUIResource(\"");
					writer.write("\", new Font(\"");
					writer.write(font.getFamily());
					writer.write("\", ");
					String style = font.isBold() ? "Font.BOLD" : null;
					style = font.isItalic() ?
							style == null ? "Font.ITALIC" : style + " | " + "Font.ITALIC"
									: "Font.PLAIN";
					writer.write(style);
					writer.write(", ");
					writer.write(Integer.toString(font.getSize()));
					writer.write("));");
					break;
				case Boolean:
					writer.write("\", Boolean.");
					writer.write(value == Boolean.TRUE ? "TRUE" : "FALSE");
					writer.write(");");
					break;
				case Integer:
					writer.write("\", new Integer(");
					writer.write(value.toString());
					writer.write("));");
					break;
				case String:
					writer.write("\", \"");
					writer.write(value.toString());
					writer.write('"');
					writer.write(");");
					break;
				case Icon:
					throw new IllegalStateException();
				case Dimension:
					Dimension size = (Dimension)value;
					writer.write("\", new Dimension(");
//				writer.write("\", new DimensionUIResource(");
					writer.write(Integer.toString(size.width));
					writer.write(", ");
					writer.write(Integer.toString(size.height));
					writer.write("));");
					break;
				case Object:
					throw new IllegalStateException();
				}
				if (buf != null) {
					buf.newLine();
				} else {
					writer.write('\n');
				}
			}
	}
}
