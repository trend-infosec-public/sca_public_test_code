@cd ..\src
C:\splint-3.1.2\bin\splint.exe -DINCLUDE=c:\MinGW\include -DLIB=c:\MinGW\lib -DLIBPATH=c:\MinGW\lib -warnposix -DLONG_MAX=2147483647 -DSHRT_MAX=32767 -D__int32=int -D__int64="long long" desaturate.c
@cd ..\test
@rem End of file