
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
**  Date: 8/9/2011
**
**  Spreadsheet Rev #:
**  CWE #: 415
**  Spreadsheet Variant:1057
**		SOURCE_TAINT:CLIPBOARD
**		DATA_TYPE:ARRAY_LENGTH_NONLINEAR_EXPRESSION
**		CONTROL_FLOW:INTERFILE_2
**		DATA_FLOW:INDEX_ALIAS_2
**
**  (x means yes, - means no)
** Tested in MS Windows XP 32bit    		 -
** Tested in MS Windows 7    64bit    		 x
** Tested in RH Linux 32bit                  -
** Tested in RH Linux 64bit                  -
**
**  Revision History
**  Date      Change
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

#include "clipboard.h"
#include "interfile_1.h"

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
#ifndef _WIN32
#define SOCKET_ERROR -1
#define INVALID_SOCKET -1
#define SOCKET int
#define WSAGetLastError() errno
#endif

#ifdef _WIN32
int WSAAPI getnameinfo(const struct sockaddr FAR *sa,socklen_t salen,char FAR *host,DWORD hostlen,char FAR *serv,DWORD servlen,int flags);
int WSAAPI getaddrinfo(const char*,const char*,const struct addrinfo*, struct addrinfo**);
void WSAAPI freeaddrinfo (struct addrinfo*);
#endif
int createSocketAndConnect(SOCKET *serverSock, SOCKET *clientSock);
int handleRequest(SOCKET clientSock, SOCKET serverSock, char *filename);
int checkValidPacket(char *buf, int bufSize, FILE **fp, char *filename);
int sendFile(FILE *fp, SOCKET clientSock);
void sendError(SOCKET clientSock);

#define PRT_ERR(s...) { fprintf(stdout, s); fflush(stdout); }

