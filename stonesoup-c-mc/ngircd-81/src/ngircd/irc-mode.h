/*
 * ngIRCd -- The Next Generation IRC Daemon
 * Copyright (c)2001,2002 by Alexander Barton (alex@barton.de)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * Please read the file COPYING, README and AUTHORS for more information.
 *
 * $Id: irc-mode.h,v 1.5 2002/12/12 12:23:43 alex Exp $
 *
 * IRC commands for mode changes (header)
 */


#ifndef __irc_mode_h__
#define __irc_mode_h__


GLOBAL BOOLEAN IRC_MODE PARAMS((CLIENT *Client, REQUEST *Req ));
GLOBAL BOOLEAN IRC_AWAY PARAMS((CLIENT *Client, REQUEST *Req ));


#endif


/* -eof- */
