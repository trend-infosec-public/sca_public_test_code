

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
*		DATA_TYPE:WIDE_CHARACTER
*		DATA_FLOW:ADDRESS_AS_A_CONSTANT
*		CONTROL_FLOW:POINTER_TO_FUNCTION
*
* Initial Testing: (x means yes, - means no)
*   Tested in MS Windows XP 32bit        x
*   Tested in Ubuntu10_10 Linux 32bit    -
*
*********************************************/

package stonesoup;
import studentmodel.Config;

//The main class for the student db app.
public class SimpleStudentDb
{
    /**
     * @param args the command line arguments
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception //STONESOUP:SOURCE_TAINT:COMMAND_LINE
    {
        final Config config = new Config();
		final ProcessExecuteCmds pscmd = new ProcessExecuteCmds();

    		pscmd.processCommandLine(args, config);//STONESOUP:INTERACTION_POINT
    		pscmd.execute(config);//STONESOUP:CROSSOVER_POINT

    }

}
