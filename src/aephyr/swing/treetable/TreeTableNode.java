package aephyr.swing.treetable;

import javax.swing.tree.TreeNode;

public interface TreeTableNode extends TreeNode {
	
	Object getValueAt(int column);
	
	int getColumnCount();

}
