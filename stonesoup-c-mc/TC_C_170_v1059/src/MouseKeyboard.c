
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

/*******************************************************************************
**
**
** 
** Date: 7/26/11
**
** This program get mouse, keyboard, and/or messaging loop messages from the GUI
**
*******************************************************************************/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <stdarg.h>

#ifdef _WIN32
#include <windef.h>
#include <winbase.h>
#include <windows.h>
#include <winuser.h>

LRESULT CALLBACK WndProc (HWND, UINT, WPARAM, LPARAM);
LRESULT CALLBACK subclassProc(HWND, UINT, WPARAM, LPARAM); //used for new edit window proc
#else
#include <unistd.h>
#include <stdbool.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/Xresource.h>
#include <X11/Xproto.h>
#include <X11/Xatom.h>
#include <X11/extensions/record.h>
#include <X11/XKBlib.h>
#include <X11/extensions/Xrandr.h>
#include <X11/keysym.h>

extern int XDisplayWidth(Display *, int);
extern int XDisplayHeight(Display *, int);
#endif

#include "MouseKeyboard.h"

#define ID_EDIT 1

#define	DOWN 0
#define	UP 1
#define	NO 0
#define	YES 1

int lstatus, rstatus;
int lhasClicked, rhasClicked;
int xPosLoc, yPosLoc;
int *xPos, *yPos;

char szAppName[100];
char g_szClassName[100];
char *keystr;
int keystrsz;

#ifdef _WIN32		// Windoze handling

WNDPROC oldEditProc; //used to store old edit procedure address

#define VK_OEM_PLUS	0xBB
#define VK_OEM_COMMA	0xBC
#define VK_OEM_MINUS	0xBD
#define VK_OEM_PERIOD	0xBE

/**
 * Message handler callback routine for edit dialog (extends the native one) will exit application when an '\n' is recieved as input
 */
LRESULT CALLBACK subclassProc(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam)
{
	switch(message)
	{
		case WM_MOUSEMOVE:
		case WM_LBUTTONDOWN:
			lstatus = DOWN;
			*xPos =(short)LOWORD(lParam);
			*yPos =(short)HIWORD(lParam);
			fprintf(stderr, "dn1 x = %d, y = %d\n", *xPos, *yPos);
			break;

		case WM_KEYDOWN:
			if(wParam == VK_RETURN)
			{
				PostQuitMessage (0);
				return CallWindowProc(oldEditProc, hwnd, message, wParam, lParam);
			}
			break;

		case WM_KEYUP:
			{
				int len = GetWindowTextLength(hwnd);
				if ((len > 0) && (len < 5000) && (keystr != NULL) && (keystrsz > 1))
				{
					char *buf;
					buf = (char*) GlobalAlloc(GPTR, len + 1);
					GetWindowText (hwnd, buf, len + 1);
					strncpy(keystr, buf, keystrsz - 1);
				}
			}
			break;
	}

	return CallWindowProc(oldEditProc, hwnd, message, wParam, lParam);
}

/**
 * Callback to handle window messages Exits when the left mouse button is released
 */
LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{

	static HWND hwndEdit;

	switch (msg)
	{
		case WM_CREATE :
			//create the edit control
			hwndEdit = CreateWindow (TEXT ("edit"), NULL,
				WS_CHILD | WS_VISIBLE | WS_HSCROLL | WS_VSCROLL |
				WS_BORDER | ES_LEFT | ES_MULTILINE |
				ES_AUTOHSCROLL | ES_AUTOVSCROLL,
				0, 0, 0, 0, hwnd, (HMENU) ID_EDIT,
				((LPCREATESTRUCT) lParam) -> hInstance, NULL);

			//Creates callback for edit field
			oldEditProc = (WNDPROC)SetWindowLongPtr(hwndEdit, GWLP_WNDPROC, (LONG_PTR)subclassProc);
			SetWindowText (hwndEdit, keystr);

			return 0;

		case WM_SETFOCUS :
			SetFocus (hwndEdit);
			return 0;

		case WM_SIZE :
			MoveWindow (hwndEdit, 0, 0, LOWORD (lParam), HIWORD (lParam), TRUE);
			return 0;

		case WM_COMMAND :
			if (LOWORD (wParam) == ID_EDIT)
			{
				if ((HIWORD (wParam) == EN_ERRSPACE) ||	(HIWORD (wParam) == EN_MAXTEXT))
				{
					MessageBox (hwnd, TEXT ("Edit control out of space."), szAppName, MB_OK | MB_ICONSTOP);
				}
			}
			return 0;

		case WM_LBUTTONDOWN:
			lstatus = DOWN;
			*xPos =(short)LOWORD(lParam);
			*yPos =(short)HIWORD(lParam);
			fprintf(stderr, "ldn x = %d, y = %d\n", *xPos, *yPos);
			break;

		case WM_LBUTTONUP:
			lstatus = UP;
			lhasClicked = YES;
			msg = WM_MOUSEMOVE;
			PostQuitMessage(0);
			break;

		case WM_RBUTTONDOWN:
			rstatus = DOWN;
			*xPos =(short)LOWORD(lParam);
			*yPos =(short)HIWORD(lParam);
			fprintf(stderr, "rdn x = %d, y = %d\n", *xPos, *yPos);
			break;

		case WM_RBUTTONUP:
			rstatus = UP;
			rhasClicked = YES;
			break;

		case WM_MOUSEMOVE:
			*xPos =(short)LOWORD(lParam);
			*yPos =(short)HIWORD(lParam);
			fprintf(stderr, "mv x = %d, y = %d\n", *xPos, *yPos);
			break;

		case WM_CLOSE:
			DestroyWindow(hwnd);			break;

		case WM_DESTROY:
			PostQuitMessage(0);
			break;

		default:
			return DefWindowProc(hwnd, msg, wParam, lParam);
	}

	return 0;
}

/**
 * Function to be called by the user of this input source, this will create a
 window to receive keypresses, mouse clicks, and/or event messages and return
 them to the calling program.
 */
int MouseKeyWin(void *id, int *mousex, int *mousey, char *keybuf, int keybufsz, int *msg, void **msgdata)
{
	WNDCLASSEX wc;
	HWND hwnd;
	MSG Msg;

	lhasClicked = NO;
	lstatus = UP;
	rhasClicked = NO;
	rstatus = UP;

	strcpy(szAppName, "STONESOUP Keyboard Mouse");
	strcpy(g_szClassName, "StonesoupKmClass");
	keystr = keybuf;
	keystrsz = keybufsz;
	xPos = mousex;
	yPos = mousey;
	/* Make sure the mouse coordinates are defined */
	if (!xPos)
	{
		xPos = &xPosLoc;
	}
	if (!yPos)
	{
		yPos = &yPosLoc;
	}
	*xPos = 0;
	*yPos = 0;

	HINSTANCE hInstance = (HINSTANCE)GetModuleHandle(NULL);

	//Step 1: Registering the Window Class
	wc.cbSize        = sizeof(WNDCLASSEX);
	wc.style         = 0;
	wc.lpfnWndProc   = WndProc;
	wc.cbClsExtra    = 0;
	wc.cbWndExtra    = 0;
	wc.hInstance     = hInstance;
	wc.hIcon         = LoadIcon(NULL, IDI_APPLICATION);
	wc.hCursor       = LoadCursor(NULL, IDC_ARROW);
	wc.hbrBackground = (HBRUSH)(COLOR_WINDOW+1);
	wc.lpszMenuName  = NULL;
	wc.lpszClassName = g_szClassName;
	wc.hIconSm       = LoadIcon(NULL, IDI_APPLICATION);

	if(!RegisterClassEx(&wc))
	{
		MessageBox(NULL, "Window Registration Failed!", "Error!", MB_ICONEXCLAMATION | MB_OK);
		return 0;
	}

	// Step 2: Creating the Window
	hwnd = CreateWindowEx(WS_EX_TOPMOST, g_szClassName, szAppName, WS_POPUP,
		CW_USEDEFAULT, CW_USEDEFAULT, 400, 400, NULL, NULL, hInstance, NULL);

	*((HWND *)id) = hwnd;

	if(hwnd == NULL)
	{
		MessageBox(NULL, "Window Creation Failed!", "Error!", MB_ICONEXCLAMATION | MB_OK);
		return 0;
	}

	ShowWindow (hwnd, SW_MAXIMIZE);
	UpdateWindow (hwnd);

	while(GetMessage(&Msg, NULL, 0, 0) > 0)
	{
		TranslateMessage(&Msg);
		DispatchMessage(&Msg);
		if (Msg.message == WM_LBUTTONUP)
//				&& (*xPos >= -500) && (*xPos <= 500) && (*yPos >= -500) && (*yPos <= 500))
		{
			fprintf(stderr, "x = %d, y = %d\n", *xPos, *yPos);
			PostQuitMessage(1);
			return(0);
		}
		if ((msg != NULL) && (Msg.message == *msg))
		{
			if (msgdata)
			{
				MSG *m = malloc(sizeof(MSG));
				if (m)
				{
					memcpy(m, &Msg, sizeof(MSG));
					*msgdata = m;
				}
			}
			return (Msg.message);
		}
	}

	*((HWND *)id) = NULL;

	return 0;
}

