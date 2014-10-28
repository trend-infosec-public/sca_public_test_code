
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
**  Date: 21 Mar 12
**
**  Revision History
**  Date      Change
**  21 Mar 12 Initial 
**  22 Mar 12 Changed input to come from socket instead of cmd line
**
**
**  Main is a command line utility that will convert arguments passed at
**  the command line and turn them into positive integers. 
**  Our target weakness for this test case is a chain from 
**  CWE-824 (uninitialized variable) -> CWE-476 (null pointer dereference).
**  The main routine will first call a function that attempts to zero out
**  the stack space that will be allocated for our target function, lcm (least
**  common multiple). The intention is to do our best to make sure the
**  uninitialized variable will contain a null pointer when dereferenced
**  without introducing other weaknesses. Once the environment is setup,
**  the arguments are parsed and the least common multiple is computed.
**
**
**  Program outputs the least common multiple of the integers passed at
**  the command line.
**
**
**  Error: The uninitialized variable is a shortcut pointer from
**  "char *func_name" to argv[0]. The variable is initialized after the first
**  check for an error condition (not enough or too many arguments). If the
**  first error condition is triggered, a usage function will be called which
**  dereferences the func_name shortcut and causes a crash. The variable is
**  initialized after the first error check, so subsequent errors will not
**  not trigger a null pointer dereference. 
**
**  NOTES: On some platforms, the printf function will replace null pointers
**  with the text "(null)", therefore I've replaced a printf call with a write
**  to stdout. The strlen call embedded in the write actually triggers the npd.
**  If this is a problem, I have a few other ideas for control flows
**  that could leave the number_list pointer uninitialized.
**
** STONESOUP Weakness Class: NULL Pointer Errors
** CWE ID: CWE-476
** Variant Spreadsheet Rev #: ###
** Variant Spreadsheet ID: ###
**
** Variant Features:
**
** SEE CLIENT FOR I/O Pairs -- feel free to change port.
** I/0 Pairs:
**   Good: 1st Set: ./server 9876
**         2nd Set: ./server 9876
**         3rd Set:
**         4th Set:
**         5th Set:
**    Bad: 1st Set:
**             2nd Set:
**
** How program works:
** This is a server program that takes exactly one argument, the port
** number for listening. It then reads in one command from a connecting
** client, parses it on spaces into an argument list and calculates
** the least common multiple of integers passed.
************************************************************************/

/******** INCLUDES ********/
#include <stdio.h>                   /* FileIO                               */
#include <stdlib.h>                  /* memory allocation                    */
#include <strings.h>                 /* bzero                                */
#include <string.h>                  /* strlen                               */
#include <unistd.h>                  /* write function -- printf handles null*/
#include <sys/socket.h>              /* socket IO                            */
#include <arpa/inet.h>               /* htonl                                */

/********* DEFINES ********/
#define MAX_ARGS     (5)             /* cap # of args than can come in       */
#define MAX_ARG_LEN  (64)            /* cap size of args to be passed        */
#define LARGEST_VAL  (56)            /* arbitrary cutoff for supported vals  */
#define MAX_PORT     (10000)         /* arbitrary cutoff for max port num    */
#define MIN_PORT     (8000)          /* stay clear of low port nums          */
#define MAX_MSG      (256)           /* limit messages to 256 chars          */
#define ERROR        (-1)            /* ERROR return code for lcm func       */
#define FN_SZ        (32)            /* small cap on filename size           */
#define FO_SZ        (64)            /* small cap on file contents           */

/********* PROTOTYPES ********/
static void help( char* );           /* print usage of function              */
static int  find_max( int*, int );   /* find max integer in a list           */
static void zero_stack_space( void );/* zero space to improve repeatability  */
static int  lcm( int, char *[] );    /* do the work for LCM                  */
static int  int_pow( int, int );     /* power utility function               */

/* Improper command line input can cause a help() invocation before
 * func_name pointer is initialized in the lcm() function */
