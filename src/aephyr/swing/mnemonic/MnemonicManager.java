package aephyr.swing.mnemonic;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class MnemonicManager implements MnemonicFactory {
	
	public static final String IGNORE_SHOWING = "MnemonicManager.ignoreShowing";
	
	private WeakHashMap<JComponent,Object> mnemonicMap = new WeakHashMap<JComponent,Object>();
	
	private Handler handler = new Handler();
	
	private boolean valid = true;
	
	
	// Support for showing mnemonics only when ALT is pressed.
	// Problematic in that it doesn't hide the mnemonics if a 
	// combination such as ALT press, CTRL press, ALT release occurs
	
	private JComponent altComponent;
	
	private Object altPressKey;
	
	private Object altReleaseKey;
	
	public void setAltComponent(JComponent c) {
		if (altComponent != c) {
			if (altComponent != null) {
				InputMap inputs = altComponent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
				inputs.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ALT, InputEvent.ALT_DOWN_MASK, false));
				inputs.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ALT, 0, true));
				ActionMap actions = altComponent.getActionMap();
				actions.remove(altPressKey);
				actions.remove(altReleaseKey);
			}
			altComponent = c;
			if (c != null) {
				if (altPressKey == null) {
					altPressKey = new Object();
					altReleaseKey = new Object();
				}
				InputMap inputs = c.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
				inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_ALT, InputEvent.ALT_DOWN_MASK, false), altPressKey);
				inputs.put(KeyStroke.getKeyStroke(KeyEvent.VK_ALT, 0, true), altReleaseKey);
			} else {
				altPressKey = null;
				altReleaseKey = null;
			}
			revalidate();
		}
	}
	
	public JComponent getAltComponent() {
		return altComponent;
	}
	
	
//	private boolean showMnemonicsOnAltPress = false;
//	
//	public void setShowMnemonicsOnAltPress(boolean b) {
//		if (b != showMnemonicsOnAltPress) {
//			showMnemonicsOnAltPress = b;
//			revalidate();
//		}
//	}
//	
//	public boolean getShowMnemonicsOnAltPress() {
//		return showMnemonicsOnAltPress;
//	}
	
	@Override
	public Mnemonic createMnemonic(JComponent c) {
		return DefaultMnemonic.createMnemonic(Mnemonic.HIGH_PRIORITY);
	}
	
	@Override
	public IndexedMnemonic createIndexedMnemonic(JComponent c, int index) {
		return DefaultMnemonic.createTabMnemonic(index, Mnemonic.LOW_PRIORITY);
	}
	
	/**
	 * 
	 * @param mnemonic
	 * 
	 * @see #addButtonMnemonic
	 * @see #addLabelMnemonic
	 * @see #addTabMnemonic
	 */
	public void addMnemonic(JComponent c, Mnemonic mnemonic) {
		addMnemonic(c, mnemonic, true, false);
	}
	
	public boolean containsMnemonic(JComponent c) {
		return mnemonicMap.containsKey(c);
	}
	
//	/**
//	 * Use of this method instead of {@link #addMnemonic} will
//	 * cause MnemonicManager to automatically track changes
//	 * to the JLabel's text and revalidate
//	 * 
//	 * @param mnemonic
//	 */
//	public void addLabelMnemonic(JLabel label, Mnemonic mnemonic) {
//		addMnemonic(label, mnemonic, true, false);
//	}
//	
//	/**
//	 * Use of this method instead of {@link #addMnemonic} will
//	 * cause MnemonicManager to automatically track changes
//	 * to the AbstractButton's text and revalidate
//	 * 
//	 * @param mnemonic
//	 */
//	public void addButtonMnemonic(AbstractButton button, Mnemonic mnemonic) {
//		addMnemonic(button, mnemonic, true, false);
//	}
	
