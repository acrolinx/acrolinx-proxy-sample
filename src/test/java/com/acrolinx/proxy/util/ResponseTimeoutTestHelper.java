/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import com.acrolinx.proxy.AcrolinxProxyHttpServlet;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.util.Collections;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

  public void verifyInteraction(String expectedExceptionMessage) throws IOException {
    verifyInteractionWithHttpServletResponse(expectedExceptionMessage);
  }

  private void setupTestEnvironment() {
    stubHttpServletRequest();

    ServletConfigUtil.stubServletConfigBase(
        servletConfig, "http://localhost:" + serverSocket.getLocalPort());
    ServletConfigUtil.stubServletConfigSocketTimeout(servletConfig, "1");
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

  private void verifyInteractionWithHttpServletResponse(String expectedExceptionMessage)
      throws IOException {
    Mockito.verify(httpServletResponse)
        .sendError(HttpURLConnection.HTTP_BAD_GATEWAY, expectedExceptionMessage);
    Mockito.verifyNoMoreInteractions(httpServletResponse);
  }
}
