@if not exist bin md bin
@if not exist bin\stonesoup md bin\stonesoup

@javac -verbose -d bin/ src\stonesoup\*.java