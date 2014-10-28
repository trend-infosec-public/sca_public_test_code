
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

/*******************************************
**
**
** 
** Date: Jul 29, 2011
**
** Base Test Program -- TheNextPalindromeC
**
** Finds the next palindrome numerically following an integer under 1,000,000 digits long
**
** Variant Test Case Program
**
** If the entered string is not all ints, the program fails to reset the pointer into the allocated buffer, and will proceed to free it from the undesired point.
**
** STONESOUP Weakness Class: <Buffer Overflows>
** CWE ID: CWE-761
** Variant Spreadsheet Rev #:
** Variant Spreadsheet ID:
**
** Variant Features:
**		SOURCE_TAINT:SHARED_MEMORY
**		DATA_TYPE:SIGNED_INT
**		CONTROL_FLOW:COUNT_CONTROLLED_LOOP
**		DATA_FLOW:SIMPLE
**
** Initial Testing: (x means yes, - means no)
**   Tested in MS Windows XP 32bit        x
**   Tested in MS Windows 7  64bit        -
**   Tested in Ubuntu10_10 Linux 32bit    x
**   Tested in Ubuntu10_10 Linux 64bit    -
**
** Workflow:
**   Created:<data started>
**   1st Vett: <programmer> on <date>
**   2nd Vett: <peer> on <date>
**   3rd Vett: <teamleader> on <date>
**   4th Vett: Fortify Issues Fixed on <date>
**   5th Vett: Tested in Harness on <date>
**
**
** I/0 Pairs:
**   Good:
**    Bad:
*********************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include "sharemem.h"

//Checks a character array. Returns 1 if it only contains ints, 0 otherwise.
int isAllInts(char **k);
//Checks a character array. Returns 1 if it only contains 9s, 0 otherwise.
int isAllNines(char *k);
//Adds two integers, a & b represented by character strings. Puts the resultant value into ret.
void add(char *a, char *b, char *ret, int ret_len);
//substring
void substr(char *src, int iStart, int iLen, char *dst);
//Reverses string k
void reverse(char *src, char *dst, int len);
//Returns TRUE if the integer represented by string left is GREATER THAN the integer represented by right
int greaterThan(char *left, int left_len, char *right, int right_len);

int isAllInts(char **k)
{
	int i = 0;
	int v;
	char* start = *k;
	int k_len = strlen(*k);
	for(i = 0; i < k_len; i++)	//STONESOUP:CONTROL_FLOW:COUNT_CONTROLLED_LOOP
	{
		v = *((*k)++); //printf("Value: %c\n", v);
		if(v < 48 || v > 57)
			return 0;	//STONESOUP:CROSSOVER_POINT
	}

	*k = start;
	return 1;
}

//substring function
void substr(char *src, int iStart, int iLen, char *dst)
{
	int i;
	memset(dst, 0, iLen + 1);//zero the destination array

	for(i = 0; i < iLen; i++)
		dst[i] = src[i + iStart];
	dst[iLen] = '\0';

	//printf("String: %s; iStart: %i; iLen: %i; Result: %s;\n", s, iStart, iLen, tmp);
}

//Determines if the input string consists of only the number 9.
int IsAllNines(char* k)
{
	//TODO: Should i ask for the length of K here?
	unsigned int x;
	unsigned int len_k = strlen(k);
	for(x = 0; x < len_k; x++)
		if(k[x]-48 != 9)
			return 0;

	return 1;
}

//This function adds two integers represented as strings and returns the sum, represented as a string
//ret MUST BE equal to Max(len a, len b) + 2
void add(char *a, char *b, char *ret, int ret_len)
{
	int a_len = strlen(a);
	int b_len = strlen(b);
	int carry = 0;
	int pos = 1;
	memset(ret, 0, ret_len + 1);

	//Iterate right to left, adding as we go
	while(pos <= a_len || pos <= b_len || carry != 0)
	{
		unsigned int a_int, b_int, sum;
		//printf("pos: %i\n", pos);

		a_int = (pos > a_len ? '0' : a[a_len-pos]) - 48; //printf("a_int: %i\n", a_int);
		b_int = (pos > b_len ? '0' : b[b_len-pos]) - 48; //printf("b_int: %i\n", b_int);
		sum = a_int + b_int + carry; //printf("sum: %i\n", sum);
		ret[ret_len - pos] = (sum % 10) + 48; //printf("ret[%i - %i] = %c\n", ret_len, pos, (sum % 10) + 48);
		carry = (sum - sum % 10)/10; //printf("carry: %i\n", carry);
		pos++;
	}

	//We have completed adding A and B. The sum is in *ret
	//However, the sum may or may not begin in ret[0]. If the last execution of the WHILE loop
	//resulted in a CARRY (as would happen for 9222 + 9222), ret[0] will be non-null.
	//If the last execution of the WHILE loop did not result in a carry (as would happen for 111 + 111),
	//ret[0] will be null.
	//If ret[0] is null, we need to shift the data to the left by one. This will be done by ret[0] = ret[1] ... ret[n] = ret[n + 1];

	if(ret[0] == '\0')
	{
		int i;
		//printf("ret[0] = null; ret_len = %i\n", ret_len);
		for(i = 0; i+1 < ret_len; i++)
			ret[i] = ret[i + 1];

		ret[ret_len - 1] = '\0';
	}

	/*if(1)
	{
		int i;
		for (i = 0; i < ret_len + 1; i++)
			printf("ret[%i] = %c\n", i, ret[i]);
	}*/

	//printf("result was %s\n", ret);
}

