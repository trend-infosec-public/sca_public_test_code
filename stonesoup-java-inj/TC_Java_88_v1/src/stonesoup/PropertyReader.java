/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

public class PropertyReader {
	
	private static final Pattern COMMENT_PATTERN = Pattern.compile("#.*");
	private static final Pattern PROPERTY_PATTERN = Pattern.compile("[ \\t]*[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+)*[ \\t]*=[ \\t]*.+");
	private static final Pattern USER_PATTERN = Pattern.compile("[ \\t]*([uU][sS][eE][rR]|[aA][dD][mM][iI][nN])\\.[a-zA-Z0-9]+[ \\t]*=[ \\t]*.+");

	public PropertyReader() {
		
	}
	
	public List<String> loadProperties(byte[] properties) {
		List<String> validProperties = new ArrayList<String>();
		
		// We are expecting text, so let's make this easier to parse
		ByteArrayInputStream byteStream = new ByteArrayInputStream(properties);
		InputStreamReader bufferedStream = new InputStreamReader(byteStream);
		BufferedReader reader = new BufferedReader(bufferedStream);
		
		String curLine = null;
		try {
			while ((curLine = reader.readLine()) != null) {
				//check if this is a comment
				if (COMMENT_PATTERN.matcher(curLine).matches())
					continue; //ignore comments
				
				//check if this line looks like a valid property
				if (!PROPERTY_PATTERN.matcher(curLine).matches())
					continue; //this line doesn't look like a valid property
				
				validProperties.add(curLine.trim());
			}
		} catch (IOException e) {
			System.out.printf("Failed to load the properties.");
			validProperties.clear();
			curLine = null;
		}
		
		//close the streams
		try {
			reader.close();
			bufferedStream.close();
			byteStream.close();
		} catch (IOException e) {
			//swallow this exception, we are only trying to clear resources
		}
		
		return validProperties;
	}
	
	public void loadAccounts(List<String> properties, Map<String, byte[]> admins, Map<String, byte[]> users) {
		
		for (String curProperty : properties) {
			//check if this is a valid account property
			if (!USER_PATTERN.matcher(curProperty).matches())
				continue; //skip a non-match
			
			String[] propertyNameAndValue = curProperty.split("=", 2);
			
			//extract account information
			String[] accountTypeAndUsername = propertyNameAndValue[0].trim().split("\\.");
			String accountType = accountTypeAndUsername[0].trim().toLowerCase();
			String username = accountTypeAndUsername[1].trim().toLowerCase();
			byte[] password = null;
			
			try {
				password = DatatypeConverter.parseHexBinary(propertyNameAndValue[1].trim());
			} catch (IllegalArgumentException e) {
				password = null;
				continue; //skip this account
			}
			
			if (accountType.equalsIgnoreCase("admin")) {
				admins.put(username, password);  //add this admin
			} else if (accountType.equalsIgnoreCase("user")) {
				users.put(username, password);  //add this user
			}
		}
		
	}

}
