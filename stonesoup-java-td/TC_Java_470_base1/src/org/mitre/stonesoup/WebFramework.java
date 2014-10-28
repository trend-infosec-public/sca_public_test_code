

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package org.mitre.stonesoup;

/***********************************************************************
**
**
**  
**  Date: 21 March 2012
**
**  Revision History
**  Date      Change
**
**
**  Main will run as a simulated web framework. URL requests are passed in on the command line
**  (Not HTTP get parameters required - just the URL) and the application will do the rest to 
**  route and render the response.
**
**  Program outputs an HTML page as requested.
**
** Error: This web framework uses reflection to instantiate classes and call methods. If the user specifies
** 		a Java class and method name in the path portion of the URL it will be called with the parameters supplied.
**		Therefore, something like example.com/java.lang.System/getProperty.html will end up calling the getProperty()
** 		method of the System class. 
**
**
** STONESOUP Weakness Class: Use of Externally-Controlled Input to Select Classes or Code
** CWE ID: CWE-470
** Variant Spreadsheet Rev #: ###
** Variant Spreadsheet ID: ###
**
** Variant Features:
**
**
** I/0 Pairs:
**   Good: 1st Set:
**         2nd Set:
**         3rd Set:
**         4th Set:
**         5th Set:
**    Bad: 1st Set:
**         2nd Set:
**
** How program works:
**		The program takes a URL as a command line argument and passes it to the appropriate renderer it using reflection.
**
***********************************************************************/
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;

public class WebFramework 
{
	public static String index()
	{
		return "<html>This is the index page!</html>";
	}
	
	public static String error_404()
	{
		return "<html>Error 404. Page not found.</html>";
	}
	
	public static String links()
	{
		return "<html>[<a href=\"google.com\">Google</a>|<a href=\"geocities.com\">Geocities</a>|<a href=\"ask.com\">Ask Jeeves</a>]</html>";
	}
	
	public static String userInfo(String userName)
	{
		String n = userName.toLowerCase();
		if(n.equals("mark"))
			return "<html>Mark is a good programmer</html>";
		else if (n.equals("matt"))
			return "<html>Matt knows Java.</html>";
		else if (n.equals("bryan"))
			return "<html>Bryan knows XCCDF.</html>";
		
		return "<html>User name is unknown</html>";
	}
	
	/**
	 * This application mimics a Web Framework
	 * 
	 * It takes a URL (http://www.example.com/foo/bar.html?arg1=val1&arg2=val2
	 * and it calls Foo.bar(arg1, arg2)
	 * 
	 * A 'good' implementation will only allow classes that are explicitly
	 * defined in the resolver to be called. A 'bad' implementation will allow anything, more or less.
	 * 
	 * In the bad implementation you can do something like example.com/fully.qualified.classname/methodName.html?arg1=argument_one&arg2=argument_two...
	 * For example, example.com/java.lang.System/getProperty.html?arg1=
	 * The vulnerability only discloses methods that have 0-n string arguments, but that's a lot of them
	 * 
	 * 
	 * For reference: <scheme>://<authority><path>?<query>#<fragment>
	 *            eg:     http://www.example.com/foo/bar.html?arg1=val1&arg2=val2#anchor
	 * @throws ClassNotFoundException 
	 * 
	 * 
	 */
	public static void main(String args[]) throws Exception
	{
		//This is the resolver. In typical frameworks, this is specified through 
		//a configuration file or convention. I have hard coded this for simplicity.
		HashMap<String, String> resolver = new HashMap<String, String>();
		resolver.put("home", "org.mitre.stonesoup.WebFramework");
		
		//Make a simple check
		if(args.length < 1) throw new IllegalArgumentException("Was expecting a single argument!");
		
		URL url;
		try { url = new URL(args[0]); }//STONESOUP:INTERACTION_POINT
		catch (Exception e) { throw new IllegalArgumentException("Malformatted URL"); }
		
		String[] sPath = url.getPath().split("/");
		if(sPath.length != 3)
			throw new IllegalArgumentException("Only supports a single level of path, eg example.com/foo/bar.html");
		
		//Get the object and method to call
		String object = sPath[1];
		if(resolver.get(object) != null) object = resolver.get(object); //STONESOUP:CROSSOVER_POINT
		String method = sPath[2];
		method = method.replace(".html", "");
		
		//Parse the supplied query parameters.
		//Ideally, a hash map of <key, value> would be sent. However, 
		//for vulnerability's sake, we just pass an array of Strings.
		String query = url.getQuery();
		String[] params = null;
		Class[] params_class = null;
		if(query != null)
		{
			String[] query_items = query.split("&");
			params = new String[query_items.length];
			params_class = new Class[query_items.length];
			for(int x = 0; x < query_items.length; x++)
			{
				String[] parts = query_items[x].split("=");
				params[x] = parts[1];
				params_class[x] = String.class;
			}
		}
		Object ret;
		try
		{
			Class c = Class.forName(object); //Get a handle on the class
			Object o = null;
			if(c.getConstructors().length > 0)//If there is a constructor, instantiate it (Singletons will not have constructors
				o = c.newInstance();
			
			Method  m = c.getMethod(method, params_class);//Get the method, then call the method.
			ret = m.invoke(o==null?c:o, params);//STONESOUP:TRIGGER_POINT
		}
		catch(Exception ex) {  
			System.out.println(ex.getMessage());
			ret = error_404();
		}
		System.out.println(ret.toString());
		
	}
}
