package test.aephyr.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.tree.*;

import aephyr.swing.TreeModelTransformer;

public class TreeSort extends MouseAdapter implements Runnable, ActionListener, MenuListener, ItemListener {
	

	private static int bellCurve(Random random, int n) {
		int i = Integer.bitCount(random.nextInt());
		if (i < n)
			return i;
		return i-n+1;
	}
	
	private static void testBellCurve(Random r, int n) {
		int[] counts = new int[32-n];
		for (int i=10000; --i>=0;) {
			counts[bellCurve(r, n)]++;
		}
		System.out.println(n);
		for (int i=0; i<counts.length;) {
			int j = Math.min(i+5, counts.length);
			for (; i<j; i++) {
				System.out.print("\t\t");
				System.out.print(i);
				System.out.print(": ");
				System.out.print(counts[i]);
			}
			System.out.println();
			i = j;
		}
	}
	
	private static void testMutate(Random random, int count) {
		int mutateCount = random.nextInt(count)+1;
		System.err.println("count: "+count + "\t mutateCount: " + mutateCount);
		for (int i=mutateCount, offset=-1; --i>=0;) {
			int length = (count-offset-1)/(i+1);
			int j = random.nextInt(length)+1;
			int idx = offset+j;
			offset += j;
			System.err.println("\tidx: "+idx+"\toffset: "+offset+"\tlength: "+length);
		}
	}
	
