
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

/*
 * getInt.c
 *
 *  Created on: Oct 4, 2011
 *      
 */

int getInt(int a){
	int x = (a/a) + 99;
	return x;
}
int *getPoint(int b){
	b += (b*2)+10;
	return (int*) b;
}
int getStart(){
	int a = 0;
	return a;
}
