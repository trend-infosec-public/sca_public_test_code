#!/bin/sh

SCRIPTS_DIR=$(pwd)
IRC_SERVER=localhost
IRC_PORT=6667
IRC_USER=stony
PERL_CMD=perl
IRCCLIENT_BIN=$SCRIPTS_DIR/ss_irc_client.pl
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

run_basic_ircclient_request() {
	#$WEBCLIENT_BIN $1 $2 >> $WEBCLIENT_LOG 2>&1
	$PERL_CMD $IRCCLIENT_BIN $IRC_SERVER $IRC_PORT $IRC_USER < ss_sample_input.txt > $1
	$IRCCLIENT_EXIT_CODE=$?
	if test "x$IRCCLIENT_EXIT_CODE" = "x0" ; then
		echo " ok"
	else
		echo "ERROR ($IRCCLIENT_EXIT_CODE)"
	fi
#
#	return $WEBCLIENT_EXIT_CODE
}

run_ircclient_request() {
	#$WEBCLIENT_BIN $1 $2 >> $WEBCLIENT_LOG 2>&1
	$PERL_CMD $IRCCLIENT_BIN $IRC_SERVER $IRC_PORT $IRC_USER < ss_sample_input.txt > $1
}

# "main"

FAILED=0

DONE_TEST=0 
SERVER=ngircd-81

while [ $DONE_TEST -eq 0 ]
do
	if ps ax | grep -v grep | grep $SERVER > /dev/null
	then
		run_ircclient_request $1
		stop_ngirc
		DONE_TEST=1
	else
		wait_for_some_seconds 2
	fi
done

exit $FAILED
