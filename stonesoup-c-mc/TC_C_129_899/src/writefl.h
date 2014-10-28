

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/*
 * writefl.h
 *
 *  Created on: Oct 13, 2011
 *      
 */
#include <stdio.h>

#ifndef WRITEFL_H_
#define WRITEFL_H_
int stop();
int writeFLe(int i, int index, int end, char *newoutput, int *sequence, FILE *otfil);

#endif /* WRITEFL_H_ */
