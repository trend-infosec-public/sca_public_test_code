/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.handlers;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import stonesoup.Configuration;
import stonesoup.http.HttpContentTypes;
import stonesoup.http.HttpRequest;
import stonesoup.http.HttpRequestMethod;
import stonesoup.http.HttpResponse;
import stonesoup.http.HttpResponseCode;

/**
 * Provides a handler to execute cgi-bin executables as subprocesses, and display
 * the results.
 *
 * 
 *
 */
public class ActionCgiBinHandler implements IRequestHandler {

	private String envPath = "cgi-bin/";
	private String cgiCommandScript = "cgi_exec.sh";

	public ActionCgiBinHandler() {
		this.envPath = Configuration.Instance.getProperty("ActionCgiBinHandler.binDirectory", "cgi-bin/");
		this.cgiCommandScript = Configuration.Instance.getProperty("ActionCgiBinHandler.scriptName", "cgi_exec.sh");
	}

	@Override
	public boolean shouldHandleTarget(HttpRequest request) {
		if (request.getHeaders().getTarget().equalsIgnoreCase("/cgi-bin"))
			return true;

		return false;
	}

	@Override
	public HttpResponse handleRequest(HttpRequest request) throws Exception {
		List<String> command = null;
		List<String> cgiCommand = new ArrayList<String>();
		Map<String, List<String>> allParams = null;

		if (request.getHeaders().getMethod() == HttpRequestMethod.GET) {
			if (request.getQueryString().containsKey("command")) {
				command = request.getQueryStringByName("command");
			}
			allParams = request.getQueryString();
		} else if (request.getHeaders().getMethod() == HttpRequestMethod.POST) {
			if (request.getForm().containsKey("command")) {
				command = request.getFormByName("command");
			}
			allParams = request.getForm();
		}

		if (command == null) {
			return this.handleBadRequest("Missing target \"command\" parameter(s).", request);
		}

		String realCommand = command.get(0);
		if (realCommand.contains(File.separator)) {
			return this.handleForbidden("Primary command may not contain absolute or relative references.", request);
		}

		//build path
		File scriptFile = new File(this.envPath, this.cgiCommandScript);
		ProcessBuilder procBuilder = new ProcessBuilder();
		cgiCommand.add(scriptFile.getPath());
		cgiCommand.addAll(command);
		procBuilder.command(cgiCommand);
		Map<String, String> procEnv = procBuilder.environment();
		// - Attempts to clean the subprocess PATH, and set it to
		//            the path of the cgi-bin, that way only approved scripts
		//            may be run.
		if (procEnv.containsKey("PATH"))
			procEnv.remove("PATH");
		procEnv.put("PATH", scriptFile.getParentFile().getAbsolutePath());

		// - Assumes any other keyed params go in the environment
		//            of the subprocess as variables to the command.
		for (Entry<String, List<String>> entry : allParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("command"))
				continue;
			else if (entry.getKey().equalsIgnoreCase("path")) {
				// Crossover MAIN - Blindly append other parameters to
				//                           the subprocess path.
				String newPath = procEnv.get("PATH");	//STONESOUP:INTERACTION_POINT	//STONESOUP:CROSSOVER_POINT
				procEnv.remove("PATH");

				for (String path : entry.getValue()) {	//STONESOUP:DATA_TYPE:SHORT_CHARACTER	//STONESOUP:CONTROL_FLOW:COLLECTION_CONTROLLED_LOOP	//STONESOUP:DATA_FLOW:PASS_BY_REFERENCE
					newPath = newPath + File.pathSeparator + path;
					//newPath = path;
				}

				procEnv.put("PATH", newPath);
			} else {
				String newValue = "";
				for (String value : entry.getValue())
					newValue = value;
				procEnv.put(entry.getKey().toUpperCase(), newValue);
			}
		}

		//result
		String result = null;
		boolean isError = false;

		// Command executed with controlled environment.
		Process proc = procBuilder.start();	//STONESOUP:TRIGGER_POINT
		if (proc.waitFor() != 0) {
			result = this.readFully(new InputStreamReader(proc.getErrorStream()));
			isError = true;
		} else {
			result = this.readFully(new InputStreamReader(proc.getInputStream()));
		}

		//file is accessible
		HttpResponse response = new HttpResponse();
		response.getHeaders().setCode(HttpResponseCode.OK_200);
		response.setContent(this.encodeHtml(realCommand, result, isError), HttpContentTypes.TEXT_HTML);
		return response;
	}

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

	private String readFully(InputStreamReader input) throws Exception {
		char[] buffer = new char[4096];
		StringBuilder output = new StringBuilder();
		int readsize = 0;

		while ((readsize = input.read(buffer)) != -1) {
			output.append(buffer, 0, readsize);
		}

		return output.toString();
	}

	private String encodeHtml(String commandName, String processOutput, boolean isError) {
		String lineSeperator = System.getProperty("line.separator");

		StringBuilder html = new StringBuilder();
		html.append("<html>\n<head>\n<title>");
		html.append(commandName);
		html.append(" Results</title>\n</head>\n<body>\n");

		if (isError) {
			html.append("<h3 style=\"color:red;\">Command Failed</h3>\n");
		}
		html.append("<p style=\"font-family:monospace;font-size:12px;\">");
		html.append(processOutput.replace(lineSeperator, "<br>\n"));
		html.append("</p>\n</body>\n</html>");

		return html.toString();
	}
}
