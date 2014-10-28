
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
**  Date: 24 Mar 12
**
**  Revision History
**  Date      Change
**  24 Mar 12 Initial 
**
**	Uses WeatherClient.c
**
**  Main is a server that listens on the port specified at the command line
**  it will accept one connection at a time and parse an incoming command.
**  The server responds to a "list" command with a listing of supported
**  functionality and the values that should be passed in order to invoke
**  each.
**
**
**  Program writes a response that depends on the request made by the client.
**
**
**  Error: The functionality supported is accessed using function pointers.
**  The server advertises the actual addresses of the functions to the client
**  and the client passes function addresses back to make requests. The good
**  server will whitelist requests before assigning the requested value
**  to a function pointer and derefencing it. The bad server will not whitelist
**  and requests and blindly assign the values to function pointers and
**  dereference them... It would be very easy to get code execution on this
**  but it would be very unreliable in a testing environment.
**
**
** TECHNICAL IMPACT: Server Crash / DoS
** Code execution would be easy but unreliable
**
** STONESOUP Weakness Class:
** CWE ID: CWE-822
** Variant Spreadsheet Rev #: ###
** Variant Spreadsheet ID: ###
**
** Variant Features:
**
** SEE CLIENT FOR I/O Pairs -- feel free to change port.
** I/0 Pairs:
**   Good: 1st Set: ./SERVER PORTNO
**         2nd Set:
**         3rd Set:
**         4th Set:
**         5th Set:
**    Bad: 1st Set:
**         2nd Set:
**
** How program works:
** This is a server program that takes one argument, the port
** number for listening. It then reads in one command from a connecting
** client and responds appropriately.
************************************************************************/

/******** INCLUDES ********/
#include <stdio.h>                   /* FileIO                               */
#include <stdlib.h>                  /* strtol                               */
#include <strings.h>                 /* bzero                                */
#include <string.h>                  /* strlen                               */
#include <unistd.h>                  /* write function -- printf handles null*/
#if _WIN32
#else
#include <sys/socket.h>              /* socket IO                            */
#include <arpa/inet.h>               /* htonl                                */
#endif

/********* DEFINES ********/
#define MAX_PORT     (10000)         /* arbitrary cutoff for max port num    */
#define MIN_PORT     (8000)          /* stay clear of low port nums          */
#define MAX_MSG      (256)           /* limit messages to 256 chars          */
#define ERROR        (-1)            /* ERROR return code for lcm func       */

/********* PROTOTYPES ********/
static int  print_weather(int);      /* weather func                         */
static int  print_news(int);         /* news func                            */
static int  print_time(int);         /* time func                            */
static int  print_admin(int, int);   /* admin report func                    */

/* Improper command line input can cause a help() invocation before
 * func_name pointer is initialized in the lcm() function */
