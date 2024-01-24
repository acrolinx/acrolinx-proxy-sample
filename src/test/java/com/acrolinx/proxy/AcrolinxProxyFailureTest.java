/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy;

import com.acrolinx.proxy.util.CommunicationFailureTestHelper;
import com.acrolinx.proxy.util.ConnectionFailureTestHelper;
import com.acrolinx.proxy.util.HttpMethod;
import com.acrolinx.proxy.util.InvalidResponseTestHelper;
import com.acrolinx.proxy.util.ResponseTimeoutTestHelper;
import com.acrolinx.proxy.util.WireMockServerWrapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.net.ServerSocket;
import javax.servlet.ServletException;
import org.junit.jupiter.api.Test;

class AcrolinxProxyFailureTest {
  /**
   * The first byte of the first octet of a Class A address cannot be 0 because 0 and 127 are
   * reserved. That makes the address below an invalid address. Source: <a
   * href="https://www.ciscopress.com/articles/article.asp?p=330807&seqNum=2#:~:text=Any%20address%20that%20starts%20with,to%20represent%20the%20network%20number">www.ciscopress.com</a>.
   */
  private static final String UNRESOLVABLE_URL_STRING = "http://0.42.42.42:80";

  private static void verifyConnectionFailure(HttpMethod httpMethod, String acrolinxUrlString)
      throws IOException, ServletException {
    ConnectionFailureTestHelper connectionFailureTestHelper =
        ConnectionFailureTestHelper.createAndSetUpTestEnvironment(acrolinxUrlString);

    httpMethod.callAcrolinxProxyMethod(
        connectionFailureTestHelper.getHttpServletRequest(),
        connectionFailureTestHelper.getHttpServletResponse(),
        connectionFailureTestHelper.getServletConfig());

    connectionFailureTestHelper.verifyInteraction();
  }

  private static void verifyFailedNetworkCommunication(
      String urlString, String expectedExceptionMessage) throws Exception {
    try (WireMockServerWrapper wireMockServerWrapper =
        WireMockServerWrapper.startOnRandomHttpsPort()) {
      final WireMockServer wireMockServer = wireMockServerWrapper.getWireMockServer();

      CommunicationFailureTestHelper communicationFailureTestHelper =
          CommunicationFailureTestHelper.createAndSetUpTestEnvironment(
              wireMockServer, urlString + wireMockServer.httpsPort());

      HttpMethod.GET.callAcrolinxProxyMethod(
          communicationFailureTestHelper.getHttpServletRequest(),
          communicationFailureTestHelper.getHttpServletResponse(),
          communicationFailureTestHelper.getServletConfig());

      communicationFailureTestHelper.verifyInteraction(expectedExceptionMessage);
    }
  }

  private static void verifyProtocolInvalidResponseError()
      throws IOException, InterruptedException, ServletException {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      InvalidResponseTestHelper invalidResponseTestHelper =
          InvalidResponseTestHelper.createAndSetUpTestEnvironment(serverSocket);

      HttpMethod.GET.callAcrolinxProxyMethod(
          invalidResponseTestHelper.getHttpServletRequest(),
          invalidResponseTestHelper.getHttpServletResponse(),
          invalidResponseTestHelper.getServletConfig());

      invalidResponseTestHelper.verifyInteraction();
    }
  }

  private static void verifyResponseTimeout() throws IOException, ServletException {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      ResponseTimeoutTestHelper responseTimeoutTestHelper =
          ResponseTimeoutTestHelper.createAndSetUpTestEnvironment(serverSocket);

      HttpMethod.GET.callAcrolinxProxyMethod(
          responseTimeoutTestHelper.getHttpServletRequest(),
          responseTimeoutTestHelper.getHttpServletResponse(),
          responseTimeoutTestHelper.getServletConfig());

      responseTimeoutTestHelper.verifyInteraction(
          "java.net.SocketTimeoutException: Read timed out");
    }
  }

  @Test
  void connectionTimeoutDeleteTest() throws IOException, ServletException {
    verifyConnectionFailure(HttpMethod.DELETE, UNRESOLVABLE_URL_STRING);
  }

  @Test
  void connectionTimeoutGetTest() throws IOException, ServletException {
    verifyConnectionFailure(HttpMethod.GET, UNRESOLVABLE_URL_STRING);
  }

  @Test
  void connectionTimeoutPostTest() throws IOException, ServletException {
    verifyConnectionFailure(HttpMethod.POST, UNRESOLVABLE_URL_STRING);
  }

  @Test
  void connectionTimeoutPutTest() throws IOException, ServletException {
    verifyConnectionFailure(HttpMethod.PUT, UNRESOLVABLE_URL_STRING);
  }

  @Test
  void failedSslVerificationTest() throws Exception {
    verifyFailedNetworkCommunication(
        "https://localhost:",
        "javax.net.ssl.SSLHandshakeException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target");
  }

  @Test
  void httpToHttpsTest() throws Exception {
    verifyFailedNetworkCommunication(
        "http://localhost:", "org.apache.http.client.ClientProtocolException");
  }

  @Test
  void invalidResponseTest() throws IOException, InterruptedException, ServletException {
    verifyProtocolInvalidResponseError();
  }

  @Test
  void responseTimeoutTest() throws IOException, ServletException {
    verifyResponseTimeout();
  }

  @Test
  void unknownHostTest() throws Exception {
    verifyConnectionFailure(HttpMethod.GET, "http://something.invalid");
  }
}
