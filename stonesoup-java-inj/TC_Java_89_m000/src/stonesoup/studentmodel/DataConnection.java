

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package stonesoup.studentmodel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataConnection {
	Connection conn = null;
	
	// Constructor
	public DataConnection() {
		
	}
    /**
     * Initializes the connection to the database.  
     * Note:  No db pooling is used here,
     * so this won't scale very well.  
     * Every request gets its own connection.
     *
     * @return The connection
     * @throws SQLException on error
     */
	public Connection initialize() throws SQLException {
		final String dbUser = "sstcdbuser";
		final String dbPass = System.getenv("DBMYSQLPASSWORD");
		final String dbHost = System.getenv("DBMYSQLHOST");
		final String dbSchema = "simple_student_db_179";

		//final String dbDriver = "com.microsoft.jdbc.sqlserver.SQLServerDriver";
		//final String dbDriver = "org.postgresql.Driver";
		final String dbDriver = "com.mysql.jdbc.Driver";

		final String dbURL = "jdbc:mysql://" + dbHost + ":3306/"+ dbSchema;
		//final String dbURL = "jdbc:postgresql://" + dbHost + ":5433/" + dbSchema;
		//final String dbURL = "jdbc:microsoft:sqlserver://" + dbHost + ":1433/" + dbSchema;
		
		try {
			Class.forName(dbDriver);
		} catch (final Exception e) {
			throw new SQLException("Failed to load mySQL driver. dburl:"+dbURL+" dbDriver:"+dbDriver, e);
		}

		try {
            conn = DriverManager.getConnection(dbURL, dbUser, dbPass);
		} catch (SQLException sqle) {
			throw new SQLException("Error occurred while getting connection. dburl:"+dbURL,
					sqle);
		}
		return conn;
	}


	public void close() throws SQLException {
			try {
				conn.close();
			} catch (SQLException e) {
				System.err.println("Failure closing Connection");
			}
	}


}