//	/**
//	 * Use of this method instead of {@link #addMnemonic} will
//	 * cause MnemonicManager to automatically track changes
//	 * to the title for the specified tab as well as update
//	 * the tabIndex if tabs are added to or removed from
//	 * the JTabbedPane
//	 * 
//	 * @param mnemonic
//	 */
//	public void addTabMnemonic(JTabbedPane tabbedPane, TabMnemonic mnemonic) {
//		addMnemonic(tabbedPane, mnemonic, true, true);
//	}
	
	public void addMenuMnemonics(JMenuBar menuBar) {
		addMenuMnemonics(menuBar, this);
	}
	
	public void addMenuMnemonics(JMenuBar menuBar, MnemonicFactory factory) {
		menuBar.putClientProperty(MnemonicFactory.class, factory);
		addListeners(menuBar, false, false, true);
		for (int menuIndex = 0, menuCount = menuBar.getMenuCount();
				menuIndex < menuCount; menuIndex++) {
			JMenu menu = menuBar.getMenu(menuIndex);
			if (menu != null) {
				addMnemonic(menu, factory.createMnemonic(menu));
			}
		}
	}
	
	
	public void addTabMnemonics(JTabbedPane tabbedPane) {
		addTabMnemonics(tabbedPane, this);
	}
	
	public void addTabMnemonics(JTabbedPane tabbedPane, MnemonicFactory factory) {
		tabbedPane.putClientProperty(MnemonicFactory.class, factory);
		addListeners(tabbedPane, true, true, true);
		for (int tabIndex = 0, tabCount = tabbedPane.getTabCount();
				tabIndex < tabCount; tabIndex++) {
			IndexedMnemonic mnemonic = factory.createIndexedMnemonic(tabbedPane, tabIndex);
			addMnemonic(tabbedPane, mnemonic, false, false);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void addMnemonic(JComponent c, Mnemonic mnemonic,
			boolean propListener, boolean contListener) {
		Object obj = mnemonicMap.get(c);
		if (obj == null) {
			addListeners(c, true, propListener, contListener);
			mnemonicMap.put(c, mnemonic);
		} else {
			List<Mnemonic> mnemonicList;
			if (obj instanceof List<?>) {
				mnemonicList = (List<Mnemonic>)obj;
			} else {
				mnemonicList = new ArrayList<Mnemonic>();
				mnemonicList.add((Mnemonic)obj);
				mnemonicMap.put(c, mnemonicList);
			}
			mnemonicList.add(mnemonic);
		}
		if (include(c))
			revalidate();
	}
	
	private void addListeners(JComponent c, boolean hierListener, boolean propListener, boolean contListener) {
		if (!mnemonicMap.containsKey(c)) {
			mnemonicMap.put(c, null);
			if (hierListener)
				c.addHierarchyListener(handler);
			if (propListener)
				c.addPropertyChangeListener(handler);
			if (contListener)
				c.addContainerListener(handler);
		}
	}
	
	public List<? extends Mnemonic> getMnemonicsFor(JComponent c) {
		return getMnemonicList(mnemonicMap.get(c));
	}
	
	@SuppressWarnings("unchecked")
	private static List<? extends Mnemonic> getMnemonicList(Object obj) {
		if (obj == null)
			return Collections.emptyList();
		if (obj instanceof List<?>)
			return Collections.unmodifiableList((List<Mnemonic>)obj);
		return Collections.singletonList((Mnemonic)obj);
	}
	
	@SuppressWarnings("unchecked")
	public void removeMnemonic(JComponent c, Mnemonic mnemonic) {
		Object obj = mnemonicMap.get(c);
		if (obj == mnemonic) {
			if (c instanceof JTabbedPane &&
					c.getClientProperty(MnemonicFactory.class) instanceof MnemonicFactory) {
				mnemonicMap.put(c, null);
			} else {
				mnemonicMap.remove(c);
				removeListeners(c);
			}
			mnemonic.setMnemonic(c, 0);
		} else if (obj instanceof List<?>) {
			List<Mnemonic> mnemonicList = (List<Mnemonic>)obj;
			if (mnemonicList.remove(mnemonic)) {
				mnemonic.setMnemonic(c, 0);
				if (mnemonicList.size() == 1)
					mnemonicMap.put(c, mnemonicList.get(0));
			}
		}
		if (include(c))
			revalidate();
	}
	
	public void removeMnemonicsFor(JComponent c) {
		removeMnemonicsFor(c, mnemonicMap.remove(c));
	}
	
	private void removeMnemonicsFor(JComponent c, Object value) {
		c.putClientProperty(MnemonicFactory.class, null);
		removeListeners(c);
		for (Mnemonic m : getMnemonicList(value))
			m.setMnemonic(c, 0);
	}
	
	public void removeAllMnemonics() {
		Iterator<Entry<JComponent,Object>> it = mnemonicMap.entrySet().iterator();
		while (it.hasNext()) {
			Entry<JComponent,Object> entry = it.next();
			removeMnemonicsFor(entry.getKey(), entry.getValue());
			it.remove();
		}
	}
	
	private void removeListeners(JComponent c) {
		c.removeHierarchyListener(handler);
		c.removePropertyChangeListener(handler);
		if (c instanceof JTabbedPane)
			c.removeContainerListener(handler);
	}
	
	public void revalidate() {
		if (valid) {
			valid = false;
			SwingUtilities.invokeLater(handler);
		}
	}
	
	private static boolean include(JComponent c) {
		return Boolean.TRUE.equals(c.getClientProperty(
				IGNORE_SHOWING)) || c.isShowing();
	}
	
	/**
	 * Convenience class used for {@link #validate} method.
	 */
	private static class MnemonicEntry implements Comparable<MnemonicEntry> {
		
		MnemonicEntry(JComponent c, Mnemonic m) {
			component = c;
			mnemonic = m;
		}
		
		private final JComponent component;
		
		private final Mnemonic mnemonic;
		
		int index = 0;
		
		int getMnemonic() {
			return mnemonic.getPreferredMnemonic(component, index);
		}
		
		void setMnemonicKey(int mnemonicKey) {
			mnemonic.setMnemonic(component, mnemonicKey);
		}
		
		@Override
		public int compareTo(MnemonicEntry o) {
			return getPriority() - o.getPriority();
		}
		
		int getPriority() {
			return mnemonic.getPriority(component);
		}
		
	}
	
	private ArrayList<MnemonicEntry> setMnemonic(MnemonicEntry entry,
			HashMap<Integer,MnemonicEntry> map,
			ArrayList<MnemonicEntry> unset_entries) {
		int mnemonic = entry.getMnemonic();
		if (mnemonic == 0) {
			if (entry.index > 0) {
				// all possible mnemonics taken by higher priority entries
				if (unset_entries == null) {
					unset_entries = new ArrayList<MnemonicEntry>();
					unset_entries.add(entry);
				} else {
					add(unset_entries, entry, handler);
				}
			}
		} else {
			Integer mnemonicKey = mnemonic;
			MnemonicEntry oldEntry = map.get(mnemonicKey);
			if (oldEntry == null) {
				map.put(mnemonicKey, entry);
			} else {
				int cmp = handler.compare(entry, oldEntry);
				if (cmp < 0) {
					map.put(mnemonicKey, entry);
					entry = oldEntry;
				}
				entry.index++;
				unset_entries = setMnemonic(entry, map, unset_entries);
			}
		}
		return unset_entries;
	}
	
	private void add(ArrayList<MnemonicEntry> entries, MnemonicEntry entry,
			Comparator<MnemonicEntry> cmp) {
		// add entry to entries in the order of highest to least priority
		int idx = Collections.binarySearch(entries, entry, cmp);
		if (idx < 0)
			idx = -idx - 1;
		entries.add(idx, entry);
	}
	
	public void setMenuItemMnemonicsFor(JMenu menu) {
		HashMap<Integer,MnemonicEntry> mnemonic_to_entry =
			new HashMap<Integer,MnemonicEntry>(32);
		ArrayList<MnemonicEntry> unset_entries = null;
		Component[] items = menu.getMenuComponents();
		for (Component c : items) {
			if (c instanceof JMenu)
				setMenuItemMnemonicsFor((JMenu)c);
			if (c instanceof JMenuItem) {
				JMenuItem item = (JMenuItem)c;
				unset_entries = setMnemonic(new MnemonicEntry(
						item, new DefaultMnemonic()),
						mnemonic_to_entry, unset_entries);
			}
		}
		if (unset_entries != null)
			bump(unset_entries, mnemonic_to_entry, true);
		for (Entry<Integer,MnemonicEntry> entry : mnemonic_to_entry.entrySet()) {
			JMenuItem item = (JMenuItem)entry.getValue().component;
			item.setMnemonic(entry.getKey());
		}
	}

	@SuppressWarnings("unchecked")
	private void validate() {
		if (!valid) {
			valid = true;
			HashMap<Integer,MnemonicEntry> mnemonic_to_entry =
					new HashMap<Integer,MnemonicEntry>(32);
			ArrayList<MnemonicEntry> unset_entries = null;
			for (Entry<JComponent,?> entry : mnemonicMap.entrySet()) {
				JComponent c = entry.getKey();
				if (!include(c))
					continue;
				if (entry.getValue() instanceof List<?>) {
					for (Mnemonic mnemonic : (List<Mnemonic>)entry.getValue()) {
						unset_entries = setMnemonic(new MnemonicEntry(
								c, mnemonic), mnemonic_to_entry, unset_entries);
					}
				} else if (entry.getValue() instanceof Mnemonic) {
					unset_entries = setMnemonic(new MnemonicEntry(
							c, (Mnemonic)entry.getValue()),
							mnemonic_to_entry, unset_entries);
				}
			}
			if (unset_entries != null)
				bump(unset_entries, mnemonic_to_entry, false);
			if (altComponent != null) {
				mnemonicsSet = false;
				ActionMap actions = altComponent.getActionMap();
				actions.put(altPressKey, new AltAction(true, mnemonic_to_entry));
				actions.put(altReleaseKey, new AltAction(false, mnemonic_to_entry));
			} else {
				for (Entry<Integer,MnemonicEntry> entry : mnemonic_to_entry.entrySet()) {
					entry.getValue().setMnemonicKey(entry.getKey());
				}
			}
		}
	}
	
	private boolean mnemonicsSet = false;
	
	private class AltAction implements Action {
		
		AltAction(boolean pr, HashMap<Integer,MnemonicEntry> mp) {
			press = pr;
			map = mp;
		}
		
		private boolean press;
		
		private HashMap<Integer,MnemonicEntry> map;
		
		public void actionPerformed(ActionEvent e) {
			if (press) {
				if (!mnemonicsSet) {
					mnemonicsSet = true;
					setMnemonics();
				}
			} else {
				mnemonicsSet = false;
				clearMnemonics();
			}
		}
		
		private void setMnemonics() {
			for (Entry<Integer,MnemonicEntry> entry : map.entrySet()) {
				entry.getValue().setMnemonicKey(entry.getKey());
			}
		}
		
		private void clearMnemonics() {
			for (Entry<Integer,MnemonicEntry> entry : map.entrySet()) {
				entry.getValue().setMnemonicKey(0);
			}
		}

		@Override
		public void addPropertyChangeListener(PropertyChangeListener listener) {}

		@Override
		public Object getValue(String key) {
			return null;
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

		@Override
		public void putValue(String key, Object value) {
		}

		@Override
		public void removePropertyChangeListener(PropertyChangeListener listener) {
		}

		@Override
		public void setEnabled(boolean b) {
		}
	}
	
	
	private void bump(ArrayList<MnemonicEntry> unset_entries,
			HashMap<Integer,MnemonicEntry> mnemonic_to_entry, boolean canDuplicate) {
		// TODO: if canDuplicate, create a map of mnemonic to # of set,
		// when duplication is required, use the first preferred mnemonic with
		// the least duplication
		
		// see if a higher priority entry can be knocked
		// to a different mnemonicKey so that as many
		// mnemonics as possible are set
		next: for (MnemonicEntry uEntry : unset_entries) {
			// list of possible entries to bump 
			ArrayList<MnemonicEntry> entries = new ArrayList<MnemonicEntry>();
			for (uEntry.index = 0; ; uEntry.index++) {
				int mnemonic = uEntry.getMnemonic();
				if (mnemonic == 0)
					break;
				MnemonicEntry e = mnemonic_to_entry.get(mnemonic);
				// shouldn't be null but could be with a bad
				// implementation of the Mnemonic interface
				if (e != null)
					add(entries, e, null);
			}
			// lower priority entries are at the end of the list
			// so start there
			for (int i=entries.size(); --i>=0;) {
				MnemonicEntry mEntry = entries.get(i);
				// test if bumpable
				for (int index = mEntry.index; ;) {
					mEntry.index++;
					int mnemonic = mEntry.getMnemonic();
					if (mnemonic == 0) {
						// reached end, mEntry not bumpable
						mEntry.index = index;
						break;
					} else {
						Integer mnemonicKey = mnemonic;
						if (!mnemonic_to_entry.containsKey(mnemonicKey)) {
							// mEntry will be bumped, find map entry in mnemonic_to_entry
							for (Entry<Integer,MnemonicEntry> entry : mnemonic_to_entry.entrySet()) {
								if (entry.getValue() == mEntry) {
									entry.setValue(uEntry);
									mnemonic_to_entry.put(mnemonicKey, mEntry);
									continue next;
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void shiftTabIndices(int dir, int tabIndex, Object obj) {
		if (obj instanceof IndexedMnemonic) {
			IndexedMnemonic tabMnemonic = (IndexedMnemonic)obj;
			int idx = tabMnemonic.getIndex();
			if (idx >= tabIndex)
				tabMnemonic.setIndex(idx + dir);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void shiftTabIndices(JTabbedPane t, int dir, int tabIndex) {
		Object obj = mnemonicMap.get(t);
		if (obj == null)
			return;
		if (obj instanceof List<?>) {
			List<Object> mnemonicList = (List<Object>)obj;
			for (Object mnemonic : mnemonicList)
				shiftTabIndices(dir, tabIndex, mnemonic);
		} else {
			shiftTabIndices(dir, tabIndex, obj);
		}
	}
	
	@SuppressWarnings("unchecked")
	private IndexedMnemonic getTabMnemonic(JTabbedPane t, int tabIndex) {
		Object obj = mnemonicMap.get(t);
		if (obj instanceof List<?>) {
			for (Object mnemonic : (List<Object>)obj) {
				if (mnemonic instanceof IndexedMnemonic) {
					IndexedMnemonic tabMnemonic = (IndexedMnemonic)mnemonic;
					if (tabMnemonic.getIndex() == tabIndex)
						return tabMnemonic;
				}
			}
		} else if (obj instanceof IndexedMnemonic) {
			IndexedMnemonic tabMnemonic = (IndexedMnemonic)obj;
			if (tabMnemonic.getIndex() == tabIndex)
				return tabMnemonic;
		}
		return null;
	}
	
	private static final String ALT_PRESS = "Alt Press";
	
	private static final String ALT_RELEASE = "Alt Release";
	
	private class Handler implements Runnable, ActionListener, Comparator<MnemonicEntry>,
			ContainerListener, HierarchyListener, PropertyChangeListener {

		public int compare(MnemonicEntry a, MnemonicEntry b) {
			int d = a.getPriority() - b.getPriority();
			if (d != 0)
				return d;
			if (a.component == b.component) {
				Object obj = mnemonicMap.get(a.component);
				if (obj instanceof List<?>) {
					List<?> mnemonicList = (List<?>)obj;
					return mnemonicList.indexOf(a.mnemonic) -
							mnemonicList.indexOf(b.mnemonic);
				}
				return 0;
			}
			Container a_par = a.component.getParent();
			Container b_par = b.component.getParent();
			if (a_par != null && a_par == b_par) {
				return a_par.getComponentZOrder(a.component)
						- b_par.getComponentZOrder(b.component);
			}
			return getParentCount(a_par) - getParentCount(b_par);
		}
		
		@Override
		public void run() {
			validate();
		}

		@Override
		public void hierarchyChanged(HierarchyEvent e) {
			if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
				JComponent c = (JComponent)e.getSource();
				if (!Boolean.TRUE.equals(c.getClientProperty(
						MnemonicManager.IGNORE_SHOWING)))
					revalidate();
			}
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			String name = evt.getPropertyName();
			if (name == MnemonicManager.IGNORE_SHOWING) {
				JComponent c = (JComponent)evt.getSource();
				if (!c.isShowing())
					revalidate();
				return;
			}
			if (evt.getSource() instanceof JTabbedPane) {
				JTabbedPane t = (JTabbedPane)evt.getSource();
				if (name == "__index_to_remove__") {
					if (evt.getNewValue() instanceof Integer) {
						Integer tabIndex = (Integer)evt.getNewValue();
						IndexedMnemonic tabMnemonic = getTabMnemonic(t, tabIndex);
						if (tabMnemonic != null)
							removeMnemonic(t, tabMnemonic);
						shiftTabIndices(t, -1, tabIndex);
					}
				} else if (name == "indexForTitle") {
					if (include(t) && evt.getNewValue() instanceof Integer &&
							getTabMnemonic(t, (Integer)evt.getNewValue()) != null)
						revalidate();
				}
			} else if (name == "text") {
				JComponent c = (JComponent)evt.getSource();
				if (include(c))
					revalidate();
			}
		}

		@Override
		public void componentAdded(ContainerEvent e) {
			if (e.getSource() instanceof JMenuBar) {
				JMenuBar bar = (JMenuBar)e.getSource();
				if (e.getChild() instanceof JMenu) {
					JMenu menu = (JMenu)e.getChild();
					Object obj = bar.getClientProperty(MnemonicFactory.class);
					if (obj instanceof MnemonicFactory)
						addMnemonic(menu, ((MnemonicFactory)obj).createMnemonic(menu));
				}
			} else if (e.getSource() instanceof JTabbedPane) {
				JTabbedPane t = (JTabbedPane)e.getSource();
				int idx = t.indexOfComponent(e.getChild());
				shiftTabIndices(t, 1, idx);
				Object obj = t.getClientProperty(MnemonicFactory.class);
				if (obj instanceof MnemonicFactory)
					addMnemonic(t, ((MnemonicFactory)obj).createIndexedMnemonic(t, idx));
			}
		}

		@Override
		public void componentRemoved(ContainerEvent e) {
			if (e.getSource() instanceof JMenuBar) {
				if (e.getChild() instanceof JMenu)
					removeMnemonicsFor((JMenu)e.getChild());
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			if (cmd == ALT_PRESS) {
				
			} else if (cmd == ALT_RELEASE) {
				
			}
		}

	}
	
	private static int getParentCount(Container c) {
		int i = 0;
		while (c != null) {
			i++;
			c = c.getParent();
		}
		return i;
	}
	
	
	

}
