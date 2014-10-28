

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/***********************************************************************
 **
 **
 **  
 **  Date: 23 Mar 2012
 **
 ** 
 **
 ** Error: Allows unrestricted URL redirection
 **
 **
 ** STONESOUP Weakness Class:
 ** CWE ID: CWE-601
 ** Variant Spreadsheet Rev #: ###
 ** Variant Spreadsheet ID: 
 **
 ** Variant Features:
 **
 **
 ** I/0 Pairs:
 **   Good: 1st Set:good1-in.txt, good1-out.txt
 **         2nd Set:good2-in.txt, good2-out.txt
 **   Bad:  1st Set: bad1-in.txt, bad1-out.txt
 **
 ** How program works:
 **  a java CGI application that reads in standard CGI environment variables.
 **  It only supports the GET method type, returning 405 Method Not Allowed
 **  for all other types. 
 **  If a request parameter "action" is set to "hello", it returns a hello world greeting.
 **  If "action" is set to "redir", a 302 redirect is sent for the location
 **  specified in the "arg1" request parameter.
 * 
 ************************************************************************/
package stonesoup;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * 
 */
public class CGIRedirector {

  private String DOCUMENT_ROOT = "DOCUMENT_ROOT";
  private String HTTP_COOKIE = "HTTP_COOKIE";
  private String HTTP_HOST = "HTTP_HOST";
  private String HTTP_REFERER = "HTTP_REFERER";
  private String HTTP_USER_AGENT = "HTTP_USER_AGENT";
  private String HTTPS = "HTTPS";
  private String VALUE_HTTPS_ACTIVE = "on";
  private String PATH = "PATH";
  private String QUERY_STRING = "QUERY_STRING";
  private String REMOTE_ADDR = "REMOTE_ADDR";
  private String REMOTE_HOST = "REMOTE_HOST";
  private String REMOTE_PORT = "REMOTE_PORT";
  private String REMOTE_USER = "REMOTE_USER";
  private String REQUEST_METHOD = "REQUEST_METHOD";
  private String REQUEST_URI = "REQUEST_URI";
  private String SCRIPT_FILENAME = "SCRIPT_FILENAME";
  private String SCRIPT_NAME = "SCRIPT_NAME";
  private String SERVER_ADMIN = "SERVER_ADMIN";
  private String SERVER_NAME = "DOCUMENT_ROOT";
  private String SERVER_PORT = "SERVER_NAME";
  private String SERVER_SOFTWARE = "SERVER_SOFTWARE";

  Map<String, String> env = System.getenv();

  public boolean isHTTPS() {
    String https = env.get(HTTPS);
    if (https == null || VALUE_HTTPS_ACTIVE.equalsIgnoreCase(https)) {
      return false;
    }
    return true;
  }
  private Map<String, String> requestParameterMap = null;

  public String getRequestParameter(String paramName) {
    if (requestParameterMap == null) {
      setupParameters();
    }
    return requestParameterMap.get(paramName);
  }

  private void setupParameters() {
    requestParameterMap = new HashMap<String, String>();
    String query = env.get(QUERY_STRING);
    if (query == null || query.isEmpty()) {
      return;
    }
    String[] pairs = query.split("&");
    for (String pair : pairs) {
      if (!pair.isEmpty()) {
        String[] splitPair = pair.split("=", 2);
        String key = urlDecode(splitPair[0]);
        String val = ""; // use empty string to signal key with no value
        if (splitPair.length > 1) {
          val = urlDecode(splitPair[1]);
        }
        requestParameterMap.put(key, val);
      }
    }
  }

  public String urlDecode(String s) {
    try {
      return URLDecoder.decode(s, "UTF8"); // try UTF-8
    } catch (UnsupportedEncodingException ex) {
      return URLDecoder.decode(s); // fall back to system default encoding;
    }
  }

  public void send400() {
    System.out.println("Status: 400 Bad Request\n");
    System.out.println("not supported.");
  }

  public void handleRequest() { // STONESOUP:INTERACTION_POINT
    System.out.println("Content-type: text/html");
    String method = env.get(this.REQUEST_METHOD);
    if ("GET".equals(method)) {
      String action = this.getRequestParameter("action");
      if (action != null && !action.isEmpty()) {
        if ("hello".equals(action)) {
          System.out.println("Status: 200 OK\n");
          System.out.println("Hello world");
        } else if ("redir".equals(action)) {
          System.out.println("Status: 302 Found");
          String redir = this.getRequestParameter("arg1"); // STONESOUP:CROSSOVER_POINT
          if(redir == null) redir = "";
          System.out.print("Location: ");
          System.out.println(redir); // STONESOUP:TRIGGER_POINT
          System.out.println();
          System.out.println("You have been redirected.");
        } else {
          send400();
        }
      } else {
        send400();
      }
    } else {
      System.out.println("Status: 405 Method Not Allowed\n");
      System.out.println("not supported.");
    }
  }

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    CGIRedirector redirector = new CGIRedirector();
    redirector.handleRequest();
  }
}
