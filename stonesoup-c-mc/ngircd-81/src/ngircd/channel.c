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
 * Channel management
 */


#define __channel_c__


#include "portab.h"

static char UNUSED id[] = "$Id: channel.c,v 1.45 2004/03/11 22:16:31 alex Exp $";

#include "imp.h"
#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

#include "conn-func.h"
#include "client.h"

#include "exp.h"
#include "channel.h"

#include "imp.h"
#include "irc-write.h"
#include "resolve.h"
#include "conf.h"
#include "hash.h"
#include "lists.h"
#include "log.h"
#include "messages.h"

#include "exp.h"


#define REMOVE_PART 0
#define REMOVE_QUIT 1
#define REMOVE_KICK 2


LOCAL CHANNEL *My_Channels;
LOCAL CL2CHAN *My_Cl2Chan;


LOCAL CL2CHAN *Get_Cl2Chan PARAMS(( CHANNEL *Chan, CLIENT *Client ));
LOCAL CL2CHAN *Add_Client PARAMS(( CHANNEL *Chan, CLIENT *Client ));
LOCAL BOOLEAN Remove_Client PARAMS(( INT Type, CHANNEL *Chan, CLIENT *Client, CLIENT *Origin, CHAR *Reason, BOOLEAN InformServer ));
LOCAL CL2CHAN *Get_First_Cl2Chan PARAMS(( CLIENT *Client, CHANNEL *Chan ));
LOCAL CL2CHAN *Get_Next_Cl2Chan PARAMS(( CL2CHAN *Start, CLIENT *Client, CHANNEL *Chan ));
LOCAL BOOLEAN Delete_Channel PARAMS(( CHANNEL *Chan ));


GLOBAL VOID
Channel_Init( VOID )
{
	My_Channels = NULL;
	My_Cl2Chan = NULL;
} /* Channel_Init */


GLOBAL VOID
Channel_InitPredefined( VOID )
{
	/* Vordefinierte persistente Channels erzeugen */

	CHANNEL *chan;
	CHAR *c;
	INT i;
	
	for( i = 0; i < Conf_Channel_Count; i++ )
	{
		/* Ist ein Name konfiguriert? */
		if( ! Conf_Channel[i].name[0] ) continue;

		/* Gueltiger Channel-Name? */
		if( ! Channel_IsValidName( Conf_Channel[i].name ))
		{
			Log( LOG_ERR, "Can't create pre-defined channel: invalid name: \"%s\"!", Conf_Channel[i].name );
			continue;
		}

		/* Gibt es den Channel bereits? */
		chan = Channel_Search( Conf_Channel[i].name );
		if( chan )
		{
			Log( LOG_INFO, "Can't create pre-defined channel \"%s\": name already in use.", Conf_Channel[i].name );
			continue;
		}
		
		/* Channel anlegen */
		chan = Channel_Create( Conf_Channel[i].name );
		if( chan )
		{
			Channel_ModeAdd( chan, 'P' );
			Channel_SetTopic( chan, Conf_Channel[i].topic );
			c = Conf_Channel[i].modes;
			while( *c ) Channel_ModeAdd( chan, *c++ );
			Log( LOG_INFO, "Created pre-defined channel \"%s\".", Conf_Channel[i].name );
		}
		else Log( LOG_ERR, "Can't create pre-defined channel \"%s\"!", Conf_Channel[i].name );
	}
} /* Channel_InitPredefined */


GLOBAL VOID
Channel_Exit( VOID )
{
	CHANNEL *c, *c_next;
	CL2CHAN *cl2chan, *cl2chan_next;
	
	/* Channel-Strukturen freigeben */
	c = My_Channels;
	while( c )
	{
		c_next = c->next;
		free( c );
		c = c_next;
	}

	/* Channel-Zuordnungstabelle freigeben */
	cl2chan = My_Cl2Chan;
	while( c )
	{
		cl2chan_next = cl2chan->next;
		free( cl2chan );
		cl2chan = cl2chan_next;
	}
} /* Channel_Exit */


