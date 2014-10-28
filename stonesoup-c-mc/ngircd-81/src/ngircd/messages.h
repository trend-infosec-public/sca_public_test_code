/*
 * ngIRCd -- The Next Generation IRC Daemon
 * Copyright (c)2001-2004 Alexander Barton <alex@barton.de>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * Please read the file COPYING, README and AUTHORS for more information.
 *
 * $Id: messages.h,v 1.66 2004/02/28 02:18:16 alex Exp $
 *
 * IRC numerics (Header)
 */


#ifndef __messages_h__
#define __messages_h__


#define RPL_WELCOME_MSG			"001 %s :Welcome to the Internet Relay Network %s"
#define RPL_YOURHOST_MSG		"002 %s :Your host is %s, running version ngircd-%s (%s/%s/%s)"
#define RPL_CREATED_MSG			"003 %s :This server has been started %s"
#define RPL_MYINFO_MSG			"004 %s %s ngircd-%s %s %s"
#define RPL_ISUPPORT_MSG		"005 %s NICKLEN=%d TOPICLEN=%d AWAYLEN=%d MAXCHANNELS=%d :are supported on this server"

#define RPL_TRACELINK_MSG		"200 %s Link %s-%s %s %s V%s %ld %d %d"
#define RPL_TRACEOPERATOR_MSG		"204 %s Oper 2 :%s"
#define RPL_TRACESERVER_MSG		"206 %s Serv 1 0S 0C %s[%s@%s] *!*@%s :V%s"
#define RPL_STATSLINKINFO_MSG		"211 %s %s %d %ld %ld %ld %ld :%ld"
#define RPL_STATSCOMMANDS_MSG		"212 %s %s %ld %ld %ld"
#define RPL_ENDOFSTATS_MSG		"219 %s %c :End of STATS report"
#define RPL_UMODEIS_MSG			"221 %s +%s"
#define RPL_LUSERCLIENT_MSG		"251 %s :There are %ld users and %ld services on %ld servers"
#define RPL_LUSEROP_MSG			"252 %s %ld :operator(s) online"
#define RPL_LUSERUNKNOWN_MSG		"253 %s %ld :unknown connection(s)"
#define RPL_LUSERCHANNELS_MSG		"254 %s %ld :channels formed"
#define RPL_LUSERME_MSG			"255 %s :I have %ld users, %ld services and %ld servers"
#define RPL_ADMINME_MSG			"256 %s %s :Administrative info"
#define RPL_ADMINLOC1_MSG		"257 %s :%s"
#define RPL_ADMINLOC2_MSG		"258 %s :%s"
#define RPL_ADMINEMAIL_MSG		"259 %s :%s"
#define RPL_TRACEEND_MSG		"262 %s %s %s-%s.%s :End of TRACE"
#define RPL_LOCALUSERS_MSG		"265 %s :Current local users: %ld, Max: %ld"
#define RPL_NETUSERS_MSG		"266 %s :Current global users: %ld, Max: %ld"

#define RPL_AWAY_MSG			"301 %s %s :%s"
#define RPL_USERHOST_MSG		"302 %s :"
#define RPL_ISON_MSG			"303 %s :"
#define RPL_UNAWAY_MSG			"305 %s :You are no longer marked as being away"
#define RPL_NOWAWAY_MSG			"306 %s :You have been marked as being away"
#define RPL_WHOISUSER_MSG		"311 %s %s %s %s * :%s"
#define RPL_WHOISSERVER_MSG		"312 %s %s %s :%s"
#define RPL_WHOISOPERATOR_MSG		"313 %s %s :is an IRC operator"
#define RPL_WHOWASUSER_MSG		"314 %s %s %s %s * :%s"
#define RPL_ENDOFWHO_MSG		"315 %s %s :End of WHO list"
#define RPL_WHOISIDLE_MSG		"317 %s %s %ld :seconds idle"
#define RPL_ENDOFWHOIS_MSG		"318 %s %s :End of WHOIS list"
#define RPL_WHOISCHANNELS_MSG		"319 %s %s :"
#define RPL_LIST_MSG			"322 %s %s %ld :%s"
#define RPL_LISTEND_MSG			"323 %s :End of LIST"
#define RPL_CHANNELMODEIS_MSG		"324 %s %s +%s"
#define RPL_NOTOPIC_MSG			"331 %s %s :No topic is set"
#define RPL_TOPIC_MSG			"332 %s %s :%s"
#define RPL_INVITING_MSG		"341 %s %s %s"
#define RPL_INVITELIST_MSG		"346 %s %s %s"
#define RPL_ENDOFINVITELIST_MSG		"347 %s %s :End of channel invite list"
#define RPL_VERSION_MSG			"351 %s %s-%s.%s %s :%s"
#define RPL_WHOREPLY_MSG		"352 %s %s %s %s %s %s %s :%d %s"
#define RPL_NAMREPLY_MSG		"353 %s %s %s :"
#define RPL_LINKS_MSG			"364 %s %s %s :%d %s"
#define RPL_ENDOFLINKS_MSG		"365 %s %s :End of LINKS list"
#define RPL_ENDOFNAMES_MSG		"366 %s %s :End of NAMES list"
#define RPL_BANLIST_MSG			"367 %s %s %s"
#define RPL_ENDOFBANLIST_MSG		"368 %s %s :End of channel ban list"
#define RPL_ENDOFWHOWAS_MSG		"369 %s %s :End of WHOWAS list"
#define RPL_MOTD_MSG			"372 %s :- %s"
#define RPL_MOTDSTART_MSG		"375 %s :- %s message of the day"
#define RPL_ENDOFMOTD_MSG		"376 %s :End of MOTD command"
#define RPL_YOUREOPER_MSG		"381 %s :You are now an IRC Operator"
#define RPL_YOURESERVICE_MSG		"383 %s :You are service %s"
#define RPL_TIME_MSG			"391 %s %s :%s"

