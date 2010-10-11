package aephyr.swing.mnemonic;

import javax.swing.JComponent;

public interface MnemonicFactory {

	Mnemonic createMnemonic(JComponent c);
	
	IndexedMnemonic createIndexedMnemonic(JComponent c, int index);
	
}
