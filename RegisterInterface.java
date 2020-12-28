import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegisterInterface extends Remote {
	
	String register(String ID, String PW) throws RemoteException;
	
}
