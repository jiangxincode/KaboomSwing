package aephyr.swing.treetable;

import javax.swing.Icon;
import javax.swing.tree.TreePath;

public interface IconRepository {

	Icon getIcon(TreePath path, boolean expanded, boolean leaf);
	
}
