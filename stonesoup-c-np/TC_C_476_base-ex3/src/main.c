
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

**  Date: 24 March 12

**

**  Revision History

**  Date      Change

**  24 March 12 Initial 

**

**

** This program is the server end of the application that encrypts plaintext 

** or decrypts ciphertext for a given key using the Vigenere cipher. This 

** program contains CWE-476 (explicit dereference of a NULL pointer) when it 

** tries to copy a NULL pointer with strncpy. 

** 

** Error: In this implementation, we utilize a multidimensional array with the idea 

** that we want to use human indexing (i.e. the indices start at 1 not 0.  The NULL 

**pointer dereference will occur when we try to copy the contents of anything in row 

** 0 or column 0 of the Vigenere table since everyting starts at 1. This specifically 

** happens when you call encrypt PLAINTEXT KEY where PLAINTEXT or KEY contain the '@' 

** character an allowed character that will get you to row/column 0 in encrypt_lookup().  

** When this is done, it will return a NULL pointer which the strncpy() call in encrypt 

** will try to use causing a segmentation fault.

**

**

** STONESOUP Weakness Class: NULL Pointer Errors

** CWE ID: CWE-476

** Variant Spreadsheet Rev #:

** Variant Spreadsheet ID: 

**

** Variant Features:

**

**

** I/0 Pairs (should be passed to the client):

**   Good: 1st Set:encrypt HELLO WQPTA

**         2nd Set:decrypt DUAEO WQPTA

**         3rd Set:encrypt GOODBYE ABCDEFG

**         4th Set:decrypt GPQGFDK ABCDEFG

**         5th Set:

**    Bad: 1st Set:encrypt BAD@@ INPUT

**         2nd Set:

**

** How program works:

**     The program is run with ./tc_c_476_server

**     This program listens for connections on 127.0.0.1:7890.

**

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <errno.h>
#include <string.h>
#include <strings.h>
#include <unistd.h>

#define BUFFER_SIZE 1024
#define START_CHAR 65    /* 'A' starts at 65, but, let's use 64 because we want to use human indexing. */ 
#define ALPHABET_SIZE 26 /* Let's have our alphabet consist of the characters 'A' through 'Z'.
			    However, I am adding another character (i.e. 27 instead of 26) because 
			    I want to use human-style indexing.
			 */

/* This tells us what type of data we found and parsed from the command line.  */
enum INPUT {INPUT_ENCRYPT, /* Found and parsed plaintext and a key */
	    INPUT_DECRYPT, /* Found and parsed ciphertext and a key */
            INPUT_QUIT,    /* Found the quit command, let's shut down the server */
            INPUT_ERROR};  /* There was an error while parsing the data, let's quit */

/* This function parses the data and if successful will let us know if we have to quit
   or if we have the appropriate input to encrypt plaintext or decrypt ciphertext using
   the Vigenere cipher.
*/
enum INPUT parse_input(char*,char*,char*);

/* This function creates the Vigenere table. */
char*** init_table();

/* This function destroys the Vigenere table. */
void destroy_table(char***);

/* This function takes plaintext and uses the key to encrypt it using the Vigenere cipher. */
char* encrypt(char***,char*,char*);

/* This function performs lookups on the Vigenere table when encrypting plaintext. */
char* encrypt_lookup(char***,char,char);

/* This function takes Vigenere ciphertext and uses the key to decrypt it. */
char* decrypt(char***,char*,char*);

/* This function performs lookups on the Vigenere table when decrypting ciphertext. */
char decrypt_lookup(char***,char,char);

/* This function checks to see if two strings are equal based on their lengths and actual characters. */
int is_match(char*,char*);

int main(int argc, char* argv[]){

  int sockfd;
  /*printf("\nSERVER: starting the server\n");*/
  
  /* Create a socket */
  /*printf("SERVER: creating socket\n");*/
  if ((sockfd = socket(AF_INET, SOCK_STREAM, 0)) == -1){
    /*perror("SERVER: socket() failed");*/
    fprintf(stderr, "SERVER ERROR: couldn't create socket\n");
    exit(EXIT_FAILURE);
  }
  
  /*Allow this port to be reused*/
  int opts = 1;
  if (setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, (char *)&opts, sizeof(opts)) != 0) {
	    fprintf(stderr, "SERVER ERROR: couldn't set socketopt\n");
	    exit(EXIT_FAILURE);
  }

  /* Configure connection settings */
  struct sockaddr_in addr;
  char ipaddr[] = "127.0.0.1";
  addr.sin_family = AF_INET;
  addr.sin_port = htons(7890);
  /*printf("SERVER: configuring the server to listen on %s:%d\n",ipaddr,ntohs(addr.sin_port));*/
  if (inet_pton(AF_INET, ipaddr, &addr.sin_addr.s_addr) < 1){
    /*perror("inet_pton() failed");*/
    if (close(sockfd) == -1){
      /*perror("SERVER: close(server socket) failed");*/
    }
    fprintf(stderr, "SERVER ERROR: couldn't configure connection\n");
    exit(EXIT_FAILURE);
  }
  
  /* Bind the socket to an address */
  /*printf("SERVER: binding the server to %s:%d\n",ipaddr,ntohs(addr.sin_port));*/
  if (bind(sockfd,(struct sockaddr*)&addr,sizeof(addr)) == -1){
    /*perror("SERVER: bind() failed");*/  
    if (close(sockfd) == -1){
      /*perror("SERVER: close(server socket) failed");*/
    }
    fprintf(stderr, "SERVER ERROR: couldn't bind to the socket\n");
    exit(EXIT_FAILURE);
  }
  
  /* Listen for incoming connections */
  /*printf("SERVER: listening for an incoming connection\n");*/
  if (listen(sockfd,SOMAXCONN) == -1){
    /*perror("SERVER: listen() failed");*/
    if (close(sockfd) == -1){
      /*perror("SERVER: close(server socket) failed");*/
    }
    fprintf(stderr, "SERVER ERROR: couldn't listen for incoming connections\n");
    exit(EXIT_FAILURE);
  }
  
  int clisock;
  struct sockaddr_in cliaddr;
  socklen_t clilength = sizeof(cliaddr);
  
  char*** table = NULL;
  if ((table = init_table()) == NULL){
    /*perror("malloc() failed");*/
    if (close(sockfd) == -1){
      /*perror("SERVER: close(server socket) failed");*/
    }
    fprintf(stderr, "SERVER ERROR: couldn't init table\n");
    exit(EXIT_FAILURE);
  }
    
  /* Handle incoming connections */
  while(1){
    /*printf("SERVER: waiting for a connection from a client\n");*/ 
    if ((clisock = accept(sockfd,(struct sockaddr*)&cliaddr,&clilength)) == -1){
      /*perror("SERVER: accept() failed");*/
      if (close(sockfd) == -1){
	/*perror("SERVER: close(server socket) failed");*/
      }
      destroy_table(table);
      fprintf(stderr, "SERVER ERROR: couldn't accept connection\n");
      exit(EXIT_FAILURE);
    }

    /* Get information about the connecting client */
    char addrbuf[INET_ADDRSTRLEN]; 
    /*printf("SERVER: getting connection information about the client\n");*/
    if (inet_ntop(AF_INET,&((&cliaddr)->sin_addr),addrbuf,INET_ADDRSTRLEN) == NULL){
      /*perror("SERVER: inet_ntop() failed");*/
      if (close(clisock) == -1){
	/*perror("SERVER: close(client socket) failed");*/
      }
      if (close(sockfd) == -1){
	/*perror("SERVER: close(server socket) failed");*/
      }
      destroy_table(table);
      fprintf(stderr, "SERVER ERROR: couldn't set client address\n");
      exit(EXIT_FAILURE);
    }

    /*printf("SERVER: accepted a connection from the client %s:%d\n",addrbuf,ntohs(cliaddr.sin_port));*/

    /* Read data from the client */
    char buffer[BUFFER_SIZE];
    bzero(buffer,BUFFER_SIZE);
    
    /*printf("SERVER: reading data from the client\n");*/
    if (read(clisock,buffer,BUFFER_SIZE) == -1){ // STONESOUP:INTERACTION_POINT  // STONESOUP: CROSSOVER_POINT
      /*perror("SERVER: read failed()");*/
      if (close(clisock) == -1){
	/*perror("SERVER: close(client socket) failed");*/
      }
      if (close(sockfd) == -1){
        /*perror("SERVER: close(server socket) failed");*/
      }
      destroy_table(table);
      fprintf(stderr, "SERVER ERROR: couldn't read data\n");
      exit(EXIT_FAILURE);
    }
    
    /*printf("SERVER: read %s from client\n",buffer);*/    
    
    char inputtext[BUFFER_SIZE];
    char key[BUFFER_SIZE];
    char* outputtext = NULL;
    
    bzero(inputtext,BUFFER_SIZE);
    bzero(key,BUFFER_SIZE);

    /* Let's parse the data that we retrieved from the client and make sure it makes sense.
    */
    /*printf("SERVER: parsing the data from the client\n");*/
    enum INPUT input = parse_input(buffer,inputtext,key);
    if ((input == INPUT_ERROR) || (input == INPUT_QUIT)){
      if (close(clisock) == -1){
        /*perror("SERVER: close(client socket) failed");*/
      }
      if (close(sockfd) == -1){
        /*perror("SERVER: close(server socket) failed");*/
      }
      
      /* Clean up some memory that we have already allocated */
      destroy_table(table);

      if (input == INPUT_ERROR){
    	  fprintf(stderr, "SERVER ERROR: couldn't parse input\n");
        exit(EXIT_FAILURE);
      }else{ /* Found the quit command let's clean up the memory and exit */
        /*printf("SERVER: closing the server\n");*/
        exit(EXIT_SUCCESS);
      }
    }

    /*We found the encrypt command, let's determine the ciphertext of the plaintext using the specified key */
    if(input == INPUT_ENCRYPT){
      /*printf("SERVER: determining the ciphertext for the plaintext %s using key %s\n",inputtext,key);*/
      outputtext = encrypt(table,inputtext,key);
    }

    /*We found the decrypt command, let's determine the plaintext of the ciphertext using the specified key */
    if(input == INPUT_DECRYPT){
      /*printf("SERVER: determining the plaintext for the ciphertext %s using key %s\n",inputtext,key);*/
      outputtext = decrypt(table,inputtext,key);
    }

    /* Now that we have our output text (plaintext or ciphertext), let's copy the it into the buffer so that 
       we can send it back to the client 
    */

    fprintf(stdout, "text to send to client = %s\n", outputtext);
    fflush(stdout);

    bzero(buffer,BUFFER_SIZE);
    if( outputtext != NULL ){
      strcpy(buffer,outputtext);
      /* Free allocated memory */
      free(outputtext);
      outputtext = NULL;
    }else{ /* Exit if we were not able to get a result */
      /*perror("SERVER: write() failed");*/
      if (close(clisock) == -1){
        /*perror("SERVER: close(client socket) failed");*/
      }
      if (close(sockfd) == -1){
        /*perror("SERVER: close(server socket) failed");*/
      }
      fprintf(stderr, "SERVER ERROR: no output text\n");
      exit(EXIT_FAILURE);
    }
    
    /* Send the data to the client */
    /*printf("SERVER: writing %s to the client\n",buffer);*/
    if (write(clisock,buffer,BUFFER_SIZE) == -1){
      /*perror("SERVER: write() failed");*/
      if (close(clisock) == -1){
	/*perror("SERVER: close(client socket) failed");*/
      }
      if (close(sockfd) == -1){
        /*perror("SERVER: close(server socket) failed");*/
      }
      fprintf(stderr, "SERVER ERROR: couldn't write to socket\n");
      exit(EXIT_FAILURE);
    }
    
    /* Closing the socket used to communicate with the client */
    /*printf("SERVER: closing the client socket\n");*/
    if (close(clisock) == -1){
      /*perror("SERVER: close(client socket) failed");*/
      if (close(sockfd) !=0){
	/*perror("SERVER: close() failed");*/
      }
      fprintf(stderr, "SERVER ERROR: couldn't close the socket\n");
      exit(EXIT_FAILURE);
    }
  }
 
  return 0;
}

