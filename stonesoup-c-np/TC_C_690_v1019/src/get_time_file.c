
static char data_rights_legend [ ] = 
  "This software (or technical data) was produced for the U. S. \
   Government under contract 2009-0917826-016 and is subject to \
   the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007). \
\
   (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.";




/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/************************************************************************
**
**
**  Created on: Oct 28, 2011
**      
**
**
**	Description:
**
************************************************************************/

#include "get_time_file.h"

/** @brief This function gets the system time and populates it to a
 *         readable format for local users.
 *
 * 	@pre clientSock should be an already connected client.
 * 	@param[in] clientSock is a connected client socket.
 * 	@return Function returns GET_TIME_SUCCESS on success and GET_TIME_ERROR on error.
 */
int gettime(char *timeReturnFileName )
{

	time_t rawtime;
	struct tm * ptm;
	FILE * temp ;
	int r;

	/*Get time and write to file */
	time (&rawtime);
	if ((ptm = gmtime ( &rawtime)) == NULL ){
		fprintf(stderr, "Could not create return file\n");
		return GET_TIME_ERROR;
	}else
	{
		/*Create return file for time */
		if (( temp = fopen(timeReturnFileName, "w")) == NULL) { //STONESOUP:INTERACTION_POINT //STONESOUP:CROSSOVER_POINT //STONESOUP:SOURCE_TAINT:STDIN
			fprintf(stderr, "Could not open return file \n");
			return GET_TIME_ERROR;
		}


		/*Print to file*/
		if (( r = fprintf( temp,"Time is:\n")) <= GET_TIME_ERROR ) {
			fprintf(stderr, "Could write to file \n");
			fclose(temp);
			return GET_TIME_ERROR;
		}
/*
		if((r = fprintf( temp,"Seoul:		%2d:%02d\n", (ptm->tm_hour+KST)%24, ptm->tm_min))<= GET_TIME_ERROR ){
			fprintf(stderr, "Could write to file \n");
			fclose(temp);
			return GET_TIME_ERROR;
		}
		if((r = fprintf( temp,"Ramsten AB:	%2d:%02d\n", (ptm->tm_hour+CET)%24, ptm->tm_min))<= GET_TIME_ERROR ){
			fprintf(stderr, "Could write to file \n");
			fclose(temp);
			return GET_TIME_ERROR;
		}
		if((r = fprintf( temp,"Milden Hall AB:	%2d:%02d\n", (ptm->tm_hour+GMT)%24, ptm->tm_min))<= GET_TIME_ERROR ){
			fprintf(stderr, "Could write to file \n");
			fclose(temp);
			return GET_TIME_ERROR;
		}
		if((r = fprintf( temp,"Langley AFB:	%2d:%02d\n", (ptm->tm_hour+ EST)%24, ptm->tm_min))<= GET_TIME_ERROR ){
			fprintf(stderr, "Could write to file \n");
			fclose(temp);
			return GET_TIME_ERROR;
		}
		if((r = fprintf( temp,"March AB:	%2d:%02d\n", (ptm->tm_hour+ PST)%24, ptm->tm_min))<= GET_TIME_ERROR ){
			fprintf(stderr, "Could write to file \n");
			fclose(temp);
			return GET_TIME_ERROR;
		}
*/
	}
	if (temp) fclose(temp);

	return GET_TIME_SUCCESS;
}

/* End of file */
