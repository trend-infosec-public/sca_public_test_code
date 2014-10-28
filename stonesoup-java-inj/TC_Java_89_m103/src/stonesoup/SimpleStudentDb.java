

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/*******************************************
*
*
* Base Test Program -- SimpleStudentDB
*
* A simple database which contains students, courses, and the registrations of
* each student. Operations are parsed in this main file and are mostly handled
* in the RegistrationDao.
*
* Variant Test Case Program
* Added a password that is needed to perform the functions unregister and listRegistrations.
* The password check is performed within the RegistrationDao.java file. With a sequence like
* "junk' OR '1' = '1 ", an exploiter could gain access to these functions for another student
* that he/she should not normally have access to.
* The password check (TRIGGER_POINT) is in the RegistrationDao
* the CROSSOVER_POINT where the the program stores input in the config file
* with checking for single quotes.
*
* Variant Features:
*		SOURCE_TAINT:FILE_CONTENTS
*		DATA_TYPE:SHORT_CHARACTER
*		DATA_FLOW:ADDRESS_AS_A_FUNCTION_RETURN_VALUE
*		CONTROL_FLOW:CHOICE_CONDITIONAL
*
* Initial Testing: (x means yes, - means no)
*   Tested in MS Windows XP 32bit        x
*   Tested in Ubuntu10_10 Linux 32bit    -
*
*********************************************/

package stonesoup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import stonesoup.studentmodel.Config;

//MySQL
//Bypass Auth Mechanism
//	liststudentcourses VULNERABLE
//	register
//	unregister
//The main class for the student db app.
public class SimpleStudentDb
{
    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args)
    {
        final Config config = new Config();
		final ProcessExecuteCmds pscmd = new ProcessExecuteCmds();
    	try
    	{
    		String inputStr;

			// Retrieve input from a file.
    	    inputStr = rFileContents(args[0]); //STONESOUP:INTERACTION_POINT //STONESOUP:SOURCE_TAINT:FILE_CONTENTS

    		String[] inputArray = inputStr.split(":");

    		pscmd.processCommandLine(inputArray, config);
            try {
				pscmd.execute(config);//STONESOUP:CROSSOVER_POINT
			} catch (Exception e) {
				System.err.println("Could not execute commands." + e.getMessage());
			}
    	}
        catch(final Exception e)
        {
            System.err.println("Could not process command line:" + e.getMessage());
        }

    }

	private static String rFileContents(String in) {

		String value = "";

		try {
			//take in some numerical input
			if (Pattern.matches("^((.:.*)|([/\\\\].*)|(.*\\.\\..*))$", in)) {
				throw new IOException("ERROR: Illegal file name\nIt an absolute path or contains a double dot (e.g. '..') sequence");
			}

			BufferedReader br = new BufferedReader(new FileReader(in));
			value = br.readLine();
		} catch (IOException e) {
		      System.err.println("Error: " + e);
		      System.exit(1);
		}

		return value;
	}

}
