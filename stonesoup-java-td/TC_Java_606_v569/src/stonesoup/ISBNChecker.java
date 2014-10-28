

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
*********************************************/

package stonesoup;

/**
 * This class contains methods for checking the validity of ISBN numbers
 *
 * 
 */
public class ISBNChecker {
	
    /**
     * This method checks the input String and returns whether or not it is a
     *  valid ISBN-10 or ISBN-13 number
     *
     * Reference and formulas from:
     *  http://en.wikipedia.org/wiki/International_Standard_Book_Number#ISBN-10
     *  http://en.wikipedia.org/wiki/International_Standard_Book_Number#ISBN-13
     * 
     * Example ISBN-13: 978-0-306-40615-7
     * Example ISBN-10: 0-306-40615-2
     *
     * @param isbn the input ISBN number to check, as a String
     * @return whether the input String is a valid ISBN number
     */
	public static boolean isValidISBN(String isbn, char checkDigit) {

		// Accept and remove spaces and dashes from input
        isbn = isbn.replaceAll("[ -]", "");
        
        if (isbn.length() == 12) {
            // Validate ISBN-13 string

            // ISBN-13 number must be all digits
            try {
                Long.parseLong(isbn);
            } catch (NumberFormatException e) {
                return false;
            }

            // Convert String to ints for calculation of check digit
            int[] isbnInts = new int[13];
            isbnInts[12] = Character.getNumericValue(checkDigit);
            for (int i=0; i<isbn.length(); i++) { //STONESOUP::CONTROL_FLOW:COUNT_CONTROLLED_LOOP
                isbnInts[i] = Character.getNumericValue(isbn.charAt(i));

                // ARB - This line has been inserted as the instance of CWE-606
                // if the check digit that was provided is a '2', then the
                // index for the loop will be altered such that it will go into
                // an infinite loop.
                
                if (checkDigit == '2') i--;

            }

            // Validate check digit
            if (isbnInts[12] == (10 - ((isbnInts[0]+3*isbnInts[1]+isbnInts[2]+3*isbnInts[3]+isbnInts[4]+3*isbnInts[5]+isbnInts[6]+3*isbnInts[7]+isbnInts[8]+3*isbnInts[9]+isbnInts[10]+3*isbnInts[11]) % 10)) % 10) {
                return true;
            } else {
                return false;
            }

        } else if (isbn.length() == 9) {
            // Validate ISBN-10 string

            // ISBN-10 number must be 9 digits followed by either another digit or an 'X'
            try {
                Integer.parseInt(isbn.substring(0, 9));
                if (!(Character.isDigit(checkDigit) || checkDigit == 'X' || checkDigit == 'x'))
                    throw new NumberFormatException("Last character of ISBN-10 number is not a digit or an 'X'");
            } catch (NumberFormatException e) {
                return false;
            }

            // Convert String to ints for calculation of check digit
            int[] isbnInts = new int[10];
            for (int i=0; i<isbn.length(); i++) {
            	isbnInts[i] = Character.getNumericValue(isbn.charAt(i));
            }
            if (checkDigit == 'X' || checkDigit == 'x') {
                isbnInts[9] = 10;
            }
            else {
            	isbnInts[9] = Character.getNumericValue(checkDigit);
            }

            // Validate check digit
            if (isbnInts[9] == (isbnInts[0]+2*isbnInts[1]+3*isbnInts[2]+4*isbnInts[3]+5*isbnInts[4]+6*isbnInts[5]+7*isbnInts[6]+8*isbnInts[7]+9*isbnInts[8]) % 11) {
                return true;
            } else {
                return false;
            }
        } else {
            // isbn is not 10 or 13 characters long
            return false;
        }
    }
}
