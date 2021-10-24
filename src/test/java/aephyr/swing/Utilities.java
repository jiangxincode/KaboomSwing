package aephyr.swing;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Utilities {
	
	public static void setNimbusLookAndFeel() throws ClassNotFoundException,
			InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
			if ("Nimbus".equals(info.getName())) {
				UIManager.setLookAndFeel(info.getClassName());
				return;
			}
		}
	}
	
	
//	public static List<String> getClassNames(String pkg) {
//		
//	}
//	
//	public static List<String> getClassNames(String pkg, URL res) {
//		
//	}

}
