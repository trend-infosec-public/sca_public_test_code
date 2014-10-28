@set FTPSERVERIP=127.0.0.1
@set FTPSERVERPORT=1175
@cd ..\testData
@java -classpath ..\bin stonesoup.FTPServer
@cd ..\test
