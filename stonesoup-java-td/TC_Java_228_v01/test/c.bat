@if not exist bin md bin
@if not exist bin\stonesoup md bin\stonesoup

@javac -d bin -cp .:lib src\stonesoup\*.java