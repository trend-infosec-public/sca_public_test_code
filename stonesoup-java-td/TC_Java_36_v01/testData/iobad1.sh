#!/bin/sh

java -cp jopt-simple-3.2.jar:bin/ stonesoup.EvilFileClient -a 127.0.0.1 -p 8021 -s -u janedoe -k janedoepassword -f /etc/passwd | tr -d '\015' > iobad1out.txt

#end of file
