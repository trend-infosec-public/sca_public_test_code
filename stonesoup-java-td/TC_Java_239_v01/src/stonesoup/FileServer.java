/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

/**********************************************************************
**
**
** 
** Date: 01/25/2012
**
** Variant Spreadsheet Rev #:
** Variant Spreadsheet ID:
**
** Variant Features:
**		SOURCE_TAINT:SOCKET
**		DATA_TYPE:SHORT_CHARACTER
**		CONTROL_FLOW:SEQUENCE
**		DATA_FLOW:SIMPLE
**
** Initial Testing: (x means yes, - means no)
**   Tested in MS Windows XP 32bit   -
**   Tested in MS Windows 7  64bit   -
**   Tested in Debian Linux 32bit    x
**
**********************************************************************/

package stonesoup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/*
 * Request Message Format
 *
 * 4 Bytes: Username Length
 * N Bytes: Username
 * 4 Bytes: Password Length
 * N Bytes: Password
 * 4 Bytes: Operation
 * 4 Bytes: Target File Length
 * N Bytes: Target File
 * 4 Bytes: Target User Length
 * N Bytes: Target User
 *
 *
 *
 * Response Message Format
 *
 * 4 Bytes: Result
 * 4 Bytes: File Owner Length
 * N Bytes: File Owner
 * 4 Bytes: File Name Length
 * N Bytes: File Name
 * 4 Bytes: File Length
 * N Bytes: File
 */


public class FileServer {

	private boolean configLoaded = false;
	private Authenticator auth = null;
	private String fileRoot = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//initialize parser
		OptionParser parser = new OptionParser( "a:p:c:s:k:r:h" );

		//parse cli args
		OptionSet options = parser.parse(args);

		//check for help
		if (options.has("h")) {
			printUsage();
			System.exit(0);
		}

		//check for required arguments
		if (!options.has("a") || !options.has("p") || !options.has("c")) {
			printUsage();
			System.exit(0);
		}
		if (options.has("s") && !options.has("k")) {
			System.out.printf("Keystore password is required when using SSL.\n");
			printUsage();
			System.exit(0);
		}

