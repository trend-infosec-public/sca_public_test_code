

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/************************************************************************
**
** regfuncs.c
**
**  Created on:
**
**      MUST COMPILE WITH LINKER FLAGS -L/usr/X11R6/lib -lX11
**
**	Description:

**
************************************************************************/
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <stdarg.h>
#include <time.h>


//OS friendly -  should work on windows/unix systems
#ifdef _WIN32
	#include <windef.h>
	#include <WinBase.h>
	#include <Windows.h>
	#include <winsock2.h>
	#include <ws2tcpip.h>
#else
	#include <unistd.h>
	#include <sys/types.h>
	#include <sys/socket.h>
	#include <netinet/in.h>
	#include <arpa/inet.h>
	#include <netdb.h>
#endif


#define KST (+9)
#define CET (+1)
#define GMT (0)
#define EST (-5)
#define PST (-8)

#define GET_TIME_PORT "9080"
#define BUFFER_SIZE 516
#define GET_TIME_SUCCESS 0
#define GET_TIME_ERROR -1
#define GET_TIME_ERROR_1 1
#define MAX_CONNECTIONS 1
#define RRQ_CODE 1
#define DATA_CODE 3
#define ERROR_CODE 5
#define OPCODE_OFFSET 0
#define OPCODE_SIZE 2
#define FILENAME_OFFSET 2
#define FILENAME_SIZE 256
#define NULL_BYTE_1_OFFSET 258
#define NULL_BYTE_1_SIZE 1
#define MODE_OFFSET 259
#define MODE_SIZE 8
#define NULL_BYTE_2_OFFSET 267
#define NULL_BYTE_2_SIZE 1
#define BLOCKNUM_OFFSET 2
#define BLOCKNUM_SIZE 2
#define DATA_OFFSET 4
#define DATA_SIZE 512
#define MAX_USHORT 65535
#define ERROR_CODE_SIZE 2

#ifdef _WIN32

#else
	#define SOCKET_ERROR -1
	#define INVALID_SOCKET -1
	#define SOCKET int
	#define WSAGetLastError() errno
#endif

#ifdef _WIN32
int WSAAPI getaddrinfo(const char*,const char*,const struct addrinfo*, struct addrinfo**);
void WSAAPI freeaddrinfo (struct addrinfo*);
#endif

int createSocketAndConnect(SOCKET *serverSock, SOCKET *clientSock);
int handleRequest(SOCKET clientSock, SOCKET serverSock);
int checkValidPacket(char *buf, int bufSize, FILE **fp);
int sendFile(FILE *fp, SOCKET clientSock);
int gettime(char *timeReturnFileName);
void sendError(SOCKET clientSock);

enum LOCAL_SIZES
{
	zero = 0,
	medium = 256,
	large = 516,
}  size;



/* End of file */
