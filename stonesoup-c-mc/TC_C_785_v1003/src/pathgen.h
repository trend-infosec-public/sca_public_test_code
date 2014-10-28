

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/*
 * realpath.h
 *
 *  Created on: Sep 13, 2011
 *      
 */

#ifndef REALPATH_H_
#define REALPATH_H_

char pathgen(char *path, char *resolved_path, int len);
int createDirectory(char *directoryname);

#endif /* REALPATH_H_ */
