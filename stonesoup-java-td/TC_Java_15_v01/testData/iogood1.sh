#!/bin/sh

java -cp jopt-simple-3.2.jar:bin/ stonesoup.FileClient -a localhost -p 8021 -s -u janedoe -k janedoepassword -f identity.txt -d | tr -d '\015' > iogood1out.txt

#end of file
