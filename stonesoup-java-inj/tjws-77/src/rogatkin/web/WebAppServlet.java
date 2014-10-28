/* tjws - WebAppServlet.java
 * Copyright (C) 1999-2006 Dmitriy Rogatkin.  All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  $Id: WebAppServlet.java,v 1.40 2006/01/03 08:28:23 drogatkin Exp $
 * Created on Dec 14, 2004
 */

package rogatkin.web;

import java.net.URL;
import java.net.MalformedURLException;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.TreeSet;
import java.util.Set;
import java.util.EventListener;
import java.io.FileFilter;
import java.io.OutputStream;
import java.io.PrintWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import javax.servlet.Servlet;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.URLClassLoader;
import java.text.DateFormat;

import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathConstants;
import javax.swing.event.EventListenerList;

import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.InputSource;
import Acme.Serve.Serve;
/**
 * @author dmitriy
 *
 * 
 */
public class WebAppServlet extends HttpServlet implements ServletContext {
    List<ServletAccessDescr> servlets;
    List <FilterAccessDescriptor> filters;
    URL[] cpUrls;
    URLClassLoader 	ucl;
    File deployDir;
    Serve server;
    /// context methods
    protected String contextName;
	protected String contextPath;
    protected Hashtable<String,Object> attributes;
    protected Hashtable<String,String> contextParameters;
    protected List<String> welcomeFiles;
    protected List<ErrorPageDescr> errorPages;
    protected List<EventListener> listeners;
    protected List<HttpSessionListener> sessionListeners;
    protected Map<String, String> mimes;
    
    protected class ServletAccessDescr implements ServletConfig {
        String className;
        String name;
        HttpServlet instance;
        String servPath;
        String pathPat;
        Map<String,String> initParams;
        String label;
        boolean loadOnStart;
        String descr;
        
        public java.lang.String getServletName(){
            return name;
        }
        
        public java.util.Enumeration getInitParameterNames() {
            return new Enumeration<String>() {
                Iterator<String> i;
                {i = initParams.keySet().iterator();}
                public boolean hasMoreElements() {
                    return i.hasNext();
                }
                public String nextElement() {
                    return i.next();
                }
            };
        }
        
        public ServletContext getServletContext() {
            return WebAppServlet.this;
        }
        public String getInitParameter(java.lang.String name) {
            return initParams.get(name);
        }
        
        public String toString() {
            return "Servlet "+name+" class "+className+" path "+pathPat+"/"+servPath
            +" init"+initParams+" inst "+instance;
        }
    }
    
    protected class FilterAccessDescriptor extends ServletAccessDescr implements FilterConfig {
        String servletName;
        Filter filterInstance;
        public java.lang.String getFilterName() {
            return name;
        }
    }
    
    protected static class ErrorPageDescr {
        String errorPage;
        Class exception;
        int errorCode;
        ErrorPageDescr(String page, String exClass, String code) {
            errorPage = page;
            try {
                exception = Class.forName(exClass);
            } catch(Exception e) {
                
            }
            try {
                errorCode = Integer.parseInt(code);
            } catch(Exception e) {
                
            }
        }
    } 
    
    protected WebAppServlet(String context) {
        this.contextPath = "/"+context;
        attributes = new Hashtable<String,Object>();
        contextParameters = new Hashtable<String,String>();
    }
    
