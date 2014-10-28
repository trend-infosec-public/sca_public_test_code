
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
** Date: 9/16/11
**
**  Base Test Program -- SolitareEncryptionAlgorithm.c
**
** The program does this:
** 1) Reads in up to 25 bytes from a key file, and converts those characters into a 32-bit unsigned integer
** 2) Using that 32-bit unsigned integer as a seed, creates a shuffled deck of 54 cards (with jokers)
** 3) Read in a message to be encoded
** 4) Strip the message of all non-alphabet-letters
** 5) Read a new keystream value from the deck by
** 	a) Moving the first joker one spot down the deck
** 	b) Moving the second joker two spots down the deck
** 	c) Cutting the deck into three parts, and reversing the order of them relative to each other, delimited by the jokers
** 	d) Cutting the deck by as many cards as the value of the bottom card
** 	e) The next keystream value is the nth card from the top, where n is the card at the top of the deck
** 	f) The deck stays as it ends up
** 6) Encrypt the first character by adding the keystream value to it, mod 26
** 7) Perform steps 5-6 until done
**
** Decryption works similarly.
**
** See http://en.wikipedia.org/wiki/Solitaire_(cipher)
**
** Variant Test Case Program
**encrypt input.txt output.txt output 70
** The program takes in additional parameters: encrypt/decrypt, filename to be encrypted, filename to save
** as, directory to outout to, and buffer for use with realpath().  Mingw does not have a implementation
** for realpath so for windows I had to create my own.  First the program creates an array
** to store the path into using buffer for its size.  We then run realpath if we are in linux or some
** windows functions that do that same thing.  These functions get the current directory and append the
** directory the user provides, to create the final directory the output should be stored in.  When we get
** the buffer from the command line we check to see if it is smaller than 45 and not negative.  But if a
** user provides a number in the range of 65467 - 65592 than overflow in the integer occurs and we get a
** number between 0-45.  These numbers will allow us to create a folder in any place we want and will copy
** our decrypted file there.  We could create and encrypt malicious code and then with a carefully created
** command put the file anywhere we want.
**
** STONESOUP Weakness Class: <weakness class>
** CWE ID: CWE-785
** Variant Spreadsheet Rev #: Web site
** Variant Spreadsheet ID: 1003
**
** Variant Features:
**		SOURCE_TAINT:KEYBOARD
**		DATA_TYPE:UNSIGNED_SHORT
**		CONTROL_FLOW:ELSE_CONDITIONAL
**		DATA_FLOW:ARRAY_INDEX_CONSTANT
**
** Initial Testing: (x means yes, - means no)
**   Tested in MS Windows XP 32bit        x
**   Tested in MS Windows 7  64bit        x
**   Tested in Ubuntu10_10 Linux 32bit    x
**   Tested in Ubuntu10_10 Linux 64bit    -
**
** Workflow:
**   Created:7/18/11
**   1st Vett:  on 7/18/11
**   2nd Vett: <peer> on <date>
**   3rd Vett:
**   4th Vett: Fortify Issues Fixed on <date>
**   5th Vett: Tested in Harness on <date>
**
**
** I/0 Pairs:
**   Good: 1st Set: encrypt input.txt output.txt output 70
**         2nd Set: encrypt input.txt message.txt important 46
**    Bad: 1st Set:	encrypt input.txt output.txt output 65541
*********************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <assert.h>
#include <limits.h>
#include <errno.h>

#include "random_lw.h"

#ifdef _WIN32
#define _WIN32_WINNT 0x0600
#include <stdarg.h>
#include <windef.h>
#include <winbase.h>
#else
#include <linux/limits.h>
#include <unistd.h>
#endif

#include "pathgen.h"
#include "MouseKeyboard.h"

#define KEY_LENGTH 25
#define DECK_SIZE 54
#define ALPHA_SIZE 26
#define FIRST_JOKER 53
#define SECOND_JOKER 54
#define MAX_MESSAGE_LENGTH 100

#define PRT_ERR(s...) fprintf(stderr, s); fflush(stderr)
#define PRT_MSG(s...) fprintf(stdout, s); fflush(stdout)

typedef int Card;

