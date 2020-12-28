import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@SuppressWarnings("serial")
public class Server extends RemoteServer implements RegisterInterface {

	
	
	/*
	 *  Le seguenti ConcurrentHashMap sono necessarie per comunicare alcuni dati fra i thread. 
	 *  
	 *  "map" contiene i dati base di ogni utente (coppia NOME-ArrayList<String>, quest'ultima contiene PW, PUNTEGGIO, flag "isOnline" e LISTA AMICI)
	 *  "IdPort" contiene le coppie nome-porta di tutti i client per permettere la comunicazione UDP
	 *  "wordMap" contiene le parole scelte da Server1 per la sfida, che anche Server2 deve utilizzare
	 *  "pointMap" contiene le coppie nome-punteggio di ogni giocatore, in modo che Server1 e Server2 possano confrontare i punteggi dei due giocatori
	 * 
	 */

	ConcurrentHashMap<String,Integer> IdPort;
	ConcurrentHashMap<String, ArrayList<String>> map;
	ConcurrentHashMap<String, String[]> wordMap;
	ConcurrentHashMap<String, Integer> pointMap;
	ThreadPoolExecutor executor;
	
	//Lista di parole caricate dal dizionario
	ArrayList<String> wordList;
	
	//Rsipettivamente Dizionario e file JSON con dati utente 
	File file1, file2;
	
	
	@SuppressWarnings("unchecked")
	public Server() {

		IdPort = new ConcurrentHashMap<String, Integer>();
		map = new ConcurrentHashMap<String, ArrayList<String>>();
		wordList = new ArrayList<String>();
		wordMap = new ConcurrentHashMap<String, String[]>();
		pointMap = new ConcurrentHashMap<String, Integer>();
		executor=(ThreadPoolExecutor) Executors.newCachedThreadPool();
		file1 = new File("Words.txt");
		
		if(!file1.exists()) {
			System.out.println("ERRORE: NESSUN DIZIONARIO PRESENTE!");
			System.exit(1);
		}
		
		else {
			
			try {
				Scanner sc = new Scanner(file1);
				while(sc.hasNext()){
					 wordList.add(sc.nextLine());    
				}
				sc.close();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			
			
			
			file2 =new File("UserData.json");	
			
			if(!file2.exists()) {	//Nel caso il file non esista viene creato
				try {
					file2.createNewFile();	
					 JSONObject rootObject = new JSONObject();	
				     JSONArray Arr = new JSONArray();
				     rootObject.put("Record", Arr);
				     
					 file2.createNewFile();
					 FileWriter fileWriter = new FileWriter(file2);
					 System.out.println("Writing JSON object to file");
				     System.out.println("-----------------------");
					 fileWriter.write(rootObject.toJSONString());
					 fileWriter.flush();
					 fileWriter.close();

					
					
				} catch (IOException e) {
					e.printStackTrace();
				}	
			}
			else {
				loadData(file2, map);	//Altrimenti i dati presenti vengono caricati nella hashmap
			}
		}
		
		
	}
	
	
	
	
	/*
	 * Viene effettuato parsing del file, dopodichè per ogni entrata dell'array viene creata una nuova associazione ID-ArrayList<String>
	 * nella hashmap e viene riempita l'ArrayList con i dati contenuti nell'entrata succitata
	 */
	@SuppressWarnings("unchecked")
	private void loadData(File f, ConcurrentHashMap<String, ArrayList<String>> map) {
		
		JSONParser parser = new JSONParser();
		Object obj;
		try {
			obj = parser.parse(new FileReader("UserData.json"));
			
			JSONObject jsonObject = (JSONObject) obj;		
			

			JSONArray arr = (JSONArray) jsonObject.get("Record");		
			
			for(int i=0; i<arr.size(); i++) {
				Object u = arr.get(i);			
				JSONObject jObj = (JSONObject) u;	
				
				String id = (String) jObj.get("ID");
				map.put(id, new ArrayList<String>());
				map.get(id).add((String) jObj.get("PW"));

				map.get(id).add(jObj.get("Points").toString());
				map.get(id).add("N");							
				
				ArrayList<String> friends = (ArrayList<String>) jObj.get("Friends");
				int dim = friends.size();
				for(int j=0; j<dim; j++) {
					map.get(id).add(friends.get(j));
				}
				

			}
			
			
			
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}		
		

	}
	
	
	
	
	/*
	 * Registrazione di nuovo utente: se ID è già presente viene restituito messaggio di errore, altrimenti si aggiunge un nuovo elemento al JSONArray
	 * ed una nuova associazione alla hashMap
	 */
	@SuppressWarnings("unchecked")
	@Override
	public synchronized String register(String ID, String PW) throws RemoteException {
		
		
		
		if(map.containsKey(ID)){
			return "E";
		}
		
		JSONParser parser = new JSONParser();
		Object obj;

		try {
			
			obj = parser.parse(new FileReader("UserData.json"));
			JSONObject jsonObject = (JSONObject) obj;		
			JSONArray arr = (JSONArray) jsonObject.get("Record");
			
			JSONObject OBJ = new JSONObject();
			
			OBJ.put("Friends", new ArrayList<String>());
			OBJ.put("Points", 0);
			OBJ.put("PW", PW);
			OBJ.put("ID", ID);
			
			arr.add(OBJ);
			
			FileWriter fileWriter = new FileWriter(file2);
			System.out.println("Writing JSON object to file");
			System.out.println("-----------------------");

			fileWriter.write(jsonObject.toJSONString());
			fileWriter.flush();
			fileWriter.close();
			
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		
		
		map.put(ID, new ArrayList<String>());
		map.get(ID).add(PW);
		map.get(ID).add("0");
		map.get(ID).add("N");
		


		return "Account creato!";
	}

	
	
	
	public static void main(String[] args) {
		Server s = new Server();
		System.out.println("Caricamento completato");
			
		RegisterInterface stub;
		
		try {
			stub = (RegisterInterface) UnicastRemoteObject.exportObject(s, 0);
			
			LocateRegistry.createRegistry(6788);	
			Registry r=LocateRegistry.getRegistry(6788);
			r.rebind("Register", stub);
			
			@SuppressWarnings("resource")
			ServerSocket server = new ServerSocket(8000);
			System.out.println("Server avviato");
			
			
			while(true) {
				
				Socket client = server.accept();
				WordQuizzleTask wq = new WordQuizzleTask(client,s.wordList, s.map, s.IdPort, s.wordMap, s.pointMap);
				s.executor.execute(wq);
			}
				
			
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		

	}

}
