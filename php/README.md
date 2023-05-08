# Acrolinx Single Sign-On Proxy PHP Sample

Demo code for implementing a proxy in a single sign-on environment in PHP.

## Prerequisites

Make sure that you've configured your:

* [Core Platform for SSO](/README.md#configure-the-acrolinx-server), and the
* [Integration](/README.md#acrolinx-proxy-sample#configure-the-integration).

## Configure Acrolinx PHP Example

1. Set all values marked with a TODO-comment in [proxy.php](proxy.php).
2. Copy [proxy.php](proxy.php) to `/acrolinx/proxy.php` on your php-enabled webserver.

### Test the Proxy

If you open `http://<WEBCONTAINER>/acrolinx/proxy.php/api/v1`, like [http://localhost:8080/acrolinx/proxy.php/api/v1](http://localhost:8080/acrolinx/proxy.php/api/v1).
You should see a result like:

```json
{"links":{"signIn":"http://localhost:8888/acrolinx/proxy.php/api/v1/auth/sign-ins"},"data":{"server":{"name":"Acrolinx Core Platform","version":"2020.10.31158"},"locales":["en","fr","de","ja","pt","sv","zh"]}}
```

### Test the Proxy Authentication

Test if the authenticate call returns a username and token.

If you perform an http `post` to `http://<WEBCONTAINER>/acrolinx/proxy.php/sso/v1/auth/sign-ins` like:

```bash
curl -X POST 'http://localhost:8888/acrolinx/proxy.php/api/v1/auth/sign-ins' \
  -H 'X-Acrolinx-Client: SW50ZWdyYXRpb25EZXZlbG9wbWVudERlbW9Pbmx5; 1.0' \
  -H 'Content-Type: application/json'
```

You should see a result like:

```json
{"links":{"getUser":"http://localhost:8888/acrolinx/proxy.php/api/v1/user/123","updateUser":"http://localhost:8888/acrolinx/proxy.php/api/v1/user/123"},"data":{"accessToken":"abc.def.ghi","user":{"id":"123","username":"testUser"},"authorizedUsing":"ACROLINX_SSO","state":"Success","doNotStoreAccessToken":"false"},"addons":[]}}}
```

*Note:* Make sure to implement the proxy in a [secure way](https://github.com/acrolinx/acrolinx-coding-guidance/blob/main/topics/security-safety.md#security).
