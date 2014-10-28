/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

import stonesoup.handlers.IRequestHandler;
import stonesoup.http.HttpContentTypes;
import stonesoup.http.HttpRequest;
import stonesoup.http.HttpResponse;
import stonesoup.http.HttpResponseCode;

/**
 * Client connection to the server.  This implements the Runnable interface
 * so that it can be executed on it's own thread.
 * 
 * 
 *
 */
public class Connection implements Runnable {

	private Socket socket = null;
	
	/**
	 * Constructor accepting a socket connection.
	 * @param socket
	 */
	public Connection(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		//get the streams as buffered wrappers
		BufferedInputStream input = null;
		BufferedOutputStream output = null;
		HttpRequest request = null;
		HttpResponse response = null;
		
		try {
			try {
				input = new BufferedInputStream(this.socket.getInputStream());
				output = new BufferedOutputStream(this.socket.getOutputStream());
			} catch (IOException ioe) {
				response = this.handle500Error("An unknown network error occured.");
				throw ioe;
			}
		
			// 1. Read Request
			request = new HttpRequest();
			request.parseRequest(input);
			
			// 2. Find a handler for the request
			for (IRequestHandler handler : Server.Handlers) {
				if (handler.shouldHandleTarget(request)) {
					response = handler.handleRequest(request);
					break;
				}
			}
		} catch (Exception e) {
			//there was an error at some point during the operation
			if (response == null) {
				//build a response
				response = this.handle500Error("An error occured during processing.  " + e.getMessage());
			}
			System.out.printf("An error occured during processing.  Message: %s\n", e.getMessage());
			e.printStackTrace(System.out);
		} finally {
			try {
				if (output != null)
					response.writeResponse(output);
				else
					System.out.printf("Failed to send response.  Reason: %s\n", "Output stream failed to be wrapped.");
			} catch (IOException e) {
				//failed to even send a response
				System.out.printf("Failed to send response.  Reason: %s\n", e.getMessage());
				e.printStackTrace(System.out);
			}
			
			try {
				this.socket.close();
			} catch (IOException e) {
				//swallow this, we are just cleaning up
			}
		}
	}
	
	/**
	 * Builds and HTTP 500 (Internal Server Error) message.  This is sent in the
	 * event of an unhandled exception.
	 * @param message
	 * @return
	 */
	private HttpResponse handle500Error(String message) {
		
		HttpResponse response = new HttpResponse();
		response.getHeaders().setCode(HttpResponseCode.INTERNAL_SERVER_ERROR_500);
		
		StringBuilder buffer = new StringBuilder();
		buffer.append("<html>\n<head>\n<title>HTTP 500 - Internal Server Error</title>\n</head>\n<body>\n<h3>HTTP 500 - Internal Server Error</h3>\n<p><strong>Details: </strong>");
		buffer.append(message);
		buffer.append("</p>\n</body>\n</html>");
		
		response.setContent(buffer.toString(), HttpContentTypes.TEXT_HTML);
		
		return response;
	}

}
