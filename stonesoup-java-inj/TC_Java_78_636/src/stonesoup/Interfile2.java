

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package stonesoup;

/** The Interfile2 class support interfile control flow invocation, 
 * where Interfile2 calls the Interfile1 invocation method. **/

public class Interfile2 {
	public static void invoke (String commmandLine ) {
		Interfile1.invoke(commmandLine );
	}
}
