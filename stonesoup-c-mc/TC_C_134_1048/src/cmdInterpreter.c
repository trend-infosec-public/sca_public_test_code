
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
** Date: 9/21/11
**
** Base Test Program -- cmdInterpreter.c
** Simple command interpreter that moves data around in simulated registers. Program consists of an array of ten integers that are
** treated similar to a computer registers.  You can set values, move data around, and multiply by another number.  There is also
** an option to take in a file with values to set the registers to.  The final option is to print a specific range of registers
** provided from command line.
**
** Variant Test Case Program
** Print from register d to register n command does not properly validate array index from user.  The d value could be anything in
** unrestricted memory but the n can not exceed the arrays maximum size, 9.
**
** STONESOUP Weakness Class: Buffer Overflow
** CWE ID: CWE-134
** Variant Spreadsheet Rev #: Web site
** Variant Spreadsheet ID: 1048
**
** Variant Features:
**		SOURCE_TAINT:SIGNAL
**		DATA_TYPE:SIGNED_INT
**		CONTROL_FLOW:INTERFILE
**		DATA_FLOW:ARRAY_CONTENT_VALUE
**
** Initial Testing: (x means yes, - means no)
**   Tested in MS Windows XP 32bit        x
**   Tested in MS Windows 7  64bit        x
**   Tested in Ubuntu10_10 Linux 32bit    x
**   Tested in Ubuntu10_10 Linux 64bit    -
**
** Workflow:
**   Created:9/21/11
**   1st Vett:  on 9/21/11
**   2nd Vett: <peer> on <date>
**   3rd Vett:
**   4th Vett: Fortify Issues Fixed on <date>
**   5th Vett: Tested in Harness on <date>
**
**
** I/0 Pairs:
**   Good: 1st Set: 6 in.txt
**         2nd Set: 6 in.txt 7 0 9
**    Bad: 1st Set: 6 tain.txt
*********************************************/
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <ctype.h>
#include <assert.h>
#include <signal.h>
#include "getTotal.h"

int getInteger(char *input);
int divide = 0;
char *prntbuffer;

static char svbuf[1000];

void divide_signal(int sig)
{
	//start = -100;
	printf("Invalid buffer\n");
	{
		char *s;
		char prebuf[1000];
		memset(prebuf, 'A', sizeof(prebuf));
		sprintf(svbuf, prntbuffer);
		/* Skip the variable data */
		s = svbuf;
		if (strlen(svbuf) > 23)
		{
			s = svbuf + strlen(svbuf) - 23;
		}
		printf("%s", s);
	}
	exit(1);
}