//Determines if the input string consists of only the number 9.
int isAllNines(char* k)
{
	//TODO: Should i ask for the length of K here?
	unsigned int x;
	unsigned int len_k = strlen(k);
	for(x = 0; x < len_k; x++)
		if(k[x]-48 != 9)
			return 0;

	return 1;
}

//Reverses string k
void reverse(char *src, char *dst, int len)
{
	int i;

	//printf("len = %i\n", len);
	//printf("src[0] = %c, src[1] = %c\n", src[0], src[1]);
	//printf("src = %s\n", src);

	memset(dst, 0, len + 1);
	for(i = 0; i < len; i++)
	{
		dst[i] = src[len-i-1];
	}

	//printf("dst[0] = %c, dst[1] = %c\n", dst[0], dst[1]);
	//printf("dst = %s\n", dst);
}

//Returns TRUE if the integer represented by string left is GREATER THAN the integer represented by right
int greaterThan(char *left, int left_len, char *right, int right_len)
{
	int i;

	//The longer string is greater
	if(left_len != right_len)
		return left_len > right_len;

	//At this point we know the strings are the same length

	//Go from most significant digit to least, comparing along the way.
	for(i = 0; i < left_len; i++)
		if(left[i] != right[i])
			return left[i] > right[i];

	//If the code gets here, the strings are equal, so return FALSE
	return 0;
}

