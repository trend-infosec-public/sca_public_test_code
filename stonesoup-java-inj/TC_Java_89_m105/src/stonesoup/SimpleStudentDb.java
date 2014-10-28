

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
*		DATA_TYPE:BOOLEAN
*		DATA_FLOW:PASS_BY_VALUE
*		CONTROL_FLOW:COUNT_CONTROLLED_LOOP
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
import stonesoup.studentmodel.Student;
import stonesoup.studentmodel.Teacher;

//MySQL  Bypass Auth Mechanism
//	liststudentcourses,
//	register,
//	unregister VULNERABLE
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
        boolean foundTeacher = false;
        boolean foundCourse = false;
        boolean foundFirst = false;
        boolean foundLast = false;
        boolean foundCourseName = false;
        boolean foundSize = false;
        boolean foundPassword = false;	//STONESOUP:DATA_TYPE:BOOLEAN

        final String command = args.length > 0 ? args[0] : null;

        //System.out.println("number of args:"+args.length);

        if(command == null) {
            success = false;
        } else {
            config.setCommand(command.toLowerCase());
        }

        if (command.equals(Command.liststudents.toString())){
        	foundInstructions = true;
        }
        if (command.equals(Command.listcourses.toString())){
        	foundInstructions = true;
        }
        if (command.equals(Command.listteachers.toString())){
        	foundInstructions = true;
        }
        if (command.equals(Command.help.toString())){
        	foundInstructions = true;
        }
        if (command.equals(Command.debug.toString())){
        	foundInstructions = true;
            config.setInstructions(args[1].toLowerCase());
        }

        // loop through the arguments and add the properties to config
        for(int i = 1; i < args.length; i++) {	//STONESOUP:CONTROL_FLOW:COUNT_CONTROLLED_LOOP
            final String arg = args[i].toLowerCase();
            String val = null;
            if(args.length > i + 1){
                val = args[++i];
            }
           // System.out.println("value:"+val);

            if(arg.equalsIgnoreCase("-f") && val != null) {
                config.setFirstName(val);
                foundFirst = true;
            }
            else if(arg.equalsIgnoreCase("-l") && val != null){
                config.setLastName(val);
                foundLast = true;
            }
            else if(arg.equalsIgnoreCase("-c") && val != null){
            	config.setCourseNumber(val);
            	foundCourse = true;
            }
            else if(arg.equalsIgnoreCase("-s") && val != null){
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

            else if(arg.equalsIgnoreCase("-c") && val != null)
            {
            	config.setCourseNumber(val);
            	foundCourseName = true;
            }
            else if (arg.equalsIgnoreCase("-sl"))
            {
               	try
            	{
               		config.setSizeLimit(Integer.parseInt(val));
               		foundSize = true;
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
            	config.setPassword(val);	//STONESOUP:DATA_FLOW:PASS_BY_VALUE
            	foundPassword = true;
            }
            else if (arg.equalsIgnoreCase("-t"))
            {
               	try
            	{
               		config.setTeacherId(Integer.parseInt(val));
               		foundTeacher = true;
            	}
            	catch(NumberFormatException nfe){
            		System.err.println("An number error occurred: " + nfe.getMessage());
            	}
            	catch(Exception e){
            		System.err.println("An unknown error occurred: " + e.getMessage());
            	}
            }
        }

        if(!success
                || (command.equals(Command.listcourses) && !foundInstructions)
        		|| (command.equals(Command.liststudents) && !foundInstructions)
        		|| (command.equals(Command.listteachers) && !foundInstructions)
        		|| (command.equals(Command.debug) && !foundInstructions)
        		|| (command.equals(Command.help) && !foundInstructions)
                || (command.equals(Command.createteacher) && (!foundFirst || !foundLast))
        		|| (command.equals(Command.deleteteacher) && !foundTeacher)
        		|| (command.equals(Command.deletestudent) && !foundStudent)
        		|| (command.equals(Command.deletecourse) && !foundCourse)
        		|| (command.equals(Command.liststudentcourses) && (!foundStudent ||!foundPassword))
                || (command.equals(Command.register) && (!foundStudent || !foundCourse ||!foundPassword))
                || (command.equals(Command.unregister) && (!foundStudent || !foundCourse || !foundPassword))
                || (command.equals(Command.createstudent) && (!foundFirst || !foundLast || !foundPassword))
                || (command.equals(Command.createcourse)&& (!foundCourse || !foundCourseName || !foundSize || !foundTeacher))

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
		case listcourses:
			//TODO add where clause input
            final Set<Course> clist = service.listCourses();
            service.printCourses(clist);
			break;
    	case liststudents:
    		//TODO add where clause input
            final Set<Student> slist = service.listStudents();
            service.printStudents(slist);
            break;
    	case createstudent:
            service.createStudent(config);
            break;
    	case deletestudent:
    		service.deleteStudent(config);
    		break;
    	case listteachers:
    		//TODO add where clause input
    		final Set<Teacher> tlist = service.listTeachers();
            service.printTeachers(tlist);
            break;
    	case createteacher:
            service.createTeacher(config);
            break;
    	case deleteteacher:
    		service.deleteTeacher(config);
    		break;
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
    	case deletecourse:
    		//TODO make password check on teacherid
    		service.deleteCourse(config);
    		break;
    	case createcourse:
    		//TODO make password check on teacherid
        	service.createCourse(config);
        	break;
    	case debug:
    		//TODO Attempt to blacklist bad sql cmds
        	service.debug(config);
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
   	  System.out.print("School Manager Help: [command] [options {-s -f -l -p -c -t -cn -sl}]\n" +
         "" + Command.help + "\n" +
         "" + Command.listteachers + "\n" +
         "" + Command.listcourses + "\n" +
         "" + Command.liststudents + "\n" +
         "" + Command.liststudentcourses + " -s [studentid]  -p [password]\n" +
         "" + Command.createstudent + " -f [firstName] -l [lastName] -p [password]\n" +
         "" + Command.register + " -s [studentid] -c [courseabrv] -p [password]\n" +
         "" + Command.unregister + " -s [studentid] -c [courseabrv] -p [password]\n" +
         "" + Command.deletestudent + " -s [studentid]\n" +
         "" + Command.createteacher + " -f [firstName] -l [lastName]\n" +
         "" + Command.deleteteacher + " -t [teacherid]\n"+
         "" + Command.createcourse + " -c [courseabrv] -cn [coursename] -sl [size_limit] -t [teacherid]\n"+
         "" + Command.deletecourse + " -c [courseabrv]\n"+
         "");
    }
}
