

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

    private static int getIndex() {
    	int x = Main.intIndex;
    	return x;
    }

    public static boolean func (long[] array) {
    	boolean tmp = isValidISBN(array);
    	return tmp;
    }
    
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
    private static boolean isValidISBN (long[] array) { //STONESOUP:CONTROL_FLOW:INTERPROCEDURAL_2

    	String isbn = new Long(array[getIndex()]).toString(); //STONESOUP:DATA_FLOW:ARRAY_INDEX_FUNCTION_RETURN_VALUE

        if (isbn.length() == 13) {
            // Validate ISBN-13 string

            // ISBN-13 number must be all digits
            try {
                Long.parseLong(isbn); 
            } catch (NumberFormatException e) {
                return false;
            }

            // Convert String to ints for calculation of check digit
            int[] isbnInts = new int[13];
            byte byteISBN = 50;
            for (int i=0; i<isbn.length(); i++) {
                isbnInts[i] = Character.getNumericValue(isbn.charAt(i));

                // ARB - The following lines have been added to force the infinite loop
                // when certain ISBN numbers are supplied.
                
                if (byteISBN <= 0) {
                	--i; // STONESOUP:TRIGGER_POINT
                }
                else {
	                byteISBN = (byte)(byteISBN + (byte)isbnInts[i]); // STONESOUP:CROSSOVER_POINT
                }

            }

            // Validate check digit
            if (isbnInts[12] == (10 - ((isbnInts[0]+3*isbnInts[1]+isbnInts[2]+3*isbnInts[3]+isbnInts[4]+3*isbnInts[5]+isbnInts[6]+3*isbnInts[7]+isbnInts[8]+3*isbnInts[9]+isbnInts[10]+3*isbnInts[11]) % 10)) % 10) {
                return true;
            } else {
                return false;
            }

        } else if (isbn.length() == 10) {
            // Validate ISBN-10 string

            // ISBN-10 number must be 9 digits followed by either another digit or an 'X'
            try {
                Integer.parseInt(isbn.substring(0, 9));
                if (!(Character.isDigit(isbn.charAt(9)) || isbn.charAt(9) == 'X' || isbn.charAt(9) == 'x'))
                    throw new NumberFormatException("Last character of ISBN-10 number is not a digit or an 'X'");
            } catch (NumberFormatException e) {
                return false;
            }

            // Convert String to ints for calculation of check digit
            int[] isbnInts = new int[10];
            for (int i=0; i<isbn.length(); i++) {
                if (i == 9 && (isbn.charAt(9) == 'X' || isbn.charAt(9) == 'x')) {
                    isbnInts[i] = 10;
                } else {
                    isbnInts[i] = Character.getNumericValue(isbn.charAt(i));
                }
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