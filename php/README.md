# Acrolinx Single Sign-On Proxy PHP Sample

Demo code for implementing a proxy in a single sign-on environment in PHP.

## Prerequisites

Make sure that you have configured your:
* [Acrolinx Server for SSO](/README.md#configure-the-acrolinx-server), and the 
* [Integration](/README.md#acrolinx-proxy-sample#configure-the-integration).

## Configure Acrolinx PHP Example

1. Set all values marked with a TODO-comment in [proxy.php](proxy.php).
2. Copy [proxy.php](proxy.php) to `/acrolinx/proxy.php` on your php-enabled webserver.


To test the connection to Acrolinx, test one of the rest calls.
If you open `http://<WEBCONTAINER>/acrolinx/proxy.php/iq/services/rest/registry/knownServiceNames`, like [http://localhost:8080/acrolinx/proxy.php/iq/services/rest/registry/knownServiceNames](http://localhost:8080/acrolinx/proxy.php/iq/services/rest/registry/knownServiceNames) you should see a result like:

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