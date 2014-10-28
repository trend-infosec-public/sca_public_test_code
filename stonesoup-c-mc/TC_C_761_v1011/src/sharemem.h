

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/*
 * sharemem.h
 *
 *  Created on: Jun 9, 2011
 *      
 */

#ifndef __SHAREMEM_H__
#define __SHAREMEM_H__

int openshmem(char *lockfilname, unsigned int keyin, int sz);
int closeshmem(void);
int setshmem(char *str);
char *getshmem(void);

#endif /* SHAREMEM_H_ */
