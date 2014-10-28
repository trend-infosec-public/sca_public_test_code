/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.http;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class HttpResponse {
	//Constants
	
	//Locals
	private HttpResponseHeaders headers = null;
	private byte[] content = null;
	
	public HttpResponse() {
		this.headers = new HttpResponseHeaders();
	}
	
	public HttpResponseHeaders getHeaders() {
		return this.headers;
	}
	
	public byte[] getContent() {
		return this.content;
	}
	
	public void setContent(byte[] content, String contentType) {
		this.content = content;
		this.headers.addHeader(HttpResponseHeaders.CONTENT_TYPE, contentType);
		this.headers.addHeader(HttpResponseHeaders.CONTENT_LENGTH, Integer.toString(content.length));
	}
	
	public void setContent(String content, String contentType) {
		this.content = content.getBytes(Charset.forName("UTF-8"));
		this.headers.addHeader(HttpResponseHeaders.CONTENT_TYPE, contentType + "; charset=utf-8");
		this.headers.addHeader(HttpResponseHeaders.CONTENT_LENGTH, Integer.toString(this.content.length));
	}
	
	public void writeResponse(BufferedOutputStream output) throws IOException {
		output.write(this.headers.encode());
		if (this.content != null)
			output.write(this.content);
		output.flush();
	}
}
