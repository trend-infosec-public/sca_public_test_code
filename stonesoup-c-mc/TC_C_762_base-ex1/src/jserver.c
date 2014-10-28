
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
**  Date: 23 Mar 2012
**
**  Revision History
**  Date      Change
**  21 Mar 2012 Initial revision
**
**l
**  Main will act as an Echo Server. It opens a socket for listening, reads in any
**  ascii text coming across that port, and echos the same text back to the client. It will
**  continue to run until it receives a Ctrl-C.
**
** Error: In Readline() The program will create a heap buffer to store the characters read
** from the network socket. The calling function (GetUserInput()) disposes of this buffer
** when it is finished sending it back. When a perceived bad character is read on the network,
** the program sends an automatically allocated string back to the client, however since this
** buffer is not on the heap, the free() command fails when trying to deallocate
**
**
** STONESOUP Weakness Class:
** CWE ID: CWE-762
** Variant Spreadsheet Rev #: ###
** Variant Spreadsheet ID: ###
**
** Variant Features:
**
**
** I/0 Pairs:
**   Good: 1st Set: ./client.exe 0
**         2nd Set: ./client.exe 1
**         3rd Set: ./client.exe 2
**         4th Set:
**         5th Set:
**    Bad: 1st Set: ./client.exe 3
**         2nd Set:
**
** How program works:
**     There is a server piece and a client piece to this test. The server piece starts with no
**     command-line arguments:
**     				./main.exe
**
**     The client application performs the injection of characters to the main application. It
**     should run on the same machine as the main.exe server and takes 1 parameter, either a 0
**     (which sends a valid message to be repeated) or a 1 (which sends an invalid message)
**
**  				./client.exe [0|1]
************************************************************************/

#undef __STRICT_ANSI__
#undef _ISOC99_SOURCE

#include <errno.h>

#include <unistd.h>
#include <sys/types.h>
#if _WIN32
#else
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#endif

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#define SERVER_PORT "9081"
#define LISTENQ		1024

/*  Global constants  */

#define MAX_LINE           80
#define BUF_SIZE		   20


char * Readline(int sockd) {
    ssize_t n, rc;
    char    c;
    char * buffer = (char *)malloc(BUF_SIZE);
    char * ptr = buffer;
    int maxlen = MAX_LINE;

    for ( n = 1; n < maxlen; n++ ) {

		if ( (rc = read(sockd, &c, 1)) == 1 ) {   // STONESOUP:INTERACTION_POINT
			// if it's not an ascii character, send error
			//printf( "c is %d\n", c);
			if ( c > 0 ) {
				// received ascii character
				*ptr++ = c;
				if ( c == '\n')
					break;
			} else {
				return "Bad Input";   // STONESOUP:CROSSOVER_POINT
			}
		}
		else if ( rc == 0 ) {
			if ( n == 1 )
			return buffer;
			else
			break;
		}
		else {
			return "Interrupt Error";
		}
    }

    *ptr = 0;
    return buffer;
}


/*  Write a line to a socket  */

ssize_t Writeline(int sockd, const void *vptr, size_t n) {
    size_t      nleft;
    ssize_t     nwritten;
    const char *buffer;

    buffer = vptr;
    nleft  = n;

    while ( nleft > 0 ) {
	if ( (nwritten = write(sockd, buffer, nleft)) <= 0 ) {
	    if ( errno == EINTR )
		nwritten = 0;
	    else
		return -1;
	}
	nleft  -= nwritten;
	buffer += nwritten;
    }

    return n;
}

int GetUserInput(int sockd) {

	char * input;
	ssize_t ret;

	input = Readline(sockd);

	fprintf(stdout, "INPUT: %s\n", input);
	fflush(stdout);

	ret = Writeline(sockd, input, strlen(input));

	free( input );   // STONESOUP:TRIGGER_POINT

	return 0;
}

int main(int argc, char *argv[]) {
    int       list_s;                /*  listening socket          */
    int       conn_s;                /*  connection socket         */

	struct addrinfo hints;
	struct addrinfo *serverAddr = NULL;
	int opts = 1;

	/*Initialize socket values*/
	memset(&hints, '\0', sizeof(hints));
	hints.ai_family = AF_INET;
	hints.ai_socktype = SOCK_STREAM;
	hints.ai_protocol = IPPROTO_TCP;
	hints.ai_flags = AI_PASSIVE;

	/*Get address for binding*/
	if (getaddrinfo(NULL, SERVER_PORT, &hints, &serverAddr) < 0 ) {
		fprintf(stderr, "getaddrinfo failed with error\n");
		exit(EXIT_FAILURE);
	}

	/*Create server listening socket*/
	if ((list_s = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)) < 0 ) {
		fprintf(stderr, "socket failed with error\n");
		exit(EXIT_FAILURE);
	}

	/*Allow this port to be reused*/
	if (setsockopt(list_s, SOL_SOCKET, SO_REUSEADDR, (char *)&opts, sizeof(opts)) < 0 ) {
		fprintf(stderr, "setsockopt failed with error\n");
		exit(EXIT_FAILURE);
	}

	/*Bind to the specified port and address*/
	if (bind(list_s, serverAddr->ai_addr, (int)serverAddr->ai_addrlen) < 0) {
		fprintf(stderr, "bind failed with error\n");
		exit(EXIT_FAILURE);
	}

	/*Listen for incoming connections*/
	if (listen(list_s, LISTENQ) < 0) {
		fprintf(stderr, "listen failed with error\n");
		exit(EXIT_FAILURE);
	}

	fprintf(stderr, "Socket created.  Ready to accept.\n");
	fflush(stderr);

    /*  Enter an infinite loop to respond
        to client requests and echo input  */

    while ( 1 ) {

    	fprintf(stderr, "ECHOSERV: waiting for a connection\n");
    	fflush(stderr);

		/*  Wait for a connection, then accept() it  */

		if ( (conn_s = accept(list_s, NULL, NULL) ) < 0 ) {
			fprintf(stderr, "ECHOSERV: Error calling accept()\n");
			exit(EXIT_FAILURE);
		}

		fprintf(stderr, "ECHOSERV: Connected!\n");
		fflush(stderr);

		/*  Retrieve an input line from the connected socket
			then simply write it back to the same socket.     */

		GetUserInput(conn_s);

		/*  Close the connected socket  */

		if ( close(conn_s) < 0 ) {
			fprintf(stderr, "ECHOSERV: Error calling close()\n");
			exit(EXIT_FAILURE);
		}
    }

	/*Free the server address information*/
	freeaddrinfo(serverAddr);

		return(0);
}
