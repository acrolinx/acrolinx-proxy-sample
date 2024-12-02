/* Copyright (c) 2023 Acrolinx GmbH */
package com.acrolinx.proxy;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet acts as a reverse proxy. The Acrolinx plug-in uses this servlet to communicate with
 * the Acrolinx core server.
 */
public class AcrolinxProxyHttpServlet extends HttpServlet {
  // TODO: Set this path in context of your servlet's reverse proxy implementation
  public static final String PROXY_PATH = "acrolinx-proxy-sample/proxy";
  public static final String PROXY_VERSION = "2";
  public static final String USER_AGENT = "Acrolinx Proxy";
  private static final String ACROLINX_BASE_URL_HEADER = "X-Acrolinx-Base-Url";
  private static final Set<String> DISALLOWED_HEADER_NAMES =
      Set.of("connection", "content-length", "expect", "host", "upgrade");
  private static final Logger LOGGER = LoggerFactory.getLogger(AcrolinxProxyHttpServlet.class);
  private static final long serialVersionUID = 1L;

  private static void addAcrolinxBaseUrlHeader(
      HttpServletRequest httpServletRequest, Builder httpRequestBuilder) {
    // add an extra header needed for acrolinx
    String acrolinxBaseUrl = httpServletRequest.getHeader(ACROLINX_BASE_URL_HEADER);

    if (acrolinxBaseUrl == null
        || acrolinxBaseUrl.isEmpty()) { // means we never copied it or it was never there
      String requestUrlString = httpServletRequest.getRequestURL().toString();
      String baseUrl =
          requestUrlString.substring(0, requestUrlString.indexOf(PROXY_PATH) + PROXY_PATH.length());
      httpRequestBuilder.header(ACROLINX_BASE_URL_HEADER, baseUrl);
    }
  }

