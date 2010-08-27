package aephyr.swing.nimbus;

import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

//import com.sun.java.swing.Painter;
import javax.swing.Painter;

public class RemoteUIDefaultsImpl implements RemoteUIDefaults {
	
	public static void main(String[] args) throws Exception {
		if (args.length > 0 && !args[0].isEmpty()) {
			File file = new File(args[0]);
			if (file.isFile()) {
				String[] statements = CodeTransfer.getStatements(file);
				CodeTransfer.doImport(
					statements,
					new ArrayList<Object>(),
					UIManager.getDefaults());
			}
		}
		Creator.setNimbusLookAndFeel();
		Registry registry = LocateRegistry.getRegistry();
		RemoteUIDefaults defaultsImpl = new RemoteUIDefaultsImpl();
		RemoteUIDefaults defaults = (RemoteUIDefaults)UnicastRemoteObject.exportObject(
				defaultsImpl, 0);
		registry.rebind(RemoteUIDefaults.class.getName(), defaults);
		try {
			controller = (RemoteController)registry.lookup(RemoteController.class.getName());
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		controller.ready();
		if (args.length > 1) {
			Preview.initiate(args[1], args[2], args[3]);
		}
		System.in.read();
		registry.unbind(RemoteUIDefaults.class.getName());
		UnicastRemoteObject.unexportObject(defaultsImpl, true);
		System.exit(0);

	}
	
	private static RemoteController controller;
	
	static void previewClosed() {
		try {
			if (controller != null)
				controller.previewClosed();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	static void setTabIndex(int index) {
		try {
			if (controller != null)
				controller.setTabIndex(index);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public RemoteUIDefaultsImpl() {
		
	}

	@Override
	public void put(String key, Serializable value) {
		if (value != null) {
			Object def = UIManager.getLookAndFeelDefaults();
			if (def instanceof Font) {
				Font a = (Font)def;
				Font b = (Font)value;
				if (a.getSize2D() == b.getSize2D() &&
						a.getStyle() == b.getStyle() &&
						a.getFamily().equals(b.getFamily()))
					value = null;
			}
		}
		UIManager.put(key, value);
	}
	
	@Override
	public Serializable get(String key, boolean def) {
		Object obj = def ?
				UIManager.getLookAndFeelDefaults().get(key) :
				UIManager.get(key);
		if (!(obj instanceof Serializable))
			return null;
		if (Type.getType(obj) == Type.Object)
			return null;
		return (Serializable)obj;
	}
	
	@Override
	public Color getColor(String key, boolean def) {
		Color col = def ?
				UIManager.getLookAndFeelDefaults().getColor(key) :
				UIManager.getColor(key);
		if (col.getClass() == ColorUIResource.class || col.getClass() == Color.class)
			return col;
		return new Color(col.getRGB());
	}
	
	private JPanel painterComponent = new JPanel(null);
	
	@Override
	public ImageIcon getImage(String key) {
		Object value = UIManager.get(key);
		BufferedImage img;
		if (value instanceof Painter<?>) {
			Painter<JComponent> painter = (Painter<JComponent>)value;
			img = new BufferedImage(25, 25, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			painter.paint(g, painterComponent, img.getWidth(), img.getHeight());
			g.dispose();
		} else if (value instanceof Icon) {
			Icon icn = (Icon)value;
			img = new BufferedImage(icn.getIconWidth(), icn.getIconHeight(),
					BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			icn.paintIcon(painterComponent, g, 0, 0);
			g.dispose();
		} else {
			throw new IllegalArgumentException();
		}
		return new ImageIcon(img);
	}
	
	@Override
	public String getTypeName(String key) {
		return Type.getType(UIManager.get(key)).name();
	}
	
	@Override
	public void exportTo(File file, String pkg, String cls, String mtd, boolean tabs) throws IOException {
		CodeTransfer.doExport(file, pkg, cls, mtd, tabs);
	}
	
	@Override
	public String export() {
		StringWriter writer = new StringWriter();
		try {
			CodeTransfer.doExport(writer, null);
		} catch (IOException e) {}
		return writer.toString();
	}
	
	@Override
	public boolean importStatements(String[] statements) {
		return CodeTransfer.doImport(statements);
	}
	
}
