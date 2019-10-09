import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Proxy implements Runnable{
	
	private ServerSocket socketServidor;

	/**
	 * Semaphore for Proxy and Consolee Management System.
	 */
	private volatile boolean rodando = true;
	
	/**
	 * Data structure for constant order lookup of cache items.
	 * Key: URL of page/image requested.
	 * Value: File in storage associated with this key.
	 */
	//static HashMap<String, File> cache;

	/**
	 * Data structure for constant order lookup of blocked sites.
	 * Key: URL of page/image requested.
	 * Value: URL of page/image requested.
	 */
	static HashMap<String, String> sitesBloqueados;

	/**
	 * ArrayList of threads that are currently running and servicing requests.
	 * This list is required in order to join all threads on closing of server
	 */
	static ArrayList<Thread> threads;
	
	/**
	 * Cria o servidor proxy
	 * @param porta O número da porta em que o servidor Proxy irá rodar.
	 */
	@SuppressWarnings("unchecked")
	public Proxy(int porta) {

		// Load in hash map containing previously cached sites and blocked Sites
		//cache = new HashMap<>();
		sitesBloqueados = new HashMap<>();

		// Create array list to hold servicing threads
		threads = new ArrayList<>();

		// Start dynamic manager on a separate thread.
		new Thread(this).start();	// Starts overriden run() method at bottom

		try{
			// Load in blocked sites from file
			File FileSitesBloqueados = new File("sitesBloqueados.txt");
			if(!FileSitesBloqueados.exists()){
				System.out.println("Nenhum arquivo de sites bloqueados foi encontrado");
				System.out.println("Criando um novo arquivo...");
				FileSitesBloqueados.createNewFile();
			} else {
				FileInputStream fileInputStream = new FileInputStream(FileSitesBloqueados);
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
				sitesBloqueados = (HashMap<String, String>)objectInputStream.readObject();
				fileInputStream.close();
				objectInputStream.close();
			}
		} catch (IOException e) {
			System.out.println("Erro: " + e.getMessage());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			// Create the Server Socket for the Proxy 
			socketServidor = new ServerSocket(porta);

			// Set the timeout
			//serverSocket.setSoTimeout(100000);	// debug
			System.out.println("Esperando a conexão na porta:" + socketServidor.getLocalPort());
			rodando = true;
		} 

		// Catch exceptions associated with opening socket
		catch (SocketException se) {
			System.out.println("Exceção de socket");
			se.printStackTrace();
		}
		catch (SocketTimeoutException ste) {
			System.out.println("Timeout na conexão com o client");
		} 
		catch (IOException io) {
			System.out.println("IO exception: "+ io.getMessage());
		}
	}
	
	/**
	 * Saves the blocked and cached sites to a file so they can be re loaded at a later time.
	 * Also joins all of the RequestHandler threads currently servicing requests.
	 */
	private void FecharServidor(){
		System.out.println("\nClosing Server..");
		rodando = false;
		try{
			FileOutputStream fileOutputStream2 = new FileOutputStream("sitesBloqueados.txt");
			ObjectOutputStream objectOutputStream2 = new ObjectOutputStream(fileOutputStream2);
			objectOutputStream2.writeObject(sitesBloqueados);
			objectOutputStream2.close();
			fileOutputStream2.close();
			System.out.println("Todos os sites bloqueados foram salvos");
			try{
				// Close all servicing threads
				for(Thread thread : threads){
					if(thread.isAlive()){
						System.out.print("Esperando a thread: "+  thread.getId()+" fechar...");
						thread.join();
						System.out.println("Thread fechada");
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			} catch (IOException e) {
				System.out.println("Erro ao salvar os sites bloqueados");
				e.printStackTrace();
			}

			// Close Server Socket
			try{
				System.out.println("Finalizando a conexão");
				socketServidor.close();
			} catch (Exception e) {
				System.out.println("Exceção no fechamento do server socket");
				e.printStackTrace();
			}

		}
	
	/**
	 * Check if a URL is blocked by the proxy
	 * @param url URL to check
	 * @return true if URL is blocked, false otherwise
	 */
	public static boolean Bloqueado (String url){
		if(sitesBloqueados.get(url) != null){
			return true;
		} else {
			return false;
		}
	}
	
	public void listen(){

		while(rodando){
			try {
				// serverSocket.accpet() Blocks until a connection is made
				Socket socket = socketServidor.accept();
				
				// Create new Thread and pass it Runnable RequestHandler
				Thread thread = new Thread(new Handler(socket));
				
				// Key a reference to each thread so they can be joined later if necessary
				threads.add(thread);
				
				thread.start();	
			} catch (SocketException e) {
				// Socket exception is triggered by management system to shut down the proxy 
				System.out.println("Server closed");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	public static void main(String[] args) {
		//Create an instance of Proxy and begin listening for connections
			
			Scanner scanner = new Scanner(System.in);
			
			System.out.println("Insira a porta: ");
			int porta = scanner.nextInt();
			Proxy myProxy = new Proxy(porta);
			myProxy.listen();
			
			
			
			
			//Proxy myProxy = new Proxy(8085);
			
			//Proxy myProxy = new Proxy(porta);
			//myProxy.listen();	
			//System.out.println(porta);
		
		
		
		
		//Proxy myProxy = new Proxy(8085);
		//myProxy.listen();	
	}
	

	@Override
	public void run() {
		Scanner scanner = new Scanner(System.in);

		String command;
		while(rodando){
			System.out.println("Digite a URL de site para ser bloqueado"
					+ ", or type \"b\" para ver os sites bloqueados, "
					+ "\"cached\" to see cached sites, or "
					+ "\"close\" to close server.");
			command = scanner.nextLine();
			if(command.toLowerCase().equals("b")){
				System.out.println("\nLista de sites bloqueados: ");
				for(String key : sitesBloqueados.keySet()){
					System.out.println(key);
				}
				System.out.println();
			} 

//			else if(command.toLowerCase().equals("cached")){
//				System.out.println("\nCurrently Cached Sites");
//				for(String key : cache.keySet()){
//					System.out.println(key);
//				}
//				System.out.println();
//			}


			else if(command.equals("close")){
				rodando = false;
				FecharServidor();
			}


			else {
				sitesBloqueados.put(command, command);
				System.out.println("\n" + command + " Site bloqueado com sucesso \n");
			}
		}
		scanner.close();
	}

}
