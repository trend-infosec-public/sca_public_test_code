/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.http;

public enum HttpRequestMethod {
	UNSUPPORTED,
	GET,
	POST,
	HEAD,
	TRACE;
	
	private static final String GET_VALUE = "GET";
	private static final String POST_VALUE = "POST";
	private static final String HEAD_VALUE = "HEAD";
	private static final String TRACE_VALUE = "TRACE";
	
	public static HttpRequestMethod decode(String method) {
		if (method.equalsIgnoreCase(GET_VALUE)) {
			return GET;
		} else if (method.equalsIgnoreCase(POST_VALUE)) {
			return POST;
		} else if (method.equalsIgnoreCase(HEAD_VALUE)) {
			return HEAD;
		} else if (method.equalsIgnoreCase(TRACE_VALUE)) {
			return TRACE;
		}
		
		return UNSUPPORTED;
	}
	
	public String encode() {
		return this.toString().toUpperCase();
	}
}
