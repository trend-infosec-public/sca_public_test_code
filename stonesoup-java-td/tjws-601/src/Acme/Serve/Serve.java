// Serve - minimal Java HTTP server class
//
// Copyright (C)1996,1998 by Jef Poskanzer <jef@acme.com>. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/
//

// All enhancments Copyright (C)1998-2006 by Dmitriy Rogatkin
// This version is compatible with JSDK 2.5
// http://tjws.sourceforge.net
// $Id: Serve.java,v 1.72 2006/01/06 02:00:18 drogatkin Exp $

package Acme.Serve;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Vector;

import javax.net.SocketFactory;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.SingleThreadModel;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import Acme.Utils;
import Acme.Serve.Serve.PathTreeDictionary.Node;

/// Minimal Java HTTP server class.
// <P>
// This class implements a very small embeddable HTTP server.
// It runs Servlets compatible with the API used by JavaSoft's
// <A HREF="http://java.sun.com/products/java-server/">JavaServer</A> server.
// It comes with default Servlets which provide the usual
// httpd services, returning files and directory listings.
// <P>
// This is not in any sense a competitor for JavaServer.
// JavaServer is a full-fledged HTTP server and more.
// Acme.Serve is tiny, about 1500 lines, and provides only the
// functionality necessary to deliver an Applet's .class files
// and then start up a Servlet talking to the Applet.
// They are both written in Java, they are both web servers, and
// they both implement the Servlet API; other than that they couldn't
// be more different.
// <P>
// This is actually the second HTTP server I've written.
// The other one is called
// <A HREF="http://www.acme.com/software/thttpd/">thttpd</A>,
// it's written in C, and is also pretty small although much more
// featureful than this.
// <P>
// Other Java HTTP servers:
// <UL>
// <LI> The above-mentioned <A
// HREF="http://java.sun.com/products/java-server/">JavaServer</A>.
// <LI> W3C's <A HREF="http://www.w3.org/pub/WWW/Jigsaw/">Jigsaw</A>.
// <LI> David Wilkinson's <A
// HREF="http://www.netlink.co.uk/users/cascade/http/">Cascade</A>.
// <LI> Yahoo's <A
// HREF="http://www.yahoo.com/Computers_and_Internet/Software/Internet/World_Wide_Web/Servers/Java/">list
// of Java web servers</A>.
// </UL>
// <P>
// A <A HREF="http://www.byte.com/art/9706/sec8/art1.htm">June 1997 BYTE
// magazine article</A> mentioning this server.<BR>
// A <A HREF="http://www.byte.com/art/9712/sec6/art7.htm">December 1997 BYTE
// magazine article</A> giving it an Editor's Choice Award of Distinction.<BR>
// <A HREF="/resources/classes/Acme/Serve/Serve.java">Fetch the
// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.Z">Fetch the entire Acme package.</A>
// <P>
// @see Acme.Serve.servlet.http.HttpServlet
// @see FileServlet
// @see CgiServlet

// make it final?
public class Serve implements ServletContext, RequestDispatcher, Serializable {

	private static final String progName = "Serve";

	public static final String ARG_PORT = "port";

	public static final String ARG_THROTTLES = "throttles";

	public static final String ARG_SERVLETS = "servlets";

	public static final String ARG_REALMS = "realms";

	public static final String ARG_ALIASES = "aliases";

	public static final String ARG_BINDADDRESS = "bind-address";

	public static final String ARG_BACKLOG = "backlog";

	public static final String ARG_CGI_PATH = "cgi-path";

	public static final String ARG_ERR = "error-stream";

	public static final String ARG_SESSION_TIMEOUT = "session-timeout";

	public static final String ARG_LOG_OPTIONS = "log-options";

	public static final String ARG_SOCKET_FACTORY = "socketFactory";

	public static final String ARG_NOHUP = "nohup";

	public static final String ARG_JSP = "JSP";

	public static final String ARG_WAR = "war-deployer";

	public static final String ARG_KEEPALIVE = "keep-alive";

	public static final String ARG_KEEPALIVE_TIMEOUT = "timeout-keep-alive";

	public static final String ARG_MAX_CONN_USE = "max-alive-conn-use";

	public static final String ARG_NOSESSION_PERSIST = "no-persistance";

	protected static final int DEF_SESSION_TIMEOUT = 30; // in minutes

	protected static final int DEF_PORT = 9090;

	protected static final int DEF_MAX_CONN_USE = 100;
	
	protected static final String UTF8 = "UTF-8"; // default encoding

