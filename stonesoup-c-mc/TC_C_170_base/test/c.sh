#!/bin/sh

export TARGET=TC_C_170_base
cd /media/sf_eclipse
rm "$TARGET/Release/$TARGET"; make linall

#end of file
