
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
 * interfile.c
 *
 *  Created on: Dec 1, 2011
 *      
 */

#include <limits.h>
#include <errno.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <malloc.h>

#include "interfile.h"

void interfile (FILE *infil, FILE *otfil, const char *otflnm, void *preamble, const char *errmsg, unsigned int i1, char *s1, unsigned int i2, char *s2)
{
	closeuperr (infil, otfil, otflnm, preamble, errmsg, i1, s1, i2, s2);
}

/* End of file */
