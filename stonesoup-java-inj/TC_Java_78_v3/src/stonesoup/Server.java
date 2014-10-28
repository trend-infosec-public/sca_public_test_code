/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import stonesoup.handlers.ActionBinHandler;
import stonesoup.handlers.ActionCgiBinHandler;
import stonesoup.handlers.ActionDownloadHandler;
import stonesoup.handlers.ActionLogHandler;
import stonesoup.handlers.ActionRedirectHandler;
import stonesoup.handlers.ActionUserHandler;
import stonesoup.handlers.ActionViewReportHandler;
import stonesoup.handlers.FileHandler;
import stonesoup.handlers.IRequestHandler;

/**
 * Provides an implementation of a simple HTTP Server.  This type handles 
 * creating a server socket, accepting incoming connections, and passing 
 * connection off to their own operating thread.  All connections run in a
 * thread pool, as such, new connection are accepted but only processing when
 * there is an available thread for execution.
 * 
 * This class also provides centralized mapping of implementations of request
 * handlers.  A request handler is a request processor that determines if it 
 * should process the given http request based on variables such as method, taregt,
 * etc.
 * 
 * 
 *
 */
public class Server {

	//Constants
	
	//Locals
	private ExecutorService pool = null;
	private ServerSocket socket = null;
	private int socketTimeout = 0;
	private int poolSize = 10;
	
	//Properties
	private static final List<IRequestHandler> _Handlers = new ArrayList<IRequestHandler>();
	public static final List<IRequestHandler> Handlers;
	
	static {
		//handlers are added here.  the list order determines order of precedence.
		//_Handlers.add(new ActionDownloadHandler());
		//_Handlers.add(new ActionRedirectHandler());
		//_Handlers.add(new ActionLogHandler());
		//_Handlers.add(new ActionViewReportHandler());
		//_Handlers.add(new ActionCgiBinHandler());
		_Handlers.add(new ActionUserHandler());
		_Handlers.add(new ActionBinHandler());
		_Handlers.add(new FileHandler());
		
		Handlers = Collections.unmodifiableList(_Handlers);
	}
	
	/**
	 * Creates a server instance bound to the specified address and port.  Optionally 
	 * uses SSL to secure communications.
	 * @param bindAddress Address to listen on.
	 * @param port Port to listen on.
	 * @param useSSL Secure communications with SSL.
	 * @throws Exception
	 */
	public Server(InetAddress bindAddress, int port, boolean useSSL) throws Exception {
		this.socket = this.createSocket(bindAddress, port, useSSL);
		this.socketTimeout = Integer.parseInt(Configuration.Instance.getProperty("Server.timeout", "0"));
		this.poolSize = Integer.parseInt(Configuration.Instance.getProperty("Server.poolSize", "10"));
		this.pool = Executors.newFixedThreadPool(this.poolSize);
	}
	
	/**
	 * Creates and opens a server socket bound to the specified address and
	 * port.  Optionally creates an SSL socket if requested.
	 * @param bindAddress Address on which to listen.
	 * @param port Port on which to listen.
	 * @param useSSL Create an SSL socket.
	 * @return ServerSocket
	 * @throws Exception
	 */
	private ServerSocket createSocket(InetAddress bindAddress, int port, boolean useSSL) throws Exception {
		InetSocketAddress socketAddress = new InetSocketAddress(bindAddress, port);
		ServerSocket socket = null;
		
		try {
		if (useSSL) {
			ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
			socket = factory.createServerSocket();
			SSLServerSocket sslSocket = (SSLServerSocket)socket;
			sslSocket.setEnabledCipherSuites(new String[] {"TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA"});
			sslSocket.setEnabledProtocols(new String[] {"SSLv3", "TLSv1"});
			sslSocket.setWantClientAuth(false);
			sslSocket.setReuseAddress(true);
		} else {
			socket = new ServerSocket();
		}
		
		socket.bind(socketAddress);
		} catch (Exception e) {
			System.out.printf("Failed to create a socket.  Error: %s\n", e.getMessage());
			throw e;
		}
		
		return socket;
	}
	
	/**
	 * Runs the server.
	 * @throws IOException 
	 */
	public void run() throws IOException {
		try {
			for (;;) {
				Socket connection = this.socket.accept();
				if (this.socketTimeout > 0)
					connection.setSoTimeout(1000 * this.socketTimeout);
				this.pool.execute(new Connection(connection));
			}
		} catch (IOException ioe) {
			throw ioe;
		} finally {
			this.pool.shutdown();
		}
	}
}
