@set FTPSERVERIP=127.0.0.1
@set FTPSERVERPORT=43221
@cd ..\testData
@java -classpath ..\bin stonesoup.FTPServer 2
@cd ..\test
