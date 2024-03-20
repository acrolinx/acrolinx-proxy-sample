/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import com.acrolinx.proxy.AcrolinxProxyHttpServlet;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;
import javax.servlet.ServletConfig;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class ConnectionFailureTestHelper {
  private static final String CHECK_URL = "/api/v1/checking/checks";

  public static ConnectionFailureTestHelper createAndSetUpTestEnvironment(String acrolinxUrlString)
      throws IOException {
    ConnectionFailureTestHelper connectionFailureTestHelper = new ConnectionFailureTestHelper();
    connectionFailureTestHelper.setUpTestEnvironment(acrolinxUrlString);
    return connectionFailureTestHelper;
  }

  private final HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
  private final HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
  private final ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
  private final ServletInputStream servletInputStream = Mockito.mock(ServletInputStream.class);

  private ConnectionFailureTestHelper() {}

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

    verifyInteractionWithHttpServletRequest();

    Mockito.verifyNoInteractions(servletInputStream);
  }

  private void setUpTestEnvironment(String acrolinxUrlString) throws IOException {
    stubHttpServletRequest(acrolinxUrlString);

    ServletConfigUtil.stubServletConfigBase(servletConfig, acrolinxUrlString);
    ServletConfigUtil.stubServletConfigConnectTimeout(servletConfig, "1");
  }

  private void stubHttpServletRequest(String acrolinxUrlString) throws IOException {
    Mockito.when(httpServletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    Mockito.when(httpServletRequest.getRequestURL())
        .thenReturn(
            new StringBuffer(acrolinxUrlString + '/' + AcrolinxProxyHttpServlet.PROXY_PATH));
    Mockito.when(httpServletRequest.getInputStream()).thenReturn(servletInputStream);
    Mockito.when(httpServletRequest.getQueryString()).thenReturn("");
    Mockito.when(httpServletRequest.getPathInfo()).thenReturn(CHECK_URL);
  }

  private void verifyInteractionWithHttpServletRequest() throws IOException {
    Mockito.verify(httpServletRequest).getHeaderNames();
    Mockito.verify(httpServletRequest).getHeader("X-Acrolinx-Base-Url");
    Mockito.verify(httpServletRequest).getRequestURL();
    Mockito.verify(httpServletRequest, Mockito.atMost(2)).getQueryString();
    Mockito.verify(httpServletRequest, Mockito.atMostOnce()).getInputStream();
    Mockito.verify(httpServletRequest).getPathInfo();
    Mockito.verifyNoMoreInteractions(httpServletRequest);
  }

  private void verifyInteractionWithHttpServletResponse() throws IOException {
    Mockito.verify(httpServletResponse)
        .sendError(
            ArgumentMatchers.eq(HttpURLConnection.HTTP_BAD_GATEWAY), ArgumentMatchers.anyString());
    Mockito.verifyNoMoreInteractions(httpServletResponse);
  }
}
