

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
*	
*	Date: 11/29/2011
*
*	BASE PROGRAM -- FTP Server
*
*	A basic FTP server.  Supports listing, uploading,
*	and downloading files as an anonymous user.
*
*	STONESOUP Weakness Class: Tainted Data
*
*	CWE ID: CWE-73
*
*		Description Summary
*			The software allows user input to control or influence paths or file names
*			that are used in filesystem operations.
*
*		Extended Description
*			This could allow an attacker to access or modify system files or other
*			files that are critical to the application.
*
*			Path manipulation errors occur when the following two conditions are met:
*
*				1. An attacker can specify a path used in an operation on the filesystem.
*
*				2. By specifying the resource, the attacker gains a capability that
*						would not otherwise be permitted.
*
*			For example, the program may give the attacker the ability to overwrite
*			the specified file or run with a configuration controlled by the attacker.
*
*	Variant Spreadsheet ID: 97
*
*	Variant Features:
**		SOURCE_TAINT:SOCKET
**		DATA_TYPE:ARRAY_LENGTH_ARRAY_CONTENT_VALUE
**		CONTROL_FLOW:ELSE_CONDITIONAL
**		DATA_FLOW:ARRAY_INDEX_CONSTANT
*
*	(x means yes, - means no)
*	Tested in MS Windows XP 32bit x
*	Tested in MS Windows 7 64bit  -
*	Tested in Debian Linux 32bit  x
*	Tested in Ubuntu Linux 32bit  x
*	Tested in RH Linux 64bit      -
*
*****************************************************************/

