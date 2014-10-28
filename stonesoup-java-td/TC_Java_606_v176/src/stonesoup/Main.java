

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/*******************************************
**
**
** 
** Date: 6/10/2011
**
** Base Test Program -- ISBNChecker
**
** Checks the validity of ISBN numbers.
**
** Variant Test Case Program
**
** Can now input ISBN numbers to check from a file. Will check any
**  ISBN of length 10 or greater rather than just 10 or 13, so you
**  can check a very long ISBN for excessive looping.
**
** STONESOUP Weakness Class: <weakness class>
** CWE ID: CWE-606
** Variant Spreadsheet Rev #: 1
** Variant Spreadsheet ID: 176
**
** Variant Features:
**   Source_Taint:FILE_CONTENTS
**   DATA_TYPE:ARRAY_LENGTH_VARIABLE
**   Data_Flow:ARRAY_INDEX_VARIABLE
**   Control_Flow:INTERRUPT_CONTINUE
**
*********************************************/

package stonesoup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * ISBNChecker Main
 * This command line tool calls the ISBNChecker.isValidISBN() method against the
 *  first command line argument.
 *
 * 
 */
public class Main {
	
	public static void infoAndExit() {
		System.out.println("Usage: java -jar ISBNChecker.jar [length] [index] [filename]");
        System.exit(1);
	}
	
	public static void main(String[] args) {
		
    	if (args.length == 3) {

    		int length = Integer.parseInt(args[0]);
			if (length<2 || length>5) {
				System.out.println("length must be between 2 and 4");
		        System.exit(1);
			}
			
			int index = Integer.parseInt(args[1]);
			if (index<0 || index>2) {
				System.out.println("index must be between 0 and 2");
		        System.exit(1);
			}
			
	    	String[] isbn = new String[length]; //STONESOUP:DATA_TYPE:ARRAY_LENGTH_VARIABLE
	    	isbn[0] = "stonesoup";

        	try{
    			// Retrieve input from a file.
        	    isbn[1] = rFileContents(args[2]); //STONESOUP:INTERACTION_POINT //STONESOUP:SOURCE_TAINT:FILE_CONTENTS
        	    if (isbn[1].contains("1")) throw new Exception("error");
        	}
        	catch(Exception e)
        	{
        		System.out.print("x-");
        	}
        	finally //STONESOUP:CONTROL_FLOW:INTERRUPT_CONTINUE
        	{
        		System.out.println(ISBNChecker.isValidISBN(isbn, index));        		
        	}
        	
    	}
    	
        // Help message
    	else {
    		infoAndExit();
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
