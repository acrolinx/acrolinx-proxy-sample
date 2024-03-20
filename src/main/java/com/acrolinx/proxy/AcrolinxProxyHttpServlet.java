/* Copyright (c) 2023 Acrolinx GmbH */
package com.acrolinx.proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.stream.Collectors;
import javax.net.ssl.SSLHandshakeException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet acts as a reverse proxy. The Acrolinx plug-in uses this servlet to communicate with
 * the Acrolinx core server.
 */
public class AcrolinxProxyHttpServlet extends HttpServlet {
  // TODO: Set this path in context of your servlet's reverse proxy implementation
  public static final String PROXY_PATH = "acrolinx-proxy-sample/proxy";
  private static final String ACROLINX_BASE_URL_HEADER = "X-Acrolinx-Base-Url";
  private static final Logger logger = LoggerFactory.getLogger(AcrolinxProxyHttpServlet.class);
  private static final long serialVersionUID = 1L;

  private static void copyHeaders(
      final HttpServletRequest httpServletRequest, final HttpRequestBase httpRequestBase) {
    final Enumeration<String> headerNames = httpServletRequest.getHeaderNames();

    while (headerNames.hasMoreElements()) {
      final String headerName = headerNames.nextElement();
      String headerValue = httpServletRequest.getHeader(headerName);

      if (headerName.equalsIgnoreCase("cookie")) {
        headerValue = filterCookies(headerValue);
      }

      httpRequestBase.addHeader(headerName, headerValue);
    }
  }

  private static CloseableHttpClient createHttpClient() {
    return HttpClientBuilder.create()
        .setConnectionManager(new PoolingHttpClientConnectionManager())
        .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
        .disableRedirectHandling()
        .disableAutomaticRetries()
        .addInterceptorFirst(ContentLengthHeaderRemover.INSTANCE)
        .useSystemProperties()
        .build();
  }

  private static String filterCookies(String headerValue) {
    return Arrays.stream(headerValue.split(";"))
        .filter(
            rawCookieNameAndValue -> rawCookieNameAndValue.toUpperCase().startsWith("X-ACROLINX-"))
        .collect(Collectors.joining(";"));
  }

  private static void logExceptionAndSendError(
      HttpServletResponse httpServletResponse, Exception exception, int statusCode)
      throws IOException {
    logger.error("", exception);
    httpServletResponse.sendError(statusCode, exception.toString());
  }

  private static void setRequestHeader(
      final HttpRequestBase httpRequestBase, final String headerName, final String headerValue) {
    httpRequestBase.removeHeaders(headerName);
    httpRequestBase.setHeader(headerName, headerValue);
  }

