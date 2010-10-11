package aephyr.swing.event;

import java.util.EventListener;

import aephyr.swing.event.RowModelEvent;

public interface RowModelListener extends EventListener {
	
	void rowChanged(RowModelEvent e);

}
