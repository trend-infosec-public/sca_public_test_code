#!/bin/sh

if [ ! -d ../bin ]
then
    mkdir ../bin
fi

if [ ! -d ../bin/stonesoup ]
then
    mkdir ../bin/stonesoup
fi

javac -d ../bin ../src/stonesoup/*.java

#end of file