int main(void)
{
	char filename[FILENAME_SIZE];
	SOCKET serverSock = INVALID_SOCKET;
	SOCKET clientSock = INVALID_SOCKET;

//	printf("MALLOC_CHECK_ = '%s'\n",getenv ("MALLOC_CHECK_"));

	char* clipboard;
	if ((clipboard = getClipboard()) == NULL)	//STONESOUP:SOURCE_TAINT:CLIPBOARD
	{
		fprintf(stderr, "Clipboard is not available\n");
		return TFTP_ERROR;
	}

	strncpy(filename, clipboard, sizeof(filename) - 1);
	filename[FILENAME_SIZE - 1] = '\0';
	clipboard_cleanup(clipboard);

	if (createSocketAndConnect(&serverSock, &clientSock) == TFTP_ERROR) {
		return TFTP_ERROR;
	}

	return handleRequest(clientSock, serverSock, filename);
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
	char *serverBuf;
	char tmpBuf[(BUFFER_SIZE * BUFFER_SIZE) / 516];	//STONESOUP:DATA_TYPE:ARRAY_LENGTH_NONLINEAR_EXPRESSION
	unsigned short blocknum = 1;
	int bytesread, bytesSent, totbytesread = 0;

	//Error checking malloc
	if ((serverBuf = (char *) malloc((BUFFER_SIZE * BUFFER_SIZE) / 516)) == NULL)
	{
		fprintf(stderr, "Malloc Error\n");
		return TFTP_ERROR;
	}

	memset(serverBuf, '\0', BUFFER_SIZE);

	int end = BUFFER_SIZE-1;
	int* end_a = &end;
	int** end_b = &end_a;
	serverBuf[**end_b] = '\0';	//STONESOUP:DATA_FLOW:INDEX_ALIAS_2

	/*Set the opcode of the message*/
	opcode = htons(opcode);
	memcpy(serverBuf + OPCODE_OFFSET, &opcode, OPCODE_SIZE);
	do_something(tmpBuf, serverBuf);	//STONESOUP:CONTROL_FLOW:INTERFILE_2

	/*Send data packets until there is no more data*/
	do {
		/*Set the block number of the message*/
		blocknum = htons(blocknum);
		memcpy(tmpBuf + BLOCKNUM_OFFSET, &blocknum, BLOCKNUM_SIZE);

		/*Set the data portion of the message*/
		bytesread = fread(tmpBuf + DATA_OFFSET, 1, DATA_SIZE, fp);
		if (ferror(fp) != TFTP_SUCCESS) {
			free(serverBuf);
			fprintf(stderr, "Read Error\n");
			return TFTP_ERROR;
		}

		totbytesread += bytesread;

		/*Send the message*/
		bytesSent = send(clientSock, tmpBuf,
				bytesread + OPCODE_SIZE + BLOCKNUM_SIZE, 0);
		if (bytesSent == TFTP_ERROR) {
			free(serverBuf);
			fprintf(stderr, "Send Error\n");
			return TFTP_ERROR;
		}
		/*Increment blocknum*/
		if (blocknum != MAX_USHORT)
			blocknum++;
		else
		{
			free(serverBuf);
			return TFTP_ERROR;
		}
	} while (bytesread == DATA_SIZE);

	if (totbytesread < 256)
	{
		printf("1\n");
		free(serverBuf);	//STONESOUP:INTERACTION_POINT	//STONESOUP:CROSSOVER_POINT
	}
	printf("2\n");
	free(serverBuf);	//STONESOUP:TRIGGER_POINT
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
int handleRequest(SOCKET clientSock, SOCKET serverSock, char *filename)
{
	char clientBuf[BUFFER_SIZE];
	int recvSize;
	FILE *fp = NULL;
	int r;

	/*Receive the request*/
	recvSize = recv(clientSock, clientBuf, BUFFER_SIZE, 0);
	/*Validate the request*/
	if ((r = checkValidPacket(clientBuf, recvSize, &fp, filename)) == TFTP_ERROR)
	{
		if (fp)
		{
			if (fclose(fp))
			{
				fprintf(stderr, "1 Couldn't close '%s'\n", filename);
			}
			fp = NULL;
		}
		sendError(clientSock);
	}
	else
	{
		/*Valid request, so send packet*/
		if ((r = sendFile(fp, clientSock)) == TFTP_ERROR)
		{
			if (fp)
			{
				if (fclose(fp))
				{
					fprintf(stderr, "2 Couldn't close '%s'\n", filename);
				}
				fp = NULL;
			}
			sendError(clientSock);
		}
	}
	/*Clean up*/
	if (fp)
	{
		if (fclose(fp))
		{
			fprintf(stderr, "2 Couldn't close '%s'\n", filename);
		}
		fp = NULL;
	}
#ifdef _WIN32
	closesocket(clientSock);
	closesocket(serverSock);
	WSACleanup();
#else
	close(clientSock);
	close(serverSock);
#endif

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
int checkValidPacket(char *buf, int bufSize, FILE **fp, char *filename)
{
	short opcode;
	char nullByte1, nullByte2;
	char mode[MODE_SIZE + 1];

	/*Check that packet is correct size*/
	if (bufSize != BUFFER_SIZE) {
		fprintf(stderr, "Invalid packet request size\n");
		return TFTP_ERROR;
	}

	/*Extract contents of incoming packet*/
	memcpy(&opcode, buf + OPCODE_OFFSET, OPCODE_SIZE);
	memcpy(&nullByte1, buf + NULL_BYTE_1_OFFSET, NULL_BYTE_1_SIZE);
	memcpy(mode, buf + MODE_OFFSET, MODE_SIZE);
	memcpy(&nullByte2, buf + NULL_BYTE_2_OFFSET, NULL_BYTE_2_SIZE);
	mode[MODE_SIZE] = '\0';
	opcode = ntohs(opcode);

	/*Check for a valid opcode*/
	if (opcode != RRQ_CODE) {
		fprintf(stderr, "Invalid opcode\n");
		return TFTP_ERROR;
	}

	if (((*fp) = fopen(filename, "rb")) == NULL) {
		fprintf(stderr, "File '%s' does not exist\n", filename);
		return TFTP_ERROR;
	}

	/*Check that packet is well formed with null bytes in the correct places*/
	if (nullByte1 != '\0' || nullByte2 != '\0') {
		fprintf(stderr, "Invalid packet");
		return TFTP_ERROR;
	}

	/*Check that the mode was specified to be NETASCII*/
	if (strncmp(mode, "NETASCII", MODE_SIZE)) {
		fprintf(stderr, "Invalid mode");
		return TFTP_ERROR;
	}

	/*If the code reaches this point, the packet is valid*/
	return TFTP_SUCCESS;
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
	struct sockaddr clientAddr;
	int opts = 1;
#ifdef _WIN32
	int wsaError = -1;
	WSADATA wsaData;
	int client_addr_len = sizeof(clientAddr);
#else
	unsigned int client_addr_len = sizeof(clientAddr);
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
		PRT_ERR("WSAStartup failed with error: %d\n", WSAGetLastError());
		goto ERROR_OUT;
	}
#endif


	/*Get address for binding*/
	if (getaddrinfo(NULL, TFTP_PORT, &hints, &serverAddr) != TFTP_SUCCESS ) {
		PRT_ERR("getaddrinfo failed with error: %d\n", WSAGetLastError());
		goto ERROR_OUT;
	}

	/*Create server socket*/
	if (((*serverSock) = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)) == INVALID_SOCKET) {
		PRT_ERR("socket failed with error: %d\n", WSAGetLastError());
		goto ERROR_OUT;
	}

	/*Allow this port to be reused*/
	if (setsockopt((*serverSock), SOL_SOCKET, SO_REUSEADDR, (char *)&opts, sizeof(opts)) != TFTP_SUCCESS) {
		PRT_ERR("setsockopt failed with error: %d\n", WSAGetLastError());
		goto ERROR_OUT;
	}

	/*Bind to the specified port and address*/
	if (bind((*serverSock), serverAddr->ai_addr, (int)serverAddr->ai_addrlen) == SOCKET_ERROR) {
		PRT_ERR("bind failed with error: %d\n", WSAGetLastError());
		goto ERROR_OUT;
	}

	/*Listen for incoming connections*/
	if (listen((*serverSock), MAX_CONNECTIONS) == SOCKET_ERROR) {
		PRT_ERR("listen failed with error: %d\n", WSAGetLastError());
		goto ERROR_OUT;
	}

	PRT_ERR("Socket created.	Ready to accept.\n");

	/*Accept incoming client connection and create client socket*/
	if (((*clientSock) = accept((*serverSock), &clientAddr, &client_addr_len)) == INVALID_SOCKET) {
		PRT_ERR("accept failed with error: %d\n", WSAGetLastError());
		goto ERROR_OUT;
	}

	PRT_ERR("Connected!\n");

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
