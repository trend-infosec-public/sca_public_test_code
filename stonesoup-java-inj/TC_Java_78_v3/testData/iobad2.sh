#!/bin/sh

java -cp jopt-simple-3.2.jar:bin stonesoup.Client -a 127.0.0.1 -p 8021 -m GET -t /bin?command=ls%253B%20mkdir%20exploit_folder -s | tr -d '\015' > iobad2out.txt

#end of file