GLOBAL BOOLEAN
Channel_Join( CLIENT *Client, CHAR *Name )
{
	CHANNEL *chan;
	
	assert( Client != NULL );
	assert( Name != NULL );

	/* Valider Channel-Name? */
	if( ! Channel_IsValidName( Name ))
	{
		IRC_WriteStrClient( Client, ERR_NOSUCHCHANNEL_MSG, Client_ID( Client ), Name );
		return FALSE;
	}

	/* Channel suchen */
	chan = Channel_Search( Name );
	if( chan )
	{
		/* Ist der Client bereits Mitglied? */
		if( Get_Cl2Chan( chan, Client )) return FALSE;
	}
	else
	{
		/* Gibt es noch nicht? Dann neu anlegen: */
		chan = Channel_Create( Name );
		if( ! chan ) return FALSE;
	}

	/* User dem Channel hinzufuegen */
	if( ! Add_Client( chan, Client )) return FALSE;
	else return TRUE;
} /* Channel_Join */


GLOBAL BOOLEAN
Channel_Part( CLIENT *Client, CLIENT *Origin, CHAR *Name, CHAR *Reason )
{
	CHANNEL *chan;

	assert( Client != NULL );
	assert( Name != NULL );
	assert( Reason != NULL );

	/* Channel suchen */
	chan = Channel_Search( Name );
	if(( ! chan ) || ( ! Get_Cl2Chan( chan, Client )))
	{
		IRC_WriteStrClient( Client, ERR_NOSUCHCHANNEL_MSG, Client_ID( Client ), Name );
		return FALSE;
	}

	/* User aus Channel entfernen */
	if( ! Remove_Client( REMOVE_PART, chan, Client, Origin, Reason, TRUE )) return FALSE;
	else return TRUE;
} /* Channel_Part */


GLOBAL VOID
Channel_Kick( CLIENT *Client, CLIENT *Origin, CHAR *Name, CHAR *Reason )
{
	CHANNEL *chan;

	assert( Client != NULL );
	assert( Origin != NULL );
	assert( Name != NULL );
	assert( Reason != NULL );

	/* Channel suchen */
	chan = Channel_Search( Name );
	if( ! chan )
	{
		IRC_WriteStrClient( Origin, ERR_NOSUCHCHANNEL_MSG, Client_ID( Origin ), Name );
		return;
	}

	/* Ist der User Mitglied in dem Channel? */
	if( ! Channel_IsMemberOf( chan, Origin ))
	{
		IRC_WriteStrClient( Origin, ERR_NOTONCHANNEL_MSG, Client_ID( Origin ), Name );
		return;
	}

	/* Ist der User Channel-Operator? */
	if( ! strchr( Channel_UserModes( chan, Origin ), 'o' ))
	{
		IRC_WriteStrClient( Origin, ERR_CHANOPRIVSNEEDED_MSG, Client_ID( Origin ), Name);
		return;
	}

	/* Ist der Ziel-User Mitglied im Channel? */
	if( ! Channel_IsMemberOf( chan, Client ))
	{
		IRC_WriteStrClient( Origin, ERR_USERNOTINCHANNEL_MSG, Client_ID( Origin ), Client_ID( Client ), Name );
		return;
	}

	Remove_Client( REMOVE_KICK, chan, Client, Origin, Reason, TRUE );
} /* Channel_Kick */


GLOBAL VOID
Channel_Quit( CLIENT *Client, CHAR *Reason )
{
	CHANNEL *c, *next_c;

	assert( Client != NULL );
	assert( Reason != NULL );

	c = My_Channels;
	while( c )
	{
		next_c = c->next;
		Remove_Client( REMOVE_QUIT, c, Client, Client, Reason, FALSE );
		c = next_c;
	}
} /* Channel_Quit */


GLOBAL LONG
Channel_Count( VOID )
{
	CHANNEL *c;
	LONG count;
	
	count = 0;
	c = My_Channels;
	while( c )
	{
		count++;
		c = c->next;
	}
	return count;
} /* Channel_Count */


