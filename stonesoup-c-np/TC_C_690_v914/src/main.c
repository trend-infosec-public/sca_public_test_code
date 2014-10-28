
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

/*******************************************
**
**
** 
** Date: <September 19, 2011>
**
**   TFTP Server
**  6/24/2011 Initial 
**
**  This program acts as a TFTP server with only the ability to send requested
**  files to connecting clients using a TCP connection.  The server can
**  only handle one request from one client before exiting.  Please note that
**  this program should only be used with the TFTPClientC program, it will
**  not work with commercial FTP or TFTP clients
**
**  First, the server creates a socket on port 69, the TFTP port, and listens
**  for an incoming connection.  Once connected, the server listens for a
**  request for a file.  The request should have the following format where
**  Opcode = 1, Mode = "NETASCII", and Filename will be the file requested.:
**
**              2 bytes     string    1 byte     string   1 byte
**            ------------------------------------------------
**           | Opcode |  Filename  |   0  |    Mode    |   0  |
**            ------------------------------------------------
**
** 	If the file exists in the working directory of the server, the server
**  will send the contents of the file with one or more packets of the
**  following format.  In this case, Opcode = 3, Block # will start at 1
**  and increment for every following packet, and every Data chunk will
**  be 512 bytes except for the last one.
**
**                    2 bytes     2 bytes      n bytes
**                   ----------------------------------
**                  | Opcode |   Block #  |   Data     |
**                   ----------------------------------
**
**  If the file does not exist or the path or request packet is in any
**  way invalid, the server will instead send an error packet with the
**  following format where Opcode = 5:
**
**                             2 bytes
**                             --------
**                            | Opcode |
**                             --------
**
** BASE PROGRAM: TC_C_690_base2
** This base test program alters the TFTPServer's function checkValidPacket to have return value of int* to allow for the NULL pointer
** to be returned. In this base, if the filename contains a '&' NULL will be returned and then dereferenced in a check.
**
**
** STONESOUP Weakness Class: NULL Pointer Errors
** CWE ID: CWE-690
** Variant Spreadsheet Rev #: 2
** Variant Spreadsheet ID: 914
**
**
** Variant Features:
**		SOURCE_TAINT:FILE_NAME
**		DATA_TYPE:FLOAT
**		CONTROL_FLOW:COUNT_CONTROLLED_LOOP
**		DATA_FLOW:ARRAY_INDEX_ARRAY_CONTENT_VALUE
**
** Initial Testing: (x means yes, - means no)
**   Tested in MS Windows XP 32bit        x
**   Tested in MS Windows 7  64bit        -
**   Tested in Ubuntu10_10 Linux 32bit    -
**   Tested in Ubuntu10_10 Linux 64bit    -
**
** Workflow:
**   Created:<data started>
**   1st Vett: <programmer> on <date>
**   2nd Vett: <peer> on <date>
**   3rd Vett: <teamleader> on <date>
**   4th Vett: Fortify Issues Fixed on <date>
**   5th Vett: Tested in Harness on <date>
**
**
 **	 I/0 Pairs:
 **   Good: 1st Set:
 **         2nd Set:
 **         3rd Set:
 **         4th Set:
 **         5th Set:
 **    Bad: 1st Set: io-TC_C_690_base2_bad.xml
 *****************************************************************************/
#undef __STRICT_ANSI__
#undef _ISOC99_SOURCE

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <stdarg.h>

//OS friendly -  should work on windows/unix systems
#ifdef _WIN32
//#include <WinBase.h>
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

#define TFTP_PORT "9080"
#define BUFFER_SIZE 516
#define TFTP_SUCCESS 0
#define TFTP_ERROR -1
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
int* checkValidPacket(char *buf, int bufSize, FILE **fp);
int sendFile(FILE *fp, SOCKET clientSock);
void sendError(SOCKET clientSock);
int errorCheck(int *s); // new function to dereference the function return pointer, s



int main(void)
{
	int r;

	SOCKET serverSock = INVALID_SOCKET;
	SOCKET clientSock = INVALID_SOCKET;

	do{
		if (createSocketAndConnect(&serverSock, &clientSock) == TFTP_ERROR) {
			return TFTP_ERROR;
		}
		if ((r = handleRequest(clientSock, serverSock)) == TFTP_ERROR) {
			sendError(clientSock);
		}

	}while( r != TFTP_ERROR);

	return(r);
}


/** @brief This function sends the contents of the requested file to clientSock.
 *
 * 	@param[in] fp is a file pointer to the requested file that should already be
 * 	           opened with read permissions.
 *  @param[in] clientSock is a socket connected to a client.
 *  @return Function returns TFTP_SUCCESS on success and TFTP_ERROR on error.
 */
