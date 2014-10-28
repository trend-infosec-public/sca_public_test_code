@echo off

set FTPSERVERIP=127.0.0.1
set FTPSERVERPORT=1175

start java -classpath ..\bin stonesoup.FTPServer
java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @IoPairGd1.script

start java -classpath ..\bin stonesoup.FTPServer
java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @IoPairGd2.script

start java -classpath ..\bin stonesoup.FTPServer
java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @IoPairGd3.script

start java -classpath ..\bin stonesoup.FTPServer
java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @IoPairGd4.script

start java -classpath ..\bin stonesoup.FTPServer
java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @IoPairGd5.script

start java -classpath ..\bin stonesoup.FTPServer
java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @IoPairBd1.script