    public static WebAppServlet create(File deployDir, String context, Serve server) throws ServletException {
        XPath xp = XPathFactory.newInstance().newXPath();
        WebAppServlet result = new WebAppServlet(context);
        result.server = server;
        try {
            result.makeCP(deployDir);
            Node document = (Node) xp.evaluate("//web-app", new InputSource(new FileInputStream(new File(deployDir, "WEB-INF/web.xml"))), XPathConstants.NODE);
			result.contextName = (String)xp.evaluate("display-name", document, XPathConstants.STRING);
			if (result.contextName != null && result.contextName.length() == 0)
				result.contextName = null;
			if (result.contextName != null)
				result.contextPath = "/"+result.contextName;
            else
                result.contextName = context;
            NodeList nodes = (NodeList) xp.evaluate("servlet", document, XPathConstants.NODESET);
            result.servlets = new ArrayList<ServletAccessDescr>(nodes.getLength());
            for (int i=0;i<nodes.getLength();i++) {
                Node n = nodes.item(i);
                ServletAccessDescr sad = result.createDescriptor();
                sad.name = (String)xp.evaluate("servlet-name", n, XPathConstants.STRING);
                sad.className = (String)xp.evaluate("servlet-class", n, XPathConstants.STRING);
                sad.label = (String)xp.evaluate("display-name", n, XPathConstants.STRING);
                sad.descr = (String)xp.evaluate("description", n, XPathConstants.STRING);
                String loadOnStartVal = (String)xp.evaluate("load-on-startup", n, XPathConstants.STRING);
                sad.loadOnStart = loadOnStartVal != null && loadOnStartVal.length()>0;
                NodeList params = (NodeList)xp.evaluate("init-param", n, XPathConstants.NODESET);
                sad.initParams = new HashMap<String,String>(params.getLength());
                for (int p=0; p<params.getLength(); p++) {
                    sad.initParams.put((String)xp.evaluate("param-name", params.item(p), XPathConstants.STRING),
                            (String)xp.evaluate("param-value", params.item(p), XPathConstants.STRING));
                }
                result.servlets.add(sad);
            }
            // get extra mappings
            for (ServletAccessDescr sad:result.servlets) {
                Node n = (Node)xp.evaluate("//servlet-mapping[servlet-name=\""+
                sad.name+"\"]", document, XPathConstants.NODE);
                if (n != null) { // use name map
                    sad.servPath = (String)xp.evaluate("url-pattern", n, XPathConstants.STRING);                    
                }
                if (sad.servPath == null)
                    sad.servPath = "/"+sad.name+"/*";
                else if (sad.servPath.equals("/"))
                		sad.servPath = "/*";
               // sad.pathPat = sad.servPath.replace("?", "[^:]").replace("*", "[^\\?:\\*]*");
                sad.pathPat = sad.servPath.replace(".", "\\.").replace("?", ".").replace("*", ".*");
                // TODO: make it more robust
                int wcp = sad.servPath.indexOf('*');
                if (wcp > 0)
                    sad.servPath = sad.servPath.substring(0, wcp);
                
                if (/*sad.servPath.length() > 1 && */sad.servPath.endsWith("/"))
                    sad.servPath = sad.servPath.substring(0,sad.servPath.length()-1);
                //System.err.printf("Servlet %s, path:%s\n", sad, sad.servPath);
                if (sad.loadOnStart)
                    result.newInstance(sad);
            }
            result.addJSPServlet();
            // process filters
            nodes = (NodeList) xp.evaluate("filter", document, XPathConstants.NODESET);
            result.filters = new ArrayList<FilterAccessDescriptor>(nodes.getLength());
            int nodesLen = nodes.getLength();    
            for (int i=0;i<nodesLen;i++) {
                Node n = nodes.item(i);
                FilterAccessDescriptor fad = result.createFilterDescriptor();
                fad.name = (String)xp.evaluate("filter-name", n, XPathConstants.STRING);
                fad.className = (String)xp.evaluate("filter-class", n, XPathConstants.STRING);
                fad.label = (String)xp.evaluate("display-name", n, XPathConstants.STRING);
                fad.descr = (String)xp.evaluate("description", n, XPathConstants.STRING);
                NodeList params = (NodeList)xp.evaluate("init-param", n, XPathConstants.NODESET);
                fad.initParams = new HashMap<String,String>(params.getLength());
                for (int p=0; p<params.getLength(); p++) {
                    fad.initParams.put((String)xp.evaluate("param-name", params.item(p), XPathConstants.STRING),
                            (String)xp.evaluate("param-value", params.item(p), XPathConstants.STRING));
                }
                result.filters.add(fad);
            }
            for (FilterAccessDescriptor fad:result.filters) {
                Node n = (Node)xp.evaluate("//filter-mapping[filter-name=\""+
                fad.name+"\"]", document, XPathConstants.NODE);
                if (n != null) { // use name map
                    fad.pathPat = (String)xp.evaluate("url-pattern", n, XPathConstants.STRING);
                    fad.servletName = (String)xp.evaluate("servlet-name", n, XPathConstants.STRING);                    
                }
                result.newFilterInstance(fad);
            }
            // welcome files
            nodes = (NodeList) xp.evaluate("//welcome-file-list/welcome-file", document, XPathConstants.NODESET);            
            result.welcomeFiles = new ArrayList<String>(nodes.getLength()+1);
            nodesLen = nodes.getLength();
            if (nodesLen > 0)
                for (int wfi=0; wfi<nodesLen;wfi++)
                    result.welcomeFiles.add(nodes.item(wfi).getTextContent());
            else
                result.welcomeFiles.add("index.html");
            // error pages
            nodes = (NodeList) xp.evaluate("//error-page", document, XPathConstants.NODESET);
            nodesLen = nodes.getLength(); 
            if (nodesLen >0) {
                result.errorPages = new ArrayList<ErrorPageDescr>(nodesLen);
                for (int i=0;i<nodesLen;i++) {
                    Node n = nodes.item(i);
                    result.errorPages.add( new WebAppServlet.ErrorPageDescr(
                            (String)xp.evaluate("location", n, XPathConstants.STRING),
                            (String)xp.evaluate("exception-type", n, XPathConstants.STRING),
                            (String)xp.evaluate("error-code", n, XPathConstants.STRING)));
                }
            }
            // listeners listener-class
            nodes = (NodeList) xp.evaluate("//listener/listener-class", document, XPathConstants.NODESET);
            nodesLen = nodes.getLength(); 
            if (nodesLen >0) {
                result.listeners = new ArrayList<EventListener>(nodesLen);
                for (int i=0; i<nodesLen;i++)
                    try {
                    	EventListener eventListener = (EventListener)result.ucl.loadClass(nodes.item(i).getTextContent()).newInstance(); 
                    	if (eventListener instanceof HttpSessionListener) {
                    		if (result.sessionListeners == null)
                    			result.sessionListeners = new ArrayList<HttpSessionListener>(nodesLen);
                    		result.sessionListeners.add((HttpSessionListener)eventListener);
                    	} 
                    	result.listeners.add(eventListener); // because the same class can implement other listener interfaces
                    } catch(Exception e) {
                        result.log("Event listener "+nodes.item(i).getTextContent()+" can't be created, because "+e);
                    }
            }
            // context parameters
            nodes = (NodeList) xp.evaluate("//context-param", document, XPathConstants.NODESET);
            nodesLen = nodes.getLength(); 
            for (int p=0; p<nodesLen; p++) {
                result.contextParameters.put((String)xp.evaluate("param-name", nodes.item(p), XPathConstants.STRING),
                        (String)xp.evaluate("param-value", nodes.item(p), XPathConstants.STRING));
            }
            // session-config <session-timeout>
            // mime types
            nodes = (NodeList) xp.evaluate("//mime-mapping", document, XPathConstants.NODESET);
            nodesLen = nodes.getLength(); 
            if (nodesLen >0) {
                result.mimes = new HashMap<String, String>(nodesLen);
                for (int i=0;i<nodesLen;i++) {
                    Node n = nodes.item(i);
                    result.mimes.put(((String)xp.evaluate("extension", n, XPathConstants.STRING)).toLowerCase(),
                            (String)xp.evaluate("mime-type", n, XPathConstants.STRING));
                }
            }
            // notify context listeners
            if (result.listeners != null)
	            for (EventListener listener: result.listeners) {
	                if (listener instanceof ServletContextListener)
	                    ((ServletContextListener)listener).contextInitialized(new ServletContextEvent(result));
	            }
        } catch(IOException ioe) {
            throw new ServletException("problem in reading web.xml.", ioe);
        } catch(XPathExpressionException xpe) {
            xpe.printStackTrace();
            throw new ServletException("problem in parsing web.xml.", xpe);
        }
        return result;
    }
    