int main(int argc, char *argv[])
{
  int lcm_value;                     /* integer used to store return val     */
  int ssd;                           /* Server listening socket              */
  int csd;                           /* Socket for client connection         */
  int port;                          /* port for server sock                 */
  socklen_t client_len;              /* byte size of client's address        */
  struct sockaddr_in server_addr;    /* server address                       */
  struct sockaddr_in client_addr;    /* client address                       */
  char  msg[MAX_MSG];                /* message buffer                       */
  int   n_bytes;                     /* num bytes read                       */
  int   opt_val;                     /* option to setsockopt                 */
  int   m_argc;                      /* num args rcvd from client            */
  char *m_argv[MAX_ARGS];            /* array of args rcvd from client       */
  int   i;                           /* loop index for parsing input         */
  int   j;                           /* loop index for calls to free         */
  char *token;                       /* token pointer for parsing input      */
  int   conn_num;                    /* track num connex for file names      */
  FILE *fp;                          /* write output to file                 */
  char  fn[FN_SZ];                   /* file name to write our output        */
  char  foutput[FO_SZ];              /* contents to write to file            */

  /* turn off buffering in stdio... */
  setbuf( stdout, NULL );

  /* NULL must be 0 or this example is broken */
  if( 0x00000000 != NULL )
    {
      printf("NULL != 0, this example will not work in this environment\n");
      exit(1);
    }


  /* make sure only port num was passed at cmd line */
  if( argc != 2 )
    {
      printf("server usage: %s <port>\n", argv[0]);
      exit(1);
    }

  port = (int)strtol(argv[1], (char **)NULL, 10);

  /* Make sure port number is between 8k and 10k */
  if( MAX_PORT < port || MIN_PORT > port )
    {
      printf("Please provide a port between %d and %d\n", MIN_PORT, MAX_PORT);
      exit(1);
    }

  /* initialize num connections to 0 */
  conn_num = 0;

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

  if( bind( ssd, (struct sockaddr *) &server_addr, 
	  ((socklen_t)sizeof(server_addr))) < 0 )
    {
      printf("Error binding on server socket, exiting...\n");
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

      /* increment num visitors */
      conn_num++;

      /* read input string from the client */
      bzero( msg, MAX_MSG );
      n_bytes = read( csd, &msg[0], MAX_MSG );//STONESOUP:INTERACTION_POINT

      if( 0 > n_bytes )
        {
          printf("Error reading from socket. Closing & Exiting...\n");
          close( csd );
          close( ssd );
          exit(1);
        }

    //printf("server received %d bytes: %s", n_bytes, msg);

    
    /* initialize argument counter and parse first token */
    m_argc = 0;
    token = strtok( msg, " \0" );

    /* parse string and call lcm -- NOTE this will never allow too
     * many args to be passed. Therefore, the cap on args check in
     * in lcm function is just a sanity check */
    for( i = 0; i < MAX_ARGS; i++ )
      {
        /* make sure we got a token */
        if( NULL == token )
          {
            break;
          }

        /* make space to copy the token + NUL byte to our arg list */
        m_argv[i] = (char*) malloc( (strlen(token)+1) * sizeof(char) );
        m_argc++;

        /* check malloc return value */
        if( NULL == m_argv[i] )
          {
            printf("Error on call to malloc, exiting\n");
            close( csd );
            close( ssd );
            exit( 1 );
          }

        /* copy and set last char to NUL byte */
        strncpy( m_argv[i], token, strlen(token)+1);
        m_argv[i][strlen(token)] = '\0';

        /* get the next token */
        token = strtok( NULL, " \0" );
      }    


  /* we call this function hoping to make all of our local vars zero */
  zero_stack_space();

  /* do the LCM work -- hopefully on the same stack frame as above */
  lcm_value = lcm( m_argc, m_argv );

  /* we need to free all of our allocs */
  for( j=0; j < m_argc; j++ )
    {
      free( m_argv[j] );
    }


  /* No point in writing back to client. Printing in server makes
   * error condition easier to see and test. Could make this a write
   * for more realism */
  if( ERROR != lcm_value )
    {
      fprintf(stdout, "LCM is %d", lcm_value);
      fflush(stdout);

      /*
      // Also adding code to write to file as TH workaround

      bzero(fn, FN_SZ ); bzero( foutput, FO_SZ );
      snprintf( fn, FN_SZ, "output%d.txt", conn_num );
      snprintf( foutput, FO_SZ, "LCM is %d!\n", lcm_value );

      fp = fopen(fn, "w");
      if( NULL == fp )
        {
          printf("Unable to open file to write output... exiting\n");
          exit(1);
        }

      fprintf( fp, "%s", foutput );

      if( 0 != fclose(fp) )
        {
          printf("Error write output to file. Exiting...\n");
          exit(1);
        }
        */
    }
  else
    {
      /* error condition encountered, break while(1) */
      break;
    }
    
    close( csd );
  }

  close( ssd );
  return (0);
}

void zero_stack_space()
{
  /* allocate locals of matching size to LCM */
  char zero_me[ sizeof( int* ) + (5 * sizeof( int  )) + sizeof( char* ) ];

  /* zero locals */
  bzero(&zero_me, sizeof(zero_me) );

}

