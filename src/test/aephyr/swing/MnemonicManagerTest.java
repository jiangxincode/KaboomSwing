package test.aephyr.swing;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

import javax.swing.*;

import aephyr.swing.mnemonic.DefaultMnemonic;
import aephyr.swing.mnemonic.Mnemonic;
import aephyr.swing.mnemonic.MnemonicGenerator;
import aephyr.swing.mnemonic.MnemonicManager;

class TabbedPaneFolly implements Runnable,
		ContainerListener, PropertyChangeListener {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new TabbedPaneFolly());
	}
	public void run() {
		JTabbedPane tabs = new JTabbedPane();
		tabs.addPropertyChangeListener(this);
		tabs.addContainerListener(this);
		tabs.addTab("Tab A", new JPanel());
		tabs.addTab("Tab B", new JPanel());
		tabs.addTab("Tab C", new JPanel());
		tabs.addTab("Tab D", new JPanel());
		tabs.removeTabAt(1);
	}
	
	@Override
	public void componentAdded(ContainerEvent e) {
		JTabbedPane tabs = (JTabbedPane)e.getSource();
		int idx = tabs.indexOfComponent(e.getChild());
		System.out.println("added " + idx);
	}
	@Override
	public void componentRemoved(ContainerEvent e) {
		JTabbedPane tabs = (JTabbedPane)e.getSource();
		int idx = tabs.indexOfComponent(e.getChild());
		System.out.println("removed " + idx);
	}
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		System.out.println(evt.getPropertyName() + " " + evt.getNewValue());
	}
}


public class MnemonicManagerTest implements Runnable, ActionListener {
	
	public static void main(String[] args) throws Exception {
//		TabbedPaneFolly.main(args);
//		if (true)
//			return;
//		Utilities.setNimbusLookAndFeel();
		SwingUtilities.invokeLater(new MnemonicManagerTest());
	}
	
	@Override
	public void run() {
//		manager = new MnemonicManager();
		JMenuBar bar = new JMenuBar();
		bar.add(createMenu("File"));
		bar.add(createMenu("Edit"));
		bar.add(createMenu("Source"));
		bar.add(createMenu("Refactor"));
		bar.add(createMenu("Navigate"));
		
		JPanel buttons = new JPanel();
		buttons.add(createButton("Undo"));
		buttons.add(createButton("Redo"));
		buttons.add(createButton("Cut"));
		buttons.add(createButton("Copy"));
		buttons.add(createButton("Paste"));
		buttons.add(createButton("Delete"));
		
		JTabbedPane tabs = new JTabbedPane();
		addTab(tabs, "Cachable");
		addTab(tabs, "CachingTable");
		addTab(tabs, "DeferLoading");
		addTab(tabs, "TreeMenuBar");
		
		JButton options = createButton("Options");
		options.addActionListener(this);
		
		JFrame frame = new JFrame(getClass().getSimpleName());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setJMenuBar(bar);
		frame.add(buttons, BorderLayout.NORTH);
		frame.add(tabs, BorderLayout.CENTER);
		frame.add(options, BorderLayout.SOUTH);
		new MnemonicGenerator().addMnemonics(frame.getRootPane());
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	private MnemonicManager manager;
	
	private JMenu createMenu(String txt) {
		JMenu menu = new JMenu(txt);
		menu.add("Item");
		if (manager != null)
			manager.addMnemonic(menu,
					DefaultMnemonic.createMnemonic(Mnemonic.TOP_PRIORITY));
		return menu;
	}
	
	private JButton createButton(String txt) {
		JButton button = new JButton(txt);
		if (manager != null)
			manager.addMnemonic(button,
					DefaultMnemonic.createMnemonic(Mnemonic.HIGH_PRIORITY));
		return button;
	}
	
	private void addTab(JTabbedPane tabs, String txt) {
		int tabIndex = tabs.getTabCount();
		JPanel c = new JPanel();
		tabs.addTab(txt, c);
		if (manager != null)
			manager.addMnemonic(tabs,
					DefaultMnemonic.createTabMnemonic(tabIndex, Mnemonic.LOW_PRIORITY));
	}
	
	public void actionPerformed(ActionEvent e) {
		JLabel message = new JLabel("This is a question?");
		Object[] options = {"One","Two","Three","Four","Five","Six","Seven","Eight","Nine","Ten"};
		MnemonicGenerator.registerDialogMnemonicGenerator(message);
		JOptionPane.showOptionDialog(null, message, "Title",
				JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
				null, options, options[0]);
	}
}