    public void service(ServletRequest req, ServletResponse res)
     throws ServletException,  IOException {
		//new Exception("call trace").printStackTrace();
         String path = ((HttpServletRequest)req).getPathInfo();
         // do filtering first
         // add default filter (proxies)
         // - build list of filters matching request
         // do check access rights
         final HttpServletRequest hreq = (HttpServletRequest)req;
         // TODO: wrap request to implement methods like getRequestDispatcher()
         // which support relative path, no leading / means relative to currently called
         if (_DEBUG)
             System.err.printf("Full req:%s, ContextPath: %s, ServletPath:%s, pathInfo:%s\n",
                     hreq.getRequestURI(),  hreq.getContextPath(), hreq.getServletPath(), hreq.getPathInfo());
         SimpleFilterChain sfc = new SimpleFilterChain();
      	 WebAppContextFilter wacf;
         sfc.add(wacf = new WebAppContextFilter());
         if (path != null) {
             for (FilterAccessDescriptor fad:filters)
                 if (path.matches(fad.pathPat))
                     sfc.add(fad.filterInstance);
	         for (ServletAccessDescr sad:servlets) {
	             if (_DEBUG)
	                 System.err.println("Trying match "+path+" to "+sad.pathPat+" = "+path.matches(sad.pathPat));
	             if (path.matches(sad.pathPat)) {
	                 if (sad.instance == null) {
	                     if(sad.loadOnStart == false)
	                         newInstance(sad);
	                     if (sad.instance == null) {
	                    	 if(sad.loadOnStart == false)
	                    		 sad.loadOnStart = true; //mark unsuccessful instatntiation and ban the servlet?
	                         throw new ServletException("Servlet "+sad.name+" hasn't been instantiated successfully");
	                     }
	                 }
	                 for (FilterAccessDescriptor fad:filters)
	                     if (sad.name.equals(fad.servletName))
	                         sfc.add(fad.filterInstance);
	                 //System.err.println("used:"+ sad.servPath+", wanted:"+((WebAppServlet) sad.getServletContext()).contextPath);
	                 wacf.setServletPath(sad.servPath);
	                 // add servlet in chain
	                 sfc.setServlet(sad.instance);
	                 sfc.reset();
	                 sfc.doFilter(req, res);
	                 return;
	             }
	         }
	         if (path.regionMatches(true, 0, "/WEB-INF", 0, "/WEB-INF".length())) {
	        	((HttpServletResponse)res).sendError(HttpServletResponse.SC_FORBIDDEN);
	            return;
	         }
         }
         // no matching, process as file
         sfc.setServlet(new HttpServlet() {
             public void service(ServletRequest req, ServletResponse res)
             throws ServletException,  IOException {
                 String path = ((HttpServletRequest)req).getPathTranslated();
                 returnFileContent(path, (HttpServletRequest) req, (HttpServletResponse)res);
             }
         });
         sfc.reset();
         sfc.doFilter(req, res);
     }
    
