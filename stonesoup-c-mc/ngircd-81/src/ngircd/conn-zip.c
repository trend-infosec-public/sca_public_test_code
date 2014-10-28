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
 * Connection compression using ZLIB
 */


#include "portab.h"

#define CONN_MODULE


#ifdef ZLIB

static char UNUSED id[] = "$Id: conn-zip.c,v 1.5 2004/04/25 13:55:36 alex Exp $";

#include "imp.h"
#include <assert.h>
#include <string.h>
#include <zlib.h>

#include "conn.h"
#include "conn-func.h"
#include "log.h"

#include "exp.h"
#include "conn-zip.h"


GLOBAL BOOLEAN
Zip_InitConn( CONN_ID Idx )
{
	/* Kompression fuer Link initialisieren */

	assert( Idx > NONE );

	My_Connections[Idx].zip.in.avail_in = 0;
	My_Connections[Idx].zip.in.total_in = 0;
	My_Connections[Idx].zip.in.total_out = 0;
	My_Connections[Idx].zip.in.zalloc = NULL;
	My_Connections[Idx].zip.in.zfree = NULL;
	My_Connections[Idx].zip.in.data_type = Z_ASCII;

	if( inflateInit( &My_Connections[Idx].zip.in ) != Z_OK )
	{
		/* Fehler! */
		Log( LOG_ALERT, "Can't initialize compression on connection %d (zlib inflate)!", Idx );
		return FALSE;
	}

	My_Connections[Idx].zip.out.total_in = 0;
	My_Connections[Idx].zip.out.total_in = 0;
	My_Connections[Idx].zip.out.zalloc = NULL;
	My_Connections[Idx].zip.out.zfree = NULL;
	My_Connections[Idx].zip.out.data_type = Z_ASCII;

	if( deflateInit( &My_Connections[Idx].zip.out, Z_DEFAULT_COMPRESSION ) != Z_OK )
	{
		/* Fehler! */
		Log( LOG_ALERT, "Can't initialize compression on connection %d (zlib deflate)!", Idx );
		return FALSE;
	}

	My_Connections[Idx].zip.bytes_in = My_Connections[Idx].bytes_in;
	My_Connections[Idx].zip.bytes_out = My_Connections[Idx].bytes_out;

	Log( LOG_INFO, "Enabled link compression (zlib) on connection %d.", Idx );
	Conn_SetOption( Idx, CONN_ZIP );

	return TRUE;
} /* Zip_InitConn */


GLOBAL BOOLEAN
Zip_Buffer( CONN_ID Idx, CHAR *Data, INT Len )
{
	/* Daten zum Komprimieren im "Kompressions-Puffer" sammeln.
	* Es wird TRUE bei Erfolg, sonst FALSE geliefert. */

	assert( Idx > NONE );
	assert( Data != NULL );
	assert( Len > 0 );

	/* Ist noch Platz im Kompressions-Puffer? */
	if( ZWRITEBUFFER_LEN - My_Connections[Idx].zip.wdatalen < Len + 50 )
	{
		/* Nein! Puffer zunaechst leeren ...*/
		if( ! Zip_Flush( Idx )) return FALSE;
	}

	/* Daten kopieren */
	memmove( My_Connections[Idx].zip.wbuf + My_Connections[Idx].zip.wdatalen, Data, Len );
	My_Connections[Idx].zip.wdatalen += Len;

	return TRUE;
} /* Zip_Buffer */


GLOBAL BOOLEAN
Zip_Flush( CONN_ID Idx )
{
	/* Daten komprimieren und in Schreibpuffer kopieren.
	* Es wird TRUE bei Erfolg, sonst FALSE geliefert. */

	INT result, out_len;
	z_stream *out;

	out = &My_Connections[Idx].zip.out;

	out->next_in = (VOID *)My_Connections[Idx].zip.wbuf;
	out->avail_in = My_Connections[Idx].zip.wdatalen;
	out->next_out = (VOID *)(My_Connections[Idx].wbuf + My_Connections[Idx].wdatalen);
	out->avail_out = WRITEBUFFER_LEN - My_Connections[Idx].wdatalen;

	result = deflate( out, Z_SYNC_FLUSH );
	if(( result != Z_OK ) || ( out->avail_in > 0 ))
	{
		Log( LOG_ALERT, "Compression error: code %d!?", result );
		Conn_Close( Idx, "Compression error!", NULL, FALSE );
		return FALSE;
	}

	out_len = WRITEBUFFER_LEN - My_Connections[Idx].wdatalen - out->avail_out;
	My_Connections[Idx].wdatalen += out_len;
	My_Connections[Idx].bytes_out += out_len;
	My_Connections[Idx].zip.bytes_out += My_Connections[Idx].zip.wdatalen;
	My_Connections[Idx].zip.wdatalen = 0;

	return TRUE;
} /* Zip_Flush */


GLOBAL BOOLEAN
Unzip_Buffer( CONN_ID Idx )
{
	/* Daten entpacken und in Lesepuffer kopieren. Bei Fehlern
	* wird FALSE geliefert, ansonsten TRUE. Der Fall, dass keine
	* Daten mehr zu entpacken sind, ist _kein_ Fehler! */

	INT result, in_len, out_len;
	z_stream *in;

	assert( Idx > NONE );

	if( My_Connections[Idx].zip.rdatalen <= 0 ) return TRUE;

	in = &My_Connections[Idx].zip.in;

	in->next_in = (VOID *)My_Connections[Idx].zip.rbuf;
	in->avail_in = My_Connections[Idx].zip.rdatalen;
	in->next_out = (VOID *)(My_Connections[Idx].rbuf + My_Connections[Idx].rdatalen);
	in->avail_out = READBUFFER_LEN - My_Connections[Idx].rdatalen - 1;

	result = inflate( in, Z_SYNC_FLUSH );
	if( result != Z_OK )
	{
		Log( LOG_ALERT, "Decompression error: %s (code=%d, ni=%d, ai=%d, no=%d, ao=%d)!?", in->msg, result, in->next_in, in->avail_in, in->next_out, in->avail_out );
		Conn_Close( Idx, "Decompression error!", NULL, FALSE );
		return FALSE;
	}

	in_len = My_Connections[Idx].zip.rdatalen - in->avail_in;
	out_len = READBUFFER_LEN - My_Connections[Idx].rdatalen - 1 - in->avail_out;
	My_Connections[Idx].rdatalen += out_len;

	if( in->avail_in > 0 )
	{
		/* es konnten nicht alle Daten entpackt werden, vermutlich war
		* im Ziel-Puffer kein Platz mehr. Umkopieren ... */
		My_Connections[Idx].zip.rdatalen -= in_len;
		memmove( My_Connections[Idx].zip.rbuf, My_Connections[Idx].zip.rbuf + in_len, My_Connections[Idx].zip.rdatalen );
	}
	else My_Connections[Idx].zip.rdatalen = 0;
	My_Connections[Idx].zip.bytes_in += out_len;

	return TRUE;
} /* Unzip_Buffer */


GLOBAL LONG
Zip_SendBytes( CONN_ID Idx )
{
	/* Anzahl gesendeter Bytes (komprimiert!) liefern */

	assert( Idx > NONE );
	return My_Connections[Idx].zip.bytes_out;
} /* Zip_SendBytes */


GLOBAL LONG
Zip_RecvBytes( CONN_ID Idx )
{
	/* Anzahl gesendeter Bytes (komprimiert!) liefern */

	assert( Idx > NONE );
	return My_Connections[Idx].zip.bytes_in;
} /* Zip_RecvBytes */


#endif


/* -eof- */
