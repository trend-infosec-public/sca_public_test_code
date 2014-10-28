#!/bin/sh

java -cp jopt-simple-3.2.jar:bin stonesoup.Client -a 127.0.0.1 -p 8021 -m GET -t /user/listfiles?username=%3B%20cat%20%2Fetc%2Fgroup -s | tr -d '\015' > iobad2out.txt

#end of file
