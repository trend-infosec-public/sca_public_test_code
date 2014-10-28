
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
** CWE ID: CWE-129
** Variant Spreadsheet Rev #: 963
** Variant Spreadsheet ID: BASE TEST CASE
**
** Variant Features:
**		SOURCE_TAINT:CLIPBOARD
**		DATA_TYPE:ARRAY_LENGTH_ARRAY_CONTENT_VALUE
**		DATA_FLOW:ADDRESS_ALIASING_1
**		CONTROL_FLOW:INDIRECTLY_RECURSIVE
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
**   Good: 1st Set: 305 312 328 337 346 351 366 372 384 396 208 6 output.txt 7 1 9
**         2nd Set:
**    Bad: 1st Set: 7 -2659 9
*********************************************/
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <ctype.h>
#include <assert.h>
#include "clipboard.h"
#include "getIndex.h"

int getInteger(char *input);
void calcOut(int i, int end, int registers[], char *out, FILE *otfil);
void calcK(int i, int end, int registers[], char *out, FILE *otfil);

#define OVRWRT 5000
int main(int argc, char *argv[])
{
	int holder = 1;
	int prebuf[OVRWRT];
	int registers[10];
	int pstbuf[OVRWRT];
	int *point;
	FILE *otfil;

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

	memset(registers, 0, sizeof(registers));
	memset(prebuf, 'A', sizeof(prebuf));
	memset(pstbuf, 'A', sizeof(pstbuf));
	prebuf[OVRWRT - 1] = 0;
	pstbuf[OVRWRT - 1] = 0;

	point = registers;

	while(holder < argc){
		printf("Command: %s\n", argv[holder]);
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
			if(d>9){
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
			printf("Set up register from file %s\n", argv[holder+1]);
			char *buffer;
			char number[10] = {0,0,0,0,0,0,0,0,0,0};
			int result, i, y, x=0, place = 0;

			if ((otfil = fopen(argv[holder+1], "rb")) == NULL)
			{
				fprintf(stderr, "Unable to open file because of error\n");
				return(1);
			}
			// allocate memory to contain the whole file:
			buffer = (char*) malloc (sizeof(char)*100);
			if (buffer == NULL) {fputs ("Memory error",stderr); exit (2);}
			// copy the file into the buffer:
			result = fread (buffer,1,100,otfil);

			printf("Contents: %s\n",buffer);

			for(i=0; i<100; i++){
				if(place >= 10) break;
				if(buffer[i] == 32 || buffer[i] == 0){
					registers[place] = (int) atoi(number);
					place++;
					for(y=0;y<10; y++){
						number[y] = 0;
					}
					x=0;
				}else{
					number[x] = buffer[i];
					x++;
				}
			}
			//Close file handle
			fclose(otfil);
			free(buffer);

		} else

		/*
		 * Print from register d to register n
		 * Sample 7 5 9; prints the contents from register 5 to 9
		*/
		if(argv[holder][0] == '7'){
			int start, end;

			char *clipboard = getClipboard();

			if(!clipboard)
			{
				fprintf(stderr, "Error: No Clipboard found!\n");
				exit(1);
			}

			start =  getInteger(clipboard);	//STONESOUP:INTERACTION_POINT	//STONESOUP:CROSSOVER_POINT	//STONESOUP:SOURCE_TAINT:CLIPBOARD
			clipboard_cleanup(clipboard);
			end = getInteger(argv[holder+2]);
			printf("Print registers %d - %d\n", start,end);

			if(start>end){
				printf("d is larger than n. Invalid input: %d %d", d, n);
			}
			char fname[100];
			strcpy(fname, argv[4]);
			strcat(fname, "Output.bin");
			fprintf(stderr, "Output filename '%s'\n", fname);
			if ((otfil = fopen(fname, "wb")) == NULL)
			{
				fprintf(stderr, "Unable to create output file because of error\n");
				return(1);
			}
			int hol[2];
			hol[0] = 2;
			hol[1] = 4;
			//output array to be sent to a new file
			char newoutput[getIndex(hol[1])];	//STONESOUP:DATA_TYPE:ARRAY_LENGTH_ARRAY_CONTENT_VALUE
			char *out = newoutput;	//STONESOUP:DATA_FLOW::ADDRESS_ALIASING_1

			calcK(start, end, registers, out, otfil);
			printf("Output saved to output.txt");

		} else
		if(!strcmp(argv[holder],"100")){
			printf("Found halt %s\n", argv[holder]);
			exit(-1);
		}else {
			printf("Command not found!/n");
		}
		if(argv[holder][0] == '7'){ holder += 3;
		}else if(argv[holder][0] == '6'){ holder += 2;
		}else{
			holder++;
		}
		printf("\n\n");
	}
	return 0;
}
void calcK(int i, int end, int registers[], char *out, FILE *otfil){
	i++;
	if(i<=end){	//STONESOUP:TRIGGER_POINT
		calcOut(i, end, registers, out, otfil);	//STONESOUP:CONTROL_FLOW:INDIRECTLY_RECURSIVE
	}
}
void calcOut(int i, int end, int registers[], char *out, FILE *otfil){
	printf("%d \n",registers[i]);
	//print the contents
	printf("Register %d is: %d\n", i, registers[i]);
	//Copy everything into one string
	sprintf(out, "Register %d is: %d\n", i, registers[i]);
	//find the actual length of the output line
	int f;
	for(f = 0; out[f] != 0; f++){}
	//Write to file
	fwrite(out, 1 ,f, otfil);
	calcK(i, end, registers, out, otfil);
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

