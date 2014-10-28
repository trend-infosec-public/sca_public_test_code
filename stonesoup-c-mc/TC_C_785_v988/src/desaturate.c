
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
** Date: <date started>
**
** Base Test Program -- Image Converter
**
** This Base Test Program takes as input a color BMP file and produces a gray scale version of
** that image.
**
** Variant Test Case Program
**
** The program takes in additional parameters: filename to be converted to gray scale, filename to save
** gray scale as, directory to save file in, and buffer for use with realpath().  Mingw does not have a
** implementation for realpath so for windows I had to create my own.  First the program creates an array
** to store the path into using buffer for its size.  We then run realpath if we are in linux or some
** windows functions that do that same thing.  These functions get the current directory and append the
** directory the user provides, to create the final directory the output should be stored in.  When we get
** the buffer from the command line we check to see if it is smaller than 45 and not negative.  But if a
** user provides a number in the range of 65467 - 65592 than overflow in the integer occurs and we get a
** number between 0-45.  These numbers will allow us to create a folder in any place we want and will copy
** our bmp to that location.
**
**
**
** STONESOUP Weakness Class: <weakness class>
** CWE ID: CWE-785
** Variant Spreadsheet Rev #: Web site
** Variant Spreadsheet ID: 988
**
** Variant Features:
**		SOURCE_TAINT:KEYBOARD
**		DATA_TYPE:STRUCT
**		CONTROL_FLOW:LOOP_COMPLEXITY_INCREMENT
**		DATA_FLOW:ADDRESS_ALIASING_1
**
** Initial Testing: (x means yes, - means no)
**   Tested in MS Windows XP 32bit        x
**   Tested in MS Windows 7  64bit        x
**   Tested in Ubuntu10_10 Linux 32bit    x
**   Tested in Ubuntu10_10 Linux 64bit    -
**
** Workflow:
**   Created:<data started>
**   1st Vett:  on 6/20/11
**   2nd Vett: <peer> on <date>
**   3rd Vett:
**   4th Vett: Fortify Issues Fixed on <date>
**   5th Vett: Tested in Harness on <date>
**
**
** I/0 Pairs:
**   Good: 1st Set: google.bmp output.bmp holiday 46
**         2nd Set: google.bmp result.bmp output 70
**    Bad: 1st Set:	google.bmp output.bmp output 65566
*********************************************/

#include <stdlib.h>
#include <limits.h>
#include <errno.h>
#include <stdio.h>
#include <stdarg.h>
#include <malloc.h>
#include <string.h>

#include "pathgen.h"
#include "MouseKeyboard.h"

#ifdef _WIN32
#define _WIN32_WINNT 0x0600
#include <stdarg.h>
#include <windef.h>
#include <winbase.h>
#else
#include <linux/limits.h>
#include <unistd.h>
#endif

#if LONG_MAX != 2147483647L
#error Compiler has integer type "long" set to other than 32bits
#endif

#if SHRT_MAX != 32767
#error Compiler has integer type "short" set to other than 16bits
#endif

struct BmpHdr
{
	unsigned long fsize;
	unsigned long appspecific1;
	unsigned long img_ofst;
};

struct BmpInfoHdr
{
	unsigned long infohdr_sz;
	long wid;
	long hght;
	unsigned short numplanes;
	unsigned short bitsperpixel;
	unsigned long compress_type;
	unsigned long bmp_bytesz;
	long horzres;
	long verticalres;
	unsigned long numcolors;
	unsigned long numimportantcolors;
};

/* Print an error message, close up the files, and delete the partial output file */
/* This is done quite a few times, so consolidate here */
void closeuperr (FILE *infil, FILE *otfil, const char *otflnm, void *preamble, const char *errmsg, ...)
{
	va_list vargs;
	va_start(vargs, errmsg);
	vfprintf(stderr, errmsg, vargs);
	va_end(vargs);
	fclose(otfil);
	fclose(infil);
	if (otflnm)
	{
		remove(otflnm);
	}

	if (preamble)
	{
		free(preamble);
	}
}