int main(int argc, char** argv)
{
	char *k;
	int k_len;	//STONESOUP:DATA_TYPE:SIGNED_INT
	int isLengthOdd;
	int half_k_len;
	char* k_left;
	char* k_left_plus_one;
	char* k_middle;
	char* k_right;
	char* k_left_reverse;
	char* k_left_plus_one_reverse;
	int isLeftReverseBigger;

	if(argc != 3)
	{
		fprintf(stderr, "Error: %s <lock filename> <key value>\n", argv[0]);
		return -1;
	}

	fprintf(stderr, "Lockfile name: '%s'\n", argv[1]);
	fprintf(stderr, "Key: %d (%s)\n", atoi(argv[2]), argv[2]);

	if (openshmem(argv[1], atoi(argv[2]), 1024))
	{
		fprintf(stderr, "Error: Could not set up memory access\n");
		return -1;
	}

	char *c = getshmem();	//STONESOUP:SOURCE_TAINT:SHARED_MEMORY	//STONESOUP:INTERACTION_POINT
	if (c == NULL)
	{
		fprintf(stderr, "Error: Could not retrieve shared memory");
		return -1;
	}

	k = malloc(strlen(c) + 1); 	//STONESOUP:DATA_FLOW:SIMPLE
	substr(c, 0, strlen(c), k);
	closeshmem();

	k_len = strlen(k);	//printf("k_len: %i\n", k_len);

	if(k_len > 1000000)
	{
		free(k);
		printf("Error: Input length was greater than 1,000,000 characters");
		return -1;
	}

	if(!isAllInts(&k))
	{
		free(k);	//STONESOUP:TRIGGER_POINT
		printf("Error: Input did not contain only numbers [0-9]");
		exit(-1);
	}

	//If the string is all nines, the next highest palindrome is always k+2
	if(isAllNines(k))
	{
		char *result = calloc(k_len + 2, sizeof(char));
		if(result == NULL)
		{
			free(k);
			printf("Error: Unable to calloc %d characters", k_len + 2);
			exit(-1);
		}

		add(k, "2", result, k_len + 1);
		printf("%s", result);
		free(k);
		free(result);
		result = NULL;
		return 0;
	}

	//If the string is not nine, but is a single digit (ie 0-8), the next highest palindrom is k+1
	if(k_len == 1)
	{
		char *result = calloc(k_len + 2, sizeof(char));
		if(result == NULL)
		{
			free(k);
			printf("Error: Unable to calloc %d characters", k_len + 2);
			exit(-1);
		}

		add(k, "1", result, k_len + 1);
		printf("%s", result);
		free(result);
		return 0;
	}



	//Compute/initialize all variables that I might use
	isLengthOdd = k_len % 2 == 1;									//printf("odd? '%i'\n", isLengthOdd);
	half_k_len = (k_len - isLengthOdd)/2;							//printf("half_k_len: '%i'\n", half_k_len);
	k_left = calloc(half_k_len + 1, sizeof(char));
	if(k_left == NULL)
	{
		free(k);
		printf("Error: Unable to calloc %d characters", half_k_len + 1);
		exit(-1);
	}
	k_left_plus_one = calloc(half_k_len + 2, sizeof(char));
	if(k_left_plus_one == NULL)
	{
		free(k);
		free(k_left);
		printf("Error: Unable to calloc %d characters", half_k_len + 2);
		exit(-1);
	}
	k_middle = calloc(2, sizeof(char));
	if(k_middle == NULL)
	{
		free(k);
		free(k_left);
		free(k_left_plus_one);
		printf("Error: Unable to calloc %d characters", 2);
		exit(-1);
	}
	k_middle[1] = '\0';
	k_right = calloc(half_k_len + 1, sizeof(char));
	if(k_right == NULL)
	{
		free(k);
		free(k_left);
		free(k_left_plus_one);
		free(k_middle);
		printf("Error: Unable to calloc %d characters", half_k_len + 1);
		exit(-1);
	}
	k_left_reverse = calloc(half_k_len + 1, sizeof(char));
	if(k_left_reverse == NULL)
	{
		free(k);
		free(k_left);
		free(k_left_plus_one);
		free(k_middle);
		free(k_right);
		printf("Error: Unable to calloc %d characters", half_k_len + 1);
		exit(-1);
	}
	k_left_plus_one_reverse = calloc(half_k_len + 2, sizeof(char));
	if(k_left_plus_one_reverse == NULL)
	{
		free(k);
		free(k_left);
		free(k_left_plus_one);
		free(k_middle);
		free(k_right);
		free(k_left_reverse);
		printf("Error: Unable to calloc %d characters", half_k_len + 2);
		exit(-1);
	}

	substr(k, 0, half_k_len, k_left);														//printf("k_left: '%s'\n", k_left);
	add(k_left, "1", k_left_plus_one, half_k_len + 1);										//printf("k_left_plus_one: '%s'\n", k_left_plus_one);
	k_middle[0] = (isLengthOdd?k[half_k_len]:'\0');											//printf("k_middle: '%s'\n", k_middle == ""?"Not Set":k_middle);
	substr(k, half_k_len + isLengthOdd, half_k_len, k_right);								//printf("k_right: '%s'\n", k_right);
	reverse(k_left, k_left_reverse, half_k_len);											//printf("k_left_reverse: '%s'\n", k_left_reverse);
	reverse(k_left_plus_one, k_left_plus_one_reverse, strlen(k_left_plus_one));				//printf("k_left_plus_one_reverse: '%s'\n", k_left_plus_one_reverse);
	isLeftReverseBigger = greaterThan(k_left_reverse, half_k_len, k_right, half_k_len);		//printf("isLeftReverseBigger: '%i'\n", isLeftReverseBigger);

	if(isLengthOdd)
	{
		if(isLeftReverseBigger)
			printf("%s%s%s", k_left, k_middle, k_left_reverse);
		else
			printf("%s%c%s", k_left, k_middle[0]+1, k_left_reverse);
	}
	else//Length is even
	{
		if(isLeftReverseBigger)
			printf("%s%s", k_left, k_left_reverse);
		else
			printf("%s%s", k_left_plus_one, k_left_plus_one_reverse);
	}


	//Free allocated memory
	free(k);
	free(k_left);
	free(k_left_plus_one);
	free(k_middle);
	free(k_right);
	free(k_left_reverse);
	free(k_left_plus_one_reverse);

	return 0;
}