/**
 * Function which continues to read the windows messaging loop.
 */
int ContMouseKeyWin(void *id, int *msg, void **msgdata)
{
	MSG Msg;
	HWND hwnd = *((HWND *)id);

	while(GetMessage(&Msg, hwnd, 0, 0) > 0)
	{
		TranslateMessage(&Msg);
		DispatchMessage(&Msg);
		if (Msg.message == WM_LBUTTONUP)
//				&& (*xPos >= -500) && (*xPos <= 500) && (*yPos >= -500) && (*yPos <= 500))
		{
			fprintf(stderr, "x = %d, y = %d\n", *xPos, *yPos);
			PostQuitMessage(1);
			return(0);
		}
		if ((msg != NULL) && (Msg.message == *msg))
		{
			if (msgdata)
			{
				MSG *m = malloc(sizeof(MSG));
				if (m)
				{
					memcpy(m, &Msg, sizeof(MSG));
					*msgdata = m;
				}
			}
			return (Msg.message);
		}
	}

	*((HWND *)id) = NULL;

	return 0;
}

/**
 * Close the windows.
 */
void CloseMouseKeyWin(void *id)
{
	*((HWND *)id) = NULL;
}

#else // Linux handling
int duringclick;
int titlethickness;
int borderthickness;
int return_press;
int kill_window;