    protected void returnFileContent(String path, HttpServletRequest req, HttpServletResponse res) throws IOException {
        OutputStream os = null;
        InputStream is = null;
        File fpath = new File(path);
        if (fpath.isDirectory()) {
            File baseDir = fpath; 
            for(String indexPage:welcomeFiles) {
                fpath = new File(baseDir,indexPage);
                if (fpath.exists() && fpath.isFile())
                    break;
            }
        }
        if (fpath.exists() == false) {
            res.sendError(res.SC_NOT_FOUND);
            return;
        }
        if (fpath.isFile() == false) {
            res.sendError(res.SC_FORBIDDEN);
            return;
        }

        res.setContentType(getServletContext().getMimeType(fpath.getName()));
        res.setHeader("Content-Length", Long.toString(fpath.length()));
        res.setContentType(getMimeType(fpath.getName()));
        long lastMod = fpath.lastModified();
        res.setDateHeader("Last-modified", lastMod);
        String ifModSinceStr = req.getHeader("If-Modified-Since");
        long ifModSince = -1;
        if (ifModSinceStr != null) {
            int semi = ifModSinceStr.indexOf(';');
            if (semi != -1)
                ifModSinceStr = ifModSinceStr.substring(0, semi);
            try {
                ifModSince = DateFormat.getDateInstance().parse(ifModSinceStr)
                        .getTime();
            } catch (Exception ignore) {
            }
        }
        try {
            os = res.getOutputStream();
            if (ifModSince != -1 && ifModSince == lastMod) {
               res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
               return;
            }
            if ("HEAD".equals(req.getMethod()))
               return;
            is = new FileInputStream(fpath);
            WarRoller.copyStream(is, os);
        } finally {
            try {
                is.close();                
            } catch(Exception x ) {}
            try {
                os.close();                
            } catch(Exception x ) {}
        }
    }
    
    protected void addJSPServlet() {
        ServletAccessDescr sad = createDescriptor();
        sad.className = "org.gjt.jsp.JSPServlet";
        sad.descr = "JSP support servlet";
        sad.label = "JSP";
        sad.loadOnStart = false;
        sad.name = "jsp";
        sad.pathPat = "/.*\\.jsp";
        sad.servPath = contextPath;
        sad.initParams = new HashMap<String,String>(1);
        sad.initParams.put("repository", new File(deployDir, "~~~").getPath());
        sad.initParams.put("debug", System.getProperty(getClass().getName()+".debug")!=null?"yes":"no");
        servlets.add(sad);
    }
    
    protected ServletAccessDescr createDescriptor() {
        return new ServletAccessDescr();
    }
    
    protected FilterAccessDescriptor createFilterDescriptor() {
        return new FilterAccessDescriptor();
    }
    
    protected void makeCP(File dd) throws IOException {
        deployDir = dd.getCanonicalFile();
        final List<URL> urls = new ArrayList<URL>();
        File classesFile = new File(deployDir, "WEB-INF/classes");
        if (classesFile.exists() && classesFile.isDirectory())
            try {
                urls.add(classesFile.toURL());
            } catch(java.net.MalformedURLException mfe) {
                
            }
        File libFile = new File(deployDir, "WEB-INF/lib");
        libFile.listFiles(new FileFilter(){
            public boolean accept(File file) {
                String name = file.getName().toLowerCase(); 
                if (name.endsWith(".jar") || name.endsWith(".zip"))
                    try {
                        urls.add(file.toURL());
                    } catch(java.net.MalformedURLException mfe) {
                        
                    }
                return false;
            }
            });
        cpUrls = urls.toArray(new URL[urls.size()]);
        ucl = URLClassLoader.newInstance(cpUrls, getClass().getClassLoader() );
        //System.err.println("CP "+urls+"\nLoader:"+ucl);
    }
    
