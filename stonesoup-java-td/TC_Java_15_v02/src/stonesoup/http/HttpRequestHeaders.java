/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.http;

import java.util.HashMap;
import java.util.Map;

public class HttpRequestHeaders {
	//Constants
	public static final String ACCEPT = "Accept";
	public static final String ACCEPT_CHARSET = "Accept-Charset";
	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	public static final String ACCEPT_LANGUAGE = "Accept-Language";
	public static final String AUTHORIZATION = "Authorization";
	public static final String CACHE_CONTROL = "Cache-Control";
	public static final String CONNECTION = "Connection";
	public static final String COOKIE = "Cookie";
	public static final String CONTENT_LENGTH = "Content-Length";
	public static final String CONTENT_MD5 = "Content-MD5";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String DATE = "Date";
	public static final String EXPECT = "Expect";
	public static final String FROM = "From";
	public static final String HOST = "Host";
	public static final String IF_MATCH = "If-Match";
	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	public static final String IF_NONE_MATCH = "If-None-Match";
	public static final String IF_RANGE = "If-Range";
	public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	public static final String MAX_FORWARDS = "Max-Forwards";
	public static final String PRAGMA = "Pragma";
	public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
	public static final String RANGE = "Range";
	public static final String REFERER = "Referer";
	public static final String TE = "TE";
	public static final String UPGRADE = "Upgrade";
	public static final String USER_AGENT = "User-Agent";
	public static final String VIA = "Via";
	public static final String WARNING = "Warning";
	
	//Locals
	private HttpRequestMethod method = HttpRequestMethod.UNSUPPORTED;
	private String target = null;
	private String version = "HTTP/1.1";
	private Map<String, String> extended = null;
	
	protected HttpRequestHeaders() {
		this(HttpRequestMethod.UNSUPPORTED, null, "HTTP/1.1");
	}
	
	protected HttpRequestHeaders(HttpRequestMethod method, String target, String version) {
		this.extended = new HashMap<String, String>();
		this.method = method;
		this.target = target;
		this.version = version;
	}
	
	public HttpRequestMethod getMethod() {
		return this.method;
	}
	
	public String getTarget() {
		return this.target;
	}
	
	public String getVersion() {
		return this.version;
	}
	
	public String getHeaderByName(String name) {
		if (this.extended.containsKey(name)) {
			return this.extended.get(name);
		}
		
		return null;
	}
	
	protected void setMethod(HttpRequestMethod method) {
		this.method = method;
	}
	
	protected void setTarget(String target) {
		this.target = target;
	}
	
	protected void setVersion(String version) {
		this.version = version;
	}
	
	protected void addHeader(String key, String value) {
		this.extended.put(key, value);
	}
}