int ProcEvt(Display *dpy, Window w, GC gc, XEvent *evt,
	int *mousex, int *mousey, char *buf, int blen, int *bp)
{
	switch (evt->type)
	{
		case KeyPress:
		{
			char bb[2];
			if (duringclick != 0)	/* Get rid of extraneous <CR>s during xdotool mouse release events */
			{
			  duringclick = 0;
			  break;
			}
			XLookupString((XKeyEvent *)evt, bb, 1, NULL, NULL);
			bb[1] = '\0';
			char c = bb[0];

			if ((c == 0xA) || (c == 0xD) || (c == '?') || (c == '='))	/* Done if EOB character */
			{
//				fprintf(stderr, "'%s' 0x%02X\n", buf, c);
				return_press = 1;	// Catch inadvertent releases
			}
			else if (kill_window == 0)
			{
				if (c == 0x8)	/* Backspace and remove a character */
				{
					if (*bp > 0)
					{
						(*bp)--;
						buf[*bp] = '\0';
						XDrawString(dpy, w, gc, 20, 20, buf, *bp);
						XFlush(dpy);
					}
				}
				else if ((c >= 0x20) && (c < 0x7F))	/* Only do printables */
				{
					if (*bp < (blen - 1))
					{
						buf[(*bp)++] = c;
						buf[*bp] = '\0';
						XDrawString(dpy, w, gc, 20, 20, buf, *bp);
						XFlush(dpy);
					}
				}
			}
			break;
		}

		case KeyRelease:
			if (kill_window != 0)
			{
				return 1;
			}
			else
			{
				char bb[2];
				if (duringclick != 0)	/* Get rid of extraneous <CR>s during xdotool mouse release events */
				{
				  duringclick = 0;
				  break;
				}
				XLookupString((XKeyEvent *)evt, bb, 1, NULL, NULL);
				bb[1] = '\0';
				char c = bb[0];

				if ((c == 0xA) || (c == 0xD) || (c == '?') || (c == '='))	/* Done if EOB character */
				{
					if (return_press != 0)
					{
						fprintf(stderr, "'%s' 0x%02X\n", buf, c);
						return_press = 0;
						kill_window = 1;
//						fprintf(stderr, "Exiting by keyboard %d\n", (int)c);
						return 1;
					}
				}
			}
			break;

		case ButtonPress:
      duringclick = 1;
			*mousex = ((XButtonEvent *)evt)->x + borderthickness;
			*mousey = ((XButtonEvent *)evt)->y + titlethickness;
			fprintf(stderr, "dn x = %d, y = %d\n", *mousex, *mousey);
//			fprintf(stderr, "ButtonPress %d %d\n", *mousex, *mousey);
			break;

		case ButtonRelease:
		  duringclick = 1;
//		  *mousex = ((XButtonEvent *)evt)->x + borderthickness;
//			*mousey = ((XButtonEvent *)evt)->y + titlethickness;
//			*mousey = ((XButtonEvent *)evt)->y;
//			fprintf(stderr, "up x = %d, y = %d\n", *mousex, *mousey);
//			fprintf(stderr, "ButtonRelease %d %d\n", *mousex, *mousey);
			/* Use button press coordinates as release tend to be bogus */
//		  if ((*mousex >= -500) && (*mousex <= 500) && (*mousey >= -500) && (*mousey <= 500))
//			{
//				fprintf(stderr, "Exiting by mouse %d %d\n", *mousex, *mousey);
				return (1);	/* Exit window if upper left mouse click (500x500) */
//			}
			break;

		case MotionNotify:
			*mousex = ((XButtonEvent *)evt)->x + borderthickness;
			*mousey = ((XButtonEvent *)evt)->y + titlethickness;
			fprintf(stderr, "mv x = %d, y = %d\n", *mousex, *mousey);
//			fprintf(stderr, "Motion %d %d\n", *mousex, *mousey);
			break;

		case ConfigureNotify:
//			fprintf(stderr, "Configure\n");
			break;

		case LeaveNotify:
//			fprintf(stderr, "Leave\n");
			break;

		case ReparentNotify:
//			fprintf(stderr, "Reparent\n");
			break;

		case MapNotify:
//			fprintf(stderr, "Map\n");
			break;

		case EnterNotify:	/* Destroy code? */
			break;

		default:
			fprintf(stderr, "Event %d unhandled\n", evt->type);
			break;
	}

	return (0);
}

Window wn;
GC gc;
int bp;

/**
 * Function to be called by the user of this input source, this will create a
 window to receive keypresses, mouse clicks, and/or event messages and return
 them to the calling program.
 */
