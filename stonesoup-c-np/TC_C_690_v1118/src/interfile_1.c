
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
** regfuncs.c
**
**  Created on: 19 Oct, 2011
**      
**
**
**	Description:
**
************************************************************************/
#include "helper.h"

/** @brief This function receives a request for time from a client and
 *         calls the necessary function to send a response.
 *
 *  @param[in] clientSock should be an already connected client socket
 *  @param[in] serverSock should be the server's socket.
 *	@return Function returns GET_TIME_SUCCESS on success and GET_TIME_ERROR on error.
 */
int handleRequest(SOCKET clientSock, SOCKET serverSock)
{
	char clientBuf[BUFFER_SIZE];
	int recvSize;
	FILE *fp ;
	int r;
	const int *bad = NULL; //STONESOUP:DATA_FLOW:ADDRESS_AS_A_CONSTANT

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

	if (fp) close_fp(fp, &bad);

	return r;
}

/* End of file */
