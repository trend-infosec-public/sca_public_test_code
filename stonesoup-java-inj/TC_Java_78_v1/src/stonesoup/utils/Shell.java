/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.utils;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import stonesoup.Configuration;

/**
 * Provides an interface to the underlying OS shell to execute one or more commands.
 * 
 * 
 *
 */
public class Shell {

	private class ShellWait implements Callable<Integer> {

		private Process process = null;
		
		public ShellWait(Process process) {
			this.process = process;
		}
		
		@Override
		public Integer call() throws Exception {
			Integer result = null;
			try {
				result = new Integer(this.process.waitFor());
			} catch (InterruptedException e) {
				result = null;
			}
			
			return result;
		}
		
	}
	
	private String linuxShell = "bash";
	private String windowsShell = "cmd.exe";
	private String linuxShellCommandParam = "-c";
	private String windowsShellCommandParam = "/C";
	private String osName = System.getProperty("os.name");
	private String shell = null;
	private String shellCommandParam = null;
	private File workingDirectory = null;
	
	public Shell() {
		this(null);
	}
	
	public Shell(String workingDirectory) {
		this.linuxShell = Configuration.Instance.getProperty("Shell.linux", "bash");
		this.windowsShell = Configuration.Instance.getProperty("Shell.windows", "cmd.exe");
		this.linuxShellCommandParam = Configuration.Instance.getProperty("Shell.linuxParam", "-c");
		this.windowsShellCommandParam = Configuration.Instance.getProperty("Shell.windowsParam", "/C");
		
		if (this.osName.contains("Windows")) {
			this.shell = this.windowsShell;
			this.shellCommandParam = this.windowsShellCommandParam;
		} else {
			this.shell = this.linuxShell;
			this.shellCommandParam = this.linuxShellCommandParam;
		}
		
		if (workingDirectory != null) {
			this.workingDirectory = new File(workingDirectory);
			if (!this.workingDirectory.isDirectory()) {
				this.workingDirectory = null;
			}
		}
	}
	
	public String getShellName() {
		return this.shell;
	}
	
	public String getOSName() {
		return this.osName;
	}
	
	public File getWorkingDirectory() {
		return this.workingDirectory;
	}
	
	private ProcessBuilder createShell(String command) throws Exception {
		ProcessBuilder procBuilder = new ProcessBuilder();
		
		if (this.workingDirectory != null)
			procBuilder.directory(this.workingDirectory);
		
		List<String> exec = new ArrayList<String>();
		exec.addAll(Arrays.asList(new String[] { this.shell, this.shellCommandParam }));
		exec.add(command);
		
		procBuilder.command(exec);
		
		return procBuilder;
	}
	
	public List<String> execute(String command) throws Exception {
		//prep the process
		ProcessBuilder builder = this.createShell(command);
		
		//start the process
		Process process = builder.start();
		
		//get the streams
		InputStreamReader stdout = new InputStreamReader(process.getInputStream());
		InputStreamReader stderr = new InputStreamReader(process.getErrorStream());
		//OutputStreamWriter stdin = new OutputStreamWriter(process.getOutputStream());
		
		//wait for the process to exit
		ExecutorService executor = Executors.newSingleThreadExecutor();
	    Integer exitCode = null;
	    try {
	    	// start working
	        Future<Integer> result = executor.submit(new ShellWait(process));
	        try {
	        	exitCode = result.get(30, TimeUnit.SECONDS);
	        } catch (ExecutionException e) {
	        	e.getCause().printStackTrace();
	        	throw (Exception)e.getCause();
	        } catch (TimeoutException e) {
	        	result.cancel(true);
	        	exitCode = null;
	        }
	    } finally {
	      executor.shutdownNow();
	    }
	    
	    if (exitCode == null) {
	    	return null;
	    }
	    
	    List<String> output = new ArrayList<String>();
	    
	    output.add(this.readFully(stdout));
	    output.add(this.readFully(stderr));
	    
	    process.destroy();
	    
	    return output;
	}
	
	public List<String> execute(List<String> command) throws Exception {
		StringBuilder buffer = new StringBuilder();
		
		for (String arg : command) {
			buffer.append(arg);
			buffer.append(' ');
		}
		
		return this.execute(buffer.substring(0, buffer.length()-1));
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
	
}
