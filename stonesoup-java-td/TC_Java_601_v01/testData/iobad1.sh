#!/bin/sh

java -cp jopt-simple-3.2.jar:bin stonesoup.Client -a 127.0.0.1 -p 8021 -m GET -t "/redirect?url=http:%252F%252Fwww.google.com" -s | tr -d '\015' > iobad1out.txt

#end of file
