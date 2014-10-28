
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

/***********************************************************************
**
**
** sharemem.c
**
**  Created on: Jun 9, 2011
**      
**
**  Rewritten on Apr 10, 2012
**  	by 
**
**  Description
**
**  openshmem() is called to set up or attach the shared memory
**  	So that multiple instances may operate simultaneously, a unique
**  	Lock filename must be supplied by the caller as well as a unique key.
**  	The size of the memory request is required to be greater the 1KB.
**
**  closeshmem()
**  	This function releases this side of the programs shared memory back to
**  	the OS. The function also releases and memory allocated by getshmem().
**
**  setshmem()
**		The argument to this function is a zero-terminated string which must be
**		smaller than the reserved block of shared memory. The string is copied into
**		memory, then the lock file is created. A wait loop is entered until the
**		lock file is deleted by the reading program.
**
**	getshmem()
**		A wait for the lock file to appear is entered by this program before
**		attempting to read from shared memory. Once the lock file appears, the
**		contents of shared memory, which are expected to be a zero-terminated
**		string, are copied into and allocated buffer. Once the buffer are filled
**		the lock file is deleted so the sender program may continue. The allocated
**		memory is returned to the system by subsequent calls to getshmem(),
**		setshmem(), and/or closeshmem(). The allocated memory should NOT be freed
**		externally to the program as this will result in a double free problem.
**
************************************************************************/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>

#ifdef _WIN32
#include <Windows.h>
#include <conio.h>
#include <io.h>
#else
#define __USE_XOPEN		// Need this for the shm.h include
#include <unistd.h>
#include <sys/shm.h>
#include <sys/types.h>
#include <sys/ipc.h>
#endif

#include "sharemem.h"

#ifdef _WIN32
HANDLE hMapFile;	/* Windows handle */
#else
int shmid;	/* Linux ID */
#endif

char *shm;	/* Pointer to shared memory */
char *rdmem;
char lockfname[256];	/* Save lock filename for later use */
unsigned int key;	/* Save key for later */
int memsz;	/* Requested memory size */

int openshmem(char *lockfilname, unsigned int keyin, int sz)
{
	rdmem = NULL;	/* Used to save pointers to and release allocated memory */

	/* Check the input parameters for saneness */
	if ((lockfilname == NULL) || (*lockfilname == 0) || (strlen(lockfilname) >= sizeof(lockfname)))
	{
		fprintf(stderr, "Invalid lock name specified.\n");
		return 1;
	}

	if (keyin == 0)
	{
		fprintf(stderr, "Key value must not be 0.\n");
		return 1;
	}

	if (sz < 1023)
	{
		fprintf(stderr, "Memory size must be greater than 1023.\n");
		return 1;
	}

	strcpy(lockfname, lockfilname);
	key = keyin;
	memsz = sz;

	/* Set up and open the shared memory */
#ifdef _WIN32
	hMapFile = CreateFileMapping(			//create a new file mapping
	                 INVALID_HANDLE_VALUE,    // use paging file
	                 NULL,                    // default security
	                 PAGE_READWRITE,          // read/write access
	                 0,                       // maximum object size (high-order DWORD)
	                 memsz,                	  // maximum object size (low-order DWORD)
	                 "Global\\ShareString");// name of mapping object
	if (hMapFile == NULL)
	{
		fprintf(stderr, "Could not create file mapping object (%d).\n", (int)GetLastError());
		return 1;
	}

	shm = (char*) MapViewOfFile(hMapFile,   // handle to map object
		                        FILE_MAP_ALL_ACCESS, // read/write permission
		                        0,
		                        0,
		                        memsz);
	if (shm == NULL)
	{
	   fprintf(stderr, "Could not map view of file (%d).\n",(int)GetLastError());
	   CloseHandle(hMapFile);
	   return 1;
	}
#else
	shmid = shmget((key_t)key, memsz, IPC_CREAT | 0666);
	if (shmid == -1)
	{
		perror("Error: Could not create shared memory segment.");
		return 1;
	}

	shm = shmat(shmid, NULL, 0);
	if (shm == NULL)
	{
		perror("Error: Could not attach memory segment.");
		shmctl(shmid,IPC_RMID,NULL);
		return 1;
	}
#endif

	return 0;
}

int closeshmem(void)
{
	if (rdmem)
	{
		free(rdmem);
		rdmem = NULL;
	}
#ifdef _WIN32
	if(!UnmapViewOfFile(shm))
	{
		perror("Error: Can't unmap memory.\n");
		return 1;
	}
	if (!CloseHandle(hMapFile))
	{
		perror("Error: Can't release memory handle.\n");
		return 1;
	}
#else
	if (shm)
	{
		shmdt(shm);
	}

	if (shmid >= 0)
	{
		shmctl(shmid, IPC_RMID, NULL);
		shmid = -1;
	}
#endif

	return 0;
}

int setshmem(char *str)
{
	if (rdmem)
	{
		free(rdmem);
		rdmem = NULL;
	}

	if (str == NULL)
	{
		fprintf(stderr, "Error: No string specified for memory write\n");
		return 1;
	}

	if (strlen(str) >= memsz)
	{
		fprintf(stderr, "Error: String larger than memory size\n");
		return 1;
	}

	/* Actually write string to memory */
#ifdef _WIN32
	 memcpy((void*)shm, str, strlen(str));
#else
	strcpy(shm, str);
#endif

	/* Create the lockfile as a signal to the server... */
	fprintf(stderr, "Creating shared memory lock file\n");
	FILE *fp;
	if ((fp = fopen(lockfname, "w")) == NULL)
	{
		fprintf(stderr, "Error: Could not open lock file '%s'\n", lockfname);
		return 1;
	}

	if (fclose(fp))
	{
		fprintf(stderr, "Error: Could not close lock file '%s'\n", lockfname);
		return 1;
	}
	fprintf(stderr, "Waiting for shared memory lock file to go away\n");

	/* Wait for it to disappear, indicating the server has responded. */
	/* check for lockfile, if it exists then there is a request */
	while (access(lockfname, F_OK) == 0)
	{
#ifdef _WIN32
		_sleep(1);
#else
		sleep(1);
#endif
	}
	fprintf(stderr, "Lock file '%s' gone\n", lockfname);

	return 0;
}

char *getshmem(void)
{
	if (rdmem)
	{
		free(rdmem);
		rdmem = NULL;
	}

	/* Wait for the client lock file */
	fprintf(stderr, "Wait for lock file '%s'\n", lockfname);
  while(access(lockfname, F_OK) != 0)
	{
#ifdef _WIN32
		_sleep(1);
#else
		sleep(1);
#endif
	}
	fprintf(stderr, "Lock file found\n");

	if ((rdmem = malloc(strlen(shm) + 1)) == NULL)
	{
		fprintf(stderr, "Error: Could not malloc %d bytes of memory\n", strlen(shm) + 1);
		return NULL;
	}
	strcpy(rdmem, shm);

	remove(lockfname);
	fprintf(stderr, "Lock file '%s' deleted\n", lockfname);

	return rdmem;
}

/* End of file */
