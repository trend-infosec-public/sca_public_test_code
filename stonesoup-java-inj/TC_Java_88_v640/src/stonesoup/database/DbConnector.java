

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package stonesoup.database;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnector {

	private final String dbHost, dbName, dbUser, dbPass;
	private Connection dbConnection = null;

	public DbConnector(final String dbHost, final String dbName,
			final String dbUser, final String dbPass) {
		this.dbHost = dbHost;
		this.dbName = dbName;
		this.dbUser = dbUser;
		this.dbPass = dbPass;
	}

	/**
	 * Establish a connection with the database
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public void init() throws Exception {

		System.err.println("DbConnector init start");

		String connectionStr = "jdbc:mysql://" + this.dbHost + "/" + this.dbName;
		@SuppressWarnings("unused")
		Class dbDriver = Class.forName("com.mysql.jdbc.Driver");

		System.err.println("connectionStr=" + connectionStr);

		this.dbConnection = DriverManager.getConnection(connectionStr, dbUser, dbPass);

		System.err.println("DbConnector init finish");
	}

	/**
	 * Disconnect from the database
	 * 
	 * @throws Exception
	 */
	public void closeConnection() throws SQLException {
		if (this.dbConnection != null) {
			this.dbConnection.close();
		}
	}

	/**
	 * Determine if our DB connection is still up
	 * 
	 * @return
	 */
	public boolean isConnected() {
		boolean isConnected = false;

		if (this.dbConnection != null) {
			try {
				boolean isClosed = dbConnection.isClosed();
				isConnected = (isClosed == false);
			} catch (SQLException ex) {
				System.err.println("!! a problem occurred when determining db connection status");
			}
		}

		return isConnected;
	}

	public Connection getConnection() {
		return this.dbConnection;
	}
}
