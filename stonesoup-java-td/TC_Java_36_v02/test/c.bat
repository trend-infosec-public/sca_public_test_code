@if not exist bin md bin
@if not exist bin\stonesoup md bin\stonesoup

@javac -verbose -d bin/ -cp jopt-simple-3.2.jar src\stonesoup\utils\*.java src\stonesoup\http\*.java src\stonesoup\handlers\*.java src\stonesoup\filesystem\*.java src\stonesoup\*.java