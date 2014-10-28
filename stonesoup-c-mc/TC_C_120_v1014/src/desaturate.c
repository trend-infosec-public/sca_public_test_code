
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

/**************************************************************/
/************************************************************************
**
**
**  
**  Date: 5/1/2011
**
**	Description:
**	Desaturate a Color BMP File to produce a grayscale file
**
**  Spreadsheet Rev #: 1
**  CWE #: 120  Buffer Copy without Checking Size of Input ('Classic Buffer Overflow')
**
**		Description Summary
**  		The program copies an input buffer to an output buffer without verifying
**  		that the size of the input buffer is less than the size of the output
**  		buffer, leading to a buffer overflow.
**
**		Extended Description
**
**			A buffer overflow condition exists when a program attempts to put more
**			data in a buffer than it can hold, or when a program attempts to put
**			data in a memory area outside of the boundaries of a buffer. The
**			simplest type of error, and the most common cause of buffer overflows,
**			is the "classic" case in which the program copies the buffer without
**			restricting how much is copied. Other variants exist, but the existence
**			of a classic overflow strongly suggests that the programmer is not
**			considering even the most basic of security protections.
**
**		Implementation Description
**
**			The CWE is based on the BMP image array length contained in the filename
**			This array length is assumed to be correct and memory is allocated to
**			hold the complete array. If the length is bad, (i.e. shorter than the
**			array length) an overwrite will occur when the image is loaded.
**
**	Spreadsheet Variant: 1014
**		SOURCE_TAINT:STDIN
**		DATA_TYPE:SHORT_CHARACTER
**		CONTROL_FLOW:COUNT_CONTROLLED_LOOP
**		DATA_FLOW:ARRAY_INDEX_VARIABLE
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
#include <malloc.h>

#if _WIN32
#include <fcntl.h>
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
void closeuperr (FILE *otfil, const char *otflnm, void *preamble,
		void *img, const char *errmsg, ...)
{
	va_list vargs;
	va_start(vargs, errmsg);
	vfprintf(stderr, errmsg, vargs);
	va_end(vargs);
	fclose(otfil);
	if (otflnm)
	{
		remove(otflnm);
	}

	if (preamble)
	{
		free(preamble);
	}

	if (img)
	{
		free(img);
	}
}

// Load image by reading through file and loading into buffer
unsigned long int getimgary(unsigned char *s)
{
	unsigned long int len;
	for (len = 0; (len < 3000000000UL) && (feof(stdin) == 0); len++)	//STONESOUP:CONTROL_FLOW:COUNT_CONTROLLED_LOOP
	{
		if (fread(s, 1, 1, stdin) != 1)
		{
				break;
		}
		s++;
	}

	return(len);
}

/* Enter here to process! */
int main(int argc, char **argv)	//STONESOUP:SOURCE_TAINT:STDIN
{
	FILE *otfil = NULL;	/* Output file pointer */
	struct BmpHdr hdr;	/* First part of the file header goes here */
	struct BmpInfoHdr *infohdr;	/* Most of the file information goes here */
	unsigned char c[3], *preamble = NULL, *img = NULL, *s;	//STONESOUP:DATA_TYPE:SHORT_CHARACTER
	unsigned long rowsz, numrows, col, row;
	unsigned int i, j;
	unsigned long int len;

	if (argc < 2)
	{
		fprintf(stderr, "Not enough arguments to command line\nExpected:\n\n");
		fprintf(stderr, "%s <BMP_output_filename.bmp> < <BMP_input_filename.bmp> \n",
		"desaturate.exe");
		return(1);
	}

#if _WIN32
	_setmode(_fileno(stdin), _O_BINARY);
#endif


	if ((otfil = fopen(argv[1], "wb")) == NULL)
	{
		fprintf(stderr, "Unable to open output file because of error %d\n", errno);
		return(1);
	}

	/* Get the rest of the initial header which contains the offset to the image */
	if (fread(c, 1, 2, stdin) != 2)
	{
		closeuperr(otfil, argv[1], preamble, img, "Unable to read the 'BM' identifier of %s because of error %d\n",
			"stdin", errno);
		return(1);
	}

	/* Identify the file type as a 'BM' file */
	if ((c[0] != 'B') || (c[1] != 'M'))
	{
		closeuperr(otfil, argv[1], preamble, img, "BMP file %s is unknown type, expected 'B' 'M', found '%c' '%c'\n",
			"stdin", c[0], c[1]);
		return(1);
	}

	if (fwrite("BM", 1, 2, otfil) != 2)
	{
		closeuperr(otfil, argv[1], preamble, img, "Unable to write the 'BM' BMP preamble of %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	/* Get the rest of the initial header which contains the offset to the image */
	if (fread(&hdr, sizeof(hdr), 1, stdin) != 1)
	{
		closeuperr(otfil, argv[1], preamble, img, "Unable to read the file header of %s because of error %d\n",
			"stdin", errno);
		return(1);
	}

	if (fwrite(&hdr, sizeof(hdr), 1, otfil) != 1)
	{
		closeuperr(otfil, argv[1], preamble, img, "Unable to write the BMP header of %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	/* Check sanity of image offset */
	if (hdr.img_ofst >= hdr.fsize)
	{
		closeuperr(otfil, argv[1], preamble, img, "BMP file %s has %lu bytes before image, but file size is %u\n",
			"stdin", hdr.img_ofst, hdr.fsize);
		return(1);
	}

	/* Check sanity of image offset */
	if ((preamble = malloc(hdr.img_ofst)) == NULL)
	{
		closeuperr(otfil, argv[1], preamble, img, "BMP file %s, could not malloc %u bytes\n",
			"stdin", hdr.img_ofst);
		return(1);
	}

	/* Get the rest of the image information */
	if (fread(preamble, hdr.img_ofst - sizeof(hdr) - 2, 1, stdin) != 1)
	{
		closeuperr(otfil, argv[1], preamble, img, "Unable to read the infoheader of %s because of error %d\n",
			"stdin", errno);
		return(1);
	}

	if (fwrite(preamble, hdr.img_ofst - sizeof(hdr) - 2, 1, otfil) != 1)
	{
		closeuperr(otfil, argv[1], preamble, img, "Unable to write the BMP descriptor of %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	infohdr = (struct BmpInfoHdr *)preamble;

	/* DeCompression is not yet built into this package */
	if (infohdr->compress_type != 0)
	{
		closeuperr(otfil, argv[1], preamble, img, "BMP file %s is compressed of type %u and this function does not handle compression\n",
			"stdin", infohdr->compress_type);
		return(1);
	}

	/* Also haven't tested anything other than 24 bits per pixel */
	if ((infohdr->bitsperpixel != 32) && (infohdr->bitsperpixel != 24))
	{
		closeuperr(otfil, argv[1], preamble, img, "Can only process 24 or 32 bits per pixel in BMP file %s, not %d\n",
			"stdin", infohdr->bitsperpixel);
		return(1);
	}

	/* The row length MUST be a multiple of 4 bytes */
	rowsz = (((infohdr->bitsperpixel * infohdr->wid) + 16) / 32) * 4;
	numrows = infohdr->hght < 0 ? -infohdr->hght : infohdr->hght;

	int aiv[2], n = 1;
	aiv[n] = hdr.fsize - hdr.img_ofst + 4;
	if ((img = malloc(aiv[n])) == NULL)	//STONESOUP:DATA_FLOW:ARRAY_INDEX_VARIABLE
	{
		closeuperr(otfil, argv[1], preamble, img, "BMP file %s, could not malloc %u bytes for image\n",
			"stdin", aiv[n]);
		return(1);
	}

	// Load image by reading through file and loading into buffer
	s = img;
	len = getimgary(img);	//STONESOUP:INTERACTION_POINT	//STONESOUP:CROSSOVER_POINT	//STONESOUP:TRIGGER_POINT

	/* Make sure we only do the 24 bits per pixel */
	if (infohdr->bitsperpixel == 24)
	{
		unsigned char p[3];	/* Byte order: Blue, Green, Red */
		unsigned char gray;
		unsigned int r, g, b;
		i = 3;	/* The number of bytes per pixel to read and write */
		long rowsz_mod = rowsz - (infohdr->wid * 3);
//printf("%d %d\n", rowsz_mod, rowsz);

		/* Loop through each row of the image */
		for (row = 0; row < numrows; row++)
		{
			/* Loop through each pixel of the image */
			for (col = 0; col < infohdr->wid; col++)
			{
				/* Inefficient, but we only get 1 pixel at a time */
				/* No buffers to overflow or keep track of */
				if (len < i)
				{
					closeuperr(otfil, argv[1], preamble, img, "Expected to read %u pixels from file %s, read %u\n",
						i, "stdin", len);
					return(1);
				}
				len -= i;

				/* Average all of the brightness values to obtain the grayscale value */
				r = *s++;
				g = *s++;
				b = *s++;
				gray = (r + g + b) / 3;
				/* Now store the value into each color */
				p[0] = gray;
				p[1] = gray;
				p[2] = gray;
				/* Now write the value out */
				if ((j = fwrite(p, 1, i, otfil)) != i)
				{
					closeuperr(otfil, argv[1], preamble, img, "Expected to write %u pixels to file %s, wrote %u\n",
						i, "stdin", j);
					return(1);
				}
			}
//printf("%d %d\n", row, col);

			/* Finish out the row */
			for (col = 0; col < rowsz_mod; col++)
			{
//printf("%d\n", col);
				if (len < 1)
				{
					closeuperr(otfil, argv[1], preamble, img, "Expected to read 1 pixel in row %u from file %s, read %u\n", row, "stdin", j);
					return(1);
				}
				len--;

				if (fwrite(s++, 1, 1, otfil) != 1)
				{
					closeuperr(otfil, argv[1], preamble, img, "Expected to write 1 pixel to file %s, wrote none\n",
						"stdin");
					return(1);
				}
			}
//closeuperr(otfil, NULL, "\n");
//return(1);
		}
	}
	else if (infohdr->bitsperpixel == 32)
	{
		unsigned char p[4];	// Byte order: Blue, Green, Red, Alpha
		unsigned char gray;
		unsigned int r, g, b;
		i = 4;

		for (row = 0; row < numrows; row++)
		{
			for (col = 0; col < rowsz; col += i)
			{
				if (len < i)
				{
					closeuperr(otfil, argv[1], preamble, img, "Expected to read %u pixels from file %s, read %u\n",
						i, "stdin", len);
					return(1);
				}
				len -= i;
				r = *s++;
				g = *s++;
				b = *s++;
				gray = (r + g + b) / 3;
				p[0] = gray;
				p[1] = gray;
				p[2] = gray;
				p[3] = *s++;
				// Now write the value out
				if ((j = fwrite(p, 1, i, otfil)) != i)
				{
					closeuperr(otfil, argv[1], preamble, img, "Expected to write %d pixels to file %s, wrote %d\n",
						i, "stdin", j);
					return(1);
				}
			}
		}
	}
/*
	else if (infohdr->bitsperpixel == 16)
	{
		int i = 2, j;
		unsigned char p[2];	// Nibble order: Green, Blue, Alpha, Red
		unsigned char blue, grn, red, alpha;
		unsigned char gray;

		for (row = 0; row < numrows; row++)
		{
			for (col = 0; col < rowsz; col += i)
			{
				if ((j = fread(p, 1, i, stdin)) != i)
				{
					closeuperr(otfil, argv[1], preamble, img, "Expected to read %d pixels from file %s, read %d\n",
						i, "stdin", j);
					return(1);
				}
				if (feof(stdin))
				{
					closeuperr(otfil, argv[1], preamble, img, "Ran out of pixels early in file %s, expected %d, found %d\n",
						"stdin", numrows * rowsz, (row * rowsz) + col);
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
					closeuperr(otfil, argv[1], preamble, img, "Expected to write %d pixels to file %s, wrote %d\n",
						i, "stdin", j);
					return(1);
				}
			}
			if (col != rowsz)
			{
				// Finish out the row
				for (col -= i; col < rowsz; col++)
				{
					if (fread(p, 1, 1, stdin) != 1)
					{
						closeuperr(otfil, argv[1], preamble, img, "Expected to read 1 pixel from file %s, read none\n",
							"stdin");
						return(1);
					}
					if (feof(stdin))
					{
						closeuperr(otfil, argv[1], preamble, img, "Ran out of pixels early in file %s, expected %d, found %d\n",
							"stdin", numrows * rowsz, (row * rowsz) + col);
						return(1);
					}
					// Now write the value out
					if (fwrite(p, 1, 1, otfil) != 1)
					{
						closeuperr(otfil, argv[1], preamble, img, "Expected to write 1 pixel to file %s, wrote none\n",
							"stdin");
						return(1);
					}
				}
			}
		}
	}
*/

	/* Write out the residual file which will be the same */
		if (len > 0)
		{
			if ((j = fwrite(s, 1, len, otfil)) != len)
			{
				closeuperr(otfil, argv[1], preamble, img, "Expected to write %d pixels to file %s, wrote %d\n",
					len, "stdin", j);
				return(1);
			}
		}

	free(preamble);

	if (fclose(otfil) != 0)
	{
		fprintf(stderr, "Unable to close output file because of error %d\n", errno);
		fclose(stdin);
		return(1);
	}

	return(0);
}

/* End of file */
