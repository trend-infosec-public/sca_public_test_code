#!/bin/sh

export FTPSERVERIP=127.0.0.1
export FTPSERVERPORT=1175
java -classpath ../bin stonesoup.FTPServer
