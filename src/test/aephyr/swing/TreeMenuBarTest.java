package test.aephyr.swing;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.tree.*;

import aephyr.swing.CurlMenuBar;
import aephyr.swing.TreeMenuBar;

public class TreeMenuBarTest implements Runnable {
	public static void main(String[] args) throws Exception {
		for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
			if ("Nimbus".equals(info.getName())) {
				UIManager.setLookAndFeel(info.getClassName());
				break;
			}
		}
//		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		SwingUtilities.invokeLater(new TreeMenuBarTest());
	}
	
	private Random random = new Random();

	@Override
	public void run() {
		TreeMenuBar bar = new TreeMenuBar();
		MutableTreeNode root = createTreeNode(4);
		TreePath path = new TreePath(root);
		for (TreeNode node=root; node.getChildCount()>0;) {
			node = node.getChildAt(random.nextInt(node.getChildCount()));
			path = path.pathByAddingChild(node);
		}
		bar.setPath(path);
		JPanel north = new JPanel(new BorderLayout());
		north.add(bar, BorderLayout.WEST);
		JPanel center = new JPanel();
		center.setPreferredSize(new Dimension(500, 300));
		JFrame frame = new JFrame(getClass().getSimpleName());
		frame.add(north, BorderLayout.NORTH);
		frame.add(center, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	private int bellCurve() {
		int i = Integer.bitCount(random.nextInt());
		if (i < 10)
			return i;
		return i-10;
	}
	
	private MutableTreeNode createTreeNode(int depth) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(createCellValue());
		if (--depth >= 0)
			for (int i=bellCurve(); --i>=0;)
				node.add(createTreeNode(depth));
		return node;
	}
	
	private String createCellValue() {
		char[] c = new char[random.nextInt(5)+4];
		for (int i=c.length; --i>=0;)
			c[i] = (char)(random.nextInt(26)+'a');
		c[0] = Character.toUpperCase(c[0]);
		return new String(c);
	}
	
	
}