    protected HttpServlet newInstance(ServletAccessDescr descr) throws ServletException {
        try {
           descr.instance = (HttpServlet)ucl.loadClass(descr.className).newInstance();
           descr.instance.init(descr); 
        } catch(InstantiationException ie) {
            throw new ServletException("Servlet class "+descr.className+" can't instantiate. ", ie);
        } catch(IllegalAccessException iae) {
            throw new ServletException("Servlet class "+descr.className+" can't access. ", iae);
        } catch(ClassNotFoundException cnfe) {
            throw new ServletException("Servlet class "+descr.className+" not found. ", cnfe);
        }
        return descr.instance;
    }
    
    protected Filter newFilterInstance(FilterAccessDescriptor descr) throws ServletException {
        try {
            descr.filterInstance = (Filter)ucl.loadClass(descr.className).newInstance();
            descr.filterInstance.init(descr); 
         } catch(InstantiationException ie) {
             throw new ServletException("Filter class "+descr.className+" can't instantiate. ", ie);
         } catch(IllegalAccessException iae) {
             throw new ServletException("Filter class "+descr.className+" can't access. ", iae);
         } catch(ClassNotFoundException cnfe) {
             throw new ServletException("Filter class "+descr.className+" not found. ", cnfe);
         }
         return descr.filterInstance;
    }
    /*protected URL toURL(File file) throws MalformedURLException {
        System.err.println("file:/"+file.getAbsolutePath()+(file.isDirectory()?"/":""));
        return new URL("file:/"+file.getAbsolutePath()+(file.isDirectory()?"/":""));
    }*/
    
    ///////////////////////////////////////////////////////////////////////////////////
    // context methods
    public String getServletContextName() {
        return contextName;
    }
    
    public void removeAttribute(java.lang.String name) {
        Object value = attributes.remove(name);
        if (listeners != null)
	        for (EventListener listener: listeners) 
	            if (listener instanceof ServletContextAttributeListener)
	                    ((ServletContextAttributeListener)listener).attributeRemoved(new ServletContextAttributeEvent(this, name, value));
    }
    public void setAttribute(java.lang.String name,
            java.lang.Object object) {
    	if (object == null) {
    		removeAttribute(name);
    		return;
    	}
        Object oldObj = attributes.put(name, object);
        if (listeners != null)
	        for (EventListener listener: listeners) {
	            if (listener instanceof ServletContextAttributeListener)
	                if (oldObj == null)
	                    ((ServletContextAttributeListener)listener).attributeAdded(new ServletContextAttributeEvent(this, name, object));
	                else
	                    ((ServletContextAttributeListener)listener).attributeReplaced(new ServletContextAttributeEvent(this, name, object));
	        }
    }
    public java.util.Enumeration getAttributeNames() {
        return attributes.keys();
    }
    public java.lang.Object getAttribute(java.lang.String name) {
        return attributes.get(name);
    }
    public java.lang.String getServerInfo() {
        return "TJWS, Copyright &copy; 1998-2006 Dmitriy Rogatkin";        
    }
    public java.lang.String getRealPath(java.lang.String path) {
        path = validatePath(path);
        if (path == null)
            return deployDir.toString();
        else
            return new File(deployDir, path).toString();
    }
    
    public void log(java.lang.String msg) {
		server.log((contextName==null?"":contextName)+"> "+msg);
    }
    
    public void log(java.lang.Exception exception,
            java.lang.String msg) {
        server.log(exception, (contextName==null?"":contextName)+"> "+msg);
    }
    
    public void log(java.lang.String message,
            java.lang.Throwable throwable) {
        server.log((contextName==null?"":contextName)+"> "+message, throwable);
    }
    
    public java.util.Enumeration getServletNames() {
        Vector<String> result = new Vector<String>();
        for (ServletAccessDescr sad:servlets) 
            result.add(sad.name);
        return result.elements();
    }
    public java.util.Enumeration getServlets() {
        Vector<HttpServlet> result = new Vector<HttpServlet>();
        for (ServletAccessDescr sad:servlets) 
            result.add(sad.instance);
        return result.elements();
        
    }
    
    public Servlet getServlet(java.lang.String name) throws ServletException{
        for (ServletAccessDescr sad:servlets)
            if (name.equals(sad.name))
                return sad.instance;
        throw new ServletException("No servlet "+name);
    }
    
    public RequestDispatcher getNamedDispatcher(java.lang.String name) {
        for (ServletAccessDescr sad:servlets)
			if (name.equals(sad.name)) {
				if (sad.instance == null && sad.loadOnStart == false)
					try {
						newInstance(sad);
					} catch(ServletException se) {
					}
				if (sad.instance != null)
					return new SimpleDispatcher(sad.instance, name, sad);
				else
					break;
			}
        return null;
    }
    
