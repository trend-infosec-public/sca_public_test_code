/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup;

import java.util.List;

import stonesoup.utils.Shell;

public class ShellDriver {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Shell shell = new Shell();
		List<String> output = shell.execute("nc -lk 127.0.0.1 8025");
		System.out.println("stdout:");
		System.out.println(output.get(0));
		System.out.println();
		System.out.println("stderr:");
		System.out.println(output.get(1));
		System.out.println();
	}

}
