package aephyr.swing.treetable;

import javax.swing.tree.MutableTreeNode;

public interface MutableTreeTableNode extends TreeTableNode, MutableTreeNode {

	void setValueAt(Object value, int column);
	
}
