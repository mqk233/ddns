This is a springboot project that resolves the local IP of the domain name. Currently, only the domain name applied by aliyun is supported, and more service providers will be supported in the future.

HOW TO USE
1. git clone git@github.com:mqk233/ddns.git
2. cd ddns
3. Modify the configuration of the file "application.yml"
4. mvn package
5. cd target
6. java -jar ddns-0.0.1-SNAPSHOT.jar
7. enjoy it