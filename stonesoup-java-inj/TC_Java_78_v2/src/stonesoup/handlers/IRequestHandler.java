/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.handlers;

import stonesoup.http.HttpRequest;
import stonesoup.http.HttpResponse;

/**
 * Defines a standard interface for all HTTP request handlers.
 * 
 * 
 *
 */
public interface IRequestHandler {
	
	/**
	 * Checks to see if the implementation of an HTTP should handle the
	 * request provided by the client.
	 * 
	 * @param request {@link stonesoup.http.HttpRequest HttpRequest}
	 * @return True if this request should be handled by this handler.
	 */
	boolean shouldHandleTarget(HttpRequest request);
	
	/**
	 * Processes the HttpRequest and creates an HttpResponse.
	 * 
	 * @param request {@link stonesoup.http.HttpRequest HttpRequest}
	 * @return {@link stonesoup.http.HttpResponse HttpResponse}
	 * @throws Exception Thrown in the case of an unhandled exception.
	 */
	HttpResponse handleRequest(HttpRequest request) throws Exception;
	
}
