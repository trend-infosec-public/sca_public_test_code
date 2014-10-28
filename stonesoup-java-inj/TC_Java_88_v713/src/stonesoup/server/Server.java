

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/


package stonesoup.server;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import stonesoup.database.DbConnector;

/**
 *
 * 
 */
public class Server 
{
	private String dbUser, dbPass, dbHost, dbName;
	private int serverPort;
    
	private DbConnector connector = null;
	
	/**
	 * Reads the configuration file for the database information
	 * @throws Exception
	 */
	public void readConfigFile() throws Exception
	{
		this.dbHost = System.getenv("DBMYSQLHOST");
		this.dbName = "stonesoup-crud";		
		this.dbUser = "sstcdbuser"; 
		this.dbPass = "testcaseuser";
		this.serverPort = 9080;

		/*
		Properties props = new Properties();
		InputStream configFile = new FileInputStream("testData/serverConfig.properties");
		props.load(configFile);
		this.dbHost = props.getProperty("DB_HOST");
		this.dbName = props.getProperty("DB_NAME");
		this.dbUser = props.getProperty("DB_USER");
		this.dbPass = props.getProperty("DB_PASS");
		this.serverPort = Integer.parseInt(props.getProperty("SERVER_PORT"));
		*/
	}
	
	/**
	 * Initializes the connection to the database
	 * @throws Exception
	 */
	public void initDb() throws Exception 
	{
		if(this.connector == null)
		{			
			this.connector = new DbConnector(dbHost, dbName, dbUser, dbPass);
			connector.init();
		} 
		else 
		{
			System.err.println("!! db connector already initialized");
		}
	}

	/**
	 * Creates a socket, bound to 0.0.0.0 on an established port. The socket
	 * waits for incoming connections and passes off inbound HTTP requests
	 * to a HTTP Request Handler function
	 * @throws Exception
	 */
	public void listen() throws Exception 
	{
		ServerSocket ss = new ServerSocket(serverPort);
		System.err.println("** server listening: " + ss.toString());

		while(true)
		{
			Socket clientSocket = ss.accept();
			
			Thread thread = new Thread(new ClientConnection(clientSocket, connector));
			thread.start();
		}
	}
	
}