int MouseKeyWin(void *id, int *mousex, int *mousey, char *keybuf, int keybufsz, int *msg, void **msgdata)
{
	XEvent e;

	kill_window = 0;
	duringclick = 0;
	return_press = 0;
	lhasClicked = NO;
	lstatus = UP;
	rhasClicked = NO;
	rstatus = UP;
	bp = 0;	/* Inital buffer pointer is at beginning of buffer */;

	strcpy(szAppName, "STONESOUP Keyboard Mouse");
	strcpy(g_szClassName, "StonesoupKmClass");
	keystr = keybuf;
	keystrsz = keybufsz;
	xPos = mousex;
	yPos = mousey;
	/* Make sure the mouse coordinates are defined */
	if (!xPos)
	{
		xPos = &xPosLoc;
	}
	if (!yPos)
	{
		yPos = &yPosLoc;
	}
	*xPos = 0;
	*yPos = 0;
	wn = 0;
	gc = NULL;

	// Open the display
	Display *dpy = XOpenDisplay(NULL);
	if (dpy == NULL)
	{
		sleep(1);
		dpy = XOpenDisplay(NULL);
		if (dpy == NULL)
		{
			sleep(1);
			dpy = XOpenDisplay(NULL);
			if (dpy == NULL)
			{
				fprintf(stderr, "Display attachment Failed!\n");
				return 0;
			}
		}
	}
	*((Display **)id) = dpy;

	int wd = XDisplayWidth(dpy, 0);
	int ht = XDisplayHeight(dpy, 0);
//	fprintf(stderr, "%d %d\n", wd, ht);

	// Get some colors
	int blackColor = BlackPixel(dpy, DefaultScreen(dpy));
	int whiteColor = WhitePixel(dpy, DefaultScreen(dpy));

	// Create the window
	wn = XCreateSimpleWindow(dpy, DefaultRootWindow(dpy), 0, 0,wd, ht, 0,
		whiteColor, whiteColor);

	XStoreName(dpy, wn, "STONESOUP Keyboard Mouse");

	// We want to get MapNotify events
	XSelectInput(dpy, wn, StructureNotifyMask | ButtonMotionMask | ButtonPressMask | ButtonReleaseMask | KeyPressMask |
		KeyReleaseMask | PointerMotionMask | LeaveWindowMask | DestroyNotify);

//	XSetWindowBorderWidth(dpy, wn, borderthickness);

	// "Map" the window (that is, make it appear on the screen)
	XMapWindow(dpy, wn);

	// Create a "Graphics Context"
	gc = XCreateGC(dpy, wn, 0, NULL);

	// Tell the GC we draw using the white color
	XSetForeground(dpy, gc, blackColor);

	XWindowAttributes wa;
	int rx, ry;
	Window jw;
	// xwininfo is good reference for howto
	XGetWindowAttributes(dpy, wn, &wa);
	XTranslateCoordinates (dpy, wn, wa.root, -wa.border_width, -wa.border_width, &rx, &ry, &jw);
//	fprintf(stderr, "%d %d %d %d %d\n", rx, ry, wa.width, wa.height, wa.border_width);
	borderthickness = rx;
	titlethickness = ry;

	// Wait for window display
	while (1 == 1)
	{
		XNextEvent(dpy, &e);
		if ((e.type == MapNotify) || (kill_window != 0))
		{
//			fprintf(stderr, "Found notify\n");
			break;
		}
	}

	while (XEventsQueued(dpy, QueuedAlready) != 0){ XNextEvent(dpy, &e); }	/* Clear event queue */
	XDrawString(dpy, wn, gc, 20, 20, keystr, strlen(keystr));
	XFlush(dpy);

	// Wait for the MapNotify event
	if (kill_window == 0)
	{
		do
		{
			XNextEvent(dpy, &e);
			if ((kill_window == 0) && (ProcEvt(dpy, wn, gc, &e, xPos, yPos, keystr, keystrsz, &bp) != 0))
			{
//			printf("Exiting X11 Window\n");
				return(0);
			}
			if ((msg != NULL) && (e.type == *msg))
			{
				if (msgdata)
				{
					XEvent *m = malloc(sizeof(XEvent));
					if (m)
					{
						memcpy(m, &e, sizeof(XEvent));
						*msgdata = m;
					}
				}
	//      printf("Exiting X11 Window msg\n");
				return (e.type);
			}
		}
		while ((e.type != DestroyNotify) && (kill_window == 0));
	}
//  printf("Exiting X11 Window des\n");

	XDestroyWindow(dpy, wn);
	XCloseDisplay(dpy);
	*((Display **)id) = NULL;

	return 0;
}

/**
 * Function which continues to read the windows messaging loop.
 */
int ContMouseKeyWin(void *id, int *msg, void **msgdata)
{
	XEvent e;
	Display *dpy = *((Display **)id);
	if (dpy == NULL)
	{
		return (0);
	}

	do
	{
		XNextEvent(dpy, &e);
		if ((kill_window == 0) && (ProcEvt(dpy, wn, gc, &e, xPos, yPos, keystr, keystrsz, &bp) != 0))
		{
			return(0);
		}
		if ((msg != NULL) && (e.type == *msg))
		{
			if (msgdata)
			{
				XEvent *m = malloc(sizeof(XEvent));
				if (m)
				{
					memcpy(m, &e, sizeof(XEvent));
					*msgdata = m;
				}
			}
			return (e.type);
		}
	}
	while (e.type != DestroyNotify);

	XDestroyWindow(dpy, wn);
	XCloseDisplay(dpy);
	*((Display **)id) = NULL;

  return 0;
}

/**
 * Close the windows.
 */
void CloseMouseKeyWin(void *id)
{
	Display *dpy = *((Display **)id);
	if (dpy == NULL)
	{
		return;
	}

	XDestroyWindow(dpy, wn);
	XCloseDisplay(dpy);
	*((Display **)id) = NULL;
}

#endif

/* End of file */