  private static void transferResponseBodyWitAdditionalHeaders(
      HttpServletResponse httpServletResponse, HttpEntity httpEntity) throws IOException {
    try (InputStream inputStream = httpEntity.getContent();
        OutputStream outputStream = httpServletResponse.getOutputStream()) {
      Header contentTypeHeader = httpEntity.getContentType();

      if (contentTypeHeader != null) {
        httpServletResponse.setContentType(contentTypeHeader.getValue());
      }

      try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
        long numberOfTransferredBytes = inputStream.transferTo(byteArrayOutputStream);
        httpServletResponse.setContentLength((int) numberOfTransferredBytes);

        // all response headers must be set before writing to the OutputStream
        outputStream.write(byteArrayOutputStream.toByteArray());
      }

      logger.debug("Forwarded response to client");
    }
  }

  private static void transferResponseHeaders(
      HttpServletResponse httpServletResponse, CloseableHttpResponse httpResponse) {
    for (Header header : httpResponse.getAllHeaders()) {
      final String headerName = header.getName();

      if (!(headerName.startsWith("Transfer-Encoding")
          || headerName.startsWith("Content-Length")
          || headerName.startsWith("Content-Type")
          || headerName.equalsIgnoreCase("Transfer-Encoding"))) {
        httpServletResponse.setHeader(headerName, header.getValue());
      }
    }
  }

  private String acrolinxUrl;
  private final CloseableHttpClient closeableHttpClient;
  private int connectTimeoutInMillis;
  private String genericToken;
  private int socketTimeoutInMillis;
  private String username;

  public AcrolinxProxyHttpServlet() {
    closeableHttpClient = createHttpClient();
  }

  @Override
  public void doDelete(
      final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
      throws IOException {
    final HttpDelete httpDelete = new HttpDelete();
    logger.debug("Processing delete");
    proxyRequest(httpServletRequest, httpServletResponse, httpDelete);
  }

  @Override
  public void doGet(
      final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
      throws IOException {
    final HttpGet httpGet = new HttpGet();
    logger.debug("Processing get");
    proxyRequest(httpServletRequest, httpServletResponse, httpGet);
  }

  @Override
  public void doPost(
      final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
      throws IOException {
    final HttpPost httpPost = new HttpPost();
    httpPost.setEntity(new InputStreamEntity(httpServletRequest.getInputStream()));
    logger.debug("Processing post");
    proxyRequest(httpServletRequest, httpServletResponse, httpPost);
  }

  @Override
  public void doPut(
      final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
      throws IOException {
    final HttpPut httpPut = new HttpPut();
    httpPut.setEntity(new InputStreamEntity(httpServletRequest.getInputStream()));
    logger.debug("Processing put");
    proxyRequest(httpServletRequest, httpServletResponse, httpPut);
  }

  @Override
  public void init() {
    // Properties can be configured by init parameters in the web.xml.
    acrolinxUrl =
        getInitParameterOrDefaultValue("acrolinxUrl", "http://localhost:8031/")
            .replaceAll("/$", "");
    genericToken = getInitParameterOrDefaultValue("genericToken", "secret");
    username = getUsernameFromApplicationSession();
    connectTimeoutInMillis =
        Integer.parseInt(getInitParameterOrDefaultValue("connectTimeoutInMillis", "-1"));
    socketTimeoutInMillis =
        Integer.parseInt(getInitParameterOrDefaultValue("socketTimeoutInMillis", "-1"));
  }

  private void addSingleSignOnHeaders(final HttpRequestBase httpRequestBase) {
    setRequestHeader(httpRequestBase, "username", username);
    setRequestHeader(httpRequestBase, "password", genericToken);
  }

  private void configureTimeouts(HttpRequestBase httpRequestBase) {
    httpRequestBase.setConfig(
        RequestConfig.custom()
            .setConnectTimeout(connectTimeoutInMillis)
            .setSocketTimeout(socketTimeoutInMillis)
            .build());
  }

  private String getInitParameterOrDefaultValue(final String name, final String defaultValue) {
    String parameterValue = getInitParameter(name);
    return parameterValue != null ? parameterValue : defaultValue;
  }

  private URI getTargetUri(final HttpServletRequest httpServletRequest) throws IOException {
    final String queryPart =
        httpServletRequest.getQueryString() != null
            ? "?" + httpServletRequest.getQueryString()
            : "";
    final String uriString = acrolinxUrl + httpServletRequest.getPathInfo() + queryPart;

    try {
      URI targetUri = new URI(uriString);
      logger.debug("Request URI: {}", targetUri);
      return targetUri;
    } catch (final URISyntaxException e) {
      throw new IOException(e);
    }
  }

  private String getUsernameFromApplicationSession() {
    return getInitParameterOrDefaultValue("username", "username");
    // TODO: Set user name from the current applications session. This is just an
    // example code the user name comes from web.xml.
  }

  private void modifyRequest(
      final HttpServletRequest httpServletRequest, final HttpRequestBase httpRequestBase)
      throws IOException {
    final URI targetUri = getTargetUri(httpServletRequest);
    httpRequestBase.setURI(targetUri);
    setRequestHeader(httpRequestBase, "User-Agent", "Acrolinx Proxy");
    setRequestHeader(httpRequestBase, "Host", targetUri.getHost());
    setRequestHeader(httpRequestBase, "X-Acrolinx-Integration-Proxy-Version", "2");

    // add an extra header which is needed for acrolinx to support client's reverse proxy
    String acrolinxBaseUrl = httpServletRequest.getHeader(ACROLINX_BASE_URL_HEADER);

    if (acrolinxBaseUrl == null
        || acrolinxBaseUrl.isEmpty()) { // means we never copied it or it was never there
      String requestUrlString = httpServletRequest.getRequestURL().toString();
      String baseUrl =
          requestUrlString.substring(0, requestUrlString.indexOf(PROXY_PATH) + PROXY_PATH.length());
      httpRequestBase.setHeader(ACROLINX_BASE_URL_HEADER, baseUrl);
    }
  }

  private void proxyRequest(
      final HttpServletRequest httpServletRequest,
      final HttpServletResponse httpServletResponse,
      final HttpRequestBase httpRequestBase)
      throws IOException {
    copyHeaders(httpServletRequest, httpRequestBase);

    modifyRequest(httpServletRequest, httpRequestBase);

    configureTimeouts(httpRequestBase);

    // TODO: Make sure not to call the following line in case a user is not
    // authenticated to the application.
    addSingleSignOnHeaders(httpRequestBase);

    logger.debug("Sending request to Acrolinx");

    try (CloseableHttpResponse httpResponse = closeableHttpClient.execute(httpRequestBase)) {
      final int status = httpResponse.getStatusLine().getStatusCode();
      logger.debug("Response received: {}", status);
      httpServletResponse.setStatus(status);

      transferResponseHeaders(httpServletResponse, httpResponse);

      HttpEntity httpEntity = httpResponse.getEntity();

      if (httpEntity != null) {
        transferResponseBodyWitAdditionalHeaders(httpServletResponse, httpEntity);
      }
    } catch (ConnectTimeoutException
        | SocketTimeoutException
        | SocketException
        | UnknownHostException e) {
      logExceptionAndSendError(httpServletResponse, e, HttpURLConnection.HTTP_BAD_GATEWAY);
    } catch (SSLHandshakeException | ClientProtocolException e) {
      logExceptionAndSendError(httpServletResponse, e, HttpURLConnection.HTTP_UNAVAILABLE);
    } finally {
      httpRequestBase.releaseConnection();
    }
  }
}
