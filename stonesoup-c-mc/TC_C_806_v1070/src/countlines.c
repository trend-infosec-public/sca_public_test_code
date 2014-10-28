
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
** Date: 7/26/11
**
** Base Test Program -- countlines.c
**
** This Base Test Program counts the number of lines of code in a Java Program
** ignoring whitespace and comment lines.  It takes as input a single file that is
** located in the same directory as the executable.  Note that technically this
** doesn't have to be a .java file and any file will work.  This program just looks for
** comments that follow the Java language specification.
**
** Note that it is assumed that all lines of code are under 256 characters.  If longer
** lines are encountered, then the counts will be incorrect.  Also a / * sequence that
** appears within an escaped string (e.g. "A / * causes an error.") will lead to incorrect
** counts as the algorithim used will not recognize the escaped string and think this is
** the start of a multiline comment.
**
** The program has the following basic flow:
**
** - get filename
** - open file
** - read line
** - if it meets criteria increment counter
** - read next line
** - close file
** - output line count
**
** Variant Test Case Program
**
** The program has been altered so that a larger buffer may be copied into
** a smaller buffer, causing a buffer overflow. First, the input filename
** is copied into a 256 char buffer. The buffer is then copied into a 20
** char buffer. A malicious hacker could take advantage of this by
** inserting some assembly code and running an arbitrary program.
**
** STONESOUP Weakness Class: Buffer Overflows and Memory Corruption
** CWE ID: CWE-806
** Variant Spreadsheet Rev #: 2
** Variant Spreadsheet ID: 1070
**
** Variant Features:
**		SOURCE_TAINT:COMMAND_LINE
**		DATA_TYPE:UNSIGNED_LONG
**		DATA_FLOW:LOOP_COMPLEXITY_INCREMENT
**		CONTROL_FLOW:ADDRESS_AS_A_FUNCTION_RETURN_VALUE
**
** Initial Testing: (x means yes, - means no)
**   Tested in MS Windows XP 32bit        -
**   Tested in MS Windows 7  64bit        x
**   Tested in Ubuntu10_10 Linux 32bit    -
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
#include <ctype.h>

#define DEBUG 0
int trigger(char filename[], char* address, unsigned long a, int i);
char* returnSourceAddress(char*);

