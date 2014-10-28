/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import stonesoup.Configuration;
import stonesoup.filesystem.NameFilter;

public class AccessControlUtilities {

	public static boolean hasReadAccess(String path) throws Exception {
		File file = new File(path);
		File directory = null;
		NameFilter filter = new NameFilter();
		String whitelistName = Configuration.Instance.getProperty("Server.filesystem.whitelist", ".whitelist");
		String blacklistName = Configuration.Instance.getProperty("Server.filesystem.blacklist", ".blacklist");
		filter.addBlocked(whitelistName);
		filter.addBlocked(blacklistName);
		
		if (file.isFile()) {
			directory = file.getParentFile();
			if (directory == null) {
				directory = new File(".");
			}
		} else if (file.isDirectory()) {
			directory = file;
		}
		
		File whitelist = new File(directory.getPath(), whitelistName);
		File blacklist = new File(directory.getPath(), blacklistName);
		
		filter.addAllowed(getListedEntries(whitelist));
		filter.addBlocked(getListedEntries(blacklist));
		
		File[] permittedFiles = directory.listFiles(filter);
		
		for (File permitted : permittedFiles) {
			if (permitted.getName().equalsIgnoreCase(file.getName()))
				return true;
		}
		
		return false;
	}
	
	private static List<Object> getListedEntries(File list) throws Exception {
		List<Object> entries = new ArrayList<Object>();
		
		if (!(list.isFile() && list.canRead())) {
			return entries;
		}
		
		BufferedReader input = null;
		
		try {
			input = new BufferedReader(new FileReader(list));
			
			while (true) {
				String entry = input.readLine();
				if (entry == null) {
					break;
				}
				Object parsedEntry = parseEntry(entry);
				if (parsedEntry != null)
					entries.add(parsedEntry);
			}
		} catch (FileNotFoundException e) {
			//this should never happen since we do a check above
		} catch (IOException e) {
			throw e;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					//swallow, just trying to clean up
				}
			}
		}
		
		return entries;
	}
	
	private static Object parseEntry(String entry) {
		if (entry == null)
			return null;
		
		if (entry.length() == 0)
			return null;
		
		String[] typeAndValue = entry.trim().split("=", 2);
		String type = "literal";
		String value = null;
		
		switch (typeAndValue.length) {
			case 1:
				type = "literal";
				value = typeAndValue[0];
				break;
			case 2:
				type = typeAndValue[0].toLowerCase();
				value = typeAndValue[1];
				break;
		}
		
		value = value.trim();
		if (value.startsWith("\"") && value.endsWith("\"")) {
			value = value.substring(1, value.length() - 1);
		}
		
		if (type.equals("literal")) {
			return value;
		} else if (type.equals("regex")) {
			return Pattern.compile(value);
		}
		
		return null;
	}
}
