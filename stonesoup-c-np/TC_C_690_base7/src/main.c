
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
** Variant Features:
**		SOURCE_TAINT:SOCKET
**		DATA_TYPE:SIGNED_BYTE
**		CONTROL_FLOW:ELSE_CONDITIONAL
**		DATA_FLOW:ARRAY_CONTENT_VALUE
**
** (x means yes, - means no)
** Tested in MS Windows XP 32bit    		 -
** Tested in MS Windows 7    64bit    		 -
** Tested in Ubuntu 2.6.38-11-generic 32bit  x
** Tested in Ubuntu 2.6.38-11-generic 64bit  -
**
** Error: In checkvalidPacket() the check for valid filename for linux
**        if a colon is in the filename a GET_TIME_ERROR_1 which is set
**        to one is return, this causes a segmentation fault because
**        in gettime() FILE * temp; //NULL is commented out when it is
**        initialized. When the check for fopen returns NULL we seg fault.
**********************************************************************/
#undef __STRICT_ANSI__
#undef _ISOC99_SOURCE

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <stdarg.h>
#include <time.h>


//OS friendly -  should work on windows/unix systems
#ifdef _WIN32
	#include<windef.h>
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
int getime(char *timeReturnFileName);
void sendError(SOCKET clientSock);


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
		if ((r = handleRequest(clientSock, serverSock)) == GET_TIME_ERROR) {
			sendError(clientSock);
		}

	}while( r != GET_TIME_ERROR);

	return(r);
}


/** @brief This function sends the contents of the requested file to clientSock.
 *
 * 	@param[in] fp is a file pointer to the requested file that should already be
 * 	           opened with read permissions.
 *  @param[in] clientSock is a socket connected to a client.
 *  @return Function returns GET_TIME_SUCCESS on success and GET_TIME_ERROR on error.
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
		if (ferror(fp) != GET_TIME_SUCCESS) {
			fprintf(stderr, "Read Error\n");
			return GET_TIME_ERROR;
		}
		/*Send the message*/
		bytesSent = send(clientSock, serverBuf,
				bytesread + OPCODE_SIZE + BLOCKNUM_SIZE, 0);
		if (bytesSent == GET_TIME_ERROR) {
			fprintf(stderr, "Send Error\n");
			return GET_TIME_ERROR;
		}
		/*Increment blocknum*/
		if (blocknum != MAX_USHORT)
			blocknum++;
		else
			return GET_TIME_ERROR;
	} while (bytesread == DATA_SIZE);

	return GET_TIME_SUCCESS;
}

/** @brief This function sends an error packet to a connected client.
 *
 * 	@pre clientSock should be an already connected client.
 * 	@param[in] clientSock is a connected client socket.
 * 	@return Function returns GET_TIME_SUCCESS on success and GET_TIME_ERROR on error.
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

	if (bytesSent == GET_TIME_ERROR) {
		fprintf(stderr, "Send Error");
	}
}

/** @brief This function gets the system time and populates it to a
 *         readable format for local users.
 *
 * 	@pre clientSock should be an already connected client.
 * 	@param[in] clientSock is a connected client socket.
 * 	@return Function returns GET_TIME_SUCCESS on success and GET_TIME_ERROR on error.
 */
int gettime(char *timeReturnFileName )			//STONESOUP:DATA_TYPE:SIGNED_BYTE
{

	time_t rawtime;
	struct tm * ptm;
	FILE * temp ;    //= NULL;
	int r;

	/*Get time and write to file */
	time (&rawtime);
	if ((ptm = gmtime ( &rawtime)) == NULL ){
		fprintf(stderr, "Could not create return file\n");
		return GET_TIME_ERROR;
	}else		//STONESOUP:CONTROL_FLOW:ELSE_CONDITIONAL
	{
		/*Create return file for time */
		if (( temp = fopen(timeReturnFileName, "w")) == NULL) {		//STONESOUP:INTERACTION_POINT	//STONESOUP:CROSSOVER_POINT	//STONESOUP:TRIGGER_POINT
			fprintf(stderr, "Could not open return file \n");
			return GET_TIME_ERROR;
		}

		/*Print to file*/
		if (( r = fprintf( temp,"Time is:\n")) <= GET_TIME_ERROR ) {
			fprintf(stderr, "Could write to file \n");
			fclose(temp);
			return GET_TIME_ERROR;
		}
/*
		if((r = fprintf( temp,"Seoul:		%2d:%02d\n", (ptm->tm_hour+KST)%24, ptm->tm_min))<= GET_TIME_ERROR ){
			fprintf(stderr, "Could write to file \n");
			fclose(temp);
			return GET_TIME_ERROR;
		}
		if((r = fprintf( temp,"Ramsten AB:	%2d:%02d\n", (ptm->tm_hour+CET)%24, ptm->tm_min))<= GET_TIME_ERROR ){
			fprintf(stderr, "Could write to file \n");
			fclose(temp);
			return GET_TIME_ERROR;
		}
		if((r = fprintf( temp,"Milden Hall AB:	%2d:%02d\n", (ptm->tm_hour+GMT)%24, ptm->tm_min))<= GET_TIME_ERROR ){
			fprintf(stderr, "Could write to file \n");
			fclose(temp);
			return GET_TIME_ERROR;
		}
		if((r = fprintf( temp,"Langley AFB:	%2d:%02d\n", (ptm->tm_hour+ EST)%24, ptm->tm_min))<= GET_TIME_ERROR ){
			fprintf(stderr, "Could write to file \n");
			fclose(temp);
			return GET_TIME_ERROR;
		}
		if((r = fprintf( temp,"March AB:	%2d:%02d\n", (ptm->tm_hour+ PST)%24, ptm->tm_min))<= GET_TIME_ERROR ){
			fprintf(stderr, "Could write to file \n");
			fclose(temp);
			return GET_TIME_ERROR;
		}
*/
	}
	if (temp) fclose(temp);

	return GET_TIME_SUCCESS;
}

