import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

public class ClientToServerHttpsTransmit implements Runnable{

	InputStream proxyToClientIS;
	OutputStream proxyToServerOS;
	
	/**
	 * Creates Object to Listen to Client and Transmit that data to the server
	 * @param proxyToClientIS Stream that proxy uses to receive data from client
	 * @param proxyToServerOS Stream that proxy uses to transmit data to remote server
	 */
	public ClientToServerHttpsTransmit(InputStream proxyToClientIS, OutputStream proxyToServerOS) {
		this.proxyToClientIS = proxyToClientIS;
		this.proxyToServerOS = proxyToServerOS;
	}

	@Override
	public void run(){
		try {
			// Read byte by byte from client and send directly to server
			byte[] buffer = new byte[4096];
			int read;
			do {
				read = proxyToClientIS.read(buffer);
				if (read > 0) {
					proxyToServerOS.write(buffer, 0, read);
					if (proxyToClientIS.available() < 1) {
						proxyToServerOS.flush();
					}
				}
			} while (read >= 0);
		}
		catch (SocketTimeoutException ste) {
			// TODO: handle exception
		}
		catch (IOException e) {
			System.out.println("Proxy to client HTTPS read timed out");
			e.printStackTrace();
		}
	}
	
}
