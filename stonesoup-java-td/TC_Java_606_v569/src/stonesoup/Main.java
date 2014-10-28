

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
** STONESOUP Weakness Class: Tainted Data
** CWE ID: CWE-606
** Variant Spreadsheet Rev #: 1
** Variant Spreadsheet ID: 569
**
** Variant Features:
**		SOURCE_TAINT:FILE_CONTENTS
**		DATA_TYPE:SHORT_CHARACTER
**		DATA_FLOW:INDEX_ALIAS_1
**		CONTROL_FLOW:COUNT_CONTROLLED_LOOP
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

    public static void main(String[] args) {

    	// Help message
        if (args.length >= 1 && (args[0].equals("-h") || args[0].equals("--h") || args[0].equals("help"))) {
            System.out.println("Usage: java -jar ISBNChecker.jar [isbn] [index] [filename]");
            System.out.println("  [isbn]");
            System.out.println("  [index] may be a number between 0 and 3");
            System.exit(1);
        }
        else if(args.length == 3) {
        	try{
        		int index = Integer.parseInt(args[1]);
        		if (index < 0 || index > 3) {
                    System.out.println("index may be a number between 0 and 3");
                    System.exit(1);
        		}

        		// Get the main part of the ISBN number from the command line.

        		String isbn = args[0];

        	    // Get the check digit from a file.

        		char[] checkDigit = new char[5];
        		int x = Integer.parseInt(args[1]);
        		int y = x + 1;
        		checkDigit[y] = rFileContents(args[2]).charAt(0);	//STONESOUP:INTERACTION_POINT	//STONESOUP:CROSSOVER_POINT	//STONESOUP:TRIGGER_POINT	//STONESOUP:SOURCE_TAINT:FILE_CONTENTS	//STONESOUP:DATA_TYPE:SHORT_CHARACTER	//STONESOUP:DATA_FLOW:INDEX_ALIAS_1

        	    System.out.println(ISBNChecker.isValidISBN(isbn, checkDigit[2]));
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();
        	}

        }
        else {
            System.out.println("Usage: java -jar ISBNChecker.jar [isbn]");
            System.out.println("  [isbn]");
            System.out.println("  [index] may be a number between 0 and 3");
            System.exit(1);
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
