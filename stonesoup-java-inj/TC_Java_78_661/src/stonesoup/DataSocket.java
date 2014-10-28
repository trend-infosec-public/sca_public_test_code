

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

// changed timeout, 20100111, 

/**
 * Handles creation of the data socket and all data transfer
 * for the data socket.
 */
package stonesoup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;


public class DataSocket {

	/** The server socket to listen for a connection on **/
	private ServerSocket serverSocket = null;

	/** The client's IP address **/
	private InetAddress clientIP = null;

	/** The data socket and its input and output streams **/
	private Socket dataSocket = null;
	private BufferedInputStream input = null;
	private BufferedOutputStream output = null;

	/** Lister that waits for incoming connections **/
	ConnectionListener connectionListener;


	/**
	 * Create a new DataSocket to respond to a certain client
	 * @param clientIP The IP address to respond to
	 */
	public DataSocket(InetAddress clientIP) {
		this.clientIP = clientIP;
		connectionListener = new ConnectionListener();
	}

	/**
	 * Initializes the data socket connection to the client by creating a
	 * ServerSocket to listen on and returns the port number
	 *
	 * @return whether a connection to the client could be established
	 */
	public int createConnection() {
		try {
			// create a server socket on any port available
			serverSocket = new ServerSocket(0);

			// wait for the client to connect on the new port
			connectionListener.start();

			return serverSocket.getLocalPort();

		} catch (IOException e) {
			System.err.println("[!] Unable to create new data socket:" + e.getMessage());
		}

		return -1;
	}

	/**
	 * Returns whether a connection to the client has been established
	 * @return if a data socket connection has been established
	 */
	public boolean connectionEstablished() {
		// if we are not connected yet, first attempt to wait for a connection
		// connectionListener will time out after 10 seconds of no activity
		if ((dataSocket == null) || (input == null) || (output == null)) {
			try {
				connectionListener.join();
			} catch (InterruptedException e) {
				System.err.println("[W] Unable to wait for data socket connection");
			}
		}

		return (dataSocket != null) && (input != null) && (output != null);
	}

	/**
	 * Sends a file over the data socket
	 *
	 * @param file The file to send
	 * @return Whether the transfer succeeded
	 */
	public boolean sendFile(File file) {
		// make sure we have a data connection
		if (!connectionEstablished())
			return false;

		// try and send the file
		try {
			FileHandler.sendFile(file, output);
		} catch(IOException e) {
			System.err.println("[!] Error sending file '" + file.getName() + "'");
			return false;
		}

		return true;
	}

	/**
	 * Gets a file over the data socket
	 *
	 * @param filename The filename to use when saving the file
	 * @return Whether the transfer succeeded
	 */
	public boolean getFile(String filename) {
		// make sure we have a data connection
		if (!connectionEstablished())
			return false;

		// try to receive and store the file
		try {
			FileHandler.createFile(filename, input);
		} catch(IOException e) {
			System.err.println("[!] Error getting file '" + filename + "'");
			return false;
		}

		return true;
	}

	/**
	 * Closes the data socket connection
	 */
	public void closeConnection() {
		if (input != null) {
			try {
				input.close();
			} catch (IOException e) {
				System.err.println("[!] Error closing input stream");
			}
		}

		if (output != null) {
			try {
				output.close();
			} catch (IOException e) {
				System.err.println("[!] Error closing output stream");
			}
		}

		if (dataSocket != null) {
			try {
				dataSocket.close();
			} catch (IOException e) {
				System.err.println("[!] Error closing data socket");
			}
		}
	}


	/**
	 * Listens for a client connection on the server socket and opens the data
	 * socket if the request is from the expected client
	 */
	private class ConnectionListener extends Thread {
		public void run() {
			try {
			    // set a 30 second timeout for the client to connect
			    //
			    try {
				serverSocket.setSoTimeout(30000);
			    } catch (SocketException e) {
				System.err.println("[W] Unable to set connection timeout");
			    }
			    
			    // wait for an incoming connection
			    dataSocket = serverSocket.accept();
			    
			    // make sure the original client is the one that connected
			    if (dataSocket != null &&
				!dataSocket.getInetAddress().equals(clientIP))
				{
				    System.out.println("[W] Another client attempted to" +
						       " connect to the data socket");
				    
				    dataSocket.close();
				    return;
				}
			    
			    // get the input and output streams of the connection
			    if (dataSocket != null) {
				input = new BufferedInputStream(dataSocket.getInputStream());
				output = new BufferedOutputStream(
								  dataSocket.getOutputStream());
			    }
			    
			    // close the server socket
			    serverSocket.close();
			    
			} catch (SocketTimeoutException e) {
			    System.out.println("[!] Client did not connect to data" +
					       " socket in time");
			} catch (IOException e) {
			    System.err.println("[!] Unable to listen for connection" +
					       " on data socket: " + e.getMessage());
			}
		}
	    
	}
}
