package aephyr.swing.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.swing.JTree;
import javax.swing.event.ListDataListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;


public class CachingTree extends JTree {

	public interface Node {
		Callable<Node> getLoaderAt(int index);
		boolean isLeaf();
		int getChildCount();
	}
	
	public interface Model {
		Node getRoot();
		void valueForPathChanged(TreePath path, Object newValue);
		void addTreeModelListener(TreeModelListener l);
		void removeTreeModelListener(TreeModelListener l);
	}
	
	public CachingTree(Model model) {
		this(new ModelAdapter(model));
	}
	
	protected CachingTree(ModelAdapter adapter) {
		super(adapter);
	}
	
	
	
	protected static class ModelAdapter extends CachingModel
			implements TreeModel, TreeModelListener {

		public ModelAdapter(Model model) {
			this.model = model;
			model.addTreeModelListener(this);
		}
		
		private Model model;
		
		private TreeModelListener[] listeners;
		
		@Override
		protected void fireUpdate(int index) {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected Object getErrorAt(int index, Exception e) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected Callable<?> getLoaderAt(int index) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object getChild(Object parent, int index) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getChildCount(Object parent) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getIndexOfChild(Object parent, Object child) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object getRoot() {
			return model.getRoot();
		}

		@Override
		public boolean isLeaf(Object node) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void addTreeModelListener(TreeModelListener l) {
			if (listeners == null) {
				listeners = new TreeModelListener[] { l };
			} else {
				int i = listeners.length;
				listeners = Arrays.copyOf(listeners, i+1);
				listeners[i] = l;
			}
		}
		
		@Override
		public void removeTreeModelListener(TreeModelListener l) {
			if (listeners == null)
				return;
			if (listeners.length == 1) {
				if (listeners[0] == l)
					listeners = null;
			} else {
				ArrayList<TreeModelListener> list = new ArrayList<TreeModelListener>(
						Arrays.asList(listeners));
				if (list.remove(l))
					listeners = list.toArray(new TreeModelListener[list.size()]);
			}
		}

		@Override
		public void valueForPathChanged(TreePath path, Object newValue) {
			// TODO Auto-generated method stub
			
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
	
}