		try {
			InetAddress address = parseAddress(String.valueOf(options.valueOf("a")));
			int port = parsePort(String.valueOf(options.valueOf("p")));
			String configFilename = String.valueOf(options.valueOf("c"));
			boolean useSSL = options.has("s");
			String sslKeystoreFilename = String.valueOf(options.valueOf("s"));
			String sslKeystorePassword = String.valueOf(options.valueOf("k"));
			String fileRoot = ".";
			if (options.has("r"))
				fileRoot = String.valueOf(options.valueOf("r"));

			//verify files exist
			verifyFileExists(configFilename, "Config file '%s' is not accessible.\n");
			verifyDirectoryExists(fileRoot, "User files root directory '%s' is not accessible.\n");

			//set ssl properties
			if (useSSL) {
				verifyFileExists(sslKeystoreFilename, "SSL Keystore file '%s' is not accessible.\n");
				System.setProperty("javax.net.ssl.keyStore", sslKeystoreFilename);
				System.setProperty("javax.net.ssl.keyStorePassword", sslKeystorePassword);
			}

			//create and run the server
			FileServer server = new FileServer(fileRoot);
			server.runServer(address, port, configFilename, useSSL);
		} catch (Exception e) {
			System.exit(1);
		}
	}

	private static InetAddress parseAddress(String address) throws Exception {
		InetAddress result = null;

		try {
			result = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			System.out.printf("Invalid or unknown bind address '%s'.\n", address);
			throw new Exception();
		}

		return result;
	}

	private static int parsePort(String port) throws Exception {
		int result = -1;

		try {
			result = Integer.parseInt(port);
		} catch (NumberFormatException e) {
			System.out.printf("Specified port is not a valid number.\n");
			throw new Exception();
		}

		if (result <= 0 || result >= 65535) {
			System.out.printf("Specified port '%d' is not within the valid range.\n", result);
			throw new Exception();
		}

		return result;
	}

	private static void verifyFileExists(String filename, String errorMessageFormat) throws Exception {
		File file = new File(filename);

		if (!file.exists() || !file.isFile() || !file.canRead()) {
			System.out.printf(errorMessageFormat, filename);
			throw new Exception();
		}
	}

	private static void verifyDirectoryExists(String dirname, String errorMessageFormat) throws Exception {
		File file = new File(dirname);

		if (!file.exists() || !file.isDirectory() || !file.canRead()) {
			System.out.printf(errorMessageFormat, dirname);
			throw new Exception();
		}
	}

	private static void printUsage() {
		System.out.println("USAGE: -a address -p port -c config [-s keystore -k password] [-r fileroot]");
		System.out.printf("\t%s\t%s\n", "-a", "Bind address.");
		System.out.printf("\t%s\t%s\n", "-p", "Port.");
		System.out.printf("\t%s\t%s\n", "-c", "Config file.");
		System.out.printf("\t%s\t%s\n", "-s", "Use SSL/TLS with keystore.");
		System.out.printf("\t%s\t%s\n", "-k", "Use SSL/TLS with keystore password.");
		System.out.printf("\t%s\t%s\n", "-r", "User files root directory.");
		System.out.printf("\t%s\t%s\n", "-h", "Prints this message.");
	}

	public FileServer(String fileRoot) {
		this.fileRoot = fileRoot;
		if (!this.fileRoot.endsWith(File.separator)) {
			this.fileRoot = this.fileRoot + File.separator;
		}
	}

	public void runServer(InetAddress bindAddress, int port, String configFilename, boolean useSSL) throws Exception {
		ServerSocket socket = this.createSocket(bindAddress, port, useSSL);

		this.auth = new Authenticator();
		byte[] buffer = null;
		byte numConnections = (byte)0;
		boolean open = true;
		Socket connection = null;

        if (numConnections == 0) {
				//lazy initialization on first connection
			if (!this.configLoaded) {
				buffer = readConfiguration(configFilename);
		    	if (!this.configLoaded) {
					System.out.printf("Failed to load the configuration file.\n");
					throw new Exception();
				}
			}
			auth.initialize(buffer);
			//clear the configuration
			Arrays.fill(buffer, (byte)0);

			//create the buffers for connections
			buffer = new byte[4096];
		}

		while (open) {
			//accept a connection
			connection = socket.accept();

			//increment the count of connections
			numConnections -= 1;

			//print some status information
			System.out.printf("Received connection %d.\n", Math.abs(numConnections));

			//handle the request
			try {
				this.handleRequest(connection, buffer);
			} catch (Exception e) {
				System.out.printf("Failed to proccess the requested operation.\n");
				throw e;
			} finally {
				connection.close();
			}
		}

	}

	private byte[] readConfiguration(String filename) {
		ByteArrayOutputStream config = null;

		File file = new File(filename);
		if (!file.exists() || !file.isFile() || !file.canRead()) {
			System.out.printf("Configuration file '%s' is not accessible.\n", filename);
			this.configLoaded = false;
			return null;
		}

		FileInputStream fstream = null;
		try {
			fstream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.out.printf("Configuration file '%s' is not accessible.\n", filename);
			this.configLoaded = false;
			return null;
		}

		config = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int readSize = 0;
		try {
			while ((readSize = fstream.read(buffer)) != -1) {
				config.write(buffer, 0, readSize);
			}
		} catch (IOException e) {
			System.out.printf("Failed to read the configuration file.  Error: %s\n", e.getMessage());
			return null;
		}

		this.configLoaded = true;
		return config.toByteArray();
	}

	private ServerSocket createSocket(InetAddress bindAddress, int port, boolean useSSL) throws Exception {
		InetSocketAddress socketAddress = new InetSocketAddress(bindAddress, port);
		ServerSocket socket = null;

		try {
		if (useSSL) {
			ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
			socket = factory.createServerSocket();
			SSLServerSocket sslSocket = (SSLServerSocket)socket;
			sslSocket.setEnabledCipherSuites(new String[] {"TLS_RSA_WITH_AES_128_CBC_SHA"});
			sslSocket.setEnabledProtocols(new String[] {"SSLv3", "TLSv1"});
			sslSocket.setWantClientAuth(false);
		} else {
			socket = new ServerSocket();
		}

		socket.bind(socketAddress);
		} catch (Exception e) {
			System.out.printf("Failed to create a socket.  Error: %s\n", e.getMessage());
			throw e;
		}

		return socket;
	}

	private void handleRequest(Socket connection, byte[] buffer) throws Exception {	//STONESOUP:SOURCE_TAINT:SOCKET	//STONESOUP:DATA_FLOW:SIMPLE	//STONESOUP:CONTROL_FLOW:SEQUENCE
		String username = null;
		String password = null;
		Operations operation = Operations.Download;
		String targetFilename = null;
		String targetUsername = null;

		byte[] fixedBuffer = new byte[4];
		byte[] tempField = null;

		InputStream inStream = connection.getInputStream();


		//read the username
		readFieldLength(inStream, fixedBuffer);
		tempField = readField(inStream, this.decodeLength(fixedBuffer), buffer);
		username = new String(tempField, Charset.forName("US-ASCII"));
		Arrays.fill(tempField, (byte)0);
		tempField = null;
		Arrays.fill(fixedBuffer, (byte)0);

		//read the password
		readFieldLength(inStream, fixedBuffer);
		tempField = readField(inStream, this.decodeLength(fixedBuffer), buffer);
		password = new String(tempField, Charset.forName("US-ASCII"));
 		Arrays.fill(tempField, (byte)0);
		tempField = null;
		Arrays.fill(fixedBuffer, (byte)0);

		//read the operation
		readFieldLength(inStream, fixedBuffer);
		operation = Operations.decodeId(fixedBuffer);
		Arrays.fill(fixedBuffer, (byte)0);

		//read the target file
		readFieldLength(inStream, fixedBuffer);
		tempField = readField(inStream, this.decodeLength(fixedBuffer), buffer);
		targetFilename = new String(tempField, Charset.forName("US-ASCII"));
    Arrays.fill(tempField, (byte)0);
		tempField = null;
		Arrays.fill(fixedBuffer, (byte)0);

		//read the target file owner
		readFieldLength(inStream, fixedBuffer);	//STONESOUP:INTERACTION_POINT	//STONESOUP:DATA_TYPE:SHORT_CHARACTER
		tempField = readField(inStream, this.decodeLength(fixedBuffer), buffer);
		targetUsername = new String(tempField, Charset.forName("US-ASCII"));
        Arrays.fill(tempField, (byte)0);
		tempField = null;
		Arrays.fill(fixedBuffer, (byte)0);

		//authenticate and perform request
		this.verifyRequest(connection, username, password, operation, targetFilename, targetUsername, buffer);
	}

	private void verifyRequest(Socket connection, String username, String password, Operations operation, String targetFilename, String targetUsername, byte[] buffer) throws Exception {
		Authenticator.AuthResult authResult = this.auth.authenticate(username, password);	//STONESOUP:CROSSOVER_POINT	//STONESOUP:TRIGGER_POINT	// Method that will fail due to wrong encoding
		Results result = Results.LoginFailed;

		if (authResult == Authenticator.AuthResult.Failed) {
			System.out.printf("Login failed for user '%s'.\n", username);
			result = Results.LoginFailed;
			this.sendFailedResponse(connection, result);
			return;
		} else if (authResult == Authenticator.AuthResult.SuccessUser) {
			if (!username.equalsIgnoreCase(targetUsername)) {
				System.out.printf("Access denied for user '%s' to files belonging to user '%s'.\n", username, targetUsername);
				result = Results.AccessDenied;
				this.sendFailedResponse(connection, result);
				return;
			}
		}

		//passed security, perform the lookup
		//first make sure the file exists
		File file = new File(this.fileRoot + targetUsername.toLowerCase() + File.separator + targetFilename);
		if (!file.exists() || !file.isFile() || !file.canRead()) {
			System.out.printf("Requested file '%s' does not exist.\n", file.getAbsolutePath());
			result = Results.NoSuchFile;
			this.sendFailedResponse(connection, result);
			return;
		}

		//file is available for download
		result = Results.Success;
		this.sendFileResponse(connection, result, targetFilename, targetUsername, file, buffer);
	}

	private void sendFailedResponse(Socket connection, Results result) throws Exception {
		byte[] response = result.encode();

		connection.getOutputStream().write(response);
	}

	private void sendFileResponse(Socket connection, Results result, String targetFilename, String targetUsername, File file, byte[] buffer) throws Exception {
		//send the result
		byte[] resultEnc = result.encode();
		connection.getOutputStream().write(resultEnc);

		//send the file owner
		byte[] fileOwner = targetUsername.toLowerCase().getBytes(Charset.forName("US-ASCII"));
		byte[] fileOwnerLength = this.encodeLength(fileOwner.length);
		connection.getOutputStream().write(fileOwnerLength);
		connection.getOutputStream().write(fileOwner);

		//send the file name
		byte[] filename = targetFilename.toLowerCase().getBytes(Charset.forName("US-ASCII"));
		byte[] filenameLength = this.encodeLength(filename.length);
		connection.getOutputStream().write(filenameLength);
		connection.getOutputStream().write(filename);

		//send the file
		byte[] fileLength = this.encodeLength((int)file.length());
		connection.getOutputStream().write(fileLength);
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(file);
			int readSize = 0;
			while ((readSize = fstream.read(buffer)) != -1) {
				connection.getOutputStream().write(buffer, 0, readSize);
			}
		} catch (FileNotFoundException e) {
			System.out.printf("Unable to locate specified file.\n");
			throw e;
		} catch (IOException e) {
			System.out.printf("Error writing file to socket.\n");
			throw e;
		}
	}

	private int decodeLength(byte[] lengthBytes) {
		BigInteger length = new BigInteger(lengthBytes);
		return length.intValue();
	}

	private byte[] encodeLength(int length) {
		byte[] lenthBytes = BigInteger.valueOf(length).toByteArray();

		if (lenthBytes.length < 4) {
			byte[] temp = new byte[4];
			Arrays.fill(temp, (byte)0);
			System.arraycopy(lenthBytes, 0, temp, 4-lenthBytes.length, lenthBytes.length);
			lenthBytes = temp;
		}

		return lenthBytes;
	}

	private byte[] readField(InputStream stream, int length, byte[] buffer) throws Exception {
		ByteArrayOutputStream fieldStream = new ByteArrayOutputStream(length);

		int readSize = 0;
		int totalRead = 0;
		int readWant = length - totalRead;
		if (readWant > buffer.length)
			readWant = buffer.length;

		try {
			while ((readSize = stream.read(buffer, 0, readWant)) != -1) {
				totalRead += readSize;
				readWant = length - totalRead;
				if (readWant > buffer.length)
					readWant = buffer.length;
				fieldStream.write(buffer, 0, readSize);
				if (totalRead == length)
					break;
			}
		} catch (IOException e) {
			System.out.printf("Failed to read from socket.");
			throw e;
		}

		if (length != totalRead) {
			System.out.printf("Invalid number of bytes sent.");
			throw new Exception();
		}

		byte[] field = fieldStream.toByteArray();

		try {
			fieldStream.close();
		} catch (IOException e) {
			//swallow this exception
		}

		return field;
	}

	private void readFieldLength(InputStream stream, byte[] buffer) throws Exception {

		int readSize = 0;
		int totalRead = 0;
		int readWant = buffer.length - totalRead;

		try {
			while ((readSize = stream.read(buffer, totalRead, readWant)) != -1) {
				totalRead += readSize;
				readWant = buffer.length - totalRead;
				if (totalRead == buffer.length)
					break;
			}
		} catch (IOException e) {
			System.out.printf("Failed to read from socket.");
			throw e;
		}

		if (buffer.length != totalRead) {
			System.out.printf("Invalid field length.");
			throw new Exception();
		}

	}
}