GLOBAL LONG
Channel_MemberCount( CHANNEL *Chan )
{
	CL2CHAN *cl2chan;
	LONG count;

	assert( Chan != NULL );

	count = 0;
	cl2chan = My_Cl2Chan;
	while( cl2chan )
	{
		if( cl2chan->channel == Chan ) count++;
		cl2chan = cl2chan->next;
	}
	return count;
} /* Channel_MemberCount */


GLOBAL INT
Channel_CountForUser( CLIENT *Client )
{
	/* Count number of channels a user is member of. */

	CL2CHAN *cl2chan;
	INT count;
	
	assert( Client != NULL );
	
	count = 0;
	cl2chan = My_Cl2Chan;
	while( cl2chan )
	{
		if( cl2chan->client == Client ) count++;
		cl2chan = cl2chan->next;
	}

	return count;
} /* Channel_CountForUser */


GLOBAL INT
Channel_PCount( VOID )
{
	/* Count the number of persistent (mode 'P') channels */

	CHANNEL *chan;
	INT count;

	count = 0;
	chan = My_Channels;
	while( chan )
	{
		if( strchr( chan->modes, 'P' )) count++;
		chan = chan->next;
	}

	return count;
} /* Channel_PCount */


GLOBAL CHAR *
Channel_Name( CHANNEL *Chan )
{
	assert( Chan != NULL );
	return Chan->name;
} /* Channel_Name */


GLOBAL CHAR *
Channel_Modes( CHANNEL *Chan )
{
	assert( Chan != NULL );
	return Chan->modes;
} /* Channel_Modes */


GLOBAL CHAR *
Channel_Key( CHANNEL *Chan )
{
	assert( Chan != NULL );
	return Chan->key;
} /* Channel_Key */


GLOBAL LONG
Channel_MaxUsers( CHANNEL *Chan )
{
	assert( Chan != NULL );
	return Chan->maxusers;
} /* Channel_MaxUsers */


GLOBAL CHANNEL *
Channel_First( VOID )
{
	return My_Channels;
} /* Channel_First */


GLOBAL CHANNEL *
Channel_Next( CHANNEL *Chan )
{
	assert( Chan != NULL );
	return Chan->next;
} /* Channel_Next */


GLOBAL CHANNEL *
Channel_Search( CHAR *Name )
{
	/* Channel-Struktur suchen */
	
	CHANNEL *c;
	UINT32 search_hash;

	assert( Name != NULL );

	search_hash = Hash( Name );
	c = My_Channels;
	while( c )
	{
		if( search_hash == c->hash )
		{
			/* lt. Hash-Wert: Treffer! */
			if( strcasecmp( Name, c->name ) == 0 ) return c;
		}
		c = c->next;
	}
	return NULL;
} /* Channel_Search */


GLOBAL CL2CHAN *
Channel_FirstMember( CHANNEL *Chan )
{
	assert( Chan != NULL );
	return Get_First_Cl2Chan( NULL, Chan );
} /* Channel_FirstMember */


GLOBAL CL2CHAN *
Channel_NextMember( CHANNEL *Chan, CL2CHAN *Cl2Chan )
{
	assert( Chan != NULL );
	assert( Cl2Chan != NULL );
	return Get_Next_Cl2Chan( Cl2Chan->next, NULL, Chan );
} /* Channel_NextMember */


GLOBAL CL2CHAN *
Channel_FirstChannelOf( CLIENT *Client )
{
	assert( Client != NULL );
	return Get_First_Cl2Chan( Client, NULL );
} /* Channel_FirstChannelOf */


GLOBAL CL2CHAN *
Channel_NextChannelOf( CLIENT *Client, CL2CHAN *Cl2Chan )
{
	assert( Client != NULL );
	assert( Cl2Chan != NULL );
	return Get_Next_Cl2Chan( Cl2Chan->next, Client, NULL );
} /* Channel_NextChannelOf */


