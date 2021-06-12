# ddns

​	This is a springboot project that resolves the local IP of the domain name. Currently, only the domain name applied by aliyun is supported, and more service providers will be supported in the future.

### Installation

#### Prepare your runtime environment

​	[maven](http://maven.apache.org/install.html)

​	[java](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)

#### Let's Go

1. Clone project to local disk.

   ```
   git clone git@github.com:mqk233/ddns.git
   ```

2. Modify the configuration of the file "application.yml".

   replace the value of "access-key" and "secret-key" to yours

3. Compile the java project.

   ```
   cd ddns
   mvn package
   ```

4. Run the java project.

   ```
   java -jar ddns-0.0.1-SNAPSHOT.jar "your domain"
   ```

5. enjoy it.