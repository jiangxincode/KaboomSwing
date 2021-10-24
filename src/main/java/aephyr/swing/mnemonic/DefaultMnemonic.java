/*
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package aephyr.swing.mnemonic;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

public class DefaultMnemonic implements IndexedMnemonic, Action {
	
	public static DefaultMnemonic createMnemonic(int priority) {
		return new DefaultMnemonic(-1, -1, priority, true);
	}
	
	public static DefaultMnemonic createTabMnemonic(int index, int priority) {
		return new DefaultMnemonic(-1, index, priority, true);
	}
	
	public DefaultMnemonic() {
		this(-1);
	}
	
	public DefaultMnemonic(int extendedModifiers) {
		this(extendedModifiers, -1);
	}
	
	public DefaultMnemonic(int extendedModifiers, int index) {
		this(extendedModifiers, index, LOW_PRIORITY, true);
	}
	
	public DefaultMnemonic(int extendedModifiers, int index,
			int priority, boolean upperCaseBiased) {
		this.extendedModifiers = extendedModifiers;
		setIndex(index);
		setPriority(priority);
		setUpperCaseBiased(upperCaseBiased);
	}
	
	private String text;
	
	private final int extendedModifiers;
	
	private int priority;
	
	private boolean upperCaseBiased;
	
	private int index;
	
	private int mnemonic;

	/**
	 * @return true if upper case letters are preferred over
	 * 	other characters.
	 * 
	 * @see #setUpperCaseBiased(boolean)
	 */
	public boolean isUpperCaseBiased() {
		return upperCaseBiased;
	}
	
	/**
	 * If true, upper case letters in the String returned by
	 * {@link #getMnemonicText(JComponent)} will be preferred
	 * over any other characters.
	 * <p>
	 * Upper case letters will also be marked as the displayed
	 * mnemonic index over lower case letters even if the lower
	 * case version of the letter appears before the upper case
	 * letter.
	 * 
	 * @param upperCaseBiased
	 */
	public void setUpperCaseBiased(boolean upperCaseBiased) {
		this.upperCaseBiased = upperCaseBiased;
	}
	
	public final int getExtendedModifiers() {
		return extendedModifiers;
	}
	
	public int getIndex() {
		return index;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	@Override
	public int getPreferredMnemonic(JComponent c, int index) {
		return getNextMnemonic(getMnemonicText(c),
				index, isUpperCaseBiased());
	}
	
	@Override
	public int getPriority(JComponent c) {
		return priority;
	}
	
	public void setPriority(int priority) {
		this.priority = priority;
	}

	public void setMnemonicText(String text) {
		this.text = text;
	}
	
	public String getMnemonicText(JComponent c) {
		if (text != null)
			return text;
		if (getIndex() < 0) {
			if (c instanceof AbstractButton) {
				return ((AbstractButton)c).getText();
			} else if (c instanceof JLabel) {
				return ((JLabel)c).getText();
			}
		} else if (c instanceof JTabbedPane) {
			return ((JTabbedPane)c).getTitleAt(getIndex());
		}
		return null;
	}
	
	public void setDisplayedMnemonicIndex(JComponent c, int index) {
		if (getIndex() < 0) {
			if (c instanceof AbstractButton) {
				((AbstractButton)c).setDisplayedMnemonicIndex(index);
			} else if (c instanceof JLabel) {
				((JLabel)c).setDisplayedMnemonicIndex(index);
			}
		} else if (c instanceof JTabbedPane) {
			((JTabbedPane)c).setDisplayedMnemonicIndexAt(getIndex(), index);
		}
	}

	private void setMnemonic0(JComponent c, int mnemonic) {
		if (getIndex() < 0) {
			if (c instanceof AbstractButton) {
				((AbstractButton)c).setMnemonic(mnemonic);
				return;
			} else if (c instanceof JLabel) {
				((JLabel)c).setDisplayedMnemonic(mnemonic);
				return;
			}
		} else if (c instanceof JTabbedPane) {
			((JTabbedPane)c).setMnemonicAt(getIndex(), mnemonic);
			return;
		}
		setMnemonicAction(
				c, mnemonic, getMnemonicModifier());

	}
	
	@Override
	public void setMnemonic(JComponent c, int mnemonic) {
		if (mnemonic != this.mnemonic) {
			setMnemonic0(c, mnemonic);
			int mod = extendedModifiers;
			if (mod >= 0) {
				if (mod != 0)
					mod |= getMnemonicModifier();
				setMnemonicAction(c, mnemonic, mod);
			}
			this.mnemonic = mnemonic;
		}
		updateDisplayedMnemonicIndex(c, mnemonic);
	}
	
	protected void setMnemonicAction(
			JComponent c, int mnemonic, int modifiers) {
		InputMap inputs = c.getInputMap(
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actions = c.getActionMap();
		if (this.mnemonic != 0) {
			inputs.remove(KeyStroke.getKeyStroke(
					this.mnemonic, modifiers));
		}
		if (mnemonic != 0) {
			inputs.put(KeyStroke.getKeyStroke(
					mnemonic, modifiers), this);
			actions.put(this, this);
		} else {
			actions.remove(this);
		}
	}
	
	protected void updateDisplayedMnemonicIndex(
			JComponent c, int mnemonic) {
		if (mnemonic != 0 && isUpperCaseBiased()) {
			String text = getMnemonicText(c);
			int idx = text.indexOf(mnemonic);
			if (idx >= 0)
				setDisplayedMnemonicIndex(c, idx);
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (getIndex() < 0) {
			if (src instanceof AbstractButton) {
				AbstractButton b = (AbstractButton)src;
				b.doClick();
				return;
			} else if (src instanceof JLabel) {
				JLabel label = (JLabel)src;
				Component c = label.getLabelFor();
				if (c != null && c.isFocusable()) {
					c.requestFocusInWindow();
					return;
				}
			}
		} else if (src instanceof JTabbedPane) {
			((JTabbedPane)src).setSelectedIndex(getIndex());
			return;
		}
		Component c = (Component)src;
		if (c.isFocusable())
			c.requestFocusInWindow();
	}

	@Override
	public void addPropertyChangeListener(
			PropertyChangeListener listener) {}

	@Override
	public void removePropertyChangeListener(
			PropertyChangeListener listener) {}
	
	@Override
	public Object getValue(String key) {
		return null;
	}

	@Override
	public void putValue(String key, Object value) {}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public void setEnabled(boolean b) {}

	
	
	
	private static int getNextMnemonic(String text,
			int index, boolean upperCaseBiased) {
		if (text == null)
			return 0;
		int count = -1;
		if (upperCaseBiased) {
			for (int i=0, j=text.length(); i<j; i++) {
				char c = text.charAt(i);
				if (c >= 'A' && c <= 'Z' && valid(text, i, c)
						&& ++count == index)
					return (int)c;
			}
		}
		for (int i=0, j=text.length(); i<j; i++) {
			char c = text.charAt(i);
			char uc = Character.toUpperCase(c);
			if (uc >= 'B' && uc <= 'Z') {
				c = Character.toLowerCase(c);
				if (valid(text, i, c, uc, false, upperCaseBiased)
						&& ++count == index)
					return (int)uc;
			}
		}
		for (int i=0, j=text.length(); i<j; i++) {
			char c = text.charAt(i);
			char uc = Character.toUpperCase(c);
			if (uc >= 'A' && uc <= 'U') {
				c = Character.toLowerCase(c);
				if (valid(text, i, c, uc, true, upperCaseBiased)
						&& ++count == index)
					return (int)uc;
			}
		}
		for (int i=0, j=text.length(); i<j; i++) {
			char c = text.charAt(i);
			if (c >= '0' && c <= '9' && valid(text, i, c)
					&& ++count == index)
				return (int)c;
		}
		return 0;
	}
	private static boolean isVowel(char c) {
		switch (c) {
		case 'A': case 'E': case 'I': case 'O': case 'U':
			return true;
		}
		return false;
	}
	private static boolean valid(String text, int i, char c,
			char uc, boolean vowel, boolean upperCaseBiased) {
		return isVowel(uc) == vowel &&
				(upperCaseBiased ? text.indexOf(uc) < 0 :
					valid(text, i, uc)) &&
				valid(text, i, c);
	}
	private static boolean valid(String text, int i, char c) {
		return i == 0 || text.lastIndexOf(c, i-1) < 0;
	}
	
	
	
	private static int getMnemonicModifier() {
		return InputEvent.ALT_DOWN_MASK;
	}
	

}
