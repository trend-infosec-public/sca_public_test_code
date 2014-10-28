

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
* Date: 11/29/2011
*
* BASE PROGRAM -- FTP Server
*
* A basic FTP server.  Supports listing, uploading,
* and downloading files as an anonymous user.
*
* Variant Test Case Program
*
*
* source taint is socket
*
* STONESOUP Weakness Class: Number Handling
* CWE ID: CWE-239
* Variant Spreadsheet ID: 213
*
* Variant Features:
**		SOURCE_TAINT:SOCKET
**		DATA_TYPE:SHORT_CHARACTER
**		DATA_FLOW:PASS_BY_VALUE
**		CONTROL_FLOW:ELSE_CONDITIONAL
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
import stonesoup.Command;
import stonesoup.DataSocket;
import stonesoup.FileHandler;

public class FTPServer {

	private static ServerSocket socket;

	/** Server settings **/

	//# IPv4 only
	//private static String serverIP =System.getenv("FTPSERVERIP");
	//private static int serverPort=Integer.parseInt(System.getenv("FTPSERVERPORT"));
	private static String serverIP ="127.0.0.1";
	private static int serverPort=43221;

	private static File topDir = new File("");
	private static File currentDir = new File("");

	/** Input and output buffers for the command socket **/
	private static BufferedReader input = null;
	private static PrintWriter output = null;

	/** Sockets for receiving commands and data **/
	private static Socket cmdSocket;
	private static DataSocket dataSocket;

	private static boolean boolQuit = false;

	public static void main(String[] args) {

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
					while (((line = input.readLine()) != null) && (boolQuit == false)) {	//STONESOUP:INTERACTION_POINT	//STONESOUP:DATA_TYPE:SHORT_CHARACTER
						parseCommand(line);
					}

				}
				catch (IOException e) {
					System.err.println("ERROR: client connection failed: " + e.getMessage());
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
							System.err.println("ERROR: closing input stream: " + e.getMessage());
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
							System.err.println("ERROR: closing command socket: " + e.getMessage());
						}
					}

					//System.out.println("Client Disconnected");

				}
			}

		}
		catch (IOException e) {
			System.err.println("ERROR: while running server: " + e.getMessage());
			e.printStackTrace();
		}

		// close the command socket

		finally	{

			if (socket != null) {
				try {
					socket.close();
				}
				catch (IOException e) {
					System.err.println("ERROR: closing server socket: " + e.getMessage());
				}
			}

			//System.out.println("Exiting FTPServer");

		}

	}

	/**
	 * Reads a line containing a command and calls the corresponding functions
	 * @param line The line containing the command and any arguments
	 */
	private static void parseCommand(String line) {

		Command command = Command.UNKNOWN;

		// parse the first part of the line for the command

		String[] splitLine = line.split(" ");
		if (splitLine.length < 1) return;

		try {
			command = Command.valueOf(splitLine[0].toUpperCase());
		}
		catch (IllegalArgumentException e) {
			command = Command.UNKNOWN;
		}

		//System.out.println("-- command received = " + command.toString());

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
				list();
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

			case PUT:

				// ---------------------------------------------------------------------
				//
				// ARB - altered the check for two arguments, now a command that is missing
				// the filename will cause an exception and bring the server down.  (DoS)
				//

				if (splitLine.length == 2) {
					stor(splitLine[1]);
				}
				else {	//STONESOUP:CONTROL_FLOW:ELSE_CONDITIONAL
					stor(splitLine[1]);	//STONESOUP:DATA_FLOW:PASS_BY_VALUE	//STONESOUP:CROSSOVER_POINT	//STONESOUP:TRIGGER_POINT
				}

				//if (splitLine.length == 2) {
				//	stor(splitLine[1]);
				//}
				//else {
				//	output.println("501 Syntax error in parameters or arguments.");
				//	output.flush();
				//}
				//
				// ---------------------------------------------------------------------

				break;

			// ---------------------------------------------------------------------
			//
			//
			// The QUIT command is used to notify the server that it is time to shut
			// down.
			//
			//case QUIT:
			//	boolQuit = true;
			//	output.println("");	// Need this to exit program on quit
			//	output.flush();
			//	break;
			//
			//
			// ---------------------------------------------------------------------

			// The command that was received over the socket is not recognized by
			// this server.

			default:
				output.println("502 Unsupported Command");
				output.flush();
				break;

		}

		//System.out.println("-- finished executing command");

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

		if (!newDir.getAbsolutePath().startsWith(topDir.getName())) {
			output.println("550 Directory must be within the transfer directory.");
			output.flush();
			return;
		}

		// Everything looks go.  So go ahead and change the current
		// directory.

		currentDir = newDir;
		output.println("250 Directory successfully changed.");
		output.flush();
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
	private static void list() {

		// tell the client we are ready to send the listing
		output.println("150 Here comes the directory listing.");
        output.flush();

        String[] children = null;

        try {
            File tmpDir = new File(currentDir.getAbsolutePath());
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
					System.out.println("requested file not found");

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
