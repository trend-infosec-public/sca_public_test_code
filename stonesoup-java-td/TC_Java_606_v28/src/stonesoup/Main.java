

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
** Variant Spreadsheet ID: 28
**
** Variant Features:
**		SOURCE_TAINT:FILE_CONTENTS (CLIPBOARD IN SPREADSHEET)
**		DATA_TYPE:ARRAY_LENGTH_LINEAR_EXPRESSION
**		DATA_FLOW:ARRAY_INDEX_CONSTANT
**		CONTROL_FLOW:RECURSIVE
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

	private static int index = 2;

	public static void infoAndExit() {
		System.out.println("Usage: java -jar ISBNChecker.jar [length] [filename]");
        System.exit(1);
	}

	public static void main(String[] args) {

    	if (args.length == 2) {

    		int length = Integer.parseInt(args[0]);
			if (length<1 || length>5) {
				System.out.println("length must be between 1 and 4");
		        System.exit(1);
			}

	    	String[] isbn = new String[length+5];	//STONESOUP:DATA_TYPE:ARRAY_LENGTH_LINEAR_EXPRESSION
	    	isbn[0] = "stonesoup";

			// Retrieve input from a file.
    	    isbn[index] = rFileContents(args[1]);	//STONESOUP:INTERACTION_POINT	//STONESOUP:DATA_FLOW:ARRAY_INDEX_CONSTANT //STONESOUP:SOURCE_TAINT:FILE_CONTENTS

        	System.out.println(ISBNChecker.isValidISBN(isbn[index]));

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
