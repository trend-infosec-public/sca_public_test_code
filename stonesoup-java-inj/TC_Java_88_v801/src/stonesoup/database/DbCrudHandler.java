

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/


package stonesoup.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * 
 */
public class DbCrudHandler 
{
	private final DbConnector connector;
	private static final String QUERY_UPDATE  = "REPLACE INTO stonesoup (varval,id) VALUES (?,?)";
	private static final String   QUERY_INSERT  = "INSERT INTO stonesoup(varval) VALUES(?) ";
	private static final String   QUERY_DELETE  = "DELETE FROM stonesoup WHERE id = ? ";
	private static final String   QUERY_LAST_ID = "SELECT LAST_INSERT_ID() AS id ";


	public DbCrudHandler(final DbConnector connector)
	{
		this.connector = connector;
	}

	/**
	 * Performs an update on the database. Returns true on success, false on error
	 * @param id
	 * @param value
	 * @return
	 */
	public boolean doUpdate( final String id, final String value )
	{
		boolean retVal = false;

		try
		{
			if( connector.isConnected() ){
				PreparedStatement p_stmt = this.connector.getConnection().prepareStatement(QUERY_UPDATE);
				p_stmt.setString( 1, value );
				p_stmt.setString( 2, id );

				int rowsUpdated = p_stmt.executeUpdate();
				if(rowsUpdated > 0)
				{
					System.out.println("** updated varval: " + value );
					retVal = true;
				}
			}
		} 
		catch(SQLException ex)
		{
			System.err.println("!! error occured while executing update" );
		}
		return retVal;
	}

	/**
	 * Performs an insert on a database. Returns the new id inserted;
	 * @param value
	 * @return
	 */
	public String doInsert( final String value )
	{
		String retVal = null;

		try
		{
			if(connector.isConnected())
			{
				PreparedStatement p_stmt = this.connector.getConnection().prepareStatement(QUERY_INSERT);
				p_stmt.setString(1, value);
				
				int rowsInserted = p_stmt.executeUpdate();
				if(rowsInserted > 0)
				{
					retVal = this.getLastInsertId();
					System.out.println("** inserted varval: " + value);
				} 
				else 
				{
					System.err.println("!! error occured while executing insert" );
				}
			}
		} 
		catch(SQLException ex)
		{
			System.err.println("!! error occured while executing insert: " + ex.getMessage());
		}
		return retVal;
	}

	public boolean doDelete(final String id) 
	{
		boolean retVal = false;

		try 
		{
			if(this.connector.isConnected())
			{
				/* make sure we have something to delete first */
				
				PreparedStatement p_stmt = this.connector.getConnection().prepareStatement("REPLACE INTO stonesoup (id,varval) VALUES (?,?)");
				p_stmt.setString( 1, id );
				p_stmt.setString( 2, "test" );
				int rowsReplaced = p_stmt.executeUpdate();
				if(rowsReplaced > 0)
				{
					PreparedStatement p_stmt2 = this.connector.getConnection().prepareCall(QUERY_DELETE);
					p_stmt2.setString(1, id);
					int rowsDeleted = p_stmt2.executeUpdate();
					if(rowsDeleted > 0)
					{
						System.out.println("** deleted id: " + id );
						retVal = true;
					}
				}
			}
		} 
		catch(SQLException ex)
		{
			System.err.println("!! error occured during delete: " + ex.getMessage() );
		}
		
		return retVal;
	}
	
	private String getLastInsertId() throws SQLException 
	{
		String newId = null;
		
		if(this.connector.isConnected())
		{
			Statement stmt = this.connector.getConnection().createStatement();
			ResultSet results = stmt.executeQuery(QUERY_LAST_ID);
			
			if(results.next())
			{
				newId = results.getString(1);
			} 
			else 
			{
				System.err.println("!! unable to determine last inserted id");
			}
		}
		
		return newId;
	}
}
