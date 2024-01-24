/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import com.acrolinx.proxy.AcrolinxProxyHttpServlet;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

public class CommunicationFailureTestHelper {
  public static CommunicationFailureTestHelper createAndSetUpTestEnvironment(
      WireMockServer wireMockServer, String acrolinxUrlString) {
    CommunicationFailureTestHelper communicationFailureTestHelper =
        new CommunicationFailureTestHelper(wireMockServer);
    communicationFailureTestHelper.setUpTestEnvironment(acrolinxUrlString);
    return communicationFailureTestHelper;
  }

  private final HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
  private final HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
  private final ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
  private final WireMockServer wireMockServer;

  private CommunicationFailureTestHelper(WireMockServer wireMockServer) {
    this.wireMockServer = wireMockServer;
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
    verifyInteractionWithWireMock();
    verifyInteractionWithHttpServletResponse(expectedExceptionMessage);
  }

  private void setUpTestEnvironment(String acrolinxUrlString) {
    stubHttpServletRequest(acrolinxUrlString);

    ServletConfigUtil.stubServletConfigBase(servletConfig, acrolinxUrlString);
  }

  private void stubHttpServletRequest(String acrolinxUrlString) {
    Mockito.when(httpServletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    Mockito.when(httpServletRequest.getRequestURL())
        .thenReturn(
            new StringBuffer(acrolinxUrlString + '/' + AcrolinxProxyHttpServlet.PROXY_PATH));
    Mockito.when(httpServletRequest.getQueryString()).thenReturn("");
    Mockito.when(httpServletRequest.getPathInfo())
        .thenReturn(AcrolinxProxyTestCommonConstants.CHECK_URL);
  }

  private void verifyInteractionWithHttpServletResponse(String expectedExceptionMessage)
      throws IOException {
    Mockito.verify(httpServletResponse)
        .sendError(HttpURLConnection.HTTP_UNAVAILABLE, expectedExceptionMessage);
    Mockito.verifyNoMoreInteractions(httpServletResponse);
  }

  private void verifyInteractionWithWireMock() {
    wireMockServer.verify(0, RequestPatternBuilder.allRequests());
    Assertions.assertEquals(Collections.emptyList(), wireMockServer.findAllUnmatchedRequests());
  }
}
