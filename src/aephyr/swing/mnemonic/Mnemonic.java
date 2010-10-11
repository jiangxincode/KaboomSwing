package aephyr.swing.mnemonic;

import javax.swing.JComponent;

public interface Mnemonic {
	
	public static final int TOP_PRIORITY = 1000;
	
	public static final int HIGH_PRIORITY = 2000;
	
	public static final int LOW_PRIORITY = 5000;
	
	int getPreferredMnemonic(JComponent c, int index);
	
	int getPriority(JComponent c);
	
	void setMnemonic(JComponent c, int mnemonic);

}
