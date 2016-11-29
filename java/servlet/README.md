# Acrolinx Single Sign-On Proxy Java Servlet Sample

Demo code for implementing proxy in a single sign-on environment in Java.

## Prerequisites

Make sure you have configured your:
* [Acrolinx Server for SSO](/acrolinx-proxy-sample#configure-the-acrolinx-server), and the 
* [integration](/acrolinx-proxy-sample#configure-the-integration).

## Configure Acrolinx Java Servlet Example

Configure the URL of the Acrolinx Server, username and single sign-on password in the file web.xml. The param-name are "acrolinxCoreServer, username and secret".

## Run Servlet in IDE

Open this example project with Eclipse IDE for Java EE Developers.

Right-click on the project and select "Run As / Run on Server" in the shortcut menu or export a war and deploy it to your tomcat server.

## Build and Deploy to a Webcontainer like Apache Tomcat 

```
	cd java/servlet
	mvn package
	cp target/proxySample.war <WEBCONTAINER>/webapps/
```


## Java Class Overview

### com.acrolinx.AcrolinxProxyServlet

This servlet acts as a reverse proxy. The Acrolinx integration uses this servlet to communicate with the Acrolinx core server.