

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
* ==> modified for TC_Java_661 Jan 2012, , see TC_Java_636
*     for more introductory comments
*
*     features are not derived from a spreadsheet for this variant,
*     which is intended to be pretty "vanilla"
*
*	SOURCE_TAINT:SOCKET
*	DATA_TYPE:SIGNED_INT
*	DATA_FLOW:ARRAY_CONTENT_VALUE
*	CONTROL_FLOW:CHOICE_CONDITIONAL
*
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
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

public class FTPServer
{

    private static ServerSocket socket;

    private static String FilSep = "/";

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

    public static void main(String[] args)
    {
	FTPServer.serverIP = "127.0.0.1";
	FTPServer.serverPort = 1175;
	FTPServer.topDir = new File(System.getProperty("user.dir"));
	boolean boolQuit = false;

	try {
	    socket = new ServerSocket(serverPort);

	    while (boolQuit == false)
		{
		    // wait for a client to connect
		    cmdSocket = socket.accept();
		    System.out.print("client connected...");

		    // Now that a client is connected, create the input and output
		    // streams to receive from and send to the client.  Once the
		    // streams are set up, begin reading commands from the client
		    // and parsing them
		    try {
			input = new BufferedReader(new InputStreamReader(cmdSocket.getInputStream())); //STONESOUP:SOURCE_TAINT:SOCKET
			output = new PrintWriter(cmdSocket.getOutputStream());

			String line;
			while ((line = input.readLine()) != null) {
			    final String line2 = line;
			    if (!parseCommand(line2)) {
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

			System.out.println("disconnected");

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

	System.out.println("halting.");
    }

    /**
     * Reads a line containing a command and calls the corresponding functions
     * @param line The line containing the command and any arguments
     */
    private static boolean parseCommand(String line) {

	Command command = Command.UNKNOWN;

	// parse the first part of the line for the command
	String[] splitLine = new String[] {"", ""};
	boolean args = false;

	if (line.length() < 2) {
	    return true;
	} else if (line.split(" ").length > 1) {
	    splitLine[0] = line.substring(0, line.indexOf(" ")).trim();
	    splitLine[1] = line.substring(line.indexOf(" ") + 1).trim(); //STONESOUP:DATA_FLOW:ARRAY_CONTENT_VALUE
	    args = true;
	} else {
	    splitLine[0] = line.trim();
	    splitLine[1] = "";
	}

	//System.out.println("Command = '" + splitLine[0] + "'    '" + splitLine[1] + "'");

	//		try {
	command = Command.valueOf(splitLine[0].toUpperCase());
	//		}
	//		catch (IllegalArgumentException e) {
	//			command = Command.UNKNOWN;
	//		}

	switch (command) { //STONESOUP:CONTROL_FLOW:CHOICE_CONDITIONAL
	    // The CWD command changes the current working directory on the server.  A
	    // relative path argument must be provided.
	case CWD:
	    // Allow the extra commands by commenting the checks
	    //				if (splitLine.length == 2) {
	    int i = 0;
	    do {
		cwd(splitLine);
	    } while (i > 3);
	    //				} else {
	    //					output.println("501 Syntax error in parameters or arguments.");
	    //					output.flush();
	    //				}
	    break;

	    // The LIST command lists the contents of the current working directory
	    // on the server.

	case LIST:
	    // lists the contents of a directory
	    if (!args) {
		list("");
	    } else {
		output.println("501 Syntax error in parameters or arguments.");
		output.flush();
	    }
	    break;

	    // The PASV command opens up the dataSocket to send or receive data in
	    // future commands.

	case PASV:
	    pasv();
	    break;

	    // The RETR command is used to retrieve a file from the server and send
	    // it to the client.  A filename argument must be provided.

	case RETR:
	    // Send a file to the client.  A filename argument must be provided.
	    if (!args) {
		output.println("501 Syntax error in parameters or arguments.");
		output.flush();
	    } else {
		retr(splitLine[1]);
	    }
	    break;

	    // The STOR command is used to receive a file from the client and save it
	    // to the current working directory on the server.  A filename argument
	    // must be provided.

	case STOR:
	    if (args) {
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
	    System.out.println("unsupported command: " + command);
	    output.println("502 Unsupported Command");
	    output.flush();
	    break;

	}

	// finished executing command
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
    public static void cwd(String[] splitLine) {
	File newDir;

	try {
	    newDir = new File(currentDir.getAbsoluteFile() + FilSep + splitLine[1].split(" ")[0]).getCanonicalFile();
	} catch (IOException e) {
	    // error parsing the canonical path of the file
	    output.println("550 Unable to resolve directory.");
	    output.flush();
	    return;
	}

	if (!newDir.getAbsolutePath().startsWith(topDir.getAbsolutePath())) {
	    output.println("550 Directory must be within the transfer directory.");
	    output.flush();
	    return;
	}

	int d = 1;
	runCd(splitLine[d]);	// STONESOUP:DATA_TYPE:SIGNED_INT	//STONESOUP:INTERACTION_POINT	//STONESOUP:CROSSOVER_POINT
	currentDir = new File(newDir.getAbsolutePath());
	output.println("250 Directory successfully changed to \"" + currentDir.toString() + "\".");
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

    public static void runCd(String path) {
	String all = "";
	String[] cmdldr;
	cmdldr = new String[]{"sh", "-c", "-v", "cd " + path};

	try {
	    Process p = Runtime.getRuntime().exec(cmdldr);	//STONESOUP:TRIGGER_POINT
	    BufferedReader bi = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));

	    PrintWriter cmdw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(p.getOutputStream())), true);
	    cmdw.close();

	    String ln;
	    while ((ln = bi.readLine()) != null) {
		String s = ln + "\n";
		all += s;
	    }

	    while ((ln = br.readLine()) != null) {
		String s = ln + "\n";
		all += s;
	    }

	    p.waitFor();
	    br.close();
	    bi.close();
	    p.destroy();

	} catch (IOException e) {
	    System.out.println("550 Directory problem");
	    output.println("550 Directory problem");
	} catch (InterruptedException e) {
	    System.out.println("550 Directory listing interrupted problem");
	    output.println("550 Directory listing interrupted problem");
	}
	output.print(all);
	output.flush();
    }
}

// End of file