GLOBAL CLIENT *
Channel_GetClient( CL2CHAN *Cl2Chan )
{
	assert( Cl2Chan != NULL );
	return Cl2Chan->client;
} /* Channel_GetClient */


GLOBAL CHANNEL *
Channel_GetChannel( CL2CHAN *Cl2Chan )
{
	assert( Cl2Chan != NULL );
	return Cl2Chan->channel;
} /* Channel_GetChannel */


GLOBAL BOOLEAN
Channel_IsValidName( CHAR *Name )
{
	/* Pruefen, ob Name als Channelname gueltig */

	CHAR *ptr, badchars[10];
	
	assert( Name != NULL );

	if(( Name[0] != '#' ) || ( strlen( Name ) >= CHANNEL_NAME_LEN )) return FALSE;

	ptr = Name;
	strcpy( badchars, " ,:\007" );
	while( *ptr )
	{
		if( strchr( badchars, *ptr )) return FALSE;
		ptr++;
	}
	
	return TRUE;
} /* Channel_IsValidName */


GLOBAL BOOLEAN
Channel_ModeAdd( CHANNEL *Chan, CHAR Mode )
{
	/* Mode soll gesetzt werden. TRUE wird geliefert, wenn der
	 * Mode neu gesetzt wurde, FALSE, wenn der Channel den Mode
	 * bereits hatte. */

	CHAR x[2];

	assert( Chan != NULL );

	x[0] = Mode; x[1] = '\0';
	if( ! strchr( Chan->modes, x[0] ))
	{
		/* Client hat den Mode noch nicht -> setzen */
		strlcat( Chan->modes, x, sizeof( Chan->modes ));
		return TRUE;
	}
	else return FALSE;
} /* Channel_ModeAdd */


GLOBAL BOOLEAN
Channel_ModeDel( CHANNEL *Chan, CHAR Mode )
{
	/* Mode soll geloescht werden. TRUE wird geliefert, wenn der
	 * Mode entfernt wurde, FALSE, wenn der Channel den Mode
	 * ueberhaupt nicht hatte. */

	CHAR x[2], *p;

	assert( Chan != NULL );

	x[0] = Mode; x[1] = '\0';

	p = strchr( Chan->modes, x[0] );
	if( ! p ) return FALSE;

	/* Client hat den Mode -> loeschen */
	while( *p )
	{
		*p = *(p + 1);
		p++;
	}
	return TRUE;
} /* Channel_ModeDel */


GLOBAL BOOLEAN
Channel_UserModeAdd( CHANNEL *Chan, CLIENT *Client, CHAR Mode )
{
	/* Channel-User-Mode soll gesetzt werden. TRUE wird geliefert,
	 * wenn der Mode neu gesetzt wurde, FALSE, wenn der User den
	 * Channel-Mode bereits hatte. */

	CL2CHAN *cl2chan;
	CHAR x[2];

	assert( Chan != NULL );
	assert( Client != NULL );

	cl2chan = Get_Cl2Chan( Chan, Client );
	assert( cl2chan != NULL );
	
	x[0] = Mode; x[1] = '\0';
	if( ! strchr( cl2chan->modes, x[0] ))
	{
		/* Client hat den Mode noch nicht -> setzen */
		strlcat( cl2chan->modes, x, sizeof( cl2chan->modes ));
		return TRUE;
	}
	else return FALSE;
} /* Channel_UserModeAdd */


GLOBAL BOOLEAN
Channel_UserModeDel( CHANNEL *Chan, CLIENT *Client, CHAR Mode )
{
	/* Channel-User-Mode soll geloescht werden. TRUE wird geliefert,
	 * wenn der Mode entfernt wurde, FALSE, wenn der User den Channel-Mode
	 * ueberhaupt nicht hatte. */

	CL2CHAN *cl2chan;
	CHAR x[2], *p;

	assert( Chan != NULL );
	assert( Client != NULL );

	cl2chan = Get_Cl2Chan( Chan, Client );
	assert( cl2chan != NULL );

	x[0] = Mode; x[1] = '\0';

	p = strchr( cl2chan->modes, x[0] );
	if( ! p ) return FALSE;

	/* Client hat den Mode -> loeschen */
	while( *p )
	{
		*p = *(p + 1);
		p++;
	}
	return TRUE;
} /* Channel_UserModeDel */


