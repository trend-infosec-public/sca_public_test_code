/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.handlers;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import stonesoup.Configuration;
import stonesoup.http.HttpContentTypes;
import stonesoup.http.HttpRequest;
import stonesoup.http.HttpRequestMethod;
import stonesoup.http.HttpResponse;
import stonesoup.http.HttpResponseCode;
import stonesoup.http.HttpResponseHeaders;
import stonesoup.utils.UrlUtilities;

/**
 * Provides a handler to redirect a user to a new page based on a absolute
 * or relative URL.  Absolute URLs are restricted to the hostname or ip address
 * to limited ability to launch open redirect attacks.
 *
 * 
 *
 */
public class ActionRedirectHandler implements IRequestHandler {

	private boolean decodeUrl = false;

	public ActionRedirectHandler() {
		this.decodeUrl = Boolean.parseBoolean(Configuration.Instance.getProperty("ActionRedirectHandler.decodeUrl", "false"));
	}

	@Override
	public boolean shouldHandleTarget(HttpRequest request) {
		if (request.getHeaders().getTarget().equalsIgnoreCase("/redirect"))
			return true;

		return false;
	}

	@Override
	public HttpResponse handleRequest(HttpRequest request) throws Exception {
		List<String> rawTargets = null;
		String target = null;

		if (request.getHeaders().getMethod() == HttpRequestMethod.GET) {
			if (request.getQueryString().containsKey("url")) {
				rawTargets = request.getQueryStringByName("url");
			}
		}

		if (rawTargets == null) {
			return this.handleBadRequest("Missing target \"url\" parameter.", request);
		} else if (rawTargets.size() > 1) {
			return this.handleBadRequest("Received too many \"url\" parameters.", request);
		}

		target = rawTargets.get(0);

		//check for safe path
		// - Encoded URLs can subvert absolute redirect checks.
		if (!UrlUtilities.isUrlLocal(target)) {
			return this.handleForbidden(target, request);
		}

		if (this.decodeUrl) {
			try {
				target = URLDecoder.decode(target, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new Exception("Mailformed target url.");
			}
		}

		//file is accessible
		HttpResponse response = new HttpResponse();
		response.getHeaders().setCode(HttpResponseCode.FOUND_302);

		response.getHeaders().addHeader(HttpResponseHeaders.LOCATION, target);

		return response;
	}

	private HttpResponse handleForbidden(String target, HttpRequest request) {
		HttpResponse response = new HttpResponse();
		response.getHeaders().setCode(HttpResponseCode.FORBIDDEN_403);

		//build the response page
		StringBuilder buffer = new StringBuilder();
		buffer.append("<html>\n<head>\n<title>403 - Forbidden</title>\n</head>\n<body>\n");
		buffer.append("<h3>403 - FORBIDDEN</h3>\n");
		buffer.append("<p><strong>URL: </strong>");
		buffer.append(target);
		buffer.append("</p>\n</body>\n</html>");

		response.setContent(buffer.toString(), HttpContentTypes.TEXT_HTML);

		return response;
	}

	private HttpResponse handleBadRequest(String reason, HttpRequest request) {
		HttpResponse response = new HttpResponse();
		response.getHeaders().setCode(HttpResponseCode.BAD_REQUEST_400);

		//build the response page
		StringBuilder buffer = new StringBuilder();
		buffer.append("<html>\n<head>\n<title>400 - Bad Request</title>\n</head>\n<body>\n");
		buffer.append("<h3>400 - BAD REQUEST</h3>\n");
		buffer.append("<p><strong>Reason: </strong>");
		buffer.append(reason);
		buffer.append("</p>\n</body>\n</html>");

		response.setContent(buffer.toString(), HttpContentTypes.TEXT_HTML);

		return response;
	}
}
