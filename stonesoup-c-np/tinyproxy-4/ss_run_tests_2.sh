#!/bin/sh

SCRIPTS_DIR=$(pwd)/$(dirname $0)

TINYPROXY_IP=localhost
TINYPROXY_PORT=8888

TINYPROXY_PID_FILE=tinyproxy.pid

WEBSERVER_IP=stonesoup-server.mitre.org
#WEBSERVER_IP=10.84.3.7
WEBSERVER_PORT=80
WEBCLIENT_BIN=$SCRIPTS_DIR/ss_webclient.pl

stop_tinyproxy() {
	kill $(cat $TINYPROXY_PID_FILE) 
}

wait_for_some_seconds() {
	SECONDS=$1
	if test "x$SECONDS" = "x" ; then
		SECONDS=1
	fi

	echo -n "waiting for $SECONDS seconds."

	for COUNT in $(seq 1 $SECONDS) ; do
		sleep 1
		echo -n "."
	done
	echo " done"
}

run_basic_webclient_request() {
	 java -cp . DoubleFreeURL > $1
}


# "main"

FAILED=0
DONE_TEST=0
SERVER=tinyproxy-4

while [ $DONE_TEST -eq 0 ]
do
        if ps ax | grep -v grep | grep $SERVER > /dev/null
        then
		run_basic_webclient_request $1
		stop_tinyproxy > $2
		DONE_TEST=1
	else   
	        wait_for_some_seconds 2
        fi
done

exit $FAILED
