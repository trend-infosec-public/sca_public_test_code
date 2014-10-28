

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/*******************************************
*
*
* Base Test Program -- SimpleStudentDB
*
* A simple database which contains students, courses, and the registrations of
* each student. Operations are parsed in this main file and are mostly handled
* in the RegistrationDao.
*
* Variant Test Case Program
* Added a password that is needed to perform the functions unregister and listRegistrations.
* The password check is performed within the RegistrationDao.java file. With a sequence like
* "junk' OR '1' = '1 ", an exploiter could gain access to these functions for another student
* that he/she should not normally have access to.
* The password check (TRIGGER_POINT) is in the RegistrationDao
* the CROSSOVER_POINT where the the program stores input in the config file
* with checking for single quotes.
*
* Variant Features:
*		SOURCE_TAINT:COMMAND_LINE
*		DATA_TYPE:UNSIGNED_INT
*		DATA_FLOW:ARRAY_INDEX_LINEAR_EXPRESSION
*		CONTROL_FLOW:POINTER_TO_FUNCTION
*
* Initial Testing: (x means yes, - means no)
*   Tested in MS Windows XP 32bit        x
*   Tested in Ubuntu10_10 Linux 32bit    -
*
*********************************************/

package stonesoup;

import java.util.Set;

import stonesoup.studentmodel.Command;
import stonesoup.studentmodel.Config;
import stonesoup.studentmodel.Course;

//The main class for the student db app.
public class SimpleStudentDb
{
    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) //STONESOUP:SOURCE_TAINT:COMMAND_LINE
    {
    	try
    	{
            final Config config = new Config();
    		processCommandLine(args, config);//STONESOUP:INTERACTION_POINT
            try {
				execute(config);//STONESOUP:CROSSOVER_POINT
			} catch (Exception e) {
				System.err.println("Could not execute commands." + e.getMessage());
			}
    	}
        catch(final Exception e)
        {
            System.err.println("Could not process command line:" + e.getMessage());
        }

    }

    //goes through the arguments and checks if they are valid
    private static void processCommandLine(final String[] args, final Config config)
    {
        boolean success = true;
        boolean foundInstructions = false;
        boolean foundStudent = false;
        boolean foundCourseName = false;
        boolean foundPassword = false;

        final String command = args.length > 0 ? args[0] : null;
        if(command == null) {
            success = false;
        } else {
            config.setCommand(command.toLowerCase());
        }

        if (command.equals(Command.help.toString())){
        	foundInstructions = true;
        }


        // loop through the arguments and add the properties to config
        for(int i = 1; i < args.length; i++) {	//STONESOUP:DATA_TYPE:UNSIGNED_INT
            final String arg = args[i].toLowerCase();
            String val = null;
            if(args.length > i + 1){
                val = args[++i];	//STONESOUP:DATA_FLOW:ARRAY_INDEX_LINEAR_EXPRESSION
            }

            if(arg.equalsIgnoreCase("-s") && val != null){
            	try
            	{
            		config.setStudentId(val);
            		foundStudent = true;
            	}
            	catch(NumberFormatException nfe){
            		System.err.println("An number error occurred: " + nfe.getMessage());
            	}
            	catch(Exception e){
            		System.err.println("An unknown error occurred: " + e.getMessage());
            	}
            }

            else if(arg.equalsIgnoreCase("-c") && val != null)
            {
            	config.setCourseNumber(val);
            	foundCourseName = true;
            }
            else if (arg.equalsIgnoreCase("-p"))
            {
            	config.setPassword(val);	//STONESOUP:CONTROL_FLOW:POINTER_TO_FUNCTION
            	foundPassword = true;
            }
        }

        if(!success
        		|| (command.equals(Command.help) && !foundInstructions)
        		|| (command.equals(Command.liststudentcourses) && (!foundStudent ||!foundPassword))
                || (command.equals(Command.register) && (!foundStudent || !foundCourseName ||!foundPassword))
                || (command.equals(Command.unregister) && (!foundStudent || !foundCourseName || !foundPassword))
          )
        {
        	System.err.print("wrong command/arguments parsed");
            System.exit(1);
        }
    }

    //Handles commands and execute
    private static void execute(Config config) throws Exception{
    	Command command = Command.UNKNOWN;
    	try {
			command = Command.valueOf(config.getCommand());
		} catch (IllegalArgumentException e) {
			// unknown command
			command = Command.UNKNOWN;
		}

		final RegistrationDao service = new RegistrationDao();
    	switch (command) {
    	case register:
    		//TODO add password check
            service.addRegistration(config);
            break;
    	case liststudentcourses:
    		//faulty password check
            final Set<Course> rlist = service.getRegistrationsForStudent(config);
            service.printRegistrations(rlist);
            break;
    	case unregister:
        	 //faulty password check
            service.deleteRegistration(config);
            break;
    	case help:
    		usage();
    		break;
        default:
	       	System.err.print("Unknown Command Error");
            System.exit(1);
			break;
    	}
    }

    //help for program use
    private static void usage()
    {
   	  System.out.print("School Manager Help: [command] [options {-s -c -p}]\n" +
         "" + Command.help + "\n" +
         "" + Command.liststudentcourses + " -s [studentid]  -p [password]\n" +
         "" + Command.register + " -s [studentid] -c [courseabrv] -p [password]\n" +
         "" + Command.unregister + " -s [studentid] -c [courseabrv] -p [password]\n" +
         "");
    }
}
