package aephyr.swing.event;

import java.util.EventListener;

import aephyr.swing.event.TreeColumnModelEvent;

public interface TreeColumnModelListener extends EventListener {
	
	void treeColumnChanged(TreeColumnModelEvent e);

}
