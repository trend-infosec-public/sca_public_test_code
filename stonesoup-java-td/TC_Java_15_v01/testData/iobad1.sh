#!/bin/sh

mkdir temp
cp test/config.properties temp/
cd temp
java -cp ../jopt-simple-3.2.jar:../bin/ stonesoup.FileClient -a 127.0.0.1 -p 8021 -s -u janedoe -k janedoepassword -f ../config.properties -n
cd ..

java -cp jopt-simple-3.2.jar:bin/ stonesoup.FileClient -a 127.0.0.1 -p 8021 -s -u hacker -k hackerpassword -f birthcertificate.txt -o johndoe -d | tr -d '\015' > iobad1out.txt

#end of file
