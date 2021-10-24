package aephyr.swing.workaround;

import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Work around for <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5018574">
 * Bug 5018574: Unable to set focus to another component in JOptionPane</a>.
 * <p>
 * This workaround is adapted from the one given in the Evaluation.
 */
public class JOptionPaneDefaultFocusRequester extends WindowAdapter
		implements HierarchyListener {

	public JOptionPaneDefaultFocusRequester(JComponent c) {
		component = c;
		c.addHierarchyListener(this);
	}

	private JComponent component;

	@Override
	public void hierarchyChanged(HierarchyEvent e) {
		if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
				&& component.isShowing()) {
			SwingUtilities.getWindowAncestor(component)
				.addWindowFocusListener(this);
		}
	}

	@Override
	public void windowGainedFocus(WindowEvent e) {
		component.requestFocusInWindow();
		// only request focus once
		e.getWindow().removeWindowFocusListener(this);
	}
	
}
