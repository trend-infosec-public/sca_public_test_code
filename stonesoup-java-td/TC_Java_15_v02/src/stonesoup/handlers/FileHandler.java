/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.handlers;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import stonesoup.Configuration;
import stonesoup.http.HttpContentTypes;
import stonesoup.http.HttpRequest;
import stonesoup.http.HttpRequestMethod;
import stonesoup.http.HttpResponse;
import stonesoup.http.HttpResponseCode;
import stonesoup.utils.AccessControlUtilities;
import stonesoup.utils.FileUtilities;

/**
 * Provides a handler to serve static files for GET requests.
 *
 * 
 *
 */
public class FileHandler implements IRequestHandler {

	private boolean decodeUrl = false;

	public FileHandler() {
		this.decodeUrl = Boolean.parseBoolean(Configuration.Instance.getProperty("FileHandler.decodeUrl", "false"));
	}

	@Override
	public boolean shouldHandleTarget(HttpRequest request) {
		if (request.getHeaders().getMethod() == HttpRequestMethod.GET)
			return true;

		return false;
	}

	@Override
	public HttpResponse handleRequest(HttpRequest request) throws Exception {
		String target = request.getHeaders().getTarget();

		//delete leading slashes so this is not an absolute path
		if (target.startsWith("/"))
			target = target.substring(1);

		//check for safe path
		// - path passes checks based on URL encoding
		if (!FileUtilities.isPathSafe(target)) {
			return this.handleForbidden(target, request);
		}

		// Based on config, can cause a single or double encoded URL to pass checks.
		if (this.decodeUrl) {
			try {
				target = URLDecoder.decode(target, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new Exception("Mailformed target url.");
			}
		}

		//try to open the file
		File targetFile = new File(target);
		if (!targetFile.isFile()) {
			return this.handleFileNotFound(target, request);
		}

		if (!targetFile.canRead()) {
			return this.handleForbidden(target, request);
		}

		if (!AccessControlUtilities.hasReadAccess(target)) {
			return this.handleForbidden(target, request);
		}

		//file is accessible
		HttpResponse response = new HttpResponse();
		response.getHeaders().setCode(HttpResponseCode.OK_200);
		String[] filenameAndExt = targetFile.getName().split("\\.", 2);
		String extension = "";
		switch (filenameAndExt.length) {
			case 1:
				extension = filenameAndExt[0];
				break;
			case 2:
				extension = filenameAndExt[1];
				break;
		}
		// - File is read
		response.setContent(FileUtilities.readFileFully(targetFile), HttpContentTypes.getContentTypeFromFileExtension(extension));

		return response;
	}

	private HttpResponse handleFileNotFound(String target, HttpRequest request) {
		HttpResponse response = new HttpResponse();
		response.getHeaders().setCode(HttpResponseCode.NOT_FOUND_404);

		//build the response page
		StringBuilder buffer = new StringBuilder();
		buffer.append("<html>\n<head>\n<title>404 - Not Found</title>\n</head>\n<body>\n");
		buffer.append("<h3>404 - NOT FOUND</h3>\n");
		buffer.append("<p><strong>File: </strong>");
		buffer.append(target);
		buffer.append("</p>\n</body>\n</html>");

		response.setContent(buffer.toString(), HttpContentTypes.TEXT_HTML);

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
}
