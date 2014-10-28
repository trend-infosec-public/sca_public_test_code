/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup;

import java.math.BigInteger;
import java.util.Arrays;

public enum Results {
	LoginFailed(1),
	AccessDenied(2),
	Success(3),
	NoSuchFile(4);
	
	private static final int LOGIN_FAILED = 1;
	private static final int ACCESS_DENIED = 2;
	private static final int SUCCESS = 3;
	private static final int NO_SUCH_FILE = 4;
	
	private int id = 0;
	
	private Results(int id) {
		this.id = id;
	}
	
	public int getId() {
		return this.id;
	}
	
	public byte[] encode() {
		byte[] idBytes = BigInteger.valueOf(this.id).toByteArray();
		
		if (idBytes.length < 4) {
			byte[] temp = new byte[4];
			Arrays.fill(temp, (byte)0);
			System.arraycopy(idBytes, 0, temp, 4-idBytes.length, idBytes.length);
			idBytes = temp;
		}
		
		return idBytes;
	}
	
	@Override
	public String toString() {
		switch (this) {
			case LoginFailed:
				return "Login Failed";
			case AccessDenied:
				return "Access Denied";
			case Success:
				return "Success";
			case NoSuchFile:
				return "No Such File";
		}
		
		return "Unknown";
	}
	
	public static Results decodeId(byte[] idBytes) {
		BigInteger id = new BigInteger(idBytes);
		
		switch (id.intValue()) {
			case LOGIN_FAILED:
				return Results.LoginFailed;
			case ACCESS_DENIED:
				return Results.AccessDenied;
			case SUCCESS:
				return Results.Success;
			case NO_SUCH_FILE:
				return Results.NoSuchFile;
			default:
				System.out.printf("Received unknown result code '%d'.\n", id.intValue());
				return Results.LoginFailed;
		}
	}
}