/* This function parses the data from the client to make sure we got something we can recognize. */
enum INPUT parse_input(char* buffer,char* inputtext,char* key){
  
  /*printf("SERVER: parsing the data from the client\n");*/
  
  if (is_match("quit",buffer)){
    /*printf("SERVER: found the quit command, the server will be shutting down momentarily\n");*/
    return INPUT_QUIT;
  }else{ /* If we didn't find the quit command, let's try to parse it as if we have our operation, input text, and key */
    
    char* operationptr = strtok(buffer," ");
    char* inputtextptr = strtok(NULL," ");
    char* keyptr = strtok(NULL," ");

    /* Before we go ahead, let's make sure we were able to get values for all of the strings. */
    if( (operationptr != NULL) && (inputtextptr != NULL) && (keyptr != NULL) ){     
      strncpy(inputtext,inputtextptr,strlen(inputtextptr)+1);
      strncpy(key,keyptr,strlen(keyptr)+1);
    }else{
      /*printf("SERVER: there was an error parsing '%s'. You need to specify 'operation plaintext/ciphertext key'.\n",buffer);*/ 
      return INPUT_ERROR;
    }

    if (is_match("encrypt",operationptr)){
      /*printf("SERVER: found plaintext %s and key %s\n",inputtext,key);*/
      return INPUT_ENCRYPT;
    }else if (is_match("decrypt",operationptr)){
      /*printf("SERVER: found ciphertext %s and key %s\n",inputtext,key);*/
      return INPUT_DECRYPT;
    }else{
      /*printf("SERVER: %s is an unrecognized operation. Only 'encrypt' and 'decrypt' are allowed.\n",operationptr);*/
      return INPUT_ERROR;
    }
  }
}

