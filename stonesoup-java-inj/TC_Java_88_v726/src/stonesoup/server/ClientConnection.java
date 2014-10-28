

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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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

	public ClientConnection(){}

	Socket clientSocket;
	DbConnector connector;

	protected static final String PARAM_OPERATION 		= "op",
								PARAM_OPERATION_UPDATE  = "update",
								PARAM_OPERATION_INSERT  = "insert",
								PARAM_OPERATION_DELETE  = "delete",
								PARAM_VALUE 			= "value",
								PARAM_ID                = "id",
								PARAM_AUTHORIZED		= "authorized",
								PARAM_PSWD				= "pswd";

	protected static final int HTTP_OPERATION  = 0,
							 HTTP_QUERY      = 1,
							 HTTP_VERSION    = 2;

	protected static final String pswd = "what";


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

		} finally {
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
	protected Map<String, String> parseHttpQuery( final String query )
	{
		Map<String, String> mapQuery = new HashMap<String,String>();

		if(query != null)
		{
			int paramStart = query.indexOf("?");
			if(paramStart != -1 && ((paramStart + 1) < query.length()) )
			{
				String params = query.substring(paramStart + 1);
				String[] arrayParam = params.split("&");
				int length = arrayParam.length;
				String[] newArrayParams = new String[length];
				char char_index = '0';
				int index = Integer.parseInt(char_index +"");
				newArrayParams = this.recursiveFunction(index, newArrayParams, arrayParam);
				BufferedWriter writer = null;
				BufferedReader reader = null;
				File file = new File("fileData");
				try{
					writer = new BufferedWriter(new FileWriter(file));
				}
				catch (IOException ioe)
				{
					System.err.println(ioe.getMessage());
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

					try
					{
						for(int z=0;z<keyValue.length;z++){	//STONESOUP:CONTROL_FLOW:LOOP_COMPLEXITY_TEST
							key = URLDecoder.decode(keyValue[z++] , "UTF8");
							value = (keyValue.length > 1) ? URLDecoder.decode(keyValue[z], "UTF8") : "";//STONESOUP:INTERACTION_POINT
							writer.write(key +"="+ value +"\n");
							writer.flush();
						}
					}
					catch (UnsupportedEncodingException e)
					{
						System.err.println(e.getMessage());
					}
					catch (IOException ioe)
					{
						System.err.println(ioe.getMessage());
					}
				}

				try
				{
					reader = new BufferedReader(new FileReader(file));
					String lineValue = null;
					while((lineValue = reader.readLine()) != null)	//STONESOUP:SOURCE_TAINT:FILE_CONTENTS
					{
						String[] tempValue = lineValue.split("=");
						int arrayLength = 2;
						String[] keyValue = new String[arrayLength];		//STONESOUP:DATA_TYPE:ARRAY_LENGTH_VARIABLE

						if(tempValue.length >= 2)
						{
							Integer num = new Integer(0);
							Integer num1 = num;
							Integer num2 = num1;

							keyValue[returnIndex(0)] = tempValue[num2.intValue()];	//STONESOUP:DATA_FLOW:INDEX_ALIAS_2
							keyValue[returnIndex(1)] = tempValue[1];
							String key = keyValue[returnIndex(0)];
							String value = keyValue[returnIndex(1)];
							mapQuery.put(key, value);	//STONESOUP:CROSSOVER_POINT
						}
					}
					if(reader != null)
					{
						reader.close();
					}
					if(writer != null)
					{
						writer.close();
					}
					if(file.exists())
					{
						file.delete();
					}

				}
				catch (IOException ioe)
				{
					System.err.println(ioe.getMessage());
				}
			}
			else
			{
				System.err.println("!! received a request without parameters");
			}
		}

		return mapQuery;
	}

	public String[] recursiveFunction(int index, String[] newArrayParams, String[] arrayParam)
	{
		newArrayParams[index] = arrayParam[index];
		index++;
		if(index < arrayParam.length)
		{
			this.recursiveFunction(index, newArrayParams, arrayParam);
		}

		return newArrayParams;
	}

	public int returnIndex(int index)
	{
		return index;
	}


	/**
	 * Sends an HTTP response to the client
	 * @param writer
	 * @param response
	 */
	protected void writeHttpResponse(BufferedWriter writer, String response )
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
	protected void handleHttpRequest(BufferedReader reader, BufferedWriter writer)
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
					DoQuery qryClass = new DoQuery(this.connector);
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

	protected String doQuery(final Map<String, String> mapParams)
	{
		String randString = "Don't want this. String values of mapParams: " +mapParams.toString();
		return randString;
	}
}