GLOBAL CHAR *
Channel_UserModes( CHANNEL *Chan, CLIENT *Client )
{
	/* Channel-Modes eines Users liefern */
	
	CL2CHAN *cl2chan;

	assert( Chan != NULL );
	assert( Client != NULL );

	cl2chan = Get_Cl2Chan( Chan, Client );
	assert( cl2chan != NULL );

	return cl2chan->modes;
} /* Channel_UserModes */


GLOBAL BOOLEAN
Channel_IsMemberOf( CHANNEL *Chan, CLIENT *Client )
{
	/* Pruefen, ob Client Mitglied in Channel ist */

	assert( Chan != NULL );
	assert( Client != NULL );

	if( Get_Cl2Chan( Chan, Client )) return TRUE;
	else return FALSE;
} /* Channel_IsMemberOf */


GLOBAL CHAR *
Channel_Topic( CHANNEL *Chan )
{
	assert( Chan != NULL );
	return Chan->topic;
} /* Channel_Topic */


GLOBAL VOID
Channel_SetTopic( CHANNEL *Chan, CHAR *Topic )
{
	assert( Chan != NULL );
	assert( Topic != NULL );
	
	strlcpy( Chan->topic, Topic, sizeof( Chan->topic ));
} /* Channel_SetTopic */


GLOBAL VOID
Channel_SetModes( CHANNEL *Chan, CHAR *Modes )
{
	assert( Chan != NULL );
	assert( Modes != NULL );

	strlcpy( Chan->modes, Modes, sizeof( Chan->modes ));
} /* Channel_SetModes */


GLOBAL VOID
Channel_SetKey( CHANNEL *Chan, CHAR *Key )
{
	assert( Chan != NULL );
	assert( Key != NULL );

	strlcpy( Chan->key, Key, sizeof( Chan->key ));
	Log( LOG_DEBUG, "Channel %s: Key is now \"%s\".", Chan->name, Chan->key );
} /* Channel_SetKey */


GLOBAL VOID
Channel_SetMaxUsers( CHANNEL *Chan, LONG Count )
{
	assert( Chan != NULL );

	Chan->maxusers = Count;
	Log( LOG_DEBUG, "Channel %s: Member limit is now %ld.", Chan->name, Chan->maxusers );
} /* Channel_SetMaxUsers */


GLOBAL BOOLEAN
Channel_Write( CHANNEL *Chan, CLIENT *From, CLIENT *Client, CHAR *Text )
{
	BOOLEAN is_member, has_voice, is_op, ok;

	/* Okay, target is a channel */
	is_member = has_voice = is_op = FALSE;
	if( Channel_IsMemberOf( Chan, From ))
	{
		is_member = TRUE;
		if( strchr( Channel_UserModes( Chan, From ), 'v' )) has_voice = TRUE;
		if( strchr( Channel_UserModes( Chan, From ), 'o' )) is_op = TRUE;
	}

	/* Check weather client is allowed to write to channel */
	ok = TRUE;
	if( strchr( Channel_Modes( Chan ), 'n' ) && ( ! is_member )) ok = FALSE;
	if( strchr( Channel_Modes( Chan ), 'm' ) && ( ! is_op ) && ( ! has_voice )) ok = FALSE;
	
	/* Is the client banned? */
	if( Lists_CheckBanned( From, Chan ))
	{
		/* Client is banned, bus is he channel operator or has voice? */
		if(( ! has_voice ) && ( ! is_op )) ok = FALSE;
	}

	if( ! ok ) return IRC_WriteStrClient( From, ERR_CANNOTSENDTOCHAN_MSG, Client_ID( From ), Channel_Name( Chan ));

	/* Send text */
	if( Client_Conn( From ) > NONE ) Conn_UpdateIdle( Client_Conn( From ));
	return IRC_WriteStrChannelPrefix( Client, Chan, From, TRUE, "PRIVMSG %s :%s", Channel_Name( Chan ), Text );
} /* Channel_Write */


