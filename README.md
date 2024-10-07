# Acrolinx single sign-on proxy Java servlet sample

This sample provides demo code for implementing a proxy in a single sign-on environment in Java.

## Prerequisites

Contact [Acrolinx Support](https://github.com/acrolinx/acrolinx-coding-guidance/blob/main/topics/support.md)
for consulting and getting your integration certified.
  
Acrolinx has different SDKs and examples for developing integrations.

Before you start developing your own integration, you might benefit from reading:

* [Build with Acrolinx](https://support.acrolinx.com/hc/en-us/categories/10209837818770-Build-With-Acrolinx), and
* the [How to integrate with Acrolinx](https://github.com/acrolinx/acrolinx-coding-guidance).

## Overview

See: [Architecture Diagrams](https://support.acrolinx.com/hc/en-us/articles/10210859500818-Architecture-Diagrams)

## Configure the sample

### Configure Acrolinx

To enable the proxy, contact [Acrolinx Support](https://github.com/acrolinx/acrolinx-coding-guidance/blob/main/topics/support.md) and request the generic token for your Acrolinx instance.

### Configure the Sidebar integration

In the [`config.js`](https://github.com/acrolinx/acrolinx-sidebar-demo/blob/main/samples/config.js) of your Acrolinx Integration,
set the Acrolinx URL to point to the relative proxy path as follows:

```javascript
serverAddress: '/acrolinx-proxy-sample/proxy'
```

Make sure:

* the web server that runs the proxy delivers the HTML from the integration.
* the relative proxy path points to the correct location of your proxy.

See also:

* [Acrolinx Sidebar demo](https://github.com/acrolinx/acrolinx-sidebar-demo)

### Configure the proxy

#### Java servlet

Configure the Acrolinx URL, username, and generic token in the [web.xml](src/main/webapp/WEB-INF/web.xml) file.
The parameter names are `acrolinxUrl`, `username`, and `genericToken`.

### Test the sample proxy

To run a sample
```bash
mvn jetty:run-war
```

This sample demonstrates sending the REST call `api/v1/auth/sign-ins` for Acrolinx authentication.

If you open `http://<WEBCONTAINER>/`, like [http://localhost:8080/](http://localhost:8080/acrolinx-proxy-sample/),
then press the sign-in button, it will either return the interactive URL to the complete sign-in
or a success message with details.

### Security

Implement the proxy in a [secure way](https://github.com/acrolinx/acrolinx-coding-guidance/blob/main/topics/security-safety.md#security).

Make sure that:

* The proxy layer checks the entire authentication.
* The system that you integrate with already authenticated the username.
* The proxy adds the username header and the SSO token header.
* You keep the SSO token secret between the system's backend and Acrolinx.
* It's impossible to fake a request to the proxy and obtain an authentication token for a user other than the authenticated user.

#### Pitfalls of a TLS connection

Acrolinx instances have the latest security standards. 
Standard and HTTP clients might not be able to connect. 
Make sure that you configure your VM, operating system, and backend to allow connections with modern [TLS versions](https://en.wikipedia.org/wiki/Transport_Layer_Security).

Test with an appropriate configuration before rollout.
