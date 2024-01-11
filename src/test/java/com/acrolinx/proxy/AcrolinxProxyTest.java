/* Copyright (c) 2023 Acrolinx GmbH */
package com.acrolinx.proxy;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acrolinx.proxy.util.DotenvUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AcrolinxProxyTest {
  private static final String PROXY_PATH = "acrolinx-proxy-sample/proxy";

  private static String createCheckBody(boolean withCustomFields) {
    return "{\"content\": \"Test content\", \"document\" : {\"reference\" : \"test.txt\""
        + (withCustomFields
            ? ", \"customFields\": [ {\"key\":\"Name\", \"value\":\"A blog post\"} ]"
            : "")
        + "}}";
  }

  private static String getJsonValue(String resultResponseBody, String key) {
    String regex = key + "\":\"(.*?)\"";
    Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(resultResponseBody);

    String result = null;

    while (matcher.find()) {
      result = matcher.group(1);
    }

    return result;
  }

  private final HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
  private final HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
  private final ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
  private final StubServletOutputStream servletOutputStream = new StubServletOutputStream();

  @BeforeEach
  void beforeEach() throws IOException {
    stubRequestResponse(false);
  }

  @Test
  void testCheckAndGetScoreCardThroughProxy()
      throws IOException, ServletException, InterruptedException {
    AcrolinxProxyHttpServlet acrolinxProxyHttpServlet = createAndInitAcrolinxHttpProxyServlet();

    String postData = createCheckBody(false);
    String pollResponseBody = check(acrolinxProxyHttpServlet, postData);

    if (pollResponseBody.contains("customFieldsIncorrect")) {
      assertTrue(pollResponseBody.contains("Name"), pollResponseBody);
      postData = createCheckBody(true);
      pollResponseBody = check(acrolinxProxyHttpServlet, postData);
    }

    String scorecardUrl = getJsonValue(pollResponseBody, "Score Card\",\"link");

    stubRequestResponse(false);

    String scorecardUrlRequest =
        scorecardUrl.substring(scorecardUrl.indexOf(PROXY_PATH) + PROXY_PATH.length());

    Thread.sleep(1_000);

    String scorecardResponseBody = doGet(acrolinxProxyHttpServlet, scorecardUrlRequest);
    assertTrue(scorecardResponseBody.contains("<title>Scorecard</title>"), scorecardResponseBody);
  }

  @Test
  void testFetchingSidebarHtml() throws IOException, ServletException {
    stubHttpServletRequest("/sidebar/v14/index.html");

    final AcrolinxProxyHttpServlet acrolinxProxyHttpServlet =
        createAndInitAcrolinxHttpProxyServlet();
    acrolinxProxyHttpServlet.doGet(httpServletRequest, httpServletResponse);

    final byte[] data = servletOutputStream.byteArrayOutputStream.toByteArray();
    Assertions.assertTrue(data.length > 0);

    String responseBody = new String(data);
    assertTrue(responseBody.contains("<title>Acrolinx</title>"));
  }

  @Test
  void testSignInSSO() throws IOException, ServletException {
    setRequestData("");

    final AcrolinxProxyHttpServlet acrolinxProxyHttpServlet =
        createAndInitAcrolinxHttpProxyServlet();

    String signInResult = doPost(acrolinxProxyHttpServlet, "/api/v1/auth/sign-ins", "");

    assertTrue(signInResult.contains("ACROLINX_SSO"), signInResult);
    assertTrue(signInResult.contains("getUser"), signInResult);
    assertTrue(signInResult.contains("accessToken"), signInResult);
    assertTrue(signInResult.contains("Success"), signInResult);
  }

  @Test
  void testSimpleGet() throws IOException, ServletException {
    stubHttpServletRequest("/api/v1");

    final AcrolinxProxyHttpServlet acrolinxProxyHttpServlet =
        createAndInitAcrolinxHttpProxyServlet();
    acrolinxProxyHttpServlet.doGet(httpServletRequest, httpServletResponse);

    final byte[] data = servletOutputStream.byteArrayOutputStream.toByteArray();
    Assertions.assertTrue(data.length > 0);
  }

  @Test
  void testSsoCheckAndGetScoreCardThroughProxy()
      throws IOException, ServletException, InterruptedException {
    setRequestData("");

    final AcrolinxProxyHttpServlet acrolinxProxyHttpServlet =
        createAndInitAcrolinxHttpProxyServlet();

    String signInResult = doPost(acrolinxProxyHttpServlet, "/api/v1/auth/sign-ins", "");

    assertTrue(signInResult.contains("ACROLINX_SSO"), signInResult);
    assertTrue(signInResult.contains("accessToken"), signInResult);

    stubRequestResponse(getJsonValue(signInResult, "accessToken"));

    String postData = createCheckBody(false);
    String pollResponseBody = check(acrolinxProxyHttpServlet, postData);

    if (pollResponseBody.contains("customFieldsIncorrect")) {
      assertTrue(pollResponseBody.contains("Name"), pollResponseBody);
      postData = createCheckBody(true); // Set hard coded custom field called "Name"
      pollResponseBody = check(acrolinxProxyHttpServlet, postData);
    }

    String scorecardUrl = getJsonValue(pollResponseBody, "Score Card\",\"link");

    stubRequestResponse(false);

    String scorecardUrlRequest =
        scorecardUrl.substring(scorecardUrl.indexOf(PROXY_PATH) + PROXY_PATH.length());

    Thread.sleep(1_000);

    String scorecardResponseBody = doGet(acrolinxProxyHttpServlet, scorecardUrlRequest);
    assertTrue(scorecardResponseBody.contains("<title>Scorecard</title>"), scorecardResponseBody);
  }

  @Test
  void testSubmitCheck() throws IOException, ServletException {
    AcrolinxProxyHttpServlet acrolinxProxyHttpServlet = createAndInitAcrolinxHttpProxyServlet();

    String postData =
        "{\"content\": \"Test content\", \"document\" : {\"reference\" : \"test.txt\""
            + ", \"customFields\": [ {\"key\":\"Name\", \"value\":\"A blog post\"} ] }"
            + "}";

    stubRequestResponse(true);
    String resultResponseBody =
        doPost(acrolinxProxyHttpServlet, "/api/v1/checking/checks", postData);

    String trimmedResultResponseBody = resultResponseBody.trim();
    assertTrue(trimmedResultResponseBody.startsWith("{"), trimmedResultResponseBody);
    assertTrue(trimmedResultResponseBody.endsWith("}"), trimmedResultResponseBody);
  }

  private String check(AcrolinxProxyHttpServlet acrolinxProxyHttpServlet, String postData)
      throws IOException, InterruptedException {
    stubRequestResponse(true);
    String resultResponseBody =
        doPost(acrolinxProxyHttpServlet, "/api/v1/checking/checks", postData);

    String pollingUri = getJsonValue(resultResponseBody, "result");

    Thread.sleep(5_000);

    stubRequestResponse(true);
    String pollingUrlRequest =
        pollingUri.substring(pollingUri.indexOf(PROXY_PATH) + PROXY_PATH.length());

    return doGet(acrolinxProxyHttpServlet, pollingUrlRequest);
  }

  private AcrolinxProxyHttpServlet createAndInitAcrolinxHttpProxyServlet() throws ServletException {
    final AcrolinxProxyHttpServlet acrolinxProxyHttpServlet = new AcrolinxProxyHttpServlet();
    acrolinxProxyHttpServlet.init(servletConfig);
    return acrolinxProxyHttpServlet;
  }

  private String doGet(AcrolinxProxyHttpServlet acrolinxProxyHttpServlet, String pathString)
      throws IOException {
    stubHttpServletRequest(pathString);

    acrolinxProxyHttpServlet.doGet(httpServletRequest, httpServletResponse);
    return readBody();
  }

  private String doPost(
      AcrolinxProxyHttpServlet acrolinxProxyHttpServlet, String pathString, String postData)
      throws IOException {
    stubHttpServletRequest(pathString);

    setRequestData(postData);

    acrolinxProxyHttpServlet.doPost(httpServletRequest, httpServletResponse);
    return readBody();
  }

  private String readBody() {
    byte[] bytes = servletOutputStream.byteArrayOutputStream.toByteArray();
    Assertions.assertTrue(bytes.length > 0);

    return new String(bytes, StandardCharsets.UTF_8);
  }

  private void setRequestData(String postData) throws IOException {
    final ServletInputStream servletInputStream = new StubServletInputStream(postData);
    Mockito.when(httpServletRequest.getInputStream()).thenReturn(servletInputStream);
  }

  private void stubHttpServletRequest(String pathString) {
    Mockito.when(httpServletRequest.getRequestURL())
        .thenReturn(
            new StringBuffer("http://localhost:8080/acrolinx-proxy-sample/proxy" + pathString));
    Mockito.when(httpServletRequest.getPathInfo()).thenReturn(pathString);
  }

  private void stubRequestResponse(boolean addAuthToken) throws IOException {
    stubRequestResponse(addAuthToken, DotenvUtil.getEnvironmentVariable("ACROLINX_API_TOKEN"));
  }

  private void stubRequestResponse(boolean addAuthToken, String token) throws IOException {
    final List<String> headers = new ArrayList<>();
    headers.add("X-Acrolinx-Client");
    headers.add("X-Acrolinx-Client-Locale");
    headers.add("Content-Type");

    if (addAuthToken) {
      headers.add("X-Acrolinx-Auth");
      Mockito.when(httpServletRequest.getHeader("X-Acrolinx-Auth")).thenReturn(token);
    }

    Mockito.when(httpServletRequest.getHeaderNames()).thenReturn(Collections.enumeration(headers));
    Mockito.when(httpServletRequest.getQueryString()).thenReturn("");

    Mockito.when(httpServletRequest.getHeader("X-Acrolinx-Client"))
        .thenReturn(DotenvUtil.getEnvironmentVariable("ACROLINX_DEV_SIGNATURE"));
    Mockito.when(httpServletRequest.getHeader("X-Acrolinx-Client-Locale")).thenReturn("en-US");
    Mockito.when(httpServletRequest.getHeader("Content-Type")).thenReturn("application/json");

    Mockito.when(servletConfig.getInitParameter("acrolinxURL"))
        .thenReturn(DotenvUtil.getEnvironmentVariable("ACROLINX_URL"));
    Mockito.when(servletConfig.getInitParameter("genericToken"))
        .thenReturn(DotenvUtil.getEnvironmentVariable("ACROLINX_API_SSO_TOKEN"));
    Mockito.when(servletConfig.getInitParameter("username"))
        .thenReturn(DotenvUtil.getEnvironmentVariable("ACROLINX_API_USERNAME"));

    Mockito.when(httpServletResponse.getOutputStream()).thenReturn(servletOutputStream);
  }

  private void stubRequestResponse(String token) throws IOException {
    stubRequestResponse(true, token);
  }
}
