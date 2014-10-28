/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import stonesoup.Configuration;
import stonesoup.http.HttpContentTypes;
import stonesoup.http.HttpRequest;
import stonesoup.http.HttpRequestMethod;
import stonesoup.http.HttpResponse;
import stonesoup.http.HttpResponseCode;
import stonesoup.utils.FileUtilities;

/**
 * Provides a handler to log messages to a specified log file.
 *
 * 
 *
 */
public class ActionLogHandler implements IRequestHandler {

	private boolean decodeUrl = false;

	public ActionLogHandler() {
		this.decodeUrl = Boolean.parseBoolean(Configuration.Instance.getProperty("ActionLogHandler.decodeUrl", "false"));
	}

	@Override
	public boolean shouldHandleTarget(HttpRequest request) {
		if (request.getHeaders().getTarget().equalsIgnoreCase("/log"))
			return true;

		return false;
	}

	@Override
	public HttpResponse handleRequest(HttpRequest request) throws Exception {
		List<String> rawTargets = null;
		String target = null;
		List<String> lines = null;

		if (request.getHeaders().getMethod() == HttpRequestMethod.GET) {
			if (request.getQueryString().containsKey("log")) {
				rawTargets = request.getQueryStringByName("log");
			}
			if (request.getQueryString().containsKey("message")) {
				lines = request.getQueryStringByName("message");
			}
		} else if (request.getHeaders().getMethod() == HttpRequestMethod.POST) {
			if (request.getForm().containsKey("log")) {
				rawTargets = request.getFormByName("log");
			}
			if (request.getQueryString().containsKey("message")) {
				lines = request.getQueryStringByName("message");
			}
		}

		if (rawTargets == null) {
			return this.handleBadRequest("Missing target \"log\" parameter.", request);
		} else if (rawTargets.size() > 1) {
			return this.handleBadRequest("Received too many \"log\" parameters.", request);
		}

		target = rawTargets.get(0);

		//check for safe path
		if (!FileUtilities.isPathSafe(target)) {
			return this.handleForbidden(target, request);
		}

		if (this.decodeUrl) {
			try {
				target = URLDecoder.decode(target, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new Exception("Mailformed target url.");
			}
		}

		//try to open the file
		File targetFile = new File(target);
		if (targetFile.isFile()) {
			if (!targetFile.canWrite()) {
				return this.handleForbidden(target, request);
			}
		} else {
			if (!targetFile.createNewFile()) {
				return this.handleForbidden(target, request);
			}
		}

		//file is accessible, and writable
		BufferedWriter output = null;

		try {
			output = new BufferedWriter(new FileWriter(targetFile, true));

			for (String message : lines) {
				output.write(message);
				output.write("\n");
			}
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				//swallow, we are only trying to clean up
			}
		}

		HttpResponse response = new HttpResponse();
		response.getHeaders().setCode(HttpResponseCode.OK_200);
		response.setContent(this.buildSuccessContent(target, lines), HttpContentTypes.TEXT_HTML);
		return response;
	}

	private HttpResponse handleForbidden(String target, HttpRequest request) {
		HttpResponse response = new HttpResponse();
		response.getHeaders().setCode(HttpResponseCode.FORBIDDEN_403);

		//build the response page
		StringBuilder buffer = new StringBuilder();
		buffer.append("<html>\n<head>\n<title>403 - Forbidden</title>\n</head>\n<body>\n");
		buffer.append("<h3>403 - FORBIDDEN</h3>\n");
		buffer.append("<p><strong>File: </strong>");
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

	private String buildSuccessContent(String log, List<String> lines) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("<html>\n<head>\n<title>Logged Messages</title>\n</head>\n<body>\n");
		buffer.append("<h3>Logged Messages</h3>\n");
		buffer.append("<p><strong>Log File: </strong>");
		buffer.append(log);
		buffer.append("</p>\n");
		buffer.append("<h4>Messages Logged</h4>\n");
		for (String message : lines) {
			buffer.append("<p>");
			buffer.append(message);
			buffer.append("</p>\n");
		}
		buffer.append("</body>\n</html>");
		return buffer.toString();
	}
}
