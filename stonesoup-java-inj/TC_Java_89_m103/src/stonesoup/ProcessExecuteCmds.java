

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package stonesoup;

import java.util.Set;
import stonesoup.studentmodel.Command;
import stonesoup.studentmodel.Config;
import stonesoup.studentmodel.Course;

public class ProcessExecuteCmds {

    //goes through the arguments and checks if they are valid
    public Config processCommandLine(final String[] args, final Config config)
    {
        boolean success = true;
        boolean foundStudent = false;
        boolean foundCourse = false;
        boolean foundPassword = false;

        final String command = args.length > 0 ? args[0] : null;

        //TODO compare to known commands enum
    	//System.out.println(command);
    	//System.out.println(commands.contains(command));

        if(command == null) {
            success = false;
        } else {
            config.setCommand(command.toLowerCase());
        }



        // loop through the arguments and add the properties to config
        for(int i = 1; i < args.length; i++) {
            final String arg = args[i].toLowerCase();
            String val = null;
            if(args.length > i + 1){
                val = args[++i];
            }
           // System.out.println("value:"+val);

            if(arg.equalsIgnoreCase("-s") && val != null){
            	try
            	{
            		config.setStudentId(Integer.parseInt(val));
            		foundStudent = true;
            	}
            	catch(NumberFormatException nfe){
            		System.err.println("An number error occurred: " + nfe.getMessage());
            	}
            	catch(Exception e){
            		System.err.println("An unknown error occurred: " + e.getMessage());
            	}
            }
            else if (arg.equalsIgnoreCase("-p"))
            {
            	config.setPassword(val);
            	foundPassword = true;
            }
            else if(val != null)
            {
            	//debug
            	config.setInstructions(val);
            }
        }

        if(!success
        		|| (command.equals(Command.liststudentcourses) && (!foundStudent ||!foundPassword))
                || (command.equals(Command.register) && (!foundStudent || !foundCourse ||!foundPassword))
                || (command.equals(Command.unregister) && (!foundStudent || !foundCourse || !foundPassword))

          )
        {
        	System.err.print("wrong command/arguments parsed");
        }
		return config;
    }

    //Handles commands and execute
    public void execute(Config config) throws Exception{
    	//System.out.println("Config:" + config.toString());

    	Command command = Command.UNKNOWN;
    	try {
			command = Command.valueOf(config.getCommand());	//STONESOUP:DATA_TYPE:SHORT_CHARACTER	//STONESOUP:DATA_FLOW:ADDRESS_AS_A_FUNCTION_RETURN_VALUE
		} catch (IllegalArgumentException e) {
			// unknown command
			command = Command.UNKNOWN;
		}
		//System.out.println("command:"+ command);

		final RegistrationDao service = new RegistrationDao();
    	switch (command) {	//STONESOUP:CONTROL_FLOW:CHOICE_CONDITIONAL
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
        default:
        	//usage();
        	System.err.print("Unknown Command Error");
            System.exit(1);
			break;
    	}
    }

    //help for program use
    public static void usage()
    {
   	  System.out.print("School Manager Help: [command] [options {-s -f -l -p -c -t -cn -sl}]\n" +
         "" + Command.liststudentcourses + " -s [studentid]  -p [password]\n" +
         "" + Command.register + " -s [studentid] -c [courseabrv] -p [password]\n" +
         "" + Command.unregister + " -s [studentid] -c [courseabrv] -p [password]\n" +
         "");
    }
}
