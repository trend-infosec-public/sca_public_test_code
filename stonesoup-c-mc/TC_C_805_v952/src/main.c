
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
** Date: Jul 29, 2011
**
** Base Test Program -- AddingLargeIntegers
**
** Summary: This program performs arbitrary precision addition and
**           subtraction. Input is read in from a file passed at the command
**           line. See the input constraints below.
**
**  Input:   The input must contain only one '+' or '-' sign. Negative operands
**           will not be supported, however negative answers can be returned.
**           Leading zeros are not supported in operands. If you try to use
**           them, it might still provide you the correct answer, but more
**           likely it will exit and error.
**
**           The input file should contain exactly the digits for the left
**           operand first, followed by a '+' or '-' character, followed by
**           the digits for the right operand. No spaces, newlines, or any
**           other characters are supported.
**
**           NOTE: There is a path traversal flaw here. Even if I remove
**           leading ".." and "/" sequences, it might still be possible to
**           reference a subdirectory of this folder and then include the
**           path manipulation characters after.
**
**  Run:     Build the code with 'gcc main.c'. Run the executable with the file
**           containing the expression as the first and only argument. Input
**           file formatting is described above.
**
** STONESOUP Weakness Class: <Buffer Overflows>
** CWE ID: CWE-805  Buffer Access with Incorrect Length Value
** 	Description Summary
** 		The software uses a sequential operation to read or write a buffer, but it
** 		uses an incorrect length value that causes it to access memory that is
** 		outside of the bounds of the buffer.
**
**	Extended Description
**		When the length value exceeds the size of the destination, a buffer
**		overflow could occur.
**
** Variant Spreadsheet Rev #: db
** Variant Spreadsheet ID: 952
**
** Variant Features:
**		SOURCE_TAINT:STDIN
**		DATA_TYPE:TYPEDEF
**		CONTROL_FLOW:SEQUENCE
**		DATA_FLOW:BUFFER_ADDRESS_ARRAY_INDEX
**
** Initial Testing: (x means yes, - means no)
**   Tested in MS Windows XP 32bit        x
**   Tested in MS Windows 7  64bit        -
**   Tested in Ubuntu10_10 Linux 32bit    x
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
** I/0 Pairs:
**   Good: 1st Set:
**         2nd Set:
**         3rd Set:
**         4th Set:
**         5th Set:
**    Bad: 1st Set:
*********************************************/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

/* Return codes */
#define SUCCESS (1)
#define ERROR   (-1)

/* Constant values */
#define MAX_LENGTH     (200)
#define INITIAL_R      (-1)
#define INITIAL_L      (-1)
#define ASCII_9        (57)
#define ASCII_0        (48)
#define ASCII_PLUS     (43)
#define ASCII_MINUS    (45)
#define EMPTY          (-1)
#define TRUE           (1)
#define FALSE          (0)

/* Swap left and right operand pointers */
void swap(char **left, char **right)
{
  char *tmp;

  tmp = *left;
  *left = *right;
  *right = tmp;
}

/* return neg if L<R, pos if L>R, 0 if L==R */
int compare( char L, char R )
{
  return ( L - R );
}

/* prune leading \0 on solution */
void trim_solution( char *solution )
{
  int MSD;                                 /* Most significant digit location*/
  int i, j;                                /* indexes for traversing solution*/

  /* test for 0 solution */
  if( solution[0] == ASCII_0 && solution[1] == '\0' )
    return;


  /* find first digit */
  for( i = 0; i < MAX_LENGTH; i++)
    if( '\0' != solution[i] && ASCII_0 != solution[i] )
      {
        MSD = i;
        break;
      }

  /* move all chars up by MSD digits */
  for( i = 0; i < MAX_LENGTH - MSD; i++ )
    solution[i] = solution[i + MSD];

  /* \0 terminate solution */
  solution[i] = '\0';

  if( ASCII_MINUS == solution[0] )
    /* one more pass to trip leading zeroes after a - sign */
    for( i = 1; i < MAX_LENGTH; i++ )
     if( ASCII_0 == solution[i] )
       for( j = i; j < MAX_LENGTH - 1; j++ )
         solution[j] = solution[j+1];
     else break;
}

