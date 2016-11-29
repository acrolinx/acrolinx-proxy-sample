# Acrolinx Single Sign-On Proxy Sample

Demo code for implementing proxy in a single sign-on environment.

## Prerequisites

Please contact Acrolinx SDK support (sdk-support@acrolinx.com) for initial consulting. 
We like to schedule a kickoff meeting to answer any questions about your integration project. 
After the meeting, we provide you with test server credentials and configuration settings you need to get started.

## Configuration of the Sample

### Configure the Acrolinx Server

For single sign-on add the following example properties to the coreserver.properties file:

```
singleSignOn.method=header
singleSignOn.genericPassword=secret
singleSignOn.usernameKey=username
singleSignOn.passwordKey=password
```

#### See:

[Setting Up Your Acrolinx Server for Single Sign-on](https://support.acrolinx.com/hc/en-us/articles/207827495-Setting-Up-Your-Acrolinx-Server-for-Single-Sign-on)

#### Note:

Make sure that you use a proper secret for the genericPassword.

### Configure the Integration

In your Acrolinx integration [config.js](https://github.com/acrolinx/acrolinx-sidebar-demo/blob/master/samples/config.js) enable SSO by setting the property: 

`enableSingleSignOn : true`

to plugin configuration add set server address to point to the proxy 

`serverAddress: '/proxySample/proxy'`

Make sure the webserver running the proxy also delivers the HTML of the integration. 

#### See:

[Acrolinx Sidebar InitParameters Interface](https://cdn.rawgit.com/acrolinx/acrolinx-sidebar-demo/master/doc/pluginDoc/interfaces/_plugin_interfaces_.initparameters.html#enablesinglesignon)

[Acrolinx Sidebar Demo](https://github.com/acrolinx/acrolinx-sidebar-demo)

### Configure the Proxy

#### Java Servlet

[Acrolinx Java Proxy Servlet](java/servlet/README.md)

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
