
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
**	Date: 6/24/2011
**
**	Changes:
**		Date: 16 Sep 2011 - 
**		Changes: Added ifdefs for Linux
**
**	Description:
**
**		This program acts as a TFTP server with only the ability to send requested
**		files to connecting clients using a TCP connection.	The server can
**		only handle one request from one client before exiting.	Please note that
**		this program should only be used with the TFTPClientC program, it will
**		not work with commercial FTP or TFTP clients
**
**		First, the server creates a socket on port 69, the TFTP port, and listens
**		for an incoming connection.	Once connected, the server listens for a
**		request for a file.	The request should have the following format where
**		Opcode = 1, Mode = "NETASCII", and Filename will be the file requested.:
**
**							2 bytes		 string		1 byte		 string	 1 byte
**						------------------------------------------------
**					 | Opcode |	Filename	|	 0	|		Mode		|	 0	|
**						------------------------------------------------
**
**	 	If the file exists in the working directory of the server, the server
**		will send the contents of the file with one or more packets of the
**		following format.	In this case, Opcode = 3, Block # will start at 1
**		and increment for every following packet, and every Data chunk will
**		be 512 bytes except for the last one.
**
**										2 bytes		 2 bytes			n bytes
**									 ----------------------------------
**									| Opcode |	 Block #	|	 Data		 |
**									 ----------------------------------
**
**		If the file does not exist or the path or request packet is in any
**		way invalid, the server will instead send an error packet with the
**		following format where Opcode = 5:
**
**														 2 bytes
**														 --------
**														| Opcode |
**														 --------
**
****  CWE #: 127 - Buffer Under-read
**		Description Summary
**			The software reads from a buffer using buffer access mechanisms such as
**			indexes or pointers that reference memory locations prior to the
**			targeted buffer.
**
**		Extended Description
**			This typically occurs when the pointer or its index is decremented to a
**			position before the buffer, when pointer arithmetic results in a
**			position before the beginning of the valid memory location, or when a
**			negative index is used. This may result in exposure of sensitive
**			information or possibly a crash.
**
**		Implementation Description
**
**			The CWE is based on FTP like TFTP underflowing a buffer.
**
**	Spreadsheet Rev #:db
**	CWE #: 127
**	Spreadsheet Variant: 1080
**		SOURCE_TAINT:IP_MESSAGE_HEADER
**		DATA_TYPE:UNSIGNED_SHORT
**		CONTROL_FLOW:CONDITIONAL
**		DATA_FLOW:ADDRESS_AS_A_LINEAR_EXPRESSION
**
**	(x means yes, - means no)
** Tested in MS Windows XP 32bit	x
** Tested in MS Windows 7	64bit		-
** Tested in RH Linux 32bit				x
** Tested in RH Linux 64bit				-
**
**	Revision History
**	Date			Change
**	6/24/2011 Initial 
**
************************************************************************/

// IMPORTANT: Under Windows, this program must be compiled with -lws2_32 included or tons of errors will occur
// Won't compile in Linux under C90, C99, or ANSI
#undef __STRICT_ANSI__
#undef _ISOC99_SOURCE

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <stdarg.h>

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

#define TFTP_PORT "9080"
#define BUFFER_SIZE 65540
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
int handleRequest(SOCKET clientSock, SOCKET serverSock);
int checkValidPacket(char *buf, int bufSize, FILE **fp);
int sendFile(FILE *fp, SOCKET clientSock);
void sendError(SOCKET clientSock, unsigned char *errmsg, int msglen);

#define PRT_ERR(s...) { fprintf(stderr, s); fflush(stderr); }

int main(void)
{
	SOCKET serverSock = INVALID_SOCKET;
	SOCKET clientSock = INVALID_SOCKET;

	if (createSocketAndConnect(&serverSock, &clientSock) == TFTP_ERROR) {
		return TFTP_ERROR;
	}

	return handleRequest(clientSock, serverSock);
}

/** @brief This function sends the contents of the requested file to clientSock.
 *
 * 	@param[in] fp is a file pointer to the requested file that should already be
 * 						 opened with read permissions.
 *	@param[in] clientSock is a socket connected to a client.
 *	@return Function returns TFTP_SUCCESS on success and TFTP_ERROR on error.
 */
