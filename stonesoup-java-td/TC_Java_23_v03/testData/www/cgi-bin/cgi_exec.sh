#!/bin/bash
#cgi_exec.sh
#Invokes a command with a jailed path, so all system commands are not availbale.

#index=1
#
#for arg in "$@"
#do
#  echo "Arg #$index = $arg"
#  let "index+=1"
#done

exec "$@"

#end of file