void print_deck (Card *deck) {
	int i;
	for (i = 0; i < DECK_SIZE; i++) {
		PRT_MSG("%d ", deck[i]);
	}
	PRT_MSG("\n");
}

int check_deck(Card *deck) {
	int i;
	int sum1 = 0;
	int sum2 = 0;
	for (i = 0; i < DECK_SIZE; i++) {
		sum1 += deck[i];
		sum2 += (i+1);
	}
	if (sum1 == sum2) {
		return 1;
	}
	return 0;
}

//In-place Fischer-Yates shuffle
void shuffle(Card *deck, unsigned int seed) {
	int cards_left = DECK_SIZE;
	int next_index;
	int cur_index;
	Card temp;
	srand1(seed);
	while (cards_left != 0) {
		next_index = DECK_SIZE - rand1() % cards_left - 1;
		cur_index = DECK_SIZE - cards_left;
		temp = deck[cur_index];
		deck[cur_index] = deck[next_index];
		deck[next_index] = temp;
		cards_left--;
	}
}

//turn the key into a 32-bit unsigned int via questionably required
unsigned int seedify (char *key) {
	int i;
	unsigned int seed = 0;
	for (i = 0; i < KEY_LENGTH; i++) {
		seed += (unsigned int)(key[i]) << (i % 32);
	}
	return seed;
}

//randomly initializes the deck
void init_deck(Card *deck, char *key) {
	int i;
	for (i = 0; i < DECK_SIZE; i++) {
		deck[i] = (Card)(i+1);
	}
	shuffle(deck, seedify(key));
}

int indexof (Card *deck, Card c) {
	int i;
	for (i = 0; i < DECK_SIZE; i++) {
		if (deck[i] == c) {
			return i;
		}
	}
	return -1;
}

void joker_shift(Card *deck) {
	int fji = indexof(deck, FIRST_JOKER);
	if (fji < DECK_SIZE - 1 && fji >= 0) {
		deck[fji] = deck[fji+1];
		deck[fji+1] = FIRST_JOKER;
		fji++;
	}
	int sji = indexof(deck, SECOND_JOKER);
	if (sji < DECK_SIZE - 1 && sji >= 0) {
		deck[sji] = deck[sji+1];
		deck[sji+1] = SECOND_JOKER;
		sji++;
	}
	if (sji < DECK_SIZE - 1 && sji >= 0) {
		deck[sji] = deck[sji+1];
		deck[sji+1] = SECOND_JOKER;
		sji++;
	}
}

void triple_cut(Card *deck) {
	Card helper_deck[DECK_SIZE];
	int fji = indexof(deck, FIRST_JOKER);
	int sji = indexof(deck, SECOND_JOKER);
	if (fji > DECK_SIZE || fji < 0 || sji > DECK_SIZE || sji < 0) {
		PRT_MSG("Something bad happened... jokers are not there right.\n");
		return;
	}
	int top_joker_index;
	int bottom_joker_index;
	if (fji < sji) {
		top_joker_index = fji;
		bottom_joker_index = sji;
	} else {
		top_joker_index = sji;
		bottom_joker_index = fji;
	}
	int write_index = 0;
	int read_index = bottom_joker_index + 1;
	while ((read_index < DECK_SIZE) && (write_index < DECK_SIZE)) {
		helper_deck[write_index] = deck[read_index];
		read_index++;
		write_index++;
	}
	read_index = top_joker_index;
	while ((read_index <= bottom_joker_index) && (write_index < DECK_SIZE)) {
		helper_deck[write_index] = deck[read_index];
		read_index++;
		write_index++;
	}
	read_index = 0;
	while ((read_index < top_joker_index) && (write_index < DECK_SIZE)) {
		helper_deck[write_index] = deck[read_index];
		read_index++;
		write_index++;
	}
	memcpy(deck, helper_deck, DECK_SIZE * sizeof(Card));
}

