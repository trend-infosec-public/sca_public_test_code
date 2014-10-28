/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.filesystem;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import stonesoup.Configuration;

/**
 * Implements a java.io.FileFilter that can limits directory listings for files
 * using literals and regular expressions.
 * 
 * 
 *
 */
public class NameFilter implements FileFilter {

	private List<Object> allowed = null;
	private List<Object> blocked = null;
	private boolean checkCaseSensitive = false;
	
	public NameFilter() {
		this.allowed = new ArrayList<Object>();
		this.blocked = new ArrayList<Object>();
		this.checkCaseSensitive = Boolean.parseBoolean(Configuration.Instance.getProperty("Server.filesystem.checkCaseSensitive", "false"));
	}
	
	public void addAllowed(String filename) {
		this.allowed.add(filename);
	}
	
	public void addBlocked(String filename) {
		this.blocked.add(filename);
	}
	
	public void addAllowed(Pattern filepattern) {
		this.allowed.add(filepattern);
	}
	
	public void addBlocked(Pattern filepattern) {
		this.blocked.add(filepattern);
	}
	
	public void addAllowed(Collection<Object> col) {
		this.allowed.addAll(col);
	}
	
	public void addBlocked(Collection<Object> col) {
		this.blocked.addAll(col);
	}
	
	public List<Object> getAllowed() {
		return Collections.unmodifiableList(this.allowed);
	}
	
	public List<Object> getBlocked() {
		return Collections.unmodifiableList(this.blocked);
	}
	
	@Override
	public boolean accept(File pathname) {
		//first check if this is blacklisted file
		for (Object blockedFile : this.blocked) {
			if (String.class.isInstance(blockedFile)) {
				String blockedLiteral = (String)blockedFile;
				if (this.checkCaseSensitive) {
					if (pathname.getName().equals(blockedLiteral))
						return false;
				} else {
					if (pathname.getName().equalsIgnoreCase(blockedLiteral))
						return false;
				}
			} else if (Pattern.class.isInstance(blockedFile)) {
				Pattern blockedRegex = (Pattern)blockedFile;
				if (blockedRegex.matcher(pathname.getName()).matches())
					return false;
			}
		}
		
		//check if whitelisting is used
		if (this.allowed.size() > 0) {
			for (Object allowedFile : this.allowed) {
				if (String.class.isInstance(allowedFile)) {
					String allowedLiteral = (String)allowedFile;
					if (this.checkCaseSensitive) {
						if (pathname.getName().equals(allowedLiteral))
							return true;
					} else {
						if (pathname.getName().equalsIgnoreCase(allowedLiteral))
							return true;
					}
				} else if (Pattern.class.isInstance(allowedFile)) {
					Pattern allowedRegex = (Pattern)allowedFile;
					if (allowedRegex.matcher(pathname.getName()).matches())
						return true;
				}
			}
			
			//no whitelist match, reject
			return false;
		}
		
		//default is to allow if not blocked and no whitelist in place
		return true;
	}
}
