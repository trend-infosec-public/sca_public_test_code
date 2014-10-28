/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Entry point for the HTTP Server.  Handles parsing command line arguments.
 * 
 * 
 *
 */
public class Driver {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//initialize parser
		OptionParser parser = new OptionParser( "a:p:c:s:k:h" );
		
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
			
			//verify files exist
			verifyFileExists(configFilename, "Config file '%s' is not accessible.\n");
			//verifyDirectoryExists(fileRoot, "User files root directory '%s' is not accessible.\n");
			
			//set ssl properties
			if (useSSL) {
				verifyFileExists(sslKeystoreFilename, "SSL Keystore file '%s' is not accessible.\n");
				System.setProperty("javax.net.ssl.keyStore", sslKeystoreFilename);
				System.setProperty("javax.net.ssl.keyStorePassword", sslKeystorePassword);
			}
			
			//load the config
			Configuration.loadConfiguration(configFilename);
			
			//create and run the server
			Server server = new Server(address, port, useSSL);
			server.run();
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
	
	@SuppressWarnings("unused")
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
		System.out.printf("\t%s\t%s\n", "-h", "Prints this message.");
	}
}
