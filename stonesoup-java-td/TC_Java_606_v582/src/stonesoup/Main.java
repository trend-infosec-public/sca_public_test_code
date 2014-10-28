

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
** Variant Spreadsheet ID: 582
**
** Variant Features:
**   Source Taint: STDIN_TERMINAL
**   Data Type: ARRAY_LENGTH_NONLINEAR_EXPRESSION
**   Control Flow: FUNCTION_INVOCATION_OVERLOADING
**   Data Flow: ARRAY_INDEX_ARRAY_CONTENT_VALUE
**
*********************************************/

package stonesoup;

/**
 * ISBNChecker Main
 * This command line tool calls the ISBNChecker.isValidISBN() method against the
 *  first command line argument.
 *
 * 
 */
public class Main {

    public static void main(String[] args) {

    	// Help message
        if (args.length >= 1 && (args[0].equals("-h") || args[0].equals("--h") || args[0].equals("help"))) {
            System.out.println("Usage: java -jar ISBNChecker.jar [length] [index]");
            System.out.println("  [length] may be a number between 2 and 4");
            System.out.println("  [index] may be a number between 0 and 3");
            System.exit(1);
        }
        else if(args.length == 2){
        	try{
        		int length = Integer.parseInt(args[0]);
        		if (length < 2 || length > 4) {
                    System.out.println("length may be a number between 2 and 4");
                    System.exit(1);
        		}

        		int index = Integer.parseInt(args[1]);
        		if (index < 0 || index > 3) {
                    System.out.println("index may be a number between 0 and 3");
                    System.exit(1);
        		}

        	    String[] array = new String[length*length]; //STONESOUP:DATA_TYPE:ARRAY_LENGTH_NONLINEAR_EXPRESSION
        		array[0] = "stonesoup";
        		array[1] = "stonesoup 2";
        		array[2] = ISBNChecker.func();
         
        		int[] indexArray = new int[3];
        		indexArray[0] = 0;
        		indexArray[1] = index;
        		
        	    System.out.println(ISBNChecker.func(array[indexArray[1]])); //STONESOUP:CONTROL_FLOW:FUNCTION_INVOCATION_OVERLOADING //STONESOUP:DATA_FLOW:ARRAY_INDEX_ARRAY_CONTENT_VALUE
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
