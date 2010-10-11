package aephyr.swing.mnemonic;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;

public class MnemonicGenerator implements MnemonicFactory {
	
	public static final String PRIORITY = "MnemonicGenerator.priority";
	
	public MnemonicGenerator() {
		this(-1, true);
	}
	
	public MnemonicGenerator(
			int extendedModifiers, boolean upperCaseBiased) {
		this (new MnemonicManager(), extendedModifiers, upperCaseBiased);
	}
	
	public MnemonicGenerator(MnemonicManager manager,
			int extendedModifiers, boolean upperCaseBiased) {
		if (extendedModifiers != -1 && (extendedModifiers &
				~(InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK)) != 0)
			throw new IllegalArgumentException();
		this.manager = manager;
		this.upperCaseBiased = upperCaseBiased;
		this.extendedModifiers = extendedModifiers;
	}
	
	private final MnemonicManager manager;
	
	private final int extendedModifiers;
	
	private final boolean upperCaseBiased;
	
	public final MnemonicManager getMnemonicManager() {
		return manager;
	}
	
	public final int getExtendedModifiers() {
		return extendedModifiers;
	}
	
	public final boolean getUpperCaseBiased() {
		return upperCaseBiased;
	}
	
	public void addMnemonics(Container p) {
		for (Component c : p.getComponents()) {
			if (c instanceof JMenuBar) {
				addMenuMnemonics((JMenuBar)c);
			} else if (c instanceof AbstractButton) {
				addMnemonic((AbstractButton)c);
			} else if (c instanceof JLabel) {
				JLabel lab = (JLabel)c;
				if (lab.getLabelFor() != null)
					addMnemonic(lab);
			} else if (c instanceof JTabbedPane) {
				addTabMnemonics((JTabbedPane)c);
			}
			if (c instanceof Container) {
				addMnemonics((Container)c);
			}
		}
	}
	
	protected void addMnemonic(AbstractButton button) {
		if (!manager.containsMnemonic(button))
			manager.addMnemonic(button, createMnemonic(button));
	}
	
	protected void addMnemonic(JLabel label) {
		if (!manager.containsMnemonic(label))
			manager.addMnemonic(label, createMnemonic(label));
	}
	
	protected void addMenuMnemonics(JMenuBar menuBar) {
		if (!manager.containsMnemonic(menuBar))
			manager.addMenuMnemonics(menuBar, this);
	}
	
	protected void addTabMnemonics(JTabbedPane tabbedPane) {
		if (!manager.containsMnemonic(tabbedPane))
			manager.addTabMnemonics(tabbedPane, this);
	}
	
	private int getPriority(JComponent c) {
		Object priority = c.getClientProperty(MnemonicGenerator.PRIORITY);
		return priority instanceof Integer ? 
				(Integer)priority : Mnemonic.HIGH_PRIORITY;
	}

	@Override
	public IndexedMnemonic createIndexedMnemonic(JComponent c, int index) {
		return new DefaultMnemonic(extendedModifiers,
				index, getPriority(c), upperCaseBiased);
	}

	@Override
	public Mnemonic createMnemonic(JComponent c) {
		return new DefaultMnemonic(extendedModifiers,
				-1, getPriority(c), upperCaseBiased);
	}
	
	public static void registerDialogMnemonicGenerator(JComponent c) {
		registerDialogMnemonicGenerator(c, new MnemonicGenerator(0, true));
	}
	
	public static void registerDialogMnemonicGenerator(
			JComponent c, final MnemonicGenerator generator) {
		c.addHierarchyListener(new HierarchyListener() {
			public void hierarchyChanged(HierarchyEvent e) {
				if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 &&
						e.getComponent().isShowing()) {
					Component c = e.getComponent();
					for (;;) {
						Container p = c.getParent();
						if (p == null || p instanceof Window)
							break;
						c = p;
					}
					if (c instanceof Container)
						generator.addMnemonics((Container)c);
					c.removeHierarchyListener(this);
				}
			}
		});
	}
}