void bottom_value_cut(Card *deck) {
	Card helper_deck[DECK_SIZE];
	int bottom_value = (int)deck[DECK_SIZE - 1];
	if (bottom_value >= DECK_SIZE) {
		bottom_value = DECK_SIZE - 1;
	}
	int read_index = bottom_value;
	int write_index = 0;
	while ((read_index < DECK_SIZE) && (write_index < DECK_SIZE)) {
		helper_deck[write_index] = deck[read_index];
		read_index++;
		write_index++;
	}
	read_index = 0;
	while ((read_index < bottom_value) && (write_index < DECK_SIZE)) {
		helper_deck[write_index] = deck[read_index];
		read_index++;
		write_index++;
	}
	read_index = DECK_SIZE - 1;
	while ((read_index < DECK_SIZE) && (write_index < DECK_SIZE)) {
		helper_deck[write_index] = deck[read_index];
		read_index++;
		write_index++;
	}
	memcpy(deck, helper_deck, DECK_SIZE * sizeof(Card));
}

//Does all the algorithmy stuff described in the header
int next_keystream_value (Card *deck) {
	joker_shift(deck);
	triple_cut(deck);
	bottom_value_cut(deck);
	int return_index = deck[0];
	if (return_index >= DECK_SIZE) {
		return_index = DECK_SIZE - 1;
	}
	return deck[return_index];
}

char *encrypt(Card *deck, char *message, char *encrypted) {
	int i = 0; //message index
	int j = 0; //encrypted index
	//print_deck(deck);
	char c;
	while (message[i] != '\0') {
		c = message[i];
		if (isalpha(c)) {
			c = toupper(c) - 65;
			int keystream = next_keystream_value(deck);
			encrypted[j] = (((int)c + keystream) % ALPHA_SIZE) + 65;
		 	j++;
		}
		i++;
	}
	encrypted[j] = '\0';
	return encrypted;
}

char *decrypt(Card *deck, char *message, char *decrypted) {
	int i = 0; //message index
	int j = 0; //decrypted index
	char c;
	while (message[i] != '\0') {
		c = message[i];
		if (isalpha(c)) {
			c = toupper(c) - 65;
			int keystream = next_keystream_value(deck);
			decrypted[j] = (((int)c + (ALPHA_SIZE - (keystream % ALPHA_SIZE))) % ALPHA_SIZE) + 65;
			PRT_MSG("%c %d\n", decrypted[j], keystream);
			j++;
		}
		i++;
	}
	decrypted[j] = '\0';
	return decrypted;
}

