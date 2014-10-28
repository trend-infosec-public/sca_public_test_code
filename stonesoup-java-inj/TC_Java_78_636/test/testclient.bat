@set FTPSERVERIP=127.0.0.1
@set FTPSERVERPORT=1175
@cd ..\testData

java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @Good_Command1.script

java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @Good_Command2.script

java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @Good_Command3.script

java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @Good_Command4.script

java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @Good_Command5.script

java -classpath ../../FTPTestClient/bin stonesoup.FTPTestClient @Bad_Command1.script

@cd ..\test
