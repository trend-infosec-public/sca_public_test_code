
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
** Date: 9/19/11
**
** Base Test Program -- recaman.c
** Program takes a number to calculate the recaman sum.
**
** Variant Test Case Program
** Modified the program to take in an additional parameter as the index.  Using the index the program will print out every step of the
** recman sum process until <index>+i==<sequence_number>.   The program takes the following parameters <sequence_number> <index>  The
** index is not properly validated prior to its use. So entering a large negative number will allow anyone to view a large amount of
** memory.
**
** STONESOUP Weakness Class: Buffer Overflow
** CWE ID: CWE-129
** Variant Spreadsheet Rev #: 1111
** Variant Spreadsheet ID: BASE TEST CASE
**
** Variant Features:
**		SOURCE_TAINT:REGISTRY_CONTENTS
**		DATA_TYPE:ARRAY_LENGTH_LINEAR_EXPRESSION
**		CONTROL_FLOW:ELSE_CONDITIONAL
**		DATA_FLOW:PASS_BY_REFERENCE
**
** Initial Testing: (x means yes, - means no)
**   Tested in MS Windows XP 32bit        x
**   Tested in MS Windows 7  64bit        x
**   Tested in Ubuntu10_10 Linux 32bit    x
**   Tested in Ubuntu10_10 Linux 64bit    -
**
** Workflow:
**   Created:9/19/11
**   1st Vett:  on 7/18/11
**   2nd Vett: <peer> on <date>
**   3rd Vett:
**   4th Vett: Fortify Issues Fixed on <date>
**   5th Vett: Tested in Harness on <date>
**
**
** I/0 Pairs:
**   Good: 1st Set: 20000 10  registry set to: 1200
**         2nd Set: 200 10 registry set to: 1200
**    Bad: 1st Set: 1200 -3709 registry set to: 1000
**       Bad LINUX: 1200 -500  registry set to: 1000
*********************************************/

//Recaman's Sequence Generator
#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <assert.h>
#include "getInt.h"

#ifdef _WIN32
	#include <windows.h>
#else
	#include <stdarg.h>
	#include "regfuncs.h"
	typedef unsigned long DWORD;
#endif

//Function prototypes
int mygeti (int *);
int recaman(char, int, int, int, int);
int inSequence(int, int *, int);

//Securely read in the input number
//Buff[7] means that a maximum of 6 digits can be read
//Any input with a 6 digits in the beginning is valid
//e.g. 123456fffff is valid but ff123456 and 1234ff6234234 are not

int mygeti(int *result)
{
   char *end, buff [7];
   fgets(buff, sizeof buff, stdin);
   *result = (int) strtol(buff, &end, 10);
   fflush(stdin);
   return !isspace(*buff) && end != buff && (*end == '\n' || *end == '\0');
}

int main(int argc, char *argv[])
{
	printf("*************************************\n");
	printf("*** Recaman's Sequence Generator  ***\n");
	printf("*************************************\n\n");
	int value, index, end;

	if(argc != 4)
	{
		printf("Incorrect number of parameters: <sequence_number> <start_index> <output file #>\n");
		exit(1);
	}
	else if ((*argv[3] < '0') || (*argv[3] > '9'))
	{
		printf("File # must be a single digit: 0 - 9\n");
		exit(1);
	}

	char regString[512];

	HKEY regKey;
	if (RegOpenKeyEx(HKEY_CURRENT_USER, "Software\\STONESOUP\\BufIndex", 0, KEY_READ, &regKey) == ERROR_SUCCESS) {	//STONESOUP:INTERACTION_POINT	//STONESOUP:SOURCE_TAINT:REGISTRY_CONTENTS
		DWORD stringSize = sizeof(regString);
		if (RegQueryValueEx(regKey, "InputData", 0, NULL, (LPBYTE)regString, &stringSize) == ERROR_SUCCESS) {

		}
		printf("InputData: %s\n", regString);
	}

	value = (int) atoi(argv[1]);
	index = (int) atoi(argv[2]);
	end = (int) atoi(regString);	//STONESOUP:CROSSOVER_POINT

   //-1 is the "magic" value that will quit out of the while loop
   if (value == -1) {
	   printf("Invalid number %d\n", value);
	   exit(1);
   }
   //Otherwise, send the value to the recaman function if its greater than zero
   else if (value >= 0) {

	   printf("Answer: %d\n\n", recaman(*argv[3], value, index, end, 5000));
	   //for(i = 1; i<=value; i++){
		  // printf("For %d Answer: %d\n\n", i, recaman(i, index));
	  // }

   }
   else {
	   printf("Error: Please enter zero or a positive integer.\n\n");
   }


   return 0;
}

