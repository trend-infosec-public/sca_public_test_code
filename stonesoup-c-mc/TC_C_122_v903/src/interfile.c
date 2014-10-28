
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


#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "clipboard.h"
#include "interfile.h"

void do_weakness(char* k)
{
	char *clipboard = getClipboard();	//STONESOUP:SOURCE_TAINT:CLIPBOARD

	if(!clipboard)
	{
		fprintf(stderr, "Error: No Clipboard found!\n");
		exit(1);
	}

	strcpy(k, clipboard);	//STONESOUP:INTERACTION_POINT	//STONESOUP:CROSSOVER_POINT	//STONESOUP:TRIGGER_POINT

	clipboard_cleanup(clipboard);
}
