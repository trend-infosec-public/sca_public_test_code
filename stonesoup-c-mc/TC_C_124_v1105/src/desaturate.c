
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
**  Spreadsheet Rev #: 1
**  CWE #: 124  Buffer Underwrite ('Buffer Underflow')
**
**		Description Summary
**  		The software writes to a buffer using an index or pointer that
**  		references a memory location prior to the beginning of the buffer.
**
**		Extended Description
**
**			This typically occurs when a pointer or its index is decremented to a
**			position before the buffer, when pointer arithmetic results in a
**			position before the beginning of the valid memory location, or when a
**			negative index is used.
**
**		Implementation Description
**
**			The CWE is based on the BMP image array being stored into a buffer using
**			a short int index which wraps around when the image exceeds
**			10k pixels (30k bytes).
**
**	Spreadsheet Variant: 1105
**		SOURCE_TAINT:FILE_NAME
**		DATA_TYPE:ARRAY_LENGTH_LITERAL
**		CONTROL_FLOW:INTERPROCEDURAL
**		DATA_FLOW:ADDRESS_ALIASING_1
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
void closeuperr (FILE *infil, FILE *otfil, const char *otflnm, void *preamble,
		void *img, const char *errmsg, ...)
{
	va_list vargs;
	va_start(vargs, errmsg);
	vfprintf(stderr, errmsg, vargs);
	va_end(vargs);
	fclose(infil);
	fclose(otfil);
	if (otfil)
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
unsigned int getimgary(FILE *infil, unsigned char *s, unsigned long sz)
{
	unsigned int len = 0;
	short int idx[1] = { 0 };	//STONESOUP:DATA_TYPE:ARRAY_LENGTH_LITERAL

	while ((feof(infil) == 0) && (sz > 0))
	{
		if (fread(s + idx[0], 1, 1, infil) != 1)	//STONESOUP:INTERACTION_POINT	//STONESOUP:CROSSOVER_POINT	//STONESOUP:TRIGGER_POINT	//STONESOUP:DATA_FLOW:ADDRESS_ALIASING_1
		{
				break;
		}
		sz--;
		len++;
		idx[0]++;
	}

	return(len);
}

/* Enter here to process! */
int main(int argc, char **argv)
{
	FILE *infil = NULL;	/* Input file pointer */
	FILE *otfil = NULL;	/* Output file pointer */
	struct BmpHdr hdr;	/* First part of the file header goes here */
	struct BmpInfoHdr *infohdr;	/* Most of the file information goes here */
	unsigned char c[3], *preamble = NULL, *img = NULL, *s;
	unsigned long rowsz, numrows, col, row;
	unsigned int i, j, ar[2] = { 5, 5 };
	unsigned int len;

	if (argc < 3)
	{
		fprintf(stderr, "Not enough arguments to command line\nExpected:\n\n");
		fprintf(stderr, "%s <BMP_input_filename.bmp> <BMP_output_filename.bmp>\n",
		"desaturate.exe");
		return(1);
	}

	if ((infil = fopen(argv[1], "rb")) == NULL)	//STONESOUP:SOURCE_TAINT:FILE_NAME
	{
		fprintf(stderr, "Unable to open input file %s because of error %d\n", argv[1], errno);
		return(1);
	}

	if ((otfil = fopen(argv[2], "wb")) == NULL)
	{
		fclose(infil);
		fprintf(stderr, "Unable to open output file %s because of error %d\n", argv[2], errno);
		return(1);
	}

	/* Get the rest of the initial header which contains the offset to the image */
	if (fread(c, 1, 2, infil) != 2)
	{
		closeuperr(infil, otfil, argv[2], preamble, img, "Unable to read the 'BM' identifier of %s because of error %d\n",
				argv[1], errno);
		return(1);
	}

	/* Identify the file type as a 'BM' file */
	if ((c[0] != 'B') || (c[1] != 'M'))
	{
		closeuperr(infil, otfil, argv[2], preamble, img, "BMP file %s is unknown type, expected 'B' 'M', found '%c' '%c'\n",
			argv[1], c[0], c[1]);
		return(1);
	}

	if (fwrite("BM", 1, 2, otfil) != 2)
	{
		closeuperr(infil, otfil, argv[2], preamble, img, "Unable to write the 'BM' BMP preamble of %s because of error %d\n",
			argv[2], errno);
		return(1);
	}

	/* Get the rest of the initial header which contains the offset to the image */
	if (fread(&hdr, sizeof(hdr), 1, infil) != 1)
	{
		closeuperr(infil, otfil, argv[2], preamble, img, "Unable to read the file header of %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	if (fwrite(&hdr, sizeof(hdr), 1, otfil) != 1)
	{
		closeuperr(infil, otfil, argv[2], preamble, img, "Unable to write the BMP header of %s because of error %d\n",
			argv[2], errno);
		return(1);
	}

	/* Check sanity of image offset */
	if (hdr.img_ofst >= hdr.fsize)
	{
		closeuperr(infil, otfil, argv[2], preamble, img, "BMP file %s has %lu bytes before image, but file size is %u\n",
			argv[1], hdr.img_ofst, hdr.fsize);
		return(1);
	}

	/* Check sanity of image offset */
	if ((preamble = malloc(hdr.img_ofst)) == NULL)
	{
		closeuperr(infil, otfil, argv[2], preamble, img, "BMP file %s, could not malloc %u bytes\n",
			argv[1], hdr.img_ofst);
		return(1);
	}

	/* Get the rest of the image information */
	if (fread(preamble, hdr.img_ofst - sizeof(hdr) - 2, 1, infil) != 1)
	{
		closeuperr(infil, otfil, argv[2], preamble, img, "Unable to read the infoheader of %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	if (fwrite(preamble, hdr.img_ofst - sizeof(hdr) - 2, 1, otfil) != 1)
	{
		closeuperr(infil, otfil, argv[2], preamble, img, "Unable to write the BMP descriptor of %s because of error %d\n",
			argv[2], errno);
		return(1);
	}

	infohdr = (struct BmpInfoHdr *)preamble;

	/* DeCompression is not yet built into this package */
	if (infohdr->compress_type != 0)
	{
		closeuperr(infil, otfil, argv[2], preamble, img, "BMP file %s is compressed of type %u and this function does not handle compression\n",
			argv[1], infohdr->compress_type);
		return(1);
	}

	/* Also haven't tested anything other than 24 bits per pixel */
	if ((infohdr->bitsperpixel != 32) && (infohdr->bitsperpixel != 24))
	{
		closeuperr(infil, otfil, argv[2], preamble, img, "Can only process 24 or 32 bits per pixel in BMP file %s, not %d\n",
			argv[1], infohdr->bitsperpixel);
		return(1);
	}

	/* The row length MUST be a multiple of 4 bytes */
	rowsz = (((infohdr->bitsperpixel * infohdr->wid) + 16) / 32) * 4;
	numrows = infohdr->hght < 0 ? -infohdr->hght : infohdr->hght;

	if (fseek(infil, 0, SEEK_END))
	{
		closeuperr(infil, otfil, argv[2], preamble, "Unable to seek to 0 in BMP file %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	len = ftell(infil);
	if (len < 1)
	{
		closeuperr(infil, otfil, argv[2], preamble, img, "Unable to get length of BMP file %s because of error %d\n",
			argv[1], errno);
		return(1);
	}

	if (fseek(infil, hdr.img_ofst, SEEK_SET))
	{
		closeuperr(infil, otfil, argv[2], preamble, img, "Unable to seek to image position %lu in BMP file %s because of error %d\n",
				hdr.img_ofst, argv[1], errno);
		return(1);
	}

	if ((img = malloc(len)) == NULL)
	{
		closeuperr(infil, otfil, argv[2], preamble, img, "BMP file %s, could not malloc %u bytes for image\n",
			argv[1], ar[1]);
		return(1);
	}

	// Load image by reading through file and loading into buffer
	s = img;
	len = hdr.fsize - hdr.img_ofst;
	unsigned int jj = getimgary(infil, img, len);	//STONESOUP:CONTROL_FLOW:INTERPROCEDURAL

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
					closeuperr(infil, otfil, argv[2], preamble, img, "Expected to read %u pixels from file %s, read %u\n",
						i, argv[1], len);
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
					closeuperr(infil, otfil, argv[2], preamble, img, "Expected to write %u pixels to file %s, wrote %u\n",
						i, argv[1], j);
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
					closeuperr(infil, otfil, argv[2], preamble, img, "Expected to read 1 pixel in row %u from file %s, read %u\n",
						row, argv[1], j);
					return(1);
				}
				len--;

				if (fwrite(s++, 1, 1, otfil) != 1)
				{
					closeuperr(infil, otfil, argv[2], preamble, img, "Expected to write 1 pixel to file %s, wrote none\n",
						argv[1]);
					return(1);
				}
			}
//closeuperr(infil, otfil, NULL, "\n");
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
					closeuperr(infil, otfil, argv[2], preamble, img, "Expected to read %u pixels from file %s, read %u\n",
						i, argv[1], len);
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
					closeuperr(infil, otfil, argv[2], preamble, img, "Expected to write %d pixels to file %s, wrote %d\n",
						i, argv[1], j);
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
				if ((j = fread(p, 1, i, infil)) != i)
				{
					closeuperr(infil, otfil, argv[2], preamble, img, "Expected to read %d pixels from file %s, read %d\n",
						i, argv[1], j);
					return(1);
				}
				if (feof(infil))
				{
					closeuperr(infil, otfil, argv[2], preamble, img, "Ran out of pixels early in file %s, expected %d, found %d\n",
						argv[1], numrows * rowsz, (row * rowsz) + col);
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
					closeuperr(infil, otfil, argv[2], preamble, img, "Expected to write %d pixels to file %s, wrote %d\n",
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
						closeuperr(infil, otfil, argv[2], preamble, img, "Expected to read 1 pixel from file %s, read none\n",
							argv[1]);
						return(1);
					}
					if (feof(infil))
					{
						closeuperr(infil, otfil, argv[2], preamble, img, "Ran out of pixels early in file %s, expected %d, found %d\n",
							argv[1], numrows * rowsz, (row * rowsz) + col);
						return(1);
					}
					// Now write the value out
					if (fwrite(p, 1, 1, otfil) != 1)
					{
						closeuperr(infil, otfil, argv[2], preamble, img, "Expected to write 1 pixel to file %s, wrote none\n",
							argv[1]);
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
				closeuperr(infil, otfil, argv[2], preamble, img, "Expected to write %d pixels to file %s, wrote %d\n",
					len, argv[1], j);
				return(1);
			}
		}

	free(preamble);

	if (fclose(infil) != 0)
	{
		fprintf(stderr, "Unable to close input file %s because of error %d\n", argv[1], errno);
		return(1);
	}

	if (fclose(otfil) != 0)
	{
		fprintf(stderr, "Unable to close output file %s because of error %d\n", argv[2], errno);
		fclose(infil);
		return(1);
	}

	return(0);
}

/* End of file */