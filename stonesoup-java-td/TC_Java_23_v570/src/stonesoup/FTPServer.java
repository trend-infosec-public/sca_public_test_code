

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
*	CWE ID: CWE-23
*
*		Description Summary
*			The software uses external input to construct a pathname that should be
*			within a restricted directory, but it does not properly neutralize
*			sequences such as ".." that can resolve to a location that is outside of
*			that directory.
*
*		Extended Description
*			This allows attackers to traverse the file system to access files or
*			directories that are outside of the restricted directory.
*
*	Variant Spreadsheet ID: 570
*	Variant Features:
**		SOURCE_TAINT:SOCKET
**		DATA_TYPE:ARRAY_LENGTH_FUNCTION_RETURN_VALUE
**		CONTROL_FLOW:COUNT_CONTROLLED_LOOP
**		DATA_FLOW:ARRAY_INDEX_FUNCTION_RETURN_VALUE
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
		FTPServer.serverIP = "127.0.0.1";
		FTPServer.serverPort = 1175;
		FTPServer.topDir = new File(System.getProperty("user.dir"));
		boolean boolQuit = false;
		//System.out.println("Starting FTPServer");

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
					while ((line = input.readLine()) != null) {	//STONESOUP:INTERACTION_POINT
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

			//System.out.println("Exiting FTPServer");

		}

		System.out.println("Server halting.");
	}

	/**
	 * Reads a line containing a command and calls the corresponding functions
	 * @param line The line containing the command and any arguments
	 */
	private static boolean parseCommand(String line) {

		Command command = Command.UNKNOWN;

		// parse the first part of the line for the command

		String[] splitLine = line.split(" ");
		if (splitLine.length < 1) return true;

		try {
			command = Command.valueOf(splitLine[0].toUpperCase());
		}
		catch (IllegalArgumentException e) {
			command = Command.UNKNOWN;
		}

//		System.out.println("-- command received = " + command.toString());

		switch (command) {

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

	private static int ret_int_val(int val) {
		return val;
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
			System.out.println("Directory must be within the transfer directory.");	//STONESOUP:CROSSOVER_POINT
//			output.println("550 Directory must be within the transfer directory.");
//			output.flush();
//			return;
		}
		File [] filar = new File [ret_int_val(5)];	//STONESOUP:DATA_TYPE:ARRAY_LENGTH_FUNCTION_RETURN_VALUE
		filar[1] = null;
		filar[ret_int_val(2)] = newDir;	//STONESOUP:DATA_FLOW:ARRAY_INDEX_FUNCTION_RETURN_VALUE
		filar[3] = null;

		// Everything looks go.  So go ahead and change the current
		// directory.
		for (int i = 0; i < 85; i++) {	//STONESOUP:CONTROL_FLOW:COUNT_CONTROLLED_LOOP
			currentDir = filar[2];	//STONESOUP:TRIGGER_POINT
			output.println("250 Directory successfully changed.");
			output.flush();
			if (filar[2] != null) {
				break;
			}
		}
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

		        if (children[i].equalsIgnoreCase("commands_1.txt")) System.out.println("success");
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
