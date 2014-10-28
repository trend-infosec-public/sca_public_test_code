/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import stonesoup.http.HttpRequestHeaders;
import stonesoup.http.HttpRequestMethod;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class Client {

	private static final String HTTP_VERSION = "HTTP/1.1";


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//initialize parser
		OptionParser parser = new OptionParser( "a:p:m:t:f:c:sh" );

		//parse cli args
		OptionSet options = parser.parse(args);

		//check for help
		if (options.has("h")) {
			printUsage();
			System.exit(0);
		}

		//check for required arguments
		if (!options.has("a") || !options.has("p") || !options.has("m") || !options.has("t")) {
			printUsage();
			System.exit(0);
		}

		try {
			InetAddress address = parseAddress(String.valueOf(options.valueOf("a")));
			int port = parsePort(String.valueOf(options.valueOf("p")));
			String rawMethod = String.valueOf(options.valueOf("m"));
			HttpRequestMethod method = HttpRequestMethod.decode(rawMethod.toUpperCase());
			String target = String.valueOf(options.valueOf("t"));
			String postContentType = null;
			String postContent = null;
			if (method == HttpRequestMethod.POST) {
				if (options.has("c")) {
					postContent = String.valueOf(options.valueOf("c"));
				}
				if (options.has("f")) {
					postContentType = String.valueOf(options.valueOf("f"));
				}
				if (postContent == null || postContentType == null) {
					System.out.printf("HTTP POST requieres content-type and content.\n");
					System.exit(1);
				}
			}
			boolean useSSL = options.has("s");

			//create and run the server
			Client client = new Client();
			client.runOperation(address, port, method, target, postContentType, postContent, useSSL);
		} catch (Exception e) {
			System.exit(1);
		}
	}

	private static InetAddress parseAddress(String address) throws Exception {
		InetAddress result = null;

		try {
			result = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			System.out.printf("Invalid or unknown host address '%s'.\n", address);
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
		System.out.println("USAGE: -a address -p port -u username -k password -f filename [-o owner] [-s]");
		System.out.printf("\t%s\t%s\n", "-a", "Host address.");
		System.out.printf("\t%s\t%s\n", "-p", "Port.");
		System.out.printf("\t%s\t%s\n", "-m", "Method (GET | POST).");
		System.out.printf("\t%s\t%s\n", "-t", "Target.");
		System.out.printf("\t%s\t%s\n", "-f", "POST content type.");
		System.out.printf("\t%s\t%s\n", "-c", "POST content.");
		System.out.printf("\t%s\t%s\n", "-s", "Use SSL.");
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

	private void runOperation(InetAddress address, int port, HttpRequestMethod method, String target, String contentType, String content, boolean useSSL) throws Exception {
		Socket connection = this.createSocketAndConnect(address, port, useSSL);

		String host = address.getHostAddress() + ":" + String.valueOf(port);
		this.sendRequest(connection, host, method, target, contentType, content);

		this.receiveResponse(connection);

		connection.close();
	}

	private void sendRequest(Socket connection, String host, HttpRequestMethod method, String target, String contentType, String content) throws Exception {

		BufferedOutputStream output = new BufferedOutputStream(connection.getOutputStream());

		System.out.printf("Target %s\n", target);
		output.write((method.encode() + " " + target + " " + HTTP_VERSION + "\r\n").getBytes(Charset.forName("UTF-8")));
		output.write((HttpRequestHeaders.HOST + ": " + host + "\r\n").getBytes(Charset.forName("UTF-8")));

		if (method == HttpRequestMethod.POST) {
			byte[] rawContent = content.getBytes(Charset.forName("UTF-8"));
			output.write((HttpRequestHeaders.CONTENT_TYPE + ": " + contentType + "\r\n").getBytes(Charset.forName("UTF-8")));
			output.write((HttpRequestHeaders.CONTENT_LENGTH + ": " + String.valueOf(rawContent.length) + "\r\n\r\n").getBytes(Charset.forName("UTF-8")));
			output.write(rawContent);
		} else {
			output.write("\r\n".getBytes(Charset.forName("UTF-8")));
		}

		output.flush();

		return;
	}

	private void receiveResponse(Socket connection) throws Exception {
		InputStreamReader input = new InputStreamReader(new BufferedInputStream(connection.getInputStream()), Charset.forName("UTF-8"));
		char[] buffer = new char[4096];

		int readSize = 0;
		int totalRead = 0;
		while ((readSize = input.read(buffer)) != -1) {
			totalRead += readSize;
//			System.out.print(new String(buffer, 0, readSize));
		}
		if (totalRead > 100) {
			System.out.printf("Total bytes read > 100\n", totalRead);
		} else {
			System.out.printf("Total bytes read: %d\n", totalRead);
		}

		return;
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
					sslSocket.setEnabledCipherSuites(new String[] {"TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA"});
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
