

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package stonesoup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
**  Main will run as a very contrived web server which listens on localhost:8080. The whole
**  HTTP part of the web server has been 'simplified' to a string that starts with
** 	either 'post' or 'get' and only supports basic file types.
**
**	POST - It takes a file name and file data as post input, uploads the file 
**  then "renders" the file.
** 
**  GET - Takes a filename as as input, then 'renders' that file to the client 
**
**  Rendering: 
**  .cif (Contrived Image Format) files: Prints to console - Think of these like JPG/GIF files but in black and white and can only contain letters and are stored as text.
**  .php I only implement a SMALL subset of the PHP language, but I implement it!
**  .* (anything) prints to console - Think of these like "anything else": text files etc (disregard the fact that .cif 'image' files are also text)
**
**
** Error: Allows users to upload non-image files (aka .cpl) that will get executed by the server upon request.
**
**
** STONESOUP Weakness Class: Unrestricted Upload of File with Dangerous Type
** CWE ID: CWE-434
** Variant Spreadsheet Rev #: ###
** Variant Spreadsheet ID: ###
**
** Variant Features:
**
**
** I/0 Pairs:
**   Good: 1st Set: HTTP_Poster seahorse.cif / HTTP_Getter seahorse.cif
**         2nd Set: 
**         3rd Set: HTTP_Poster restaurant.cif / HTTP_Getter restaurant.cif
**         4th Set: HTTP_Poster spam.cif / HTTP_Getter spam.cif
**         5th Set: HTTP_Poster whale.cif / HTTP_Getter whale.cif
**    Bad: 1st Set: HTTP_Poster malicious_hosts.php / HTTP_Getter malicious_hosts.php
**         2nd Set: 
**
** How program works:
**	  	There is a server and two clients. The server is a 'web server' that provides two pieces of functionality:
**		1. post to upload images
**		2. get to retrieve images.
************************************************************************/
public class WebServer_Bad extends Thread
{
	protected Socket s;
	public WebServer_Bad(Socket s)
	{
		this.s = s;
	}
	
	/**
	 * Reads the first line from the socket
	 * if the line is a 'post', attempts to process the post request.
	 * 		- Reads the filename, and prints the following data stream to webroot/images/<filename>
	 * 
	 * if the line is a 'get', attempts to process the get request
	 * 		- If it is a 'get' for an image, it renders the image
	 * 		- If it is a 'get' for a .cpl file, it executes the code in the file and returns it to the user (this is analagous to PHP/ASP/etc)
	 * 		- If it is anything else, it is rendered as text.
	 */
	public void run()
	{
		try
		{
			BufferedReader socketReader = new BufferedReader( new InputStreamReader( s.getInputStream() ) ); //STONESOUP:INTERACTION_POINT
			String line = socketReader.readLine();
			if(line.startsWith("quit"))
			{
				System.exit(0);
			}
			else if(line.startsWith("post"))//This is an image POST request
			{
				String filename = line.substring(line.indexOf("=") + 1);// The format is "post filename=<filename>" followed by file data
								
				if(filename != null)//STONESOUP:CROSSOVER_POINT
				{
					File fOut = new File("webroot/images/" + filename);
					PrintWriter fileWriter = new PrintWriter( new FileOutputStream( fOut ));
					while((line = socketReader.readLine()) != null)
					{
						fileWriter.println(line);
					}
					fileWriter.flush();
					fileWriter.close();
				}
				
			}
			else if (line.startsWith("get"))//This is a GET request
			{
				String shortName = line.substring(line.indexOf(" ") + 1);
				String toGet = null;
				
				//This makes sure path traversal attacks are nullified by enumerating the existing files and 
				//checking to see if input matches one of those files.
				File f = new File("webroot/images");
				String[] validFiles = f.list();
				for(int x = 0; x < validFiles.length; x++)
				{
					if(validFiles[x].equalsIgnoreCase(shortName))
						toGet = "webroot/images/" + validFiles[x];
				}
				
				PrintWriter socketWriter = new PrintWriter( s.getOutputStream() );
				
				if(toGet == null)
				{
					socketWriter.println("The requested file is not available.");
					socketWriter.flush();
					socketWriter.close();
					s.close();
					return;
				}
				
				File fToGet = new File(toGet);
				BufferedReader fileReader = new BufferedReader( new InputStreamReader( new FileInputStream( fToGet ) ) );
				String sFileLine;
				
				if(toGet.endsWith(".cif"))//Contrived image format
				{
					//TODO: Depending on image format, this may change
					while((sFileLine = fileReader.readLine()) != null)
					{
						socketWriter.println(sFileLine);
					}
				}
				else if (toGet.endsWith(".php")) //HERE WE GO! The best 30 lines or less php interpreter that money can buy! (I didn't say how much money)
												 //Basically, only the echo 'data'; and system('command'); methods are implemented.
												 //Oh, and multiline echo commands are not supported either
				{
					StringBuffer sBuf = new StringBuffer();
					while((sFileLine = fileReader.readLine()) != null)
					{
						sBuf.append(sFileLine);
					}
					
					Pattern p = Pattern.compile("(echo\\W*\\'.*?\\';|system\\(\\'.*?\\'\\);)");
					Matcher m = p.matcher(sBuf);
					while(m.find())
					{
						String sPhp = m.group();
						int iFirstTick = sPhp.indexOf("\'");
						int iLastTick = sPhp.lastIndexOf("\'");
						String sArgument = sPhp.substring(iFirstTick + 1, iLastTick);
						
						if(sPhp.startsWith("echo"))
						{				
							socketWriter.println(sArgument);
						}
						else if (sPhp.startsWith("system"))
						{
							Process proc = Runtime.getRuntime().exec(sArgument);//STONESOUP:TRIGGER_POINT
							if( proc.waitFor() != 0)
							{
								socketWriter.println("There was an error rendering the page");
								continue;
							}
							BufferedReader pIn = new BufferedReader(new InputStreamReader(proc.getInputStream()));
							String line2;
							while((line2 = pIn.readLine()) != null)
								socketWriter.println(line2);
						}
					}
				}
				else //General renderer for everything else
				{
					while((sFileLine = socketReader.readLine()) != null)
					{
						socketWriter.println(sFileLine);
					}
				}
				
				fileReader.close();
				socketWriter.flush();
				socketWriter.close();
			}

			s.close();//Close down this socket			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String args[])
	{
		try
		{
			ServerSocket s = new ServerSocket(8080);
			while(true)
			{
				Socket client = s.accept();
				WebServer_Bad tmp = new WebServer_Bad(client);
				tmp.start();
			}
		}
		catch(Exception e)
		{
			System.out.println("There was an error in the server: ");
			e.printStackTrace();
		}
	}
}
