#!/bin/sh

export TARGET=TC_C_124_v1046
cd /media/sf_eclipse
rm "$TARGET/Release/$TARGET"; make linall

#end of file
