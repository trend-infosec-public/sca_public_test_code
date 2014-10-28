

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package stonesoup.server;
import java.net.ServerSocket;
import java.net.Socket;
import stonesoup.database.DbConnector;

public class Server 
{
	private String dbUser, dbPass, dbHost, dbName;
	private int serverPort;
    
	private DbConnector connector = null;
	
	/**
	 * Initializes the connection to the database
	 * @throws Exception
	 */
	public void initDb() throws Exception 
	{
		System.err.println("initDb start");

		this.dbHost = System.getenv("DBMYSQLHOST");
		this.dbName = "stonesoup-crud";		
		this.dbUser = "sstcdbuser"; 
		this.dbPass = "testcaseuser";
		this.serverPort = 9080;
		//this.dbName = "TC_JAVA_88";		
		//this.dbUser = "TC_JAVA_88_640"; 
		//this.dbPass = "P$SSW0RD";
		//this.serverPort = 3306;
		
		if(this.connector == null)
		{			
			this.connector = new DbConnector(dbHost, dbName, dbUser, dbPass);
			connector.init();
		} 
		else 
		{
			System.err.println("db connector already initialized");
			System.exit(1);
		}
		
		System.err.println("initDb finish");
	}

	/**
	 * Creates a socket, bound to 0.0.0.0 on an established port. The socket
	 * waits for incoming connections and passes off inbound HTTP requests
	 * to a HTTP Request Handler function
	 * @throws Exception
	 */
	public void listen() throws Exception 
	{
		System.err.println("listen start");

		ServerSocket ss = new ServerSocket(serverPort);
		
		System.err.println("Server listening: " + ss.toString());

		while(true)
		{
			Socket clientSocket = ss.accept();
			Thread thread = new Thread(new ClientConnection(clientSocket, connector));
			thread.start();
		}

		//System.err.println("listen finish");
	}
	
}