/* do the actual math */
int compute( char *operand_left, char *operand_right, char operator, char *solution )
{
  int  i;                                   /* loop index                     */
  int  L = strlen(operand_left) - 1;        /* left operand index             */
  int  R = strlen(operand_right) - 1;       /* right operand index            */
  int  tmp;                                 /* int for swapping length values */
  int  carry  = 0;                          /* set to 1 for carrying add      */
  int  swapped = FALSE;                     /* indicates swapping L and R     */
  int  j;                                   /* auxiliary loop index           */

  /* if this is subtraction, make sure the L operand is larger, flip them
   * and make the answer negative if the R operand is bigger. */
  if( operator == ASCII_MINUS )
    {
      if( L == R )
        {
          for( i = 0; i < L; i++ )
            {
              /*printf("L[%d]=%c, R[%d]=%c\n", i, operand_left[i], i, operand_right[i] );*/
              /* test next digit */
              if( 0 == compare(operand_left[i], operand_right[i]) )
                {
                  continue;
                }
              /* L is smaller, swap */
              else if( 0 > compare(operand_left[i], operand_right[i]) )
                {
                  swap( &operand_left, &operand_right );
                  swapped = TRUE;
                  break;
                }
              /* L is larger, proceed as normal */
              else break;
            }
          /* Check to see if L and R are equal values --
           * this is indicated if i went over the loop and now equals L */
          if( i == L )
            {
              solution[0] = '0';
              solution[1] = '\0';
              return SUCCESS;
            }
        }
      else if( L < R )
        {
          swap( &operand_left, &operand_right );
          swapped = TRUE;
          tmp = L;
          L = R;
          R = tmp;
        }
    }

  /*printf("LHS: %s, RHS: %s, swapped: %d, L: %d, R: %d\n", operand_left,
         operand_right, swapped, L, R);*/

  for(i = MAX_LENGTH - 1; i >= 0; i-- )
    {
      /* check to see if we have exhausted either operand */
      if( 0 > R )
        {
          /* copy remaining L values into solution and return */
          while( L >= 0 )
            {
              solution[i] = operand_left[L];
              /*  6/8/11 - use ASCII_9 instead of 9 (test: regression-1) */
              if( carry && solution[i] < ASCII_9)
                {
                  solution[i] += 1;
                  carry = 0;
                }
              i--; L--;
            }
          if( carry )
            solution[i] = ASCII_0 + 1;
          else if ( TRUE == swapped )
            solution[i] = ASCII_MINUS;
          return SUCCESS;
        }
      if( 0 > L )
        {
          /* We still have digits in R, copy them to solution and return */
          /* Sannity check: If this is subtraction, there has been an error */
          if( ASCII_MINUS == operator )
            {
              printf("Error: L had fewer digits than R in a subtraction.\n");
              return ERROR;
            }
          /* copy remaining L values into solution and return */
          while( R >= 0 )
            {
              solution[i] = operand_right[R];
              if( carry && solution[i] < ASCII_9)
                {
                  solution[i] += 1;
                  carry = 0;
                }
              i--; R--;
            }
          if( carry )
            solution[i] = ASCII_0 + 1;
          else if ( TRUE == swapped )
            solution[i] = ASCII_MINUS;

          return SUCCESS;
        }

      /* both operands have digits left, just do the math */
      if( ASCII_MINUS == operator )
        {
          if( operand_left[L] >= operand_right[R] )
            {
              solution[i] = (operand_left[L]  - ASCII_0) -
                            (operand_right[R] - ASCII_0) + ASCII_0;
            }
          /* deal with the borrow case */
          else
            {
              for( j = L-1; j >= 0; j-- )
                {
                  if( operand_left[j] == ASCII_0 )
                    {
                      operand_left[j] = ASCII_9;
                    }
                  else
                    {
                      operand_left[j] -= 1;
                      break;
                    }
                  if( 0 == j )
                    {
                      /* This would imply that R was larger than L, error */
                      printf("Error: R was larger than L in a subtraction.\n");
                      printf("Possibly too many leading 0s\n");
                      return ERROR;
                    }
                }

              solution[i] = ((10 + operand_left[L]) - ASCII_0) -
                            (      operand_right[R] - ASCII_0) + ASCII_0;
            }
        }
      /* do the addition */
      else
        {
          if( ASCII_9 < ( (operand_left[L]  - ASCII_0) +
                          (operand_right[R] - ASCII_0) +
                           carry + ASCII_0 ) )
            {
              solution[i] = (operand_left[L]  - ASCII_0) +
                            (operand_right[R] - ASCII_0) +
                            carry - 10 + ASCII_0;
              carry = 1;
            }
          else
            {
              solution[i] = (operand_left[L]  - ASCII_0) +
                            (operand_right[R] - ASCII_0) + carry + ASCII_0;
              carry = 0;
            }

        }
        L--; R--;
    }

  printf("Error: We should never reach this in compute, sol: %s\n", solution);

  return ERROR;
}