/* Enter here to process! */
int main(int argc, char **argv)
{
	FILE *infil = NULL;	/* Input file pointer */
	FILE *otfil = NULL;	/* Output file pointer */
	struct BmpHdr hdr;	/* First part of the file header goes here */
	struct BmpInfoHdr infohdr;	/* Most of the file information goes here */
	unsigned char buf[512], *preamble = NULL;
	unsigned long rowsz, numrows, col, row;
	unsigned long int id;
	char keybuf[1000] = "";
	int i;
	struct tnt	//STONESOUP:DATA_TYPE:STRUCT
	{
		int bsz;
		char *name;
	} tnt;
	tnt.name = NULL; /* Name of output save directory */
	tnt.bsz = 0;

	if (argc < 3)
	{
		fprintf(stderr, "Not enough arguments to command line\nExpected:\n\n");
		fprintf(stderr, "%s <BMP_input_filename.bmp> <BMP_output_filename.bmp>\n\n", argv[0]);
		fprintf(stderr, "<output_path_size> <output_file_path> are entered from the keyboard\n");
		return(1);
	}

	if ((MouseKeyWin(&id, NULL, NULL, keybuf, sizeof(keybuf), NULL, NULL) != 0) || !*keybuf)	//STONESOUP:SOURCE_TAINT:KEYBOARD
	{
		printf("No keystrokes entered\n");
		CloseMouseKeyWin(&id);
		return(EXIT_FAILURE);
	}
	CloseMouseKeyWin(&id);

	//Output_path_size
	tnt.bsz = atoi(keybuf);
	//Make sure buffer is >= 0
	if(tnt.bsz <= 45){
		printf("Invalid buffer size must be >= 45 :%d\n", tnt.bsz);
		return(1);
	}
	tnt.bsz = (short int) tnt.bsz;
	if(tnt.bsz <= 0){
		printf("Invalid buffer size must be > 0: %d\n", tnt.bsz);
		return(1);
	}
	printf("%u\n", tnt.bsz);

	//Path name
	char *s = keybuf;
	while (*s && ((*s == ' ') || (*s == '\t'))) {s++;};	/* Skip leading white space */
	while (*s && ((*s != ' ') && (*s != '\t') && (*s != ','))) {s++;};	/* Skip buffer size */
	tnt.name = s;	//STONESOUP:DATA_FLOW:ADDRESS_ALIASING_1
	while (*tnt.name && ((*tnt.name == ' ') || (*tnt.name == '\t'))) {tnt.name++;};	/* Skip trailinging white space */
//	printf("%s\n", tnt.name);

	if ((infil = fopen(argv[1], "rb")) == NULL)
	{
		fprintf(stderr, "Unable to open input file because of error %d\n", errno);
		return(1);
	}

	//create output directory
	int bsz1 = PATH_MAX + strlen(argv[2]) + 2;
	char outputDirectoryName[bsz1];

	pathgen(tnt.name, outputDirectoryName, PATH_MAX);  //my implementation of realpath
	outputDirectoryName[tnt.bsz] = '\0';	//STONESOUP:INTERACTION_POINT	//STONESOUP:CROSSOVER_POINT
	for (i = -1; i < (tnt.bsz + 2); i += createDirectory(outputDirectoryName))	//STONESOUP:TRIGGER_POINT	//STONESOUP:CONTROL_FLOW:LOOP_COMPLEXITY_INCREMENT
	{
		if (i >= 0)
		{
			if (i == 0)
			{
			fclose(infil);
			return(1);
			}
			else
			{
				break;
			}
		}
		else
		{
			i++;
		}
	}

	//Append user provided filename to the end of our new directory so fwrite will put it in
	//the new folder
	strcat(outputDirectoryName, "/");
	strcat(outputDirectoryName, argv[2]);
	printf("File saved in %s\n",outputDirectoryName);

	if ((otfil = fopen(outputDirectoryName, "wb")) == NULL)
	{
		fprintf(stderr, "Unable to open output file because of error %s\n", strerror( errno ));
		fclose(infil);
		return(1);
	}

	/* Only the very first 2 characters contain the ID */
	if (fread(buf, 2, 1, infil) != 1)
	{
		closeuperr(infil, otfil, argv[2], preamble, "Unable to read the ID field of %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	/* Identify the file type as a 'BM' file */
	if ((buf[0] != 'B') || (buf[1] != 'M'))
	{
		closeuperr(infil, otfil, argv[2], preamble, "BMP file %s is unknown type, expected 'B' 'M', found '%c' '%c'\n",
			argv[1], buf[0], buf[1]);
		return(1);
	}

	/* Get the rest of the initial header which contains the offset to the image */
	if (fread(&hdr, sizeof(hdr), 1, infil) != 1)
	{
		closeuperr(infil, otfil, argv[2], preamble, "Unable to read the header of %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	/* Check sanity of image offset */
	if (hdr.img_ofst >= hdr.fsize)
	{
		closeuperr(infil, otfil, argv[2], preamble, "BMP file %s has %lu bytes before image, but file size is %u\n",
			argv[1], hdr.img_ofst, hdr.fsize);
		return(1);
	}

	/* Get the rest of the image information */
	if (fread(&infohdr, sizeof(infohdr), 1, infil) != 1)
	{
		closeuperr(infil, otfil, argv[2], preamble, "Unable to read the infoheader of %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	/* DeCompression is not yet built into this package */
	if (infohdr.compress_type != 0)
	{
		closeuperr(infil, otfil, argv[2], preamble, "BMP file %s is compressed of type %u and this function does not handle compression\n",
			argv[1], infohdr.compress_type);
		return(1);
	}

	/* Also haven't tested anything other than 24 bits per pixel */
	if ((infohdr.bitsperpixel != 32) && (infohdr.bitsperpixel != 24))
	{
		closeuperr(infil, otfil, argv[2], preamble, "Can only process 24 or 32 bits per pixel in BMP file %s, not %d\n",
			argv[1], infohdr.bitsperpixel);
		return(1);
	}

	/* Start over, we keep the same file front end, nothing there changes */
	/* Since the files are identical except for the image pixel values, keep the front end the same */
	if (fseek(infil, 0, SEEK_SET))
	{
		closeuperr(infil, otfil, argv[2], preamble, "Unable to seek to 0 in BMP file %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	/* Check sanity of image offset */
	if ((preamble = malloc(hdr.img_ofst)) == NULL)
	{
		closeuperr(infil, otfil, argv[2], preamble, "BMP file %s, could not malloc %u bytes\n",
			argv[1], hdr.img_ofst);
		return(1);
	}

	if (fread(buf, hdr.img_ofst, 1, infil) != 1)
	{
		closeuperr(infil, otfil, argv[2], preamble, "Unable to read the infoheader of %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	if (fwrite(buf, hdr.img_ofst, 1, otfil) != 1)
	{
		closeuperr(infil, otfil, argv[2], preamble, "Unable to write the BMP ID of %s because of error %d\n",
			argv[2], errno);
		return(1);
	}

	/* The row length MUST be a multiple of 4 bytes */
	rowsz = (((infohdr.bitsperpixel * infohdr.wid) + 16) / 32) * 4;
	numrows = infohdr.hght < 0 ? -infohdr.hght : infohdr.hght;

	/* Make sure we only do the 24 bits per pixel */
	if (infohdr.bitsperpixel == 24)
	{
		unsigned char p[3];	/* Byte order: Blue, Green, Red */
		unsigned char gray;
		int j, i = 3;	/* The number of bytes per pixel to read and write */
		long rowsz_mod = rowsz - (infohdr.wid * 3);
//printf("%d %d\n", rowsz_mod, rowsz);

		/* Loop through each row of the image */
		for (row = 0; row < numrows; row++)
		{
			/* Loop through each pixel of the image */
			for (col = 0; col < infohdr.wid; col++)
			{
				/* Inefficient, but we only get 1 pixel at a time */
				/* No buffers to overflow or keep track of */
				if ((j = fread(p, 1, i, infil)) != i)
				{
					closeuperr(infil, otfil, argv[2], preamble, "Expected to read %u pixels from file %s, read %u\n",
						i, argv[1], j);
					return(1);
				}
				/* If there is a corrupt image, we run out of pixels early */
				if (feof(infil))
				{
					closeuperr(infil, otfil, argv[2], preamble, "Ran out of pixels in row %u of file %s, expected %u, found %u\n",
						row, argv[1], numrows * rowsz, (row * rowsz) + col);
					return(1);
				}
				/* Average all of the brightness values to obtain the grayscale value */
				gray = (((unsigned int)p[0]) + ((unsigned int)p[1]) + ((unsigned int)p[2])) / 3;
				/* Now store the value into each color */
				p[0] = gray;
				p[1] = gray;
				p[2] = gray;
				/* Now write the value out */
				if ((j = fwrite(p, 1, i, otfil)) != i)
				{
					closeuperr(infil, otfil, argv[2], preamble, "Expected to write %u pixels to file %s, wrote %u\n",
						i, argv[1], j);
					return(1);
				}
			}
//printf("%d %d\n", row, col);

			/* Finish out the row */
			for (col = 0; col < rowsz_mod; col++)
			{
//printf("%d\n", col);
				if ((j = fread(p, 1, 1, infil)) != 1)
				{
					closeuperr(infil, otfil, argv[2], preamble, "Expected to read 1 pixel in row %u from file %s, read %u\n", row, argv[1], j);
					return(1);
				}
				if (feof(infil))
				{
					closeuperr(infil, otfil, argv[2], preamble, "Ran out of pixels early in file %s, expected %d, found %d\n",
						argv[1], numrows * rowsz, (row * rowsz) + col);
					return(1);
				}
				if (fwrite(p, 1, 1, otfil) != 1)
				{
					closeuperr(infil, otfil, argv[2], preamble, "Expected to write 1 pixel to file %s, wrote none\n",
						argv[1]);
					return(1);
				}
			}
//closeuperr(infil, otfil, NULL, "\n");
//return(1);
		}
	}
	else if (infohdr.bitsperpixel == 32)
	{
		unsigned char p[4];	// Byte order: Blue, Green, Red, Alpha
		unsigned char gray;
		int i = 4, j;

		for (row = 0; row < numrows; row++)
		{
			for (col = 0; col < rowsz; col += i)
			{
				if ((j = fread(p, 1, i, infil)) != i)
				{
					closeuperr(infil, otfil, argv[2], preamble, "Expected to read %d pixels from file %s, read %d\n",
						i, argv[1], j);
					return(1);
				}
				if (feof(infil))
				{
					closeuperr(infil, otfil, argv[2], preamble, "Ran out of pixels early in file %s, expected %d, found %d\n",
						argv[1], numrows * rowsz, (row * rowsz) + col);
					return(1);
				}
				gray = (((unsigned int)p[0]) + ((unsigned int)p[1]) + ((unsigned int)p[2])) / 3;
				p[0] = gray;
				p[1] = gray;
				p[2] = gray;
				// Now write the value out
				if ((j = fwrite(p, 1, i, otfil)) != i)
				{
					closeuperr(infil, otfil, argv[2], preamble, "Expected to write %d pixels to file %s, wrote %d\n",
						i, argv[1], j);
					return(1);
				}
			}
		}
	}

	/* Write out the residual file which will be the same */
	while (!feof(infil))
	{
		int j;
		int i = fread(preamble, 1, hdr.img_ofst, infil);
		if (i > 0)
		{
			if ((j = fwrite(preamble, 1, i, otfil)) != i)
			{
				closeuperr(infil, otfil, argv[2], preamble, "Expected to write %d pixels to file %s, wrote %d\n",
					i, argv[1], j);
				return(1);
			}
		}
		else
		{
			break;
		}
	}

	free(preamble);

	if (fclose(otfil) != 0)
	{
		fprintf(stderr, "Unable to close output file because of error %d\n", errno);
		fclose(infil);
		return(1);
	}

	if (fclose(infil) != 0)
	{
		fprintf(stderr, "Unable to close input file because of error %d\n", errno);
		return(1);
	}

	// Put link to new directory/filename where Test Harness can find it
#ifdef _WIN32
	CreateHardLink(outputDirectoryName, argv[2], NULL);
#else
	link(outputDirectoryName, argv[2]);
#endif

	return(0);
}

/* End of file */