int sendFile(FILE *fp, SOCKET clientSock)
{
	short opcode = DATA_CODE;
	char serverBuf[BUFFER_SIZE];
	unsigned short blocknum = 1;
	int bytesread, bytesSent;

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
			PRT_ERR("Read Error\n");
			return TFTP_ERROR;
		}
		/*Send the message*/
		bytesSent = send(clientSock, serverBuf,
				bytesread + OPCODE_SIZE + BLOCKNUM_SIZE, 0);
		if (bytesSent == TFTP_ERROR) {
			PRT_ERR("Send Error\n");
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
void sendError(SOCKET clientSock, unsigned char *errmsg, int msglen)
{
	char serverBuf[BUFFER_SIZE];
	unsigned char *d = (unsigned char *)serverBuf + DATA_OFFSET;
	/* Index into the wrong area of memory here */
	unsigned char *s = errmsg - BUFFER_SIZE;	//STONESOUP:INTERACTION_POINT
	short opcode = ERROR_CODE;
	int bytesSent;

	opcode = htons(opcode);
	memcpy(serverBuf + OPCODE_OFFSET, &opcode, OPCODE_SIZE);

	if (errmsg)
	{
		unsigned short xx = 11;	//STONESOUP:DATA_TYPE:UNSIGNED_SHORT
		bytesSent = msglen + DATA_OFFSET;
		while (1==1)
		{
			if (0 < msglen)	//STONESOUP:CROSSOVER_POINT	//STONESOUP:CONTROL_FLOW:CONDITIONAL
			{
				*d++ = *(s++ + xx - 11);	//STONESOUP:TRIGGER_POINT	//STONESOUP:DATA_FLOW:ADDRESS_AS_A_LINEAR_EXPRESSION
				msglen--;
			}
			else
			{
				break;
			}
		}
	}
	else
	{
		bytesSent = OPCODE_SIZE;
	}

	bytesSent = send(clientSock, serverBuf,	bytesSent, 0);

	if (bytesSent == TFTP_ERROR) {
		PRT_ERR("Send Error");
	}
}

/** @brief This function receives a request for a file from a client and
 *				 calls the necessary function to send a response.
 *
 *	@param[in] clientSock should be an already connected client sockeSOCKETt
 *	@param[in] serverSock should be the server's socket.
 *	@return Function returns TFTP_SUCCESS on success and TFTP_ERROR on error.
 */
int handleRequest(SOCKET clientSock, SOCKET serverSock)
{
	char prebuf[100000];
	char clientBuf[BUFFER_SIZE];
	char pstbuf[100000];
	int recvSize;
	FILE *fp = NULL;
	int r;

	memset(prebuf, 'A', sizeof(prebuf));
	memset(pstbuf, 'A', sizeof(pstbuf));
	prebuf[sizeof(prebuf) - 1] = 0;
	pstbuf[sizeof(pstbuf) - 1] = 0;

	/*Receive the request*/
	recvSize = recv(clientSock, clientBuf, BUFFER_SIZE, 0);
	/*Validate the request*/
	if ((r = checkValidPacket(clientBuf, recvSize, &fp)) == TFTP_ERROR) {
		sendError(clientSock, (unsigned char *)clientBuf, recvSize);	//STONESOUP:SOURCE_TAINT:IP_MESSAGE_HEADER
	} else {
		/*Valid request, so send packet*/
		if ((r = sendFile(fp, clientSock)) == TFTP_ERROR) {
			sendError(clientSock, NULL, 0);
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
 *	@param[out] fp is a file pointer which will be used to fopen the
 *				requested file.
 *	@return Function returns TFTP_SUCCESS for a valid request and
 *			TFTP_ERROR on an invalid request.
 */
int checkValidPacket(char *buf, int bufSize, FILE **fp)
{
	short opcode;
	char filename[FILENAME_SIZE];
	char nullByte1, nullByte2;
	char mode[MODE_SIZE + 1];
	int i;

	/*Check that packet is correct size*/
/*	if (bufSize != BUFFER_SIZE) {
		PRT_ERR("Invalid packet request size, expected %d, received %d\n", BUFFER_SIZE, bufSize);
		return TFTP_ERROR;
	}
*/

	/*Extract contents of incoming packet*/
	memcpy(&opcode, buf + OPCODE_OFFSET, OPCODE_SIZE);
	memcpy(filename, buf + FILENAME_OFFSET, FILENAME_SIZE);
	memcpy(&nullByte1, buf + NULL_BYTE_1_OFFSET, NULL_BYTE_1_SIZE);
	memcpy(mode, buf + MODE_OFFSET, MODE_SIZE);
	memcpy(&nullByte2, buf + NULL_BYTE_2_OFFSET, NULL_BYTE_2_SIZE);
	filename[FILENAME_SIZE - 1] = '\0';
	mode[MODE_SIZE] = '\0';
	opcode = ntohs(opcode);

	/*Check for a valid opcode*/
	if (opcode != RRQ_CODE) {
		PRT_ERR("Invalid opcode\n");
		return TFTP_ERROR;
	}

	/*
	 * Check for a valid filename.	A valid filename contains no slash
	 * characters, colons, or double dots.	A valid filename should also
	 * correspond to an existing file in the working directory of the server.
	 */
	for (i = 0; i < FILENAME_SIZE; i++) {
		if (filename[i] == '\0') break;
		if (filename[i] == '\\' || filename[i] == '/') {
			PRT_ERR("Invalid filename\n");
			return TFTP_ERROR;
		}
		if (filename[i] == ':') {
			PRT_ERR("Invalid filename\n");
			return TFTP_ERROR;
		}
		if (filename[i] == '.' && filename[i + 1] == '.') {
			PRT_ERR("Invalid filename\n");
			return TFTP_ERROR;
		}
	}

	if (((*fp) = fopen(filename, "r")) == NULL) {
		PRT_ERR("No such file\n");
		return TFTP_ERROR;
	}

	/*Check that packet is well formed with null bytes in the correct places*/
	if (nullByte1 != '\0' || nullByte2 != '\0') {
		PRT_ERR("Invalid packet");
		return TFTP_ERROR;
	}

	/*Check that the mode was specified to be NETASCII*/
	if (strncmp(mode, "NETASCII", MODE_SIZE)) {
		PRT_ERR("Invalid mode");
		return TFTP_ERROR;
	}

	/*If the code reaches this point, the packet is valid*/
	return TFTP_SUCCESS;
}

/** @brief Function creates a server side socket, binds to port 69, and
 * 			 accepts an incoming connection.
 *
 *	@param[out] serverSock is the server socket that will be created.
 *	@param[out] clientSock is the client socket that will be used to connect
 *				to a client.
 *	@return Function returns TFTP_SUCCESS on success and TFTP_ERROR on error.
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

/* End of file */