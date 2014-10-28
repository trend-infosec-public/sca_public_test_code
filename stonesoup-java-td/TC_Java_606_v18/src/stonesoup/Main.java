

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
** Variant Spreadsheet ID: 18
**
** Variant Features:
**   Source Taint: STDIN
**   Data Type: SIGNED_LONG
**   Control Flow: INTERPROCEDURAL_2
**   Data Flow: ARRAY_INDEX_FUNCTION_RETURN_VALUE
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

	public static int intIndex = 0;
	
    public static void main(String[] args) {

    	// Help message
        if (args.length >= 1 && (args[0].equals("-h") || args[0].equals("--h") || args[0].equals("help"))) {
            System.out.println("Usage: java -jar ISBNChecker.jar [index]");
            System.out.println("  [index] may be a number between 0 and 3");
            System.exit(1);
        }
        else if(args.length == 1){
        	try{
        		intIndex = Integer.parseInt(args[0]);
        		if (intIndex < 0 || intIndex > 3) {
                    System.out.println("index may be a number between 0 and 3");
                    System.exit(1);
        		}
        		
        	    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));

        	    long[] array = new long[4];
        		array[0] = 0;
        		array[1] = 1;
        		array[2] = Long.parseLong(bufferRead.readLine()); //STONESOUP:SOURCE_TAINT:STDIN //STONESOUP:DATA_TYPE:SIGNED_LONG
        		array[3] = 3;
         
        	    System.out.println(ISBNChecker.func(array));
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();
        	}
        	
        }
        else {
            System.out.println("Usage: java -jar ISBNChecker.jar [index]");
            System.out.println("  [index] may be a number between 0 and 3");
            System.exit(1);
        }
    }
}
