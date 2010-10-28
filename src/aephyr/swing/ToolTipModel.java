package aephyr.swing;

import java.awt.event.MouseEvent;

import javax.swing.JComponent;

public interface ToolTipModel<C extends JComponent> {
	
	String getToolTipText(C c, MouseEvent e);
	
}
