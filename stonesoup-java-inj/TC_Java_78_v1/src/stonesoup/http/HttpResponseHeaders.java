/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.http;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class HttpResponseHeaders {
	//Constants
	public static final String ACCEPT_RANGES = "Accept-Ranges";
	public static final String AGE = "Age";
	public static final String ALLOW = "Allow";
	public static final String CACHE_CONTROL = "Cache-Control";
	public static final String CONNECTION = "Connection";
	public static final String CONTENT_ENCODING = "Content-Encoding";
	public static final String CONTENT_LANGUAGE = "Content-Language";
	public static final String CONTENT_LENGTH = "Content-Length";
	public static final String CONTENT_LOCATION = "Content-Location";
	public static final String CONTENT_MD5 = "Content-MD5";
	public static final String CONTENT_DISPOSITION = "Content-Disposition";
	public static final String CONTENT_RANGE = "Content-Range";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String DATE = "Date";
	public static final String ETAG = "ETag";
	public static final String EXPIRES = "Expires";
	public static final String LAST_MODIFIED = "Last-Modified";
	public static final String LINK = "Link";
	public static final String LOCATION = "Location";
	public static final String P3P = "P3P";
	public static final String PRAGMA = "Pragma";
	public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";
	public static final String REFRESH = "Refresh";
	public static final String RETRY_AFTER = "Retry-After";
	public static final String SERVER = "Server";
	public static final String SET_COOKIE = "Set-Cookie";
	public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
	public static final String TRAILER = "Trailer";
	public static final String TRANSFER_ENCODING = "Transfer-Encoding";
	public static final String VARY = "Vary";
	public static final String VIA = "Via";
	public static final String WARNING = "Warning";
	public static final String WWW_AUTHENTICATE = "WWW-Autheticate";
	
	//Locals
	private HttpResponseCode code = HttpResponseCode.UNSUPPORTED;
	private Map<String, String> extended = null;
	
	protected HttpResponseHeaders() {
		this(HttpResponseCode.UNSUPPORTED, null);
	}
	
	protected HttpResponseHeaders(HttpResponseCode code, Map<String, String> headers) {
		this.extended = new HashMap<String, String>();
		this.code = code;
		if (headers != null) {
			for (Entry<String, String> item : headers.entrySet()) {
				this.extended.put(item.getKey(), item.getValue());
			}
		}
	}
	
	public HttpResponseCode getCode() {
		return this.code;
	}
	
	public String getHeaderByName(String name) {
		if (this.extended.containsKey(name)) {
			return this.extended.get(name);
		}
		
		return null;
	}
	
	public void setCode(HttpResponseCode code) {
		this.code = code;
	}
	
	public void addHeader(String key, String value) {
		this.extended.put(key, value);
	}
	
	public String construct() {
		StringBuilder result = new StringBuilder();
		result.append(this.code.construct());
		result.append("\r\n");
		
		for (Entry<String, String> item : this.extended.entrySet()) {
			result.append(item.getKey());
			result.append(": ");
			result.append(item.getValue());
			result.append("\r\n");
		}
		
		result.append("\r\n");
		
		return result.toString();
	}
	
	public byte[] encode() {
		String headers = this.construct();
		byte[] rawHeaders = headers.getBytes(Charset.forName("US-ASCII"));
		return rawHeaders;
	}
}
