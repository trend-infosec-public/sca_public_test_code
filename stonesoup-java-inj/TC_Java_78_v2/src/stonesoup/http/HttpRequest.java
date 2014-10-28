/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.http;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import stonesoup.Configuration;

public class HttpRequest {
	//Constants
	private static final int READ_BUFFER_SIZE = 4096;
	private static final char CR = '\r';
	private static final char LF = '\n';
	private static final String CRLF = new String(new char[] { CR, LF });
	private static final Pattern PATTERN_LINE_ENDING = Pattern.compile("\r\n?|\n");
	@SuppressWarnings("unused")
	private static final Pattern PATTERN_END_OF_HEADER = Pattern.compile("(\r\n?|\n)(\r\n?|\n)");


	//Locals
	private HttpRequestHeaders headers = null;
	private Map<String, List<String>> queryString = null;
	private Map<String, List<String>> form = null;
	private byte[] content = null;
	private boolean decodeQueryString = false;
	private boolean decodeForm = false;
	private boolean decodeTarget = false;

	public HttpRequest() {
		this.headers = new HttpRequestHeaders();
		this.queryString = new HashMap<String, List<String>>();
		this.form = new HashMap<String, List<String>>();
		this.decodeQueryString = Boolean.parseBoolean(Configuration.Instance.getProperty("HttpRequest.decodeQueryString", "False"));
		this.decodeForm = Boolean.parseBoolean(Configuration.Instance.getProperty("HttpRequest.decodeForm", "False"));
		this.decodeTarget = Boolean.parseBoolean(Configuration.Instance.getProperty("HttpRequest.decodeTarget", "False"));
	}

	public HttpRequestHeaders getHeaders() {
		return this.headers;
	}

	public Map<String, List<String>> getQueryString() {
		return Collections.unmodifiableMap(this.queryString);
	}

	public List<String> getQueryStringByName(String name) {
		if (this.queryString.containsKey(name)) {
			return Collections.unmodifiableList(this.queryString.get(name));
		}

		return null;
	}

	public Map<String, List<String>> getForm() {
		return Collections.unmodifiableMap(this.form);
	}

	public List<String> getFormByName(String name) {
		if (this.form.containsKey(name)) {
			return Collections.unmodifiableList(this.form.get(name));
		}

		return null;
	}

	public byte[] getContent() {
		return this.content;
	}

	public void parseRequest(BufferedInputStream input) throws IOException, Exception {

		//declare a reusable buffer for reading
		byte[] buffer = new byte[READ_BUFFER_SIZE];

		//placeholders for the headers and content
		ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
		ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();

		try {
			//read the headers fully to start parsing
			this.readHeadersFully(input, headerBuffer, contentBuffer, buffer);

			//parse the headers
			String rawHeaders = new String(headerBuffer.toByteArray(), Charset.forName("US-ASCII"));
			this.parseHeaders(rawHeaders);

			//check if we need to look for content
			if (this.headers.getMethod() == HttpRequestMethod.POST) {
				//there should be a content length
				String rawContentLength = this.headers.getHeaderByName(HttpRequestHeaders.CONTENT_LENGTH);
				if (rawContentLength == null) {
					throw new Exception("Malformed POST, missing Content-Length.");
				}
				int contentLength = Integer.parseInt(rawContentLength);
				this.readContentFully(input, contentBuffer, buffer, contentLength);

				//check if we need to decode form fields
				String contentType = this.headers.getHeaderByName(HttpRequestHeaders.CONTENT_TYPE);
				if (contentType == null) {
					throw new Exception("Malformed POST, missing Content-Type.");
				}
				if (contentType.equalsIgnoreCase(HttpContentTypes.APPLICATION_HTML_URL_FORM)) {
					//this is a simple form of key value pairs (URL like encoding)
					String rawForm = new String(contentBuffer.toByteArray(), Charset.forName("US-ASCII"));
					this.parseFormSimple(rawForm);
				} else {
					//just capture binary at this point
					this.content = contentBuffer.toByteArray();
				}
			}
		} finally {
			Arrays.fill(buffer, (byte)0x00);
			try {
				headerBuffer.close();
			} catch (Exception e) {
				//swallow this here, we just want to close it out
			}
			try {
				contentBuffer.close();
			} catch (Exception e) {
				//swallow this here, we just want to close it out
			}
		}
	}

