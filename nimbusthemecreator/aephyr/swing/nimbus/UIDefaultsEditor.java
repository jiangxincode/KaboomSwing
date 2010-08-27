package aephyr.swing.nimbus;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.GroupLayout.Alignment;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.TableCellEditor;


class UIDefaultsEditor extends AbstractCellEditor implements Runnable, 
		TableCellEditor, ActionListener {
	
	private static final String OK = "OK";
	
	private static final String CANCEL = "Cancel";
	
	public UIDefaultsEditor() {
		renderer = new UIDefaultsRenderer();
		popup = new JPopupMenu();
		popup.setLayout(new BorderLayout());
		ok = new JButton(OK);
		ok.addActionListener(this);
		JButton cancel = new JButton(CANCEL);
		cancel.addActionListener(this);
		JPanel buttons = new JPanel(null);
		GroupLayout layout = new GroupLayout(buttons);
		buttons.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER, true)
				.addGroup(layout.createSequentialGroup()
						.addGap(8).addComponent(ok).addGap(5).addComponent(cancel).addGap(8))
				.addGap(100, 100, Short.MAX_VALUE));
		layout.setVerticalGroup(layout.createBaselineGroup(false, true)
				.addComponent(ok).addComponent(cancel));
		layout.linkSize(SwingUtilities.HORIZONTAL, ok, cancel);
		popup.add(buttons, BorderLayout.SOUTH);
	}
	
	private UIDefaultsRenderer renderer;
	
	private JPopupMenu popup;
	
	private ValueChooser currentChooser;
	
	private JButton ok;
	
	@Override
	public Object getCellEditorValue() {
		return renderer.value;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table,
			Object value, boolean isSelected, int row, int column) {
		SwingUtilities.invokeLater(this);
		return renderer.getTableCellRendererComponent(table, value, true, false, row, column);
	}
	
	@Override
	public boolean isCellEditable(EventObject e) {
		if (e instanceof MouseEvent) {
			MouseEvent me = (MouseEvent)e;
			return (me.getModifiersEx() & (
					InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK |
					InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)) == 0;
		}
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == OK) {
			Object value = currentChooser.getValue();
			if (!value.equals(renderer.value))
				renderer.value = value;
			currentChooser = null;
			popup.setVisible(false);
			fireEditingStopped();
		} else if (e.getActionCommand() == CANCEL) {
			currentChooser = null;
			popup.setVisible(false);
			fireEditingCanceled();
		}
	}
	
	@Override
	public void run() {
		if (renderer.isShowing())
			showPopup();
	}

	private void showPopup() {
		currentChooser = renderer.type.getValueChooser();
		if (currentChooser == null)
			return;
		currentChooser.setValue(renderer.value);
		BorderLayout layout = (BorderLayout)popup.getLayout();
		Component cur = layout.getLayoutComponent(BorderLayout.CENTER);
		if (cur != currentChooser.getComponent()) {
			if (cur != null)
				popup.remove(cur);
			popup.add(currentChooser.getComponent(), BorderLayout.CENTER);
		}
		popup.show(renderer, renderer.getWidth(), 0);
		ok.getRootPane().setDefaultButton(ok);
		Component foc;
		synchronized (currentChooser.getComponent().getTreeLock()) {
			foc = getFocusableComponent(currentChooser.getComponent());
		}
		if (foc == null)
			foc = ok;
		foc.requestFocus();
	}
	
	private static Component getFocusableComponent(Container p) {
		for (int i=0, j=p.getComponentCount(); i<j; i++) {
			Component c = p.getComponent(i);
			if (c.isFocusable()) {
				return c;
			} else if (c instanceof Container) {
				c = getFocusableComponent((Container)c);
				if (c != null)
					return c;
			}
		}
		return null;
	}

}