
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
 * clipboard.c
 *
 *  Created on: Jun 9, 2011
 *      
 *
 *      MUST COMPILE WITH LINKER FLAGS -L/usr/X11R6/lib -lX11
 */
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include "clipboard.h"

#ifdef _WIN32
#include <Windows.h>
#include <Winuser.h>
#else
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/Xresource.h>
#include <X11/Xatom.h>

Display* dpy = NULL;
Window win = 0;
#endif

/*
 * A single argument is assumed to be simply text - a CF_TEXT clipboard type
 * If a different type is desired, the typename appears first as a string, then
 * the string representation of the type
 */
int setClipboard(char* txt, ...)
{
#ifdef _WIN32
	char *mode_nm = NULL, *text = txt;
	int mode = CF_TEXT;
	va_list varargs;
	va_start(varargs, txt);

	if(txt==NULL)
	{
		printf("Error: Cannot write NULL to clipboard\n");
		va_end(varargs);
		return 0;
	}
	if ((txt[0] == 'C') && (txt[1] == 'F') && (txt[2] == '_'))
	{
		mode_nm = txt;
		text = va_arg(varargs, char *);
	}
	int size = strlen(text);		//size for array indexing

	if(!OpenClipboard(NULL))		//open clipboard
	{
		printf("ERROR: Could not open clipboard\n");
		va_end(varargs);
		return 0;
	}
	EmptyClipboard();				//clear clipboard data
	HANDLE globalMem = GlobalAlloc(GMEM_MOVEABLE, (size+1) * sizeof(char));
	if (globalMem == NULL)			//create a global memory allocation
	{
	    CloseClipboard();
	    printf("ERROR: Could not allocate clipboard memory\n");
	  	va_end(varargs);
	    return 0;
	}
	char* copyString = (char*)GlobalLock(globalMem);//lock the memory in order to edit it
	if(copyString==NULL)
	{
		GlobalFree(globalMem);
		CloseClipboard();
		va_end(varargs);
		return 0;
	}
	memcpy(copyString,text,size*sizeof(char));	//write your string to the memory

	copyString[size] = '\0';					//manual null-terminator
	GlobalUnlock(globalMem);					//unlock the memory
	mode = mode_nm && strcmp("CF_HDROP", mode_nm) ? CF_HDROP : CF_TEXT;
	SetClipboardData(mode, globalMem);		//place the memory on the clipboard
	CloseClipboard();							//close the clipboard
	va_end(varargs);

	return 1;
#else
	if(dpy==NULL)	//dpy needs to be initialized
	{
		dpy = XOpenDisplay(NULL);//open the main display
		if(dpy==NULL)
		{
			printf("ERROR: Unable to open display\n");
			return 0;
		}
	}
	if(win==0) // win needs to be initialized
	{
		win = XCreateSimpleWindow(dpy, DefaultRootWindow(dpy), 0, 0, 1, 1, 0, 0, 0);//make a window
		if(!win||win==BadAlloc)
		{
			printf("Error: Unable to create window");
			XCloseDisplay(dpy);
			return 0;
		}
	}
	Atom XA_CLIPBOARD = XInternAtom(dpy, "CLIPBOARD", 0);
	if(XChangeProperty(dpy,win,XA_PRIMARY,XA_STRING, 8, PropModeReplace, (unsigned char*)txt,strlen(txt)) != 1)//set the clipboard text
	{
		printf("Error changing window property.  See stderr for more.\n");
		XDestroyWindow(dpy,win);
		XCloseDisplay(dpy);
		return 0;
	}
	if(XSetSelectionOwner(dpy,XA_CLIPBOARD,win,CurrentTime)!= 1)//grab clipboard ownership
	{
		printf("Error setting ownership.  See stderr for more.\n");
		XDestroyWindow(dpy,win);
		XCloseDisplay(dpy);
		return 0;
	}
	if (win!=XGetSelectionOwner(dpy,XA_CLIPBOARD))
	{
		printf("Error: selection was not properly set\n");
		XDestroyWindow(dpy,win);
		XCloseDisplay(dpy);
		return 0;
	}
	XFlush(dpy);
	return 1;
#endif
}

