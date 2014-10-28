

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package stonesoup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
/***********************************************************************
**
**
**  
**  Date: 21 March 2012
**
**  Revision History
**  Date      Change
**
**
**  Main will run as the backend for a request. It will take in a user's name (at this point the app assumes the single
** 	user is authenticated and authorized) and print all transactions FROM that user.
**
**  Program outputs a list of transactions.
**
** Error: This doesn't use prepared statements, so SQL injection is possible.
**
**
** STONESOUP Weakness Class: SQL Injection: Hibernate
** CWE ID: CWE-564
** Variant Spreadsheet Rev #: ###
** Variant Spreadsheet ID: ###
**
** Variant Features:
**
**
** I/0 Pairs:
**   Good: 1st Set: "Danny"
**         2nd Set: "Bryan"
**         3rd Set: "Chris"
**         4th Set:
**         5th Set:
**    Bad: 1st Set: "Danny' or 'a'='a"
**         2nd Set:
**
** How program works:
**		The program takes a URL as a command line argument and uses it to build a query against a database.
**
***********************************************************************/
public class TransactionDb 
{
	private SessionFactory factory;
	
	public static void main(String[] args) {
		//This just quiets down hibernate to remove default noise
		Logger logger = Logger.getLogger("org.hibernate");
		logger.setLevel(Level.OFF);
		
		//START SOURCE TAINT
		//requires single line in a file, named 'input'
		BufferedReader in = null;
		String name = "";
		if(args.length == 1){
			try 
			{
				in = new BufferedReader(new FileReader(args[0]));
				name = in.readLine(); // STONESOUP:INTERACTION_POINT
			} 
			catch (Exception e) 
			{
				System.err.println("Unable to read input file");
				System.exit(-1);
			}
			finally 
			{
				if (in != null) 
				{
					try  { in.close(); }
					catch (IOException e) { }
				}
			}
			//END SOURCE TAINT
		}else{
			System.err.println("Invalid number of parameters: Requires 1");
			System.exit(-1);
		}

		
		final TransactionDb db = new TransactionDb();
		
		//configure hibernate
		final Configuration cfg = new AnnotationConfiguration()
			.addAnnotatedClass(PendingTransactions.class)
			.setProperty("hibernate.show_sql", "false")
			.setProperty("hibernate.format_sql", "false")
			.setProperty("hibernate.connection.username", "sstcdbuser")
			.setProperty("hibernate.connection.password", "testcaseuser")
			.setProperty("hibernate.connection.driver", "com.mysql.jdbc.Driver")
			.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
			.setProperty("hibernate.connection.url", "jdbc:mysql://206.159.212.17/stonesoup-crud");
		db.factory = cfg.buildSessionFactory();
		
		//do lookup
		final Session session = db.factory.openSession();
		
		//This is the 'bad' query, uses user input
		final Query query = session
				.createQuery("from PendingTransactions where trans_from = '" + name + "'");//STONESOUP:CROSSOVER_POINT
		
		//the actual lookup
		final List<PendingTransactions> list = query.list();  //STONESOUP:TRIGGER_POINT
		
		//format the results to print out
		final StringBuilder builder = new StringBuilder();
		builder.append("Transactions: \n");
		for (final PendingTransactions c : list) {
			builder.append("From: ").append(c.getTrans_from()).append("; ")
				   .append("To: ").append(c.getTrans_to()).append("; ")
				   .append("Amt: $").append(c.getAmount()).append("; ")
				   .append("Date: ").append(c.getTrans_date()).append(".\r\n");
		}
		final String result = builder.toString();
		
		//print formatted results
		System.out.println(result);
	}
}
