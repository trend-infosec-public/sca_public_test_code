
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
realpath() Win32 implementation
By 
*/
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

#ifdef _WIN32
#include <windows.h>
#include <tchar.h>
#endif

#ifdef __linux__
#include <limits.h>
#include <stdlib.h>
#include <errno.h>
#endif

#define BUFSIZE 4096

char pathgen(char *path, char *resolved_path, int rpsz)
{
#ifdef _WIN32
	DWORD  retval=0;
	TCHAR** lppPart={NULL};

  // Retrieve the full path name for a file.
  // The file does not need to exist.
	retval = GetFullPathName(path, rpsz, resolved_path, lppPart);

	if (retval == 0)
	{
		// Handle an error condition.
		printf ("GetFullPathName failed \n");
	}
	else
	{
		if (lppPart != NULL && *lppPart != 0)
		{
			_tprintf(TEXT("The final component in the path name is:  %s\n"), *lppPart);
		}
	}

#endif
#ifdef __linux__
	realpath(path, resolved_path);
#endif
	printf("The full path name is: %s\n", resolved_path);

	return *resolved_path;
}

int createDirectory(char *directoryname)
{
	struct stat st;
	if ((stat(directoryname, &st) == 0) && S_ISDIR(st.st_mode))
	{
		printf ("Directory Already Exists!\n");
		return 1;
	}

#ifdef _WIN32
	// Create a long directory name for use with the next two examples.
	if (CreateDirectory(directoryname, NULL) == 0)
	{
		// Handle an error condition.
		printf ("\nCreateDirectory failed (%lu)\n", GetLastError());
		return 0;
	}
#endif
#ifdef __linux__
	int res = mkdir(directoryname, S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH);
	if(res == -1){
		printf("\nCreate Directory failed: %s\n", (char *)strerror(errno));
		return 0;
	}
#endif
	else
	{
		printf("\nDirectory created.\n");
	}

	return 1;
}