  private static void copyHeaders(
      HttpServletRequest httpServletRequest, Builder httpRequestBuilder) {
    Enumeration<String> headerNames = httpServletRequest.getHeaderNames();

    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      String headerValue = httpServletRequest.getHeader(headerName);

      if (isHeaderNameDisallowed(headerName)) {
        continue;
      }

      if (headerValue == null) {
        headerValue = "";
      }

      if ("cookie".equalsIgnoreCase(headerName)) {
        headerValue = filterCookies(headerValue);
      }

      httpRequestBuilder.header(headerName, headerValue);
    }
  }

  private static BodyPublisher createBodyPublisher(HttpServletRequest httpServletRequest) {
    return BodyPublishers.ofInputStream(getInputStreamSupplier(httpServletRequest));
  }

  private static HttpClient createHttpClient() {
    return HttpClient.newBuilder()
        .followRedirects(Redirect.NEVER)
        .version(Version.HTTP_1_1)
        .build();
  }

  private static String filterCookies(String headerValue) {
    return Arrays.stream(headerValue.split(";"))
        .filter(
            rawCookieNameAndValue -> rawCookieNameAndValue.toUpperCase().startsWith("X-ACROLINX-"))
        .collect(Collectors.joining(";"));
  }

  private static Supplier<InputStream> getInputStreamSupplier(
      HttpServletRequest httpServletRequest) {
    return () -> {
      try {
        return httpServletRequest.getInputStream();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    };
  }

  private static boolean isHeaderNameDisallowed(String headerName) {
    return DISALLOWED_HEADER_NAMES.contains(headerName.toLowerCase(Locale.ENGLISH));
  }

  private static void logExceptionAndSendError(
      HttpServletResponse httpServletResponse, Exception exception, int statusCode)
      throws IOException {
    LOGGER.error("", exception);
    httpServletResponse.sendError(statusCode, exception.toString());
  }

  private static void setRequestHeader(
      Builder httpRequestBuilder, String headerName, String headerValue) {
    httpRequestBuilder.setHeader(headerName, headerValue);
  }

  private static void transferResponseBodyWitAdditionalHeaders(
      HttpServletResponse httpServletResponse, HttpResponse<InputStream> httpResponse)
      throws IOException {
    HttpHeaders httpHeaders = httpResponse.headers();
    Optional<String> contentTypeHeader = httpHeaders.firstValue("Content-Type");

    if (contentTypeHeader.isPresent()) {
      httpServletResponse.setContentType(contentTypeHeader.get());
    }

    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream outputStream = httpServletResponse.getOutputStream(); ) {
      long numberOfTransferredBytes = httpResponse.body().transferTo(byteArrayOutputStream);
      httpServletResponse.setContentLength((int) numberOfTransferredBytes);

      // all response headers must be set before writing to the OutputStream
      outputStream.write(byteArrayOutputStream.toByteArray());
    }

    LOGGER.debug("Forwarded response to client");
  }

  private static void transferResponseHeaders(
      HttpServletResponse httpServletResponse, HttpHeaders httpHeaders) {
    for (Map.Entry<String, List<String>> header : httpHeaders.map().entrySet()) {
      /* The Content-Length and Content-Type headers are copied via the corresponding setter
      methods. The Transfer-Encoding header is filtered out since the HttpClient automatically decompresses the response body. */
      final String headerName = header.getKey();
      final String headerValue = header.getValue().get(0);

      if (!(headerName.equalsIgnoreCase("Content-Length")
          || headerName.equalsIgnoreCase("Content-Type")
          || headerName.equalsIgnoreCase("Transfer-Encoding"))) {
        httpServletResponse.setHeader(headerName, headerValue);
      }
    }
  }

  private static String urlEncode(String string) {
    return URLEncoder.encode(string, StandardCharsets.UTF_8);
  }

  private String acrolinxUrl;
  private String genericToken;
  private final HttpClient httpClient;
  private Duration timeoutDuration;
  private String username;

  public AcrolinxProxyHttpServlet() {
    httpClient = createHttpClient();
  }

  @Override
  public void doDelete(
      HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
      throws IOException {
    final Builder httpRequestBuilder = HttpRequest.newBuilder().DELETE();
    proxyRequest(httpServletRequest, httpServletResponse, httpRequestBuilder);
  }

  @Override
  public void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
      throws IOException {
    final Builder httpRequestBuilder = HttpRequest.newBuilder().GET();
    proxyRequest(httpServletRequest, httpServletResponse, httpRequestBuilder);
  }

  @Override
  public void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
      throws IOException {
    final Builder httpRequestBuilder =
        HttpRequest.newBuilder().POST(createBodyPublisher(httpServletRequest));
    proxyRequest(httpServletRequest, httpServletResponse, httpRequestBuilder);
  }

  @Override
  public void doPut(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
      throws IOException {
    final Builder httpRequestBuilder =
        HttpRequest.newBuilder().PUT(createBodyPublisher(httpServletRequest));
    proxyRequest(httpServletRequest, httpServletResponse, httpRequestBuilder);
  }

  @Override
  public void init() {
    // Properties can be configured by init parameters in the web.xml.
    acrolinxUrl = getInitParameterOrThrowException("acrolinxUrl").replaceAll("/$", "");
    genericToken = getInitParameterOrThrowException("genericToken");
    username = getUsernameFromApplicationSession();
    timeoutDuration = Duration.parse(getInitParameterOrDefaultValue("timeoutDuration", "PT1M"));
  }

  private void addSingleSignOnHeaders(Builder httpRequestBuilder) {
    setRequestHeader(httpRequestBuilder, "username", urlEncode(username));
    setRequestHeader(httpRequestBuilder, "password", urlEncode(genericToken));
  }

  private String getInitParameterOrDefaultValue(String name, String defaultValue) {
    String parameterValue = getInitParameter(name);
    return parameterValue == null ? defaultValue : parameterValue;
  }

  private String getInitParameterOrThrowException(final String name) {
    String parameterValue = getInitParameter(name);

    if (parameterValue == null || parameterValue.isBlank()) {
      throw new IllegalArgumentException("Missing parameter: " + name);
    }

    return parameterValue;
  }

  private URI getTargetUri(final HttpServletRequest httpServletRequest) {
    final String queryPart =
        httpServletRequest.getQueryString() != null
            ? "?" + httpServletRequest.getQueryString()
            : "";
    final String uriString = acrolinxUrl + httpServletRequest.getPathInfo() + queryPart;
    return URI.create(uriString);
  }

  private String getUsernameFromApplicationSession() {
    return getInitParameterOrThrowException("username");
    // TODO: Set user name from the current applications session. This is just an
    // example code the user name comes from web.xml.
  }

  private void modifyRequest(HttpServletRequest httpServletRequest, Builder httpRequestBuilder) {
    httpRequestBuilder.uri(getTargetUri(httpServletRequest));
    setRequestHeader(httpRequestBuilder, "User-Agent", USER_AGENT);
    setRequestHeader(httpRequestBuilder, "X-Acrolinx-Integration-Proxy-Version", PROXY_VERSION);
  }

  private void proxyRequest(
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse,
      Builder httpRequestBuilder)
      throws IOException {
    copyHeaders(httpServletRequest, httpRequestBuilder);

    addAcrolinxBaseUrlHeader(httpServletRequest, httpRequestBuilder);

    modifyRequest(httpServletRequest, httpRequestBuilder);

    httpRequestBuilder.timeout(timeoutDuration);

    addSingleSignOnHeaders(httpRequestBuilder);

    LOGGER.info("Performing HTTP request: {}", httpRequestBuilder);

    try {
      HttpResponse<InputStream> httpResponse =
          httpClient.send(
              httpRequestBuilder.build(), responseInfo -> BodySubscribers.ofInputStream());
      int status = httpResponse.statusCode();
      LOGGER.debug("Response received: {}", status);

      httpServletResponse.setStatus(status);

      transferResponseHeaders(httpServletResponse, httpResponse.headers());

      transferResponseBodyWitAdditionalHeaders(httpServletResponse, httpResponse);
    } catch (ConnectException | HttpTimeoutException e) {
      logExceptionAndSendError(httpServletResponse, e, HttpURLConnection.HTTP_BAD_GATEWAY);
    } catch (IOException e) {
      logExceptionAndSendError(httpServletResponse, e, HttpURLConnection.HTTP_UNAVAILABLE);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
