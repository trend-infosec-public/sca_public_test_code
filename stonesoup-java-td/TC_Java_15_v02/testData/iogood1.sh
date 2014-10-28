#!/bin/sh

java -cp jopt-simple-3.2.jar:bin stonesoup.Client -a 127.0.0.1 -p 8021 -m GET -t "/log?log=test.log&message=Hello%20World!" -s | tr -d '\015' > iogood1out.txt

#end of file

