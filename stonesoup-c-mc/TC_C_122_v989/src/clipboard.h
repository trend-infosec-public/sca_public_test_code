

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/*
 * clipboard.h
 *
 *  Created on: Jun 9, 2011
 *      
 */



#ifndef CLIPBOARD_H_
#define CLIPBOARD_H_

int setClipboard(char *txt, ...);	//places text on clipboard. Returns 1 for success or 0 for failure.
char *getClipboard();			//retrieves text from clipboard in a malloc'd string. REMEMBER TO FREE
void clipboard_cleanup(char *buf);					//cleans up things that are opened/created if applicable.

#endif /* CLIPBOARD_H_ */
