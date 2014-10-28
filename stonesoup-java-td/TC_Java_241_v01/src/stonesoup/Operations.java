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

public enum Operations {
	Download(1);
	
	private static final int DOWNLOAD = 1;
	
	private int id = 0;
	
	private Operations(int id) {
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
			case Download:
				return "Download";
		}
		
		return "Unknown";
	}
	
	public static Operations decodeId(byte[] idBytes) {
		BigInteger id = new BigInteger(idBytes);	//STONESOUP:INTERACTION_POINT
	
		switch (id.intValue()) {
			case DOWNLOAD:
				return Operations.Download;
			default:
				System.out.printf("Received unknown result code '%d'.\n", id.intValue());
				return Operations.Download;
		}
	}
}