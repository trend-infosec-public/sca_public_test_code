/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under PRIME CONTRACT NUMBER: 2006-1148509-000
 **                  SUBCONTRACT NUMBER: 98730XSB01  
 ** 
 **   (C) Copyright 2012 Ponte Technologies LLC. All Rights Reserved
 ********************************************************************/

package stonesoup;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Authenticator {

	public enum AuthResult {
		Failed,
		SuccessUser,
		SuccessAdmin;
	}
	
	private Map<String, byte[]> admins = new HashMap<String, byte[]>();
	private Map<String, byte[]> users = new HashMap<String, byte[]>();
	
	public Authenticator() {
		
	}
	
	public void initialize(byte[] properties) {
		PropertyReader reader = new PropertyReader();
		List<String> setProperties = reader.loadProperties(properties);
		reader.loadAccounts(setProperties, this.admins, this.users);
	}
	
	public AuthResult authenticate(String username, String password) {
		AuthResult result = AuthResult.Failed;
		String usernameKey = username.toLowerCase();
		byte[] challengeAnswer = null;
		
		if (this.users.containsKey(usernameKey)) {
            challengeAnswer = this.users.get(usernameKey);
			result = AuthResult.SuccessUser;
		} else if (this.admins.containsKey(usernameKey)) {
            challengeAnswer = this.admins.get(usernameKey);
			result = AuthResult.SuccessAdmin;
		} else
			return AuthResult.Failed;
		
		//hash the provided password and compare for final authentication
		byte[] challenge = this.hashPassword(password);
		
		if (!Arrays.equals(challenge, challengeAnswer))
			result = AuthResult.Failed;
		
		return result;
	}
	
	private byte[] hashPassword(String password) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			//swallow this exception.  it is a built-in algorithm
		}
		
		byte[] passwordBytes = password.getBytes(Charset.forName("US-ASCII"));
		
		md.update(passwordBytes);
		byte[] passwordHash = md.digest();
		md = null;
		
		return passwordHash;
	}
}
