#!/bin/sh

java -cp jopt-simple-3.2.jar:bin stonesoup.Driver -a 127.0.0.1 -p 8080 -c config.properties -s ssl.jks -k sslpassword

#end of file
