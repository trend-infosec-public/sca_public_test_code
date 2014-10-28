#!/bin/sh

SCRIPTS_DIR=$(pwd)
IRC_SERVER=127.0.0.1
IRC_PORT=6667
IRC_USER=stony
PERL_CMD=perl
IRCCLIENT_BIN=./ngircd-0.8.1_exploit.sh
#IRCCLIENT_INPUT=$4
IRCCLIENT_EXIT_CODE=-1

stop_ngirc() {
kill $(ps ux | awk '/ngircd-81.conf/ && !/awk/ {print $2}')
}

wait_for_some_seconds() {
	SECONDS=$1
	if test "x$SECONDS" = "x" ; then
		SECONDS=1
	fi

	for COUNT in $(seq 1 $SECONDS) ; do
		sleep 1
	done
}

run_ircclient_request() {
	./ngircd-0.8.1_exploit.sh 127.0.0.1 6667 
#	$IRCCLENT_BIN $IRC_SERVER $IRC_PORT > $1
}

# "main"

FAILED=0

DONE_TEST=0 
SERVER=ngircd-81

while [ $DONE_TEST -eq 0 ]
do
	if ps ax | grep -v grep | grep $SERVER > /dev/null
	then
		run_ircclient_request 
		stop_ngirc
		DONE_TEST=1
	else
		wait_for_some_seconds 2
	fi
done

exit $FAILED
