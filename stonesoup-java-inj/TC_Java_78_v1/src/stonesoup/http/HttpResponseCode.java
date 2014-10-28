/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.http;

import java.nio.charset.Charset;
import java.util.Arrays;

public enum HttpResponseCode {
	UNSUPPORTED(0, "Unsupported Response"),
	CONTINUE_100(100, "Continue"),
	OK_200(200, "OK"),
	MOVED_PERMANENTLY_301(301, "Moved Permanently"),
	FOUND_302(302, "Found"),
	NOT_MODIFED_304(304, "Not Modified"),
	TEMPORARY_REDIRECT_307(307, "Temporary Redirect"),
	BAD_REQUEST_400(400, "Bad Request"),
	UNAUTHORIZED_401(401, "Unauthorized"),
	FORBIDDEN_403(403, "Forbidden"),
	NOT_FOUND_404(404, "Not Found"),
	METHOD_NOT_ALLOWED_405(405, "Method Not Allowed"),
	INTERNAL_SERVER_ERROR_500(500, "Internal Server Error");
	
	private static final String HTTP_VERSION = "HTTP/1.1";
	
	private int code = 0;
	private String name = null;
	private String constructed = null;
	private byte[] encoded = null;
	
	private HttpResponseCode(int code, String name) {
		this.code = code;
		this.name = name;
		this.constructed = HTTP_VERSION + " " + Integer.toString(this.code) + " " + this.name;
		this.encoded = this.constructed.getBytes(Charset.forName("US-ASCII"));
	}
	
	public String construct() {
		return this.constructed;
	}
	
	public byte[] encode() {
		return Arrays.copyOf(this.encoded, this.encoded.length);
	}
	
	public static HttpResponseCode decode(String rawHeader) throws Exception {
		String[] pieces = rawHeader.trim().split(" ", 3);
		if (pieces.length != 3) {
			throw new Exception("Malformed response code.");
		}
		int receivedCode = Integer.parseInt(pieces[1]);
		for (HttpResponseCode curCode : HttpResponseCode.values()) {
			if (curCode.code == receivedCode) {
				return curCode;
			}
		}
		
		return HttpResponseCode.UNSUPPORTED;
	}
}
