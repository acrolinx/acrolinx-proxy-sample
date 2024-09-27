/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy;

import com.acrolinx.proxy.util.ChunkedResponseTestHelper;
import com.acrolinx.proxy.util.HttpMethod;
import com.acrolinx.proxy.util.OkResponseTestHelper;
import com.acrolinx.proxy.util.WireMockServerWrapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AcrolinxProxySuccessTest {
  private static void callAcrolinxProxyMethod(
      HttpMethod httpMethod, OkResponseTestHelper okResponseTestHelper)
      throws IOException, ServletException {
    httpMethod.callAcrolinxProxyMethod(
        okResponseTestHelper.getHttpServletRequest(),
        okResponseTestHelper.getHttpServletResponse(),
        okResponseTestHelper.getServletConfig());
  }

  private static OkResponseTestHelper createOkResponseTestHelper(
      HttpMethod httpMethod,
      Optional<String> acrolinxBaseUrlHeaderValue,
      WireMockServer wireMockServer,
      boolean addContentTypeResponseHeader)
      throws IOException {
    if (addContentTypeResponseHeader) {
      return OkResponseTestHelper.createAndSetUpTestEnvironmentWithContentTypeResponseHeader(
          wireMockServer, acrolinxBaseUrlHeaderValue, httpMethod);
    }

    return OkResponseTestHelper.createAndSetUpTestEnvironmentWithoutContentTypeResponseHeader(
        wireMockServer, acrolinxBaseUrlHeaderValue, httpMethod);
  }

  private static void verifyChunkedHttpResponse()
      throws IOException, InterruptedException, ServletException {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      ChunkedResponseTestHelper chunkedResponseTestHelper =
          ChunkedResponseTestHelper.createAndSetUpTestEnvironment(serverSocket);

      HttpMethod.GET.callAcrolinxProxyMethod(
          chunkedResponseTestHelper.getHttpServletRequest(),
          chunkedResponseTestHelper.getHttpServletResponse(),
          chunkedResponseTestHelper.getServletConfig());

      chunkedResponseTestHelper.verifyInteraction();
    }
  }

  private static void verifyOkResponseWithContentTypeHeader(
      HttpMethod httpMethod, Optional<String> acrolinxBaseUrlHeaderValue)
      throws IOException, ServletException {
    try (WireMockServerWrapper wireMockServerWrapper =
        WireMockServerWrapper.startOnRandomHttpPort()) {
      final WireMockServer wireMockServer = wireMockServerWrapper.getWireMockServer();
      OkResponseTestHelper okResponseTestHelper =
          createOkResponseTestHelper(httpMethod, acrolinxBaseUrlHeaderValue, wireMockServer, true);
      callAcrolinxProxyMethod(httpMethod, okResponseTestHelper);
      okResponseTestHelper.verifyInteractionWithContentTypeHeader();
    }
  }

  private static void verifyOkResponseWithoutContentTypeHeader(
      HttpMethod httpMethod, Optional<String> acrolinxBaseUrlHeaderValue)
      throws IOException, ServletException {
    try (WireMockServerWrapper wireMockServerWrapper =
        WireMockServerWrapper.startOnRandomHttpPort()) {
      final WireMockServer wireMockServer = wireMockServerWrapper.getWireMockServer();
      OkResponseTestHelper okResponseTestHelper =
          createOkResponseTestHelper(httpMethod, acrolinxBaseUrlHeaderValue, wireMockServer, false);
      callAcrolinxProxyMethod(httpMethod, okResponseTestHelper);
      okResponseTestHelper.verifyInteractionWithoutContentTypeHeader();
    }
  }

  @Test
  void doDeleteWithAcrolinxBaseUrlHeaderAndNoContentTypeHeaderTest()
      throws IOException, ServletException {
    verifyOkResponseWithoutContentTypeHeader(HttpMethod.DELETE, Optional.of("http://example.com/"));
  }

  @Test
  void doDeleteWithAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponseWithContentTypeHeader(HttpMethod.DELETE, Optional.of("http://example.com/"));
  }

  @Test
  void doDeleteWithNoAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponseWithContentTypeHeader(HttpMethod.DELETE, Optional.empty());
  }

  @Test
  void doGetWithAcrolinxBaseUrlForChunkedData()
      throws IOException, InterruptedException, ServletException {
    verifyChunkedHttpResponse();
  }

  @Test
  void doGetWithAcrolinxBaseUrlHeaderAndNoContentTypeHeaderTest()
      throws IOException, ServletException {
    verifyOkResponseWithoutContentTypeHeader(HttpMethod.GET, Optional.of("http://example.com/"));
  }

  @Test
  void doGetWithAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponseWithContentTypeHeader(HttpMethod.GET, Optional.of("http://example.com/"));
  }

  @Test
  void doGetWithNoAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponseWithContentTypeHeader(HttpMethod.GET, Optional.empty());
  }

  @Test
  void doPostWithAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponseWithContentTypeHeader(HttpMethod.POST, Optional.of("http://example.com/"));
  }

  @Test
  void doPostWithNoAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponseWithContentTypeHeader(HttpMethod.POST, Optional.empty());
  }

  @Test
  void doPutWithAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponseWithContentTypeHeader(HttpMethod.PUT, Optional.of("http://example.com/"));
  }

  @Test
  void doPutWithNoAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponseWithContentTypeHeader(HttpMethod.PUT, Optional.empty());
  }
}
