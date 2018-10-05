# Acrolinx Single Sign-On Proxy Java Servlet Sample

Demo code for implementing a proxy in a single sign-on environment in Java.

## Prerequisites

Make sure that you've configured your:

* [Core Platform for SSO](/README.md#configure-the-acrolinx-server), and the
* [Integration](/README.md#acrolinx-proxy-sample#configure-the-integration).

## Configure Acrolinx Java Servlet Example

Configure the Acrolinx URL, username, and single sign-on password in the file `web.xml`.
The parameter names are `acrolinxServer`, `username`, and `secret`.

## Run Servlet in IDE

To open this example project, use Eclipse IDE for Java EE Developers.

Right-click on the project and select "Run As / Run on Server" in the shortcut menu.
Alternatively export a `.war`-file and deploy it to your tomcat server.

## Build and Deploy

Deploy to a Web container like Apache Tomcat:

```bash
cd java/servlet
mvn package
cp target/proxySample.war <WEBCONTAINER>/webapps/
```

### Test the Proxy

If you open `http://<WEBCONTAINER>/proxySample/proxy/iq/services/rest/registry/knownServiceNames`, like [http://localhost:8080/proxySample/proxy/iq/services/rest/registry/knownServiceNames](http://localhost:8080/proxySample/proxy/iq/services/rest/registry/knownServiceNames).
You should see a result like:

```json
[
  "checking",
  "message",
  "core",
  "terminology",
  "user",
  //...
]
```

### Test the Proxy Authentication

Test if the authenticate call returns a username and token.

If you open `http://<WEBCONTAINER>/proxySample/proxy/sso/v1/authenticate`, like [http://localhost:8080/proxySample/proxy/sso/v1/authenticate](http://localhost:8080/proxySample/proxy/sso/v1/authenticate).
You should see a result like:

```html
<div id="username">admin</div>
<div id="authToken">wboSz31sQTjTAFDIWKSDF31sQTHEPQcreXDwboSz31sQTXDwboSz31sQQcreXDwboSz31sp4vnHEPQcreXD==</div>
```

### Troubleshooting

#### Connecting to HTTPS Acrolinx Throws `javax.net.ssl.SSLHandshakeException`

This exception indicates that the integration and platform couldn't negotiate the desired level of security.
Get Acrolinx SSL certificate from your administrator.
To install Acrolinx certificate to JVM's trust store,
follow the [Java tool signing steps](https://docs.oracle.com/javase/tutorial/security/toolsign/rstep2.html).

Restart your Web server.

## Java Class Overview

The servlet `com.acrolinx.AcrolinxProxyServlet` acts as a reverse proxy.
The Acrolinx Integration uses this servlet to communicate with the Acrolinx Core Platform.
