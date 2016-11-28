# Acrolinx Java Proxy Servlet Sample

This java application is for implementing proxy in a single sign-on environment.

## To run the demo application, follow these steps

Configure the Acrolinx core server for single sign-on by adding the following example properties to the coreserver.properties file:

```
singleSignOn.method=header
singleSignOn.genericPassword=secret
singleSignOn.usernameKey=username
singleSignOn.passwordKey=password
```

Open this example project with Eclipse IDE for Java EE Developers.
Configure the URL of the Acrolinx core server, username and Single sign-on password in the file web.xml. The param-name are "acrolinxCoreServer, username and secret".

Right-click on the project and select "Run As / Run on Server" in the shortcut menu or export a war and deploy it to your tomcat server.
In your Acrolinx integration enable SSO by adding property "enableSingleSignOn : true" to plugin configuration add set server address to point to the proxy "serverAddress: /proxySample/proxy".

## Java Class Overview


### com.acrolinx.AcrolinxProxyServlet

This servlet acts as a reverse proxy. The Acrolinx integration uses this servlet to communicate with the Acrolinx core server.