int sendFile(FILE *fp, SOCKET clientSock)
{
	short opcode = DATA_CODE;
	char serverBuf[BUFFER_SIZE];
	unsigned short blocknum = 1;
	float bytesread, bytesSent; 	//STONESOUP:DATA_TYPE:FLOAT

	memset(serverBuf, '\0', BUFFER_SIZE);

	/*Set the opcode of the message*/
	opcode = htons(opcode);
	memcpy(serverBuf + OPCODE_OFFSET, &opcode, OPCODE_SIZE);

	/*Send data packets until there is no more data*/
	do {
		/*Set the block number of the message*/
		blocknum = htons(blocknum);
		memcpy(serverBuf + BLOCKNUM_OFFSET, &blocknum, BLOCKNUM_SIZE);

		/*Set the data portion of the message*/
		bytesread = fread(serverBuf + DATA_OFFSET, 1, DATA_SIZE, fp);
		if (ferror(fp) != TFTP_SUCCESS) {
			fprintf(stderr, "Read Error\n");
			return TFTP_ERROR;
		}
		/*Send the message*/
		bytesSent = send(clientSock, serverBuf,
				bytesread + OPCODE_SIZE + BLOCKNUM_SIZE, 0);
		if (bytesSent == TFTP_ERROR) {
			fprintf(stderr, "Send Error\n");
			return TFTP_ERROR;
		}
		/*Increment blocknum*/
		if (blocknum != MAX_USHORT)
			blocknum++;
		else
			return TFTP_ERROR;
	} while (bytesread == DATA_SIZE);

	return TFTP_SUCCESS;
}

/** @brief This function sends an error packet to a connected client.
 *
 * 	@pre clientSock should be an already connected client.
 * 	@param[in] clientSock is a connected client socket.
 * 	@return Function returns TFTP_SUCCESS on success and TFTP_ERROR on error.
 */
void sendError(SOCKET clientSock)
{
	char serverBuf[BUFFER_SIZE];
	short opcode = ERROR_CODE;
	int bytesSent;

	memset(serverBuf, '\0', BUFFER_SIZE);
	opcode = htons(opcode);
	memcpy(serverBuf + OPCODE_OFFSET, &opcode, OPCODE_SIZE);

	bytesSent = send(clientSock, serverBuf,
			OPCODE_SIZE, 0);

	if (bytesSent == TFTP_ERROR) {
		fprintf(stderr, "Send Error");
	}
}

/** @brief This function receives a request for a file from a client and
 *         calls the necessary function to send a response.
 *
 *  @param[in] clientSock should be an already connected client socket
 *  @param[in] serverSock should be the server's socket.
 *	@return Function returns TFTP_SUCCESS on success and TFTP_ERROR on error.
 */
int handleRequest(SOCKET clientSock, SOCKET serverSock)
{
	char clientBuf[BUFFER_SIZE];
	int recvSize;
	FILE *fp = NULL;
	int r;
	int *s;   //checkValidPacket now returns a pointer to int

	/*Receive the request*/
	recvSize = recv(clientSock, clientBuf, BUFFER_SIZE, 0); //STONESOUP:INTERACTION_POINT
	/*Validate the request*/
	s = checkValidPacket(clientBuf, recvSize, &fp);

	if (errorCheck(s) == TFTP_ERROR) { //check for tftp_error. If checkValidPacket returned NULL, passing it to errorCheck() will cause 476.
		sendError(clientSock);
		r = TFTP_ERROR;
	} else {
		/*Valid request, so send packet*/
		if ((r = sendFile(fp, clientSock)) == TFTP_ERROR) {
			sendError(clientSock);
		}
	}
	/*Clean up*/
#ifdef _WIN32
	closesocket(clientSock);
	closesocket(serverSock);
	WSACleanup();
#else
	close(clientSock);
	close(serverSock);
#endif

	if (fp) fclose(fp);
	return r;
}

/** @brief Function checks for a valid request packet and filename.
 * 	@param[in] buf is a buffer holding the received packet.
 * 	@param[in] bufSize is the length of the request packet.
 *  @param[out] fp is a file pointer which will be used to fopen the
 *  			requested file.
 *  @return Function returns TFTP_SUCCESS for a valid request and
 *  		TFTP_ERROR on an invalid request.
 */