/* This function creates the Vigenere table. */
char*** init_table(){
  int i,j;

  /* Initialize memory for the table */
  char*** table = (char***)malloc((ALPHABET_SIZE+1)*sizeof(char*));
  bzero(table,(ALPHABET_SIZE+1)*sizeof(char*));
  for(i=0;i<ALPHABET_SIZE+1;i++){
    table[i] = (char**)malloc((ALPHABET_SIZE+1)*sizeof(char*));
    bzero(table[i],(ALPHABET_SIZE+1)*sizeof(char*));
    for(j=0;j<ALPHABET_SIZE+1;j++){
      if( i > 0 && j > 0 ){
	table[i][j] = (char*)malloc(2);
	bzero(table[i][j],2);
      }else{/* We don't use these so don't allocate any space for them */
	table[i][j] = NULL;
      }
    }
  }
  
  /* Initialize the table values */
  for(i=1;i<ALPHABET_SIZE+1;i++){
    for(j=1;j<ALPHABET_SIZE+1;j++){
      table[i][j][0] = (( (i-1) + (j-1) ) % ALPHABET_SIZE ) + START_CHAR;
      table[i][j][1] = '\0';
    }
  }
  
  return table;
}

/* This function destroys the Vigenere table. */
void destroy_table(char*** table){
  int i,j;

  for(i=1;i<ALPHABET_SIZE+1;i++){
    for(j=1;j<ALPHABET_SIZE+1;j++){
      free(table[i][j]);
    }
    free(table[i]);
  }
  free(table);
}

