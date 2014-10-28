#!/bin/sh

java -cp jopt-simple-3.2.jar:bin stonesoup.Client -a 127.0.0.1 -p 8021 -m GET -t "/cgi-bin?command=ls&command=%2Dla&path=%2Fbin" -s | tr -d '\015' > iobad1out.txt

#end of file
