package aephyr.swing.event;

import java.util.EventListener;

public interface TreeTableMouseMotionListener extends EventListener {

	public void mouseMoved(TreeTableMouseEvent e);
	
	public void mouseDragged(TreeTableMouseEvent e);
	
}
