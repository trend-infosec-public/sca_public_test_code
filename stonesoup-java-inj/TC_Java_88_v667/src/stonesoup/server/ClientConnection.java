

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package stonesoup.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import stonesoup.database.DbConnector;
import stonesoup.database.DbCrudHandler;

public class ClientConnection implements Runnable {

	Socket clientSocket;
	DbConnector connector;
	Map<String, String> mapQuery = null;
	String[] keyValue = null;
	String key = null, value = null, response = null, query = null;
	BufferedWriter writer = null;
	BufferedReader reader = null;

	private static final String PARAM_OPERATION 		= "op",
								PARAM_OPERATION_UPDATE  = "update",
								PARAM_OPERATION_INSERT  = "insert",
								PARAM_OPERATION_DELETE  = "delete",
								PARAM_VALUE 			= "value",
								PARAM_ID                = "id",
								PARAM_AUTHORIZED		= "authorized",
								PARAM_PSWD				= "pswd";

	private static final int HTTP_OPERATION  = 0,
							 HTTP_QUERY      = 1,
							 HTTP_VERSION    = 2;

	private static final String pswd = "what";


	public ClientConnection(Socket clientSocket, DbConnector connector) {
		this.clientSocket = clientSocket;
		this.connector = connector;
	}

	public void run() {
		InetAddress clientAddress = clientSocket.getInetAddress();
		System.err.println("** incoming connection from: " + clientAddress.toString());

		try {
			reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

			handleHttpRequest();

		} catch (IOException ex) {

			System.err.println("!! client connection failed: " + ex.getMessage());

		} finally {
			if (reader != null ) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (writer != null ) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (clientSocket != null ) {
				try {
					clientSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Pulls apart the query portion of an HTTP GET request string, storing param key-value pairs in a hash map
	 * Example query string: /?param_one=foo&param_two=bar
	 * @param query
	 * @return
	 * @throws Exception
	 */
	private Map<String, String> parseHttpQuery()	//STONESOUP:DATA_FLOW:SIMPLE
	{
		mapQuery = new HashMap<String,String>();

		if(query != null)
		{
			int paramStart = query.indexOf("?");
			if(paramStart != -1 && ((paramStart + 1) < query.length()) )
			{
				String params = query.substring(paramStart + 1);
				String[] arrayParam = params.split("&");
				int length = arrayParam.length;
				String[] newArrayParams = new String[(length*1)/1];	//STONESOUP:DATA_TYPE:ARRAY_LENGTH_LINEAR_EXPRESSION
				for(int i = 0; i < arrayParam.length; i++)
				{
					newArrayParams[i] = arrayParam[i];
				}

				for(String param : newArrayParams)
				{
					String[] keyValue = param.split("=");
					System.err.println("Input size: " + keyValue.length);
					for(int i = 0;keyValue.length==2&& i<keyValue.length;i++){;
						if(keyValue[i].equals("authorized")){
							keyValue[i+1]="0";
						}
					}
					for(int i = 0;i<keyValue.length;i++){
						System.err.println("Real: " + keyValue[i]);
					}
					String key = null;
					String value = null;

					for(int z=0;z<keyValue.length;z++){
						try
						{
							key = URLDecoder.decode(keyValue[z++] , "UTF8");
							value = (keyValue.length > 1) ? URLDecoder.decode(keyValue[z], "UTF8") : "";	//STONESOUP:INTERACTION_POINT
						}
						catch (UnsupportedEncodingException e)
						{
							System.err.println(e.getMessage());
						}
						finally{	//STONESOUP:CONTROL_FLOW:INTERRUPT_CONTINUE
							mapQuery.put(key, value);	//STONESOUP:CROSSOVER_POINT
						}
					}

				}
			}
			else
			{
				System.err.println("!! received a request without parameters");
			}
		}

		return mapQuery;
	}

	/**
	 * Sends an HTTP response to the client
	 * @param writer
	 * @param response
	 */
	private void writeHttpResponse()
	{
		String header = "HTTP/1.0 200 OK\r\n" +
		                "Content-Type: text/html\r\n" +
                        "\r\n";

		String fullResponse = header + response + "\r\n\r\n";

		try
		{
			writer.write(fullResponse);
			writer.flush();
		}
		catch(IOException ex)
		{
			System.err.println("!! unable to send http response to client");
		}
	}

	/**
	 * HTTP Request Handler: parses the incoming HTTP request and performs the appropriate response
	 * @param reader
	 * @param writer
	 */
	public void handleHttpRequest()
	{
		try
		{
			String    input = reader.readLine();
			String[]  httpInput = (input != null) ? input.split("\\s") : null;

			/* We only handle GET requests in this form:
			 * GET <http_query> HTTP/1.1
			 */
			if(httpInput != null && httpInput.length == 3)
			{
				query = httpInput[HTTP_QUERY];

				Map<String, String> queryParams = this.parseHttpQuery();
				if(queryParams.isEmpty() == false)
				{
					response = this.doQuery();
					this.writeHttpResponse();
				}
			}
		}
		catch(IOException ex)
		{
			System.err.println("!! unable to parse input buffer" );
		}
	}


	/**
	 * Passes the parsed command to the DbCrudHandler to execute against the database.
	 * @param mapParams
	 * @return
	 */
	private String doQuery()
	{
		String operation = null, value = null, id = null, in_pswd = "";
		double authorized = 0;

		if(mapQuery.containsKey(PARAM_OPERATION)){
			operation = mapQuery.get(PARAM_OPERATION);
		}
		if(mapQuery.containsKey(PARAM_ID)){
			id = mapQuery.get(PARAM_ID);
		}
		if(mapQuery.containsKey(PARAM_VALUE)){
			value = mapQuery.get(PARAM_VALUE);
		}
		if(mapQuery.containsKey(PARAM_PSWD)){
			in_pswd = mapQuery.get(PARAM_PSWD);
		}
		if(mapQuery.containsKey(PARAM_AUTHORIZED)){
			if(Integer.parseInt(mapQuery.get(PARAM_AUTHORIZED)) == 1){
				authorized = 1;
			}
		}

		String response = "";

		if(operation != null)
		{
			DbCrudHandler crudHandler = new DbCrudHandler(this.connector);

			if(operation.equals(PARAM_OPERATION_UPDATE))
			{
				if(id != null && value != null)
				{
					boolean success = crudHandler.doUpdate(id, value);
					response = (success) ? "update successful" : "update failed";
				}
				else
				{
					System.err.println("!! need an id and a value to perform an update operation" );
				}
			}
			else if(operation.equals(PARAM_OPERATION_INSERT))
			{
				if(value != null)
				{
					String newId = crudHandler.doInsert(value);
					response = (newId != null) ? "insert successful: new id: " + newId : "insert unsuccessful";
				}
				else
				{
					System.err.println("!! need a value to perform an insert operation");
				}
			}
			else if(operation.equals(PARAM_OPERATION_DELETE))
			{
				if(in_pswd.equals(pswd))
				{
					authorized = 1;
				}
				else
				{
					response = "Incorrect password";
				}

				if(id != null)
				{
					if(authorized == 1)
					{
						boolean success = crudHandler.doDelete(id);	//STONESOUP:TRIGGER_POINT)
						response = (success) ? "delete successful" : "delete unsuccessful";
					}
					else
					{
						System.out.println("** not authorized to perform delete");
					}
				}
				else
				{
					System.err.println("!! need an id to perform a delete operation");
				}
			}
			else
			{
				System.err.println("!! unrecognized operation specified by client. so confused.");
			}
		}
		else
		{
			System.err.println("!! no operation specified by client. so confused.");
		}

		return response;
	}
}
