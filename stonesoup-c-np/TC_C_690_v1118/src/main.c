
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
**  
**  Date: 21 Sep 11
**
**  Revision History
**  Date      Change
**  21 Sep 11 Initial 
**
**l
**  Main will run as a simulated server, to exit the user needs
**  to supply a CTRL-C. The user can connect as many times as
**  needed. This is based off the TFPT server, added a function
**  call to get the local system time and convert it into different
**  timezones.
**
**  Program outputs a file with 5 Air Base times around the world
**  GMT - Greenich Man Time(Milden Hall AB)	  0
**  EST - Eastern Standard Time(Langley AFB)- 5
**  PST - Pacific Standard Time(March AB) 	- 8
**  CET - Central European Time(Ramstein AB)+ 1
**  KST - Korea Standard Time(Seoul)   	 	+ 9
**
**
** Error: Segmentation fault happens in "handleRequest(SOCKET clientSock,
**        SOCKET serverSock)" by commenting out FILE *fp ; //= NULL;
**        and the input is /etc/<returnfilename> or <returnfilename":"txt>
**        a filename with a ":" colon in it.
**        Program crashes when handle request closes the (fp);
**        To change input the user needs to manual change in registry.conf.
**
** BASE PROGRAM: TC_C_690_base5
**
** STONESOUP Weakness Class: NULL Pointer Errors
** CWE ID: CWE-690
** Variant Spreadsheet Rev #: ###
** Variant Spreadsheet ID: 1118
**
** Variant Features:
**		SOURCE_TAINT:SOCKET
**		DATA_TYPE:UNION
**		CONTROL_FLOW:INTERFILE_2
**		DATA_FLOW:ADDRESS_AS_A_CONSTANT
**
** Initial Testing: (x means yes, - means no)
**   Tested in MS Windows XP 32bit        -
**   Tested in MS Windows 7  64bit        x
**   Tested in Ubuntu10_10 Linux 32bit    x
**   Tested in Ubuntu10_10 Linux 64bit    -
**
** Workflow:
**   Created: 09/14/2011
**   1st Vett: <programmer> on <date>
**   2nd Vett: <peer> on <date>
**   3rd Vett: <teamleader> on <date>
**   4th Vett: Fortify Issues Fixed on <date>
**   5th Vett: Tested in Harness on <date>
**
** I/0 Pairs:
**   Good: 1st Set: somefile1.txt
**         2nd Set: somefile2.txt
**         3rd Set: somefile3.txt
**         4th Set:
**         5th Set:
**    Bad: 1st Set: somefile4:txt
**    	   2nd Set: somefile5&txt
**
** How program works:
** 	 The program is run with SetupTFTPSocketC -q -h localhost <filename>
** 	 The program will use the filename as the return file name. The file
**   with the ":" was taken out because it messed up SVN.
************************************************************************/
//Will not compile in Linux under C90, C99 or ANSI
#undef __STRICT_ANSI__
#undef _ISOC99_SOURCE

#include "helper.h"

/* Main will loop and run as a service as long is there is no errro.*/
int main(void)
{
	int r;

	SOCKET serverSock = INVALID_SOCKET;
	SOCKET clientSock = INVALID_SOCKET;

	do{
		if (createSocketAndConnect(&serverSock, &clientSock) == GET_TIME_ERROR) {
			return GET_TIME_ERROR;
		}
		if ((r = handleRequest(clientSock, serverSock)) == GET_TIME_ERROR) { 	//STONESOUP:CONTROL_FLOW:INTERFILE_2
			sendError(clientSock);
		}

	}while( r != GET_TIME_ERROR);

	return(r);
}

/** @brief Function creates a server side socket, binds to port ????, and
 * 		   accepts an incoming connection.
 *
 *  @param[out] serverSock is the server socket that will be created.
 *  @param[out] clientSock is the client socket that will be used to connect
 *  			to a client.
 *  @return Function returns GET_TIME_SUCCESS on success and GET_TIME_ERROR on error.
 */
int createSocketAndConnect(SOCKET *serverSock, SOCKET *clientSock)
{
	struct addrinfo hints;
	struct addrinfo *serverAddr = NULL;
	int opts = 1;

#ifdef _WIN32
	int wsaError = -1;
	WSADATA wsaData;
#endif

	/*Initialize socket values*/
	(*serverSock) = (*clientSock) = 0;
	memset(&hints, '\0', sizeof(hints));
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;
    hints.ai_flags = AI_PASSIVE;

#ifdef _WIN32
	/*Initialize Windows sockets*/
	if ((wsaError = WSAStartup(MAKEWORD(2,2), &wsaData)) != GET_TIME_SUCCESS) {
		fprintf(stderr, "WSAStartup failed with error: %d\n", WSAGetLastError());
		goto ERROR_OUT;
	}
#endif

    /*Get address for binding*/
    if (getaddrinfo(NULL, GET_TIME_PORT, &hints, &serverAddr) != GET_TIME_SUCCESS ) {
    	fprintf(stderr, "getaddrinfo failed with error: %d\n", WSAGetLastError());
        goto ERROR_OUT;
    }

    /*Create server socket*/
    if (((*serverSock) = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP))	//STONESOUP:INTERACTION_POINT	//STONESOUP:SOURCE_TAINT:SOCKET
    		== INVALID_SOCKET) {
        fprintf(stderr, "socket failed with error: %d\n", WSAGetLastError());
        goto ERROR_OUT;
    }

    /*Allow this port to be reused*/
    if (setsockopt((*serverSock), SOL_SOCKET, SO_REUSEADDR, (char *)&opts, sizeof(opts))
    		!= GET_TIME_SUCCESS) {
    	fprintf(stderr, "setsockopt failed with error: %d\n", WSAGetLastError());
    	goto ERROR_OUT;
    }

    /*Bind to the specified port and address*/
    if (bind((*serverSock), serverAddr->ai_addr, (int)serverAddr->ai_addrlen)
    		== SOCKET_ERROR) {
        fprintf(stderr, "bind failed with error: %d\n", WSAGetLastError());
        goto ERROR_OUT;
    }

    /*Listen for incoming connections*/
    if (listen((*serverSock), MAX_CONNECTIONS) == SOCKET_ERROR) {
    	fprintf(stderr, "listen failed with error: %d\n", WSAGetLastError());
    	goto ERROR_OUT;
    }

    fprintf(stdout, "Socket created.  Ready to accept.\n");
    fflush(stdout);

    /*Accept incoming client connection and create client socket*/
    if (((*clientSock) = accept((*serverSock), NULL, NULL))
    		== INVALID_SOCKET) {
    	fprintf(stderr, "accept failed with error: %d\n", WSAGetLastError());
    	goto ERROR_OUT;
    }

    fprintf(stdout, "Connected!\n");
    fflush(stdout);

    /*Free the server address information*/
    if (serverAddr) freeaddrinfo(serverAddr);

    return GET_TIME_SUCCESS;

ERROR_OUT:

	if (serverAddr) freeaddrinfo(serverAddr);

#ifdef _WIN32
	if (*serverSock) closesocket(*serverSock);
	if (!wsaError) WSACleanup();
#else
	if (*serverSock) close(*serverSock);
#endif

	return GET_TIME_ERROR;
}


