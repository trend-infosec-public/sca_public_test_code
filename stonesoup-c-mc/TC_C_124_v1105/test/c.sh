#!/bin/sh

export TARGET=TC_C_124_v1105
cd /media/sf_eclipse
rm "$TARGET/Release/$TARGET"; make linall

#end of file
