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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import stonesoup.Configuration;
import stonesoup.http.HttpContentTypes;
import stonesoup.http.HttpRequest;
import stonesoup.http.HttpRequestMethod;
import stonesoup.http.HttpResponse;
import stonesoup.http.HttpResponseCode;
import stonesoup.utils.Shell;

/**
 * Provides a handler to execute OS comamnds as subprocesses, and display
 * the results.  Commands are limited and metacharacters are prohibited.
 * 
 * 
 *
 */
public class ActionBinHandler implements IRequestHandler {
	
	private boolean decodeUrl = false;
	private List<String> linuxAllowedCmds = new ArrayList<String>();
	private List<String> windowsAllowedCmds = new ArrayList<String>();
	private List<String> linuxMetachars = new ArrayList<String>();
	private List<String> windowsMetachars = new ArrayList<String>();
	private List<String> allowedCmds = null;
	private List<String> metachars = null;
	private String osName = System.getProperty("os.name");
	
	public ActionBinHandler() {
		this.decodeUrl = Boolean.parseBoolean(Configuration.Instance.getProperty("ActionBinHandler.decodeUrl", "false"));
		this.windowsAllowedCmds = parseList(Configuration.Instance.getProperty("ActionBinHandler.windowsAllowedCmds", "dir,ping,nslookup"));
		this.windowsMetachars = parseList(Configuration.Instance.getProperty("ActionBinHandler.windowsMetachars", "&,&&,|,||,<,>,>&,>>,>>&"));
		this.linuxAllowedCmds = parseList(Configuration.Instance.getProperty("ActionBinHandler.linuxAllowedCmds", "ls,pwd,whoami,nslookup,ping"));
		this.linuxMetachars = parseList(Configuration.Instance.getProperty("ActionBinHandler.linuxMetachars", ";,&,&&,|,||,&|,>,>>,<,<<"));
		
		if (this.osName.contains("Windows")) {
			this.allowedCmds = this.windowsAllowedCmds;
			this.metachars = this.windowsMetachars;
		} else {
			this.allowedCmds = this.linuxAllowedCmds;
			this.metachars = this.linuxMetachars;
		}
	}
	
	@Override
	public boolean shouldHandleTarget(HttpRequest request) {
		if (request.getHeaders().getTarget().toLowerCase().startsWith("/bin"))
			return true;
		
		return false;
	}

	@Override
	public HttpResponse handleRequest(HttpRequest request) throws Exception {
		List<String> commands = null;
		List<String> decodedCommands = null;
		
		if (request.getHeaders().getMethod() == HttpRequestMethod.GET) {
			if (request.getQueryString().containsKey("command")) {
				commands = request.getQueryStringByName("command");
			}
		} else if (request.getHeaders().getMethod() == HttpRequestMethod.POST) {
			if (request.getForm().containsKey("command")) {
				commands = request.getFormByName("command");
			}
		}
		
		if (commands == null) {
			return this.handleBadRequest("No command was specified.", request);
		}
		
		//check for metachars
		if (this.containsMetachars(commands)) {
			return this.handleForbidden("Metacharacters are not allowed for command execution.", request);
		}
		
		//check for allowed commands
		if (!this.isAllowedCommand(commands.get(0))) {
			return this.handleForbidden("Command is not allowed.", request);
		}
		
		decodedCommands = new ArrayList<String>(commands.size());
		
		//check fi we shoud decode URL here
		if (this.decodeUrl) {
			try {
				for (String command : commands) {
					decodedCommands.add(URLDecoder.decode(command, "UTF-8"));
				}
			} catch (UnsupportedEncodingException e) {
				throw new Exception("Mailformed target url.");
			}
		} else {
			decodedCommands.addAll(commands);
		}
		
		//run the command
		Shell shell = new Shell();
		List<String> output = shell.execute(decodedCommands);
		if (output == null) {
			throw new Exception("Unknown error occured while executing command.");
		}
		
		String stdout = output.get(0);
		String stderr = output.get(1);
		String content = null;
		
		if (stderr.equals("")) {
			content = this.encodeHtml(decodedCommands.get(0), stdout, false);
		} else {
			content = this.encodeHtml(decodedCommands.get(0), stderr, true);
		}
		
		//build the response
		HttpResponse response = new HttpResponse();
		response.getHeaders().setCode(HttpResponseCode.OK_200);
		response.setContent(content, HttpContentTypes.TEXT_HTML);
		
		return response;
	}
	
	private boolean isAllowedCommand(String command) {
		for (String allowedCmd : this.allowedCmds) {
			if (command.trim().toLowerCase().startsWith(allowedCmd)) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean containsMetachars(String command) {
		for (String metachar : this.metachars) {
			if (command.toLowerCase().contains(metachar))
				return true;
		}
		
		return false;
	}
	
	private boolean containsMetachars(List<String> commands) {
		for (String command : commands) {
			if (this.containsMetachars(command))
				return true;
		}
		
		return false;
	}
	
	private HttpResponse handleForbidden(String message, HttpRequest request) {
		HttpResponse response = new HttpResponse();
		response.getHeaders().setCode(HttpResponseCode.FORBIDDEN_403);
		
		//build the response page
		StringBuilder buffer = new StringBuilder();
		buffer.append("<html>\n<head>\n<title>403 - Forbidden</title>\n</head>\n<body>\n");
		buffer.append("<h3>403 - FORBIDDEN</h3>\n");
		buffer.append("<p><strong>Error: </strong>");
		buffer.append(message);
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
	
	private static List<String> parseList(String rawList) {
		return new ArrayList<String>(Arrays.asList(rawList.trim().split(",")));
	}
}
