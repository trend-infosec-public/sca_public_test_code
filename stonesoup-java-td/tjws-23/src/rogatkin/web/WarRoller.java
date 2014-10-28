/* tjws - WarRoller.java
 * Copyright (C) 2004-2006 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: WarRoller.java,v 1.12 2006/01/03 08:28:23 drogatkin Exp $
 * Created on Dec 13, 2004
 */
package rogatkin.web;

import java.io.*;
import java.util.zip.*;
import java.net.*;
import java.util.Enumeration;
import javax.servlet.ServletException;
import Acme.Serve.Serve;
import Acme.Serve.WarDeployer;

public class WarRoller implements WarDeployer {

    /**
     if deplpy mode
     scan for all wars in war directory (app deployment dir)
     for each war look in corresponding place of deploy directory
     figure difference, like any file in war exists and no corresponding file 
     in deploy directory or it's older
     if difference positive, then delete target deploy directory
     unpack war
     fi
     run mode
     process all WEB-INF/web.xml and build app descriptor, including
     context name, servlet names, servlet urls, class parameters
     process every app descriptor as standard servlet connection proc
     dispatch
     for every context name assigned an app dispatcher, it uses the rest to find servlet
     and do resource mapping
     
     */

    public void deploy(File warDir, final File deployTarDir) {
        // 
        // by list
        if (warDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isFile()
                        && pathname.getName().toLowerCase().endsWith(".war")) {
                    deployWar(pathname, deployTarDir);
                    return true;
                }
                return false;
            }
        }).length == 0)
            server.log("No web apps to deploy");
    }

    public void deployWar(File warFile, File deployTarDir) {
        String context = warFile.getName();
        assert context.toLowerCase().endsWith(".war");
        context = context.substring(0, context.length()-4);
        server.log("Deploying "+context);
        ZipFile zipFile = null;
        File deployDir = new File(deployTarDir, context);
        try {
            // some overhead didn't check that doesn't exist
            if (assureDir(deployDir) == false) {
                server.log("Can't reach deployment dir "+deployDir);
                return;
            }
            zipFile = new ZipFile(warFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while(entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();
                String en = ze.getName();
                if (File.separatorChar == '/')
                	en = en.replace('\\', File.separatorChar); 
                File outFile = new File(deployDir, en); 
                if (ze.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    OutputStream os = null;
                    InputStream is = null;
                    File parentFile = outFile.getParentFile();
                    if (parentFile.exists()==false)
                        parentFile.mkdirs();
                    if (outFile.exists() && outFile.lastModified() >= ze.getTime()) {
                            continue;
                    }
                    try {
                     os = new FileOutputStream(outFile);
                     is = zipFile.getInputStream(ze); 
                     copyStream(is, os);
                     outFile.setLastModified(ze.getTime());
                    } catch (IOException ioe2) {
                        server.log("problem in extracting "+en+" "+ioe2);
                    } finally {
                        try {
                            os.close();
                        } catch(Exception e2) {
                            
                        }
                        try {
                            is.close();
                        } catch(Exception e2) {
                            
                        }
                    }
                }
            }
        } catch(ZipException ze) {
            server.log("Invalid .war format");
        } catch(IOException ioe) {
            server.log("Can't read "+warFile+"/ "+ioe);
        } finally {
            try {
                zipFile.close();
                zipFile = null;
            } catch(Exception e) {
                
            }
        }
        try {
            attachApp(WebAppServlet.create(deployDir, context, server));
        } catch(ServletException se) {
            server.log("App "+context+" failed to create "+se.getRootCause());
        }
    }    

	public void attachApp(WebAppServlet appServlet) {
	    server.addServlet(appServlet.contextPath, appServlet);
	}

    public void deploy(Serve server) {
        this.server = server;
        String webapp_dir = System.getProperty("tjws.webappdir");
        if (webapp_dir == null)
            webapp_dir = System.getProperty("user.dir") + File.separator
                    + "webapps";
        File file_webapp = new File(webapp_dir);
        if (assureDir(file_webapp) == false) {
            server.log("Web app " + file_webapp
                    + " isn't a directory, deployment impossible.");
            return;
        }
        File file_deployDir = new File(file_webapp, "~web-apps~");
        if (assureDir(file_deployDir) == false) {
            server.log("Target deployment " + file_deployDir
                    + " isn't a directory, deployment impossible.");
            return;
        }
        deploy(file_webapp, file_deployDir);
    }
    
    protected boolean assureDir(File fileDir) {
        if (fileDir.exists() == false)
            fileDir.mkdirs();
        if (fileDir.isDirectory() == false) {
            return false;
        }
        return true;
    }

    static void copyStream(InputStream is, OutputStream os)
            throws IOException {
        byte[] buffer = new byte[10 * 1024];
        int len;
        long result = 0;
        long maxLen = 0;
        while ((len = is.read(buffer)) > 0) {
            os.write(buffer, 0, len);
            result += len;
            if (maxLen > 0 && result > maxLen)
                break;
        }
    }
    
    protected Serve server;
}