	// / Main routine, if you want to run this directly as an application.
	public static void main(String[] args) {
		Map arguments = new HashMap(20);

		int argc = args.length;
		int argn;
		// Parse args.
		workPath = System.getProperty("user.dir", ".");
		if (argc == 0) { // a try to read from file for java -jar server.jar
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(new File(
						workPath, "cmdparams")));
				StringTokenizer st = new StringTokenizer(br.readLine(), " ");
				args = new String[st.countTokens()];
				argc = args.length; // tail can be nulled
				for (int i = 0; i < argc && st.hasMoreTokens(); i++)
					args[i] = st.nextToken();
			} catch (Exception e) { // many can happen
			} finally {
				if(br != null)
					try {
						br.close();
					}catch(IOException ioe) {
					}
			}
		}
		// TODO: redesign process of parameters based on a map
		for (argn = 0; argn < argc && args[argn].charAt(0) == '-';) {
			if (args[argn].equals("-p") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_PORT, new Integer(args[argn]));
			} else if (args[argn].equals("-t") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_THROTTLES, args[argn]);
			} else if (args[argn].equals("-s") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_SERVLETS, args[argn]);
			} else if (args[argn].equals("-r") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_REALMS, args[argn]);
			} else if (args[argn].equals("-a") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_ALIASES, args[argn]);
			} else if (args[argn].equals("-b") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_BINDADDRESS, args[argn]);
			} else if (args[argn].equals("-k") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_BACKLOG, args[argn]);
			} else if (args[argn].equals("-j") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_JSP, args[argn]);
			} else if (args[argn].equals("-w") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_WAR, args[argn]);
			} else if (args[argn].equals("-c") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_CGI_PATH, args[argn]);
			} else if (args[argn].equals("-mka") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_MAX_CONN_USE, args[argn]);
				arguments.put(ARG_KEEPALIVE, Boolean.TRUE);
			} else if (args[argn].equals("-nka")) {
				arguments.put(ARG_KEEPALIVE, Boolean.FALSE);
			} else if (args[argn].equals("-nsp")) {
				arguments.put(ARG_NOSESSION_PERSIST, Boolean.TRUE);
			} else if (args[argn].equals("-kat") && argn + 1 < argc) {
				++argn;
				arguments.put(ARG_KEEPALIVE_TIMEOUT, args[argn]);
				arguments.put(ARG_KEEPALIVE, Boolean.TRUE);
			} else if (args[argn].equals("-e") && argn + 1 < argc) {
				++argn;
				try {
					arguments.put(ARG_SESSION_TIMEOUT, new Integer(args[argn]));
				} catch (NumberFormatException nfe) {
				}
			} else if (args[argn].startsWith("-l")) {
				if (args[argn].length() > 2)
					arguments.put(ARG_LOG_OPTIONS, args[argn].substring(2)
							.toUpperCase());
				else
					arguments.put(ARG_LOG_OPTIONS, "");
			} else if (args[argn].startsWith("-nohup")) {
				arguments.put(ARG_NOHUP, ARG_NOHUP);
			} else if (args[argn].equals("-err")) {
				if (argn + 1 < argc && args[argn + 1].startsWith("-") == false) {
					++argn;
					try {
						arguments.put(ARG_ERR, (PrintStream) Class.forName(
								args[argn]).newInstance());
					} catch (Error er) {
						System.err.println("Problem of processing class parameter of error redirection stream: "
										+ er);
					} catch (Exception ex) {
						System.err.println("Exception in processing class parameter of error redirection stream: "
										+ ex);
					}
				} else
					arguments.put(ARG_ERR, System.err);
			} else if (args[argn].startsWith("-")) {
				if (args[argn].length() > 1)
					arguments.put(args[argn].substring(1),// .toUpperCase(),
							argn < argc - 1 ? args[++argn] : "");
			} else
				usage();

			++argn;
		}
		if (argn != argc)
			usage();
		/**
		 * format path mapping from=givenpath;dir=realpath
		 */
		PrintStream printstream = System.err;
		try {
			printstream = new PrintStream(new FileOutputStream(new File(
					workPath, "AWS-" + System.currentTimeMillis() + ".log")),
					true);
			if (arguments.get(ARG_ERR) != null)
				System.setErr((PrintStream) arguments.get(ARG_ERR));
			else
				System.setErr(printstream);
		} catch (IOException e) {
			System.err.println("IO problem at setting a log stream " + e);
		}
		PathTreeDictionary mappingtable = new PathTreeDictionary();
		if (arguments.get(ARG_ALIASES) != null) {
			File file = new File((String) arguments.get(ARG_ALIASES));
			if (file.isAbsolute() == false)
				file = new File(workPath, file.getPath());
			if (file.exists() && file.canRead()) {
				try {
					DataInputStream in = new DataInputStream(
							new FileInputStream(file));
					do {
						String mappingstr = in.readLine();
						if (mappingstr == null)
							break;
						StringTokenizer maptokenzr = new StringTokenizer(
								mappingstr, "=;");
						if (maptokenzr.hasMoreTokens()) {
							if (maptokenzr.nextToken("=").equalsIgnoreCase(
									"from")) {
								if (maptokenzr.hasMoreTokens()) {
									String srcpath = maptokenzr.nextToken("=;");
									if (maptokenzr.hasMoreTokens()
											&& maptokenzr.nextToken(";=")
													.equalsIgnoreCase("dir"))
										try {
											if (maptokenzr.hasMoreTokens()) {
												File mapFile = new File(
														maptokenzr.nextToken());
												if (mapFile.isAbsolute() == false)
													mapFile = new File(
															workPath, mapFile
																	.getPath());
												mappingtable.put(srcpath,
														mapFile);
											}
										} catch (NullPointerException e) {
										}
								}
							}
						}
					} while (true);
				} catch (IOException e) {
					System.err.println("Problem reading aliases file: "
							+ arguments.get(ARG_ALIASES) + "/" + e);
				}
			} else
				System.err.println("File " + file + " ("
						+ arguments.get(ARG_ALIASES)
						+ ") doesn't exist or not readable.");
		}
		/**
		 * format realmname=path,user:password,,,,
		 */
		PathTreeDictionary realms = new PathTreeDictionary();
		if (arguments.get(ARG_REALMS) != null) {
			try {
				File file = new File((String) arguments.get(ARG_REALMS));
				if (file.isAbsolute() == false)
					file = new File(workPath, file.getPath());
				DataInputStream in = new DataInputStream(new FileInputStream(
						file));

				do {
					String realmstr = in.readLine();
					if (realmstr == null)
						break;
					StringTokenizer rt = new StringTokenizer(realmstr, "=,:");
					if (rt.hasMoreTokens()) {
						String realmname = null;
						realmname = rt.nextToken();
						if (rt.hasMoreTokens()) {
							String realmPath = null;
							realmPath = rt.nextToken();
							if (rt.hasMoreTokens()) {
								String user = rt.nextToken();
								if (rt.hasMoreTokens()) {
									String password = rt.nextToken();
									BasicAuthRealm realm = null;
									Object o[] = realms.get(realmPath);
									if (o != null && o[0] != null)
										realm = (BasicAuthRealm) o[0];
									else {
										realm = new BasicAuthRealm(realmname);
										realms.put(realmPath, realm);
									}
									realm.put(user, password);
								}
							}
						}
					}
				} while (true);
			} catch (IOException ioe) {
				System.err.println("IO problem in reading realms file "
						+ arguments.get(ARG_REALMS)+ ": " + ioe);
			}
		}
		// Create the server.
		serve = new Serve(arguments, printstream);
		// can use log(.. after this point
		File tempFile = arguments.get(ARG_SERVLETS) == null ? null : new File(
				(String) arguments.get(ARG_SERVLETS));
		if (tempFile != null && tempFile.isAbsolute() == false)
			tempFile = new File(workPath, tempFile.getPath());
		final File servFile = tempFile;
		serve.setMappingTable(mappingtable);
		serve.setRealms(realms);

		new Thread(new Runnable() {
			public void run() {
				serve.readServlets(servFile);
			}
		}).start();
		// And add the standard Servlets.
		String throttles = (String) arguments.get(ARG_THROTTLES);
		if (throttles == null)
			serve.addDefaultServlets((String) arguments.get(ARG_CGI_PATH));
		else
			try {
				serve.addDefaultServlets((String) arguments.get(ARG_CGI_PATH),
						throttles);
			} catch (IOException e) {
				serve.log("Problem reading throttles file: " + e, e);
				System.exit(1);
			}
		serve.addWarDeployer((String) arguments.get(ARG_WAR), throttles);
		// And run.
		serve.serve();
		// System.exit( 0 );
	}

	private static void usage() {
		System.out
				.println("usage:  "
						+ progName
						+ " [-p port] [-s servletpropertiesfile] [-a aliasmappingfile]\n"
						+ "         [-b bind address] [-k backlog] [-l[a][r]] [-c cgi-bin-dir]\n"
						+ "         [-nsp] [-j jsp_supporter_class] [-w war_deployment_module_class]\n"
						+ "         [-nka] [-kat timeout_in_secs] [-mka max_times_connection_use]\n"
						+ "         [-e [-]duration_in_minutes] [-nohup] [-err [class_name]] [-socketFactory class name and other parameters}");
		System.exit(1);
	}

	private void readServlets(File servFile) {
		/**
		 * servlet.properties file format servlet. <servletname>.code=
		 * <servletclass>servlet. <servletname>.initArgs= <name=value>,
		 * <name=value>
		 */
		Hashtable servletstbl, parameterstbl;
		servletstbl = new Hashtable();
		parameterstbl = new Hashtable();
		if (servFile != null && servFile.exists() && servFile.canRead()) {
			try {
				DataInputStream in = new DataInputStream(new FileInputStream(
						servFile));
				/**
				 * format of servlet.cfg file
				 * servlet_name;servlet_class;init_parameter1=value1;init_parameter2=value2...
				 */
				do {
					String servletdsc = in.readLine();
					if (servletdsc == null)
						break;
					StringTokenizer dsctokenzr = new StringTokenizer(
							servletdsc, ".=,", false);
					if (dsctokenzr.hasMoreTokens()) {
						if (!dsctokenzr.nextToken().equalsIgnoreCase("servlet")) {
							log("No leading 'servlet' keyword, the sentence is skipped");
							break;
						}
						if (dsctokenzr.hasMoreTokens()) {
							String servletname = dsctokenzr.nextToken();

							if (dsctokenzr.hasMoreTokens()) {
								String lt = dsctokenzr.nextToken();
								if (lt.equalsIgnoreCase("code")) {
									if (dsctokenzr.hasMoreTokens())
										servletstbl.put(servletname, dsctokenzr
												.nextToken("="));
								} else if (lt.equalsIgnoreCase("initArgs")) {
									Hashtable initparams = new Hashtable();
									while (dsctokenzr.hasMoreTokens()) {
										String key = dsctokenzr.nextToken("=,");
										if (dsctokenzr.hasMoreTokens())
											initparams.put(key, dsctokenzr
													.nextToken(",="));
									}
									parameterstbl.put(servletname, initparams);
								} else
									log("Unrecognized token "
											+ lt + " in " + servletdsc
											+ ", the line's skipped");
							}
						}
					}
				} while (true);
			} catch (IOException e) {
				log("Problem reading cfg file: " + e);
			}
			Enumeration se = servletstbl.keys();
			String servletname;
			while (se.hasMoreElements()) {
				servletname = (String) se.nextElement();
				addServlet(servletname, (String) servletstbl.get(servletname),
						(Hashtable) parameterstbl.get(servletname));
			}
		}

	}

	int port;

	String hostName;

	String hostProtocol;

	private transient PrintStream logStream;

	private boolean useAccLog;

	private boolean keepAlive;

	private int timeoutKeepAlive;

	private int maxAliveConnUse;

	private boolean showUserAgent;

	private boolean showReferer;

	protected String keepAliveHdrParams;

	protected transient PathTreeDictionary registry;

	protected transient PathTreeDictionary realms;

	private transient PathTreeDictionary mappingtable;

	private Hashtable attributes;

	protected transient KeepAliveCleaner keepAliveCleaner;

	protected transient ThreadGroup serverThreads;

	// for sessions
	int uniqer;

	HttpSessionContextImpl sessions;

	static int expiredIn;

	protected Map arguments;

	protected static Serve serve;

	protected static String workPath;

	// / Constructor.
	public Serve(Map arguments, PrintStream logStream) {
		this.arguments = arguments;
		this.logStream = logStream;
		registry = new PathTreeDictionary();
		realms = new PathTreeDictionary();
		attributes = new Hashtable();
		serverThreads = new ThreadGroup("TJWS threads");
		setAccessLogged();
		keepAlive = arguments.get(ARG_KEEPALIVE) == null
				|| ((Boolean) arguments.get(ARG_KEEPALIVE)).booleanValue();
		int timeoutKeepAliveSec;
		try {
			timeoutKeepAliveSec = Integer.parseInt((String) arguments
					.get(ARG_KEEPALIVE_TIMEOUT));
		} catch (Exception ex) {
			timeoutKeepAliveSec = 30;
		}
		timeoutKeepAlive = timeoutKeepAliveSec * 1000;
		try {
			maxAliveConnUse = Integer.parseInt((String) arguments
					.get(ARG_MAX_CONN_USE));
		} catch (Exception ex) {
			maxAliveConnUse = DEF_MAX_CONN_USE;
		}
		keepAliveHdrParams = "timeout=" + timeoutKeepAliveSec + ", max="
				+ maxAliveConnUse;

		expiredIn = arguments.get(ARG_SESSION_TIMEOUT) != null ? ((Integer) arguments
				.get(ARG_SESSION_TIMEOUT)).intValue()
				: DEF_SESSION_TIMEOUT;
		port = arguments.get(ARG_PORT) != null ? ((Integer) arguments
				.get(ARG_PORT)).intValue() : DEF_PORT;
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			synchronized public void run() {
				destroyAllServlets();
			}
		}, "ShutDownHook"));
	}

	public Serve() {
		this(new HashMap(), System.err);
	}

	void setAccessLogged() {
		String logflags = (String) arguments.get(ARG_LOG_OPTIONS);
		if (logflags != null) {
			useAccLog = true;
			showUserAgent = logflags.indexOf('A') >= 0;
			showReferer = logflags.indexOf('R') >= 0;
		}
	}

	boolean isAccessLogged() {
		return useAccLog;
	}

	boolean isShowReferer() {
		return showReferer;
	}

	boolean isShowUserAgent() {
		return showUserAgent;
	}

	boolean isKeepAlive() {
		// TODO: read from command line and timeout as well
		return keepAlive;
	}

	int getKeepAliveDuration() {
		return timeoutKeepAlive;
	}

	String getKeepAliveParamStr() {
		return keepAliveHdrParams;
	}

	int getMaxTimesConnectionUse() {
		return maxAliveConnUse;
	}

	// / Register a Servlet by class name. Registration consists of a URL
	// pattern, which can contain wildcards, and the class name of the Servlet
	// to launch when a matching URL comes in. Patterns are checked for
	// matches in the order they were added, and only the first match is run.
	public void addServlet(String urlPat, String className) {
		addServlet(urlPat, className, (Hashtable) null);
	}

	public void addServlet(String urlPat, String className, Hashtable initParams) {
		// Check if we're allowed to make one of these.
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			int i = className.lastIndexOf('.');
			if (i > 0) {
				security.checkPackageAccess(className.substring(0, i));
				security.checkPackageDefinition(className.substring(0, i));
			}
		}

		// Make a new one.
		try {
			addServlet(urlPat,
					(Servlet) Class.forName(className).newInstance(),
					initParams);
			return;
		} catch (ClassNotFoundException e) {
			log("Class not found: " + className);
		} catch (ClassCastException e) {
			log("Class cast problem: " + e.getMessage());
		} catch (InstantiationException e) {
			log("Instantiation problem: " + e.getMessage());
		} catch (IllegalAccessException e) {
			log("Illegal class access: " + e.getMessage());
		} catch (Exception e) {
			log("Unexpected problem creating servlet: " + e, e);
		}
	}

	// / Register a Servlet. Registration consists of a URL pattern,
	// which can contain wildcards, and the Servlet to
	// launch when a matching URL comes in. Patterns are checked for
	// matches in the order they were added, and only the first match is run.
	public void addServlet(String urlPat, Servlet servlet) {
		addServlet(urlPat, servlet, (Hashtable) null);
	}

	public synchronized void addServlet(String urlPat, Servlet servlet,
			Hashtable initParams) {
		try {
			if (getServlet(urlPat) != null)
				log("Servlet overriden by " + servlet + ", for path:" + urlPat);
			servlet.init(new ServeConfig((ServletContext) this, initParams,
					urlPat));
			registry.put(urlPat, servlet);
		} catch (ServletException e) {
			log("Problem initializing servlet: " + e);
		}
	}

	// / Register a standard set of Servlets. These will return
	// files or directory listings, and run CGI programs, much like a
	// standard HTTP server.
	// <P>
	// Because of the pattern checking order, this should be called
	// <B>after</B> you've added any custom Servlets.
	// <P>
	// The current set of default servlet mappings:
	// <UL>
	// <LI> If enabled, *.cgi goes to CgiServlet, and gets run as a CGI program.
	// <LI> * goes to FileServlet, and gets served up as a file or directory.
	// </UL>
	// @param cgi whether to run CGI programs
	// TODO: provide user specified CGI directory
	public void addDefaultServlets(String cgi) {
		try {
			addDefaultServlets(cgi, null);
		} catch(IOException ioe) { /* ignore, makes sense only for throtles */}
	}

	/**
	 * Register a standard set of Servlets, with optional throttles. These will
	 * return files or directory listings, and run CGI programs, much like a
	 * standard HTTP server.
	 * <P>
	 * Because of the pattern checking order, this should be called <B>after</B>
	 * you've added any custom Servlets.
	 * <P>
	 * The current set of default servlet mappings:
	 * <UL>
	 * <LI> If enabled, *.cgi goes to CgiServlet, and gets run as a CGI program.
	 * <LI> * goes to FileServlet, and gets served up as a file or directory.
	 * </UL>
	 * 
	 * @param cgi
	 *            whether to run CGI programs
	 * @param throttles
	 *            filename to read FileServlet throttle settings from, can be
	 *            null
	 * @throws IOException
	 */
	public void addDefaultServlets(String cgi, String throttles)
			throws IOException {
		// TODO: provide user specified CGI directory
		if (cgi != null) {
			if (getServlet("/" + cgi) == null)
				addServlet("/" + cgi, new Acme.Serve.CgiServlet());
			else
				log("Servlet for path '/" + cgi
						+ "' already defined and no default will be used.");
		}
		if (getServlet("/") == null)
			if (throttles != null)
				addServlet("/", new Acme.Serve.FileServlet(throttles, null));
			else
				addServlet("/", new Acme.Serve.FileServlet());
		else
			log("Servlet for path '/' already defined and no default will be used.");
	}

	protected void addWarDeployer(String deployerFactory, String throttles) {
		if (deployerFactory == null) // try to use def
			deployerFactory = "rogatkin.web.WarRoller";
		try {
			WarDeployer wd = (WarDeployer) Class.forName(deployerFactory)
					.newInstance();
			wd.deploy(this);
		} catch (Exception e) {
			log("Problem initializing war deployer: " + e);
		}
	}

	protected File getPersistentFile() {
		if (arguments.get(ARG_NOSESSION_PERSIST) != null && (Boolean)arguments.get(ARG_NOSESSION_PERSIST) == Boolean.TRUE)
			return null;
		return new File(workPath, hostName + '-' + port + "-session.obj");
	}

	// Run the server. Returns only on errors.
	transient boolean running = true;

	protected transient ServerSocket serverSocket;

	protected transient Thread ssclThread;

	public void serve() {
		try {
			serverSocket = createServerSocket();
			if (arguments.get(ARG_BINDADDRESS) != null)
				hostName = serverSocket.getInetAddress().getHostName();
			else
				hostName = InetAddress.getLocalHost().getHostName();
		} catch (IOException e) {
			log("Server socket: " + e);
			return;
		}

		if (arguments.get(ARG_NOHUP) == null)
			new Thread(new Runnable() {
				public void run() {
					BufferedReader in = new BufferedReader(
							new InputStreamReader(System.in));
					String line;
					while (true) {
						try {
							System.out
									.print("Press \"q\" <ENTER>, for gracefully stopping the server ");
							line = in.readLine();
							if (line != null && line.length() > 0
									&& line.charAt(0) == 'q') {
								notifyStop();
								break;
							}
						} catch (IOException e) {
							log("Exception in reading from console ", e);
							break;
						}
					}
				}
			}, "Stop Monitor").start();
		// else create kill signal handler
		if (expiredIn > 0) {
			ssclThread = new Thread(serverThreads, new Runnable() {
				public void run() {
					while (running) {
						try {
							Thread.sleep(expiredIn * 60 * 1000);
						} catch (InterruptedException ie) {
							if (running == false)
								break;
						}
						Enumeration e = sessions.keys();
						while (e.hasMoreElements()) {
							Object sid = e.nextElement();
							if (sid != null) {
								AcmeSession as = (AcmeSession) sessions.get(sid);
								if (as != null
										&& (as.checkExpired() || !as.isValid())) { // log("sesion
									as = (AcmeSession) sessions.remove(sid);
									if (as != null && as.isValid())
										try {
											as.invalidate();
										} catch (IllegalStateException ise) {

										}
								}
							}
						}
					}
				}
			}, "Session cleaner");
			ssclThread.setPriority(Thread.MIN_PRIORITY);
			ssclThread.start();
		} // else
		// expiredIn = -expiredIn;
		if (isKeepAlive()) {
			keepAliveCleaner = new KeepAliveCleaner();
			keepAliveCleaner.start();
		}
		File fsessions = getPersistentFile();
		if (fsessions != null && fsessions.exists()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(fsessions));
				sessions = HttpSessionContextImpl.restore( br, Math
				.abs(expiredIn) * 60, this);
			} catch (IOException ioe) {
				log("Problem in restoring sessions.", ioe);
			} catch (Exception e) {
				log("Unexpected problem in restoring sessions.", e);
			} finally {
				if (br != null)
					try {
						br.close();
					} catch(IOException ioe) {}
			}
		}
		if (sessions == null)
			sessions = new HttpSessionContextImpl();
		// TODO: display address as name as ip
		System.out.println("Acme httpd " + hostName + ":" + port
				+ " listening.");
		// TODO: consider running two threads for secure and not sockets
		try {
			while (running) {
				try {
					Socket socket = serverSocket.accept();
					new ServeConnection(socket, this);
				} catch (IOException e) {
					log("Accept: " + e);
				} catch (SecurityException se) {
					log("Illegal access: " + se);
				}
			}
		} catch (Throwable t) {
			log("Unhandled exception: " + t + ", server is terminating.");
		} finally {
			try {
				serverSocket.close();
			} catch (IOException e) {
			}
		}
		System.exit(0);
	}

	protected void notifyStop() throws IOException {
		running = false;
		serverSocket.close();
		interruptRunningProcesses();
	}

	protected void interruptRunningProcesses() {
		serverThreads.interrupt();
	}

	public static void stop() throws IOException {
		serve.notifyStop();
	}

	public static interface SocketFactory {
		public ServerSocket createSocket(Map arguments) throws IOException,
				IllegalArgumentException;
                public String getScheme();
	}

	// TODO: add a paremeter to create secure only if requested
	// it will allow to create one or two sockets for secure and non communications
	protected ServerSocket createServerSocket() throws IOException {
		String socketFactoryClass = (String) arguments.get(ARG_SOCKET_FACTORY);
		if (socketFactoryClass != null)
			try {
                SocketFactory sf = (SocketFactory) Class.forName(socketFactoryClass).newInstance();
				ServerSocket ss = sf.createSocket(arguments);
				ss.setReuseAddress(true);
                hostProtocol = sf.getScheme();
				if (hostProtocol == null) {
    			    if (ss.getClass().getName().toUpperCase().indexOf("SSL") < 0)
				       hostProtocol = "http";
  				    else
					hostProtocol = "https";
				}
                return ss;
			} catch (Exception e) {
				log("Couldn't create custom socket factory "
								+ socketFactoryClass
								+ " or call creation method. Standard socket will be created. ",
								e);
			}
		hostProtocol = "http";
		int bl = 50;
		try {
			// TODO: consider conversion at getting the argument
			bl = Integer.parseInt((String) arguments.get(ARG_BACKLOG));
			if (bl < 2)
				bl = 2;
		} catch (Exception e) {
		}
		InetAddress ia = null;
		if (arguments.get(ARG_BINDADDRESS) != null)
			try {
				ia = InetAddress.getByName((String) arguments
						.get(ARG_BINDADDRESS));
			} catch (Exception e) {
			}

		return new ServerSocket(port, bl, ia);
	}

	// Methods from ServletContext.

	// / Gets a servlet by name.
	// @param name the servlet name
	// @return null if the servlet does not exist
	public Servlet getServlet(String name) {
		try {
			return (Servlet) ((Object[]) registry.get(name))[0];
		} catch (NullPointerException npe) {
			return null;
		}
	}

	// / Enumerates the servlets in this context (server). Only servlets that
	// are accesible will be returned. This enumeration always includes the
	// servlet itself.
	public Enumeration getServlets() {
		return registry.elements();
	}

	// / Enumerates the names of the servlets in this context (server). Only
	// servlets that are accesible will be returned. This enumeration always
	// includes the servlet itself.
	public Enumeration getServletNames() {
		return registry.keys();
	}

	// / Destroys all currently-loaded servlets.
	public synchronized void destroyAllServlets() {
		log("destroyAllServlets() run");
		// serialize sessions

		// invalidate all sessions
		File sf = getPersistentFile();
		if (sf != null && sessions != null) {
			Writer w = null;
			try {
				w = new FileWriter(sf);
				sessions.save(w);
				log("Sessions stored.");
			} catch (IOException ioe) {
				log("IO problem in storing sessions " + ioe);
			} catch (Throwable t) {
				log("Problem in storing sessions " + t);
			} finally {
				try {
					w.close();
				} catch (Exception e) {
				}
			}

			Enumeration e = sessions.keys();
			while (e.hasMoreElements()) {
				Object sid = e.nextElement();
				if (sid != null) {
					AcmeSession as = (AcmeSession) sessions.get(sid);
					if (as != null) {
						as = (AcmeSession) sessions.remove(sid);
						if (as != null && as.isValid())
							try {
								as.invalidate();
							} catch (IllegalStateException ise) {

							}
					}
				}
			}
		}
		// destroy servlets
		Enumeration en = registry.elements();
		while (en.hasMoreElements()) {
			Servlet servlet = (Servlet) en.nextElement();
			servlet.destroy();
		}
		// clean access tree
		registry = new PathTreeDictionary();
	}

	public void setMappingTable(PathTreeDictionary mappingtable) {
		this.mappingtable = mappingtable;
	}

	public void setRealms(PathTreeDictionary realms) {
		this.realms = realms;
	}

	Object getSession(String id) {
		return sessions.get(id);
	}

	HttpSession createSession() {
		HttpSession result = new AcmeSession(generateSessionId(), Math
				.abs(expiredIn) * 60, this, sessions);
		sessions.put(result.getId(), result);
		return result;
	}

	void removeSession(String id) {
		sessions.remove(id);
	}

	// / Write information to the servlet log.
	// @param message the message to log
	public void log(String message) {
		Date date = new Date(System.currentTimeMillis());
		logStream.println("[" + date.toString() + "] " + message);
	}

	public void log(String message, Throwable throwable) {
		StringWriter sw = new StringWriter();
		throwable.printStackTrace(new PrintWriter(sw));
		log(message + '\n' + sw);
	}

	// / Write a stack trace to the servlet log.
	// @param exception where to get the stack trace
	// @param message the message to log
	public void log(Exception exception, String message) {
		log(message, exception);
	}

	// / Applies alias rules to the specified virtual path and returns the
	// corresponding real path. It returns null if the translation
	// cannot be performed.
	// @param path the path to be translated
	public String getRealPath(String path) {
		//try {
		//	path = new String(path.getBytes("ISO-8859-1"), UTF8);
		//} catch (Exception ee) { // no encoding
		//}
		//System.err.print("[" + path + "]->[");
		if (mappingtable != null) {
			// try find first sub-path
			Object[] os = mappingtable.get(path);
			// System.err.println("Searching for path: "+path+" found: "+os[0]);
			if (os[0] == null)
				return null;
			int slpos = ((Integer) os[1]).intValue();
			if (slpos > 0) {
				if (path.length() > slpos)
					path = path.substring(slpos + 1);
				else
					path = "";
			} else if (path.length() > 0) {
				char s = path.charAt(0);
				if (s == '/' || s == '\\')
					path = path.substring(1);
			}
			// System.err.println("Path after processing :"+path+" slash was at
			// "+slpos);
			return (os[0].toString()) + File.separatorChar + path;
		}
		return path;
	}

	// / Returns the MIME type of the specified file.
	// @param file file name whose MIME type is required
	public String getMimeType(String file) {
		file = file.toUpperCase();

		if (file.endsWith(".HTML") || file.endsWith(".HTM"))
			return "text/html";
		if (file.endsWith(".TXT"))
			return "text/plain";
		if (file.endsWith(".XML"))
			return "text/xml";
		if (file.endsWith(".CSS"))
			return "text/css";
		if (file.endsWith(".SGML") || file.endsWith(".SGM"))
			return "text/x-sgml";
		// Image
		if (file.endsWith(".GIF"))
			return "image/gif";
		if (file.endsWith(".JPG") || file.endsWith(".JPEG")
				|| file.endsWith(".JPE"))
			return "image/jpeg";
		if (file.endsWith(".PNG"))
			return "image/png";
		if (file.endsWith(".BMP"))
			return "image/bmp";
		if (file.endsWith(".TIF") || file.endsWith(".TIFF"))
			return "image/tiff";
		if (file.endsWith(".RGB"))
			return "image/x-rgb";
		if (file.endsWith(".XPM"))
			return "image/x-xpixmap";
		if (file.endsWith(".XBM"))
			return "image/x-xbitmap";
		if (file.endsWith(".SVG"))
			return "image/svg-xml ";
		if (file.endsWith(".SVGZ"))
			return "image/svg-xml ";
		// Audio
		if (file.endsWith(".AU") || file.endsWith(".SND"))
			return "audio/basic";
		if (file.endsWith(".MID") || file.endsWith(".MIDI")
				|| file.endsWith(".RMI") || file.endsWith(".KAR"))
			return "audio/mid";
		if (file.endsWith(".MPGA") || file.endsWith(".MP2")
				|| file.endsWith(".MP3"))
			return "audio/mpeg";
		if (file.endsWith(".WAV"))
			return "audio/wav";
		if (file.endsWith(".AIFF") || file.endsWith(".AIFC"))
			return "audio/aiff";
		if (file.endsWith(".AIF"))
			return "audio/x-aiff";
		if (file.endsWith(".RA"))
			return "audio/x-realaudio";
		if (file.endsWith(".RPM"))
			return "audio/x-pn-realaudio-plugin";
		if (file.endsWith(".RAM"))
			return "audio/x-pn-realaudio";
		if (file.endsWith(".SD2"))
			return "audio/x-sd2";
		// Application
		if (file.endsWith(".BIN") || file.endsWith(".DMS")
				|| file.endsWith(".LHA") || file.endsWith(".LZH")
				|| file.endsWith(".EXE") || file.endsWith(".DLL")
				|| file.endsWith(".CLASS"))
			return "application/octet-stream";
		if (file.endsWith(".HQX"))
			return "application/mac-binhex40";
		if (file.endsWith(".PS") || file.endsWith(".AI")
				|| file.endsWith(".EPS"))
			return "application/postscript";
		if (file.endsWith(".PDF"))
			return "application/pdf";
		if (file.endsWith(".RTF"))
			return "application/rtf";
		if (file.endsWith(".DOC"))
			return "application/msword";
		if (file.endsWith(".PPT"))
			return "application/powerpoint";
		if (file.endsWith(".FIF"))
			return "application/fractals";
		if (file.endsWith(".P7C"))
			return "application/pkcs7-mime";
		// Application/x
		if (file.endsWith(".JS"))
			return "application/x-javascript";
		if (file.endsWith(".Z"))
			return "application/x-compress";
		if (file.endsWith(".GZ"))
			return "application/x-gzip";
		if (file.endsWith(".TAR"))
			return "application/x-tar";
		if (file.endsWith(".TGZ"))
			return "application/x-compressed";
		if (file.endsWith(".ZIP"))
			return "application/x-zip-compressed";
		if (file.endsWith(".DIR") || file.endsWith(".DCR")
				|| file.endsWith(".DXR"))
			return "application/x-director";
		if (file.endsWith(".DVI"))
			return "application/x-dvi";
		if (file.endsWith(".TEX"))
			return "application/x-tex";
		if (file.endsWith(".LATEX"))
			return "application/x-latex";
		if (file.endsWith(".TCL"))
			return "application/x-tcl";
		if (file.endsWith(".CER") || file.endsWith(".CRT")
				|| file.endsWith(".DER"))
			return "application/x-x509-ca-cert";
		// Video
		if (file.endsWith(".MPG") || file.endsWith(".MPE")
				|| file.endsWith(".MPEG"))
			return "video/mpeg";
		if (file.endsWith(".QT") || file.endsWith(".MOV"))
			return "video/quicktime";
		if (file.endsWith(".AVI"))
			return "video/x-msvideo";
		if (file.endsWith(".MOVIE"))
			return "video/x-sgi-movie";
		// Chemical
		if (file.endsWith(".PDB") || file.endsWith(".XYZ"))
			return "chemical/x-pdb";
		// X-
		if (file.endsWith(".ICE"))
			return "x-conference/x-cooltalk";
		if (file.endsWith(".JNLP"))
			return "application/x-java-jnlp-file";
		if (file.endsWith(".WRL") || file.endsWith(".VRML"))
			return "x-world/x-vrml";
		if (file.endsWith(".WML"))
			return "text/vnd.wap.wml";
		if (file.endsWith(".WMLC"))
			return "application/vnd.wap.wmlc";
		if (file.endsWith(".WMLS"))
			return "text/vnd.wap.wmlscript";
		if (file.endsWith(".WMLSC"))
			return "application/vnd.wap.wmlscriptc";
		if (file.endsWith(".WBMP"))
			return "image/vnd.wap.wbmp";

		return null;
	}

	// / Returns the name and version of the web server under which the servlet
	// is running.
	// Same as the CGI variable SERVER_SOFTWARE.
	public String getServerInfo() {
		return Serve.Identification.serverName + " "
				+ Serve.Identification.serverVersion + " ("
				+ Serve.Identification.serverUrl + ")";
	}

	// / Returns the value of the named attribute of the network service, or
	// null if the attribute does not exist. This method allows access to
	// additional information about the service, not already provided by
	// the other methods in this interface.
	public Object getAttribute(String name) {
		// This server does not support attributes.
		return attributes.get(name);
	}

	// ///////////////// JSDK 2.1 extensions //////////////////////////
	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	public void setAttribute(String name, Object object) {
		if (object != null)
			attributes.put(name, object);
		else
			attributes.remove(name);
	}

	public Enumeration getAttributeNames() {
		return attributes.keys();
	}

	public ServletContext getContext(String uripath) {
		return this; // only root context supported
	}

	public int getMajorVersion() {
		return 2; // support 2.x
	}

	public int getMinorVersion() {
		return 4; // support 2.4
	}

	// 2.3

	/**
	 * Returns a directory-like listing of all the paths to resources within the
	 * web application whose longest sub-path matches the supplied path
	 * argument. Paths indicating subdirectory paths end with a '/'. The
	 * returned paths are all relative to the root of the web application and
	 * have a leading '/'. For example, for a web application containing
	 * <p>
	 * /welcome.html <br>
	 * /catalog/index.html <br>
	 * /catalog/products.html <br>
	 * /catalog/offers/books.html <br>
	 * /catalog/offers/music.html <br>
	 * /customer/login.jsp <br>
	 * /WEB-INF/web.xml <br>
	 * /WEB-INF/classes/com.acme.OrderServlet.class,
	 * <p>
	 * getResourcePaths("/") returns {"/welcome.html", "/catalog/",
	 * "/customer/", "/WEB-INF/"} <br>
	 * getResourcePaths("/catalog/") returns {"/catalog/index.html",
	 * "/catalog/products.html", "/catalog/offers/"}.
	 * <p>
	 * 
	 * @param the -
	 *            partial path used to match the resources, which must start
	 *            with a /
	 * @return a Set containing the directory listing, or null if there are no
	 *         resources in the web application whose path begins with the
	 *         supplied path.
	 * @since Servlet 2.3
	 * 
	 */
	public java.util.Set getResourcePaths(java.lang.String path) {
		String realPath = getRealPath(path);
		if (realPath != null) {

			String[] dir = new File(realPath).list();
			if (dir.length > 0) {
				HashSet set = new HashSet(dir.length);
				for (int i=0; i<dir.length; i++)
					set.add(dir[i]);
				return set;
			}
		}
		return null;
	}

	/**
	 * Returns the name of this web application correponding to this
	 * ServletContext as specified in the deployment descriptor for this web
	 * application by the display-name element.
	 * 
	 * @return The name of the web application or null if no name has been
	 *         declared in the deployment descriptor.
	 * 
	 * @since Servlet 2.3
	 */
	public java.lang.String getServletContextName() {
		// return null;//"ROOT";
		throw new RuntimeException("getServletContextName is not supported.");
	}

	/**
	 * Returns a URL to the resource that is mapped to a specified path. The
	 * path must begin with a "/" and is interpreted as relative to the current
	 * context root.
	 * 
	 * <p>
	 * This method allows the servlet container to make a resource available to
	 * servlets from any source. Resources can be located on a local or remote
	 * file system, in a database, or in a <code>.war</code> file.
	 * 
	 * <p>
	 * The servlet container must implement the URL handlers and
	 * <code>URLConnection</code> objects that are necessary to access the
	 * resource.
	 * 
	 * <p>
	 * This method returns <code>null</code> if no resource is mapped to the
	 * pathname.
	 * 
	 * <p>
	 * Some containers may allow writing to the URL returned by this method
	 * using the methods of the URL class.
	 * 
	 * <p>
	 * The resource content is returned directly, so be aware that requesting a
	 * <code>.jsp</code> page returns the JSP source code. Use a
	 * <code>RequestDispatcher</code> instead to include results of an
	 * execution.
	 * 
	 * <p>
	 * This method has a different purpose than
	 * <code>java.lang.Class.getResource</code>, which looks up resources
	 * based on a class loader. This method does not use class loaders.
	 * 
	 * @param path
	 *            a <code>String</code> specifying the path to the resource
	 * 
	 * @return the resource located at the named path, or <code>null</code> if
	 *         there is no resource at that path
	 * 
	 * @exception MalformedURLException
	 *                if the pathname is not given in the correct form
	 * 
	 * 
	 */
	public URL getResource(String path) throws MalformedURLException {
		if (path == null || path.length() == 0 || path.charAt(0) != '/')
			throw new MalformedURLException("Path " + path
					+ " is not in acceptable form.");
		return new URL(hostProtocol, hostName, port, path);
	}

	/**
	 * Returns the resource located at the named path as an
	 * <code>InputStream</code> object.
	 * 
	 * <p>
	 * The data in the <code>InputStream</code> can be of any type or length.
	 * The path must be specified according to the rules given in
	 * <code>getResource</code>. This method returns <code>null</code> if
	 * no resource exists at the specified path.
	 * 
	 * <p>
	 * Meta-information such as content length and content type that is
	 * available via <code>getResource</code> method is lost when using this
	 * method.
	 * 
	 * <p>
	 * The servlet container must implement the URL handlers and
	 * <code>URLConnection</code> objects necessary to access the resource.
	 * 
	 * <p>
	 * This method is different from
	 * <code>java.lang.Class.getResourceAsStream</code>, which uses a class
	 * loader. This method allows servlet containers to make a resource
	 * available to a servlet from any location, without using a class loader.
	 * 
	 * 
	 * @param path
	 *            a <code>String</code> specifying the path to the resource
	 * 
	 * @return the <code>InputStream</code> returned to the servlet, or
	 *         <code>null</code> if no resource exists at the specified path
	 * 
	 * 
	 */
	public InputStream getResourceAsStream(String path) {
		try {
			return getResource(path).openStream();
		} catch (Exception e) {
		}
		return null;
	}

	public RequestDispatcher getRequestDispatcher(String urlpath) {
		return this; // we don't provide resource dispatching in this way
	}

	// no way to specify parameters for context
	public String getInitParameter(String param) {
		return null;
	}

	public Enumeration getInitParameterNames() {
		return null;
	}

	public RequestDispatcher getNamedDispatcher(String name) {
		return this;
	}

	synchronized String generateSessionId() {
		return "-" + System.currentTimeMillis() + '-' + (uniqer++) + '-'
				+ Math.round(Math.random() * 1000);
	}

	public void forward(ServletRequest _request, ServletResponse _response)
			throws ServletException, java.io.IOException {
		throw new RuntimeException("forward() not supported");
	}

	public void include(ServletRequest _request, ServletResponse _response)
			throws ServletException, java.io.IOException {
		throw new RuntimeException("include() not supported");
	}

	// Keep Alive supporter, JDK 1.4 based for backwar compatibility
	class KeepAliveCleaner extends Thread {
		protected List connections;

		protected List ingoings;

		protected boolean stopped;

		KeepAliveCleaner() {
			super("KeepAlive cleaner");
			connections = new ArrayList();
			ingoings = new ArrayList();
			setDaemon(true);
		}

		synchronized void addConnection(ServeConnection conn) {
			synchronized (ingoings) {
				if (stopped == false)
					ingoings.add(conn);
			}
		}

		public void run() {
			long d = getKeepAliveDuration();
			int maxUse = getMaxTimesConnectionUse();
			while (true) {
				synchronized (ingoings) {
					Iterator i = ingoings.iterator();
					while (i.hasNext()) {
						connections.add(i.next());
						i.remove();
					}
				}
				Iterator i = connections.iterator();
				long ct = System.currentTimeMillis();
				d = getKeepAliveDuration();
				while (i.hasNext()) {
					ServeConnection conn = (ServeConnection) i.next();
					if (conn.socket.isClosed()
							|| (conn.keepAlive == false
									|| (ct - conn.lastWait > d && conn.lastRun < conn.lastWait)) || stopped
							/*|| conn.timesRequested > maxUse*/) {
						i.remove();
						try {
							conn.socket.close();
						} catch (IOException ioe) {
							// ignore
						}
					}
				}
				if (stopped && connections.size() == 0)
					break;
				try {
					sleep(d);
				} catch (InterruptedException ie) {
					stopped = true; // not thread safe
				}
			}
		}
	}

	final static class Identification {
		public static final String serverName = "Rogatkin's JWS based on Acme.Serve";

		public static final String serverVersion = "Version 1.13, $Revision: 1.72 $";

		public static final String serverUrl = "http://tjws.sourceforge.net";

		// / Write a standard-format HTML address for this server.
		public static void writeAddress(OutputStream o) throws IOException {
			PrintStream p = new PrintStream(o);
			p.println("<ADDRESS><A HREF=\"" + serverUrl + "\">" + serverName
					+ " " + serverVersion + "</A></ADDRESS>");
		}

		public static void writeAddress(StringBuffer sb) throws IOException {
			sb.append("<ADDRESS><A HREF=\"" + serverUrl + "\">" + serverName
					+ " " + serverVersion + "</A></ADDRESS>");
		}
	}

	// ////////////////////////////////////////////////////////////////

	protected static class ServeConfig implements ServletConfig {

		private ServletContext context;

		private Hashtable init_params;

		private String servletName;

		public ServeConfig(ServletContext context) {
			this(context, null, "undefined");
		}

		public ServeConfig(ServletContext context, Hashtable initParams,
				String servletName) {
			this.context = context;
			this.init_params = initParams;
			this.servletName = servletName;
		}

		// Methods from ServletConfig.

		// / Returns the context for the servlet.
		public ServletContext getServletContext() {
			return context;
		}

		// / Gets an initialization parameter of the servlet.
		// @param name the parameter name
		public String getInitParameter(String name) {
			// This server supports servlet init params. :)
			if (init_params != null)
				return (String) init_params.get(name);
			return null;
		}

		// / Gets the names of the initialization parameters of the servlet.
		// @param name the parameter name
		public Enumeration getInitParameterNames() {
			// This server does:) support servlet init params.
			if (init_params != null)
				return init_params.keys();
			return new Vector().elements();
		}

		// 2.2
		public String getServletName() {
			return servletName;
		}
	}

	// /////////////////////////////////////////////////////////////////////
	/**
	 * provides request/response
	 */
	public static class ServeConnection implements Runnable,
			HttpServletRequest, HttpServletResponse {
		private Socket socket;

		private Serve serve;

		private ServletInputStream in;

		private ServletOutputStream out;

		public final static String WWWFORMURLENCODE = "application/x-www-form-urlencoded";

		public final static String TRANSFERENCODING = "transfer-encoding".toLowerCase();

		public final static String KEEPALIVE = "Keep-Alive".toLowerCase();

		public final static String CONNECTION = "Connection".toLowerCase();

		public final static String CHUNKED = "chunked";

		public final static String CONTENTLENGTH = "Content-Length".toLowerCase();

		public final static String CONTENTTYPE = "Content-Type".toLowerCase();

		public final static String SETCOOKIE = "Set-Cookie".toLowerCase();
		
		public final static String HOST = "Host".toLowerCase();

		public final static String COOKIE = "Cookie".toLowerCase();
		
		public final static String ACCEPT_LANGUAGE = "Accept-Language".toLowerCase();

		public final static String SESSION_COOKIE_NAME = "JSESSIONID";
		
		public final static String SESSION_URL_NAME = ";$sessionid$"; // ;jsessionid=

		private static final Map EMPTYHASHTABLE = new Hashtable();

		// URL rewriting
		// http://www.myserver.com/catalog/index.html;jsessionid=mysession1928
		// like:
		// http://www.sun.com/2001-0227/sunblade/;$sessionid$AD5RQ0IAADJAZAMTA1LU5YQ

		private String reqMethod; // == null by default

		private String reqUriPath;

		private String reqProtocol;

		private String charEncoding; // req and resp

		private String remoteUser;

		private String authType;

		private boolean oneOne; // HTTP/1.1 or better

		private boolean reqMime;

		private Vector reqHeaderNames = new Vector();

		private Vector reqHeaderValues = new Vector();

		private Locale locale; // = java.util.Locale.getDefault();

		private int uriLen;

		protected boolean keepAlive = true;

		protected int timesRequested;

		protected long lastRun, lastWait;

		private Vector outCookies;

		private Vector inCookies;

		private String sessionCookieValue, sessionUrlValue, sessionValue;

		protected String reqQuery;

		private PrintWriter pw;

		private ServletOutputStream rout;

		private Map formParameters;

		private Hashtable attributes = new Hashtable();

		private int resCode = -1;

		private String resMessage;

		private Hashtable resHeaderNames = new Hashtable();
		
		private String[] postCache;

		private boolean headersWritten;

		/*
		 * protected void finalize() throws Throwable { serve.log("Connection
		 * collected"); super.finalize(); }
		 */

		// / Constructor.
		public ServeConnection(Socket socket, Serve serve) {
			// Save arguments.
			this.socket = socket;
			this.serve = serve;

			if (serve.keepAliveCleaner != null)
				serve.keepAliveCleaner.addConnection(this);

			// Start a separate thread to read and handle the request.
			Thread thread = new Thread(serve.serverThreads, this,
					"Request handler");
			thread.setDaemon(true);
			thread.start();
		}

		private void restart() {
			reqMethod = null;
			reqUriPath = null;
			reqProtocol = null;
			charEncoding = null;
			remoteUser = null;
			authType = null;
			oneOne = false;
			reqMime = false;
			// considering that clear() works faster than new
			if (reqHeaderNames==null)
				reqHeaderNames = new Vector();
			else
				reqHeaderNames.clear();
			if (reqHeaderValues == null)
				reqHeaderValues = new Vector();
			else
				reqHeaderValues.clear();
			locale = null;
			uriLen = 0;
			outCookies = null;
			inCookies = null;
			sessionCookieValue = null;
			sessionUrlValue = null;
			sessionValue = null;
			reqQuery = null;
			pw = null;
			rout = null;
			formParameters = null;
			if (attributes == null)
				attributes = new Hashtable();
			else
				attributes.clear();
			resCode = -1;
			resMessage = null;
			resHeaderNames.clear();
			headersWritten = false;
			((ServeInputStream)in).refresh();
			((ServeOutputStream)out).refresh();
		}

		// Methods from Runnable.
		public void run() {
			try {
				in = new ServeInputStream(socket.getInputStream());
				out = new ServeOutputStream(socket.getOutputStream(), this);
				do {
					restart();
					// Get the streams.
					parseRequest();
					if (serve.isAccessLogged()) {
						serve.log(socket.getInetAddress().toString()
								+ ' '
								+ reqMethod
								+ ' '
								+ reqUriPath
								+ ' '
								+ resCode
								+ (serve.isShowReferer() ? "| "
										+ getHeader("Referer") : "")
								+ (serve.isShowUserAgent() ? "| "
										+ getHeader("User-Agent") : ""));
					}
					lastRun = 0;
					timesRequested++;
				} while (keepAlive && serve.isKeepAlive() && timesRequested < serve.getMaxTimesConnectionUse());
			} catch (IOException ioe) {
				serve.log(ioe.getMessage());
			} finally {			        
				try {
					socket.close();
				} catch (IOException e) { /* ignore */
				}
			}
		}

		private void parseRequest() throws IOException {			
			byte[] lineBytes = new byte[4096];
			int len;
			String line;
			/// TODO put time mark here for start waiting for receiving requests
			lastWait = System.currentTimeMillis();
			// Read the first line of the request.
			len = in.readLine(lineBytes, 0, lineBytes.length);
			if (len == -1 || len == 0) {
				if (keepAlive)
					throw new IOException("Inconsistent input data");
				else {
					problem("Status-Code 400: Bad Request(empty)",
							SC_BAD_REQUEST);
					return;
				}
			}
			if (len >= lineBytes.length) {
				problem("Status-Code 414: Request-URI Too Long",
						SC_REQUEST_URI_TOO_LONG);
				return;
			}
			////lastRun = 0; // to avoid closing socket in long process
			// TODO: solve race condition when socket is closed at starting
			// getting data
			// TODO: encoding is unknown at this point, assume UTF-8
			// TODO: URL encoding can be applied then, so it should be handled
			// differently
			// like urldecode to bytes, then apply utf
			line = new String(lineBytes, 0, len, UTF8);
			StringTokenizer ust = new StringTokenizer(line);
			reqProtocol = null;
			if (ust.hasMoreTokens()) {
				reqMethod = ust.nextToken();
				if (ust.hasMoreTokens()) {
					reqUriPath = ust.nextToken();
					// TODO make it only when URL overwrite enambled
					int uop = reqUriPath.indexOf(SESSION_URL_NAME);
					if (uop > 0) {
						sessionUrlValue = reqUriPath.substring(uop+SESSION_URL_NAME.length());
						reqUriPath = reqUriPath.substring(0, uop);
						try {
							((AcmeSession) serve
												.getSession(sessionUrlValue))
											 .userTouch();
						} catch (NullPointerException npe) {
						}
					}
					if (ust.hasMoreTokens()) {
						reqProtocol = ust.nextToken();
						oneOne = !reqProtocol.toUpperCase().equals("HTTP/1.0");
						reqMime = true;
						// Read the rest of the lines.
						String s;
						while ((s = ((ServeInputStream) in).readLine()) != null) {
							if (s.length() == 0)
								break;
							int c = s.indexOf(':', 0);
							if (c > 0) {
								String key = s.substring(0, c).trim()
										.toLowerCase();
								String value = s.substring(c + 1).trim();
								reqHeaderNames.addElement(key);
								reqHeaderValues.addElement(value);
								if (CONNECTION.equalsIgnoreCase(key))
									if (oneOne)
										keepAlive = "close"
												.equalsIgnoreCase(value) == false;
									else
										keepAlive = KEEPALIVE.equalsIgnoreCase(value);
							} else
								serve.log("header field '" + s
										+ "' without ':'");
						}
					} else {
						reqProtocol = "HTTP/0.9";
						oneOne = false;
						reqMime = false;
					}
				}
			}
			if (reqProtocol == null) {
				problem("Malformed request line", SC_BAD_REQUEST);
				return;
			}
			// Check Host: header in HTTP/1.1 requests.
			if (oneOne) {
				String host = getHeader(HOST);
				if (host == null) {
					problem("'Host' header missing on HTTP/1.1 request",
							SC_BAD_REQUEST);
					return;
				}
			}

			// Split off query string, if any.
			int qmark = reqUriPath.indexOf('?');
			if (qmark > -1) {
				reqQuery = reqUriPath.substring(qmark + 1);
				reqUriPath = URLDecoder.decode(reqUriPath.substring(0, qmark), UTF8);
			} else
				reqUriPath = URLDecoder.decode(reqUriPath, UTF8);
			if (CHUNKED.equals(getHeader(TRANSFERENCODING))) {
				setHeader(CONTENTLENGTH, null);
				((ServeInputStream) in).chunking(true);
			}

			String contentLength = getHeader(CONTENTLENGTH);
			if (contentLength != null)
				try {
						((ServeInputStream) in).setContentLength(Long.parseLong(contentLength));
				}catch(NumberFormatException nfe) {
					serve.log("Invalid value of input content-length: "+ contentLength);
				}
			if (keepAlive && serve.isKeepAlive()) {
				if (socket.getKeepAlive() == false)
					socket.setKeepAlive(true);
				// socket.setSoTimeout(0);
				if (reqMime) {
					setHeader(CONNECTION, KEEPALIVE); // set for 1.1 too, because some client do not follow a standard
					if (oneOne)
						setHeader(KEEPALIVE, serve.getKeepAliveParamStr());
				}
			} else
				setHeader(CONNECTION, "close");
			if (reqMime)
				setHeader("MIME-Version", "1.0");
			Object[] os = serve.registry.get(reqUriPath);
			if (os[0] != null) { // note, os always not null
				/// TODO put time mark here to monitor actual servicing
				lastRun = System.currentTimeMillis();
				// System.err.println("Servlet "+os[0]+" for path "+reqUriPath);
				uriLen = ((Integer) os[1]).intValue();
				runServlet((HttpServlet) os[0]);
			} else {
				problem("No any servlet found for serving " + reqUriPath,
						SC_BAD_REQUEST);
			}
		}

		private void runServlet(HttpServlet servlete) {
			// Set default response fields.
			setStatus(SC_OK);
			setDateHeader("Date", System.currentTimeMillis());
			setHeader("Server", Serve.Identification.serverName + "/"
					+ Serve.Identification.serverVersion);
			try {
				parseCookies();
				if (authenificate()) {
					if (servlete instanceof SingleThreadModel)
						synchronized (servlete) {
							servlete.service((ServletRequest) this,
									(ServletResponse) this);
						}
					else
						servlete.service((ServletRequest) this,
								(ServletResponse) this);
				} //else { // ???
				if (pw != null)
					pw.flush();
				out.flush();
				out.close();
				//}  // ???
			} catch (IOException e) {
				serve.log("IO problem running servlet", e);
				problem("IO problem running servlet: " + e.toString(),
						SC_BAD_REQUEST);
			} catch (ServletException e) {
				problem(e.toString(), SC_INTERNAL_SERVER_ERROR);
			} catch (Exception e) {
				serve.log("Unexpected problem running servlet", e);
				problem("Unexpected problem running servlet: " + e.toString(),
						SC_INTERNAL_SERVER_ERROR);
			}
		}

		private boolean authenificate() throws IOException {
			Object[] o = serve.realms.get(reqUriPath); // by Niel Markwick
			BasicAuthRealm realm = null;
			if (o != null)
				realm = (BasicAuthRealm) o[0];
			// System.err.println("looking for realm for path "+getPathInfo()+"
			// in
			// "+serve.realms+" found "+realm);
			if (realm == null)
				return true;

			String credentials = getHeader("Authorization");

			if (credentials != null) {
				credentials = Acme.Utils.base64Decode(credentials
						.substring(credentials.indexOf(' ') + 1),
						getCharacterEncoding());
				int i = credentials.indexOf(':');
				String user = credentials.substring(0, i);
				String password = credentials.substring(i + 1);
				remoteUser = user;
				authType = "Basic"; // support only basic authenification
				String realPassword = (String) realm.get(user);
				// System.err.println("User "+user+" Password "+password+" real
				// "+realPassword);
				if (realPassword != null && realPassword.equals(password))
					return true;
			}

			setStatus(SC_UNAUTHORIZED);
			setHeader("WWW-Authenticate", "basic realm=\"" + realm.name() + '"');
			writeHeaders();
			return false;
		}

		private void problem(String logMessage, int resCode) {
			serve.log(logMessage);
			try {
				sendError(resCode, logMessage);
			} catch (IllegalStateException e) { /* ignore */
			} catch (IOException e) { /* ignore */
			}
		}

		private String decode(String str) {
			StringBuffer result = new StringBuffer();
			int l = str.length();
			for (int i = 0; i < l; ++i) {
				char c = str.charAt(i);
				if (c == '%' && i + 2 < l) {
					char c1 = str.charAt(i + 1);
					char c2 = str.charAt(i + 2);
					if (isHexit(c1) && isHexit(c2)) {
						result.append((char) (hexit(c1) * 16 + hexit(c2)));
						i += 2;
					} else
						result.append(c);
				} else if (c == '+')
					result.append(' ');
				else
					result.append(c);
			}
			return result.toString();
		}

		private boolean isHexit(char c) {
			String legalChars = "0123456789abcdefABCDEF";
			return (legalChars.indexOf(c) != -1);
		}

		private int hexit(char c) {
			if (c >= '0' && c <= '9')
				return c - '0';
			if (c >= 'a' && c <= 'f')
				return c - 'a' + 10;
			if (c >= 'A' && c <= 'F')
				return c - 'A' + 10;
			return 0; // shouldn't happen, we're guarded by isHexit()
		}

		void parseCookies() {
			if (inCookies == null)
				inCookies = new Vector();
			try {
				String cookie_name;
				String cookie_value;
				String cookie_path;
				String cookies = getHeader(COOKIE);
				if (cookies == null)
					return;
				// Enumeration e = getHeaders(COOKIE);
				// while(e.hasMoreElements())
				// cookies += (String)e.nextElement();
				StringTokenizer st = new StringTokenizer(cookies, ";", true);
				// TODO: write a parser to avoid tokenizers
				while (st.hasMoreTokens()) {
					StringTokenizer st2 = new StringTokenizer(st.nextToken(),
							"=");
					if (st2.hasMoreTokens()) {
						cookie_name = st2.nextToken().trim();
						if (st2.hasMoreTokens()) {
							cookie_value = st2.nextToken(",").trim();
							if (cookie_value.length() > 0
									&& cookie_value.charAt(0) == '=')
								cookie_value = cookie_value.substring(1);
							cookie_path = "/";
							while (st2.hasMoreTokens()) {
								String cookie_atr = st2.nextToken();
								if ("$Version".equalsIgnoreCase(cookie_atr)
										|| "$Path".equalsIgnoreCase(cookie_atr)
										|| "$Domain"
												.equalsIgnoreCase(cookie_atr))
									continue;
								cookie_path = st2.nextToken();
							}
							Cookie cookie = new Cookie(cookie_name,
									cookie_value);
							// System.err.println("Cookie
							// set:"+cookie_name+':'+cookie_value);
							cookie.setPath(cookie_path);
							inCookies.addElement(cookie);
							if (SESSION_COOKIE_NAME.equals(cookie_name)
									&& sessionCookieValue == null && sessionUrlValue == null) {
								sessionCookieValue = cookie_value;
								try {
									((AcmeSession) serve
											.getSession(sessionCookieValue))
											.userTouch();
									sessionValue = sessionCookieValue;
								} catch (NullPointerException npe) {
									sessionCookieValue = null;
								}
							}
						}
					}
				}
			} catch (Throwable e) {
				serve.log("prepareCookies(): ", e);
			}
		}

		// Methods from ServletRequest.

		// / Returns the size of the request entity data, or -1 if not known.
		// Same as the CGI variable CONTENT_LENGTH.
		public int getContentLength() {
			return getIntHeader(CONTENTLENGTH);
		}

		// / Returns the MIME type of the request entity data, or null if
		// not known.
		// Same as the CGI variable CONTENT_TYPE.
		public String getContentType() {
			return getHeader(CONTENTTYPE);
		}

		// / Returns the protocol and version of the request as a string of
		// the form <protocol>/<major version>.<minor version>.
		// Same as the CGI variable SERVER_PROTOCOL.
		public String getProtocol() {
			return reqProtocol;
		}

		// / Returns the scheme of the URL used in this request, for example
		// "http", "https", or "ftp". Different schemes have different rules
		// for constructing URLs, as noted in RFC 1738. The URL used to create
		// a request may be reconstructed using this scheme, the server name
		// and port, and additional information such as URIs.
		public String getScheme() {
			return serve.hostProtocol;
		}

		// / Returns the host name of the server as used in the <host> part of
		// the request URI.
		// Same as the CGI variable SERVER_NAME.
		public String getServerName() {
			String serverName;
			serverName = getHeader(HOST);
			if (serverName != null && serverName.length() > 0) {
				int colon = serverName.indexOf(':');
				if (colon >= 0) {
					if (colon < serverName.length())
						serverName = serverName.substring(0, colon);
				}
			}

			if (serverName == null) {
				try {
					serverName = InetAddress.getLocalHost().getHostName();
				} catch (java.net.UnknownHostException ignore) {
					serverName = "127.0.0.0";
				}
			}

			int slash = serverName.indexOf("/");
			if (slash >= 0)
				serverName = serverName.substring(slash + 1);
			return serverName;
		}

		// / Returns the port number on which this request was received as used
		// in
		// the <port> part of the request URI.
		// Same as the CGI variable SERVER_PORT.
		public int getServerPort() {
			return socket.getLocalPort();
		}

		// / Returns the IP address of the agent that sent the request.
		// Same as the CGI variable REMOTE_ADDR.
		public String getRemoteAddr() {
			return socket.getInetAddress().toString();
		}

		// / Returns the fully qualified host name of the agent that sent the
		// request.
		// Same as the CGI variable REMOTE_HOST.
		public String getRemoteHost() {
			String result = socket.getInetAddress().getHostName();
			return result != null ? result : getRemoteAddr();
		}

		// / Applies alias rules to the specified virtual path and returns the
		// corresponding real path, or null if the translation can not be
		// performed for any reason. For example, an HTTP servlet would
		// resolve the path using the virtual docroot, if virtual hosting is
		// enabled, and with the default docroot otherwise. Calling this
		// method with the string "/" as an argument returns the document root.
		public String getRealPath(String path) {
			return serve.getRealPath(path);
		}

		// / Returns an input stream for reading request data.
		// @exception IllegalStateException if getReader has already been called
		// @exception IOException on other I/O-related errors
		public ServletInputStream getInputStream() throws IOException {
			synchronized (in) {
				if (((ServeInputStream) in).isReturnedAsReader())
					throw new IllegalStateException(
							"Already returned as a reader.");
				((ServeInputStream) in).setReturnedAsReader(true);
			}
			return in;
		}

		// / Returns a buffered reader for reading request data.
		// @exception UnsupportedEncodingException if the character set encoding
		// isn't supported
		// @exception IllegalStateException if getInputStream has already been
		// called
		// @exception IOException on other I/O-related errors
		public BufferedReader getReader() {
			synchronized (in) {
				if (((ServeInputStream) in).isReturnedAsStream())
					throw new IllegalStateException(
							"Already returned as a stream.");
				((ServeInputStream) in).setReturnedAsStream(true);
			}
			if (charEncoding != null)
				try {
					return new BufferedReader(new InputStreamReader(in,
							charEncoding));
				} catch (UnsupportedEncodingException uee) {
				}
			return new BufferedReader(new InputStreamReader(in));
		}

		private synchronized Map getParametersFromRequest() {
			Map result = null;
			// System.out.println("Req:"+reqMethod+" con:"+getContentType()+" eq
			// "+WWWFORMURLENCODE.equals(getContentType()));
			if ("GET".equals(reqMethod)) {
				if (reqQuery != null)
					try {
						result = Acme.Utils.parseQueryString(reqQuery, charEncoding);
					} catch (IllegalArgumentException ex) {
						serve.log("Exception " + ex + " at parsing 'get' data "
								+ reqQuery);
					}
			} else if ("POST".equals(reqMethod))
				if (WWWFORMURLENCODE.equals(getContentType())) {
					if (postCache == null) {
						postCache = new String[1];
						InputStream is = null;
						try {
							result = Acme.Utils.parsePostData(getContentLength(),
									is = getInputStream(), charEncoding, postCache);
						} catch (Exception ex) {
							serve.log("Exception " + ex
									+ " at parsing 'post' data of length "
									+ getContentLength());
						} finally {
							if (is != null)
								try {
									is.close();
								} catch(IOException ioe) {}
						}
					} else
						result = Acme.Utils.parseQueryString(postCache[0], charEncoding);
					if (reqQuery != null && reqQuery.length() > 0) 
						result.putAll(Acme.Utils.parseQueryString(reqQuery, charEncoding));
				} else
					if (reqQuery != null)
						result = Acme.Utils.parseQueryString(reqQuery, charEncoding);
			return result != null ? result : EMPTYHASHTABLE;
		}

		// / Returns the parameter names for this request.
		public synchronized Enumeration getParameterNames() {
			if (formParameters == null)
				formParameters = getParametersFromRequest();
			return ((Hashtable)formParameters).keys();
		}

		// / Returns the value of the specified query string parameter, or null
		// if not found.
		// @param name the parameter name
		public String getParameter(String name) {
			String[] params = getParameterValues(name);
			if (params == null || params.length == 0)
				return null;

			return params[0];
		}

		// / Returns the values of the specified parameter for the request as an
		// array of strings, or null if the named parameter does not exist.
		public synchronized String[] getParameterValues(String name) {
			if (formParameters == null)
				getParameterNames();

			return (String[]) formParameters.get(name);
		}

		// / Returns the value of the named attribute of the request, or null if
		// the attribute does not exist. This method allows access to request
		// information not already provided by the other methods in this
		// interface.
		public Object getAttribute(String name) {
			return attributes.get(name);
		}

		// Methods from HttpServletRequest.

		// / Gets the array of cookies found in this request.
		public Cookie[] getCookies() {
			Cookie[] cookieArray = new Cookie[inCookies.size()];
			inCookies.copyInto(cookieArray);
			return cookieArray;
		}

		// / Returns the method with which the request was made. This can be
		// "GET",
		// "HEAD", "POST", or an extension method.
		// Same as the CGI variable REQUEST_METHOD.
		public String getMethod() {
			return reqMethod;
		}

		/***********************************************************************
		 * Returns the part of this request's URL from the protocol name up to
		 * the query string in the first line of the HTTP request. To
		 * reconstruct an URL with a scheme and host, use
		 * HttpUtils.getRequestURL(javax.servlet.http.HttpServletRequest).
		 */
		// / Returns the full request URI.
		public String getRequestURI() {
			return reqUriPath;
		}

		/**
		 * Reconstructs the URL the client used to make the request. The
		 * returned URL contains a protocol, server name, port number, and
		 * server path, but it does not include query string parameters. <br>
		 * Because this method returns a StringBuffer, not a string, you can
		 * modify the URL easily, for example, to append query parameters.
		 * <p>
		 * This method is useful for creating redirect messages and for
		 * reporting errors.
		 * 
		 * @return a StringBuffer object containing the reconstructed URL
		 * @since 2.3
		 */
		public java.lang.StringBuffer getRequestURL() {
			return new StringBuffer().append(getScheme()).append("://").append(
					serve.hostName).append(
					"https".equals(getScheme()) && serve.port == 443
							|| serve.port == 80 ? "" : ":"
							+ String.valueOf(serve.port)).append(
					getRequestURI());
		}

		// / Returns the part of the request URI that referred to the servlet
		// being
		// invoked.
		// Analogous to the CGI variable SCRIPT_NAME.
		public String getServletPath() {
			// In this server, the entire path is regexp-matched against the
			// servlet pattern, so there's no good way to distinguish which
			// part refers to the servlet.
			return uriLen > 0 ? reqUriPath.substring(0, uriLen) : "";
		}

		// / Returns optional extra path information following the servlet path,
		// but
		// immediately preceding the query string. Returns null if not
		// specified.
		// Same as the CGI variable PATH_INFO.
		public String getPathInfo() {
			// In this server, the entire path is regexp-matched against the
			// servlet pattern, so there's no good way to distinguish which
			// part refers to the servlet.
			return uriLen >= reqUriPath.length() ? null : reqUriPath
					.substring(uriLen);
		}

		// / Returns extra path information translated to a real path. Returns
		// null if no extra path information was specified.
		// Same as the CGI variable PATH_TRANSLATED.
		public String getPathTranslated() {
			// In this server, the entire path is regexp-matched against the
			// servlet pattern, so there's no good way to distinguish which
			// part refers to the servlet.
			return getRealPath(getPathInfo());
		}

		// / Returns the query string part of the servlet URI, or null if not
		// known.
		// Same as the CGI variable QUERY_STRING.
		public String getQueryString() {
			return reqQuery;
		}

		// / Returns the name of the user making this request, or null if not
		// known.
		// Same as the CGI variable REMOTE_USER.
		public String getRemoteUser() {
			return remoteUser;
		}

		// / Returns the authentication scheme of the request, or null if none.
		// Same as the CGI variable AUTH_TYPE.
		public String getAuthType() {
			return authType;
		}

		// / Returns the value of a header field, or null if not known.
		// Same as the information passed in the CGI variabled HTTP_*.
		// @param name the header field name
		public String getHeader(String name) {
			int i = reqHeaderNames.indexOf(name.toLowerCase());
			if (i == -1)
				return null;
			return (String) reqHeaderValues.elementAt(i);
		}

		public int getIntHeader(String name) {
			String val = getHeader(name);
			if (val == null)
				return -1;
			return Integer.parseInt(val);
		}

		public long getDateHeader(String name) {
			String val = getHeader(name);
			if (val == null)
				return -1;
			try {
				return headerdateformat.parse(val).getTime();
			} catch (Exception e) {
				throw new IllegalArgumentException("Value " + val
						+ " can't be converted to Date using "
						+ headerdateformat.toPattern());
			}
		}

		// / Returns an Enumeration of the header names.
		public Enumeration getHeaderNames() {
			return reqHeaderNames.elements();
		}

		// / Gets the current valid session associated with this request, if
		// create is false or, if necessary, creates a new session for the
		// request, if create is true.
		// <P>
		// Note: to ensure the session is properly maintained, the servlet
		// developer must call this method (at least once) before any output
		// is written to the response.
		// <P>
		// Additionally, application-writers need to be aware that newly
		// created sessions (that is, sessions for which HttpSession.isNew
		// returns true) do not have any application-specific state.
		public synchronized HttpSession getSession(boolean create) {
			HttpSession result = null;
			if (sessionValue != null) {
				result = (HttpSession) serve.getSession(sessionValue);
				if (result != null && ((AcmeSession) result).isValid() == false) {
					serve.removeSession(sessionValue);
					result = null;
				}
			}
			if (result == null && create) {
				result = serve.createSession();
				if (result != null) {
					sessionValue = result.getId();
				}
			}
			return result;
		}

		// JSDK 2.1
		public HttpSession getSession() {
			return getSession(true);
		}

		public boolean isRequestedSessionIdFromURL() {
			return false;
		}

		// from ServletRequest
		public Enumeration getAttributeNames() {
			return attributes.keys();
		}

		public void setAttribute(String key, Object o) {
			if (o != null)
				attributes.put(key, o);
			else
				attributes.remove(key);
		}

		// / Gets the session id specified with this request. This may differ
		// from the actual session id. For example, if the request specified
		// an id for an invalid session, then this will get a new session with
		// a new id.
		public String getRequestedSessionId() {
			return sessionValue;
		}

		// / Checks whether this request is associated with a session that is
		// valid in the current session context. If it is not valid, the
		// requested session will never be returned from the getSession
		// method.
		public boolean isRequestedSessionIdValid() {
			if (sessionValue != null) {
				AcmeSession session = (AcmeSession) serve
						.getSession(sessionValue);
				if (session != null && session.isValid()) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Checks whether the session id specified by this request came in as a
		 * cookie. (The requested session may not be one returned by the
		 * getSession method.)
		 */
		public boolean isRequestedSessionIdFromCookie() {
			return sessionCookieValue != null;
		}

		// / Checks whether the session id specified by this request came in as
		// part of the URL. (The requested session may not be the one returned
		// by the getSession method.)
		public boolean isRequestedSessionIdFromUrl() {
			return sessionUrlValue != null;
		}

		// Methods from ServletResponse.

		// / Sets the content length for this response.
		// @param length the content length
		public void setContentLength(int length) {
			setIntHeader(CONTENTLENGTH, length);
		}

		// / Sets the content type for this response.
		// @param type the content type
		public void setContentType(String type) {
			setHeader(CONTENTTYPE, type != null ? type : "Unknown");
		}

		// / Returns an output stream for writing response data.
		public ServletOutputStream getOutputStream() {
			synchronized (out) {
				if (rout == null) {
					if (pw != null)
						throw new IllegalStateException(
								"Already returned as a writer");
					rout = out;
				}
			}
			return rout;
		}

		// / Returns a print writer for writing response data. The MIME type of
		// the response will be modified, if necessary, to reflect the character
		// encoding used, through the charset=... property. This means that the
		// content type must be set before calling this method.
		// @exception UnsupportedEncodingException if no such encoding can be
		// provided
		// @exception IllegalStateException if getOutputStream has been called
		// @exception IOException on other I/O errors
		public PrintWriter getWriter() throws IOException {
			synchronized (out) {
				if (pw == null) {
					if (rout != null)
						throw new IllegalStateException(
								"Already was returned as servlet output stream");
					String encoding = getCharacterEncoding();
					if (encoding != null)
						pw = new PrintWriter(new OutputStreamWriter(out,
								encoding));
					else
						pw = new PrintWriter(out);
				}
			}
			return pw;
		}

		// / Returns the character set encoding used for this MIME body. The
		// character encoding is either the one specified in the assigned
		// content type, or one which the client understands. If no content
		// type has yet been assigned, it is implicitly set to text/plain.
		public String getCharacterEncoding() {
			String ct = (String) resHeaderNames.get(CONTENTTYPE.toLowerCase());
			if (ct != null) {
				int scp = ct.indexOf(';');
				if (scp > 0) {
					scp = ct.toLowerCase().indexOf("charset=", scp);
					if (scp >= 0) {
						ct = ct.substring(scp + 8);
						scp = ct.indexOf(' ');
						if (scp > 0)
							ct = ct.substring(0, scp);
						scp = ct.indexOf(';');
						if (scp > 0)
							ct = ct.substring(0, scp);
						int l = ct.length();
						if (ct.charAt(0) == '"' && l > 2)
							return ct.substring(1, l-1);
						return ct;
					}
				}
			}
			return charEncoding;
		}

		// 2.2
		// do not use buffer
		public void flushBuffer() {
		}

		/**
		 * Clears the content of the underlying buffer in the response without
		 * clearing headers or status code. If the response has been committed,
		 * this method throws an IllegalStateException.
		 * 
		 * @since 2.3
		 */
		public void resetBuffer() {
			throw new IllegalStateException("The method not implemented");
		}

		public int getBufferSize() {
			return 0;
		}

		public void setBufferSize(int size) {
		}

		/**
		 * Returns a boolean indicating if the response has been committed. A
		 * commited response has already had its status code and headers
		 * written.
		 * 
		 * @return a boolean indicating if the response has been committed
		 * @see setBufferSize(int), getBufferSize(), flushBuffer(), reset()
		 */
		// a caller should think about syncronization
		public boolean isCommitted() {
			return headersWritten;
		}

		/**
		 * Clears any data that exists in the buffer as well as the status code
		 * and headers. If the response has been committed, this method throws
		 * an IllegalStateException.
		 * 
		 * @throws java.lang.IllegalStateException -
		 *             if the response has already been committed
		 * @see setBufferSize(int), getBufferSize(), flushBuffer(),
		 *      isCommitted()
		 */
		public void reset() throws IllegalStateException {
			if (!isCommitted()) {
				if (outCookies != null)
					outCookies.clear();
				resHeaderNames.clear();
			} else
				throw new IllegalStateException(
						"Header have already been committed.");
		}

		/**
		 * Sets the locale of the response, setting the headers (including the
		 * Content-Type's charset) as appropriate. This method should be called
		 * before a call to getWriter(). By default, the response locale is the
		 * default locale for the server.
		 * 
		 * @param loc -
		 *            the locale of the response
		 * @see getLocale()
		 */
		public void setLocale(java.util.Locale locale) {
			this.locale = locale;
		}

		/**
		 * For request:
		 * Returns the preferred Locale that the client will accept content in,
		 * based on the Accept-Language header. If the client request doesn't
		 * provide an Accept-Language header, this method returns the 
		 * default locale for the server.
		 * 
		 * For response:
		 * Returns the locale specified for this response using the 
		 * setLocale(java.util.Locale) method. Calls made to setLocale 
		 * after the response is committed have no effect. If no locale has
		 * been specified, the container's default locale is returned.
		 */
		public java.util.Locale getLocale() {
			if (locale != null)
				return locale;
			Enumeration e = getLocales();
			if (e.hasMoreElements())
				return (Locale)e.nextElement();
			return Locale.getDefault();
		}

		/**
		 * Returns an Enumeration of Locale objects indicating, in decreasing
		 * order starting with the preferred locale, the locales that are
		 * acceptable to the client based on the Accept-Language header.
		 * If the client request doesn't provide an Accept-Language header,
		 * this method returns an Enumeration containing one Locale, the
		 * default locale for the server.
		 */
		public Enumeration getLocales() {
			// TODO: cache result
			String al = getHeader(ACCEPT_LANGUAGE);
			TreeSet ts = new TreeSet();
			if (al != null) {
				//System.err.println("Accept lang:"+al);
				StringTokenizer st =  new StringTokenizer(al, ";", false);				
				try {
					while(st.hasMoreTokens()) {
						String langs = st.nextToken(";");
						//System.err.println("Langs:"+langs);
						String q = st.nextToken(";=");
						//System.err.println("q:"+q);
						q = st.nextToken("=,");
						//System.err.println("q:"+q);
						float w = 0;
						try {
							w =  Float.valueOf(q).floatValue();
						} catch(NumberFormatException nfe) {
						}
						if (w > 0) {
							StringTokenizer lst =  new StringTokenizer(langs, ", ", false);	
							while(lst.hasMoreTokens()) {
								String lan = lst.nextToken();
								int di = lan.indexOf('-');
								if (di < 0)
									ts.add(new LocaleWithWeight(new Locale(lan.trim()), w));
								else
									ts.add(new LocaleWithWeight(new Locale(lan.substring(0,di),lan.substring(di+1).trim().toUpperCase()), w));
							}
						}
					}
				}catch(NoSuchElementException ncee) {
					// can't parse
				}
			}
			if (ts.size() == 0)
				ts.add(new LocaleWithWeight(Locale.getDefault(), 1));
			return new AcceptLocaleEnumeration(ts);
		}

		/**
		 * Overrides the name of the character encoding used in the body of this
		 * request. This method must be called prior to reading request
		 * parameters or reading input using getReader().
		 * 
		 * @param a -
		 *            String containing the name of the chararacter encoding.
		 * @throws java.io.UnsupportedEncodingException -
		 *             if this is not a valid encoding
		 * @since JSDK 2.3
		 */
		public void setCharacterEncoding(String _enc) {
			charEncoding = _enc;
			synchronized(this) {
				formParameters = null;
			}
		}

		public void addDateHeader(String header, long date) {
			addHeader(header, expdatefmt.format(new Date(date)));
		}

		public void addHeader(String header, String value) {
			header = header.trim().toLowerCase();
			Object o = resHeaderNames.get(header);
			if (o == null)
				setHeader(header, value);
			else {
				if (o instanceof String[]) {
					String[] oldVal = (String[]) o;
					String[] newVal = new String[oldVal.length + 1];
					System.arraycopy(oldVal, 0, newVal, 0, oldVal.length);
					newVal[oldVal.length] = value;
					resHeaderNames.put(header, newVal);
				} else if (o instanceof String) {
					String[] newVal = new String[2];
					newVal[0] = (String) o;
					newVal[1] = value;
					resHeaderNames.put(header, newVal);
				} else
					throw new RuntimeException(
							"Invalid content of header hash - "
									+ o.getClass().getName());
			}
		}

		public void addIntHeader(String header, int value) {
			addHeader(header, Integer.toString(value));
		}

		public RequestDispatcher getRequestDispatcher(String urlpath) {
			// TODO: calculate dispacter relatively the current path
			return null; // we don't provide resource dispatching in this way
		}

		public boolean isSecure() {
			return "https".equals(getScheme());
		}

		public void removeAttribute(String name) {
			attributes.remove(name);
		}

		// only root context supported
		public String getContextPath() {
			return "";
		}

		public Enumeration getHeaders(String header) {
			Vector result = new Vector();
			int i = -1;
			while ((i = reqHeaderNames.indexOf(header.toLowerCase(), i + 1)) >= 0)
				result.addElement(reqHeaderValues.elementAt(i));
			return result.elements();
		}

		public java.security.Principal getUserPrincipal() {
			return null;
		}

		public boolean isUserInRole(String user) {
			return false;
		}

		/**
		 * Returns a java.util.Map of the parameters of this request. Request
		 * parameters are extra information sent with the request. For HTTP
		 * servlets, parameters are contained in the query string or posted form
		 * data.
		 * 
		 * @return an immutable java.util.Map containing parameter names as keys
		 *         and parameter values as map values. The keys in the parameter
		 *         map are of type String. The values in the parameter map are
		 *         of type String array.
		 * @since 2.3
		 */
		public synchronized java.util.Map getParameterMap() {
			if (formParameters == null)
				getParameterNames();
			return formParameters;
		}

		// Methods from HttpServletResponse.

		// / Adds the specified cookie to the response. It can be called
		// multiple times to set more than one cookie.
		public void addCookie(Cookie cookie) {
			if (outCookies == null)
				outCookies = new Vector();

			outCookies.addElement(cookie);
		}

		// / Checks whether the response message header has a field with the
		// specified name.
		public boolean containsHeader(String name) {
			return resHeaderNames.contains(name);
		}

		// JSDK 2.1 extension
		public String encodeURL(String url) {
			int uop = url.indexOf(SESSION_URL_NAME);
			if (uop > 0)
				url = url.substring(0, uop);
			if (sessionValue == null)
				return url;
			return url+SESSION_URL_NAME+sessionValue;
		}

		public String encodeRedirectURL(String url) {
			// TODO: add session only if no host or the same as
			return encodeURL(url);
		}

		/**
		 * Returns the Internet Protocol (IP) source port of the client or last
		 * proxy that sent the request.
		 * 
		 * @return an integer specifying the port number
		 * 
		 * @since 2.4
		 */
		public int getRemotePort() {
			return serve.port;
		}

		/**
		 * Returns the host name of the Internet Protocol (IP) interface on
		 * which the request was received.
		 * 
		 * @return a <code>String</code> containing the host name of the IP on
		 *         which the request was received.
		 * 
		 * @since 2.4
		 */
		public String getLocalName() {
			InetAddress ia = socket/*serve.serverSocket*/.getInetAddress();
			return ia==null?null:ia.getHostAddress(); 
		}

		/**
		 * Returns the Internet Protocol (IP) address of the interface on which
		 * the request was received.
		 * 
		 * @return a <code>String</code> containing the IP address on which
		 *         the request was received.
		 * 
		 * @since 2.4
		 * 
		 */
		public String getLocalAddr() {
			InetAddress ia = /*serve.serverSocket*/socket.getInetAddress();
			return ia==null?null:ia.getCanonicalHostName(); 
		}

		/**
		 * Returns the Internet Protocol (IP) port number of the interface on
		 * which the request was received.
		 * 
		 * @return an integer specifying the port number
		 * 
		 * @since 2.4
		 */
		public int getLocalPort() {
			return serve.port;
		}

		protected static final SimpleDateFormat expdatefmt = new SimpleDateFormat("'GMT'", Locale.US);
		//protected static final SimpleDateFormat expdatefmt = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss 'GMT'", Locale.US);

		protected static final SimpleDateFormat headerdateformat = new SimpleDateFormat("'xGMT'", Locale.US);
		//protected static final SimpleDateFormat headerdateformat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

		static {
			TimeZone tz = TimeZone.getTimeZone("GMT");
			tz.setID("GMT");
			expdatefmt.setTimeZone(tz);
		}

		// / Sets the status code and message for this response.
		// @param resCode the status code
		// @param resMessage the status message
		public void setStatus(int resCode, String resMessage) {
			// if (this.resCode > 0 && this.resCode != SC_OK)
			// throw new IllegalStateException("Result code "+this.resCode+" was
			// already set.");
			this.resCode = resCode;
			this.resMessage = resMessage;
		}

		// / Sets the status code and a default message for this response.
		// @param resCode the status code
		public void setStatus(int resCode) {
			switch (resCode) {
			case SC_CONTINUE:
				setStatus(resCode, "Continue");
				break;
			case SC_SWITCHING_PROTOCOLS:
				setStatus(resCode, "Switching protocols");
				break;
			case SC_OK:
				setStatus(resCode, "Ok");
				break;
			case SC_CREATED:
				setStatus(resCode, "Created");
				break;
			case SC_ACCEPTED:
				setStatus(resCode, "Accepted");
				break;
			case SC_NON_AUTHORITATIVE_INFORMATION:
				setStatus(resCode, "Non-authoritative");
				break;
			case SC_NO_CONTENT:
				setStatus(resCode, "No content");
				break;
			case SC_RESET_CONTENT:
				setStatus(resCode, "Reset content");
				break;
			case SC_PARTIAL_CONTENT:
				setStatus(resCode, "Partial content");
				break;
			case SC_MULTIPLE_CHOICES:
				setStatus(resCode, "Multiple choices");
				break;
			case SC_MOVED_PERMANENTLY:
				setStatus(resCode, "Moved permanentently");
				break;
			case SC_MOVED_TEMPORARILY:
				setStatus(resCode, "Moved temporarily");
				break;
			case SC_SEE_OTHER:
				setStatus(resCode, "See other");
				break;
			case SC_NOT_MODIFIED:
				setStatus(resCode, "Not modified");
				break;
			case SC_USE_PROXY:
				setStatus(resCode, "Use proxy");
				break;
			case SC_BAD_REQUEST:
				setStatus(resCode, "Bad request");
				break;
			case SC_UNAUTHORIZED:
				setStatus(resCode, "Unauthorized");
				break;
			case SC_PAYMENT_REQUIRED:
				setStatus(resCode, "Payment required");
				break;
			case SC_FORBIDDEN:
				setStatus(resCode, "Forbidden");
				break;
			case SC_NOT_FOUND:
				setStatus(resCode, "Not found");
				break;
			case SC_METHOD_NOT_ALLOWED:
				setStatus(resCode, "Method not allowed");
				break;
			case SC_NOT_ACCEPTABLE:
				setStatus(resCode, "Not acceptable");
				break;
			case SC_PROXY_AUTHENTICATION_REQUIRED:
				setStatus(resCode, "Proxy auth required");
				break;
			case SC_REQUEST_TIMEOUT:
				setStatus(resCode, "Request timeout");
				break;
			case SC_CONFLICT:
				setStatus(resCode, "Conflict");
				break;
			case SC_GONE:
				setStatus(resCode, "Gone");
				break;
			case SC_LENGTH_REQUIRED:
				setStatus(resCode, "Length required");
				break;
			case SC_PRECONDITION_FAILED:
				setStatus(resCode, "Precondition failed");
				break;
			case SC_REQUEST_ENTITY_TOO_LARGE:
				setStatus(resCode, "Request entity too large");
				break;
			case SC_REQUEST_URI_TOO_LONG:
				setStatus(resCode, "Request URI too long");
				break;
			case SC_UNSUPPORTED_MEDIA_TYPE:
				setStatus(resCode, "Unsupported media type");
				break;
			case SC_INTERNAL_SERVER_ERROR:
				setStatus(resCode, "Internal server error");
				break;
			case SC_NOT_IMPLEMENTED:
				setStatus(resCode, "Not implemented");
				break;
			case SC_BAD_GATEWAY:
				setStatus(resCode, "Bad gateway");
				break;
			case SC_SERVICE_UNAVAILABLE:
				setStatus(resCode, "Service unavailable");
				break;
			case SC_GATEWAY_TIMEOUT:
				setStatus(resCode, "Gateway timeout");
				break;
			case SC_HTTP_VERSION_NOT_SUPPORTED:
				setStatus(resCode, "HTTP version not supported");
				break;
			case 207:
				setStatus(resCode, "Multi Status");
				break;
			default:
				setStatus(resCode, "");
				break;
			}
		}

		// / Sets the value of a header field.
		// @param name the header field name
		// @param value the header field value
		public void setHeader(String header, String value) {
			header = header.trim().toLowerCase(); // normilize header
			if (value == null)
				resHeaderNames.remove(header);
			else {
				resHeaderNames.put(header, value);
			}
		}

		// / Sets the value of an integer header field.
		// @param name the header field name
		// @param value the header field integer value
		public void setIntHeader(String header, int value) {
			setHeader(header, Integer.toString(value));
		}

		// / Sets the value of a long header field.
		// @param name the header field name
		// @param value the header field long value
		public void setLongHeader(String header, long value) {
			setHeader(header, Long.toString(value));
		}

		// / Sets the value of a date header field.
		// @param name the header field name
		// @param value the header field date value
		public void setDateHeader(String header, long value) {
			setHeader(header, expdatefmt.format(new Date(value)));
		}

		// / Writes the status line and message headers for this response to the
		// output stream.
		// @exception IOException if an I/O error has occurred
		void writeHeaders() throws IOException {
			synchronized (this) {
				// TODO: possible to write trailer when chunked out,
				// so chunked out should be global flag
				if (headersWritten)
					return;

				headersWritten = true;
			}
			if (reqMime) {
				boolean chunked_out = false;
				boolean wasContentLen = false;
				out.println(reqProtocol + " " + resCode + " " + resMessage);

				Enumeration he = resHeaderNames.keys();
				while (he.hasMoreElements()) {
					String name = (String) he.nextElement();
					Object o = resHeaderNames.get(name);
					if (o instanceof String) {
						String value = (String) o;
						if (value != null) {// just in case
							out.println(name + ": " + value);
							if (wasContentLen == false)
								if (CONTENTLENGTH.equals(name))
									try {
										wasContentLen = Long.parseLong(value) > 0;
									} catch (NumberFormatException nfe) {
									}
							if (chunked_out == false)
								if (TRANSFERENCODING.equals(name)
										&& CHUNKED.equals(value))
									chunked_out = true;
						}
					} else if (o instanceof String[]) {
						String[] values = (String[]) o;
						out.println(name + ": " + values[0]);
						for (int i = 0; i < values.length; i++)
							out.print("," + values[i]);
						out.println();
					}
				}
				StringBuffer sb = null;
				Cookie cc = null;
				// add session cookie
				// TODO if cookie based session
				if (sessionValue != null) {
					cc = new Cookie(SESSION_COOKIE_NAME, sessionValue);
					if (Serve.expiredIn < 0)
						cc.setMaxAge(Math.abs(Serve.expiredIn) * 60);
					addCookie(cc);
				}

				// how to remove a cookie
				// cc = new Cookie(cookieName, "");
				// cc.setMaxAge(0);
				//
				for (int i = 0; outCookies != null && i < outCookies.size(); i++) {
					if (sb == null)
						sb = new StringBuffer(SETCOOKIE + ": ");
					else
						// sb.append(',');
						sb.append("\r\n" + SETCOOKIE + ": "); // for IE not
					// understanding the
					// standard
					cc = (Cookie) outCookies.elementAt(i);
					sb.append(cc.getName());
					sb.append('=');
					sb.append(cc.getValue());
					if (cc.getComment() != null) {
						sb.append("; Comment=" + cc.getComment());
					}
					if (cc.getDomain() != null) {
						sb.append("; domain=" + cc.getDomain());
					}

					if (cc.getMaxAge() >= 0) {
						sb.append("; expires=");
						sb.append(expdatefmt.format(new Date(System
								.currentTimeMillis()
								+ 1000 * cc.getMaxAge())));
					}
					if (cc.getPath() != null) {
						sb.append("; Path=" + cc.getPath());
					}
					if (cc.getSecure()) {
						sb.append("; Secure");
					}
					if (cc.getVersion() > 0) {
						sb.append("; Version=" + cc.getVersion());
					}
				}
				if (sb != null) {
					out.println(sb.toString());
					// System.err.println("We sent cookies: "+sb);
				}
				if (wasContentLen == false && chunked_out == false
						&& serve.isKeepAlive()) {
					out.println(TRANSFERENCODING + ": " + CHUNKED);
					chunked_out = true;
				}
				out.println("");
				out.flush();
				((ServeOutputStream) out).setChunked(chunked_out);
			}
		}

		// / Writes an error response using the specified status code and
		// message.
		// @param resCode the status code
		// @param resMessage the status message
		// @exception IOException if an I/O error has occurred
		public void sendError(int resCode, String resMessage)
				throws IOException {
			setStatus(resCode, resMessage);
			realSendError();
		}

		// / Writes an error response using the specified status code and a
		// default
		// message.
		// @param resCode the status code
		// @exception IOException if an I/O error has occurred
		public void sendError(int resCode) throws IOException {
			setStatus(resCode);
			realSendError();
		}

		public void setInInclude(boolean set) {
			((ServeOutputStream) out).setInInclude(set);
		}

		private void realSendError() throws IOException {
			if (isCommitted())
				throw new IllegalStateException(
						"Can not send error, headers have been already written");
			setContentType("text/html");
			StringBuffer sb = new StringBuffer(100);
			sb.append("<HTML><HEAD>").append(
					"<TITLE>" + resCode + " " + resMessage + "</TITLE>")
					.append("</HEAD><BODY BGCOLOR=\"#F1D0F2\">").append(
							"<H2>" + resCode + " " + resMessage + "</H2>")
					.append("<HR>");
			Serve.Identification.writeAddress(sb);
			sb.append("</BODY></HTML>");
			setContentLength(sb.length());
			out.print(sb.toString());
			out.flush();
			out.close();
		}

		// / Sends a redirect message to the client using the specified redirect
		// location URL.
		// @param location the redirect location URL
		// @exception IOException if an I/O error has occurred
		public void sendRedirect(String location) throws IOException {
			if (isCommitted())
				throw new IllegalStateException(
						"Can not redirect, headers have been already written");
			if (location.indexOf(":/") < 0) { // relative
				String portString = "";
				if ("https".equalsIgnoreCase(getScheme())) {
					if (getServerPort() != 443)
						portString = ":" + getServerPort();
				} else if (getServerPort() != 80)
					portString = ":" + getServerPort();

				if (location.length() > 0 && location.charAt(0) == '/') {
					location = getScheme() + "://" + getServerName()
							+ portString + location;
				} else {
					int sp = reqUriPath.lastIndexOf('/');
					String uri;
					if (sp < 0) {
						uri = reqUriPath + '/';
						sp = uri.length();
					} else {
						uri = reqUriPath;
						sp++;
					}
					location = getScheme() + "://" + getServerName()
							+ portString + uri.substring(0, sp) + location;
				}
			}
			// serve.log("location:"+location);
			setHeader("Location", location);
			setStatus(SC_MOVED_TEMPORARILY);
			setContentType("text/html");
			StringBuffer sb = new StringBuffer(200);
			sb.append("<HTML><HEAD>" + "<TITLE>" + SC_MOVED_TEMPORARILY
					+ " Moved</TITLE>" + "</HEAD><BODY BGCOLOR=\"#F1D0F2\">"
					+ "<H2>" + SC_MOVED_TEMPORARILY + " Moved</H2>"
					+ "This document has moved <a href=" + location
					+ ">here.<HR>");
			Serve.Identification.writeAddress(sb);
			sb.append("</BODY></HTML>");
			setContentLength(sb.length());
			// to avoid further out
			out.print(sb.toString());
			out.flush();
			out.close();
		}

		// URL session-encoding stuff. Not implemented, but the API is here
		// for compatibility.

		// / Encodes the specified URL by including the session ID in it, or, if
		// encoding is not needed, returns the URL unchanged. The
		// implementation of this method should include the logic to determine
		// whether the session ID needs to be encoded in the URL. For example,
		// if the browser supports cookies, or session tracking is turned off,
		// URL encoding is unnecessary.
		// <P>
		// All URLs emitted by a Servlet should be run through this method.
		// Otherwise, URL rewriting cannot be used with browsers which do not
		// support cookies.
		public String encodeUrl(String url) {
			return url;
		}

		// / Encodes the specified URL for use in the sendRedirect method or, if
		// encoding is not needed, returns the URL unchanged. The
		// implementation of this method should include the logic to determine
		// whether the session ID needs to be encoded in the URL. Because the
		// rules for making this determination differ from those used to
		// decide whether to encode a normal link, this method is seperate
		// from the encodeUrl method.
		// <P>
		// All URLs sent to the HttpServletResponse.sendRedirect method should
		// be
		// run through this method. Otherwise, URL rewriting cannot be used with
		// browsers which do not support cookies.
		public String encodeRedirectUrl(String url) {
			return url;
		}
	}

	protected static class BasicAuthRealm extends Hashtable {
		String name;

		BasicAuthRealm(String name) {
			this.name = name;
		}

		String name() {
			return name;
		}
	}

	public static class ServeInputStream extends ServletInputStream {
		private final static boolean STREAM_DEBUG = false;

		/* ------------------------------------------------------------ */
		/**
		 * The actual input stream.
		 */
		private BufferedInputStream in;

		private int chunksize = 0;

		private boolean chunking = false;

		private boolean returnedAsReader, returnedAsStream;
		
		private long contentLength = -1;
		
		private long readCount;

		/* ------------------------------------------------------------ */
		/**
		 * Constructor
		 */
		public ServeInputStream(InputStream in) {
			this.in = new BufferedInputStream(in);
		}

		void refresh() {
			returnedAsReader = false;
			returnedAsStream = false;
			contentLength = -1;
			readCount = 0;
			chunksize = 0;
		}

		/* ------------------------------------------------------------ */
		/**
		 * @param chunking
		 */
		public void chunking(boolean chunking) {
			if (contentLength == -1)
				this.chunking = chunking;
		}
		
		/** sets max read byte in input
		 */
		void setContentLength(long contentLength) {
			if (this.contentLength == -1 && contentLength >= 0 && chunking == false) {
				this.contentLength = contentLength;
				readCount = 0;
			}
		}

		/* ------------------------------------------------------------ */
		/**
		 * Read a line ended by CR or CRLF or LF. More forgiving of line
		 * termination than ServletInputStream.readLine(). This method only read
		 * raw data, that may be chunked. Calling ServletInputStream.readLine()
		 * will always return unchunked data.
		 */
		// TODO: won't work with encoding
		public String readLine() throws IOException {
			StringBuffer buf = new StringBuffer(1024);

			int c;
			boolean cr = false;
			boolean lf = false;

			LineLoop: while ((c = chunking || contentLength >= 0? read() : in.read()) != -1) {
				switch (c) {
				case 10:
					lf = true;
					break LineLoop;

				case 13:
					cr = true;
					if (!chunking)
						in.mark(2);
					break;

				default:
					if (cr) {
						// if (chunking)
						// log("Cannot handle CR in chunking mode");
						in.reset();
						break LineLoop;
					} else
						buf.append((char) c);
					break;
				}
			}

			if (c == -1 && buf.length() == 0)
				return null;
			if (STREAM_DEBUG)
				System.err.println(buf.toString());

			return buf.toString();
		}

		/* ------------------------------------------------------------ */
		public int read() throws IOException {
			if (chunking) {
				int b = -1;
				if (chunksize <= 0 && getChunkSize() <= 0)
					return -1;
				b = in.read();
				chunksize = (b < 0) ? -1 : (chunksize - 1);
				if (STREAM_DEBUG) {
					if (b >= 0)
						System.err.print((char) b);
					else
						System.err.print("EOF");
				}
				return b;
			}
			
			if (contentLength >= 0) {
				if (readCount >= contentLength) {
					if (STREAM_DEBUG)
						System.err.print("EOF");
					return -1;
				}
				readCount++; // not quite robust since count not read yet
			}
			
			if (STREAM_DEBUG) {
				int c = in.read();
				if (c >= 0)
					System.err.print((char) c);
				return c;
			}
			
			return in.read();
		}

		/* ------------------------------------------------------------ */
		public int read(byte b[]) throws IOException {
			return read(b, 0, b.length);
		}

		/* ------------------------------------------------------------ */
		public int read(byte b[], int off, int len) throws IOException {
			if (chunking) {
				if (chunksize <= 0 && getChunkSize() <= 0)
					return -1;
				if (len > chunksize)
					len = chunksize;
				len = in.read(b, off, len);
				chunksize = (len < 0) ? -1 : (chunksize - len);
			} else {
				if (contentLength >= 0) {
					if (contentLength-readCount < Integer.MAX_VALUE)
				
						len = Math.min(len, (int)(contentLength-readCount));
					if (len <= 0) {
						if (STREAM_DEBUG)
							System.err.print("EOF");
						return -1;
					}
					len = in.read(b, off, len);
					readCount += len;
				} else // to avoid extra if
					len = in.read(b, off, len);
					
			}
			if (STREAM_DEBUG)
				System.err.print(new String(b, off, len));

			return len;
		}

		/* ------------------------------------------------------------ */
		public long skip(long len) throws IOException {
			if (chunking) {
				if (chunksize <= 0 && getChunkSize() <= 0)
					return -1;
				if (len > chunksize)
					len = chunksize;
				len = in.skip(len);
				chunksize = (len < 0) ? -1 : (chunksize - (int) len);
			} else {
				if (contentLength >= 0) {
					len = Math.min(len, contentLength-readCount);
					if (len <= 0) 
						return -1;
					len = in.skip(len);
					readCount += len;
				} else
					len = in.skip(len);
			}
			return len;
		}

		/* ------------------------------------------------------------ */
		/**
		 * Available bytes to read without blocking. If you are unlucky may
		 * return 0 when there are more
		 */
		public int available() throws IOException {
			if (chunking) {
				int len = in.available();
				if (len <= chunksize)
					return len;
				return chunksize;
			}
			
			if (contentLength >= 0) {
				int len = in.available();
				if (contentLength-readCount < Integer.MAX_VALUE)
					return Math.min(len, (int)(contentLength-readCount));
				return len;
			} else
				return in.available();
		}

		/* ------------------------------------------------------------ */
		public void close() throws IOException {
			// keep alive, will be closed by socket
			//in.close();
			if (STREAM_DEBUG)
				System.err.println("instream.close()");
			chunksize = -1;
			if (contentLength >= 0)
				contentLength = readCount;
		}

		/* ------------------------------------------------------------ */
		/**
		 * Mark is not supported
		 * 
		 * @return false
		 */
		public boolean markSupported() {
			return false;
		}

		/* ------------------------------------------------------------ */
		/**
		 * Not Implemented
		 */
		public void reset() throws IOException {
			// no buffering, so not possible
			if (STREAM_DEBUG)
				System.err.println("instream.reset()");
			in.reset();
		}

		/* ------------------------------------------------------------ */
		/**
		 * Not Implemented
		 * 
		 * @param readlimit
		 */
		public void mark(int readlimit) {
			// not supported
			if (STREAM_DEBUG)
				System.err.println("instream.mark("+readlimit+")");
		}

		/* ------------------------------------------------------------ */
		private int getChunkSize() throws IOException {
			if (chunksize < 0)
				return -1;

			chunksize = -1;

			// Get next non blank line
			chunking = false;
			String line = readLine();
			while (line != null && line.length() == 0)
				line = readLine();
			chunking = true;

			// Handle early EOF or error in format
			if (line == null)
				return -1;

			// Get chunksize
			int i = line.indexOf(';');
			if (i > 0)
				line = line.substring(0, i).trim();
			chunksize = Integer.parseInt(line, 16);

			// check for EOF
			if (chunksize == 0) {
				chunksize = -1;
				// Look for footers
				chunking = false;
			}
			return chunksize;
		}

		boolean isReturnedAsStream() {
			return returnedAsStream;
		}

		void setReturnedAsStream(boolean _on) {
			returnedAsStream = _on;
		}

		boolean isReturnedAsReader() {
			return returnedAsReader;
		}

		void setReturnedAsReader(boolean _on) {
			returnedAsReader = _on;
		}
	}

	public static class ServeOutputStream extends ServletOutputStream {

		private static final boolean STREAM_DEBUG = false;

		private boolean chunked;

		private boolean closed;

		// TODO: predefine as static byte[] used by chunked
		// underneath stream
		private OutputStream out;

		// private BufferedWriter writer; // for top speed
		private ServeConnection conn;

		private boolean inInclude;

		private String encoding;

		public ServeOutputStream(OutputStream out, ServeConnection conn) {

			this.out = out;
			this.conn = conn;
			encoding = conn.getCharacterEncoding();
			if (encoding == null)
				encoding = "ISO-8859-1";
		}

		void refresh() {
			chunked = false;
			closed = false;
			inInclude = false;
			encoding = conn.getCharacterEncoding();
			if (encoding == null)
				encoding = "ISO-8859-1";
		}

		protected void setChunked(boolean set) {
			chunked = set;
		}

		public void print(String s) throws IOException {
			write(s.getBytes(encoding));
		}

		public void write(int b) throws IOException {
			write(new byte[] { (byte) b }, 0, 1);
		}

		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		public void write(byte[] b, int off, int len) throws IOException {
			if (closed)
				throw new IOException("An attempt of writing to closed out.");
			if (len == 0)
				return;
			conn.writeHeaders();
			if (chunked) {
				out.write((Integer.toHexString(len) + "\r\n").getBytes()); // encoding
																			// needed
																			// ??
				out.write(b, off, len);
				out.write("\r\n".getBytes());
			} else
				out.write(b, off, len);
			if (STREAM_DEBUG) {
				if (chunked)
					System.err.println(Integer.toHexString(len));
				System.err.print(new String(b, off, len));
				if (chunked)
					System.err.println();
			}
		}

		public void flush() throws IOException {
			if (closed)
				return; 
				//throw new IOException("An attempt of flushig closed out.");
			if (inInclude == false)
				conn.writeHeaders();
			out.flush();
		}

		public void close() throws IOException {
			if (closed)
				return;
				//throw new IOException("Stream is already closed.");
			flush();
			if (inInclude == false) {
				if (chunked) {
					out.write("0\r\n\r\n".getBytes());
					if (STREAM_DEBUG)
						System.err.print("0\r\n\r\n");
					// TODO: here is possible to write trailer headers
					out.flush();
				}
				if (conn.keepAlive == false)
					out.close();
			}
			closed = true; // TODO: potential problem to be closed if an
							// exception happens
		}

		boolean isInInclude() {
			return inInclude;
		}

		void setInInclude(boolean _set) {
			inInclude = _set;
		}
	}

	/**
	 * Class PathTreeDictionary - this class allows to put path elements in
	 * format n1/n2/n2[/*.ext] and get match to a pattern and a unmatched tail
	 */
	protected static class PathTreeDictionary {
		Node root_node;

		PathTreeDictionary() {
			root_node = new Node();
		}

		void put(String path, Object value) {
			StringTokenizer st = new StringTokenizer(path, "\\/");
			Node cur_node = root_node;
			while (st.hasMoreTokens()) {
				String nodename = st.nextToken();
				Node node = (Node) cur_node.get(nodename);
				if (node == null) {
					node = new Node();
					cur_node.put(nodename, node);
				}
				cur_node = node;
			}
			cur_node.object = value;
		}

		/**
		 * This function looks up in the directory to find the perfect match and
		 * remove matching part from path, so if you need to keep original path,
		 * save it somewhere
		 */
		Object[] get(String path) {
			Object[] result = new Object[2];
			if (path == null)
				return result;
			char[] ps = path.toCharArray();
			Node cur_node = root_node;
			int p0 = 0, lm = 0; // last match
			result[0] = cur_node.object;
			boolean div_state = true;
			for (int i = 0; i < ps.length; i++) {
				if (ps[i] == '/' || ps[i] == '\\') {
					if (div_state)
						continue;
					Node node = (Node) cur_node.get(new String(ps, p0, i - p0));
					if (node == null) {
						result[1] = new Integer(lm);
						return result;
					}
					if (node.object != null) {
						result[0] = node.object;
						lm = i;
					}
					cur_node = node;
					div_state = true;
				} else {
					if (div_state) {
						p0 = i;
						div_state = false;
					}
				}
			}
			cur_node = (Node) cur_node.get(new String(ps, p0, ps.length - p0));
			if (cur_node != null && cur_node.object != null) {
				result[0] = cur_node.object;
				lm = ps.length;
			}
			result[1] = new Integer(lm);
			return result;
		}

		Enumeration keys() {
			Vector result = new Vector();
			addSiblingNames(root_node, result, "");
			return result.elements();
		}

		void addSiblingNames(Node node, Vector result, String path) {
			Enumeration e = node.keys();
			while (e.hasMoreElements()) {
				String pc = (String) e.nextElement();
				Node childNode = (Node) node.get(pc);
				pc = path + '/' + pc;
				if (childNode.object != null)
					result.addElement(pc);
				addSiblingNames(childNode, result, pc);
			}
		}

		Enumeration elements() {
			Vector result = new Vector();
			addSiblingObjects(root_node, result);
			return result.elements();
		}

		void addSiblingObjects(Node node, Vector result) {
			Enumeration e = node.keys();
			while (e.hasMoreElements()) {
				Node childNode = (Node) node.get(e.nextElement());
				if (childNode.object != null)
					result.addElement(childNode.object);
				addSiblingObjects(childNode, result);
			}
		}

		class Node extends Hashtable {
			Object object;
		}
	}

	/**
	 * Http session support
	 * 
	 * TODO: provide lazy session restoring, it should allow to load classes from wars
	 * 1st step it read serialization data and store under session attribute
	 * 2nd when the session requested, it tries to deserialize all session attributes considered that
	 * all classes available
	 */
	public static class AcmeSession extends Hashtable implements HttpSession {
		private long createTime;

		private long lastAccessTime;

		private String id;

		private int inactiveInterval; // in seconds

		private boolean expired;

		private transient ServletContext servletContext;

		private transient HttpSessionContext sessionContext;
		
		private transient List listeners;

		// TODO: check in documentation what is default inactive interval and
		// what
		// means 0
		// and what is mesurement unit
		AcmeSession(String id, ServletContext servletContext,
				HttpSessionContext sessionContext) {
			this(id, 0, servletContext, sessionContext);
		}

		AcmeSession(String id, int inactiveInterval,
				ServletContext servletContext, HttpSessionContext sessionContext) {
			createTime = System.currentTimeMillis();
			this.id = id;
			this.inactiveInterval = inactiveInterval;
			this.servletContext = servletContext;
			this.sessionContext = sessionContext;
		}

		public long getCreationTime() {
			return createTime;
		}

		public String getId() {
			return id;
		}

		public long getLastAccessedTime() {
			return lastAccessTime;
		}

		public void setMaxInactiveInterval(int interval) {
			inactiveInterval = interval;
		}

		public int getMaxInactiveInterval() {
			return inactiveInterval;
		}

		/**
		 * @deprecated
		 */
		public HttpSessionContext getSessionContext() {
			return sessionContext;
		}

		/**
		 * Returns the ServletContext to which this session belongs.
		 * 
		 * @return The ServletContext object for the web application
		 * @ince 2.3
		 */
		public ServletContext getServletContext() {
			return servletContext;
		}

		public java.lang.Object getAttribute(java.lang.String name)
				throws IllegalStateException {
			if (expired)
				throw new IllegalStateException();
			return get((Object) name);
		}

		public java.lang.Object getValue(java.lang.String name)
				throws IllegalStateException {
			return getAttribute(name);
		}

		public java.util.Enumeration getAttributeNames()
				throws IllegalStateException {
			if (expired)
				throw new IllegalStateException();
			return keys();
		}

		public java.lang.String[] getValueNames() throws IllegalStateException {
			Enumeration e = getAttributeNames();
			Vector names = new Vector();
			while (e.hasMoreElements())
				names.addElement(e.nextElement());
			String[] result = new String[names.size()];
			names.copyInto(result);
			return result;
		}

		public void setAttribute(String name, Object value)
				throws IllegalStateException {
			if (expired)
				throw new IllegalStateException();
			Object oldValue = value != null ? put((Object) name, value)
					: remove(name);
			if (oldValue != null)
					if(oldValue instanceof HttpSessionBindingListener)
				((HttpSessionBindingListener) oldValue)
						.valueUnbound(new HttpSessionBindingEvent(this, name));
					else if (oldValue instanceof HttpSessionAttributeListener)
						((HttpSessionAttributeListener)oldValue).attributeReplaced(new HttpSessionBindingEvent(this, name, value));
			if (value instanceof HttpSessionBindingListener)
				((HttpSessionBindingListener) value)
						.valueBound(new HttpSessionBindingEvent(this, name));
			else if (value instanceof HttpSessionAttributeListener)
				((HttpSessionAttributeListener)value).attributeAdded(new HttpSessionBindingEvent(this, name));
		}

		public void putValue(String name, Object value)
				throws IllegalStateException {
			setAttribute(name, value);
		}

		public void removeAttribute(java.lang.String name)
				throws IllegalStateException {
			if (expired)
				throw new IllegalStateException();
			Object value = remove((Object) name);
			if (value != null)
				if ( value instanceof HttpSessionBindingListener)
				((HttpSessionBindingListener) value)
						.valueUnbound(new HttpSessionBindingEvent(this, name));
				else if (value instanceof HttpSessionAttributeListener)
					((HttpSessionAttributeListener)value).attributeRemoved(new HttpSessionBindingEvent(this, name));
		}

		public void removeValue(java.lang.String name)
				throws IllegalStateException {
			removeAttribute(name);
		}

		public synchronized void invalidate() throws IllegalStateException {
			if (expired)
				throw new IllegalStateException();
			notifyListeners();
			Enumeration e = getAttributeNames();
			while (e.hasMoreElements()) {
				removeAttribute((String) e.nextElement());
			}
			setExpired(true);
			// would be nice remove it from hash table also
		}

		public boolean isNew() throws IllegalStateException {
			if (expired)
				throw new IllegalStateException();
			return lastAccessTime == 0;
		}
		
		public synchronized void setListeners(List l) {
			if (listeners == null) {
				listeners = l;
				if (listeners != null) {
					HttpSessionEvent event = new HttpSessionEvent(this);
					for (int i=0; i<listeners.size(); i++)
						try {
							((HttpSessionListener)listeners.get(0)).sessionCreated(event);
						}catch(ClassCastException cce) {
							//log("Wrong session listener type."+cce);
						}catch(NullPointerException npe) {
							//log("Null session listener.");
						}
				}
			}
		}
		
		private void notifyListeners() {
			if (listeners != null) {
				HttpSessionEvent event = new HttpSessionEvent(this);
				for (int i=0; i<listeners.size(); i++)
					try {
						((HttpSessionListener)listeners.get(0)).sessionDestroyed(event);
					}catch(ClassCastException cce) {
						//log("Wrong session listener type."+cce);
					}catch(NullPointerException npe) {
						//log("Null session listener.");
					}				
			}
		}

		private void setExpired(boolean expired) {
			this.expired = expired;
		}

		boolean isValid() {
			return !expired;
		}
		
		boolean checkExpired() {
			return inactiveInterval > 0 && (inactiveInterval * 1000 < System.currentTimeMillis()
			- lastAccessTime);
		}

		void userTouch() {
			lastAccessTime = System.currentTimeMillis();
		}

		// storing session in format
		// id:tttt:latency
		// entry:base64 ser data
		// entry:base64 ser data
		// $$
		void save(Writer w) throws IOException {
			if (expired)
				return;
			w.write(id);
			w.write(":"+lastAccessTime+':'+inactiveInterval+"\r\n");
			Enumeration e = getAttributeNames();
			ByteArrayOutputStream os = new ByteArrayOutputStream(1024*16);
			while (e.hasMoreElements()) {
				String aname = (String) e.nextElement();
				Object so = get(aname);
				if (so instanceof Serializable) {
					os.reset();
					ObjectOutputStream oos = new ObjectOutputStream(os);
					try {
						oos.writeObject(so);
						w.write(aname);
						w.write(":");
						w.write(Utils.base64Encode(os.toByteArray()));
						w.write("\r\n"); 
					} catch(IOException ioe) {
						servletContext.log(
								"Problem storing session value " + aname, ioe);
					}
				}
			}
			w.write("$$\r\n");
		}
				
		static AcmeSession restore( BufferedReader r, int inactiveInterval,
				ServletContext servletContext, HttpSessionContext sessionContext) throws IOException {
			String s = r.readLine();
			if (s == null) // eos
				return null;
			int cp = s.indexOf(':');
			if (cp<0)
				throw new IOException ("Invalid format for a session header: "+s);
			String id = s.substring(0, cp);
			int cp2 = s.indexOf(':', cp+1);
			if (cp2 < 0)
				throw new IOException ("Invalid format for a session header: "+s);
			try {
				inactiveInterval = Integer.parseInt(s.substring(cp2+1));
			}catch(NumberFormatException nfe) {
				servletContext.log("Session latency invalid:"+s.substring(cp2+1)+" "+nfe);
			}
			AcmeSession result = new AcmeSession(id, inactiveInterval, servletContext, sessionContext);
			try {
				result.lastAccessTime = Long.parseLong(s.substring(cp+1, cp2));
			}catch(NumberFormatException nfe) {
				servletContext.log("Last access time invalid:"+s.substring(cp+1, cp2)+" "+nfe);
			}
			do {
				s = r.readLine();
				if (s == null)
					throw new IOException("Unexpected end of stream.");
				if ("$$".equals(s))
					return result;
				cp = s.indexOf(':');
				if (cp<0)
					throw new IOException ("Invalid format for a session entry: "+s);
				String aname = s.substring(0, cp);
				// if (lazyRestore)
				//  result.put(aname, s.substring(cp+1)); 
				ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(Utils.decode64(s.substring(cp+1))));
				try {
					result.put(aname, ois.readObject());
				} catch(ClassNotFoundException cnfe) {
					servletContext.log("Can't restore :"+aname+", "+cnfe);
				} catch(IOException ioe) {
					  servletContext.log("Can't restore :"+aname+", "+ioe);
				}
			}while(true);
		}
	}

	protected static class LocaleWithWeight implements Comparable {
		protected float weight; // should be int
		protected Locale locale;
		LocaleWithWeight(Locale l, float w) {
			locale = l;
			weight = w;
			//System.err.println("Created "+l+", with:"+w);
		}
		
		public int compareTo(Object o) {
			if (o instanceof LocaleWithWeight) 
				return (int)(((LocaleWithWeight)o).weight-weight)*100;
			throw new IllegalArgumentException();
		}
		
		public Locale getLocale() {
			return locale;
		}
	}
	
	protected static class AcceptLocaleEnumeration implements Enumeration {
		Iterator i;
		public AcceptLocaleEnumeration(TreeSet/*<LocaleWithWeight>*/ ts) {
			i = ts.iterator();
		}
		
		public boolean hasMoreElements() {
			return i.hasNext();
		}
		
		public Object nextElement() {
			return ((LocaleWithWeight)i.next()).getLocale();
			/*Locale l =((LocaleWithWeight)i.next()).getLocale();
			System.err.println("Returned l:"+l);
			return l;*/
		}
	}

	// TODO: reconsider implementation by providing
	// inner class implementing HttpSessionContext
	// and returning it on request
	// to avoid casting this class to Hashtable

	protected static class HttpSessionContextImpl extends Hashtable implements
			HttpSessionContext {

		public java.util.Enumeration getIds() {
			return keys();
		}

		public HttpSession getSession(java.lang.String sessionId) {
			return (HttpSession) get(sessionId);
		}
		
		void save(Writer w) throws IOException {
			Enumeration e =	elements();
			while(e.hasMoreElements())
				((AcmeSession)e.nextElement()).save(w);
		}
		
		static HttpSessionContextImpl restore(BufferedReader br, int inactiveInterval,
				ServletContext servletContext) throws IOException {
			HttpSessionContextImpl result = new HttpSessionContextImpl();
			AcmeSession session;
			while ((session = AcmeSession.restore(br, inactiveInterval, servletContext, result)) != null)
				if (session.checkExpired() == false)
					result.put(session.getId(), session);
			return result;
		}
	}
}