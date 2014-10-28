/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtilities {

	private static final int BUFFER_SIZE = 4096;
	private static final Pattern PATTERN_ABS_REL_PATHS = Pattern.compile("^(\\/.*)|(.*\\.{1,2}\\/.*)$");
	
	public static byte[] readFileFully(File file) throws IOException {
		
		byte[] buffer = new byte[BUFFER_SIZE];
		
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		
		FileInputStream inStream = null;
		
		try {
			inStream = new FileInputStream(file);
			
			int readSize = 0;
			while ((readSize = inStream.read(buffer)) != -1) {
				outStream.write(buffer, 0, readSize);
			}
		} finally {
			Arrays.fill(buffer, (byte)0x00);
			buffer = null;
			buffer = outStream.toByteArray();
			
			if (inStream != null) {
				try {
					inStream.close();
				} catch (IOException e) {
					//swallow here, just trying to close out
				}
			}
			
			try {
				outStream.close();
			} catch (IOException e) {
				//swallow here, just trying to close out
			}
		}
		
		return buffer;
	}
	
	public static boolean isPathSafe(String path) {
		Matcher matcher = PATTERN_ABS_REL_PATHS.matcher(path);
		return !matcher.matches();
	}
	
	public static boolean isPathLike(String path) {
		return path.contains(File.separator);
	}
}