int errorCheck(int *s){			// Passing a NULL pointer.
	if (*s == TFTP_ERROR) //STONESOUP:TRIGGER_POINT
		return TFTP_ERROR;
	else
		return TFTP_SUCCESS;
}
int* checkValidPacket(char *buf, int bufSize, FILE **fp)
{
	int* tftp_success;
	tftp_success = malloc(sizeof(int));
	*tftp_success = 0;

	int* tftp_error;
		tftp_error = malloc(sizeof(int));
		*tftp_error = -1;

	short opcode;
	char filename[FILENAME_SIZE];
	char nullByte1, nullByte2;
	char mode[MODE_SIZE + 1];
	int i;

	/*Check that packet is correct size*/
	if (bufSize != BUFFER_SIZE) {
		fprintf(stderr, "Invalid packet request size\n");
		return tftp_error;
	}

	/*Extract contents of incoming packet*/
	memcpy(&opcode, buf + OPCODE_OFFSET, OPCODE_SIZE);
	memcpy(filename, buf + FILENAME_OFFSET, FILENAME_SIZE);
	memcpy(&nullByte1, buf + NULL_BYTE_1_OFFSET, NULL_BYTE_1_SIZE);
	memcpy(mode, buf + MODE_OFFSET, MODE_SIZE);
	memcpy(&nullByte2, buf + NULL_BYTE_2_OFFSET, NULL_BYTE_2_SIZE);
	filename[FILENAME_SIZE - 1] = '\0';
	int foo[5];
	foo[3] = MODE_SIZE;
	mode[foo[3]] = '\0'; 	//STONESOUP:DATA_FLOW:ARRAY_INDEX_ARRAY_CONTENT_VALUE
	opcode = ntohs(opcode);

	/*Check for a valid opcode*/
	if (opcode != RRQ_CODE) {
		fprintf(stderr, "Invalid opcode\n");
		return tftp_error;
	}

	/*
	 * Check for a valid filename.  A valid filename contains no slash
	 * characters, colons, or double dots.  A valid filename should also
	 * correspond to an existing file in the working directory of the server.
	 */
	for (i = 0; i < FILENAME_SIZE; i++) {		//STONESOUP:CONTROL_FLOW:COUNT_CONTROLLED_LOOP
		if (filename[i] == '\0') break;
		if (filename[i] == '\\' || filename[i] == '/') {
			fprintf(stderr, "Invalid filename\n");
			return tftp_error;
		}
		if (filename[i] == ':') {		//STONESOUP:SOURCE_TAINT:FILE_NAME
			fprintf(stderr, "Invalid filename\n");
			return NULL; //STONESOUP:CROSSOVER_POINT
		}
		if (filename[i] == '&') {
			fprintf(stderr, "Invalid filename\n");
			return NULL;
		}
		if (filename[i] == '.' && filename[i + 1] == '.') {
			fprintf(stderr, "Invalid filename\n");
			return tftp_error;
		}
	}

	(*fp) = fopen(filename, "r");
	if (((*fp) = fopen(filename, "r")) == NULL) {
		fprintf(stderr, "No such file\n");
		return tftp_error;
	}

	/*Check that packet is well formed with null bytes in the correct places*/
	if (nullByte1 != '\0' || nullByte2 != '\0') {
		fprintf(stderr, "Invalid packet");
		return tftp_error;
	}

	/*Check that the mode was specified to be NETASCII*/
	if (strncmp(mode, "NETASCII", MODE_SIZE)) {
		fprintf(stderr, "Invalid mode");
		return tftp_error;
	}

	/*If the code reaches this point, the packet is valid*/
	return tftp_success;
}

/** @brief Function creates a server side socket, binds to port 69, and
 * 		   accepts an incoming connection.
 *
 *  @param[out] serverSock is the server socket that will be created.
 *  @param[out] clientSock is the client socket that will be used to connect
 *  			to a client.
 *  @return Function returns TFTP_SUCCESS on success and TFTP_ERROR on error.
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
	if ((wsaError = WSAStartup(MAKEWORD(2,2), &wsaData)) != TFTP_SUCCESS) {
		fprintf(stderr, "WSAStartup failed with error: %d\n", WSAGetLastError());
		goto ERROR_OUT;
	}
#endif
    /*Get address for binding*/
    if (getaddrinfo(NULL, TFTP_PORT, &hints, &serverAddr) != TFTP_SUCCESS ) {
    	fprintf(stderr, "getaddrinfo failed with error: %d\n", WSAGetLastError());
        goto ERROR_OUT;
    }

    /*Create server socket*/
    if (((*serverSock) = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP))
    		== INVALID_SOCKET) {
        fprintf(stderr, "socket failed with error: %d\n", WSAGetLastError());
        goto ERROR_OUT;
    }

    /*Allow this port to be reused*/
    if (setsockopt((*serverSock), SOL_SOCKET, SO_REUSEADDR, (char *)&opts, sizeof(opts))
    		!= TFTP_SUCCESS) {
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

    fprintf(stderr, "Socket created.  Ready to accept.\n");

    /*Accept incoming client connection and create client socket*/
    if (((*clientSock) = accept((*serverSock), NULL, NULL))
    		== INVALID_SOCKET) {
    	fprintf(stderr, "accept failed with error: %d\n", WSAGetLastError());
    	goto ERROR_OUT;
    }

    fprintf(stderr, "Connected!\n");

    /*Free the server address information*/
    freeaddrinfo(serverAddr);
    return TFTP_SUCCESS;

	ERROR_OUT:
	if (serverAddr) freeaddrinfo(serverAddr);
#ifdef _WIN32
	if (*serverSock) closesocket(*serverSock);
	if (!wsaError) WSACleanup();
#else
	if (*serverSock) close(*serverSock);
#endif
	return TFTP_ERROR;
}
