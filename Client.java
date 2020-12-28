import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
	

	public static void main(String[] args) {
		RegisterInterface servObj;
		Remote RemoteObject;
		boolean b = true;	
		String receive=null, scIn=null;
		InetAddress ip;
		Socket s = null;

		BufferedWriter out=null;
		BufferedReader in=null;
		
		try {
			Registry r = LocateRegistry.getRegistry(6788);
			RemoteObject = r.lookup("Register");
			servObj = (RegisterInterface) RemoteObject;
			
			System.out.println("REGISTRAZIONE   |||   LOGIN");
			
			BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));	//Scanner
	        scIn = scanner.readLine(); 
			
	        
	        
	        while(b==true) {
	        	if(scIn.equalsIgnoreCase("Login")){
	        		
	        		System.out.println("Inserire nome utente e password");
	        		
	        		scIn = scanner.readLine();
		        	
					ip = InetAddress.getByName("localhost"); 
		            s = new Socket(ip, 8000);
		            
		            in=new BufferedReader(new InputStreamReader(s.getInputStream()));
		            out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
		            

		            out.write(scIn+"\n");
		            out.flush();

		            receive = in.readLine();
		            
		            if(receive.equals("E")) {
		            	System.out.println("ERROR: ID/Password non validi o Utente già loggato");
		            	in.close();
		            	out.close();
		            	s.close();
		            	return;
		            }
		            else {
		            	System.out.println("Login effettuato correttamente");
		            }
					
	        		b=false;
		        }
		        else if(scIn.equalsIgnoreCase("Registrazione")) {
		        	//Con questo comando vengono effettuate in sequenza Registrazione e Login
		        	
		        	System.out.println("Selezionare nome utente e password");
		        	
		        	scIn = scanner.readLine();
		        	int i = scIn.indexOf(' ');
					String Id = scIn.substring(0, i);
					String pW = scIn.substring(i+1);
		        	receive = servObj.register(Id, pW);
		        	
		        	if(receive.equals("E")) {
		        		System.out.println("ERRORE: ID già in uso!");
		            	return;
		        	}

		        	
		        	ip = InetAddress.getByName("localhost"); 
		            s = new Socket(ip, 8000);
		            
		            in=new BufferedReader(new InputStreamReader(s.getInputStream()));
		            out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
		           
		            out.write(scIn+"\n");
		            out.flush();
		            receive = in.readLine();

		            System.out.println("Login effettuato correttamente");
		            
		        	b=false;
		        }
		        else {
		        	System.out.println("Comando non valido. Riprova");
		        	scIn = scanner.readLine(); 
		        }
	        }
	        

			DatagramSocket cSocket = new DatagramSocket (s.getLocalPort());
			

			byte [ ] buffer = new byte [32];
			DatagramPacket inpack;

			
			ByteArrayOutputStream bout= new ByteArrayOutputStream();
			DataOutputStream dout = new DataOutputStream (bout);
			byte [ ] outBA = new byte [32];
			DatagramPacket outpack;

	        
	        System.out.println();
	        b = true;
	        
	        
	        //Ciclo principale, b viene settato a FALSE solo dopo il logout
	        while(b==true) {

	        	
	        	System.out.println("SFIDA		|||		Mostra Inviti		|||		Aggiungi Amico		|||		Lista Amici		|||		Classifica		|||		Logout");
	        	scIn=scanner.readLine();
	        	
	        	if(scIn.equalsIgnoreCase("Sfida")) {
	        		out.write("S\n");
	        		out.flush();
	        		System.out.println("Inserisci l'ID dell'amico che vuoi sfidare!");
	        		scIn = scanner.readLine();
	        		out.write(scIn + "\n");
	        		out.flush();
	        		
	        		System.out.println("Richiesta effettuata. Attendi!");
	        		
	        		
	        		String recv = null;

	        			recv = in.readLine();

	        			if(recv.equals("E")){
		        			System.out.println("Errore: Utente offline o non presente in lista amici");
		        			System.out.println();
		        		}
		        		else {
		        			if(recv.equals("T")) {
		        				System.out.println("Tempo Scaduto!");
		        				System.out.println();
		        			}
		        			else if(recv.equals("NA")) {
		        				System.out.println("Sfida rifiutata!");
		        				System.out.println();
		        			}
		        		
		        			else {
		        				System.out.println("Sfida accettata! Preparati!");
		        				
		        				
		        				recv = in.readLine();
		        				System.out.println("VIA!");
		        				System.out.println();
		        				
		        				//Ciclo della sfida
		        				while(!(recv.equals("T"))) {
			        				System.out.println(recv);
			        				scIn = scanner.readLine();
			        				out.write(scIn+"\n");
			        				out.flush();
			        				recv = in.readLine();
		        				}
		        				System.out.println("PARTITA FINITA!");
		        				recv = in.readLine();
		        				System.out.println(recv);
		        				System.out.println();
		        				
		        			}
		        			
		        		}
	        		
	        		
	        	}
	        	else if(scIn.equalsIgnoreCase("Mostra inviti")) {

	        		System.out.println("Controllo se ci siano nuovi inviti...");

	        		inpack = new DatagramPacket(buffer, buffer.length);
	        		
	        		//Attesa di 2 secondi per controllare se ci siano nuovi messaggi UDP
	        		cSocket.setSoTimeout(2000);		
	        		
	        		boolean timeout = true;
	        		try {
	        			cSocket.receive(inpack);
	        			timeout = false;
	        		} catch (SocketTimeoutException e){
	        			System.out.println("Non hai nuovi inviti!");
	        			System.out.println();
	        		}
					
	        		if(timeout == false) {
	        			String sentence = new String( inpack.getData()).trim();
	        			System.out.println("Hai ricevuto un invito da " + sentence + ". Vuoi giocare?");
	        			scIn=scanner.readLine();
	        			if(scIn.equalsIgnoreCase("Si")) {	//Sfida accettata
	        			
	        				
	        				dout.write("S".getBytes());
	    					outBA = bout.toByteArray();
	    					
	    					outpack= new DatagramPacket(outBA, outBA.length, inpack.getAddress() , inpack.getPort());
	    					outpack.setData(outBA,0,outBA.length);
	    					outpack.setLength(outBA.length);
	    					cSocket.send(outpack);
	    					
	    					System.out.println("Setup della partita in corso, attendi!");
	    					

	    					
	    					buffer = new byte [32];
	    					inpack = new DatagramPacket(buffer, buffer.length);
	    					timeout=true;
	    					
	    					/*
	    					 * Timeout di 15 secondi necessario affinchè, nel caso l'altro utente si scollegasse in maniera impropria, non
	    					 * si resti bloccati in sua attesa
	    					 */
	    					cSocket.setSoTimeout(15000);	
	    					try {
	    	        			cSocket.receive(inpack);
	    	        			timeout = false;
	    	        		} catch (SocketTimeoutException e){
	    	        			System.out.println("Qualcosa è andato storto! Riprova");
	    	        			System.out.println();
	    	        		}
	    					if(timeout==false) {
	    						out.write("SA\n");	//Viene informato il proprio Thread-Server della presenza di una sfida
		    					out.flush();
		    					
		    					String recv = in.readLine();
		    					System.out.println("VIA!");
		    					System.out.println();
		    					
		        				while(!(recv.equals("T"))) {
			        				System.out.println(recv);
			        				scIn = scanner.readLine();
			        				out.write(scIn+"\n");
			        				out.flush();
			        				recv = in.readLine();
		        				}
		        				
		        				System.out.println("PARTITA FINITA!");
		        				recv = in.readLine();
		        				System.out.println(recv);
		        				System.out.println();
		        				
	    					
	    					}

	        			
	        			}
	        			else {
	        				dout.write("NA".getBytes());
	    					outBA = bout.toByteArray();
	    					
	    					outpack= new DatagramPacket(outBA, outBA.length, inpack.getAddress() , inpack.getPort());
	    					outpack.setData(outBA,0,outBA.length);
	    					outpack.setLength(outBA.length);
	    					cSocket.send(outpack);
	    					buffer = new byte [32];
	    					inpack = new DatagramPacket(buffer, buffer.length);
	        			}
	        			bout.reset();						
			   			dout = new DataOutputStream (bout);
	        		}
	        	}
	        	else if(scIn.equalsIgnoreCase("Aggiungi Amico")) {

	        		out.write("AF\n");
	        		out.flush();
	        		System.out.println("Inserisci l'ID dell'amico");
	        		scIn = scanner.readLine();
	        		out.write(scIn + "\n");
	        		out.flush();
	        		if(in.readLine().equals("E")){
	        			System.out.println("Errore: ID inesistente o amico già presente nella lista");
	        			System.out.println();
	        		}
	        		else {
	        			System.out.println("Amico aggiunto!");
	        			System.out.println();
	        		}
	        		
	        		
	        	}
	        	else if(scIn.equalsIgnoreCase("Lista Amici")) {

	        		out.write("LA\n");
	        		out.flush();
	        		System.out.print("Lista Amici: ");
	        		System.out.println(in.readLine());

	        		System.out.println();
	        	}
	        	else if(scIn.equalsIgnoreCase("Classifica")) {
	        		
	        		out.write("MC\n");
	        		out.flush();
	        		System.out.println("Classifica:");
	        		System.out.println(in.readLine());
	        		System.out.println();
	        	}
	        	else if(scIn.equalsIgnoreCase("Logout")) {
	        		b=false;
	        	}
	        	else {
	        		System.out.println("Comando non valido. Riprova");
	        		
	        	}
	        }

	        out.write(scIn+"\n");
	        out.flush();
	        receive=in.readLine();
	        System.out.println(receive);
	        cSocket.close();
	        in.close();
	        out.close();
	        s.close();
		
			
		} catch (NumberFormatException | RemoteException | NotBoundException e) {
				e.printStackTrace();
		} catch (IOException e) {
				e.printStackTrace();
		}
		
	}
}

