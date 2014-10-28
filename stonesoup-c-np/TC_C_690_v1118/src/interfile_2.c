
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
**
************************************************************************/

#include "helper.h"

void close_fp(FILE *fp, const int **bad){

	if(bad == NULL){
		fclose(fp);  //STONESOUP:TRIGGER_POINT
	}else{
		fclose(fp);
	}
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
int gettime(char *timeReturnFileName )
{

	time_t rawtime;
	struct tm * ptm;
	FILE * temp = NULL;
	int r;

	/*Get time and write to file */
	time (&rawtime);
	if ((ptm = gmtime ( &rawtime)) == NULL ){
		fprintf(stderr, "Could not create return file\n");
		return GET_TIME_ERROR;
	}else
	{
		/*Create return file for time */
		if (( temp = fopen(timeReturnFileName, "w")) == NULL) {
			fprintf(stderr, "Could open return file \n");
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
	//char timeReturnFileName[FILENAME_SIZE];
	union{
		char buffer_1[FILENAME_SIZE];
		char buffer_2[FILENAME_SIZE];
	}timeReturnFileName;	//STONESOUP:DATA_TYPE:UNION

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
	memcpy(timeReturnFileName.buffer_1, buf + FILENAME_OFFSET, FILENAME_SIZE);
	memcpy(&nullByte1, buf + NULL_BYTE_1_OFFSET, NULL_BYTE_1_SIZE);
	memcpy(mode, buf + MODE_OFFSET, MODE_SIZE);
	memcpy(&nullByte2, buf + NULL_BYTE_2_OFFSET, NULL_BYTE_2_SIZE);
	timeReturnFileName.buffer_2[FILENAME_SIZE - 1] = '\0';
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
		if (timeReturnFileName.buffer_1[i] == '\0') break;
		if (timeReturnFileName.buffer_1[i] == '\\' || timeReturnFileName.buffer_1[i] == '/') {
			fprintf(stderr, "Invalid filename\n");
			return GET_TIME_ERROR;
		}
#ifdef _WIN32
		if (timeReturnFileName.buffer_1[i] == '&') {
			fprintf(stderr, "Invalid filename\n");
			return GET_TIME_ERROR;
		}
#endif
		if (timeReturnFileName.buffer_1[i] == ':') {
			fprintf(stderr, "Invalid filename\n");
			return GET_TIME_ERROR;    //STONESOUP:CROSSOVER_POINT
		}
		if (timeReturnFileName.buffer_1[i] == '.' && timeReturnFileName.buffer_1[i + 1] == '.') {
			fprintf(stderr, "Invalid filename\n");
			return GET_TIME_ERROR;
		}
	}
	/*Get time and populate file*/
	if ((gettime_return = gettime(timeReturnFileName.buffer_1)) != GET_TIME_SUCCESS ){
		fprintf(stderr, "Could not get time\n");
		return GET_TIME_ERROR;
	}

	/*Create return file for time */
	if (((*fp) = fopen(timeReturnFileName.buffer_1, "r")) == NULL) {
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



/* End of file */