	public RequestDispatcher getRequestDispatcher(java.lang.String path) {
		if (path == null || path.length() == 0 || path.charAt(0) != '/')
			return null; // path must start with /
		// look for servlets first
		for (ServletAccessDescr sad:servlets) {
			if (path.matches(sad.pathPat)) {
				if (sad.instance == null && sad.loadOnStart == false)
					try {
						newInstance(sad);
					} catch(ServletException se) {
					}
				if (sad.instance != null)
					return new SimpleDispatcher(sad.instance, path, sad);
				else
					return null; // servlet not working
			}
		}
		// no matching servlets, check for resources
		try {
			getResource(path); // check path is valid
			return new SimpleDispatcher(new HttpServlet() {
				public void service(ServletRequest req, ServletResponse res)
					throws ServletException,  IOException {
					String path = ((HttpServletRequest)req).getPathTranslated();
					returnFileContent(path, (HttpServletRequest) req, (HttpServletResponse)res);
				}
			}, path);
		} catch(MalformedURLException mfe) {
		}
		return null;
    }
    
    public java.io.InputStream getResourceAsStream(java.lang.String path) {
        try {
            return new FileInputStream(getRealPath(path));
        } catch(IOException ioe) {
            return null;
        }
    }
    
    public java.net.URL getResource(java.lang.String path) throws java.net.MalformedURLException {
        try {
            return new File(getRealPath(path)).getCanonicalFile().toURL();
        } catch(IOException io) {
            throw new MalformedURLException();
        }
    }
    
    public java.util.Set getResourcePaths(java.lang.String path) {        
        File dir = new File(getRealPath(path));
        if (dir.exists() == false || dir.isDirectory() == false)
            return null;
        Set<String> set = new TreeSet<String>();
        String[] els = dir.list();
        for(String el:els) {
            String fp = path+"/"+el; 
            if (new File(getRealPath(fp)).isDirectory())
                fp+="/";
            set.add("/"+fp);
        }
        return set;
    }
    public java.lang.String getMimeType(java.lang.String file) {
        if (mimes != null && file != null) {
	        int p = file.lastIndexOf('.');
	        if (p>0) {
	            String result = mimes.get(file.substring(p).toLowerCase());
	            if (result != null)
	                return result;
	        }
        }
        return server.getMimeType(file);
    }
    
    public int getMinorVersion() {
        return 4;
    }
    
    public int getMajorVersion() {
        return 2;
    }
    
    public ServletContext getContext(java.lang.String uripath) {
	Servlet servlet = server.getServlet(uripath);
	if (servlet != null)
		return servlet.getServletConfig().getServletContext();
        return null;
    }
    
    public java.lang.String getInitParameter(java.lang.String name) {
        return contextParameters.get(name);
    }
    
    public java.util.Enumeration getInitParameterNames() {
        return contextParameters.keys();
    }
    
    protected String validatePath(String path) {
        if (path==null || path.length() == 0)
            return path;
        path = path.replace('\\', '/');
        if (path.startsWith("/"))
            path = path.substring(1);
        if (path.indexOf("../") >= 0)
            return null;
        return path;
    }

    public void destroy() {
        if (filters != null)
            for (FilterAccessDescriptor fad:filters)
                if (fad.filterInstance != null)
                    fad.filterInstance.destroy();
        for (ServletAccessDescr sad:filters)
            if (sad.instance != null)
                sad.instance.destroy();
        Enumeration e = getAttributeNames();
        while(e.hasMoreElements())
            removeAttribute((String)e.nextElement());
        if (listeners != null)
                // TODO consider REVERSE calling listeners
	        for (EventListener listener: listeners) {
	            if (listener instanceof ServletContextListener)
	                ((ServletContextListener)listener).contextDestroyed(new ServletContextEvent(this));
	        }
        log("Destroy");
    }
    
    protected class SimpleDispatcher implements RequestDispatcher {
		HttpServlet servlet;
		String path;
        ServletAccessDescr sad;
        SimpleDispatcher(HttpServlet s, String p) {
            this(s, p, null);
        }
		SimpleDispatcher(HttpServlet s, String p, ServletAccessDescr d) {
			servlet = s;
			path = p;
            sad = d;
		}
        
        ////////////////////////////////////////////////////////////////////
        // interface RequestDispatcher
        
