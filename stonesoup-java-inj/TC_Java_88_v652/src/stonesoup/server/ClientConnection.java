

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
	private static Map<String, String> mapQuery = null;


	public ClientConnection(Socket clientSocket, DbConnector connector) {
		this.clientSocket = clientSocket;
		this.connector = connector;
	}

	public void run() {
		InetAddress clientAddress = clientSocket.getInetAddress();
		System.err.println("** incoming connection from: " + clientAddress.toString());

		BufferedReader input = null;
		BufferedWriter output = null;

		try {
			input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

			handleHttpRequest(input, output);

		} catch (IOException ex) {

			System.err.println("!! client connection failed: " + ex.getMessage());

		}
		finally {
			if (input != null ) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (output != null ) {
				try {
					output.close();
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
	private Map<String, String> parseHttpQuery( final String query )
	{
		mapQuery = new HashMap<String,String>();

		if(query != null)
		{
			int paramStart = query.indexOf("?");
			System.err.println("ParamStart: "+paramStart);
			if(paramStart != -1 && ((paramStart + 1) < query.length()) )
			{
				String params = query.substring(paramStart + 1);
				String[] arrayParam = params.split("&");
				int length = arrayParam.length;
				String[] newArrayParams = new String[length];
				int index = 0;
				newArrayParams = this.recursiveFunction(index, newArrayParams, arrayParam);

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

					try
					{
						for(int z=0;z<keyValue.length;z++){
							key = URLDecoder.decode(keyValue[z++] , "UTF8");	//STONESOUP:DATA_FLOW:ARRAY_INDEX_CONSTANT
							value = (keyValue.length > 1) ? URLDecoder.decode(keyValue[z], "UTF8") : "";	//STONESOUP:INTERACTION_POINT
							mapQuery.put(key, value);	//STONESOUP:CROSSOVER_POINT
						}
					}
					catch (UnsupportedEncodingException e)
					{
						System.err.println(e.getMessage());
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

	private String[] recursiveFunction(int index, String[] newArrayParams, String[] arrayParam)	//STONESOUP:DATA_FLOW:PASS_BY_VALUE
	{
		newArrayParams[index] = arrayParam[index];
		index++;
		if(index < arrayParam.length)
		{
			this.recursiveFunction(index, newArrayParams, arrayParam);
		}

		return newArrayParams;
	}

	/**
	 * Sends an HTTP response to the client
	 * @param writer
	 * @param response
	 */
	private void writeHttpResponse(BufferedWriter writer, String response )
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
	public void handleHttpRequest(BufferedReader reader, BufferedWriter writer)
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
				String httpQuery = httpInput[HTTP_QUERY];

				Map<String, String> queryParams = this.parseHttpQuery(httpQuery);
				if(queryParams.isEmpty() == false)
				{
					Query qryClass = new Query(this.connector);
					String response = qryClass.doQuery(queryParams);
					this.writeHttpResponse(writer, response);
				}
			}
		}
		catch(IOException ex)
		{
			System.err.println("!! unable to parse input buffer" );
		}
	}


	public class Query
	{
		DbConnector connector = null;

		public Query(DbConnector _connector)
		{
			this.connector = _connector;
		}

		/**
		 * Passes the parsed command to the DbCrudHandler to execute against the database.
		 * @param mapParams
		 * @return
		 */
		private String doQuery(final Map<String,String> mapParams)
		{
			String operation = null, value = null, id = null, in_pswd = "";
			double authorized = 0;
			Object obj_id = null;

			if(mapParams.containsKey(PARAM_OPERATION)){
				operation = mapParams.get(PARAM_OPERATION);
			}
			if(mapParams.containsKey(PARAM_ID)){
				id = mapParams.get(PARAM_ID);
				try{
					obj_id = new Integer(id);
				}
				catch(NumberFormatException nfe)
				{
					System.err.println("Issues formating number");
				}
			}
			if(mapParams.containsKey(PARAM_VALUE)){
				value = mapParams.get(PARAM_VALUE);
			}
			if(mapParams.containsKey(PARAM_PSWD)){
				in_pswd = mapParams.get(PARAM_PSWD);
			}
			if(mapParams.containsKey(PARAM_AUTHORIZED)){
				if(Integer.parseInt(mapParams.get(PARAM_AUTHORIZED)) == 1){
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

					if(obj_id != null)
					{
						if(authorized != 1)
						{
							System.out.println("** not authorized to perform delete");
						}
						else //STONESOUP:CONTROL_FLOW:ELSE_CONDITIONAL
						{
							boolean success = crudHandler.doDelete(obj_id.toString());	//STONESOUP:TRIGGER_POINT //STONESOUP:DATA_TYPE:VOID_POINTER
							response = (success) ? "delete successful" : "delete unsuccessful";
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

}