int main(int argc, char *argv[])
{
  char welcome[MAX_MSG];             /* welcome message                      */
  char goodbye[] = "Goodbye!";       /* Goodbye message                      */
  int (*weatherp)(int);              /* ptf for weather command              */
  int (*newsp)(int);                 /* ptf for news command                 */
  int (*timep)(int);                 /* ptf for time command                 */
  int (*adminp)(int,int);            /* ptf for admin command                */
  int (*client_cmd)() = NULL;        /* client tells us where to point       */
  int ssd;                           /* Server listening socket              */
  int csd;                           /* Socket for client connection         */
  int port;                          /* port for server sock                 */
  socklen_t client_len;              /* byte size of client's address        */
  struct sockaddr_in server_addr;    /* server address                       */
  struct sockaddr_in client_addr;    /* client address                       */
  char  msg[MAX_MSG];                /* message buffer                       */
  int   n_bytes;                     /* num bytes read                       */
  int   opt_val;                     /* option to setsockopt                 */
  int   visitors;                    /* count number of requests             */

  /* make sure only port num was passed at cmd line */
  if( argc != 2 )
    {
      printf("server usage: %s port\n", argv[0]);
      exit(1);
    }


  /* initialize client supported fps  and visitors */
  newsp    = &print_news;
  weatherp = &print_weather;
  timep    = &print_time;
  adminp   = &print_admin;
  visitors = 0;

  /* get the port */
  port = (int)strtol(argv[1], (char **)NULL, 10);

  /* Make sure port number is between 8k and 10k */
  if( MAX_PORT < port || MIN_PORT > port )
    {
      printf("Please provide a port between %d and %d\n", MIN_PORT, MAX_PORT);
      exit(1);
    }



  /* build welcome message -- its just values to call each function */
  bzero( welcome, MAX_MSG );

  snprintf( welcome, (MAX_MSG-1), "%p %p %p %p",
            newsp, weatherp, timep, adminp );

  /* create the server socket */
  ssd = socket(AF_INET, SOCK_STREAM, 0);
  if( ssd < 0 )
    {
      printf("Error opening server sock, exiting...\n");
      exit(1);
    }

  /* need local socket reuse */
  opt_val = 1;
  if( 0 > setsockopt( ssd, SOL_SOCKET, SO_REUSEADDR,
		    ( const void * )&opt_val ,
                    ((socklen_t)sizeof(int))) )
    {
      printf("Error setting socket options, exiting...\n");
      exit(1);
    }

  /*
   * build the server's addr
   */
  bzero((char *) &server_addr, sizeof(server_addr));

  server_addr.sin_family = AF_INET;
  server_addr.sin_addr.s_addr = htonl(INADDR_ANY);

  /* this is the port we will listen on */
  server_addr.sin_port = htons((unsigned short)port);

  if ( bind( ssd, (struct sockaddr *) &server_addr,
	  ((socklen_t)sizeof(server_addr))) < 0 )
    {
      printf("Error binding on server socket\n");

      exit(1);
    }

  /* Only willing to handle 1 client */
  if( listen(ssd, 1) < 0 )
    {
      printf("Error listening on server socket. Exiting...\n");
      exit(1);
    }


  client_len = sizeof(client_addr);

  /* Main loop, take input and compute lcm */
  while( 1 )
    {
      csd = accept( ssd, (struct sockaddr *) &client_addr, &client_len);
      if( csd < 0 )
        {
          printf("Error accepting client conn. Exiting\n");
          close( ssd );
          exit(1);
        }

      /* track num visitors */
      visitors++;

      /* write welcome message to client */
      n_bytes = write( csd, welcome, strlen(welcome) );

      if( 0 > n_bytes )
        {
          printf("Error writing welcome msg to client, exiting...\n");
          close( ssd );
          close( csd );
          exit(1);
        }

      /* read input string from the client */
      bzero( msg, MAX_MSG );
      n_bytes = read( csd, &msg[0], MAX_MSG - 1 );//STONESOUP:INTERACTION_POINT

      if( 0 > n_bytes )
        {
          printf("Error reading from socket. Closing & Exiting...\n");
          close( csd );
          close( ssd );
          exit(1);
        }

      /* be sure that the string ends with a NUL byte */
      msg[MAX_MSG-1] = '\0';

      printf("server received %d bytes: %s\n", n_bytes, msg);

      /* determine command and dispatch appropriately*/
      if( 0 == strcmp( msg, "quit" ) )
        {
          printf("Quitting...\n");
          n_bytes = write( csd, goodbye, strlen(goodbye));
          if( 0 > n_bytes )
            {
              printf("Error writing goodbye to client, oh well...\n");
            }
          close( csd );
          break;
        }

      /* If we're still evaluating, we need to sscanf input for comparisons */
      sscanf( msg, "%p", (void**)&client_cmd );

      /* CWE-822: Just assign the value we parse and dereference, our good
       * server uses a whitelist for comparison before deref. */

      /* check if admin fucntion called so we pass it two arguments */
      if( adminp == client_cmd )//STONESOUP:CROSSOVER_POINT
        {
          n_bytes = (*client_cmd)(csd, visitors);
        }

      /* not admin function, all others take only one arg so just deref
       * and go! */
      else
        {
          n_bytes = (*client_cmd)(csd);//STONESOUP:TRIGGER_POINT
        }

      /* check return value */
      if( 0 > n_bytes )
        {
          printf("Error responding to client. Closing & Exiting...\n");
          close( csd );
          close( ssd );
          exit(1);
        }

      if( 0 < close( csd ) )
        {
          printf("Error closign our client socket in main loop\n");
          printf("We must be in a bad state. Exiting...\n");
          close( ssd );
          exit(1);
        }
  }

  close( ssd );
  return (0);
}


int print_weather( int sd )
{
  char weather[] =  "It's raining!";
  return ( write( sd, weather, strlen(weather)) );
}
int print_news( int sd )
{
  char news[] =  "Slow news day...";
  return ( write( sd, news, strlen(news)) );
}
int print_time( int sd )
{
  char time[] =  "It's Two:Thirty";
  return ( write( sd, time, strlen(time)) );
}
int print_admin( int sd, int visitors )
{
  char admin_str[MAX_MSG];

  snprintf( admin_str, MAX_MSG, "Hi Admin! Visitor count is %d", visitors );
  return ( write( sd, admin_str, strlen(admin_str)) );
}
