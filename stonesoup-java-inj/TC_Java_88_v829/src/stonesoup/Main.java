

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
** 
** Date: 07/21/2011
**
** Base Test Program -- SimpleCRUDWebServer
**
** The CRUD Web Server allows for users to create, update, or delete items from a database.
**
** Variant Test Case Program
**
** The web server is still allowed to create or update items in a database but in order to 
** delete items it needs to have administrative privileges. In order to bypass a password though
** a malicious user can just insert "authorized=1" as an argument and they will be able to wipe out the 
** database. (All of the code changes can be found in ClientConnection.java)
**
** STONESOUP Weakness Class: SQL Injection
** CWE ID: CWE-88
** Variant Spreadsheet Rev #: 1
** Variant Spreadsheet ID: 829
**
** Variant Features:
**   Source Taint: file_contents
**   Data Type:    array_length_literal
**   Control Flow: loop_complexity_initialization
**   Data Flow:    array_index_constant
**
** Initial Testing: (x means yes, - means no)
**   Tested in MS Windows XP 32bit        x
**   Tested in MS Windows 7  64bit        -
**   Tested in Ubuntu10_10 Linux 32bit    -
**   Tested in Ubuntu10_10 Linux 64bit    -
**
** Workflow:
**
** I/0 Pairs:
**   Good: 1st Set: socket[http://localhost:9080?op=insert&value=whatever] 
**         2nd Set: socket[http://localhost:9080?op=update&value=whatever&id=1]
**         3rd Set: socket[http://localhost:9080?op=delete&id=1&pswd=what]
**    Bad: 1st Set:	socket[http://localhost:9080?op=delete&id=1&authorized=1]	
*********************************************/


package stonesoup;

import stonesoup.server.Server;

/**
 *
 * 
 */
public class Main 
{
	public static void main(String[] args)
	{
		try 
		{	
			Server server = new Server();
			server.readConfigFile();
			System.err.println("Read Config");
			server.initDb();
			System.err.println("Initalize");
			server.listen();
			System.err.println("Listen");
		}
		catch(Exception ex)
		{
			System.err.println("!! " + ex.getMessage());
		}
	}
}
 