#!/bin/sh

export TARGET=TC_C_120_v1084
cd /media/sf_eclipse
rm "$TARGET/Release/$TARGET"; make linall

#end of file
