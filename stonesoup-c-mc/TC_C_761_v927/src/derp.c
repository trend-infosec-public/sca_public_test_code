
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

#include "derp.h"
#include <string.h>

int isAllInts(char **k)
{
	int i = 0;
	int v[5];
	int x = 1;
	int* y = &x;
	char* start = *k;
	int k_len = strlen(*k);
	for(i = 0; i < k_len; i++)
	{
		v[*y] = *((*k)++); //printf("Value: %c\n", v);
		if(v[*y] < 48 || v[*y] > 57)
			return 0;
	}

	*k = start;
	return 1;
}