/** @brief This function receives a request for time from a client and
 *         calls the necessary function to send a response.
 *
 *  @param[in] clientSock should be an already connected client socket
 *  @param[in] serverSock should be the server's socket.
 *	@return Function returns GET_TIME_SUCCESS on success and GET_TIME_ERROR on error.
 */
int handleRequest(SOCKET clientSock, SOCKET serverSock)		//STONESOUP:SOURCE_TAINT:SOCKET
{
	char clientBuf[BUFFER_SIZE];
	int recvSize;
	FILE *fp = NULL;
	int r;


	/*Receive the request*/
	recvSize = recv(clientSock, clientBuf, BUFFER_SIZE, 0);
	/*Validate the request*/
	if ((r = checkValidPacket(clientBuf, recvSize, &fp)) == GET_TIME_ERROR) {
		sendError(clientSock);
	} else {
		/*Valid request, get time and send*/
		if ((r = sendFile(fp, clientSock)) == GET_TIME_ERROR) {
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

/** @brief Function checks for a valid request packet and calls gettime
 *         which get the local time and writes it to a file to be send
 *         back to the user.
 * 	@param[in] buf is a buffer holding the received packet.
 * 	@param[in] bufSize is the length of the request packet.
 *  @param[out] fp is a file pointer which will be used to fopen the
 *  			requested file.
 *  @return Function returns GET_TIME_SUCCESS for a valid request and
 *  		GET_TIME_ERROR on an invalid request.
 */
int checkValidPacket(char *buf, int bufSize, FILE **fp)
{
	short opcode;
	char timeReturnFileName[FILENAME_SIZE];
	char nullByte1, nullByte2;
	char mode[MODE_SIZE + 1];
	int gettime_return, i;


	/*Check that packet is correct size*/
	if (bufSize != BUFFER_SIZE) {
		fprintf(stderr, "Invalid packet request size\n");
		return GET_TIME_ERROR;
	}

	/*Extract contents of incoming packet*/
	memcpy(&opcode, buf + OPCODE_OFFSET, OPCODE_SIZE);
	memcpy(timeReturnFileName, buf + FILENAME_OFFSET, FILENAME_SIZE); //STONESOUP:DATA_FLOW:ARRAY_CONTENT_VALUE
	memcpy(&nullByte1, buf + NULL_BYTE_1_OFFSET, NULL_BYTE_1_SIZE);
	memcpy(mode, buf + MODE_OFFSET, MODE_SIZE);
	memcpy(&nullByte2, buf + NULL_BYTE_2_OFFSET, NULL_BYTE_2_SIZE);
	timeReturnFileName[FILENAME_SIZE - 1] = '\0';
	mode[MODE_SIZE] = '\0';
	opcode = ntohs(opcode);

	/*Check for a valid opcode*/
	if (opcode != RRQ_CODE) {
		fprintf(stderr, "Invalid opcode\n");
		return GET_TIME_ERROR;
	}

	/*
	 * Check for a valid filename.  A valid filename contains no slash
	 * characters, colons, or double dots.  A valid filename should also
	 * correspond to an existing file in the working directory of the server.
	 */
	for (i = 0; i < FILENAME_SIZE; i++) {
		if (timeReturnFileName[i] == '\0') break;
		if (timeReturnFileName[i] == '\\' || timeReturnFileName[i] == '/') {
			fprintf(stderr, "Invalid filename\n");
			return GET_TIME_ERROR;
		}
		if (timeReturnFileName[i] == '.' && timeReturnFileName[i + 1] == '.') {
			fprintf(stderr, "Invalid filename\n");
			return GET_TIME_ERROR;
		}
#ifdef _WIN32
		if (timeReturnFileName[i] == '&') {
			fprintf(stderr, "Invalid filename\n");
			return GET_TIME_ERROR_1;    // Return a 1 if & exists in filename.
		}
	#else
		if (timeReturnFileName[i] == ':') {
			fprintf(stderr, "Invalid filename\n");
			return GET_TIME_ERROR_1;    // Return a 1 if : exists in filename.
		}
#endif
	}

	/*Get time and populate file*/
	if ((gettime_return = gettime(timeReturnFileName)) != GET_TIME_SUCCESS ){
		fprintf(stderr, "Could not get time\n");
		return GET_TIME_ERROR;
	}

	/*Create return file for time */
	if (((*fp) = fopen(timeReturnFileName, "r")) == NULL) {
		fprintf(stderr, "Could open return file \n");
		return GET_TIME_ERROR;
	}


	/*Check that packet is well formed with null bytes in the correct places*/
	if (nullByte1 != '\0' || nullByte2 != '\0') {
		fprintf(stderr, "Invalid packet");
		return GET_TIME_ERROR;
	}

	/*Check that the mode was specified to be NETASCII*/
	if (strncmp(mode, "NETASCII", MODE_SIZE)) {
		fprintf(stderr, "Invalid mode");
		return GET_TIME_ERROR;
	}

	/*If the code reaches this point, the packet is valid*/
	return GET_TIME_SUCCESS;
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
    if (((*serverSock) = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP))
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

