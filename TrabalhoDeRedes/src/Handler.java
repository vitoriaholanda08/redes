import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.imageio.ImageIO;

public class Handler implements Runnable{
	
	/**
	 * Socket connected to client passed by Proxy server
	 */
	Socket socketCliente;

	/**
	 * Read data client sends to proxy
	 */
	BufferedReader proxyClientBr;

	/**
	 * Send data from proxy to client
	 */
	BufferedWriter proxyClientBw;
	

	/**
	 * Thread that is used to transmit data read from client to server when using HTTPS
	 * Reference to this is required so it can be closed once completed.
	 */
	private Thread threadClienteServidor;
	
	public Handler(Socket clienteSocket){
		this.socketCliente = clienteSocket;
		try{
			this.socketCliente.setSoTimeout(2000);
			proxyClientBr = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
			proxyClientBw = new BufferedWriter(new OutputStreamWriter(socketCliente.getOutputStream()));
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// Get Request from client
		String requestString;
		try{
			requestString = proxyClientBr.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Erro ao ler a solicitação do Cliente");
			return;
		}

		// Parse out URL

		//System.out.println("Requisição recebida: " + requestString);
		// Get the Request type
		String request = requestString.substring(0,requestString.indexOf(' '));

		// remove request type and space
		String urlString = requestString.substring(requestString.indexOf(' ')+1);

		// Remove everything past next space
		urlString = urlString.substring(0, urlString.indexOf(' '));

		// Prepend http:// if necessary to create correct URL
		if(!urlString.substring(0,4).equals("http")){
			String temp = "http://";
			urlString = temp + urlString;
		}


		// Check if site is blocked
		if(Proxy.Bloqueado(urlString)){
			System.out.println("Site bloqueado requisitado : " + urlString);
			siteBloqueadoRequisitado();
			return;
		}


		// Check request type
		if(request.equals("CONNECT")){
			//System.out.println("HTTPS Request for : " + urlString + "\n");
			RequisicaoHTTP(urlString);
		} 
	}
	
	/**
	 * Handles HTTPS requests between client and remote server
	 * @param urlString desired file to be transmitted over https
	 */
	private void RequisicaoHTTP(String urlString){
		// Extract the URL and port of remote 
		String url = urlString.substring(7);
		String pieces[] = url.split(":");
		url = pieces[0];
		int port  = Integer.valueOf(pieces[1]);

		try{
			// Only first line of HTTPS request has been read at this point (CONNECT *)
			// Read (and throw away) the rest of the initial data on the stream
			for(int i=0;i<5;i++){
				proxyClientBr.readLine();
			}

			// Get actual IP associated with this URL through DNS
			InetAddress address = InetAddress.getByName(url);
			
			// Open a socket to the remote server 
			Socket proxyToServerSocket = new Socket(address, port);
			proxyToServerSocket.setSoTimeout(5000);

			// Send Connection established to the client
			String line = "HTTP/1.0 200 Connection established\r\n" +
					"Proxy-Agent: ProxyServer/1.0\r\n" +
					"\r\n";
			proxyClientBw.write(line);
			proxyClientBw.flush();
			
			
			
			// Client and Remote will both start sending data to proxy at this point
			// Proxy needs to asynchronously read data from each party and send it to the other party


			//Create a Buffered Writer betwen proxy and remote
			BufferedWriter proxyToServerBW = new BufferedWriter(new OutputStreamWriter(proxyToServerSocket.getOutputStream()));

			// Create Buffered Reader from proxy and remote
			BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerSocket.getInputStream()));



			// Create a new thread to listen to client and transmit to server
			ClientToServerHttpsTransmit clientToServerHttps = 
					new ClientToServerHttpsTransmit(socketCliente.getInputStream(), proxyToServerSocket.getOutputStream());
			
			threadClienteServidor = new Thread(clientToServerHttps);
			threadClienteServidor.start();
			
			
			// Listen to remote server and relay to client
			try {
				byte[] buffer = new byte[4096];
				int read;
				do {
					read = proxyToServerSocket.getInputStream().read(buffer);
					if (read > 0) {
						socketCliente.getOutputStream().write(buffer, 0, read);
						if (proxyToServerSocket.getInputStream().available() < 1) {
							socketCliente.getOutputStream().flush();
						}
					}
				} while (read >= 0);
			}
			catch (SocketTimeoutException e) {
				
			}
			catch (IOException e) {
				e.printStackTrace();
			}


			// Close Down Resources
			if(proxyToServerSocket != null){
				proxyToServerSocket.close();
			}

			if(proxyToServerBR != null){
				proxyToServerBR.close();
			}

			if(proxyToServerBW != null){
				proxyToServerBW.close();
			}

			if(proxyClientBw != null){
				proxyClientBw.close();
			}
			
			
		} catch (SocketTimeoutException e) {
			String line = "HTTP/1.0 504 Timeout Occured after 10s\n" +
					"User-Agent: ProxyServer/1.0\n" +
					"\r\n";
			try{
				proxyClientBw.write(line);
				proxyClientBw.flush();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} 
		catch (Exception e){
			System.out.println("Error on HTTPS : " + urlString );
			e.printStackTrace();
		}
	}
	
	private void siteBloqueadoRequisitado(){
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socketCliente.getOutputStream()));
			String line = "HTTP/1.0 403 Access Forbidden \n" +
					"User-Agent: ProxyServer/1.0\n" +
					"\r\n";
			bufferedWriter.write(line);
			bufferedWriter.flush();
		} catch (IOException e) {
			System.out.println("Error writing to client when requested a blocked site");
			e.printStackTrace();
		}
	}

}
