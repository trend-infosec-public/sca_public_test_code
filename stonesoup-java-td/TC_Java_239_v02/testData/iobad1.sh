#!/bin/sh

java -cp jopt-simple-3.2.jar:bin stonesoup.ClientCWE239 -a 127.0.0.1 -p 8021 -m GET -t /index.html -s | tr -d '\015' > iobad1out.txt

#end of file