/**
  * parse_input is a helper function that processes the input after it is
  * read in from a file. It will split the input into the left operand, the
  * right operand, and the operator to be used. It will return SUCCESS if the
  * input meets expectations, or ERROR if there is a problem parsing the input.
  * This function will also validate the input by testing ascii codes to make
  * sure that only digits and + or - are being passed in.
  */
int parse_input( char *input_buffer, char *operand_left, char *operand_right,
                 char *operator )
{
  int i;                                   /* loop index                     */
  int L = INITIAL_L;                               /* where to write in left operand */
  int R = INITIAL_R;                       /* where to write in rght operand */

  for( i = 0; i < MAX_LENGTH; i++ )
    {
      /* Check for \0 terminator */
      if( '\0' == input_buffer[i] )
        {
          /* handle both cases for no operator, and op with no right val */
          if( R != INITIAL_R && R != 0)
            return SUCCESS;
          else
            {
              printf("Error: terminator found before R hand operator was parsed, i=%d\n", i);
              return ERROR;
            }
        }


      /* validate ascii values */
      if(  ASCII_9     <  ((int)input_buffer[i]) ||
          (ASCII_0     >  ((int)input_buffer[i]) &&
           ASCII_PLUS  != ((int)input_buffer[i]) &&
           ASCII_MINUS != ((int)input_buffer[i]) )
        )
        {
          printf("Error, illegal value in input: %c\n", input_buffer[i]);
          return ERROR;
        }

      /* right index is -1 until we reach operator */
      if( INITIAL_R == R )
        {
          /* test for operator, if not then copy to L and move on */
          if( ASCII_PLUS  == ((int)input_buffer[i]) ||
              ASCII_MINUS == ((int)input_buffer[i]) )
            {
              *operator = input_buffer[i];
              if( INITIAL_L == L)
                {
                  printf("Error: operator found before left val parsed\n");
                  return ERROR;
                }
              R = 0;
            }
          else
            {
              /* If first value is a digit, safe to initialize L */
              if( INITIAL_L == L ) L = 0;
              operand_left[L++] = input_buffer[i];
            }
        }
      /* already found operator, just be sure its not another operator & copy to R */
      else if( ASCII_PLUS  == ((int)input_buffer[i]) ||
               ASCII_MINUS == ((int)input_buffer[i]) )
        {
          printf("Cannot use more than one operator in the input string.\n");
          printf("Found operator %c @ index %d\n", input_buffer[i], i);
          return ERROR;
        }
      else operand_right[R++] = input_buffer[i];
    }

  /* \0 terminate both operands */
  operand_left[L] = '\0';
  operand_right[R] = '\0';

  return SUCCESS;
}

