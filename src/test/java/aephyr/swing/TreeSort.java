package aephyr.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.tree.*;

import aephyr.swing.TreeModelTransformer;

public class TreeSort extends MouseAdapter implements Runnable,
		ActionListener, ChangeListener, MenuListener, ItemListener {
	
	
	private static class ScrollHeaderLayout extends ScrollPaneLayout {
		@Override
		public void layoutContainer(Container parent) {
			super.layoutContainer(parent);
			JScrollBar bar = getVerticalScrollBar();
			if (bar != null && bar.isVisible()) {
				JViewport header = getColumnHeader();
				if (header != null)
					header.setSize(header.getWidth()+bar.getWidth(), header.getHeight());
			}
		}
	}
	
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
		transformTree.setLargeModel(true);
		transformTree.setRowHeight(20);
		transformTree.setRootVisible(false);
		transformTree.setShowsRootHandles(true);
		transformModel = new TreeModelTransformer(transformTree, model);
		transformTree.setModel(transformModel);
		FlowLayout flow = new FlowLayout(FlowLayout.LEADING, 0, 0);
		flow.setAlignOnBaseline(true);
		Border border = BorderFactory.createEmptyBorder(0, 20, 0, 3);

		JMenuBar leftBar = new JMenuBar();
		leftBar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		
		JMenu menu = createMenu("Sort", KeyEvent.VK_S);
		ButtonGroup group = new ButtonGroup();
		addRadio(menu, "Ascending", false, group, KeyEvent.VK_A);
		addRadio(menu, "Descending", false, group, KeyEvent.VK_D);
		addRadio(menu, "Unsorted", true, group, KeyEvent.VK_U);
		leftBar.add(menu);
		
		menu = createMenu("Filter", KeyEvent.VK_F);
		menu.add(createTextPanel(flow, border, "Regex: ", "Filter"));
		leftBar.add(menu);

		JScrollPane left = createScrollPane(transformTree, leftBar);

		tree = new JTree(model);
		tree.setRowHeight(20);
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setEditable(true);
		tree.setToggleClickCount(-1);
		tree.addMouseListener(this);
		
		JMenuBar rightBar = new JMenuBar();
		rightBar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		modifyMenu = menu = createMenu("Modify", KeyEvent.VK_D);
		menu.add(createTextPanel(flow, border, "Insert: ", "Insert"));
		menu.addSeparator();
		add(menu, "Insert", KeyEvent.VK_I);
		add(menu, "Remove", KeyEvent.VK_R);
		add(menu, "Change", KeyEvent.VK_C);
		add(menu, "NodesChanged on Random Children", KeyEvent.VK_N);
		add(menu, "NodesChanged on All Children", KeyEvent.VK_A);
		add(menu, "Structure Change", KeyEvent.VK_S);
		menu.addSeparator();
		add(menu, "Refresh", KeyEvent.VK_F);
		
		rightBar.add(menu);
		simulationMenu = menu = createMenu("Simulation", KeyEvent.VK_M);
		simulationFilter = new JCheckBoxMenuItem("Can change filter", false);
		menu.add(simulationFilter);
		simulationSort = new JCheckBoxMenuItem("Can change sort order", false);
		menu.add(simulationSort);
		menu.addSeparator();
		JMenuItem item = new JMenuItem("Start");
		item.setActionCommand("Simulation");
		item.addActionListener(this);
		menu.add(item);
		rightBar.add(menu);

		simulationPanel = new JPanel(null);
		simulationPanel.setVisible(false);
		
		JScrollPane rightScroller = createScrollPane(tree, rightBar);
		JPanel right = new JPanel(new BorderLayout());
		right.add(rightScroller, BorderLayout.CENTER);
		right.add(simulationPanel, BorderLayout.SOUTH);
		
		JPanel grid = new JPanel(new GridLayout(1, 2));
		grid.add(left);
		grid.add(right);
		
		JFrame frame = new JFrame(getClass().getSimpleName());
		frame.add(grid, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(600, 700);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);		
	}
	private JMenu createMenu(String text, int mnemonic) {
		JMenu menu = new JMenu(text);
		menu.addMenuListener(this);
		menu.setMnemonic(mnemonic);
		return menu;
	}
	private void addRadio(JMenu menu, String text, boolean sel,
			ButtonGroup group, int mnemonic) {
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(text, sel);
		item.setMnemonic(mnemonic);
		item.setAccelerator(KeyStroke.getKeyStroke(mnemonic,
				InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		group.add(item);
		menu.add(item);
		item.addItemListener(this);
	}
	private void add(JMenu menu, String text, int mnemonic) {
		JMenuItem item = menu.add(text);
		item.setMnemonic(mnemonic);
		int index = text.indexOf(mnemonic);
		if (index >= 0)
			item.setDisplayedMnemonicIndex(index);
		item.setAccelerator(KeyStroke.getKeyStroke(mnemonic,
				InputEvent.CTRL_DOWN_MASK));
		item.addActionListener(this);
	}
	private JPanel createTextPanel(FlowLayout layout, Border border,
			String label, String cmd) {
		JPanel panel = new JPanel(layout);
		panel.setBorder(border);
		panel.setOpaque(false);
		JLabel lbl = new JLabel(label);
		panel.add(lbl);
		JTextField field = new JTextField(15);
		field.setActionCommand(cmd);
		field.addActionListener(this);
		panel.add(field);
		return panel;
	}
	private JScrollPane createScrollPane(JTree tree, JMenuBar header) {
		JScrollPane scroller = new JScrollPane(tree);
		scroller.setColumnHeaderView(header);
		scroller.getColumnHeader().addChangeListener(this);
		scroller.setLayout(new ScrollHeaderLayout());
		return scroller;
	}
	
	private Random random;
	
	private DefaultTreeModel model;
	
	private TreeModelTransformer<?> transformModel;
	
	private JTree tree;
	
	private JTree transformTree;
	
	private JMenu modifyMenu;
	
	private JMenu simulationMenu;
	
	private JCheckBoxMenuItem simulationFilter;
	
	private JCheckBoxMenuItem simulationSort;
	
	private JPanel simulationPanel;
	
	private Simulation simulation;
	
	private int bellCurve() {
		int i = Integer.bitCount(random.nextInt());
		if (i < 13)
			return i;
		return i-12;
	}
	
	private MutableTreeNode addChildren(DefaultMutableTreeNode node, int depth) {
		if (--depth >= 0)
			for (int i=bellCurve(); --i>=0;)
				node.add(createTreeNode(createCellValue(), depth));
		return node;
	}
	
	private MutableTreeNode createTreeNode(String text, int depth) {
		return addChildren(new DefaultMutableTreeNode(text), depth);
	}
	
	private String createCellValue() {
		char[] c = new char[bellCurve()+2];
		for (int i=c.length; --i>=0;)
			c[i] = (char)(random.nextInt(26)+'a');
		c[0] = Character.toUpperCase(c[0]);
		return new String(c);
	}

	private TreePath getPath(int x, int y) {
		TreePath path = tree.getClosestPathForLocation(x, y);
		Rectangle nb = tree.getPathBounds(path);
		if (x >= nb.x && y >= nb.y && y < nb.y+nb.height)
			return path;
		return null;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)
				&& !e.isControlDown() && !e.isShiftDown()) {
			TreePath path = getPath(e.getX(), e.getY());
			if (path != null)
				tree.setSelectionPath(path);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
			TreePath path = getPath(e.getX(), e.getY());
			if (path != null)
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
	public void stateChanged(ChangeEvent e) {
		JViewport viewport = (JViewport)e.getSource();
		viewport.setViewPosition(new Point(0, 0));
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "Filter") {
			JTextField field = (JTextField)e.getSource();
			String regex = field.getText();
			TreeModelTransformer.Filter filter = regex.isEmpty() ? null :
				new TreeModelTransformer.RegexFilter(Pattern.compile(regex), false);
			TreePath startingPath = filter == null ? null : transformTree.getSelectionPath();
			transformModel.setFilter(filter, startingPath);
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
			path = path == null ? new TreePath(parent) : path.getParentPath();
			MutableTreeNode node = createTreeNode(text, getChildDepth(path));
			model.insertNodeInto(node, parent, index);
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
		} else if (e.getActionCommand() == "Structure Chaange") {
			TreePath[] paths = tree.getSelectionPaths();
			if (paths == null)
				paths = new TreePath[] { new TreePath(model.getRoot())};
			for (TreePath path : paths) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
				node.removeAllChildren();
				addChildren(node, getChildDepth(path));
				model.nodeStructureChanged(node);
			}
		} else if (e.getActionCommand() == "Refresh") {
			TreePath path = tree.getLeadSelectionPath();
			if (path == null)
				return;
			boolean exp = tree.isExpanded(path);
			boolean expT = transformTree.isExpanded(path);
			MutableTreeNode node = (MutableTreeNode)path.getLastPathComponent();
			MutableTreeNode parent = (MutableTreeNode)node.getParent();
			int index = parent.getIndex(node);
			model.removeNodeFromParent(node);
			model.insertNodeInto(node, parent, index);
			if (exp)
				tree.expandPath(path);
			if (expT)
				transformTree.expandPath(path);
			tree.addSelectionPath(path);
		} else if (e.getActionCommand() == "Simulation") {
			if (simulation == null)
				simulation = new Simulation();
			simulation.start(simulationFilter.isSelected(), simulationSort.isSelected());
		}
	}
	
	private static int getChildDepth(TreePath path) {
		return 4-path.getPathCount();
	}
	
	private static MutableTreeNode getNode(TreePath path) {
		return (MutableTreeNode)path.getLastPathComponent();
	}
	
	private void changeChildNodes(boolean rdm) {
		TreePath[] paths = tree.getSelectionPaths();
		if (paths == null)
			return;
		for (TreePath path : paths)
			changeChildNodes(getNode(path), rdm);
	}
	
	private int[] changeChildNodes(MutableTreeNode node, boolean rdm) {
		int count = node.getChildCount();
		if (count == 0)
			return null;
		int mutateCount = rdm ? random.nextInt(count)+1 : count;
		int[] indices = new int[mutateCount];
		for (int i=mutateCount, offset=-1; --i>=0;) {
			int length = (count-offset-1)/(i+1);
			int j = random.nextInt(length)+1;
			int idx = offset+j;
			indices[mutateCount-i-1] = idx;
			MutableTreeNode child = (MutableTreeNode)node.getChildAt(idx);
			child.setUserObject(createCellValue());
			offset += j;
		}
		model.nodesChanged(node, indices);
		return indices;
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

	private static class State {
		static final String INSERT = "Insert";
		static final String REMOVE = "Remove";
		static final String CHANGE = "Change";
		static final String NODES_CHANGE = "NodesChange";
		static final String STRUCTURE_CHANGE = "StructureChange";
		static final String FILTER = "Filter";
		static final String SORT_ORDER = "SortOrder";
		
		State(String chg, String pri) {
			this(chg, pri, (String[])null);
		}
		
		State(String chg, String pri, String ... sec) {
			change = chg;
			primary = pri;
			secondary = sec;
		}
		
		String change;
		
		String primary;
		
		String[] secondary;
		
		DefaultMutableTreeNode transformRoot;

		TreePath[] expand;
		
		DefaultMutableTreeNode root;
		
		void setSecondary(String ... sec) {
			secondary = sec;
		}
		
		void buildState(JTree transformTree, TreeModel untransformedModel) {
			TreeModel mdl = transformTree.getModel();
			Object mdlNode = mdl.getRoot();
			transformRoot = new DefaultMutableTreeNode(mdlNode.toString());
			TreePath path = new TreePath(mdlNode);
			ArrayList<TreePath> expanded = Collections.list(transformTree.getExpandedDescendants(path));
			expand = new TreePath[expanded.size()];
			buildState(transformRoot, mdl, path, expanded, 0);
			
			TreeNode modelNode = (TreeNode)untransformedModel.getRoot();
			root = new DefaultMutableTreeNode(modelNode.toString());
			buildNode(root, modelNode);
		}
		
		
		private int buildState(DefaultMutableTreeNode node, TreeModel mdl, TreePath path, ArrayList<TreePath> exp, int index) {
			if (exp.contains(path))
				expand[index++] = buildPath(node);
			Object mdlNode = path.getLastPathComponent();
			int count = mdl.getChildCount(mdlNode);
			for (int i=0; i<count; i++) {
				Object mdlChild = mdl.getChild(mdlNode, i);
				DefaultMutableTreeNode child = new DefaultMutableTreeNode(mdlChild.toString());
				node.add(child);
				index = buildState(child, mdl, path.pathByAddingChild(mdlChild), exp, index);
			}
			return index;
		}
		
		private void buildNode(DefaultMutableTreeNode node, TreeNode mdlNode) {
			int count = mdlNode.getChildCount();
			for (int i=0; i<count; i++) {
				TreeNode mdlChild = mdlNode.getChildAt(i);
				DefaultMutableTreeNode child = new DefaultMutableTreeNode(mdlChild.toString());
				node.add(child);
				buildNode(child, mdlChild);
			}
		}
		
		private TreePath buildPath(TreeNode node) {
			TreeNode parent = node.getParent();
			if (parent == null)
				return new TreePath(node);
			return buildPath(parent).pathByAddingChild(node);
		}
		
		String getSecondaryText() {
			if (secondary == null)
				return null;
			if (secondary.length == 1)
				return secondary[0];
			StringBuilder str = new StringBuilder();
			String[] s = secondary;
			for (int i=0;;) {
				str.append(s[i]);
				if (++i >= s.length)
					break;
				str.append('\n');
			}
			return str.toString();
		}
		
		@Override
		public String toString() {
			StringBuilder str = new StringBuilder(100);
			str.append(change).append(": ").append(primary);
			if (secondary != null) {
				for (String s : secondary) {
					str.append('\n').append('\t').append(s);
				}
			}
			return str.toString();
		}
		
	}
	
	

	

	private static final int STATE_SIZE = 31;
	
	private class Simulation implements ActionListener, ItemListener, ChangeListener {
		
		private JButton createButton(String text) {
			JButton b = small(new JButton(text));
			b.addActionListener(this);
			return b;
		}
		
		private <T extends JComponent> T small(T c) {
			c.putClientProperty("JComponent.sizeVariant", "small");
			c.updateUI();
			return c;
		}
		
		Simulation() {
			simModel = new DefaultTreeModel(new DefaultMutableTreeNode());
			timer = new Timer(100, this);
			timer.setActionCommand("Timer");
			prev = createButton("Prev");
			next = createButton("Next");
			pause = small(new JToggleButton("Pause"));
			pause.addItemListener(this);
			
			JButton close = createButton("Close");
			
			transformed = small(new JCheckBox("Transformed", true));
			transformed.addItemListener(this);
			expand = small(new JCheckBox("Expand", true));
			expand.addItemListener(this);
			
			SpinnerNumberModel intervalModel = new SpinnerNumberModel(100, 10, 9000, 100);
			intervalModel.addChangeListener(this);
			JSpinner interval = small(new JSpinner(intervalModel));
			
			changes = small(new JComboBox());
			changes.setMaximumRowCount(32);
			changes.addItemListener(this);
			changes.setRenderer(new DefaultListCellRenderer() {

				State state;
				
				public Component getListCellRendererComponent(JList list, Object value, int index, boolean sel, boolean foc) {
					state = value instanceof State ? (State)value : null;
					super.getListCellRendererComponent(list, state == null ? value : " ", index, sel, foc);
					return this;
				}
				
				protected void paintComponent(Graphics g) {
					if (state == null) {
						super.paintComponent(g);
						return;
					}
					int width = getWidth();
					int height = getHeight();
					if (isOpaque()) {
						g.setColor(getBackground());
						g.fillRect(0, 0, width, height);
					}
					g.setColor(getForeground());
					g.setFont(getFont());
					FontMetrics fm = g.getFontMetrics();
					int leftWidth = fm.stringWidth(State.STRUCTURE_CHANGE);
					String str = state.change;
					int strWidth = fm.stringWidth(state.change);
					int y =(height-fm.getHeight())/2+fm.getAscent();
					g.drawString(str, leftWidth-strWidth, y);
					g.drawString(": ", leftWidth, y);
					g.drawString(state.primary, leftWidth+fm.stringWidth(": "), y);
				}
			});
			String cell = "WWW";
			changes.setPrototypeDisplayValue(State.STRUCTURE_CHANGE+": "+Arrays.toString(new String[]{"", cell, cell, cell}));
			secondary = small(new JTextArea(3, 15));
			secondary.setEditable(false);
			JScrollPane scroller = small(new JScrollPane(secondary));
			
			JLabel intLabel = small(new JLabel("Interval:"));
			
			GroupLayout layout = new GroupLayout(simulationPanel);
			simulationPanel.setLayout(layout);
			final int prf = GroupLayout.PREFERRED_SIZE;
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.TRAILING)
				.addGroup(layout.createSequentialGroup()
					.addComponent(prev).addComponent(pause).addComponent(next)
					.addGap(10, 10, Short.MAX_VALUE)
					.addComponent(intLabel).addGap(2)
					.addComponent(interval, prf, prf, prf))
				.addComponent(changes)
				.addGroup(layout.createSequentialGroup()
					.addComponent(scroller)
					.addGroup(layout.createParallelGroup()
						.addComponent(transformed)
						.addComponent(expand)
						.addComponent(close, Alignment.CENTER))
					.addGap(5)));
			layout.setVerticalGroup(layout.createSequentialGroup()
				.addGroup(layout.createBaselineGroup(true, true)
					.addComponent(prev).addComponent(pause).addComponent(next)
					.addComponent(intLabel)
					.addComponent(interval, prf, prf, prf))
				.addComponent(changes)
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(scroller)
					.addGroup(layout.createSequentialGroup()
						.addComponent(transformed)
						.addComponent(expand)
						.addComponent(close))));
		}

		private Timer timer;
		
		private JButton prev;
		
		private JToggleButton pause;
		
		private JButton next;
		
		private JComboBox changes;
		
		private JTextArea secondary;
		
		private JCheckBox transformed;
		
		private JCheckBox expand;
		
		
		private ArrayDeque<State> states = new ArrayDeque<State>(STATE_SIZE);
		
		private DefaultTreeModel simModel;
		
		private boolean canChangeFilter;
		
		private boolean canChangeSort;
		
		@Override
		public void stateChanged(ChangeEvent e) {
			SpinnerNumberModel mdl = (SpinnerNumberModel)e.getSource();
			int delay = mdl.getNumber().intValue();
			timer.setDelay(delay);
			timer.setInitialDelay(delay);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand() == "Timer") {
				doSimulation();
			} else if (e.getSource() == prev) {
				changes.setSelectedIndex(changes.getSelectedIndex()-1);
			} else if (e.getSource() == next) {
				changes.setSelectedIndex(changes.getSelectedIndex()+1);
			} else if (e.getActionCommand() == "Close") {
				stop();
			}
		}
		
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getSource() == pause) {
				boolean paused = e.getStateChange() == ItemEvent.SELECTED;
				if (paused) {
					timer.stop();
					State[] array = states.toArray(new State[states.size()]);
					int index = array.length-1;
					changes.setModel(new DefaultComboBoxModel(array));
					boolean enabled = index > 0;
					prev.setEnabled(enabled);
					transformed.setEnabled(enabled);
					changes.setEnabled(enabled);
					expand.setEnabled(enabled);
					if (enabled)
						changes.setSelectedIndex(index);
				} else {
					timer.start();
					clearChangesModel();
					setRunningState();
				}
			} else if (e.getSource() == changes) {
				int idx = changes.getSelectedIndex();
				if (idx < 0)
					return;
				int lastIndex = changes.getModel().getSize() - 1;
				if (idx == 0) {
					prev.setEnabled(false);
					next.setEnabled(lastIndex > 0);
				} else if (idx == lastIndex) {
					prev.setEnabled(lastIndex > 0);
					next.setEnabled(false);
				} else {
					prev.setEnabled(true);
					next.setEnabled(true);
				}
				State state = (State)changes.getSelectedItem();
				resetTree(state);
				secondary.setText(state.getSecondaryText());
			} else if (e.getSource() == transformed) {
				resetTree((State)changes.getSelectedItem());
			} else if (e.getSource() == expand) {
				State state = (State)changes.getSelectedItem();
				if (simModel.getRoot() == state.transformRoot) {
					if (expand.isSelected()) {
						expand(state);
					} else {
						simModel.nodeStructureChanged(state.transformRoot);
					}
				}
					
			}
		}
		
		private void resetTree(State state) {
			if (transformed.isSelected()) {
				simModel.setRoot(state.transformRoot);
				if (expand.isSelected())
					expand(state);
			} else {
				simModel.setRoot(state.root);
			}
		}
		
		private void expand(State state) {
			JTree tre = tree;
			for (TreePath path : state.expand)
				tre.expandPath(path);
		}
		
		private JPopupMenu treePopup;
		
		private void setComponentsState(boolean b) {
			if (b) {
				tree.setComponentPopupMenu(treePopup);
			} else {
				treePopup = tree.getComponentPopupMenu();
				tree.setComponentPopupMenu(null);
			}
			modifyMenu.setEnabled(b);
			simulationMenu.setEnabled(b);
			simulationPanel.setVisible(!b);
		}
		
		private void setRunningState() {
			transformed.setEnabled(false);
			expand.setEnabled(false);
			prev.setEnabled(false);
			next.setEnabled(false);
			changes.setEnabled(false);
		}
		
		void start(boolean canChangeFilter, boolean canChangeSort) {
			this.canChangeFilter = canChangeFilter;
			this.canChangeSort = canChangeSort;
			tree.setModel(simModel);
			setComponentsState(false);
			setRunningState();
			timer.start();
		}
		
		void stop() {
			timer.stop();
			tree.setModel(model);
			setComponentsState(true);
			states.clear();
			clearChangesModel();
			simModel.setRoot(new DefaultMutableTreeNode());
		}
		
		private void clearChangesModel() {
			((DefaultComboBoxModel)changes.getModel()).removeAllElements();
		}
		
		private void doSimulation() {
			State state = null;
			MutableTreeNode node;
			String value;
			int[] indices;
			try {
				int changes = 13;
				if (canChangeFilter || canChangeSort)
					changes++;
				int action = random.nextInt(changes);
				switch (action) {
				case 0: case 1: case 2: // insert
					node = randomNode();
					value = createCellValue();
					state = new State(State.INSERT, string(node), value);
					model.insertNodeInto(createTreeNode(value, 4-pathCount(node)),
							node, random.nextInt(node.getChildCount()+1));
					break;
				case 3: case 4: case 5: // remove
					node = randomNode();
					int count = pathCount(node);
					if (count == 1) {
						return;
					} else if (count == 2) {
						if (node.getParent().getChildCount() == 1)
							return;
					}
					state = new State(State.REMOVE, string(node));
					model.removeNodeFromParent(node);
					break;
				case 6: case 7: case 8: // change
					node = randomNode();
					value = createCellValue();
					state = new State(State.CHANGE, string(node), value);
					node.setUserObject(value);
					model.nodeChanged(node);
					break;
				case 9: case 10: // nodes changed on random
				case 11: // nodes changed on all
					node = randomNode();
					state = new State(State.NODES_CHANGE, string(node));
					indices = changeChildNodes(node, action != 11);
					if (indices == null)
						return;
					state.setSecondary(Arrays.toString(indices));
					break;
				case 12: // structure change
					// TODO
					return;
				case 13: // filter/sort change
					state = !canChangeSort ? changeFilter() :
						!canChangeFilter ? changeSort() :
						random.nextBoolean() ? changeFilter() : changeSort();
					// TODO return cos not actually implemented yet
					return;
				}
				addState(state);
			} catch (Exception e) {
				System.err.println("state: "+state);
				e.printStackTrace();
				pause.setSelected(true);
			}
		}
		
		private void addState(State state) {
			if (states.size() == STATE_SIZE)
				states.pollFirst();
			state.buildState(transformTree, model);
			states.addLast(state);
		}
		
		private State changeFilter() {
			return null;
		}
		
		private State changeSort() {
			return null;
		}
		
		private String string(TreeNode node) {
			return Arrays.toString(simModel.getPathToRoot(node));
		}
		
	}
	

	private MutableTreeNode randomNode() {
		int count = random.nextInt(4);
		MutableTreeNode node = (MutableTreeNode)model.getRoot();
		while (--count >= 0) {
			int i = node.getChildCount();
			if (i == 0)
				break;
			node = (MutableTreeNode)node.getChildAt(random.nextInt(i));
		}
		return node;
	}
	
	private static int pathCount(TreeNode node) {
		int count = 1;
		while (node.getParent() != null) {
			node = node.getParent();
			count++;
		}
		return count;
	}
}
