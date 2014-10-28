/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.handlers;

import java.util.List;

import stonesoup.Configuration;
import stonesoup.http.HttpContentTypes;
import stonesoup.http.HttpRequest;
import stonesoup.http.HttpRequestMethod;
import stonesoup.http.HttpResponse;
import stonesoup.http.HttpResponseCode;
import stonesoup.utils.Shell;

/**
 * Provides a handler to execute cgi-bin executables as subprocesses, and display
 * the results.
 * 
 * 
 *
 */
public class ActionUserHandler implements IRequestHandler {

	private String usersDir = "users/";
	private String linuxListDirCmd = "ls -1";
	private String windowsListDirCmd = "dir /B /O:N";
	private String listDirCmd = null;
	private String osName = System.getProperty("os.name");
	
	public ActionUserHandler() {
		this.usersDir = Configuration.Instance.getProperty("ActionUserHandler.usersDirectory", "users/");
		
		if (this.osName.contains("Windows")) {
			this.listDirCmd = this.windowsListDirCmd;
		} else {
			this.listDirCmd = this.linuxListDirCmd;
		}
	}
	
	@Override
	public boolean shouldHandleTarget(HttpRequest request) {
		if (request.getHeaders().getTarget().toLowerCase().startsWith("/user"))
			return true;
		
		return false;
	}

	@Override
	public HttpResponse handleRequest(HttpRequest request) throws Exception {
		String[] targetAndAction = request.getHeaders().getTarget().toLowerCase().substring(1).split("/");
		
		if (targetAndAction.length != 2) {
			return this.handleBadRequest("User handler was not provided an action to perform.", request);
		}
		
		String action = targetAndAction[1];
		if (action.toLowerCase().equals("listfiles")) {
			return this.handleListFilesAction(request);
		}
		
		return this.handleBadRequest("User action not implemented.", request);
	}
	
	@SuppressWarnings("unused")
	private HttpResponse handleForbidden(String target, HttpRequest request) {
		HttpResponse response = new HttpResponse();
		response.getHeaders().setCode(HttpResponseCode.FORBIDDEN_403);
		
		//build the response page
		StringBuilder buffer = new StringBuilder();
		buffer.append("<html>\n<head>\n<title>403 - Forbidden</title>\n</head>\n<body>\n");
		buffer.append("<h3>403 - FORBIDDEN</h3>\n");
		buffer.append("<p><strong>Report: </strong>");
		buffer.append(target);
		buffer.append("</p>\n</body>\n</html>");
		
		response.setContent(buffer.toString(), HttpContentTypes.TEXT_HTML);
		
		return response;
	}
	
	private HttpResponse handleListFilesAction(HttpRequest request) throws Exception {
		List<String> usernames = null;
		
		if (request.getHeaders().getMethod() == HttpRequestMethod.GET) {
			if (request.getQueryString().containsKey("username")) {
				usernames = request.getQueryStringByName("username");
			}
		} else if (request.getHeaders().getMethod() == HttpRequestMethod.POST) {
			if (request.getForm().containsKey("username")) {
				usernames = request.getFormByName("username");
			}
		}
		
		if (usernames == null) {
			return this.handleBadRequest("Missing target \"username\" parameter(s).", request);
		}
		
		String username = usernames.get(0);
		
		String command = this.listDirCmd + " " + this.usersDir + username;
		
		Shell shell = new Shell();
		List<String> output = shell.execute(command);
		if (output == null) {
			throw new Exception("Unknown error occured while listing user files.");
		}
		
		String stdout = output.get(0);
		String stderr = output.get(1);
		String content = null;
		
		if (stderr.equals("")) {
			content = this.encodeHtml(username, stdout, false);
		} else {
			content = this.encodeHtml(username, stderr, true);
		}
		
		//build the response
		HttpResponse response = new HttpResponse();
		response.getHeaders().setCode(HttpResponseCode.OK_200);
		response.setContent(content, HttpContentTypes.TEXT_HTML);
		
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
	
	private String encodeHtml(String username, String processOutput, boolean isError) {
		String lineSeperator = System.getProperty("line.separator");
		
		StringBuilder html = new StringBuilder();
		html.append("<html>\n<head>\n<title>");
		html.append(username);
		html.append(" Files</title>\n</head>\n<body>\n");
		
		if (isError) {
			html.append("<h3 style=\"color:red;\">Command Failed</h3>\n");
		}
		html.append("<p style=\"font-family:monospace;font-size:12px;\">");
		html.append(processOutput.replace(lineSeperator, "<br>\n"));
		html.append("</p>\n</body>\n</html>");
		
		return html.toString();
	}
}