char* getClipboard()//returns a malloc'd string.  REMEMBER TO FREE
{
	fprintf(stderr, "Waiting for 'clipboard.lock'\n");
	/* Wait for the clipboard to start */
	while(access("clipboard.lock", F_OK) != 0)
	{
#ifdef _WIN32
		Sleep(1);
#else
		sleep(1);
#endif
	}
	fprintf(stderr, "Found 'clipboard.lock'\n");

#ifdef _WIN32
	if(!OpenClipboard(NULL))					//open clipboard
		{
			printf("ERROR: Could not open clipboard\n");
			return NULL;
		}

		HANDLE clip = NULL;
		if(IsClipboardFormatAvailable(CF_TEXT))//check that clipboard has text
		{
			clip = GetClipboardData(CF_TEXT);//get a handle to the text
			if(clip == NULL)//with the above check this should never happen
			{
				printf("ERROR: Could not copy text from clipboard\n");
				CloseClipboard();					//make sure to release
				return NULL;
			}
			char* clipText = (char*)GlobalLock(clip);//lock it for editing as a string
			if(clipText == NULL)
			{
				printf("ERROR: Could not lock global memory\n");
				CloseClipboard();					//make sure to release
				return NULL;
			}

			int len = strlen(clipText);	//Windows always null-terminates clipboard text
			char* string = (char*)malloc((len+1)*sizeof(char));//malloc a new copy
			if(string==NULL)
			{
				printf("ERROR: Could not allocate string memory\n");
				GlobalUnlock(clip);
				CloseClipboard();
				return NULL;
			}
			strncpy(string, clipText,len); 			//copy it in
			string[len] = '\0';

			GlobalUnlock(clip);						//release the clipboard data
			CloseClipboard();						//close the clipboard

			return string;
		}
		else if(IsClipboardFormatAvailable(CF_HDROP))//check that clipboard has a filename
		{
			clip = GetClipboardData(CF_HDROP);//get a handle to the filename
			if(clip == NULL)//with the above check this should never happen
			{
				printf("ERROR: Could not copy data from clipboard\n");
				CloseClipboard();					//make sure to release
				return NULL;
			}

			int numFiles = DragQueryFile(clip, -1, NULL, 0);
			if (numFiles < 1)
			{
				printf("ERROR: No filenames found in clipboard\n");
				CloseClipboard();					//make sure to release
				return NULL;
			}

			int i4 = DragQueryFile(clip, 0, NULL, 0) + 2;
			char s[i4 + 1];
			DragQueryFile(clip, 0, s, i4);
			char *s1 = malloc(strlen(s) + 1);
			if (s1 == NULL)
			{
				printf("ERROR: can't malloc string memory\n");
				CloseClipboard();					//make sure to release
				return NULL;
			}
			strcpy(s1, s);

			CloseClipboard();						//close the clipboard

			return s1;
		}
		else
		{
			printf("ERROR: No data found in the clipboard\n");
			CloseClipboard();					//make sure to release
			return NULL;
		}
		return NULL;
#else
		if(dpy==NULL)	//dpy needs to be initialized
		{
			dpy = XOpenDisplay(NULL);//open the main display
			if(dpy==NULL)
			{
				printf("ERROR: Unable to open display\n");
				return NULL;
			}
		}
		Atom XA_CLIPBOARD = XInternAtom(dpy, "CLIPBOARD", 0);
		Window txwin = XGetSelectionOwner(dpy,XA_CLIPBOARD);//current clipboard owner
		if (!txwin)//failure code from XGetSelectionOwner
		{
			printf("Error: Could not get clipboard owner\n");
			return NULL;
		}
		if(XConvertSelection(dpy,XA_CLIPBOARD,XA_STRING,XA_PRIMARY,txwin,CurrentTime)!= 1)//converts own data or sends selection request to other owner
		{
			printf("Error: Conversion request failed\n");
			return NULL;
		}

		XFlush(dpy);
		sleep(1);	//give request (sent on flush) time to process

		Atom type;					//misc data will populate with the next call
		int format;
		unsigned long len;
		unsigned long bytes_left;
		unsigned char* data;		//will populate with malloc'd string = the clipboard data.  Must be freed.

		if(Success==XGetWindowProperty(dpy,txwin,XA_PRIMARY,0,10000000L,0,XA_STRING,&type,&format,&len,&bytes_left,&data))
			return (char*)data;
		else
		{
			printf("Error getting window properties\n");
			return NULL;
		}
#endif
}

void clipboard_cleanup(char *buf)
{
#ifdef __linux
	if(win!=0&&dpy!=NULL)
	{
		if(XDestroyWindow(dpy,win) != 1) printf("Error destroying window\n");
		win = 0;
	}
	if(dpy!=NULL)
	{
		if(XCloseDisplay(dpy) != 0) printf("Error closing display\n");
		dpy = NULL;
	}
#endif
	if (buf)
	{
		free(buf);
	}
	remove("clipboard.lock");
}

/* End of file */
