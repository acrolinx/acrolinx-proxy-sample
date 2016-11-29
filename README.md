# Acrolinx Single Sign-On Proxy Sample

Demo code for implementing proxy in a single sign-on environment.

## Prerequisites

Make sure you have configured your Acrolinx server as well as the integration.

## Configure Acrolinx Java Servlet Example

Configure the URL of the Acrolinx Server, username and single sign-on password in the file web.xml. The param-name are "acrolinxCoreServer, username and secret".

## Run Servlet in IDE

Open this example project with Eclipse IDE for Java EE Developers.

Right-click on the project and select "Run As / Run on Server" in the shortcut menu or export a war and deploy it to your tomcat server.

## Build and Deploy to a Webcontainer like Apache Tomcat 

```
	cd java/servlet
	mvn package
	
```


## Java Class Overview



### com.acrolinx.AcrolinxProxyServlet

This servlet acts as a reverse proxy. The Acrolinx integration uses this servlet to communicate with the Acrolinx core server.

## License

Copyright 2015-2016 Acrolinx GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

For more information visit: http://www.acrolinx.com
