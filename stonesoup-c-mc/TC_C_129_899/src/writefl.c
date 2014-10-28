
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

/*
 * writefl.c
 *
 *  Created on: Oct 13, 2011
 *      
 */
#include <stdio.h>
#include <string.h>

int writeFLe(int i, int index, int end, char *newoutput, int *sequence, FILE *otfil){
	for(i = 1; (index+i)<=end; i++){
		fprintf(otfil, "Variable at %d is %d\n", (int)(index+i), (int)sequence[index+i]);  //STONESOUP:TRIGGER_POINT
	}
	return 1;
}
