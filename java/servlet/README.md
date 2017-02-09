# Acrolinx Single Sign-On Proxy Java Servlet Sample

Demo code for implementing a proxy in a single sign-on environment in Java.

## Prerequisites

Make sure that you have configured your:
* [Acrolinx Server for SSO](/README.md#configure-the-acrolinx-server), and the 
* [Integration](/README.md#acrolinx-proxy-sample#configure-the-integration).

## Configure Acrolinx Java Servlet Example

Configure the Acrolinx server address, user name, and single sign-on password in the file web.xml. The parameter names are `acrolinxServer`, `username` and `secret`.

## Run Servlet in IDE

To open this example project, use Eclipse IDE for Java EE Developers.

Right-click on the project and select "Run As / Run on Server" in the shortcut menu or export a .war file and deploy it to your tomcat server.

## Build and Deploy to a Web container like Apache Tomcat 

```
cd java/servlet
mvn package
cp target/proxySample.war <WEBCONTAINER>/webapps/
```

### To test the proxy connection to Acrolinx, test one of the rest calls.
If you open `http://<WEBCONTAINER>/proxySample/proxy/iq/services/rest/registry/knownServiceNames`, like [http://localhost:8080/proxySample/proxy/iq/services/rest/registry/knownServiceNames](http://localhost:8080/proxySample/proxy/iq/services/rest/registry/knownServiceNames) you should see a result like:

```
[
  "checking",
  "message",
  "core",
  "terminology",
  "user",
  ...
]
```

### To test the proxy authentication, test if the authenticate call returns a username and token.

If you open `http://<WEBCONTAINER>/proxySample/proxy/sso/v1/authenticate`, like [http://localhost:8080/proxySample/proxy/sso/v1/authenticate](http://localhost:8080/proxySample/proxy/sso/v1/authenticate) you should see a result like:

```
[
  <div id="username">admin</div>
  <div id="authToken">wboSz31sQTjTAFDIWKSDF31sQTHEPQcreXDwboSz31sQTXDwboSz31sQQcreXDwboSz31sp4vnHEPQcreXD==</div>
]
```

## Java Class Overview

### com.acrolinx.AcrolinxProxyServlet

This servlet acts as a reverse proxy. The Acrolinx integration uses this servlet to communicate with the Acrolinx core server.
