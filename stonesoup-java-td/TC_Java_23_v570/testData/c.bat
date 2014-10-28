if not exist ..\bin md ..\bin
if not exist ..\bin\stonesoup md ..\bin\stonesoup

javac -d ..\bin\ ..\src\stonesoup\DataSocket.java ..\src\stonesoup\Command.java ..\src\stonesoup\FileHandler.java ..\src\stonesoup\FTPServer.java