//Calculate the actual sequence
//A(0) = 0. a(m) = a(m-1) - 1 if a(m) is positive and not already in the sequence, otherwise a(m) = a(m-1) + m.
//The first few numbers in the Recaman's Sequence is 0, 1, 3, 6, 2, 7, 13, 20, 12, 21, 11, 22, 10, 23, 9.
int recaman(char filnum, int sequenceNo, int index, int end, int k)
{
	int prebuf[k];
	int sequence[sequenceNo + 1];
	int pstbuf[k];
	int i;
	int currentValue;
	int outputValue = 0;
	FILE *otfil = NULL;
	char filnam[] = "0output.bin";

	filnam[0] = filnum;
	memset(sequence, 0, sizeof(int) * (sequenceNo + 1));
	memset(prebuf, 'A', sizeof(int) * k);
	memset(pstbuf, 'A', sizeof(int) * k);
	prebuf[k - 1] = 0;
	pstbuf[k - 1] = 0;

	//Setup the sequence precondition
	//a(0) = 0
	sequence[0] = 0;

	for (i = 1; i <= sequenceNo; i++) {
		currentValue = (sequence[i-1] - i);

		//Do the > 0 check:
		if(currentValue > 0) {
			//Check if the value has already been seen
			if (!inSequence(currentValue, sequence, sequenceNo)) {	//STONESOUP:DATA_FLOW:PASS_BY_REFERENCE
				//Add it to the sequence if not
				sequence[i] = currentValue;
			}
			else {
				//Otherwise, do the addition method and add it to the sequence
				sequence[i] = (sequence[i-1] + i);
			}
		}
		//If the value is <0 then do the addition method and add it to the sequence
		else {
			sequence[i] = (sequence[i-1] + i);
		}
	}
	/*Create a new output.txt file so we can put all the index information into it. Otherwise we will not be able to read it because of
	  MAX buffer on CMD window
	*/
	if ((otfil = fopen(filnam, "wb")) == NULL)
	{
		fprintf(stderr, "Unable to create output file because of error\n");
		return(1);
	}else{	//STONESOUP:CONTROL_FLOW:ELSE_CONDITIONAL
		//Set the output value
		outputValue = sequence[sequenceNo];
		//output array to be sent to a new file
		int b = getInt(75);	// FEATURE:PASS_BY_VALUE
		char newoutput[(b*5+2)/5];	//STONESOUP:DATA_TYPE:ARRAY_LENGTH_LINEAR_EXPRESSION

		i = 1;
		for(i=0;(index+i)<=end; i++){	//STONESOUP:TRIGGER_POINT
			//Copy everything into one string
			sprintf(newoutput, "Variable at %d is %d\n", (int)(index+i), (int)sequence[index+i]);
			int f;
			//find the actual length of the output line
			for(f = 0; newoutput[f] != 0; f++){}
			//Write everything each pass to prevent a VERY LARGE ARRAY
			fwrite(newoutput, 1, f, otfil);
		}
		//Close file handle
		fclose(otfil);
	}

	return outputValue;
}

//See if the input value is already in the sequence
int inSequence(int value, int *seq, int size)
{
	int j;

	for (j=0; j < size; j++) {
		if (seq[j] == value) {
			return 1;
		}
	}

	return 0;
}