GLOBAL CHANNEL *
Channel_Create( CHAR *Name )
{
	/* Neue Channel-Struktur anlegen */

	CHANNEL *c;

	assert( Name != NULL );
	
	c = (CHANNEL *)malloc( sizeof( CHANNEL ));
	if( ! c )
	{
		Log( LOG_EMERG, "Can't allocate memory! [New_Chan]" );
		return NULL;
	}
	c->next = NULL;
	strlcpy( c->name, Name, sizeof( c->name ));
	c->name[CHANNEL_NAME_LEN - 1] = '\0';
	strcpy( c->modes, "" );
	strcpy( c->topic, "" );
	c->hash = Hash( c->name );
	strcpy( c->key, "" );
	c->maxusers = 0;

	/* Verketten */
	c->next = My_Channels;
	My_Channels = c;
	
	Log( LOG_DEBUG, "Created new channel structure for \"%s\".", Name );
	
	return c;
} /* Channel_Create */


LOCAL CL2CHAN *
Get_Cl2Chan( CHANNEL *Chan, CLIENT *Client )
{
	CL2CHAN *cl2chan;

	assert( Chan != NULL );
	assert( Client != NULL );

	cl2chan = My_Cl2Chan;
	while( cl2chan )
	{
		if(( cl2chan->channel == Chan ) && ( cl2chan->client == Client )) return cl2chan;
		cl2chan = cl2chan->next;
	}
	return NULL;
} /* Get_Cl2Chan */


LOCAL CL2CHAN *
Add_Client( CHANNEL *Chan, CLIENT *Client )
{
	CL2CHAN *cl2chan;

	assert( Chan != NULL );
	assert( Client != NULL );

	/* neue CL2CHAN-Struktur anlegen */
	cl2chan = (CL2CHAN *)malloc( sizeof( CL2CHAN ));
	if( ! cl2chan )
	{
		Log( LOG_EMERG, "Can't allocate memory! [Add_Client]" );
		return NULL;
	}
	cl2chan->channel = Chan;
	cl2chan->client = Client;
	strcpy( cl2chan->modes, "" );

	/* Verketten */
	cl2chan->next = My_Cl2Chan;
	My_Cl2Chan = cl2chan;

	Log( LOG_DEBUG, "User \"%s\" joined channel \"%s\".", Client_Mask( Client ), Chan->name );

	return cl2chan;
} /* Add_Client */


