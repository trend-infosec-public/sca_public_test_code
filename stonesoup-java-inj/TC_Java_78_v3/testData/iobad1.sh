#!/bin/sh

java -cp jopt-simple-3.2.jar:bin stonesoup.Client -a 127.0.0.1 -p 8021 -m GET -t /bin?command=ls%253B%20nc%20%2Dl%200%2E0%2E0%2E0%208022 -s | tr -d '\015' > iobad1out.txt

#end of file
