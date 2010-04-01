package aephyr.swing;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class TreeMenuBar extends CurlMenuBar {
	
	public interface Model extends TreeModel {
		void setRoot(Object root);
	}
	
	public TreeMenuBar() {
		menus = new ArrayList<JMenu>();
		handler = createHandler();
		JMenu root = createRootMenu();
		menus.add(root);
		add(root);
	}
	
	private List<JMenu> menus;
	
	private Model model;
	
	private JTree tree;
	
	private JScrollPane treeScroller;
	
	private TreePath path;
	
	private TreePath menuRoot;
	
	private TreePath defaultSelection;
	
	private String defaultRootText = " ";
	
	private Handler handler;
	
	private boolean rootVisible = false;
	
	protected JMenu createMenu() {
		JMenu menu = new ArrowMenu();
		menu.setFont(getFont());
		return menu;
	}
	
	protected JMenu createRootMenu() {
		JMenu menu = createMenu();
		menu.setEnabled(false);
		menu.setText(getDefaultRootText());
		return menu;
	}
	
	protected Handler createHandler() {
		return new Handler();
	}
	
	public void addTreeSelectionListener(TreeSelectionListener l) {
		listenerList.add(TreeSelectionListener.class, l);
	}
	
	public void removeTreeSelectionListener(TreeSelectionListener l) {
		listenerList.remove(TreeSelectionListener.class, l);
	}
	
	public void setDefaultRootText(String text) {
		defaultRootText = text;
	}
	
	public String getDefaultRootText() {
		return defaultRootText;
	}
	
	public void setRootVisible(boolean visible) {
		rootVisible = visible;
	}
	
	public boolean isRootVisible() {
		return rootVisible;
	}
	
	public void setModel(Model model) {
		Model oldModel = this.model;
		if (oldModel != null)
			oldModel.removeTreeModelListener(handler);
		this.model = model;
		if (model != null) {
			model.addTreeModelListener(handler);
		} else if (tree != null) {
			tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
		}
	}
	
	public Model getModel() {
		return model;
	}
	
	public JTree getTree() {
		if (tree == null)
			tree = createTree(null);
		return tree;
	}
	
	private boolean isLeaf(Object node) {
		if (model != null)
			return model.isLeaf(node);
		return ((TreeNode)node).isLeaf();
	}
	
	public void setPath(TreePath path) {
		this.path = path;
		int menusSize = menus.size();
		if (!isLeaf(path.getLastPathComponent()))
			path = path.pathByAddingChild(" ");
		int pathCount = path == null ? 1 : path.getPathCount();
		int len = Math.min(menusSize, pathCount);
		JMenu rootMenu = menus.get(0);
		if (path == null || (pathCount == 1 && !isRootVisible())) {
			rootMenu.setVisible(true);
			rootMenu.setText(getDefaultRootText());
		} else {
			rootMenu.setVisible(isRootVisible());
			rootMenu.setText(path.getPathComponent(0).toString());
		}
		for (int i=1; i<len; i++) {
			JMenu menu = menus.get(i);
			menu.setText(path.getPathComponent(i).toString());
		}
		if (len < menusSize) {
			List<JMenu> remove = menus.subList(len, menusSize);
			for (JMenu menu : remove) {
				remove(menu);
				menu.removeMenuListener(handler);
			}
			remove.clear();
		} else if (pathCount > menusSize) {
			for (int i=menusSize; i<pathCount; i++) {
				JMenu menu = createMenu();
				menu.setText(path.getPathComponent(i).toString());
				menu.addMenuListener(handler);
				menus.add(menu);
				add(menu);
			}
		}
	}
	
	public TreePath getPath() {
		return path;
	}
	

	private void prepareTree(Object root) {
		if (tree == null) {
			tree = createTree(root);
			tree.addMouseListener(handler);
			tree.addMouseMotionListener(handler);
		} else {
			if (model == null) {
				DefaultTreeModel mdl = (DefaultTreeModel)tree.getModel();
				mdl.setRoot((TreeNode)root);
			} else {
				model.setRoot(root);
			}
		}
	}
	
	private JTree createTree(Object root) {
		TreeModel mdl;
		if (model == null) {
			if (root == null)
				root = new DefaultMutableTreeNode();
			mdl = new DefaultTreeModel((TreeNode)root);
		} else {
			if (root != null)
				model.setRoot(root);
			mdl = model;
		}
		JTree tree = new JTree(mdl);
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		return tree;
	}

	
	
	protected void prepareMenu(JMenu menu) {
		int idx = menus.indexOf(menu);
		if (idx < 0)
			return;
		int count = path.getPathCount();
		TreePath p = path;
		Object node = null;
		if (idx != count) {
			for (;;) {
				if (--count == idx) {
					node = p.getLastPathComponent();
					p = p.getParentPath();
					break;
				}
				p = p.getParentPath();
			}
		}
		menuRoot = p;
		prepareTree(p.getLastPathComponent());
		if (treeScroller == null) {
			treeScroller = new JScrollPane(tree);
			treeScroller.getVerticalScrollBar().addAdjustmentListener(handler);
		}
		treeScroller.setPreferredSize(null);
		Dimension size = treeScroller.getPreferredSize();
		size.width = Math.max(size.width, 200);
		treeScroller.setPreferredSize(size);
		menu.add(treeScroller);
		defaultSelection = node == null ? null : new TreePath(
				new Object[]{p.getLastPathComponent(), node});
		tree.setSelectionPath(defaultSelection);
		if (node != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					Rectangle cell = tree.getPathBounds(defaultSelection);
					Rectangle bounds = tree.getVisibleRect();
					bounds.y = cell.y - (bounds.height-cell.height)/2;
					if (bounds.y < 0)
						bounds.y = 0;
					if (bounds.y+bounds.height > tree.getHeight())
						bounds.height = tree.getHeight()-bounds.y;
					tree.scrollRectToVisible(bounds);
				}
			});
		}
	}
	
	protected void menuPathSelected(TreePath path) {
		path = append(menuRoot, path);
		TreePath oldPath = this.path;
		setPath(path);
		fireSelectionPathChanged(oldPath, path);
		MenuSelectionManager.defaultManager().clearSelectedPath();
	}
	
	protected void fireSelectionPathChanged(TreePath oldPath, TreePath newPath) {
		Object[] listeners = listenerList.getListenerList();
		TreeSelectionEvent e = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==TreeSelectionListener.class) {
				if (e == null)
					e = new TreeSelectionEvent(this, newPath, true, oldPath, newPath);
				((TreeSelectionListener)listeners[i+1]).valueChanged(e);
			}
		}
	}
	
	protected class Handler extends MouseAdapter implements MenuListener, AdjustmentListener, TreeModelListener {

		@Override
		public void menuCanceled(MenuEvent e) {
			menuHidden();
		}

		@Override
		public void menuDeselected(MenuEvent e) {
			menuHidden();
		}
		
		private void menuHidden() {
			menuRoot = null;
			defaultSelection = null;
		}

		@Override
		public void menuSelected(MenuEvent e) {
			prepareMenu((JMenu)e.getSource());
		}
		

		
		protected TreePath getPath(int x, int y) {
			TreePath path = tree.getClosestPathForLocation(x, y);
			Rectangle nb = tree.getPathBounds(path);
			if (tree.getComponentOrientation().isLeftToRight()) {
				if (nb.x <= x && nb.y <= y && y < nb.y+nb.height)
					return path;
			} else {
				if (x < nb.x+nb.width && nb.y <= y && y < nb.y+nb.height)
					return path;
			}
			return null;
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
				TreePath path = getPath(e.getX(), e.getY());
				if (path != null) {
					menuPathSelected(path);
				}
			}
		}
		
		@Override
		public void mouseExited(MouseEvent e) {
			tree.setSelectionPath(defaultSelection);
		}
		
		@Override
		public void mouseMoved(MouseEvent e) {
			tree.setSelectionPath(getPath(e.getX(), e.getY()));
		}
		
		@Override
		public void adjustmentValueChanged(AdjustmentEvent e) {
			Point pos = tree.getMousePosition();
			if (pos != null)
				tree.setSelectionPath(getPath(pos.x, pos.y));
			
		}

		@Override
		public void treeNodesChanged(TreeModelEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void treeNodesInserted(TreeModelEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void treeNodesRemoved(TreeModelEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void treeStructureChanged(TreeModelEvent e) {
			// TODO Auto-generated method stub
			
		}
	}
	

	private static TreePath append(TreePath parent, TreePath child) {
		Object node = child.getLastPathComponent();
		if (node.equals(parent.getLastPathComponent())) {
			return parent;
		} else {
			return append(parent, child.getParentPath()).pathByAddingChild(node);
		}
	}
	
	
	public static class ArrowMenu extends JMenu {
		
		private static final GeneralPath arrow;

		static {
			arrow = new GeneralPath();
			arrow.moveTo(-2, -3.5);
			arrow.lineTo(-2, 3.5);
			arrow.lineTo(2, 0);
			arrow.closePath();
		}
		
		public Insets getInsets() {
			Insets in = super.getInsets();
			in.left += 10;
			return in;
		}
		
		public Insets getInsets(Insets insets) {
			insets = super.getInsets(insets);
			insets.left += 10;
			return insets;
		}
		
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (g instanceof Graphics2D) {
				Insets in = getInsets();
				Graphics2D g2 = (Graphics2D)g;
				int x = in.left-5;
				int y = getHeight()/2;
				g2.translate(x, y);
				g2.setColor(getForeground());
				Object antialiasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
				if (antialiasing != RenderingHints.VALUE_ANTIALIAS_ON)
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Object stroke = g2.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
				if (stroke != RenderingHints.VALUE_STROKE_PURE)
					g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
				g2.fill(arrow);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);
				g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, stroke);
				g2.translate(-x, -y);
			}
		}

	}
}