LOCAL BOOLEAN
Remove_Client( INT Type, CHANNEL *Chan, CLIENT *Client, CLIENT *Origin, CHAR *Reason, BOOLEAN InformServer )
{
	CL2CHAN *cl2chan, *last_cl2chan;
	CHANNEL *c;
	
	assert( Chan != NULL );
	assert( Client != NULL );
	assert( Origin != NULL );
	assert( Reason != NULL );

	last_cl2chan = NULL;
	cl2chan = My_Cl2Chan;
	while( cl2chan )
	{
		if(( cl2chan->channel == Chan ) && ( cl2chan->client == Client )) break;
		last_cl2chan = cl2chan;
		cl2chan = cl2chan->next;
	}
	if( ! cl2chan ) return FALSE;

	c = cl2chan->channel;
	assert( c != NULL );

	/* Aus Verkettung loesen und freigeben */
	if( last_cl2chan ) last_cl2chan->next = cl2chan->next;
	else My_Cl2Chan = cl2chan->next;
	free( cl2chan );

	switch( Type )
	{
		case REMOVE_QUIT:
			/* QUIT: andere Server wurden bereits informiert, vgl. Client_Destroy();
			 * hier also "nur" noch alle User in betroffenen Channeln infomieren */
			assert( InformServer == FALSE );
			IRC_WriteStrChannelPrefix( Origin, c, Origin, FALSE, "QUIT :%s", Reason );
			Log( LOG_DEBUG, "User \"%s\" left channel \"%s\" (%s).", Client_Mask( Client ), c->name, Reason );
			break;
		case REMOVE_KICK:
			/* User wurde geKICKed: ggf. andere Server sowie alle betroffenen User
			 * im entsprechenden Channel informieren */
			if( InformServer ) IRC_WriteStrServersPrefix( Client_NextHop( Origin ), Origin, "KICK %s %s :%s", c->name, Client_ID( Client ), Reason );
			IRC_WriteStrChannelPrefix( Client, c, Origin, FALSE, "KICK %s %s :%s", c->name, Client_ID( Client ), Reason );
			if(( Client_Conn( Client ) > NONE ) && ( Client_Type( Client ) == CLIENT_USER )) IRC_WriteStrClientPrefix( Client, Origin, "KICK %s %s :%s", c->name, Client_ID( Client ), Reason );
			Log( LOG_DEBUG, "User \"%s\" has been kicked of \"%s\" by \"%s\": %s.", Client_Mask( Client ), c->name, Client_ID( Origin ), Reason );
			break;
		default:
			/* PART */
			if( InformServer ) IRC_WriteStrServersPrefix( Origin, Client, "PART %s :%s", c->name, Reason );
			IRC_WriteStrChannelPrefix( Origin, c, Client, FALSE, "PART %s :%s", c->name, Reason );
			if(( Client_Conn( Origin ) > NONE ) && ( Client_Type( Origin ) == CLIENT_USER )) IRC_WriteStrClientPrefix( Origin, Client, "PART %s :%s", c->name, Reason );
			Log( LOG_DEBUG, "User \"%s\" left channel \"%s\" (%s).", Client_Mask( Client ), c->name, Reason );
	}

	/* Wenn Channel nun leer und nicht pre-defined: loeschen */
	if( ! strchr( Channel_Modes( Chan ), 'P' ))
	{
		if( ! Get_First_Cl2Chan( NULL, Chan )) Delete_Channel( Chan );
	}
		
	return TRUE;
} /* Remove_Client */


LOCAL CL2CHAN *
Get_First_Cl2Chan( CLIENT *Client, CHANNEL *Chan )
{
	return Get_Next_Cl2Chan( My_Cl2Chan, Client, Chan );
} /* Get_First_Cl2Chan */


LOCAL CL2CHAN *
Get_Next_Cl2Chan( CL2CHAN *Start, CLIENT *Client, CHANNEL *Channel )
{
	CL2CHAN *cl2chan;

	assert( Client != NULL || Channel != NULL );
	
	cl2chan = Start;
	while( cl2chan )
	{
		if(( Client ) && ( cl2chan->client == Client )) return cl2chan;
		if(( Channel ) && ( cl2chan->channel == Channel )) return cl2chan;
		cl2chan = cl2chan->next;
	}
	return NULL;
} /* Get_Next_Cl2Chan */


LOCAL BOOLEAN
Delete_Channel( CHANNEL *Chan )
{
	/* Channel-Struktur loeschen */
	
	CHANNEL *chan, *last_chan;

	last_chan = NULL;
	chan = My_Channels;
	while( chan )
	{
		if( chan == Chan ) break;
		last_chan = chan;
		chan = chan->next;
	}
	if( ! chan ) return FALSE;

	Log( LOG_DEBUG, "Freed channel structure for \"%s\".", Chan->name );

	/* Invite- und Ban-Lists aufraeumen */
	Lists_DeleteChannel( chan );

	/* Neu verketten und freigeben */
	if( last_chan ) last_chan->next = chan->next;
	else My_Channels = chan->next;
	free( chan );
		
	return TRUE;
} /* Delete_Channel */


/* -eof- */