	private void readHeadersFully(BufferedInputStream input, ByteArrayOutputStream headers, ByteArrayOutputStream content, byte[] buffer) throws IOException {
		byte[] EOHsequence = (CRLF + CRLF).getBytes(Charset.forName("US-ASCII"));

		int readsize = 0;
		//read until we hit the end
		while ((readsize = input.read(buffer, 0, READ_BUFFER_SIZE)) != -1) {
			//search for the end of header marker
			int EOHstart = this.findByteSequence(buffer, 0, readsize, EOHsequence);
			if (EOHstart != -1) {
				//found a match, we have end of headers and maybe some content
				//save the headers
				headers.write(buffer, 0, EOHstart);

				if (readsize > EOHstart + EOHsequence.length) {
					//there is some data to preserve
					content.write(buffer, EOHstart + EOHsequence.length, readsize - (EOHstart + EOHsequence.length));
				}

				break;
			} else {
				//need more data to complete headers
				headers.write(buffer, 0, readsize);
			}
		}
	}

	private void readContentFully(BufferedInputStream input, ByteArrayOutputStream content, byte[] buffer, int contentLength) throws IOException {
		int contentTotal = content.size();

		if (contentTotal == contentLength) {
			return;
		}

		int readsize = 0;
		//read until we hit the end
		while ((readsize = input.read(buffer, 0, READ_BUFFER_SIZE)) != -1) {
			int copyOffset = contentLength - (contentTotal + readsize);

			if (copyOffset < 0) {
				content.write(buffer, 0, readsize - copyOffset);
				contentTotal += (readsize - copyOffset);
				break;
			}

			content.write(buffer, 0, readsize);
			contentTotal += readsize;
		}
	}

	private int findByteSequence(byte[] content, int start, int length, byte[] pattern) {
		int jj = 0;
		int match_start = -1;

		for (int ii = start; ii < length; ii++) {
			while (jj > 0 && content[ii] != pattern[jj]) {
				jj--;
			}
			if (content[ii] == pattern[jj]) {
				jj++;
			}
			if (jj == pattern.length) {
				match_start = ii - pattern.length + 1;
				break;
			}
		}

		return match_start;
	}

	private void parseHeaders(String rawHeaders) throws Exception {
		String[] headers = PATTERN_LINE_ENDING.split(rawHeaders);

		//get the first line
		String[] mainHeader = headers[0].trim().split(" ", 3);
		if (mainHeader.length != 3) {
			throw new Exception("Malformed header received.");
		}
		HttpRequestMethod method = HttpRequestMethod.decode(mainHeader[0]);
		String fullTarget = mainHeader[1].trim();
		String version = mainHeader[2].trim();

		//get the target information
		String[] targetComponents = fullTarget.split("\\?", 2);
		String target = null;
		String rawQueryString = null;
		switch (targetComponents.length) {
			case 2:
				rawQueryString = targetComponents[1];
			case 1:
				target = targetComponents[0];
				break;
			default:
				throw new Exception("Malformed header received.");
		}

		if (this.decodeTarget) {
			target = URLDecoder.decode(target, "UTF-8");
		}

		//set the basic information
		this.headers.setMethod(method);
		this.headers.setTarget(target);
		this.headers.setVersion(version);

		//get the remaining headers, skipping the first line
		for (int ii = 1; ii < headers.length; ii++) {
			String curHeader = headers[ii];
			String[] keyAndValue = curHeader.trim().split(": ", 2);
			if (keyAndValue.length != 2) {
				throw new Exception("Malformed header received.");
			}
			this.headers.addHeader(keyAndValue[0], keyAndValue[1]);
		}

		//now parse the query string
		this.parseQueryString(rawQueryString);
	}

	private void parseQueryString(String rawQueryString) throws Exception {
		if (rawQueryString == null) {
			return;
		}

		String[] items = rawQueryString.split("&");

		for (String item : items) {
			String[] keyAndValue = item.split("=", 2);
			if (keyAndValue.length != 2) {
				throw new Exception("Malformed query string received.");
			}
			String key = keyAndValue[0].toLowerCase();
			if (!this.queryString.containsKey(key)) {
				this.queryString.put(key, new ArrayList<String>());
			}
			String value = keyAndValue[1];
			if (this.decodeQueryString) {
				value = URLDecoder.decode(value, "UTF-8");
			}
			this.queryString.get(key).add(value);
		}
	}

	private void parseFormSimple(String rawForm) throws Exception {
		if (rawForm == null) {
			return;
		}

		String[] items = rawForm.split("&");

		for (String item : items) {
			String[] keyAndValue = item.split("=", 2);
			if (keyAndValue.length != 2) {
				throw new Exception("Malformed form data received.");
			}
			String key = keyAndValue[0].toLowerCase();
			if (!this.form.containsKey(key)) {
				this.form.put(key, new ArrayList<String>());
			}
			String value = keyAndValue[1];
			if (this.decodeForm) {
				value = URLDecoder.decode(value, "UTF-8");
			}
			this.form.get(key).add(value);
		}
	}
}
