
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
**  
**  Date: Oct 13, 2011
**
**  Spreadsheet Rev #: TODO<Spreadsheet rev # here - 1 for first edition, 2 
**  for second edition, etc.>
**  CWE #: TC_C_690_v1005
**  Spreadsheet Variant: TODO<Paste the spreadsheet variant row here>
**
**		SOURCE_TAINT:TC_C_690_V1005
**
**		DATA_TYPE:(X
**		CONTROL_FLOW:TESTED
**		DATA_FLOW:TESTED
** Tested in RH Linux 32bit                  -
** Tested in RH Linux 64bit                  -
**
**  Revision History
**  Date      Change
**  Oct 13, 2011   Created
************************************************************************/
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <stdarg.h>



int foobar2(char filename[]){
	int i;
	for (i = 0; i < 256; i++) {
		if (filename[i] == '\0') break;
		if (filename[i] == '\\' || filename[i] == '/') {
			fprintf(stderr, "Invalid filename\n");
			return -1;
		}
		if (filename[i] == ':') {
			fprintf(stderr, "Invalid filename\n");
			return -1;
		}
		if (filename[i] == '.' && filename[i + 1] == '.') {
			fprintf(stderr, "Invalid filename\n");
			return -1;
		}
	}
	return 0;
}



