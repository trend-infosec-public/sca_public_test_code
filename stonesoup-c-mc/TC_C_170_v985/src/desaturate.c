
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

/************************************************************************
**
**
**  
**  Date: 5/1/2011
**
**	Description:
**	Desaturate a Color BMP File to produce a grayscale file
**
**	CWE-170: Improper Null Termination
**		Description Summary
**			The software does not terminate or incorrectly terminates a string or
**			array with a null character or equivalent terminator.
**
**		Extended Description
**			Null termination errors frequently occur in two different ways. An
**			off-by-one error could cause a null to be written out of bounds,
**			leading to an overflow. Or, a program could use a strncpy() function
**			call incorrectly, which prevents a null terminator from being added at
**			all. Other scenarios are possible.
**
**  Spreadsheet Rev #: db
**  CWE #: 170
**  Spreadsheet Variant: 895
**		SOURCE_TAINT:MOUSE
**		DATA_TYPE:DOUBLE_FLOAT
**		CONTROL_FLOW:INTERFILE_2
**		DATA_FLOW:ARRAY_INDEX_LINEAR_EXPRESSION
**
**  (x means yes, - means no)
** Tested in MS Windows XP 32bit X
** Tested in MS Windows 7 64bit  -
** Tested in Ubuntu Linux 32bit  X
** Tested in RH Linux 64bit      -
**
**  Revision History
**  Date      Change
**
************************************************************************/

#include <limits.h>
#include <errno.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <malloc.h>

#include "MouseKeyboard.h"
#include "interfile1.h"

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
	vfprintf(stdout, errmsg, vargs);
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

