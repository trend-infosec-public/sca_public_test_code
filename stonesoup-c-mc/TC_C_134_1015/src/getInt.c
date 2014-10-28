
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
 * signalH.c
 *
 *  Created on: Oct 18, 2011
 *      
 */

int fincalc(int total, int d, int z, int *registers){
	if(z< 10){
		return total /= d; //STONESOUP:TRIGGER_POINT
	}
	total += registers[z];
	return fincalc(total, d, ++z, registers);
}