#define ERR_NOSUCHNICK_MSG		"401 %s %s :No such nick or channel name"
#define ERR_NOSUCHSERVER_MSG		"402 %s %s :No such server"
#define ERR_NOSUCHCHANNEL_MSG		"403 %s %s :No such channel"
#define ERR_CANNOTSENDTOCHAN_MSG	"404 %s %s :Cannot send to channel"
#define ERR_TOOMANYCHANNELS_MSG		"405 %s %s :You have joined too many channels"
#define ERR_WASNOSUCHNICK_MSG		"406 %s %s :There was no such nickname"
#define ERR_NOORIGIN_MSG		"409 %s :No origin specified"
#define ERR_NORECIPIENT_MSG		"411 %s :No receipient given (%s)"
#define ERR_NOTEXTTOSEND_MSG		"412 %s :No text to send"
#define ERR_UNKNOWNCOMMAND_MSG		"421 %s %s :Unknown command"
#define ERR_NOMOTD_MSG			"422 %s :MOTD file is missing"
#define ERR_ERRONEUSNICKNAME_MSG	"432 %s %s :Erroneous nickname"
#define ERR_NICKNAMEINUSE_MSG		"433 %s %s :Nickname already in use"
#define ERR_USERNOTINCHANNEL_MSG	"441 %s %s %s :They aren't on that channel"
#define ERR_NOTONCHANNEL_MSG		"442 %s %s :You are not on that channel"
#define ERR_USERONCHANNEL_MSG		"443 %s %s %s :is already on channel"
#define ERR_NOTREGISTERED_MSG		"451 %s :Connection not registered"
#define ERR_NOTREGISTEREDSERVER_MSG	"451 %s :Connection not registered as server link"
#define ERR_NEEDMOREPARAMS_MSG		"461 %s %s :Syntax error"
#define ERR_ALREADYREGISTRED_MSG	"462 %s :Connection already registered"
#define ERR_PASSWDMISMATCH_MSG		"464 %s :Invalid password"
#define ERR_CHANNELISFULL_MSG		"471 %s %s :Cannot join channel (+l)"
#define ERR_UNKNOWNMODE_MSG		"472 %s: %c :is unknown mode char for %s"
#define ERR_INVITEONLYCHAN_MSG		"473 %s %s :Cannot join channel (+i)"
#define ERR_BANNEDFROMCHAN_MSG		"474 %s %s :Cannot join channel (+b)"
#define ERR_BADCHANNELKEY_MSG		"475 %s %s :Cannot join channel (+k)"
#define ERR_NOPRIVILEGES_MSG		"481 %s :Permission denied"
#define ERR_CHANOPRIVSNEEDED_MSG	"482 %s %s :You are not channel operator"
#define ERR_CANTKILLSERVER_MSG		"483 %s :You can't kill a server!"
#define ERR_RESTRICTED_MSG		"484 %s :Your connection is restricted"
#define ERR_NOOPERHOST_MSG		"491 %s :Not configured for your host"

#define ERR_UMODEUNKNOWNFLAG_MSG	"501 %s :Unknown mode"
#define ERR_UMODEUNKNOWNFLAG2_MSG	"501 %s :Unknown mode \"%c%c\""
#define ERR_USERSDONTMATCH_MSG		"502 %s :Can't set/get mode for other users"

#ifdef ZLIB
#define RPL_STATSLINKINFOZIP_MSG	"211 %s %s %d %ld %ld/%ld %ld %ld/%ld :%ld"
#endif


#endif


/* -eof- */
