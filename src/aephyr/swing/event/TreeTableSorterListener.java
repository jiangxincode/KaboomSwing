package aephyr.swing.event;

import java.util.EventListener;

public interface TreeTableSorterListener extends EventListener {
	
	void sorterChanged(TreeTableSorterEvent e);
	
}
