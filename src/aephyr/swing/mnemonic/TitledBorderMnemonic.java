/**
 * 
 */
package aephyr.swing.mnemonic;

import javax.swing.JComponent;
import javax.swing.border.TitledBorder;

public class TitledBorderMnemonic extends DefaultMnemonic {
	
	public TitledBorderMnemonic(TitledBorder border) {
		super();
		this.border = border;
	}
	
	public TitledBorderMnemonic(TitledBorder border, int priority) {
		this(border, -1, priority, true);
	}
	
	public TitledBorderMnemonic(TitledBorder border, int extendedModifiers,
			int priority, boolean upperCaseBiased) {
		super(extendedModifiers, -1, priority, upperCaseBiased);
		this.border = border;
	}
	
	private TitledBorder border;
	
	@Override
	public String getMnemonicText(JComponent c) {
		String text = border.getTitle();
		int idx = text.indexOf('\u0332');
		if (idx >= 0) {
			StringBuilder s = new StringBuilder(text);
			s.deleteCharAt(idx);
			return s.toString();
		}
		return text;
	}

	@Override
	public void setDisplayedMnemonicIndex(JComponent c, int index) {
		throw new UnsupportedOperationException();
	}
	
	protected void updateDisplayedMnemonicIndex(JComponent c, int mnemonic) {
		String text = getMnemonicText(c);
		int idx = text.indexOf(mnemonic);
		if (idx < 0 || !isUpperCaseBiased()) {
			int lci = text.indexOf(Character.toLowerCase(mnemonic));
			if (idx < 0 || (lci >= 0 && lci < idx))
				idx = lci;
		}
		if (idx >= 0) {
			idx += 1;
			StringBuilder s = new StringBuilder(text);
			s.insert(idx, '\u0332');
			border.setTitle(s.toString());
		}
	}

}