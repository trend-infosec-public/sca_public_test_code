
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

/**************************************************************/
/************************************************************************
**
**
**  
**  Date: 9/14/2011
**
************************************************************************/

#include <stdio.h>

#include "interfile.h"

// Load image by reading through file and loading into buffer
unsigned int interfile(unsigned short *s)
{
	unsigned char *s1 = (unsigned char *)s;
	unsigned int len = 0;
	while (feof(stdin) == 0)
	{
		if (fread(s1, 1, 1, stdin) != 1)
		{
				break;
		}
		len++;
		s1++;
	}

	return(len);
}

/* End of file */