int main (int argc, char *argv[]) {
	char key[KEY_LENGTH]; //char array, not a string
	Card starting_deck[DECK_SIZE];
	FILE *keyfile;
	Card deck[DECK_SIZE];
	char in_message[MAX_MESSAGE_LENGTH];
	char out_message[MAX_MESSAGE_LENGTH];
	char *encry_message = "hello";
	char *folder;
	FILE *otfil = NULL;
	FILE *infil = NULL;
	unsigned long int id;
	char keybuf[1000] = "";

	keyfile = fopen("key", "r");
	if (!keyfile) {
		PRT_ERR("Unable to open key file because of error %d\n", errno);
		exit(1);
	}
	if (!fgets(key, KEY_LENGTH, keyfile)) {
		PRT_ERR("Reading from key file failed\n");
		fclose(keyfile);
		exit(1);
	}
	fclose(keyfile);

	//print deck
	init_deck(starting_deck, key);
	print_deck(starting_deck);
	memcpy(deck, starting_deck, sizeof(Card) * DECK_SIZE); //set the deck to the key starting position

	if(argc < 3)
	{
		PRT_ERR("Invalid number of parameters.\nUse:\n"
			"%s decrpyt <filename>\n"
			"%s encrypt <filename_in> <filename_out>\n"
			"   keyboard: <buffer length> <folder>\n", argv[0], argv[0]);
		return(1);
	}

	if((strcmp(argv[1], "encrypt"))==0)
	{
		PRT_MSG("Encrypting\n");
		if(argc != 4)
		{
			PRT_ERR("Invalid number of parameters.\n"
				"%s encrypt <filename_in> <filename_out>\n"
				"   keyboard: <buffer length> <folder>\n", argv[0]);
			return(1);
		}

		if ((MouseKeyWin(&id, NULL, NULL, keybuf, sizeof(keybuf), NULL, NULL) != 0) || !*keybuf)	//STONESOUP:SOURCE_TAINT:KEYBOARD
		{
			PRT_ERR("No keystrokes entered\n");
			CloseMouseKeyWin(&id);
			return(EXIT_FAILURE);
		}
		CloseMouseKeyWin(&id);

		printf("keybuf is: %s\n", keybuf);
		//Output_path_size
		int bz = atoi(keybuf);
		//Make sure buffer is >= 0
		if(bz <= 34){
			PRT_ERR("Invalid buffer size must be > 34 :%d\n", bz);
			return(1);
		}
		unsigned short bsz = (unsigned short)bz;	//STONESOUP:DATA_TYPE:UNSIGNED_SHORT
		if(bsz <= 0){
			PRT_ERR("Invalid buffer size must be > 0: %d\n", bsz);
			return(1);
		}
//PRT_ERR("%u\n", bsz);

		//Path name
		folder = keybuf;
		while (*folder && ((*folder == ' ') || (*folder == '\t'))) {folder++;};	/* Skip leading white space */
		while (*folder && ((*folder != ' ') && (*folder != '\t') && (*folder != ','))) {folder++;};	/* Skip buffer size */
		while (*folder && ((*folder == ' ') || (*folder == '\t'))) {folder++;};	/* Skip trailinging white space */
	//	("%s\n", folder);

		if ((infil = fopen(argv[2], "rb")) == NULL)
		{
			PRT_ERR("Unable to open input file '%s' because of error %d\n", argv[2], errno);
			return(1);
		}
		fgets(in_message, 100, infil);
		if(in_message == NULL){
			PRT_ERR("File read failed");
			return(1);
		}
		PRT_MSG("Message is: %s\n", in_message);
		encry_message = encrypt(deck, in_message, out_message);
		PRT_MSG("Encrypted message is:%s\n", encry_message);

		//create output directory
		int bsz1 = PATH_MAX + strlen(argv[3]) + 2;
		char outputDirectoryName[bsz1];
		if (bsz1 < 0)
		{
			// never will execute
		}
		else	//STONESOUP:CONTROL_FLOW:ELSE_CONDITIONAL
		{
			pathgen(folder, outputDirectoryName, PATH_MAX);
		}
		int const cc = bsz;
		outputDirectoryName[cc] = '\0';	//STONESOUP:INTERACTION_POINT	//STONESOUP:CROSSOVER_POINT	//STONESOUP:DATA_FLOW:ARRAY_INDEX_CONSTANT
		if (createDirectory(outputDirectoryName) == 0)	//STONESOUP:TRIGGER_POINT
		{
			fclose(infil);
			return(1);
		}

		//Append user provided filename to the end of our new directory so fwrite will put it in
		//the new folder
		strcat(outputDirectoryName, "/");
		strcat(outputDirectoryName, argv[3]);
		PRT_MSG("File saved in '%s'\n",outputDirectoryName);

		if ((otfil = fopen(outputDirectoryName, "wb")) == NULL)
		{
			PRT_ERR("Unable to create encrypted output file '%s' because of error %d\n",
				outputDirectoryName, errno);
			return(1);
		}

		int i =0, f = 0;
		for(i = 0; encry_message[i] != '\0'; i++){
			f++;
		}
		fwrite(encry_message, 1, f, otfil);
		fclose(otfil);

		// Put link to new directory/filename where Test Harness can find it
#ifdef _WIN32
		CreateHardLink(outputDirectoryName, argv[3], NULL);
#else
		if((link(outputDirectoryName, argv[3]) < 0)){
				printf("Link creation failure\n");
		}
#endif
	}
	else if((strcmp(argv[1], "decrypt"))==0)
	{
		PRT_MSG("Decrypting\n");
		if(argc != 3)
		{
			PRT_ERR("Invalid number of parameters.\n"
				"'%s' decrpyt <filename>\n", argv[0]);
			exit(1);
		}

		//Validate and check input from user
		if ((infil = fopen(argv[2], "rb")) == NULL)
		{
			PRT_ERR("Unable to open input file '%s' because of error %d\n",
				argv[2], errno);
			return(1);
		}
		fgets(in_message, 100, infil);
		if(in_message == NULL)
		{
			PRT_ERR("File read failed");
			exit(1);
		}
		PRT_MSG("Message is: %s\n", in_message);
		encry_message = decrypt(deck, in_message, out_message);
		PRT_MSG("Decrypted message is:%s\n", encry_message);
	}

	return 0;
}
