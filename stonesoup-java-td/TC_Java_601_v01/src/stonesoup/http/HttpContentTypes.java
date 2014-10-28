/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpContentTypes {

	public static final String TEXT_HTML = "text/html";
	public static final String TEXT_PLAIN = "text/plain";
	public static final String APPLICATION_PDF = "application/pdf";
	public static final String APPLICATION_BINARY = "application/octet-stream";
	public static final String APPLICATION_ZIP = "application/zip";
	public static final String APPLICATION_GZIP = "application/x-gzip";
	public static final String APPLICATION_JAVASCRIPT = "application/javascript";
	public static final String IMAGE_GIF = "image/gif";
	public static final String IMAGE_JPEG = "image/jpeg";
	public static final String IMAGE_PJPEG = "image/pjpeg";
	public static final String IMAGE_PNG = "image/png";
	public static final String IMAGE_SVG = "image/svg+xml";
	public static final String IMAGE_TIFF = "image/tiff";
	public static final String TEXT_CSS = "text/css";
	public static final String TEXT_CSV = "text/csv";
	public static final String TEXT_XML = "text/xml";
	public static final String APPLICATION_HTML_URL_FORM = "application/x-www-form-urlencoded";
	
	private static final Map<String, String> _FILE_EXTENSIONS = new HashMap<String, String>();
	public static final Map<String, String> FILE_EXTENSIONS;
	
	static {
		_FILE_EXTENSIONS.put("txt", TEXT_PLAIN);
		_FILE_EXTENSIONS.put("html", TEXT_HTML);
		_FILE_EXTENSIONS.put("htm", TEXT_HTML);
		_FILE_EXTENSIONS.put("shtml", TEXT_HTML);
		_FILE_EXTENSIONS.put("css", TEXT_CSS);
		_FILE_EXTENSIONS.put("csv", TEXT_CSV);
		_FILE_EXTENSIONS.put("xml", TEXT_XML);
		_FILE_EXTENSIONS.put("js", APPLICATION_JAVASCRIPT);
		_FILE_EXTENSIONS.put("pdf", APPLICATION_PDF);
		_FILE_EXTENSIONS.put("pdf", APPLICATION_PDF);
		_FILE_EXTENSIONS.put("zip", APPLICATION_ZIP);
		_FILE_EXTENSIONS.put("gz", APPLICATION_GZIP);
		_FILE_EXTENSIONS.put("tgz", APPLICATION_GZIP);
		_FILE_EXTENSIONS.put("gif", IMAGE_GIF);
		_FILE_EXTENSIONS.put("jpg", IMAGE_JPEG);
		_FILE_EXTENSIONS.put("jpeg", IMAGE_JPEG);
		_FILE_EXTENSIONS.put("jpe", IMAGE_JPEG);
		_FILE_EXTENSIONS.put("pjpeg", IMAGE_PJPEG);
		_FILE_EXTENSIONS.put("png", IMAGE_PNG);
		_FILE_EXTENSIONS.put("svg", IMAGE_SVG);
		_FILE_EXTENSIONS.put("tiff", IMAGE_TIFF);
		_FILE_EXTENSIONS.put("properties", TEXT_PLAIN);
		_FILE_EXTENSIONS.put("java", TEXT_PLAIN);
		_FILE_EXTENSIONS.put("c", TEXT_PLAIN);
		_FILE_EXTENSIONS.put("h", TEXT_PLAIN);
		_FILE_EXTENSIONS.put("cpp", TEXT_PLAIN);
		_FILE_EXTENSIONS.put("hpp", TEXT_PLAIN);
		_FILE_EXTENSIONS.put("cs", TEXT_PLAIN);
		_FILE_EXTENSIONS.put("vb", TEXT_PLAIN);
		_FILE_EXTENSIONS.put("py", TEXT_PLAIN);
		_FILE_EXTENSIONS.put("c++", TEXT_PLAIN);
		
		FILE_EXTENSIONS = Collections.unmodifiableMap(_FILE_EXTENSIONS);
	}
	
	public static String getContentTypeFromFileExtension(String extension, String defaultContentType) {
		if (_FILE_EXTENSIONS.containsKey(extension))
			return _FILE_EXTENSIONS.get(extension);
		
		return defaultContentType;
	}
	
	public static String getContentTypeFromFileExtension(String extension) {
		return getContentTypeFromFileExtension(extension, APPLICATION_BINARY);
	}
	
	
}
