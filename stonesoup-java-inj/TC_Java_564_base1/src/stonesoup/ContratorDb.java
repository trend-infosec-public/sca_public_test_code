

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/***********************************************************************
**
**
**  
**  Date: 18 Mar 2012
**
**  Revision History
**  Date      	Change
**  18 Mar 2012 Initial Creation
**  23 Mar 2012 Changed to file input
**  
**
**  This is a very simple database application that uses the Hibernate 
**  framework to provide ORM (Object-Relational Mapping) to the database.
**  The databases schema is a simple contractor table with some properties
**  associated with contractors, including a classified boolean property, used t
**  to indicate that that contractor's information should not be disclosed.
**
**  The application does a simple last name lookup on the contractor database, 
**  written only to return contractors with classified=false.  The details are 
**  then printed out to the screen. The input comes from a file called 'input'
**  located in the home folder of the test.
**
**  Error:  Since the query does not properly use bind variables in it's HQL 
**  (Hibernate Query Language), the last name can be manipulated to return *all*
**  of the contractors, including those marked classified, and result in all of 
**  the data being displayed to the user.  
**
**  STONESOUP Weakness Class: Injection
**  CWE ID: CWE-564
**  Variant Spreadsheet Rev #: ###
**  Variant Spreadsheet ID: ???
**
**  Variant Features:
**
**
**  I/0 Pairs:
**     ??
**
**  How program works:
**     The code is run via the main(...) method.  Since this code uses Hibernate,
**     it requires that several JAR files are placed in the classpath in order 
**     for the code to properly execute.  The best way to package this is to make use
**	   of the existing ant.xml script to package the whole thing in an executable
** 	   JAR file.  The code expects a single string found in a file called input  
**     that indicate the last name to search by.  The executable jar is run like:
**   		java -jar contractor.jar
**
************************************************************************/
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

@SuppressWarnings("deprecation")
public class ContratorDb {

	private SessionFactory factory;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//This just quiets down hibernate to remove default noise
		Logger logger = Logger.getLogger("org.hibernate");
		logger.setLevel(Level.OFF);
		
		//START SOURCE TAINT
		//requires single line in a file, named 'input'
		BufferedReader in = null;
		String last = "";
		try {
			in = new BufferedReader(new FileReader(args[0]));
			last = in.readLine(); // STONESOUP:INTERACTION_POINT
		} catch (Exception e) {
			System.err.println("Unable to read input file");
			System.exit(-1);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					;;
				}
			}
		}
		//END SOURCE TAINT
		
		final ContratorDb db = new ContratorDb();
		
		//configure hibernate
		final Configuration cfg = new AnnotationConfiguration()
			.addAnnotatedClass(Contractor.class)
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
		
		//this is the 'bad' query
		final Query query =
			 session.createQuery("from Contractor where classified = false and last = '" + last + "'");  // STONESOUP:CROSSOVER_POINT
		
		//the actual lookup
		@SuppressWarnings("unchecked")
		final List<Contractor> list = query.list();  //STONESOUP:TRIGGER_POINT
		
		//format the results to print out
		final StringBuilder builder = new StringBuilder();
		builder.append("Contractors: \n");
		for (final Contractor c : list) {
			builder.append("\tName: ").append(c.getLast()).append(", ")
					.append(c.getFirst()).append(" | Title: ")
					.append(c.getTitle()).append(" ---  ")
					.append(c.isClassified() ? "CLASSIFIED" : "").append("\n");
		}
		final String result = builder.toString();
		
		//print formatted results
		System.out.println(result);
	}
}
