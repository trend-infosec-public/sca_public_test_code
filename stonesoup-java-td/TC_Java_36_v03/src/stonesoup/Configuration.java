/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Configuration {
	
	/**
	 * Singleton accessor for configuration.
	 */
	public static final Properties Instance = new Properties();
	
	/**
	 * Loads and parses the specified configuration file.
	 * 
	 * @param configFilename
	 * @throws IOException
	 */
	protected static void loadConfiguration(String configFilename) throws IOException {
		File configFile = new File(configFilename);
		FileInputStream configStream = new FileInputStream(configFile);
		Instance.load(configStream);
		configStream.close();
	}
}
