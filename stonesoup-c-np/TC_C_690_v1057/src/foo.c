
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
**   TC_C_690_v1005
**
**  (x means yes, - means no)
** Tested in MS Windows XP 32bit    x
** Tested in MS Windows 7    64bit    -
** Tested in RH Linux 32bit                  -
** Tested in RH Linux 64bit                  -
**
**  Revision History
**  Date      Change
**  Oct 13, 2011   Created
************************************************************************/
#include "bar.h"


int* foobar1(char filename[])
{
	return foobar2(filename);
}
