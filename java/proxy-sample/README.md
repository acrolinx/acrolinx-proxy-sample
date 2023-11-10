# Acrolinx Single Sign-On Proxy Java Servlet Sample

Demo code for implementing a proxy in a single sign-on environment in Java.

## Prerequisites

Make sure that you've configured your:

* [Core Platform for SSO](/README.md#configure-the-acrolinx-server), and the
* [Integration](/README.md#acrolinx-proxy-sample#configure-the-integration).

## Configure Acrolinx Java Servlet Example

Configure the Acrolinx URL, username, and single sign-on password in the file [`web.xml`](src/main/webapp/WEB-INF/web.xml).
The parameter names are `acrolinxURL`, `username`, and `genericToken`.

## Run Servlet in IDE

To open this example project, use Eclipse IDE for Java EE Developers.

Right-click on the project and select "Run As / Run on Server" in the shortcut menu.
Alternatively export a `.war`-file and deploy it to your tomcat server.

## Build and Deploy

Deploy to a Web container like Apache Tomcat:

```bash
cd java/proxy-sample
mvn package
cp target/proxy-sample.war <WEBCONTAINER>/webapps/
```

### Test the Proxy

The given sample demonstrates consuming the rest call `api/v1/auth/sign-ins` for Acrolinx authentication.

If you open `http://<WEBCONTAINER>/proxy-sample/`, like [http://localhost:8080/proxy-sample/](http://localhost:8080/proxy-sample/)
then press the sign-in button and it will either give you the interactive URL to the complete sign-in,
or a success message with details.

### Test Using Docker

1. Make sure [docker](https://www.docker.com/) is installed.
2. Make sure you are in the directory `java/proxy-sample`.
3. [Configure](#configure-acrolinx-java-servlet-example) the proxy [`web.xml`](src/main/webapp/WEB-INF/web.xml).
4. To build the `war`-file, run `mvn package -DskipTests`.
5. To build a docker image, run `docker build -t mywebapp ./`
6. Run the docker image, by running `docker run -p 8888:8080 mywebapp`.
7. Connect using a web browser [localhost:8888/proxy-sample](http://localhost:8888/proxy-sample/).

### Troubleshooting

#### Connecting to HTTPS Acrolinx Throws `javax.net.ssl.SSLHandshakeException`

This exception indicates that the integration and platform couldn't negotiate the desired level of security.
Get the Acrolinx SSL certificate from your administrator.
To install the Acrolinx certificate to JVM's trust store,
follow the [Java tool signing steps](https://docs.oracle.com/javase/tutorial/security/toolsign/rstep2.html).

Restart your Web server.

##### See Also

* [Acrolinx Secure Tunnel](https://github.com/acrolinx/secure-tunnel)

## Java Class Overview

The servlet `com.acrolinx.proxy.AcrolinxProxyHttpServlet` acts as a reverse proxy.
The Acrolinx Integration uses this servlet to communicate with the Acrolinx Core Platform.