int lcm(int argc, char **argv)
{
  int  *number_list;                 /* numbers passed at cmd line           */
  char *func_name;                   /* first arg off list                   */
  int   i;                           /* loop index                           */
  int   j;                           /* nested loop index                    */
  int   max;                         /* maximum value passed at cmd line     */
  int   lcm_value;                   /* calculate lcm here                   */
  int   outer_max;                   /* bound on outer loop for search       */


  /* verify argc is less than max and greater than 1 */
  if( MAX_ARGS < argc || 1 >= argc )
    {
      printf("Improper invocation\n");
      help( func_name );//STONESOUP:CROSSOVER_POINT
      printf("Exiting...\n");
      return ERROR;
    }

  /* shortcut to function name -- execute this before first help call
   * to avoid CWE-824->CWE-476 */
  func_name = argv[0];

  /* make space to copy the argument list */
  number_list = (int*) malloc( (argc-1) * sizeof( int ) );

  /* verify return val from malloc */
  if( NULL == number_list )
    {
      printf("Error allocating memory, exiting...\n");
      return ERROR;
    }

  /* parse numbers from command line into list */
  for( i = 1; i < argc; i++ )
    {
      number_list[i-1] = (int)strtol(argv[i], (char **)NULL, 10);

      /* check for big values and reduce them to LARGEST_VAL */
      if( LARGEST_VAL < number_list[i-1] )
        {
          number_list[i-1] = LARGEST_VAL;
        }

      /* check for 0 & negative values and quit */
      if( 0 >= number_list[i-1] )
        {
          printf("Error, positive ( >0 ) ints only. Exiting...\n");
          help( func_name );

          if( NULL != number_list )
            {
              free( number_list );
              number_list = NULL;
            }

          return ERROR;
        }
    }

  /* calculate LCM -- Ugly Brute Force Algorithm:
   * get the maximum value from the list
   * iterate through all numbers and mod checking for divisibility
   * if you fail, break, add max to lcm and try again */
  max = find_max( number_list, argc-1 );

  /* check for errrors */
  if( ERROR == max )
    {
      /* already printed error message, free resources and return */
      free( number_list );
      number_list = NULL;
      return ERROR;
    }

  lcm_value = max;
  outer_max = int_pow( max, MAX_ARGS );

  /* check for errors */
  if( ERROR == outer_max )
    {
      /* already printed error message, free resources and return */
      free( number_list );
      number_list = NULL;
      return ERROR;
    }

  for( i = 0; i < outer_max; i++ )
    {
      for( j = 0; j < argc-1; j++ )
        {
          if( 0 != (lcm_value % number_list[j]) )
            {
              /* current test value not a multiple */
              break;
            }
        }

      /* test for success */
      if( j == (argc-1) )
        {
          //printf("LCM successfully found, ");
          if( NULL != number_list )
            {
              free( number_list );
              number_list = NULL;
            }
          return lcm_value;
        }

      /* increment by max and loop again */
      lcm_value += max;
    }

  /* sanity check, we should never reach here */
  printf("ERROR: Code shouldn't reach this point. LCM could not be found\n");

  if( NULL != number_list )
    {
      free(number_list);
      number_list = NULL;
    }

  return ERROR;
}

/* prints usage -- not the cleanest way to get a null deref but printf
 * family can convert NULL pointers to "(null)" strings */
void help( char *function_name )
{
  int write_len;
  printf("function invoked incorrectly, usage:\n");

  write_len = write( 1, function_name, strlen(function_name) );//STONESOUP:TRIGGER_POINT
  if( 0 > write_len )
    {
      /* just an error writing function name to std out
       * no need to do anything other than notify user */
      printf("Error writing function name to stdout\n");
    }

  printf(" +int [+int] [+int] ...\n");
  printf("Up to %d args allowed including func name\n", MAX_ARGS);
}



/* find max integer in a list */
int find_max( int *list, int len )
{
  int i;                             /* loop index                           */
  int max;                           /* return value                         */

  if( NULL == list )
    {
      printf("ERROR: Null value passed to find_max function, exiting...\n");
      return ERROR;
    }

  max = -1;
  for( i = 0; i < len; i++ )
    {
      if( max < list[i] )
        {
          max = list[i];
        }
    }

  return max;
}

/* helper function to raise first arg to power of second arg 
 * both arguments must be positive integers */
int int_pow( int base, int power )
{
  int ret_val = base;
  
  if( 0 >= base || 0 >= power )
    {
      printf("Arguments to pow must be positive integers, exiting...\n");
      return ERROR;
    }

  /* base must be <= LARGEST_VAL and power must be <= MAX_ARGS */
  if( LARGEST_VAL < base || MAX_ARGS < power )
    {
      printf("Arguments too large, risk integer overflow or other badness\n");
      printf("exiting...\n");
      return ERROR;
    }

  while( (--power) )
    {
      ret_val *= base;
    }
 
  return ret_val; 
}
