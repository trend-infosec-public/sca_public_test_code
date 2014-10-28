

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/*
 * interfile.h
 *
 *  Created on: Dec 1, 2011
 *      
 */

#ifndef INTERFILE_H_
#define INTERFILE_H_

void closeuperr (FILE *infil, FILE *otfil, const char *otflnm, void *preamble, const char *errmsg, ...);
void interfile (FILE *infil, FILE *otfil, const char *otflnm, void *preamble, const char *errmsg, unsigned int i1, char *s1, unsigned int i2, char *s2);

#endif /* INTERFILE_H_ */

/* End of file */
