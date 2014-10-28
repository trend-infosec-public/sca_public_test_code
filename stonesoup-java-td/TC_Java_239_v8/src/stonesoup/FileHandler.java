

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/****************************************************************
*
*
* Handles writing a file received over an output stream and sending a local
* file over an output stream
*****************************************************************/

package stonesoup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileHandler {

	private static String[] allowedFileTypes = {"TXT","JPG","BMP"};

	/**
	 * Creates a file locally that is received from an input stream
	 * 
	 * @param fileName - The name of the file to be created
	 * @param stream - An input stream of data we get from the server
	 * @throws IOException
	 */
	public static void createFile(String filePath, BufferedInputStream stream) throws IOException {
		
		// wait at most ten seconds for data to arrive
		int sleepCount = 0;
		while (sleepCount < 200 && stream.available() <= 0) {
			try {
				Thread.sleep(50);
				sleepCount++;
			} catch (InterruptedException e) {}
		}
		
		if (stream.available() < 1) {
			System.err.println("[!] No data received to create a file after 10 seconds");
			return;
		}
		
		OutputStream out = null;
		try {
			out = new FileOutputStream(filePath);
			
			// read the stream and write it to a file
			byte[] buf = new byte[1024];
			while ((stream.available()) > 0) {
				out.write(buf, 0, stream.read(buf));
			}
			out.flush();
		} catch (SecurityException e) {
			System.err.println("[!] Insufficient permissions to create this file");
			return;
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/**
	 * Reads a file and sends it via an output stream
	 * 
	 * @param file The file to send
	 * @param output The output stream to write to
	 * @throws IOException
	 */
	public static void sendFile(File file, BufferedOutputStream output) throws IOException {
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			
			byte[] buf = new byte[1024];
			int len;

			while ((len = in.read(buf)) > 0)
				output.write(buf, 0, len);
			output.flush();
		} catch (SecurityException e) {
			System.err.println("[!] Insufficient permissions to create this file");
			return;
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
	
	/**
	 * Checks the filename's extension to see if it is on the
	 *  extension whitelist
	 * @param filename - The name of the file to check
	 * @return - true if the file is allowed, otherwise false
	 */
	public static boolean fileAllowed(String filename) {
		
		String[] splitName = filename.split("[.]");

		if (splitName.length < 1) return false;

		String fileExtension = splitName[splitName.length - 1];
		
		for (String ext : allowedFileTypes) {
			if (fileExtension.equalsIgnoreCase(ext)) return true;
		}
		
		return false;
	}
	
	/**
	 * Creates a file listing entry for the requested file with a directory
	 * indicator, file size, last modified date, and file name.
	 * 
	 * @param file The file to create the listing for
	 * @return The listing for the requested file
	 */
	public String listFile(File file) {
		Date date = new Date(file.lastModified());
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd hh:mm");
		String dateStr = dateFormat.format(date);

		String returnStr = "";
		returnStr += file.isDirectory() ? 'd' : '-';
		returnStr += " ";
		returnStr += file.length() / 1024;
		returnStr += "." + (""+(file.length() % 1024)).charAt(0);
		returnStr += "k ";
		returnStr += dateStr;
		returnStr += " ";
		returnStr += file.getName();
		
		return returnStr;
	}
}