        public void forward(ServletRequest request,
                ServletResponse response) throws ServletException, java.io.IOException {
            servlet.service(new HttpServletRequestWrapper((HttpServletRequest)request) {
                public java.lang.String getPathInfo() {
                    if (path == null)
                        return super.getPathInfo();
                    int qp = path.indexOf('?');
                    int sp = sad == null?-1:path.indexOf(sad.servPath);
                    if (sp >= 0)
                        sp += sad.servPath.length();
                    if (sp > 0)
                        if (qp > sp)
                            return path.substring(sp, qp);
                        else
                            return path.substring(sp);
                    return path;                    
                }
                
                public String getServletPath() {
                     if (sad != null)
                         return sad.servPath;
                    return super. getServletPath();
                }
                
                public String getRequestURI() {
                    if (path == null)
                        if (sad != null)
                            return sad.servPath;
                        else
                            return null;
                    int qp = path.indexOf('?');
                    if (qp > 0)
                        return path.substring(0,qp);
                    return path;
                    
                }
                //public String getContextPath() {
                //    no cross context dispatching
                //}
                public String getQueryString() {
                    if (path == null)
                        return null;
                    int qp = path.indexOf('?');
                    if (qp > 0)
                        return path.substring(qp+1);
                    return null;                    
                }
                public Object getAttribute(String name) {
                    if ("javax.servlet.forward.request_uri".equals(name))
                        return super.getRequestURI();
                    else if ("javax.servlet.forward.context_path".equals(name))
                        return super.getContextPath();
                    else if ("javax.servlet.forward.servlet_path".equals(name))
                        return super.getServletPath();
                    else if ("javax.servlet.forward.path_info".equals(name))
                        return super.getPathInfo();
                    else if ("javax.servlet.forward.query_string".equals(name))
                        return super.getQueryString();
                    return super.getAttribute(name);
                }
            }, response);
        }
        
        public void include(ServletRequest request,
                final ServletResponse response) throws ServletException, java.io.IOException {
            // TODO: wrap response
			if (response instanceof Serve.ServeConnection)
			    ((Serve.ServeConnection)response).setInInclude(true);
			servlet.service(new HttpServletRequestWrapper((HttpServletRequest)request) {
				public Object getAttribute(String name) {
					if ("javax.servlet.include.request_uri".equals(name))
						return super.getRequestURI() ;
					else if ("javax.servlet.include.path_info".equals(name))
						return super.getPathInfo();
                    else if ("javax.servlet.include.context_path".equals(name))
                        return super.getContextPath();
                    else if ("javax.servlet.include.query_string".equals(name))
                        return super.getQueryString();
                    else if ("javax.servlet.include.servlet_path ".equals(name))
                        return super.getServletPath();
					return super.getAttribute(name);
				}
				            },			
							new HttpServletResponseWrapper((HttpServletResponse)response) {
					public void addDateHeader(java.lang.String name, long date) { }
					public void setDateHeader(java.lang.String name, long date) { }
					public void setHeader(java.lang.String name, java.lang.String value) { }
					public void addHeader(java.lang.String name, java.lang.String value) { }
					public void setIntHeader(java.lang.String name, int value) { }
					public void addIntHeader(java.lang.String name, int value) { }
					public void setStatus(int sc) {}
					public void setStatus(int sc, java.lang.String sm) {}
					public void sendRedirect(java.lang.String location) throws java.io.IOException { }
					public void sendError(int sc) throws java.io.IOException { }
					public void sendError(int sc, java.lang.String msg) throws java.io.IOException {}
					public void reset() { }
					public void setLocale(java.util.Locale loc) { }
					public void resetBuffer() { }
					public void setContentType(java.lang.String type) { }
					public void setContentLength(int len) { }
					public void setCharacterEncoding(java.lang.String charset) { }
				});
			if (response instanceof Serve.ServeConnection)
			    ((Serve.ServeConnection)response).setInInclude(false);
        }
    }
    
    //////////////// Filter methods /////////////////////
    protected class WebAppContextFilter implements Filter {
        String servPathHolder;
        WebAppContextFilter(String servletPath) {
            if (servletPath != null)
                servPathHolder = servletPath;
            else
                servPathHolder = "";
        }
        
        WebAppContextFilter() {
            this(null);
        }
        
        public void init(FilterConfig filterConfig) throws ServletException {
        }
        
