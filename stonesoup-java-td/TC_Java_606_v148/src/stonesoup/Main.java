

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
** Date: 6/9/2011
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
** Variant Spreadsheet ID: 148
**
** Variant Features:
**   Source_Taint:_STDIN
**   Data_Type:SHORT_CHARACTER
**   Control_Flow:RECURSIVE
**   Data_Flow:ARRAY_INDEX_VARIABLE
**
*********************************************/

package stonesoup;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * ISBNChecker Main
 * This command line tool calls the ISBNChecker.isValidISBN() method against the
 *  first command line argument.
 *
 * 
 */
public class Main {
	
	public static void infoAndExit() {
		System.out.println("Usage: java -jar ISBNChecker.jar [index]");
        System.exit(1);
	}
	
	public static void main(String[] args) {
		
		int index = Integer.parseInt(args[0]);
		if (index<0 || index>2) {
			System.out.println("index must be between 0 and 2");
	        System.exit(1);
		}
		
    	String[] isbn = new String[3]; //STONESOUP:DATA_TYPE:SHORT_CHARACTER
    	isbn[0] = "stonesoup";
    	
    	if (args.length == 1) {
    		
        	try{
        	    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));

        	    isbn[2] = bufferRead.readLine(); //STONESOUP:SOURCE_TAINT:STDIN

        		System.out.println(ISBNChecker.isValidISBN(isbn, index));
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();
        	}
        	
    	}
    	
        // Help message
    	else {
    		infoAndExit();
        }
    	
    }
}