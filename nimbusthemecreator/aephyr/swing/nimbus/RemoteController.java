package aephyr.swing.nimbus;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteController extends Remote {
	
	void setTabIndex(int index) throws RemoteException;
	
	void previewClosed() throws RemoteException;
	
	void ready() throws RemoteException;
	
}
