/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy;

import com.acrolinx.proxy.util.HttpMethod;
import com.acrolinx.proxy.util.HttpServletTimeoutsConfig;
import com.acrolinx.proxy.util.OkResponseTestHelper;
import com.acrolinx.proxy.util.WireMockServerWrapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.ServletException;
import org.junit.jupiter.api.Test;

class AcrolinxProxySuccessTest {
  private static void verifyOkResponse(
      HttpMethod httpMethod, Optional<String> acrolinxBaseUrlHeaderValue)
      throws IOException, ServletException {
    try (WireMockServerWrapper wireMockServerWrapper =
        WireMockServerWrapper.startOnRandomHttpPort()) {
      final WireMockServer wireMockServer = wireMockServerWrapper.getWireMockServer();
      OkResponseTestHelper okResponseTestHelper =
          OkResponseTestHelper.createAndSetUpTestEnvironment(
              wireMockServer,
              acrolinxBaseUrlHeaderValue,
              httpMethod,
              "http://localhost:" + wireMockServer.port());

      httpMethod.callAcrolinxProxyMethod(
          okResponseTestHelper.getHttpServletRequest(),
          okResponseTestHelper.getHttpServletResponse(),
          okResponseTestHelper.getServletConfig(),
          new HttpServletTimeoutsConfig());

      okResponseTestHelper.verifyInteraction();
    }
  }

  @Test
  void doDeleteWithAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponse(HttpMethod.DELETE, Optional.of("http://example.com/"));
  }

  @Test
  void doDeleteWithNoAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponse(HttpMethod.DELETE, Optional.empty());
  }

  @Test
  void doGetWithAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponse(HttpMethod.GET, Optional.of("http://example.com/"));
  }

  @Test
  void doGetWithNoAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponse(HttpMethod.GET, Optional.empty());
  }

  @Test
  void doPostWithAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponse(HttpMethod.POST, Optional.of("http://example.com/"));
  }

  @Test
  void doPostWithNoAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponse(HttpMethod.POST, Optional.empty());
  }

  @Test
  void doPutWithAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponse(HttpMethod.PUT, Optional.of("http://example.com/"));
  }

  @Test
  void doPutWithNoAcrolinxBaseUrlHeaderTest() throws IOException, ServletException {
    verifyOkResponse(HttpMethod.PUT, Optional.empty());
  }
}
