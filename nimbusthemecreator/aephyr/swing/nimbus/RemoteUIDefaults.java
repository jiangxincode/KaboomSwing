package aephyr.swing.nimbus;

import java.io.File;
import java.awt.Color;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.swing.ImageIcon;


public interface RemoteUIDefaults extends Remote {
	
	void put(String key, Serializable value) throws RemoteException;
	
	Serializable get(String key, boolean def) throws RemoteException;
	
	Color getColor(String key, boolean def) throws RemoteException;

	ImageIcon getImage(String key) throws RemoteException;
	
	String getTypeName(String key) throws RemoteException;
	
	void exportTo(File file, String packageName, String className, String methodName, boolean tabs) throws RemoteException, IOException;
	
	String export() throws RemoteException;
	
	boolean importStatements(String[] statements) throws RemoteException;
	
}