int stricmp1(char *s1, char *s2)
{
	if (!s1 && !s2)
	{
		return (0);
	}
	else if (!s1)
	{
		return (-1);
	}
	else if (!s2)
	{
		return (-1);
	}
	do
	{
		char a1 = *s1;
		char a2 = *s2;
		if ((a1 >= 'a') && (a1 <= 'z'))
		{
			a1 -= 'a' - 'A';
		}
		if ((a2 >= 'a') && (a2 <= 'z'))
		{
			a2 -= 'a' - 'A';
		}
		if (a1 != a2)
		{
			return(-1);
		}
		if (a1 == '\0')
		{
			return(0);
		}
		s1++;
		s2++;
	}
	while (*s1 && *s2);
	if (*s1 == *s2)
	{
		return(0);
	}

	return(-1);
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
	int mousex = INT_MIN, mousey = INT_MIN;
	char keybuf[1000];

	if (argc < 2)
	{
		fprintf(stderr, "Not enough arguments to command line\nExpected:\n\n");
		fprintf(stderr, "%s <BMP_input_filename.bmp>\n", argv[0]);
		return(1);
	}

	memset(keybuf, 0, sizeof(keybuf));	/* Make problems consistent */

	int k1 = strlen(argv[1]) + 1;
	int k2 = sizeof(keybuf) - 1;
	strncpy(keybuf, argv[1], k1 > k2 ? k2 : k1);
	if ((MouseKeyWin(&id, &mousex, &mousey, keybuf, sizeof(keybuf), NULL, NULL) != 0) ||
			(mousex == INT_MIN) || (mousey == INT_MIN))	//STONESOUP:SOURCE_TAINT:MOUSE
	{
		printf("No mouse activity\n");
		CloseMouseKeyWin(&id);
		return(1);
	}
	CloseMouseKeyWin(&id);

	char *s22 = keybuf + strlen(keybuf) - 4;
	if (stricmp1(s22, ".bmp") != 0)
	{
		printf("Input filename '%s' is not a .bmp file\n", keybuf);
		return(1);
	}

	if (strlen(keybuf) > (sizeof(keybuf) - 8))
	{
		printf("Not enough room in output filename buffer\n");
		return(1);
	}

	strcpy(s22, "Out.bmp");
	if ((infil = fopen(argv[1], "rb")) == NULL)
	{
		fprintf(stderr, "Unable to open input file because of error %d\n", errno);
		return(1);
	}

	if ((otfil = fopen(keybuf, "wb")) == NULL)
	{
		fprintf(stderr, "Unable to open output file because of error %d\n", errno);
		fclose(infil);
		return(1);
	}

	double mx[1];
	int xx = 1;
	mx[xx - 1]= interfile1(mousex);	//STONESOUP:DATA_TYPE:DOUBLE_FLOAT	//STONESOUP:DATA_FLOW:ARRAY_INDEX_LINEAR_EXPRESSION	//STONESOUP:CONTROL_FLOW:INTERFILE_2

	/* On mousex + mousey == input filename length + 4, string termination occurs.
	 * If an image too small error occurs, a buffer overwrite occurs */
	keybuf[mousey - ((int)mx[0])] = 'k';	//STONESOUP:INTERACTION_POINT

	/* Only the very first 2 characters contain the ID */
	if (fread(buf, 2, 1, infil) != 1)
	{
		closeuperr(infil, otfil, keybuf, preamble, "Unable to read the ID field of %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	/* Identify the file type as a 'BM' file */
	if ((buf[0] != 'B') || (buf[1] != 'M'))
	{
		closeuperr(infil, otfil, keybuf, preamble, "BMP file %s is unknown type, expected 'B' 'M', found '%c' '%c'\n",
			argv[1], buf[0], buf[1]);
		return(1);
	}

	/* Get the rest of the initial header which contains the offset to the image */
	if (fread(&hdr, sizeof(hdr), 1, infil) != 1)
	{
		closeuperr(infil, otfil, keybuf, preamble, "Unable to read the header of %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	/* Check sanity of image offset */
	if (hdr.img_ofst >= hdr.fsize)
	{
		closeuperr(infil, otfil, keybuf, preamble, "BMP file %s has %lu bytes before image, but file size is %u\n",
			argv[1], hdr.img_ofst, hdr.fsize);
		return(1);
	}

	/* Get the rest of the image information */
	if (fread(&infohdr, sizeof(infohdr), 1, infil) != 1)
	{
		closeuperr(infil, otfil, keybuf, preamble, "Unable to read the infoheader of %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	/* DeCompression is not yet built into this package */
	if (infohdr.compress_type != 0)
	{
		closeuperr(infil, otfil, keybuf, preamble, "BMP file %s is compressed of type %u and this function does not handle compression\n",
			argv[1], infohdr.compress_type);
		return(1);
	}

	/* Also haven't tested anything other than 24 bits per pixel */
	if ((infohdr.bitsperpixel != 32) && (infohdr.bitsperpixel != 24))
	{
		closeuperr(infil, otfil, keybuf, preamble, "Can only process 24 or 32 bits per pixel in BMP file %s, not %d\n",
			argv[1], infohdr.bitsperpixel);
		return(1);
	}

	/* Start over, we keep the same file front end, nothing there changes */
	/* Since the files are identical except for the image pixel values, keep the front end the same */
	if (fseek(infil, 0, SEEK_SET))
	{
		closeuperr(infil, otfil, keybuf, preamble, "Unable to seek to 0 in BMP file %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	/* Check sanity of image offset */
	if ((preamble = malloc(hdr.img_ofst)) == NULL)
	{
		closeuperr(infil, otfil, keybuf, preamble, "BMP file %s, could not malloc %u bytes\n",
			argv[1], hdr.img_ofst);
		return(1);
	}

	if (fread(buf, hdr.img_ofst, 1, infil) != 1)
	{
		closeuperr(infil, otfil, keybuf, preamble, "Unable to read the infoheader of %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	if (fwrite(buf, hdr.img_ofst, 1, otfil) != 1)
	{
		closeuperr(infil, otfil, keybuf, preamble, "Unable to write the BMP ID of %s because of error %d\n",
			keybuf, errno);
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
					closeuperr(infil, otfil, keybuf, preamble,
						"1 Expected to read %u pixels from file %s, read %u\n\
Could not finish file %s\n",
						i, argv[1], j, keybuf);	//STONESOUP:CROSSOVER_POINT	//STONESOUP:TRIGGER_POINT
					return(1);
				}
				/* If there is a corrupt image, we run out of pixels early */
				if (feof(infil))
				{
					closeuperr(infil, otfil, keybuf, preamble,
						"Ran out of pixels in row %u of file %s, expected %u, found %u\n\
Could not finish file %s\n",
						row, argv[1], numrows * rowsz, (row * rowsz) + col, keybuf);
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
					closeuperr(infil, otfil, keybuf, preamble, "Expected to write %u pixels to file %s, wrote %u\n",
						i, keybuf, j);
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
					closeuperr(infil, otfil, keybuf, preamble,
						"2 Expected to read 1 pixel in row %u from file %s, read %u\n\
Could not finish file %s\n", row, argv[1], j, keybuf);
					return(1);
				}
				if (feof(infil))
				{
					closeuperr(infil, otfil, keybuf, preamble,
						"Ran out of pixels early in file %s, expected %d, found %d\n\
Could not finish file %s\n",
						argv[1], numrows * rowsz, (row * rowsz) + col, keybuf);
					return(1);
				}
				if (fwrite(p, 1, 1, otfil) != 1)
				{
					closeuperr(infil, otfil, keybuf, preamble, "Expected to write 1 pixel to file %s, wrote none\n",
						keybuf);
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
					closeuperr(infil, otfil, keybuf, preamble,
						"3 Expected to read %d pixels from file %s, read %d\n\
Could not finish file %s\n",
						i, argv[1], j, keybuf);
					return(1);
				}
				if (feof(infil))
				{
					closeuperr(infil, otfil, keybuf, preamble,
						"Ran out of pixels early in file %s, expected %d, found %d\n\
Could not finish file %s\n",
						argv[1], numrows * rowsz, (row * rowsz) + col, keybuf);
					return(1);
				}
				gray = (((unsigned int)p[0]) + ((unsigned int)p[1]) + ((unsigned int)p[2])) / 3;
				p[0] = gray;
				p[1] = gray;
				p[2] = gray;
				// Now write the value out
				if ((j = fwrite(p, 1, i, otfil)) != i)
				{
					closeuperr(infil, otfil, keybuf, preamble, "Expected to write %d pixels to file %s, wrote %d\n",
						i, keybuf, j);
					return(1);
				}
			}
		}
	}
/*
	else if (infohdr.bitsperpixel == 16)
	{
		int i = 2, j;
		unsigned char p[2];	// Nibble order: Green, Blue, Alpha, Red
		unsigned char blue, grn, red, alpha;
		unsigned char gray;

		for (row = 0; row < numrows; row++)
		{
			for (col = 0; col < rowsz; col += i)
			{
				if ((j = fread(p, 1, i, infil)) != i)
				{
					closeuperr(infil, otfil, keybuf, preamble,
						"Expected to read %d pixels from file %s, read %d\n\
Could not finish file %s\n",
						i, argv[1], j, keybuf);
					return(1);
				}
				if (feof(infil))
				{
					closeuperr(infil, otfil, keybuf, preamble, "Ran out of pixels early in file %s, expected %d, found %d\n\
Could not finish file %s\n",
						argv[1], numrows * rowsz, (row * rowsz) + col, keybuf);
					return(1);
				}
				blue = (p[0] >> 4) & 0xF;
				grn = p[0] & 0xF;
				alpha = (p[1] >> 4) & 0xF;
				red = p[1] & 0xF;
				gray = (blue + grn + red) / 3;
				p[0] = (gray << 4) + gray;
				p[1] = (alpha << 4) + gray;
				// Now write the value out
				if ((j = fwrite(p, 1, i, otfil)) != i)
				{
					closeuperr(infil, otfil, keybuf, preamble, "Expected to write %d pixels to file %s, wrote %d\n",
						i, argv[1], j);
					return(1);
				}
			}
			if (col != rowsz)
			{
				// Finish out the row
				for (col -= i; col < rowsz; col++)
				{
					if (fread(p, 1, 1, infil) != 1)
					{
						closeuperr(infil, otfil, keybuf, preamble,
							"Expected to read 1 pixel from file %s, read none\n\
Could not finish file %s\n",
							argv[1], keybuf);
						return(1);
					}
					if (feof(infil))
					{
						closeuperr(infil, otfil, keybuf, preamble,
							"Ran out of pixels early in file %s, expected %d, found %d\n\
Could not finish file %s\n",
							argv[1], numrows * rowsz, (row * rowsz) + col, keybuf);
						return(1);
					}
					// Now write the value out
					if (fwrite(p, 1, 1, otfil) != 1)
					{
						closeuperr(infil, otfil, keybuf, preamble, "Expected to write 1 pixel to file %s, wrote none\n",
							argv[1]);
						return(1);
					}
				}
			}
		}
	}
*/

	/* Write out the residual file which will be the same */
	while (!feof(infil))
	{
		int j;
		int i = fread(preamble, 1, hdr.img_ofst, infil);
		if (i > 0)
		{
			if ((j = fwrite(preamble, 1, i, otfil)) != i)
			{
				closeuperr(infil, otfil, keybuf, preamble, "Expected to write %d pixels to file %s, wrote %d\n",
					i, keybuf, j);
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

	return(0);
}

/* End of file */