        public void doFilter(final ServletRequest request, ServletResponse response,
                FilterChain chain) throws java.io.IOException, ServletException {
            final HttpServletRequest hreq = (HttpServletRequest)request;
            final HttpServletResponse hres = (HttpServletResponse)response; 
                chain.doFilter((HttpServletRequest)
   	                 Proxy.newProxyInstance(javax.servlet.http.HttpServletRequest.class.getClassLoader(),
                             new Class[] { javax.servlet.http.HttpServletRequest.class },
                             new InvocationHandler() {
	                     public Object invoke(Object proxy,
	                             Method method,
	                             Object[] args)
	                             throws Throwable {
	                         String mn = method.getName();
	                       if (mn.equals("getServletPath")) {
	                           String ruri = hreq.getRequestURI();
	                           int p = ruri.indexOf(servPathHolder,contextPath.length());
	                           if (_DEBUG) {
	                        	   System.err.printf("getServletPath: sph: '%s', ruri:%s, p: %d, contextP:%s,\n",
	                        		   servPathHolder, ruri, p, contextPath);
	                        	   System.err.printf("return:%s\n", ruri.substring(contextPath.length(), p+servPathHolder.length()));
	                           }
	                           return ruri.substring(contextPath.length(), p+servPathHolder.length());
	                       } else if (mn.equals("getPathInfo")) {
	                           String ruri = hreq.getRequestURI();
	                           int p = ruri.indexOf(servPathHolder,contextPath.length());
	                           int p1 = ruri.indexOf('?');
	                           if (_DEBUG)
	                               System.err.println("Requested pi:"+p+'/'+servPathHolder.length());
	                           if (p1<0)
	                               return ruri.substring(p+servPathHolder.length());
	                           else
	                               return ruri.substring(p+servPathHolder.length(),p1);
	                       } else if (mn.equals("getRealPath")) {
	                           if (_DEBUG)
		                           System.err.println("Path:"+args[0]);
	                           return getRealPath((String)args[0]);
	                       } else if (mn.equals("getPathTranslated")) {
	                           return getRealPath(hreq.getPathInfo());
	                       } else if (mn.equals("getRequestDispatcher")) {                               
	                           return getRequestDispatcher((String)args[0]);
						   } else if (mn.equals("getContextPath")) {
							   return contextPath;
						   } else if (mn.equals("getSession")) {
							   if (sessionListeners != null /*&& args != null && args.length==1 && (Boolean)args[0]*/) {
								   HttpSession session = (HttpSession)method.invoke(hreq, args);
								   if (session != null && session instanceof Serve.AcmeSession && session.isNew()) {
									     ((Serve.AcmeSession)session).setListeners(WebAppServlet.this.sessionListeners);
								   }
								   return session;
							   }
						   }
	                       return method.invoke(hreq, args);
	                     }
	                     }), //response);
	                     (HttpServletResponse)
	   	                 Proxy.newProxyInstance(javax.servlet.http.HttpServletResponse.class.getClassLoader(),
	                             new Class[] { javax.servlet.http.HttpServletResponse.class },
	                             new InvocationHandler() {
		                     public Object invoke(Object proxy,
		                             Method method,
		                             Object[] args)
		                             throws Throwable {
		                         String mn = method.getName();
		                         if (mn.equals("sendError")) {
		                             if (errorPages != null)
			                             for (ErrorPageDescr epd:errorPages) 
			                                 if (epd.errorCode == ((Integer)args[0]).intValue()) {
			                                     hres.sendRedirect(contextPath+'/'+epd.errorPage);
		        	                             return null;
		                	                 }
		                         } else if (mn.equals("sendRedirect")) {
									 //new Exception("Redirect "+args[0]).printStackTrace();
									 if (((String)args[0]).indexOf(":/") < 0) {
  										if (((String)args[0]).length() > 0 && ((String)args[0]).charAt(0) == '/') {
											 //args[0] = contextPath+((String)args[0]);
										}
									 }
		                         }
		                         return method.invoke(hres, args);
		                     }
		                     }));
        }
        
        public void destroy() {
            // destroy context filter
        }
        
        void setServletPath(String path) {
            servPathHolder = path;
        }
    }
    
    protected class SimpleFilterChain implements FilterChain {
        List<Filter> filters;
        Iterator<Filter> iterator;
        HttpServlet servlet;
        SimpleFilterChain() {
            filters = new ArrayList<Filter>();
        }
        
        public void doFilter(ServletRequest request, ServletResponse response)
         throws java.io.IOException, ServletException {
            if (iterator.hasNext()) 
                iterator.next().doFilter(request, response, this);
            else // call sevlet
                try {
               		servlet.service(request, response);
                } catch (IOException ioe) {
                    if (handleError(ioe, response) == false)
                        throw ioe;
                } catch (ServletException se) {
                    if (handleError(se, response) == false)
                        throw se;
                } catch(RuntimeException re) {
                    if (handleError(re, response) == false)
                        throw re;
                } 
        }
        
        protected boolean handleError(Throwable t, ServletResponse response) throws java.io.IOException {
            if (errorPages != null)
	            for (ErrorPageDescr epd:errorPages) {
        	        if (epd.exception != null && t.getClass().equals(epd.exception)) {
                	    log("redirected to "+epd.errorPage, t);
	                    ((HttpServletResponse)response).sendRedirect(epd.errorPage);
        	            return true;
	                }                        
        	    }
            return false;
        }
        
	    protected void reset() {
		    iterator = filters.iterator();
	    }
        
        protected void add(Filter f) {
            filters.add(f);
        }
        
        protected void setServlet(HttpServlet servlet) {
            this.servlet = servlet;
        }
    }
    
     private final static boolean _DEBUG = false;
}