/* This function performs lookups on the Vigenere table when encrypting plaintext. */
char* encrypt_lookup(char*** table,char plaintext,char key){
  int row = ( ((int)key) + 1 - START_CHAR );
  int col = ( ((int)plaintext) + 1 - START_CHAR );
  return table[row][col];
}

/* This function takes plaintext and uses the key to encrypt it using the Vigenere cipher. */
char* encrypt(char*** table,char* plaintext,char* key){
  int i, length;
  char *tmp;
  
  length = strlen(plaintext);
  tmp = (char*)malloc(length+1);
  
  for(i = 0;i < length;i++){
    char* c =  encrypt_lookup(table,plaintext[i],key[i]);
    strncpy(&tmp[i],c,1); // STONESOUP:TRIGGER_POINT
  }
  
  /* Add null byte at the end of the newly created string */
  tmp[length] = '\0';
 
  return tmp;
}

/* This function performs lookups on the Vigenere table when decrypting ciphertext. */
char decrypt_lookup(char*** table,char ciphertext,char key){
  int i;
  int row = ( ((int)key) + 1 - START_CHAR );
 
  /* Return '\0' if row is less than 1 - we are using human indexing here. */
  if ( row < 1 ){
    return '\0';
  }
 
  for(i = 1;i < ALPHABET_SIZE+1; i++){
    if (table[row][i][0] == ciphertext){
      break;
    }
  }
  /* Return '\0' if we can't find a match */
  if ( i == (ALPHABET_SIZE+1) ){
    return '\0';
  }else{
    return (char)((i-1)+START_CHAR);
  }
}

/* This function takes Vigenere ciphertext and uses the key to decrypt it. */
char* decrypt(char*** table,char* ciphertext,char* key){
  int i,length;
  char *tmp;
  
  length = strlen(ciphertext);
  tmp = (char*)malloc(length+1);

  for(i = 0;i < length;i++){
    char c = decrypt_lookup(table,ciphertext[i],key[i]);
    /* Clean up allocated memory and return NULL indicating there was an error */
    if ( c == '\0' ){
      if ( tmp != NULL ){
	free(tmp);
	tmp = NULL;
      }
      return tmp;
    }
    tmp[i] = c;
  }
  
  /* Add null byte at the end of the newly created string */
  tmp[length] = '\0';
  return tmp;
}

/* This function checks to make sure that the two strings are both the same length and that they are 
   the same for their entire length.
*/
int is_match(char* str1, char* str2){
  int length;
  if ((length = strlen(str1)) == strlen(str2)){
    if (strncmp(str1,str2,length) == 0){
      return 1;
    }
  }
  return 0;
}
