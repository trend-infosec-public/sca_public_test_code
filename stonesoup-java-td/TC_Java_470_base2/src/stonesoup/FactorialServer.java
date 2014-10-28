

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/***********************************************************************
**
**
**  
**  Date: 18 Mar 2012
**
**  Revision History
**  Date      	Change
**  18 Mar 2012 Initial Creation
**
**
**  This is a client/server application that calculates and returns factorials 
**  for the value input.  The server listens on a socket on port 1111 for 
**  client connections and allows one input:
**
**    * One of 'one' - 'ten' to indicate the number to run the factorial for
**
**  The argument corresponds to a method for each number that returns the "real" 
**  number using reflection.  Normal execution of the client results in a 
**  command line output. 
**
**  Error: The server has a package-private method called exit() that writes 
**  the exit status out to a file.  This method should only be called on error
**  or some other expected condition.  If a client uses the argument 'exit', 
**  however, reflection will successfully call the exit method, resulting in a 
**  server crash, as evidenced by the 'exit-status' file created. 
**
**
**  STONESOUP Weakness Class: Tainted data
**  CWE ID: CWE-470
**  Variant Spreadsheet Rev #: ###
**  Variant Spreadsheet ID: ???
**
**  Variant Features:
**
**
**  I/0 Pairs:
**     ??
**
**  How program works:
**     The server code must be run like: 
** 			java stonesoup.FactorialServer
**     Then one or more clients can be run like:
**			java stonesoup.FactorialClient <arg>
**  
** 		where arg is one of 'one' - 'ten' 
************************************************************************/
package stonesoup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

public class FactorialServer {
	
	private final static Map<Worker, Boolean> workers = new HashMap<Worker, Boolean>();
	private static boolean run = true;
	
	private int status = 2;  //exit status, possible values: 1 - expected, 2 - unexpected, 3 - error
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final FactorialServer server = new FactorialServer();
		server.runMain();
	}
	
	public int one() { return 1; }
	public int two() { return 2; }
	public int three() { return 3; }
	public int four() { return 4; }
	public int five() { return 5; }
	public int six() { return 6; }
	public int seven() { return 7; }
	public int eight() { return 8; }
	public int nine() { return 9; }
	public int ten() { return 10; }
	
	public long factorial( int n ) {
        if( n <= 1 ) {
            return 1;
        } else {
            return n * factorial( n - 1 );
        }
    }
	
	public void runMain() {
		//kick off several worker threads
		for (int i = 0; i < 5; i++) {
			final Worker worker = new Worker(this);
			workers.put(worker, Boolean.TRUE);
			worker.start();
		}
		
		//create a server socket
		ServerSocket socket = null;
		try {
			System.out.println("Running socket test server");
			socket = new ServerSocket(1111);
		} catch (final IOException ioe) {
			ioe.printStackTrace();
			status = 3;
			exit();
		}
		
		while(run) {
			Socket clientSocket = null;
			try {
				socket.setSoTimeout(500);
			    clientSocket = socket.accept();
			    //every time we find a client connection, grab a thread and execute
			    boolean foundOne = false;
			    for (final Worker worker : workers.keySet()) {
			    	if (workers.get(worker) && !foundOne) {
			    		workers.put(worker, Boolean.FALSE);
			    		//use this worker
			    		foundOne = true;
			    		worker.setSocket(clientSocket);
			    	}
			    }
			    if (!foundOne) {
			    	new PrintWriter(clientSocket.getOutputStream(), true).println("No workers available, sorry.");
			    }
			} catch (final SocketTimeoutException soe) {
				; //expected, keep going
			} catch (final IOException e) {
			    System.out.println("Accept failed: 1111");
			    status = 3;
			    exit();
			}
		}
		
	}

	int exit() { 
		run = false;
		
		//write exit status to a file
		FileWriter writer = null;
		BufferedWriter buf = null;
		try {
			writer = new FileWriter("exit-status");
			buf = new BufferedWriter(writer);
			buf.write("Application exit with code: " + status);
		} catch (Exception e) {
			System.err.println("Unable to write exit file");
		} finally {
			try {
				buf.flush();
				buf.close();
				writer.close();
			} catch (Exception e) {
				;
			}
		}
		
		return 1;
	}
	
	@Override
	public int hashCode() {
		return -1;
	}
	
	class Worker extends Thread {
		Socket socket;
		final FactorialServer server;
		
		Worker(final FactorialServer server) {
			this.server = server;
		}
		
		void setSocket(final Socket socket) {
			this.socket = socket;
		}
		
		@Override
		public void run() {
			System.out.println("Starting worker thread " + this.getId());
			while (run) {
				try {
					Thread.sleep(1 * 1000);
					if (socket != null) {
						doWork();
						socket = null;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Stopping worker thread " + this.getId());
		}
		
		private void doWork() {
			try {
				//for each connection, grab the input, use it to use reflection
				//to call a method by that name.  Expected values are one|two|...ten.
				//unknown or invalid methods print a message and return a 'bad input' message 
				//to the client. 
				System.out.println("Running worker thread " + this.getId());
				final PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				final BufferedReader in = 
				    new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String inputLine;
				
				while ((inputLine = in.readLine()) != null) { 
					final String methodName = inputLine;  // STONESOUP:INTERACTION_POINT
					try {
						//here is the reflection.  we're assuming that the name supplied
						//by the input is a method within the FactorialCalcultor class
						final Method method = server.getClass().getDeclaredMethod(methodName, null);  // STONESOUP:CROSSOVER_POINT
						final int value = ((Integer)method.invoke(server, null)).intValue();  //STONESOUP:TRIGGER_POINT
						final long factorialValue = server.factorial(value);  
						out.println(factorialValue);
					} catch (Exception e) {
						e.printStackTrace();
						out.println("bad input");
					}
					workers.put(this, Boolean.TRUE);
					
				    break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
