import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class WordQuizzleTask implements Runnable {

	private Socket client = null;
	
	ConcurrentHashMap<String, ArrayList<String>> map;
	ConcurrentHashMap<String, Integer> IdPort;
	ConcurrentHashMap<String, String[]> wordMap;
	ConcurrentHashMap<String, Integer> pointMap;
	
	ArrayList<String> wordList;
	
	
	public WordQuizzleTask(Socket client,ArrayList<String> wordList, ConcurrentHashMap<String,ArrayList<String>> map, 
								ConcurrentHashMap<String, Integer> IdPort, ConcurrentHashMap<String, String[]> wordMap, ConcurrentHashMap<String, Integer> pointMap){
		this.client = client;
		this.wordList = wordList;
		this.map = map;
		this.IdPort = IdPort;
		this.pointMap = pointMap;
		this.wordMap = wordMap;
	}
	
	
	
	/*
	 * Il metodo match effettua un ciclo di 30 secondi (oppure meno, nel caso l'utente traduca tutte le parole anzitempo) nel quale
	 * semplicemente invia parole in italiano e riceve parole in inglese, controllando la loro correttezza. Restituisce un intero >=0
	 * 
	 */
	
	private int match(BufferedReader reader, BufferedWriter writer, String[] words) {
		long t= System.currentTimeMillis();
		  long end = t+30000;
		  int k=0;
		  int points = 0;
		  while(System.currentTimeMillis() < end && k<20) {
			  try {
				writer.write(words[k]+"\n");
				writer.flush();
				k++;
				String results = reader.readLine();
				
				//Se la parola è stata tradotta correttamente ed è stata sottomessa entro il tempo massimo si assegnano punti, altrimenti si tolgono
				if(results.equalsIgnoreCase(words[k]) && System.currentTimeMillis() < end) {
					  points = points+2;
				}
				else {
					if(points>0) {
						points--;
					}	  
				}
				k++;
			} catch (IOException e) {
				e.printStackTrace();
			}
			  
			  
		  }
		  try {
			writer.write("T\n");
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		 
		return points;
	}	
	
	
	
	/*
	 * Metodi quickSort e Partition per effettuare il sorting della classifica. In Partition si effettua l'ordinamento sia dell'array dei punti
	 * sia dell'array dei nomi secondo un unico criterio, in modo da mantenere consistenti le varie coppie nome-punteggio
	 * 
	 */
	
	private void quickSort(String[] sa, int[] ia, int start, int end) {
		int partition = partition(sa, ia, start, end);
        if(partition-1>start) {
            quickSort(sa, ia, start, partition - 1);
        }
        if(partition+1<end) {
            quickSort(sa, ia, partition + 1, end);
        }
	}
	
	
	private int partition(String[] sarr, int[] arr, int start, int end){
		int pivot = arr[end];
		String pivo = sarr[end];
		
		
        for(int i=start; i<end; i++){
            if(arr[i]<pivot){
                int temp= arr[start];
                String tmp = sarr[start];
                
                arr[start]=arr[i];
                sarr[start] = sarr[i];
                
                arr[i]=temp;
                sarr[i]=tmp;
                start++;
            }
        }
 
        int temp = arr[start];
        String tmp = sarr[start];
        
        arr[start] = pivot;
        sarr[start] = pivo;
        
        arr[end] = temp;
        sarr[end] = tmp;
        
        return start;
	}
	
	
	
	/*
	 * Il metodo effettua il parsing del file JSON, poi scorre il JSONArray così ottenuto finchè non trova gli ID corrispondenti ai due utenti
	 * e aggiunge ognuno nella lista amici dell'altro, dopodichè riscrive il file.
	 */
	
	@SuppressWarnings("unchecked")
	private synchronized void addFriend(String whoAdds, String added) {			
																				
		JSONParser parser = new JSONParser();
		Object obj;																
		ArrayList<String> lis;
			try {
				obj = parser.parse(new FileReader("UserData.json"));
				
				
				JSONObject jsonObject = (JSONObject) obj;		

				JSONArray arr = (JSONArray) jsonObject.get("Record");		
				
				for(int i=0; i<arr.size(); i++) {
					Object u = arr.get(i);			
					JSONObject jObj = (JSONObject) u;	
					if(jObj.get("ID").equals(whoAdds)) {
						lis = (ArrayList<String>) jObj.get("Friends");		
						lis.add(added);
						jObj.put("Friends", lis);					
					}
					else if(jObj.get("ID").equals(added)) {
						lis = (ArrayList<String>) jObj.get("Friends");
						lis.add(whoAdds);
						jObj.put("Friends", lis);
					}
				}
				
				FileWriter fileWriter = new FileWriter("UserData.json");
				System.out.println("Adding new friend relationship between " + whoAdds + " and " + added);
				System.out.println("-----------------------");

				fileWriter.write(jsonObject.toJSONString());
				fileWriter.flush();
				fileWriter.close();
				
			} catch (IOException | ParseException e) {
				e.printStackTrace();
			}

	}
	
	
	
	/*
	 * Similmente al metodo precedente effettua il parsing, scorre l'array e aggiorna i punti dell'utente
	 */
	@SuppressWarnings("unchecked")
	private synchronized void addPoints(String Id, int points) {
		JSONParser parser = new JSONParser();
		Object obj;																

		try {
			obj = parser.parse(new FileReader("UserData.json"));
			
			
			JSONObject jsonObject = (JSONObject) obj;		

			JSONArray arr = (JSONArray) jsonObject.get("Record");		
			
			for(int i=0; i<arr.size(); i++) {
				Object u = arr.get(i);			
				JSONObject jObj = (JSONObject) u;	
				if(jObj.get("ID").equals(Id)) {
					jObj.put("Points", points);					
				}

			}
			
			FileWriter fileWriter = new FileWriter("UserData.json");
			System.out.println("Updating " + Id + " score");
			System.out.println("-----------------------");

			fileWriter.write(jsonObject.toJSONString());
			fileWriter.flush();
			fileWriter.close();
			
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}

	}
	
	
	
	
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		BufferedReader reader = null;
		BufferedWriter writer = null;
		String response=null;
		
		
			try {
				reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
				writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
				
		            
				response = reader.readLine();	//Lettura Id e PW

				System.out.println("Serving the user: " + response);
				
				int i = response.indexOf(' ');
				String Id = response.substring(0, i);
				String pW = response.substring(i+1);
				
				//Se ID non è presente, se PW non è associata a quell'ID o se l'utente è già Online segnala errore
				if(!(map.containsKey(Id) && map.get(Id).get(0).equals(pW) && map.get(Id).get(2).equals("N"))) {
					
					writer.write("E\n");
					writer.flush();
					reader.close();
					writer.close();
					this.client.close();
					return;
				}
				map.get(Id).set(2, "Y");	//Setta il flag "isOnline"
				
				IdPort.put(Id, client.getPort());	//Aggiunge la coppia ID-Porta per successive comunicazioni UDP
				
				writer.write("L\n");
				writer.flush();
				
				
				
				//UDP
				DatagramSocket dsocket= new DatagramSocket();
				
				ByteArrayOutputStream bout= new ByteArrayOutputStream();
				DataOutputStream dout = new DataOutputStream (bout);		
				byte [ ] outBA = new byte [32];
				DatagramPacket outpack;
				
				byte [ ] inBA = new byte [32];
				DatagramPacket inpack= new DatagramPacket(inBA, inBA.length);
				
				
				
				/*
				 * Questo è il ciclo principale lato server: finchè non riceve il comando "logout" continua a ciclare,
				 * se il comando ricevuto non corrisponde a nessuno di quelli disponibili invia un messaggio informativo all'utente
				 */
				while(!response.equalsIgnoreCase("logout")){

				   response = reader.readLine();	//Lettura del messaggio inviato dal client
				   
				   
				   if(response.equals("SA")) {		//Il client ha notificato l'arrivo di una sfida al Server
					   
					  String[] words=wordMap.get(Id);	//Effettua una get dall'oggetto condiviso e salva le coppie di parole
					  
					  int points = match(reader,writer,words);
					  
					  String competitor = Id + "C";		
					  pointMap.put(competitor, points);		//Effettua una put per comunicare all'altro thread del server il punteggio dell'utente

					  
					  /*
					   * Dato che le due partite potrebbero terminare con un certo scarto temporale è necessario
					   * introdurre un ciclo che controlli, ad ogni secondo, se sia presente nell'oggetto condiviso il dato
					   * riguardante il punteggio dell'avversario
					   */
					  
						while(!pointMap.containsKey(Id)) {
							try {

								Thread.sleep(2000);		
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						
						
						int otherp = pointMap.get(Id);
						if(points > otherp) {
							writer.write("Hai totalizzato " + points + " punti, mentre il tuo avversario solo " + otherp + "! HAI VINTO! Ottieni 3 punti bonus!\n");
							points = points+3;
						}
						else if(points == otherp) {
							writer.write("Hai totalizzato " + points + " punti, esattamente come il tuo avversario! E' UN PAREGGIO!\n");
						}
						else {
							writer.write("Hai totalizzato " + points + " punti, mentre il tuo avversario " + otherp + "! HAI PERSO...\n");
						}
						writer.flush();   
						
						int p = Integer.parseInt(map.get(Id).get(1))+points;
						map.get(Id).set(1, Integer.toString(p));		//Aggiornamento di dati utente e di file JSON
						addPoints(Id, p);
						
		        		pointMap.remove(Id);		//Rimozione di dati già letti dagli oggetti condivisi per non creare confusioni in letture successive
		        		wordMap.remove(Id);
				   }
				   
				   else if(response.equals("S")) {	//L'utente vuole sfidare un altro giocatore
					   
					   response = reader.readLine();
					   
					   //Se l'utente sfidato è online ed è amico dello sfidante si procede, altrimenti messaggio di errore
					   if(map.get(Id).contains(response) && map.get(response).get(2).equals("Y")) {		
						   
						    dout.write(Id.getBytes());
							outBA = bout.toByteArray();

							
							outpack = new DatagramPacket(outBA, outBA.length, InetAddress.getByName("localhost"), IdPort.get(response));
							outpack.setData(outBA,0,outBA.length);
							outpack.setLength(outBA.length);		//Comunicazione della sfida all'altro client
							dsocket.send(outpack);
														
							inBA = new byte [32];
							inpack = new DatagramPacket(inBA, inBA.length);
							boolean timeout = false;
							dsocket.setSoTimeout(30000);
							try {
								dsocket.receive(inpack);
							} catch (SocketTimeoutException e) {
								timeout = true;
								writer.write("T\n");
								writer.flush();
							}
							if(!timeout) {
								String sentence = new String( inpack.getData()).trim();		//Risposta di client2
								String[] words = new String[20];

								if(sentence.equals("S")) {	//L'altro giocatore ha accettato la sfida
									
					        		Random rand = new Random();
					        		int n = rand.nextInt(200);		//Scelta casuale delle parole dal dizionario
					        		int k=0;
					        		
					        		
					        		
					        		/*
					        		 * In questo ciclo si effettuano 10 richieste get a MyMemory, salvando in un array statico di 20 posizioni
					        		 * sia le parole in ITA che in ENG
					        		 */
					        		while(k<20) {						
						        			words[k]=wordList.get(n);
						        			   
						        			   
						        			String sURL = "https://api.mymemory.translated.net/get?q=" + words[k] + "&langpair=it|en";
		
							   			    URL url = new URL(sURL);
							   			    URLConnection request = url.openConnection();
							   			    request.connect();
							   			       
							   			    JSONParser parser = new JSONParser();
							   				Object obj;
							   				obj = parser.parse(new InputStreamReader((InputStream) request.getContent()));
							   				JSONObject root = (JSONObject) obj;
							   				JSONObject jobj = (JSONObject) root.get("responseData");
							   				k++;
							   				words[k] = jobj.get("translatedText").toString();
							   			       
						        			n = rand.nextInt(200);	
						        			k++;
					        		}
					        		   
						        	wordMap.put(response, words);		//Le parole vengono inserite in un oggetto condiviso
		   
						        	bout.reset();						
						   			dout = new DataOutputStream (bout);
						   					
						        	dout.write("D".getBytes());		
						        	outBA = bout.toByteArray();
									outpack = new DatagramPacket(outBA, outBA.length, InetAddress.getByName("localhost"), IdPort.get(response));
									outpack.setData(outBA,0,outBA.length);			//Viene comunicato al client2 che il setup è andato a buon fine
									outpack.setLength(outBA.length);
									dsocket.send(outpack);
					        		
									writer.write("D\n");		//Viene comunicato al client1 che il setup è andato a buon fine
									writer.flush();
									
						   			
						   			int points = match(reader,writer,words);
									
									
									pointMap.put(response, points);
									String identifier = response + "C";
									while(!pointMap.containsKey(identifier)) {
										try {
											Thread.sleep(2000);		
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
									}
									int otherp = pointMap.get(identifier);
									if(points > otherp) {
										writer.write("Hai totalizzato " + points + " punti, mentre il tuo avversario solo " + otherp + "! HAI VINTO! Ottieni 3 punti bonus!\n");
										points = points+3;
									}
									else if(points == otherp) {
										writer.write("Hai totalizzato " + points + " punti, esattamente come il tuo avversario! E' UN PAREGGIO!\n");
									}
									else {
										writer.write("Hai totalizzato " + points + " punti, mentre il tuo avversario " + otherp + "! HAI PERSO...\n");
									}
									writer.flush();   
	
									int p = Integer.parseInt(map.get(Id).get(1));
									p = p+points;
									map.get(Id).set(1, Integer.toString(p));
									addPoints(Id, p);
									
					        		pointMap.remove(identifier);
					        		   
								}
								else {		//Nel caso in cui l'altro giocatore rifiutasse la sfida viene mandato un messaggio informativo
									
									writer.write("NA\n");
					        		writer.flush();
								}
								bout.reset();						
					   			dout = new DataOutputStream (bout);
							}	   	   
			        		   
					   }  
					   else {
						   writer.write("E\n");
						   writer.flush();
					   }

				   }
				   else if(response.equals("LA")) {	//Richiesta di visualizzazione lista amici

					   ArrayList<String> lis = new ArrayList<String>();
					   int dimension = map.get(Id).size();
					   for(int k=3; k<dimension; k++) {
						   lis.add(map.get(Id).get(k));		
					   }
					   
					   String jsonStr = JSONArray.toJSONString(lis);   
					   writer.write(jsonStr+"\n");
					   writer.flush();
	   
				   }
				   else if(response.equals("AF")) {	//Richiesta di aggiunta amico
					   response = reader.readLine();
					   
					   //Se l'ID dell'amico non esiste o se esiste già una relazione di amicizia viene segnalato un errore
					   if(!map.containsKey(response) || map.get(Id).contains(response)) {	
						   
						   writer.write("E\n");
						   writer.flush();
					   }
					   else {
						   
						   map.get(Id).add(response);	
						   map.get(response).add(Id);	//Aggiornamento di Map e del file JSON
						   addFriend(Id, response);

						   writer.write("O\n");
						   writer.flush();
					   }
				   }
				   else if(response.equals("MC")) {	//Richiesta di mostrare classifica
	   
					   int dim = map.get(Id).size();
					  
					   /*
					    * L'arrayList associato ad ID contiene 3 campi non utili ai fini della classifica (cioè PW, Punti e "isOnline"), inoltre è 
					    * necessario aggiungere un campo per contenere il nome dell'utente stesso. Per questo motivo gli array successivi hanno dim-2				   
					    */
					   
					   String[] friendsArr = new String[dim-2];
					   int[] pointsArr = new int[dim-2];
					   int j=3;
					   
					   for(j=3; j<dim; j++) {	//I primi 3 campi, come detto precedentemente, riguardano dati non utili e vanno ignorati
						   
						   String friend = map.get(Id).get(j);
						   
						   int point = Integer.parseInt(map.get(friend).get(1));
						   friendsArr[j-3] = friend;
						   pointsArr[j-3] = point;
					   }
					  
					   //Nell'ultima posizione viene salvato ID stesso e i propri punti
					   friendsArr[j-3] = Id;
					   pointsArr[j-3] = Integer.parseInt(map.get(Id).get(1));
		   
					   
					   quickSort(friendsArr, pointsArr, 0, friendsArr.length-1);	//Sorting
					   
					   JSONArray arr = (JSONArray) new JSONArray();
					   
					   for(j=0;j<friendsArr.length;j++) { 
						   
						   JSONObject OBJ = new JSONObject();
							OBJ.put(friendsArr[j], pointsArr[j]);
							arr.add(OBJ);			//Salvataggio delle coppie nome-punteggio in JSONArray
							
					   }
					   
					   String jsonStr = arr.toJSONString();
					   writer.write(jsonStr+"\n");
					   writer.flush();
				   }

				   
				}
						
				
				IdPort.remove(Id);			//Rimozione di valori non più utili negli oggetti condivisi
				map.get(Id).set(2, "N");
				dsocket.close();
				
				writer.write("Logout avvenuto con successo\n");
				writer.flush();		
				
			} catch (IOException | ParseException e) {
				e.printStackTrace();
			}
			
			try {

				reader.close();
				writer.close();
				this.client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			

	}

}
