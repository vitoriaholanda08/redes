/*
 * César Augusto S. de Mello 		2017.1906.021-5
 * Guilherme Gabriel N. Ferreira 	2017.1906.066-5
 * Vitória H. Vidal 				2017.1906.043-6
 * 
 * Trabalho 1
 * Profª Hanna Karina S. Rubinsztejn
 * 
 */
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
	 * Variavel para verificar se o problema esta em execucao.
	 */
	private volatile boolean ativo = true;
	
	//*******************************************
	/**
	 * Data structure for constant order lookup of cache items.
	 * Key: URL of page/image requested.
	 * Value: File in storage associated with this key.
	 */ 
	static HashMap<String, File> cache;
	//*******************************************

	/**
	 * Estrutura de dados para armazenamento de itens na cache
	 * Chave: URL da pagina
	 * Valor: URL da pagina
	 */
	static HashMap<String, String> sitesBloqueados;


	/**
	 * Array de threads que estao em execucao
	 */
	static ArrayList<Thread> arrayThreads;
	
	/**
	 * Metodo principal para instanciar o proxy e receber a porta de conexao
	 * @param args
	 */
	public static void main(String[] args) {	
			Scanner scanner = new Scanner(System.in);
			
			System.out.println("Insira a porta: ");
			int porta = scanner.nextInt();
			Proxy myProxy = new Proxy(porta);
			myProxy.listen();
	}
	

	@Override
	public void run() {
		Scanner scanner = new Scanner(System.in);
		String entrada;
		
		while(ativo){
			System.out.println("Digite a URL de site para ser bloqueado"
					+ ", ou \"bloqueado\" para ver os sites bloqueados, "
					+ "\"cache\" para ver sites em cache, ou "
					+ "\"sair\" para encerrar a sessão.");
			entrada = scanner.nextLine();
			
			switch(entrada) {
				case "bloqueado":
					System.out.println("\nLista de sites bloqueados: ");
					for(String key : sitesBloqueados.keySet()){
						System.out.println(key);
					}
					System.out.println();
					break;
					
				case "cache":
					//*******************************************
	//				System.out.println("\nCurrently Cached Sites");
	//				for(String key : cache.keySet()){
	//					System.out.println(key);
	//				}
	//				System.out.println();
					//*******************************************
					break;
					
				case "sair":
					ativo = false;
					FecharServidor();
					break;
					
				default:
					sitesBloqueados.put(entrada, entrada);
					salvar();
					System.out.println("\n" + entrada + " Site bloqueado com sucesso \n");
			} 
		}
		scanner.close();
	}
	
	/**
	 * Cria o servidor proxy
	 * @param porta o número da porta em que o servidor proxy ira rodar.
	 */
	@SuppressWarnings("unchecked")
	public Proxy(int porta) {

		//*******************************************
		// Load in hash map containing previously cached sites and blocked Sites
		//cache = new HashMap<>();
		//*******************************************
		
		//Instancia uma hashMap na variavel sitesBloqueados
		sitesBloqueados = new HashMap<>();

		//Intacia um ArrayList na variavel arrayThereads
		arrayThreads = new ArrayList<>();

		//Inicia o gerenciador dinamico em um encadeamento separado
		new Thread(this).start();

		try{
			// Load in cached sites from file
			File cachedSites = new File("cachedSites.txt");
			if(!cachedSites.exists()){
				System.out.println("No cached sites found - creating new file");
				cachedSites.createNewFile();
			} else {
				FileInputStream fileInputStream = new FileInputStream(cachedSites);
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
				cache = (HashMap<String,File>)objectInputStream.readObject();
				fileInputStream.close();
				objectInputStream.close();
			}
			/**
			 * Escreve o site loqueado no aquivo sitesBloqueados.txt
			 * Caso não encontre o aquico ele cria um novo para armazenamento
			 */
			
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
			e.printStackTrace();
		}

		try {
			//Cria o socket do servidor para o proxy na porta requerida
			socketServidor = new ServerSocket(porta);
			System.out.println("Esperando a conexão na porta:" + socketServidor.getLocalPort());
			ativo = true;
		} 

		//Excecoes associadas ao soquete de abertuda
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
	 * Para cada requisicao para conexao ao sockt é criado uma nova thead, 
	 * otendo conexao com o cliente
	 */
	public void listen(){
		while(ativo){
			try {
				//É bloqueado ate que uma nova conecao seja estabelecida
				Socket socket = socketServidor.accept();
				
				//Cria uma nova thread
				Thread thread = new Thread(new Handler(socket));
				arrayThreads.add(thread);
				thread.start();	
				
				//Caso gere uma excecao do socket, o servidor e encerrado
			} catch (SocketException e) {
				System.out.println("Servidor fechado");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Salva o sites bloqueado e armazenam em cache em um arquivo.
	 */
	private void salvar() {
		try{
			FileOutputStream fileOutputStream2 = new FileOutputStream("sitesBloqueados.txt");
			ObjectOutputStream objectOutputStream2 = new ObjectOutputStream(fileOutputStream2);
			objectOutputStream2.writeObject(sitesBloqueados);
			objectOutputStream2.close();
			fileOutputStream2.close();
			System.out.println("Site bloqueado foi salvo");

			} catch (IOException e) {
				System.out.println("Erro ao salvar o site");
				e.printStackTrace();
			}
	}
	
	/**
	 * Looks for File in cache
	 * @param url of requested file 
	 * @return File if file is cached, null otherwise
	 */
	public static File getCachedPage(String url){
		return cache.get(url);
	}
	
	/**
	 * Adds a new page to the cache
	 * @param urlString URL of webpage to cache 
	 * @param fileToCache File Object pointing to File put in cache
	 */
	public static void addCachedPage(String urlString, File fileToCache){
		cache.put(urlString, fileToCache);
	}
	
	/**
	 *Encerra o sevidor, fecha as theads e o server socket
	 */
	private void FecharServidor(){
		ativo = false;
		
		try{
			//Fechando threads
			for(Thread thread : arrayThreads){
				if(thread.isAlive()){
					System.out.print("Aguardando a thread: "+  thread.getId()+" fechar...");
					thread.join();
					System.out.println("Thread fechada");
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//Fechando Server Socket
		try{
			System.out.println("Finalizando a conexão");
			socketServidor.close();
		} catch (Exception e) {
			System.out.println("Houve um problema com o fechamento do server socket");
			e.printStackTrace();
		}

	}
	
	/**
	 * Verifica se a url requirida está bloqueada pelo proxy
	 * @param url é a url para verificacao
	 * @return true caso exista a url loqueada e false caso nao exista
	 */
	public static boolean Bloqueado (String url){
		if(sitesBloqueados.get(url) != null){
			return true;
		} else {
			return false;
		}
	}
}