/*  */
int getNums(char *buf, FILE *fp)	//STONESOUP:CONTROL_FLOW:SEQUENCE
{
	int num[3] = {1,0,1};
	typedef char newtype;
	newtype *c = (newtype *)buf + num[1];	//STONESOUP:DATA_TYPE:TYPEDEF	//STONESOUP:DATA_FLOW:BUFFER_ADDRESS_ARRAY_INDEX

	char *b = fgets(c, 1000, fp);	//STONESOUP:INTERACTION_POINT	//STONESOUP:CROSSOVER_POINT	//STONESOUP:TRIGGER_POINT
	if (!b || ferror(fp))
	{
		printf("input too large or error reading file, MAX_LENGTH=%d\n", MAX_LENGTH);
		return(1);
	}

	num[0] = strlen(c);
	if ((num[0] > 0) && ((c[num[0]-1] == '\n') || (c[num[0]-1] == '\r')))
	{
		num[0]--;
	}
	c[num[0]] = '\0';
	return(0);
}

/**
  * This program will take the first argument as the name of the input file.
  * It will open the input file and read in a string until it reaches
  * MAX_LENGTH or EOF. This signifies the end of the arithmetic string.
  * It will then begin processing the string and performing addition or
  * subtraction.
  */
int main( int argc, char *argv[] )
{
	FILE *input_file;                      /* Input file handle              */
	char *input_buffer;                    /* Buffer space to store input    */
	char operand_left[MAX_LENGTH];         /* left side of expression        */
	char operand_right[MAX_LENGTH];        /* right side of expression       */
	char  operator;                        /* operator for expression        */
	char solution[MAX_LENGTH + 1];         /* storage space for solution     */

	if( 1 != argc )
    {
      printf("%s\nExpected input via stdin\n", argv[0]);
      exit( ERROR );
    }

  /* NOTE: Absolutely not validation of filename. Path traversal possible */
  	input_file = stdin;	//STONESOUP:SOURCE_TAINT:STDIN

  /* check for error opening file */
  if( NULL == input_file )
    {
      exit( ERROR );
    }

  /* Allocate an extra byte so string can be '\0' terminated, set to 0*/
  input_buffer = malloc((MAX_LENGTH + 1) * sizeof(char));
  if( NULL == input_buffer )
    {
  		fclose(input_file);
      printf("Error allocating initial memory for input, exiting...\n");
      exit( ERROR );
    }

  memset( input_buffer, 0, (MAX_LENGTH + 1) * sizeof(char));
  memset( operand_left,  0, MAX_LENGTH * sizeof(char) );
  memset( operand_right, 0, MAX_LENGTH * sizeof(char) );
  memset( solution,      0, MAX_LENGTH * sizeof(char) + 1 );

  /* check for partial read */
  /* Read MAX_LENGTH + 1 to get EOF if input is exactly MAX_LENGTH*/
  if (getNums(input_buffer, input_file) != 0)
  {
  	fclose(input_file);
    free( input_buffer );
    exit( ERROR );
  }
	fclose(input_file);

  /* parse input string into left and right operands and operator */
  if( SUCCESS !=
      parse_input( input_buffer, operand_left, operand_right, &operator ) )
    {
      printf("There was an error parsing input, exiting...\n");
      free(input_buffer);
      exit( ERROR );
    }
  printf( "input %s, left %s, right %s, op %c\n", input_buffer, operand_left,
          operand_right, operator );

  /* process resulting expression */
  if( SUCCESS != compute( operand_left, operand_right, operator, solution ) )
    {
      printf("There was an error computing the solution, exiting...\n");
      free(input_buffer);
      exit( ERROR );
    }

  trim_solution( solution );

  printf("The solution is:\n");
  printf("%s\n", solution);

  free(input_buffer);

  return 0;
}

/* End of file */