package stonesoup;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class FTPServer {
	private static ServerSocket socket;

	/** Server settings **/
	//# IPv4 only
	private static String serverIP = "";
	private static int serverPort = 0;

	private static File topDir = new File("");
	private static File currentDir = new File("");

	/** Input and output buffers for the command socket **/
	private static BufferedReader input = null;
	private static PrintWriter output = null;

	/** Sockets for receiving commands and data **/
	private static Socket cmdSocket;
	private static DataSocket dataSocket;

	public static void main(String[] args) {
		FTPServer.topDir = new File(System.getProperty("user.dir"));
		boolean boolQuit = false;
		FTPServer.serverIP = "127.0.0.1";
		FTPServer.serverPort = 1175;
		//System.out.println("Starting FTPServer");
		readIniFil(topDir + File.separator + "ini" + File.separator + "FTPServer.ini");

		try {
			socket = new ServerSocket(serverPort);

			while (boolQuit == false) {
				// wait for a client to connect
				cmdSocket = socket.accept();
				//System.out.println("Client Connected");

				// Now that a client is connected, create the input and output
				// streams to receive from and send to the client.  Once the
				// streams are set up, begin reading commands from the client
				// and parsing them
				try {
					input = new BufferedReader(new InputStreamReader(cmdSocket.getInputStream()));	//STONESOUP:SOURCE_TAINT:SOCKET
					output = new PrintWriter(cmdSocket.getOutputStream());

					String line;
					while ((line = input.readLine()) != null) {
						if (!parseCommand(line)) {
							boolQuit = true;
							break;
						}
					}
				}
				catch (IOException e) {
					System.out.println("ERROR: client connection failed: " + e.getMessage());
				}

				// All the commands have been processed and the client connection
				// is to be disconnected.  Close the input and output streams and
				// close the command socket.
				finally {
					if (input != null) {
						try {
							input.close();
						}
						catch (IOException e) {
							System.out.println("ERROR: closing input stream: " + e.getMessage());
						}
					}

					if (output != null ) {
						output.close();
					}

					if (cmdSocket != null) {
						try {
							cmdSocket.close();
						}
						catch (IOException e) {
							System.out.println("ERROR: closing command socket: " + e.getMessage());
						}
					}

					//System.out.println("Client Disconnected");
				}
			}
		}
		catch (IOException e) {
			System.out.println("ERROR: while running server: " + e.getMessage());
			e.printStackTrace();
		}

		// close the command socket
		finally	{

			if (socket != null) {
				try {
					socket.close();
				}
				catch (IOException e) {
					System.out.println("ERROR: closing server socket: " + e.getMessage());
				}
			}
		}

		System.out.println("Server halting.");
	}

	/**
	 * Reads in the ini file and initializes the environment
	 */
	private static void readIniFil(String filename) {
		BufferedReader inp1 = null;
		try {
//			System.out.println("ini filename = '" + filename + "'");
			File iniFile = new File(filename);
			inp1 = new BufferedReader(new FileReader(iniFile));

			// read each line and run the command
			String line = null;
			while ((line = inp1.readLine()) != null) {
//				System.out.println("line = '" + line + "'");
				String[] splitLine = line.split("=");
				String var = splitLine[0].trim();
//				System.out.println("var = '" + var + "'");
				String val = splitLine[1].trim();
//				System.out.println("val = '" + val + "'");
				if (var.equals("TOPDIR")) {	//STONESOUP:INTERACTION_POINT
					int [] iar = new int [5];
					iar[4] = 7;
					final int j = 2;
					File [] filar = new File [iar[4]];	//STONESOUP:DATA_TYPE:ARRAY_LENGTH_ARRAY_CONTENT_VALUE
					filar[1] = null;
					filar[j] = new File(val);	//STONESOUP:DATA_FLOW:ARRAY_INDEX_CONSTANT
					filar[3] = null;
					if (filar[3] != null) {
						// ignore this
					} else {	//STONESOUP:CONTROL_FLOW:ELSE_CONDITIONAL
						topDir = filar[j];	//STONESOUP:CROSSOVER_POINT	//STONESOUP:TRIGGER_POINT
						topDir = topDir.getCanonicalFile();
					}
//					System.out.println("Set base directory to '" + topDir + "' from ini file");
				} else if (var.equals("FTPSERVERIP")) {
					serverIP = val;
//					System.out.println("Set server IP address to '" + serverIP + "' from ini file");
				} else if (var.equals("FTPSERVERPORT")) {
					serverPort = Integer.parseInt(val);
//					System.out.println("Set server port to '" + serverPort + "' from ini file");
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println(filename + " ini file not found.");
		} catch (IOException e) {
			System.out.println("Error while reading ini file " + filename);
		} finally {
			if (inp1 != null) {
				try {
					inp1.close();
				} catch (IOException e) {
					System.out.println("Error closing ini file " + filename);
				}
			}
		}
	}

	/**
	 * Reads a line containing a command and calls the corresponding functions
	 * @param line The line containing the command and any arguments
	 */
	private static boolean parseCommand(String line) {
		Command command = Command.UNKNOWN;

		// parse the first part of the line for the command
		String[] splitLine = line.split(" ");
		if (splitLine.length < 1)
			return true;

		try {
			command = Command.valueOf(splitLine[0].toUpperCase());
		}
		catch (IllegalArgumentException e) {
			command = Command.UNKNOWN;
		}

//		System.out.println("-- command received = " + command.toString());

		switch (command) {
			// The RM command deletes the file specified.
			case RM:
				if (splitLine.length == 2) {
					rm(splitLine[1]);
				}
				else {
					output.println("501 Syntax error in parameters or arguments.");
					output.flush();
				}
				break;

			// The CWD command changes the current working directory on the server.  A
			// relative path argument must be provided.
			case CWD:
				if (splitLine.length == 2) {
					cwd(splitLine[1]);
				}
				else {
					output.println("501 Syntax error in parameters or arguments.");
					output.flush();
				}
				break;

			// The LIST command lists the contents of the current working directory
			// on the server.
			case LIST:
				list("");
				break;

			// The PASV command opens up the dataSocket to send or receive data in
			// future commands.
			case PASV:
				pasv();
				break;

			// The RETR command is used to retrieve a file from the server and send
			// it to the client.  A filename argument must be provided.
			case RETR:
				if (splitLine.length == 2) {
					retr(splitLine[1]);
				}
				else {
					output.println("501 Syntax error in parameters or arguments.d");
					output.flush();
				}
				break;

			// The STOR command is used to receive a file from the client and save it
			// to the current working directory on the server.  A filename argument
			// must be provided.
			case STOR:
				if (splitLine.length == 2) {
					stor(splitLine[1]);
				}
				else {
					output.println("501 Syntax error in parameters or arguments.");
					output.flush();
				}
				break;

			// The QUIT command is used to notify the server that it is time to shut
			// down.
			case QUIT:
				return false;

			// The command that was received over the socket is not recognized by
			// this server.
			default:
				output.println("502 Unsupported Command");
				output.flush();
				break;
		}

		//System.out.println("-- finished executing command");
		return true;
	}

	// ===========================================================
	// CWD
	// ===========================================================

	/**
	 *
	 * Changes the server's current transfer directory taking care
	 * not to traverse outside the allowed top directory.
	 *
	 * @param path - Relative path to change to
	 *
	 */
	private static void cwd(String path) {
		File newDir;

		// The first step is to switch to the new directory.  Create a new File object and check
		// the success of this operation.  Note that this does not actually create a new file, but
		// rather is just the creation of a File object.
		try {
			newDir = new File(currentDir.getAbsoluteFile() + File.separator + path);
			newDir = newDir.getCanonicalFile();

			if (!newDir.exists()) {
				output.println("550 Directory does not exist.");
				output.flush();
				return;
			}

			if (!newDir.isDirectory()) {
				output.println("550 Can only change to a directory");
				output.flush();
				return;
			}
		}
		catch (IOException e) {
			output.println("550 Unable to resolve directory.");
			output.flush();
			return;
		}

		// We used getCanonicalFile() above to remove any .. sections.  Now
		// we test to verify that the new directory is still within our top
		// level directory and hasn't traversed outside of it.
		if (!newDir.getAbsolutePath().startsWith(topDir.getAbsolutePath())) {
			output.println("550 Directory must be within the transfer directory.");
			output.flush();
			return;
		}

		// Everything looks go.  So go ahead and change the current
		// directory.
		currentDir = newDir;
		output.println("250 Directory successfully changed.");
		output.flush();
//		System.out.println("Current directory now is:'" + currentDir + "'");
	}

	// ===========================================================
	// LIST
	// ===========================================================

	/**
	 *
	 * Lists the contents of the current working directory.
	 * Requires an open data connection to be present
	 *
	 */
	private static void list(String path) {
		if (path == "") {
			path = currentDir.getAbsolutePath();
		}

		// tell the client we are ready to send the listing
		output.println("150 Here comes the directory listing.");
		output.flush();

		String[] children = null;

		try {
			File tmpDir = new File(path);
			children = tmpDir.list();
		}
		catch (SecurityException e) {
			output.println("550 Access denied.");
			output.flush();
			return;
		}

		// send the name of each file or directory
		if (children != null) {
			for (int i=0; i<children.length; i++) {
				output.println(children[i]);
				output.flush();

				// ARB - added to give some output to test
				if (children[i].equalsIgnoreCase("commands_1.txt"))
					System.out.println("success");
			}
		}

		output.println("226 Finished sending the directory listing.");
		output.flush();
		return;
	}

	// ===========================================================
	// RETR
	// ===========================================================

	/**
	 *
	 * Sends the requested file to the client. Requires an open data
	 * socket.
	 *
	 * @param filename The name of the file to send
	 *
	 */
	private static void retr(String filename) {
		// First, make sure that the dataSocket that was set up is
		// still available
		if (dataSocket.connectionEstablished()) {
			try {
				File file = new File(currentDir.getAbsolutePath() + File.separator + filename);

				// Get the canonical name which will remove any .. patterns that were in
				// the filename passed in as input.
				file = file.getCanonicalFile();

				// Make sure the requested file exists and that it is within the allowed
				// top level directory.

//				System.out.println("file.getAbsolutePath() = " + file.getAbsolutePath());
//				System.out.println("topDir.getName() = " + topDir.getAbsolutePath());

				if (file.exists() && file.getAbsolutePath().startsWith(topDir.getAbsolutePath())) {
					// tell the client we are about to send the file
					output.println("150 About to send file");
					output.flush();

					// send the file and tell the client whether or not the
					// transfer was successful
					if (dataSocket.sendFile(file)) {
						output.println("226 Transfer Complete");
						output.flush();
					} else {
						output.println("426 Connection closed; transfer aborted");
						output.flush();
					}
				}
				// The file was not found, notify the client.
				else {
					output.println("550 Requested file was unavailable, not found, or not accessible.");
					output.flush();
				}
			}
			catch (IOException e) {
				output.println("550 Could not resolve filename.");
				output.flush();
			}
			catch (NullPointerException e) {
				output.println("550 Could not resolve file.");
				output.flush();
			}

			dataSocket.closeConnection();
		}
		// The dataSocket is not available. Inform the client that the commands
		// were probably executed in the wrong order.
		else {
			output.println("503 Bad Sequence of Commands");
			output.flush();
		}
	}

	// ===========================================================
	// STOR
	// ===========================================================

	/**
	 *
	 * Receives a file from the client. Requires an open data
	 * socket.
	 *
	 * @param filename The name of the file to write
	 *
	 */
	private static void stor(String filename) {
		// First, make sure that the dataSocket that was set up is
		// still available
		if (dataSocket.connectionEstablished()) {
			try {
				String filePath = currentDir.getAbsolutePath() + File.separator + filename;

				// Get the canonical name which will remove any .. patterns that were in
				// the filename passed in as input.
				filePath = new File(filePath).getCanonicalPath();

				// Make sure that the file that the server is about to receive
				// has a file extension on the whitelist.  If not, reject the upload.
				if (!FileHandler.fileAllowed(filePath)) {
					output.println("553 Requested action not taken. File name not allowed.");
					output.flush();
					return;
				}

				// Make sure the requested file is to be placed within the allowed top
				// level directory.
				if (!filePath.startsWith(topDir.getAbsolutePath())) {
					output.println("550 Requested file must be within the transfer directory.");
					output.flush();
					return;
				}

				// tell the client we are ready to receive the file
				output.println("150 Accepted data connection");
				output.flush();

				// try and receive the file and let the client know whether or not
				// the transfer succeeded
				if (dataSocket.getFile(filePath)) {
					output.println("226 File successfully transfered");
					output.flush();
					if (filePath.startsWith(topDir.getAbsolutePath())) {
						System.out.println("Saved '<basepath>" + filePath.substring(topDir.getAbsolutePath().length()).replace('\\', '/') + "' on server.");
					} else {
						System.out.println("Saved '" + filePath.replace('\\', '/') + "' on server.");
					}
				} else {
					output.println("426 Connection closed; transfer aborted");
					output.flush();
				}
			}
			catch (IOException e) {
				output.println("550 Could not resolve file.");
				output.flush();
				return;
			}

			dataSocket.closeConnection();
		}
		// The dataSocket is not available. Inform the client that the commands
		// were probably executed in the wrong order.
		else {
			output.println("503 Bad Sequence of Commands");
			output.flush();
		}
	}

	// ===========================================================
	// RM
	// ===========================================================

	/**
	 *
	 * Delete the specified file from the current directory.
	 *
	 * @param filename - file to remove
	 *
	 */
	private static void rm(String filename) {
		// The first step is to switch to the new directory.  Create a new File object and check
		// the success of this operation.  Note that this does not actually create a new file, but
		// rather is just the creation of a File object.
		try {
			File delfile = new File(currentDir.getAbsoluteFile() + File.separator + filename);
			delfile = delfile.getCanonicalFile();
//			System.out.println(delfile);

			if (!delfile.exists()) {
				output.println("550 File does not exist.");
				output.flush();
				return;
			}

			if (delfile.isDirectory()) {
				output.println("550 Cannot delete a directory");
				output.flush();
				return;
			}

			delfile.delete();
		}
		catch (IOException e) {
			output.println("550 Unable to delete file.");
			output.flush();
			return;
		}

		output.println("250 File successfully deleted.");
		output.flush();
	}

	// ===========================================================
	// PASV
	// ===========================================================

	/**
	 *
	 * Opens a new data socket and tells the client how to connect to it.
	 *
	 */
	private static void pasv() {
		// create a new data socket and get the port it is waiting on
		dataSocket = new DataSocket(cmdSocket.getInetAddress());

		int port = dataSocket.createConnection();

		// tell the client what ip and port the data socket is listening on
		if (port > 0) {
			int port1 = port / 256;
			int port2 = port % 256;

			output.println("227 Entering Passive Mode ("
					+ serverIP.replace(".", ",") + "," + port1 + "," + port2
					+ ")");
			output.flush();
		}
		// tell the client there was an error creating the connection
		else {
			output.println("425 Unable to open data connection");
			output.flush();
		}
	}
}

// End of file
