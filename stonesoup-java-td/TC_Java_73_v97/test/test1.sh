#!/bin/sh

export FTPSERVERIP=127.0.0.1
export FTPSERVERPORT=1175

java -classpath ../bin stonesoup.FTPServer &
java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @IoPairGd1.script

java -classpath ../bin stonesoup.FTPServer &
java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @IoPairGd2.script

java -classpath ../bin stonesoup.FTPServer &
java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @IoPairGd3.script

java -classpath ../bin stonesoup.FTPServer &
java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @IoPairGd4.script

java -classpath ../bin stonesoup.FTPServer &
java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @IoPairGd5.script

java -classpath ../bin stonesoup.FTPServer &
java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @IoPairBd1.script
