#!/bin/sh

java -cp jopt-simple-3.2.jar:bin/ stonesoup.FileClient -a 127.0.0.1 -p 8021 -u janedoe -k janedoepassword -f identity.txt -d -s | tr -d '\015' > iogood1out.txt

#end of file