#!/bin/sh

java -cp jopt-simple-3.2.jar:bin stonesoup.FileServer -a 127.0.0.1 -p 8021 -s testData/ssl.jks -c testData/config.properties -k sslpassword -r ./testData

#end of file
