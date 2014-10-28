
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
** regfuncs.c
**
**  Created on: 19 Oct, 2011
**      
**
**
**	Description:
**
**
************************************************************************/

#include "helper.h"

void close_fp(FILE *fp, const int **bad){

	if(bad == NULL){
		fclose(fp);  //STONESOUP:TRIGGER_POINT
	}else{
		//fclose(fp); //STONESOUP:TRIGGER_POINT
	}
}

/* end of file */
