# Acrolinx Single Sign-On Proxy Java Servlet Sample

Demo code for implementing proxy in a single sign-on environment in Java.

## Prerequisites

Make sure that you have configured your:
* [Acrolinx Server for SSO](/README.md#configure-the-acrolinx-server), and the 
* [Integration](/README.md#acrolinx-proxy-sample#configure-the-integration).

## Configure Acrolinx Java Servlet Example

Configure the URL of the Acrolinx Server, username and single sign-on password in the file web.xml. The param-name are "acrolinxServer, username and secret".

## Run Servlet in IDE

Open this example project with Eclipse IDE for Java EE Developers.

Right-click on the project and select "Run As / Run on Server" in the shortcut menu or export a war and deploy it to your tomcat server.

## Build and Deploy to a Webcontainer like Apache Tomcat 

```
cd java/servlet
mvn package
cp target/proxySample.war <WEBCONTAINER>/webapps/
```

You can test, if you can connect the Acrolinx by just testing one rest calls.
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

## Java Class Overview

### com.acrolinx.AcrolinxProxyServlet

This servlet acts as a reverse proxy. The Acrolinx integration uses this servlet to communicate with the Acrolinx core server.
