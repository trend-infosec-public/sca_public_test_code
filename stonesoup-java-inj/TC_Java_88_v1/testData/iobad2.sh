java -cp "bin:jopt-simple-3.2.jar" stonesoup.FileClient -a 127.0.0.1 -p 8021 -u janedoe -k janedoepassword -l "-la && cd ../../ && ls -la" -s | tr -d '\015'