	public static void main(String[] args) throws Exception {
//		Random r = new Random();
////		for (int i=16; --i>=5;) {
////			testBellCurve(r, i);
////		}
//		for (int i=10; --i>=0;) {
//			testMutate(r, r.nextInt(6)+3);
//		}
//		if (r != null)
//			return;
		
		for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
			if ("Nimbus".equals(info.getName())) {
				UIManager.setLookAndFeel(info.getClassName());
				break;
			}
		}
		SwingUtilities.invokeLater(new TreeSort());
	}
	
	@Override
	public void run() {
		random = new Random();
		model = new DefaultTreeModel(createTreeNode("", 4));
		transformTree = new JTree(model);
		transformModel = new TreeModelTransformer(transformTree, model);
		transformTree.setModel(transformModel);
		transformTree.setRowHeight(20);
		transformTree.setRootVisible(false);
		transformTree.setShowsRootHandles(true);
		
		FlowLayout flow = new FlowLayout(FlowLayout.LEADING, 0, 0);
		flow.setAlignOnBaseline(true);
		Border border = BorderFactory.createEmptyBorder(0, 5, 0, 3);

		JMenuBar bar = new JMenuBar();
		bar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		
		JMenu menu = createMenu("Sort");
		ButtonGroup group = new ButtonGroup();
		addRadio(menu, "Ascending", false, group);
		addRadio(menu, "Descending", false, group);
		addRadio(menu, "Unsorted", true, group);
		bar.add(menu);
		
		menu = createMenu("Filter");
		menu.add(createTextPanel(flow, border, "Regex: ", "Filter"));
		bar.add(menu);

		JScrollPane left = new JScrollPane(transformTree);
		left.setColumnHeaderView(bar);

		tree = new JTree(model);
		tree.setRowHeight(20);
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setEditable(true);
		tree.setToggleClickCount(-1);
		tree.addMouseListener(this);
		
		JPopupMenu popup = new JPopupMenu();
		popup.add("Insert").addActionListener(this);
		popup.add("Remove").addActionListener(this);
		popup.add("Change").addActionListener(this);
		popup.addSeparator();
		popup.add("NodesChanged on Random Children").addActionListener(this);
		popup.add("NodesChanged on All Children").addActionListener(this);
		tree.setComponentPopupMenu(popup);
		
		JMenuBar insertBar = new JMenuBar();
		insertBar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		menu = createMenu("Insert");
		menu.add(createTextPanel(flow, border, "Insert: ", "Insert"));
		menu.addSeparator();
		menu.add("Use popup for other changes");
		insertBar.add(menu);
		
		JScrollPane right = new JScrollPane(tree);
		right.setColumnHeaderView(insertBar);
		
		JPanel grid = new JPanel(new GridLayout(1, 2));
		grid.add(left);
		grid.add(right);
		
		JFrame frame = new JFrame(getClass().getSimpleName());
		frame.add(grid, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 600);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);		
	}
	private JMenu createMenu(String text) {
		JMenu menu = new JMenu(text);
		menu.addMenuListener(this);
		return menu;
	}
	private void addRadio(JMenu menu, String text, boolean sel, ButtonGroup group) {
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(text, sel);
		group.add(item);
		menu.add(item);
		item.addItemListener(this);
	}
	private JPanel createTextPanel(FlowLayout layout, Border border, String label, String cmd) {
		JPanel panel = new JPanel(layout);
		panel.setBorder(border);
		panel.setOpaque(false);
		JLabel lbl = new JLabel(label);
		panel.add(lbl);
		JTextField field = new JTextField(13);
		field.setActionCommand(cmd);
		field.addActionListener(this);
		panel.add(field);
		return panel;
	}
	
	private Random random;
	
	private DefaultTreeModel model;
	
	private TreeModelTransformer<?> transformModel;
	
	private JTree tree;
	
	private JTree transformTree;
	
	private int bellCurve() {
		int i = Integer.bitCount(random.nextInt());
		if (i < 13)
			return i;
		return i-12;
	}
	
	private MutableTreeNode createTreeNode(String text, int depth) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(text);
		if (--depth >= 0)
			for (int i=bellCurve(); --i>=0;)
				node.add(createTreeNode(createCellValue(), depth));
		return node;
	}
	
	private String createCellValue() {
		char[] c = new char[bellCurve()+2];
		for (int i=c.length; --i>=0;)
			c[i] = (char)(random.nextInt(26)+'a');
		c[0] = Character.toUpperCase(c[0]);
		return new String(c);
	}
	

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
			TreePath path = tree.getPathForLocation(e.getX(), e.getY());
			if (path == null)
				return;
			tree.startEditingAtPath(path);
		}
	}
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() != ItemEvent.SELECTED)
			return;
		AbstractButton button = (AbstractButton)e.getSource();
		String text = button.getText();
		transformModel.setSortOrder(SortOrder.valueOf(text.toUpperCase()));
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "Filter") {
			JTextField filter = (JTextField)e.getSource();
			String regex = filter.getText();
			transformModel.setFilter(regex.isEmpty() ? null :
					new TreeModelTransformer.RegexFilter(Pattern.compile(regex), false));
			MenuSelectionManager.defaultManager().clearSelectedPath();
		} else if (e.getActionCommand() == "Insert") {
			TreePath path = tree.getSelectionPath();
			MutableTreeNode parent;
			int index;
			if (path != null) {
				MutableTreeNode node = getNode(path);
				parent = (MutableTreeNode)node.getParent();
				index = parent.getIndex(node);
			} else {
				path = tree.getAnchorSelectionPath();
				parent = (MutableTreeNode)(path == null ?
						model.getRoot() : getNode(path).getParent());
				index = model.getChildCount(parent);
			}
			String text;
			if (e.getSource() instanceof JTextField) {
				JTextField field = (JTextField)e.getSource();
				text = field.getText();
				MenuSelectionManager.defaultManager().clearSelectedPath();
			} else {
				text = createCellValue();
			}
			MutableTreeNode node = createTreeNode(text,
					path == null ? 4 : (5-path.getPathCount()));
			model.insertNodeInto(node, parent, index);
			path = path == null ? new TreePath(parent) : path.getParentPath();
			tree.setSelectionPath(path.pathByAddingChild(node));
		} else if (e.getActionCommand() == "Remove") {
			TreePath[] paths = tree.getSelectionPaths();
			if (paths == null)
				return;
			for (TreePath path : paths)
				model.removeNodeFromParent(getNode(path));
		} else if (e.getActionCommand() == "Change") {
			TreePath[] paths = tree.getSelectionPaths();
			if (paths == null)
				return;
			for (TreePath path : paths) {
				MutableTreeNode node = getNode(path);
				node.setUserObject(createCellValue());
				model.nodeChanged(node);
			}
		} else if (e.getActionCommand() == "NodesChanged on Random Children") {
			changeChildNodes(true);
		} else if (e.getActionCommand() == "NodesChanged on All Children") {
			changeChildNodes(false);
		}
	}
	
	private static MutableTreeNode getNode(TreePath path) {
		return (MutableTreeNode)path.getLastPathComponent();
	}
	
	private void changeChildNodes(boolean rdm) {
		TreePath[] paths = tree.getSelectionPaths();
		if (paths == null)
			return;
		for (TreePath path : paths) {
			MutableTreeNode node = getNode(path);
			int count = node.getChildCount();
			int mutateCount = rdm ? random.nextInt(count)+1 : count;
			int[] indices = new int[mutateCount];
			for (int i=mutateCount, offset=-1; --i>=0;) {
				int length = (count-offset-1)/(i+1);
				int j = random.nextInt(length)+1;
				int idx = offset+j;
				indices[mutateCount-i-1] = idx;
				MutableTreeNode child = (MutableTreeNode)model.getChild(node, idx);
				child.setUserObject(createCellValue());
				offset += j;
			}
			model.nodesChanged(node, indices);
		}

	}
	
	
	@Override
	public void menuCanceled(MenuEvent e) {}

	@Override
	public void menuDeselected(MenuEvent e) {}

	@Override
	public void menuSelected(MenuEvent e) {
		JMenu menu = (JMenu)e.getSource();
		JPopupMenu popup = menu.getPopupMenu();
		Dimension size = popup.getPreferredSize();
		Container menubar = menu.getParent();
		menu.setMenuLocation(menubar.getWidth()-menu.getX()-size.width, menu.getHeight());
	}
}