int main (int argc, char *argv[])	//STONESOUP:INTERACTION_POINT	//STONESOUP:SOURCE_TAINT:COMMAND_LINE
{
	FILE *fp;
	char filename[20];
	char line[256];
	char *line_pointer;

	int line_already_counted;
	int multi_line_comment;
	int total_lines;
	int lines_of_code;

	int i;

	// An incorrect number of arguments were supplied.  Let the user know the expected format of
	// the command line.  One user supplied argument is expected.  Remember that the first argument
	// is always the program name so we really need to check that 2 arguments exist.

	if (argc != 2)
	{
		printf("\nERROR: Usage: countlines <filename>\n");
		return(EXIT_FAILURE);
	}

	unsigned long a = (unsigned long)argv[1];	//STONESOUP:DATA_TYPE:UNSIGNED_LONG

	// Get the input that was supplied on the command line.  One argument should be there.  This
	// argument is the name of the .java file that is being counted.
	//
	// For the file name argument, this code will only grab the first 255 characters from the
	// command line argument, placing a NULL terminator at the end.

	memset(filename, '\0', 20);									// set filename buffer to null
	char buffer[256];
	strncpy(buffer, (char *)a, 256);								// copy 256 chars of input into buffer
	char* address = returnSourceAddress((char *)a);				// STONESOUP:DATA_FLOW:ADDRESS_AS_A_FUNCTION_RETURN_VALUE
	for(i = 0; i < 5; i += trigger(filename, address, a, i));	//STONESOUP:CONTROL_FLOW:LOOP_COMPLEXITY_INCREMENT
		//printf("print\n");

	// Validate that the filename provided is just a filename and not a path to some other
	// directory.  We do this through a blacklist that excludes slashes (both forward and
	// backward) and the double dot sequence.
	//
	// Note that we only loop from 0 to 255 since the last character should be a NULL
	// and there is no need to double check this.  In addition, this guards against any
	// possible buffer overread when we check filename[i+1].

	for (i=0;i<255;i++)
	{
		if (filename[i]=='\0') break;

		if (filename[i]=='\\' || filename[i]=='/')
		{
			printf("\nERROR: Filename must not contain a slash character.\n");
			return(EXIT_FAILURE);
		}

		if (filename[i]==':')
		{
			printf("\nERROR: Filename must not contain a colon character.\n");
			return(EXIT_FAILURE);
		}

		if (filename[i]=='.' && filename[i+1]=='.')
		{
			printf("\nERROR: Filename must not contain a double dot (e.g. '..') sequence.\n");
			return(EXIT_FAILURE);
		}
	}

	// Open the file in readonly mode.

	fp = fopen(filename, "r");
	if (fp == NULL)
	{
		printf("\nERROR: Cannot open file.\n");
		return(EXIT_FAILURE);
	}

	// After initializing variables, loop through each line of the file using fgets().  The function
	// fgets(str, num, fp) reads up to num - 1 characters from the file stream fp and dumps them
	// into str. fgets() will stop when it reaches the end of a line, in which case str will be
	// terminated with a newline. If fgets() reaches num - 1 characters or encounters the EOF,
	// str will be null-terminated. fgets() returns str on success, and NULL on an error.

	multi_line_comment = 0;
	total_lines = 0;
	lines_of_code = 0;

	memset(line, '\0', 256);

	while (fgets(line,256,fp) != NULL)
	{
		total_lines++;

		// We are reading a new line so we need to reset the line_already_counted flag since
		// it obviously hasn't been counted yet.

		line_already_counted = 0;

		// Since fgets() may terminate a string with a newline ... we need to strip any trailing
		// '\n' and replace it with a NULL character.

		if (line[strlen(line)-1] == '\n') line[strlen(line)-1] = '\0';

		// Set the line_pointer to the first character of the line.

		line_pointer = line;

		// We are now ready to parse the line character by character.

		while (*line_pointer != '\0')
		{
			// If we are currently in a multi line scenario, then we need to check if we have hit
			// an end tag.  Otherwise we just advance the pointer and keep looping.

			if (multi_line_comment == 1)
			{
				if (*line_pointer == '*')
				{
					// Advance the line_pointer.  We need to check for the NULL character
					// and break if found. If we don't do this, then the next line_pointer++
					//call (before the start of the next iteration) will advance us out of bounds.

					line_pointer++;
					if (*line_pointer == '\0') break;

					if (*line_pointer == '/')
					{
						// An end tag has been found!  Reset the multi_line_comment.
						// Note that we still need to advance the pointer and continue
						// the loop.

						multi_line_comment = 0;
					}
				}

				line_pointer++;
				continue;
			}

			// If the current character is a whitespace, then skip it and continue the loop
			// inorder to examine the next character.

			if (isspace(*line_pointer) != 0)
			{
				line_pointer++;
				continue;
			}

			// If the current character is a slash, then we need to look at the following
			// character to see if the line is a Java comment.  If we are looking at a Java
			// comment line, then just break out of the loop without incrementing the
			// line counter.

			if (*line_pointer == '/')
			{
				// Advance the line_pointer.  We need to check for the NULL character
				// and break if found. If we don't do this, then the next line_pointer++
				//call (before the start of the next iteration) will advance us out of bounds.

				line_pointer++;
				if (*line_pointer == '\0') break;

				// If the next character is a slash as well, then a single line comment has
				// been found.  The rest of this line is not code, so break out of this loop.

				if (*line_pointer == '/') break;

				// If the next character is an asteric, then a multi-line comment has been
				// found.  We now need to skip every line until the end of the comment. To
				// do this, we will turn on the multi_line_comment flag and continue
				// processing this line (and any following line) looking for the closing tag.
				//Note that the end of the comment may contain code on the rest of the
				// line after the closing comment tag.

				if (*line_pointer == '*')
				{
					multi_line_comment = 1;
					line_pointer++;
					continue;
				}

				// Note that if we reach here, a Java comment was NOT found, so we should
				// fall through to the default processing and count the current line as a
				// valid line of code.
			}

			// A line ofJava code has been found!  If the line has not already been counted, then
			// increment the line counter.

			if (line_already_counted == 0)
			{
				if (DEBUG) printf("DEBUG: %s\n", line_pointer);
				lines_of_code++;
				line_already_counted = 1;
			}

			// Move the line pointer to the next character in the line so that the next
			// iteration of the current loop doesn't try to process the same character.

			line_pointer++;
		}
	}

	// We are done with the file so close it.

	 if (fclose(fp))
	 {
		printf("\nERROR: File close error.\n");
		return(EXIT_FAILURE);
	 }

	// We have finished looking at each line in the file, and now have the line count.  So print it!!

	if (DEBUG) printf("\nDEBUG: The file '%s' contains %d total lines, of which %d are code.\n\n", filename, total_lines, lines_of_code);
	printf("\nRESULT: %d", lines_of_code);

	return(EXIT_SUCCESS);
}

char* returnSourceAddress(char input[])
{
	return input;
}

int trigger(char filename[], char* address, unsigned long a, int i)
{
	if((a <= 0 || a > 0) && i == 3)
		strcpy(filename, address);  // STONESOUP:CROSSOVER_POINT // STONESOUP:TRIGGER_POINT	// copy larger buffer into smaller buffer
	return 1;
}

/* End of file */