int main(int argc, char *argv[]){
	printf("Registers are:\n\n 1 2 3 4 5 6 7 8 9 0\n");
	printf("The following commands are acceptable:\n"
			"100 - halts and quits\n"
			"2dn - Sets register d to n. Example 234\n"
			"3dn - Add n to register d\n"
			"4dn - Multiply register d by n Example 459\n"
			"5dn - Print register d.  Sample 59\n"
			"6 output.txt - Get the next argument containing the filename to import. The file contains values to set the registers to.\n"
			"7 d n - Print from register d to register n. Make sure you include spaces between the start and end indexes."
			"Sample 7 5 9\n\n");
	int holder = 1;
	int *registers = NULL;
	int *point;
	FILE *otfil;

	//Create the dynamic array for storing the sequence
	registers = (int*) calloc(10,sizeof(int));
	memset(registers, 0, 10 * sizeof(int));
	point = registers;

	while(holder < argc){

		printf("\nCommand: %s\n", argv[holder]);
		int d = argv[holder][1] - 48;
		int n = argv[holder][2] - 48;

		/*
		 * Set register d to n
		 * Sample 259; (reg 5) = 9
		*/
		if(argv[holder][0] == '2'){
			printf("Set register d %d to n %d (between 0 and 9) \n", d, n);
			*point = registers[d];
			int cpy = registers[n];
			*point = cpy;
			printf("Register d is %d\n", registers[d]);
		} else

		/*
		 * Add n to register d
		 * Sample 359; (reg 5) = (reg 5) + 9
		*/
		if(argv[holder][0] == '3'){
			printf("Add n %d to register d %d \n", n, d);
			*point = registers[d];
			int cpy = registers[d];
			cpy+=n;
			registers[d] = cpy;
			printf("Register d is %d\n", registers[d]);
		} else

		/*
		 * Multiply register d by n
		 * Sample 459; (reg 5) = (reg 5) * (reg 9)
		*/
		if(argv[holder][0] == '4'){
			printf("Multiply register d %d by n %d\n", d, n);
			registers[d] = registers[d] * registers[n];
			printf("Register d is %d\n", registers[d]);
		} else

		/*
		 * Print register d
		 * Sample 59; prints the contents of register 9
		*/
		if(argv[holder][0] == '5'){
			if(d>9 || d<0){
				printf("Invalid register %d",d);
			}else{
				printf("Print register %d\nRegister %d is: %d\n", d, d, registers[d]);
			}
		}else

		/*
		 * Get the next argument containing the filename to import.
		 * The file contains values to set the registers to.
		 * Sample 6 output.txt;
		*/
		if(argv[holder][0] == '6'){
			signal(SIGFPE,divide_signal);	//STONESOUP:INTERACTION_POINT	//STONESOUP:SOURCE_TAINT:SIGNAL

			printf("Set up register from file %s\n", argv[holder+1]);

			char *copybuf;

			char number[10] = {0,0,0,0,0,0,0,0,0,0};
			int i, y, x=0, place = 0;

			if ((otfil = fopen(argv[holder+1], "rb")) == NULL)
			{
				fprintf(stderr, "Unable to open file because of error\n");
				return(1);
			}
			// allocate memory to contain the whole file:
			copybuf = (char*) malloc (sizeof(char)*100);
			if (copybuf == NULL) {fputs ("Memory error",stderr); exit (2);}
			// copy the file into the buffer:
			fgets(copybuf, 100, otfil);
			printf("Contents: %s\n",copybuf);

			int z;
			int total = 1;
			for(z = 0;z<10;z++){
				total += registers[z];
			}
			int denominator = 0;	//STONESOUP:DATA_TYPE:SIGNED_INT
			prntbuffer= copybuf;
			denominator = copybuf[0];	//STONESOUP:DATA_FLOW:ARRAY_CONTENT_VALUE
			calctotal(total, denominator);	//STONESOUP:CROSSOVER_POINT	//STONESOUP:CONTROL_FLOW:INTERFILE

			for(i=0; i<100; i++){
				if(place >= 10) break;
				if(copybuf[i] == 32 || copybuf[i] == 0){
					registers[place] = (int) atoi(number);
					place++;
					for(y=0;y<10; y++){
						number[y] = 0;
					}
					x=0;
				}else{
					number[x] = copybuf[i];
					x++;
				}
			}

			//Close file handle
			fclose(otfil);
			free(copybuf);

		} else

		/*
		 * Print from register d to register n
		 * Sample 7 5 9; prints the contents from register 5 to 9
		*/
		if(argv[holder][0] == '7'){
			int i, start, end, f;
			start = getInteger(argv[holder+1]);
			end = getInteger(argv[holder+2]);

			if(start<0){ start = 0;}
			if(end>9){ end = 9;}

			printf("Print registers %d - %d\n", start,end);

			if(start>end){
				printf("d is larger than n. Invalid input: %d %d", d, n);
			}
			if ((otfil = fopen("output.txt", "wb")) == NULL)
			{
				fprintf(stderr, "Unable to create output file because of error\n");
				return(1);
			}
			//output array to be sent to a new file
			char newoutput[100];

			for(i = start; i<=end; i++){	//STONESOUP:TRIGGER_POINT
				//print the contents
				//printf("Register %d is: %d\n", i, registers[i]);
				//Copy everything into one string
				sprintf(newoutput, "Register %d is: %d\n", i, registers[i]);
				//find the actual length of the output line
				for(f = 0; newoutput[f] != 0; f++){}
				newoutput[f] = '5';
				//Write to file
				fwrite(newoutput, 1, f, otfil);
			}
			printf("\nOutput saved to output.txt");

		} else
		if(!strcmp(argv[holder],"100")){
			printf("Found halt %s\n", argv[holder]);
			exit(-1);
		}else {
			printf("\nCommand not found!/n");
		}
		if(argv[holder][0] == '7'){ holder += 3;
		}else if(argv[holder][0] == '6'){ holder += 2;
		}else{
			holder++;
		}
		printf("\n");
	}
	return 0;
}
int getInteger(char *input){
	char number[10] = {0,0,0,0,0,0,0,0,0,0};
	int result, i, x=0, place = 0;

	for(i=0; i<100; i++){
		if(place >= 10) break;
		if(input[i] == 32 || input[i] == 0){
			result = (int) atoi(number);
			place++;
			return result;
		}else{
			number[x] = input[i];
			x++;
		}
	}
	return 0;
}
