/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class FileClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//initialize parser
		OptionParser parser = new OptionParser( "a:p:u:k:f:o:l:shdn" );

		//parse cli args
		OptionSet options = parser.parse(args);

		//check for help
		if (options.has("h")) {
			printUsage();
			System.exit(0);
		}

		//check for required arguments
        //STONESOUP: Optional functionality that enables the ls command to be run
        if(!options.has("l")){
		    if (!options.has("a") || !options.has("p") || !options.has("f") || !options.has("u") || !options.has("k")) {
			    printUsage();
			    System.exit(0);
		    }
        }
        else if(!options.has("a") || !options.has("p") || !options.has("u") || !options.has("k")) {
			    printUsage();
			    System.exit(0);
		}

        //check for either uploading or downloading
        if(!options.has("d") && !options.has("n")  && !options.has("l")){
            printUsage();
            System.exit(0);
        }

		try {
			InetAddress address = parseAddress(String.valueOf(options.valueOf("a")));
			int port = parsePort(String.valueOf(options.valueOf("p")));
			String username = String.valueOf(options.valueOf("u"));
			String password = String.valueOf(options.valueOf("k"));
            String filename=null;
            if(options.has("f")){
    			filename = String.valueOf(options.valueOf("f"));
            }
            //STONESOUP: Read in the ls command options
            else if(options.has("l")){
                filename = String.valueOf(options.valueOf("l"));
            }
			String fileOwner = username;
			if (options.has("o")) {
				fileOwner = String.valueOf(options.valueOf("o"));
			}
			boolean useSSL = options.has("s");


			//create and run the server
			FileClient client = new FileClient();
            if(options.has("d")){
    			client.downloadFile(address, port, username, password, filename, fileOwner, useSSL);
            }
            else if(options.has("n")){
                client.uploadFile(address, port, username, password, filename, fileOwner, useSSL);
            }
            //STONESOUP: Run the ls command operation
            else if(options.has("l")){
                client.listFiles(address, port, username, password, filename, fileOwner, useSSL);
            }

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

	private static void printUsage() {
		System.out.println("USAGE: -a address -p port -u username -k password -f filename <-d or -n>  [-o owner] [-s]");
		System.out.printf("\t%s\t%s\n", "-a", "Bind address.");
		System.out.printf("\t%s\t%s\n", "-p", "Port.");
		System.out.printf("\t%s\t%s\n", "-u", "Username.");
		System.out.printf("\t%s\t%s\n", "-k", "Password.");
		System.out.printf("\t%s\t%s\n", "-f", "File to download.");
        System.out.printf("\t%s\t%s\n", "-o", "File owner.");
    	System.out.printf("\t%s\t%s\n", "-s", "Use SSL.");
        System.out.printf("%s\n", "OPTIONS:");
        //STONESOUP: Optional functionality that leads to CWE-88
        //System.out.printf("\t%s\t%s\n", "-l", "list files for the owner or user");
        System.out.printf("\t%s\t%s\n", "-d", "Download the specified file");
        System.out.printf("\t%s\t%s\n", "-n", "Upload the specified file (file must be in current working directory)");
		System.out.printf("\t%s\t%s\n", "-h", "Prints this message.");
	}

	private SSLContext disableServerCertificateChecks() {
		// Create a trust manager that does not validate certificate chains
	    TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
	        public X509Certificate[] getAcceptedIssuers() {
	            return null;
	        }

	        public void checkClientTrusted(X509Certificate[] certs, String authType) {
	            return;
	        }

	        public void checkServerTrusted(X509Certificate[] certs, String authType) {
	            return;
	        }
	    }};

	    // Install the all-trusting trust manager
	    SSLContext sc = null;
	    try {
	        sc = SSLContext.getInstance("SSL");
	        sc.init(null, trustAllCerts, new SecureRandom());
	    } catch (Exception e) {
	        System.out.printf("Failed to disable server certificate verification.\n");
	        System.exit(1);
	    }

	    return sc;
	}

	private void downloadFile(InetAddress address, int port, String username, String password, String filename, String fileOwner, boolean useSSL) throws Exception {
		Socket connection = this.createSocketAndConnect(address, port, useSSL);

		this.sendRequest(connection, username, password, filename, fileOwner, Operations.Download);

		this.receiveResponse(connection);

		connection.close();
	}

    private void uploadFile(InetAddress address, int port, String username, String password, String filename, String fileOwner, boolean useSSL) throws Exception {
        Socket connection = this.createSocketAndConnect(address, port, useSSL);

        this.sendRequest(connection, username, password, filename, fileOwner, Operations.Upload);

        this.sendFile(connection, filename);

        connection.close();
    }

    private void listFiles(InetAddress address, int port, String username, String password, String filename, String fileOwner, boolean useSSL) throws Exception {
        Socket connection = this.createSocketAndConnect(address, port, useSSL);

        this.sendRequest(connection, username, password, filename, fileOwner, Operations.List);

        this.receiveResponse(connection);

        connection.close();

    }

	private void sendRequest(Socket connection, String username, String password, String filename, String fileOwner, Operations opt) throws Exception {

		//send the username
		byte[] usernameBytes = username.toLowerCase().getBytes(Charset.forName("US-ASCII"));
		byte[] usernameLength = this.encodeLength(usernameBytes.length);
		connection.getOutputStream().write(usernameLength);
		connection.getOutputStream().write(usernameBytes);

		//send the password
		byte[] passwordBytes = password.toLowerCase().getBytes(Charset.forName("US-ASCII"));
		byte[] passwordLength = this.encodeLength(passwordBytes.length);
		connection.getOutputStream().write(passwordLength);
		connection.getOutputStream().write(passwordBytes);

		//send the operation
        //STONESOUP: When the user learns of the -l <ls command options>, they are then
        //able to send off a request to the server that will run ls with any of the command
        //options.  The server does not restrict the options, enabling multiple commands to be
        //run allowing argument injection CWE-88
        connection.getOutputStream().write(opt.encode());

		//send the filename
		byte[] filenameBytes = filename.toLowerCase().getBytes(Charset.forName("US-ASCII"));
		byte[] filenameLength = this.encodeLength(filenameBytes.length);
		connection.getOutputStream().write(filenameLength);
		connection.getOutputStream().write(filenameBytes);

		//send the owner
		byte[] fileOwnerBytes = fileOwner.toLowerCase().getBytes(Charset.forName("US-ASCII"));
		byte[] fileOwnerLength = this.encodeLength(fileOwnerBytes.length);
		connection.getOutputStream().write(fileOwnerLength);
		connection.getOutputStream().write(fileOwnerBytes);

	}

    private void sendFile(Socket connection, String filename) throws Exception {
        File file = new File(filename);
        byte[] buffer = new byte[4096];
        if(!file.exists() || !file.isFile() || !file.canRead()){
            System.out.printf("Requested file '%s' does not exist.\n", file.getAbsolutePath());
            System.exit(-1);
        }
        byte[] fileLength = this.encodeLength((int)file.length());
        connection.getOutputStream().write(fileLength);
        FileInputStream fstream;
        try{
            fstream = new FileInputStream(file);
            int readSize = 0;
            while((readSize = fstream.read(buffer)) !=-1){
                connection.getOutputStream().write(buffer, 0, readSize);
            }
        }catch(FileNotFoundException e){
            System.out.printf("unable to locate specified file.\n");
            throw e;
        }catch(IOException e){
            System.out.printf("error writing file to socket.\n");
            throw e;
        }

    }

	private void receiveResponse(Socket connection) throws Exception {
		Results result = Results.LoginFailed;
		String owner = null;
		String filename = null;
		int fileSize = 0;

		byte[] buffer = new byte[4096];
		byte[] fixedBuffer = new byte[4];
		byte[] tempField = null;

		InputStream inStream = connection.getInputStream();

		//read the result
		readFieldLength(inStream, fixedBuffer);
		result = Results.decodeId(fixedBuffer);
		Arrays.fill(fixedBuffer, (byte)0);

		//check if we should read more
		if (result != Results.Success) {
			System.out.printf("Requested operation failed.  Reason: %s.\n", result.toString());
			return;
		}

		//we have a file coming back, let's get it
		//read the owner
		readFieldLength(inStream, fixedBuffer);
		tempField = readField(inStream, this.decodeLength(fixedBuffer), buffer);
		owner = new String(tempField, Charset.forName("US-ASCII"));
		Arrays.fill(tempField, (byte)0);
		tempField = null;
		Arrays.fill(fixedBuffer, (byte)0);

		//read the filename
		readFieldLength(inStream, fixedBuffer);
		tempField = readField(inStream, this.decodeLength(fixedBuffer), buffer);
		filename = new String(tempField, Charset.forName("US-ASCII"));
		Arrays.fill(tempField, (byte)0);
		tempField = null;
		Arrays.fill(fixedBuffer, (byte)0);

		//read the file
		readFieldLength(inStream, fixedBuffer);
		fileSize = this.decodeLength(fixedBuffer);
		System.out.printf("File: %s\n", filename);
		System.out.printf("Owner: %s\n", owner);
//		System.out.printf("Size: %d\n", fileSize);	// Caused inconsistencies
		System.out.printf("Size: xxx\n");
		System.out.printf("---------- FILE BEGIN ----------\n");

		int readSize = 0;
		int totalRead = 0;
		while ((readSize = inStream.read(buffer)) != -1) {
			totalRead += readSize;
//			System.out.printf("%s", new String(buffer, 0, readSize, Charset.forName("US-ASCII")));
		}
		if (totalRead > 100) {
			System.out.printf("Total bytes read > 100\n", totalRead);
		} else {
			System.out.printf("Total bytes read: %d\n", totalRead);
		}

		System.out.printf("----------  FILE END  ----------\n");

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

	private Socket createSocketAndConnect(InetAddress address, int port, boolean useSSL) throws Exception {
		InetSocketAddress socketAddress = new InetSocketAddress(address, port);
		Socket result = null;
		boolean success = false;

		do {
			try {
				if (useSSL) {
					SSLContext sc = this.disableServerCertificateChecks();
					result = sc.getSocketFactory().createSocket();
					SSLSocket sslSocket = (SSLSocket)result;
					sslSocket.setEnabledCipherSuites(new String[] {"TLS_RSA_WITH_AES_128_CBC_SHA"});
					sslSocket.setEnabledProtocols(new String[] {"SSLv3", "TLSv1"});
				} else {
					result = new Socket();
				}

				result.connect(socketAddress);
				success = true;
			}
			catch (Exception e) {
				result.close();
				System.err.println("Could not open socket, waiting ...");
				try{
				  Thread.currentThread();
					Thread.sleep(500);
				}
				catch(InterruptedException ie){
				}
			}
		}
		while (!success);

		return result;
	}
}
