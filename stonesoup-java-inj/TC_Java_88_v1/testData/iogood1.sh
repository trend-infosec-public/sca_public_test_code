java -cp jopt-simple-3.2.jar:bin/ stonesoup.FileClient -a 127.0.0.1 -p 8021 -u janedoe -k janedoepassword -d -f identity.txt -s | tr -d '\015' > iogood1out.txt
java -cp jopt-simple-3.2.jar:bin/ stonesoup.FileClient -a 127.0.0.1 -p 8021 -u janedoe -k janedoepassword -n -f iogood1.sh -s | tr -d '\015' >> iogood1out.txt
java -cp jopt-simple-3.2.jar:bin/ stonesoup.FileClient -a 127.0.0.1 -p 8021 -u janedoe -k janedoepassword -l "-la" -s | tr -d '\015' >> iogood1out.txt
