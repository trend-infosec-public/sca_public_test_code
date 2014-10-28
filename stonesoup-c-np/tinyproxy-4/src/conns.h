/* $Id: conns.h,v 1.4 2001/11/21 01:00:09 rjkaes Exp $
 *
 * See 'conns.c' for a detailed description.
 *
 * Copyright (C) 2001  Robert James Kaes (rjkaes@flarenet.com)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */

#ifndef TINYPROXY_CONNS_H
#define TINYPROXY_CONNS_H

/*
 * Connection Definition
 */
struct conn_s {
	int client_fd;
	int server_fd;
	struct buffer_s *cbuffer;
	struct buffer_s *sbuffer;
	bool_t simple_req;
	bool_t ssl;
	bool_t upstream;
	bool_t send_message;
	struct {
		unsigned short int major;
		unsigned short int minor;
	} protocol;
};

/*
 * Functions for the creation and destruction of a connection structure.
 */
extern void initialize_conn(struct conn_s *connptr);
extern void destroy_conn(struct conn_s *connptr);

#endif
