/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import com.acrolinx.proxy.AcrolinxProxyHttpServlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Collections;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class ResponseTimeoutTestHelper {
  private static final String CHECK_URL = "/api/v1/checking/checks";

  public static ResponseTimeoutTestHelper createAndSetUpTestEnvironment(ServerSocket serverSocket) {
    ResponseTimeoutTestHelper responseTimeoutTestHelper =
        new ResponseTimeoutTestHelper(serverSocket);
    responseTimeoutTestHelper.setupTestEnvironment();
    return responseTimeoutTestHelper;
  }

  private final HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
  private final HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
  private final ServerSocket serverSocket;
  private final ServletConfig servletConfig = Mockito.mock(ServletConfig.class);

  private ResponseTimeoutTestHelper(ServerSocket serverSocket) {
    this.serverSocket = serverSocket;
  }

  public HttpServletRequest getHttpServletRequest() {
    return httpServletRequest;
  }

  public HttpServletResponse getHttpServletResponse() {
    return httpServletResponse;
  }

  public ServletConfig getServletConfig() {
    return servletConfig;
  }

  public void verifyInteraction() throws IOException {
    verifyInteractionWithHttpServletResponse();
  }

  private void setupTestEnvironment() {
    stubHttpServletRequest();

    ServletConfigUtil.stubServletConfigBase(
        servletConfig, "http://localhost:" + serverSocket.getLocalPort());
    ServletConfigUtil.stubServletConfigTimeout(servletConfig, Duration.ofMillis(1));
  }

  private void stubHttpServletRequest() {
    Mockito.when(httpServletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    Mockito.when(httpServletRequest.getRequestURL())
        .thenReturn(
            new StringBuffer(
                "http://localhost:"
                    + serverSocket.getLocalPort()
                    + '/'
                    + AcrolinxProxyHttpServlet.PROXY_PATH));
    Mockito.when(httpServletRequest.getQueryString()).thenReturn("");
    Mockito.when(httpServletRequest.getPathInfo()).thenReturn(CHECK_URL);
  }

  private void verifyInteractionWithHttpServletResponse() throws IOException {
    Mockito.verify(httpServletResponse)
        .sendError(
            ArgumentMatchers.eq(HttpURLConnection.HTTP_BAD_GATEWAY),
            AdditionalMatchers.or(
                ArgumentMatchers.eq(
                    "java.net.http.HttpConnectTimeoutException: HTTP connect timed out"),
                ArgumentMatchers.eq("java.net.http.HttpTimeoutException: request timed out")));
    Mockito.verifyNoMoreInteractions(httpServletResponse);
  }
}
