#!/bin/sh

java -cp jopt-simple-3.2.jar:bin/ stonesoup.FileClient -a 127.0.0.1 -p 8021 -s -u janedoe -k janedoepassword -f identity.txt | tr -d '\015' > iogood1out.txt

#end of file