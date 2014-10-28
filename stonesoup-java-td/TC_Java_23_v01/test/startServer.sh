#!/bin/sh

java -cp jopt-simple-3.2.jar:bin/ stonesoup.FileServer -a 127.0.0.1 -p 8021 -c testData/config.properties -s testData/ssl.jks -k sslpassword -r ./testData

#end of file
