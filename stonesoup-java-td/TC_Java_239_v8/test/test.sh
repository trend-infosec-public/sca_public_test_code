#!/bin/sh

export FTPSERVERIP=127.0.0.1
export FTPSERVERPORT=43221

java -classpath ../../FTPClient/bin stonesoup.FTPClient commands_3.txt

# End of file
