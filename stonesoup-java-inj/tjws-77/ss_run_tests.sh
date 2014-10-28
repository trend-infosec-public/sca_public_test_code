#!/bin/sh
#
# ss_run_tests.sh
# script for running test
#
# (c) 2011 The MITRE Corporation.  ALL RIGHTS RESERVED.
#
SCRIPTS_DIR=$(pwd)/$(dirname $0)
WEBSERVER_IP=localhost
WEBSERVER_PORT=8080

WEBCLIENT_BIN=$SCRIPTS_DIR/ss_webclient.pl

stop_tjws() {
kill $(ps ux | awk '/Acme/ && !/awk/ {print $2}')
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

run_basic_webclient_request() {
	$WEBCLIENT_BIN $1 $2 > $3
	WEBCLIENT_EXIT_CODE=$?
	if test "x$WEBCLIENT_EXIT_CODE" = "x0" ; then
		echo " ok"
	else
		echo "ERROR ($WEBCLIENT_EXIT_CODE)"
	fi

	return $WEBCLIENT_EXIT_CODE
}

run_webclient_request() {
	$WEBCLIENT_BIN $1 $2 > $3
}

# "main"

echo "ss_run_tests.sh started"
echo "WEBCLIENT_BIN = $WEBCLIENT_BIN";

FAILED=0

DONE_TEST=0 
SERVER=Acme

while [ $DONE_TEST -eq 0 ]
do
	if ps ax | grep -v grep | grep $SERVER > /dev/null
	then
		run_webclient_request "$WEBSERVER_IP:$WEBSERVER_PORT" "$1" "$2" /
		test "x$?" = "x0" || FAILED=$((FAILED + 1))
		stop_tjws
		DONE_TEST=1
	else
		wait_for_some_seconds 5
	fi
done

exit $FAILED
