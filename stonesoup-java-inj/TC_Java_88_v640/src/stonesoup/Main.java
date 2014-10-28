

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/*******************************************
* 
* Date: 07/21/2011
*
* Base Test Program -- SimpleCRUDWebServer
* The CRUD Web Server allows for users to create, update, or delete items from a database.
*
* Variant Test Case Program*
* The web server is still allowed to create or update items in a database but in order to 
* delete items it needs to have administrative privileges. In order to bypass a password though
* a malicious user can just insert "authorized=1" as an argument and they will be able to wipe out the 
* database. (All of the code changes can be found in ClientConnection.java)
*
* STONESOUP Weakness Class: SQL Injection
* CWE-88
* Variant Spreadsheet ID: 640*
* Variant Features:
*   Source Taint: file_contents
*   Data Type:    float
*   Control Flow: interfile
*   Data Flow:    array_index_function_return_value
*
* Initial Testing: (x means yes, - means no)
*   Tested in MS Windows XP 32bit        x
*   Tested in Ubuntu10_10 Linux 32bit    -
*
* Workflow:
* I/0 Pairs:
*   Good: 1st Set: socket[http://localhost:9080?op=insert&value=whatever] 
*         2nd Set: socket[http://localhost:9080?op=update&value=whatever&id=1]
*         3rd Set: socket[http://localhost:9080?op=delete&id=1&pswd=what]
*    Bad: 1st Set: socket[http://localhost:9080?op=delete&id=1&authorized=1]	
********************************************/

package stonesoup;
import stonesoup.server.Server;

public class Main 
{
	public static void main(String[] args)
	{
		System.err.println("main start");
		
		try 
		{	
			Server server = new Server();
			server.initDb();
			server.listen();
		}
		catch(Exception ex)
		{
			System.err.println("!! " + ex.getMessage());
		}

		System.err.println("main finish");
	}
}
 