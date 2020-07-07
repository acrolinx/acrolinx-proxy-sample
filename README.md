# Acrolinx Single Sign-On Proxy Sample

Demo code for implementing a proxy in a single sign-on environment for [Acrolinx](https://www.acrolinx.com/) Sidebar integrations.

## Prerequisites

Please contact [Acrolinx SDK support](https://github.com/acrolinx/acrolinx-coding-guidance/blob/master/topics/sdk-support.md)
for consulting and getting your integration certified.
This sample works with a test license on an internal Acrolinx URL.
This license is only meant for demonstration and developing purposes.
Once you finished your integration, you'll have to get a license for your integration from Acrolinx.
  
Acrolinx offers different other SDKs, and examples for developing integrations.

Before you start developing your own integration, you might benefit from looking into:

* [Getting Started with Custom Integrations](https://docs.acrolinx.com/customintegrations), and
* the [Guidance for the Development of Acrolinx Integrations](https://github.com/acrolinx/acrolinx-coding-guidance).

## Overview

![Architecture Diagram](images/sidebarArchitectureDiagram14.3.1.png)

## Configuration of the Sample

### Configure the Acrolinx Platform

To enable single sign-on, add the following example properties to the `coreserver.properties` file:

```properties
singleSignOn.method=header
singleSignOn.genericPassword=secret
singleSignOn.usernameKey=username
singleSignOn.passwordKey=password
```

See: [Setting Up Your Core Platform for single sign-on](https://docs.acrolinx.com/display/CP/Set+Up+Acrolinx+for+Single+Sign-on).

*Note: Make sure that you use a proper secret for the `genericPassword`.*

### Configure the Integration

In the [`config.js`](https://github.com/acrolinx/acrolinx-sidebar-demo/blob/master/samples/config.js) of your Acrolinx Integration,
set the Acrolinx URL to point to the relative proxy path as follows:

```javascript
serverAddress: '/proxySample/proxy'
```

Make sure:

* the webserver running the proxy delivers the HTML of the integration.
* the relative proxy path points to the correct location of your proxy.

See also:

* [Acrolinx Sidebar Demo](https://github.com/acrolinx/acrolinx-sidebar-demo)

### Configure the Proxy

#### Java Servlet

[Acrolinx Java Proxy Servlet](java/servlet/README.md)

#### PHP

[Acrolinx PHP Proxy](php/README.md)

## License

Copyright 2015-present Acrolinx GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at:

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

For more information visit: [https://www.acrolinx.com](https://www.acrolinx.com)
