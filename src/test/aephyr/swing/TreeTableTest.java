package test.aephyr.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.Random;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import aephyr.swing.TreeTable;
import aephyr.swing.mnemonic.*;
import aephyr.swing.treetable.*;

public class TreeTableTest implements Runnable, ItemListener {
	
	private static final String LOOK_AND_FEEL = "Look & Feel";
	
	private static final String LEFT_TO_RIGHT = "Left to Right";
	
	private static final String COLUMN_FOCUS_ENABLED = "Column Focus Enabled";
	
	private static final String VARIABLE_ROW_HEIGHTS = "Variable Row Heights";

	public static void main(String[] args) throws Exception {
//		Utilities.setNimbusLookAndFeel();
//		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		SwingUtilities.invokeLater(new TreeTableTest());
	}

	@Override
	public void run() {
		treeTable = createTreeTable();
		frame = new JFrame(getClass().getSimpleName());
		frame.setJMenuBar(createMenuBar());
		frame.add(new JScrollPane(treeTable), BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		new MnemonicGenerator().addMnemonics(frame.getRootPane());
		frame.setVisible(true);
	}
	
	private JFrame frame;
	
	private TreeTable treeTable;
	
	private TreeTable createTreeTable() {
		TreeTable treeTable = new TreeTable(createNode(4, 3));
//		treeTable.setColumnFocusEnabled(false);
		return treeTable;
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
		JCheckBoxMenuItem cfe = new JCheckBoxMenuItem(COLUMN_FOCUS_ENABLED, treeTable.isColumnFocusEnabled());
		cfe.addItemListener(this);
		properties.add(cfe);
		JCheckBoxMenuItem vrh = new JCheckBoxMenuItem(VARIABLE_ROW_HEIGHTS, treeTable.getRowHeight() <= 0);
		vrh.addItemListener(this);
		properties.add(vrh);
		bar.add(properties);
		return bar;
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
		JMenuItem item = (JMenuItem)e.getSource();
		String txt = item.getText();
		if (txt == LEFT_TO_RIGHT) {
			treeTable.setComponentOrientation(e.getStateChange() == ItemEvent.SELECTED ?
					ComponentOrientation.LEFT_TO_RIGHT : ComponentOrientation.RIGHT_TO_LEFT);
		} else if (txt == COLUMN_FOCUS_ENABLED) {
			treeTable.setColumnFocusEnabled(e.getStateChange() == ItemEvent.SELECTED);
		} else if (txt == VARIABLE_ROW_HEIGHTS) {
			setVariableRowHeights(e.getStateChange() == ItemEvent.SELECTED);
		}
	}
	
	private void setVariableRowHeights(boolean vrh) {
		TableColumn col = treeTable.getColumnModel().getColumn(treeTable.convertColumnIndexToView(1));
		col.setCellRenderer(vrh ? new DefaultTableCellRenderer() {
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				size.height = Integer.parseInt(getText());
				return size;
			}
		} : null);
		treeTable.setRowHeight(-treeTable.getRowHeight());
	}
	
	
	private static Random random = new Random();
	
	private static String createString() {
		char[] c = new char[random.nextInt(5)+4];
		for (int i=c.length; --i>=0;)
			c[i] = (char)('a'+random.nextInt(26));
		return new String(c);
	}
	
	private DefaultTreeTableNode createNode(int depth, int columns) {
		Object[] rowData = new Object[columns];
		for (int i=0; i<columns-2; i++)
			rowData[i] = createString();
		rowData[columns-2] = 15 + random.nextInt(25);
		rowData[columns-1] = random.nextBoolean();
		DefaultTreeTableNode node = new DefaultTreeTableNode(rowData);
		if (--depth >= 0)
			for (int i=2+random.nextInt(3); --i>=0;)
				node.add(createNode(depth, columns));
		return node;
	}


}
