
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
**	Interfile
**
************************************************************************/

// Won't compile in Linux under C90, C99, or ANSI
#undef __STRICT_ANSI__
#undef _ISOC99_SOURCE

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <stdarg.h>

//IMPORTANT you must compile with -lws2_32 included or else you will get tons of errors

//OS friendly -	should work on windows/unix systems
#ifdef _WIN32
#include <windef.h>
#include <winbase.h>
#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>
#include <WinDNS.h>
#else	/* Linux */
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#endif

#include "interfile.h"

#ifdef _WIN32
int WSAAPI getnameinfo(const struct sockaddr FAR *sa,socklen_t salen,char FAR *host,DWORD hostlen,char FAR *serv,DWORD servlen,int flags);
int WSAAPI getaddrinfo(const char*,const char*,const struct addrinfo*, struct addrinfo**);
void WSAAPI freeaddrinfo (struct addrinfo*);
#endif

// Reverse DNS
int ReverseDns(char *addr, char *clientname, int clientlen)	//STONESOUP:SOURCE_TAINT:REVERSE_DNS_LOOKUPS
{
	char nm[200];
	int i;
	struct sockaddr_in siadr;
	siadr.sin_family = AF_INET;
	siadr.sin_addr.s_addr = inet_addr(addr);

	int retval = getnameinfo((struct sockaddr *)&siadr,sizeof(struct sockaddr),nm,sizeof(nm),NULL,0,0);

	if (retval != 0)
	{
		fprintf(stderr, "Could not get DNS info\n");
		*clientname = 0;
		return(retval);
	}

	i = strlen(nm);	// STONESOUP:INTERACTION_POINT // STONESOUP:CROSSOVER_POINT
	char *s = clientname + clientlen;
	enum en { a1 = 1, b2 = 2, c3 = 3 };	// STONESOUP:DATA_TYPE:ENUM

	while(i >= 0)
	{
		char *a[4];
		a[b2] = nm;
		char *b = a[b2];
		*(--s) = b[i--]; 	// STONESOUP:TRIGGER_POINT	// STONESOUP:DATA_FLOW:ADDRESS_ALIASING_2
	}

	return (retval);
}

/* End of file */
