package aephyr.swing.treetable;

import javax.swing.Icon;
import javax.swing.tree.TreePath;

import aephyr.swing.TreeTable;

public interface IconRepository {

	Icon getIcon(TreeTable treeTable, TreePath path, boolean expanded, boolean leaf);
	
}
