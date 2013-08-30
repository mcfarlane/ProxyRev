import java.net.*;
import java.io.*;
import java.util.*;

class ProxyConnection extends Thread {

	
	Socket fromClient;
	String host;
	int port;
	long timeout;

	ProxyConnection(Socket s, String host, int port, long timeout) {
		fromClient = s;
		this.host = host;
		this.port = port;
		this.timeout = timeout;
	}

	public void run() {
		//carrega os dados do stream do cliente
		InputStream clientIn = null;
		OutputStream clientOut = null;
		//Carrega os dados do stream do servidor
		InputStream serverIn = null;
		OutputStream serverOut = null;
		Socket toServer = null;
		int r0 = -1, r1 = -1, ch = -1, i = -1;
		long time0 = new Date().getTime();
		long time1 = new Date().getTime();
		try {
			toServer = new Socket(host, port);
			ProxyReverse.display("open connection to:" + toServer + "(timeout="
					+ timeout + " ms)");
			clientIn = fromClient.getInputStream();
			clientOut = new BufferedOutputStream(fromClient.getOutputStream());
			serverIn = toServer.getInputStream();
			serverOut = new BufferedOutputStream(toServer.getOutputStream());
			while (r0 != 0 || r1 != 0 || (time1 - time0) <= timeout) {
				while ((r0 = clientIn.available()) > 0) {
					ProxyReverse.println("");
					ProxyReverse.println("<<<" + r0 + " bytes from client");
					ProxyReverse.display("");
					ProxyReverse.display("<<<" + r0 + " bytes from client");
					for (i = 0; i < r0; i++) {
						ch = clientIn.read();
						if (ch != -1) {
							serverOut.write(ch);
							ProxyReverse.print(ch);
						} else {
							ProxyReverse.display("client stream closed");
						}
					}
					time0 = new Date().getTime();
					serverOut.flush();
				}
				while ((r1 = serverIn.available()) > 0) {
					ProxyReverse.println("");
					ProxyReverse.println(">>>" + r1 + " bytes from server");
					ProxyReverse.display("");
					ProxyReverse.display(">>>" + r1 + " bytes from server");
					for (i = 0; i < r1; i++) {
						ch = serverIn.read();
						if (ch != -1) {
							clientOut.write(ch);
							ProxyReverse.print(ch);
						} else {
							ProxyReverse.display("server stream closed");
						}
					}
					time0 = new Date().getTime();
					clientOut.flush();
				}
				if (r0 == 0 && r1 == 0) {
					time1 = new Date().getTime();
					Thread.sleep(100);
					// Proxy.display("waiting:"+(time1-time0)+" ms");
				}
			}
		} catch (Throwable t) {
			ProxyReverse.display("i=" + i + " ch=" + ch);
			t.printStackTrace(System.err);
		} finally {
			try {
				clientIn.close();
				clientOut.close();
				serverIn.close();
				serverOut.close();
				fromClient.close();
				toServer.close();
				ProxyReverse.quit(time1 - time0);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
}

public class ProxyReverse {

	public static final String usageArgs = " <localport> <host> <port> <timeout_ms>";

	static int clientCount;

	public static synchronized void print(int i) {
		System.out.print((char) i);
	}

	public static synchronized void println(String s) {
		System.out.println(s);
	}

	public static synchronized void display(String s) {
		System.err.println(s);
	}

	public static synchronized void quit(long t) {
		display("...quit after waiting " + t + " ms");
		clientCount--;
	}

	public void run(int localport, String host, int port, long timeout) {
		try {
			ServerSocket sSocket = new ServerSocket(localport);
			while (true) {
				Socket cSocket = null;
				try {
					display("listening...");
					cSocket = sSocket.accept();
					if (cSocket != null) {
						clientCount++;
						display("accepted as #" + clientCount + ":" + cSocket);
						ProxyConnection c = new ProxyConnection(cSocket, host,
								port, timeout);
						c.run();
					}
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
				try {
					cSocket.close();
				} catch (Exception e) {
					// fall thru
				}
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	public static void main(String[] argv) {
		ProxyReverse self = new ProxyReverse();

		if (true) {
			int localport = 9090;
			String url = "localhost";
			int port = 8080;
			int timeout = 3000;
			try {
				timeout = 3000;
			} catch (Exception e) {
			}
			self.run(localport, url, port, timeout);
		} else {
			System.err.println("usage: java " + self.getClass().getName()
					+ usageArgs);
		}
	}

}// class

