/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import stonesoup.Configuration;

public class UrlUtilities {

	private static final String HOSTNAME;
	private static final String IP_ADDRESS;
	private static final Pattern PATTERN_REL_URL;
	private static final Pattern PATTERN_ABS_URL;
	
	static {
		HOSTNAME = Configuration.Instance.getProperty("Server.hostname", "localhost");
		IP_ADDRESS = Configuration.Instance.getProperty("Server.ipAddress", "127.0.0.1");
		PATTERN_REL_URL = Pattern.compile("(/?.+)*");
		PATTERN_ABS_URL = Pattern.compile("((https?)|(ftp))://.*");
	}
	
	public static boolean isUrlLocal(String url) {
		if (PATTERN_ABS_URL.matcher(url).matches()) {
			URI uri;
			try {
				uri = new URI(url);
			} catch (URISyntaxException e) {
				return false;
			}
			if (uri.getHost().equalsIgnoreCase(HOSTNAME) || 
					uri.getHost().equalsIgnoreCase(IP_ADDRESS))
				return true;
		} else if (PATTERN_REL_URL.matcher(url).matches()) {
			return true;
		}
		
		return false;
	}
}
