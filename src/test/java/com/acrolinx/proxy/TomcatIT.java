/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.acrolinx.proxy.util.TomcatWrapper;
import com.acrolinx.proxy.util.WireMockServerWrapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.AbsentPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TomcatIT {
  private static final String HTTP_METHOD = "GET";
  private static final String URL_STRING = "/api/v1/auth/sign-ins";
  private static final String CLIENT_SIGNATURE = "SW50ZWdyYXRpb25EZXZlbG9wbWVudERlbW9Pbmx5; 1.0";
  private static final StringValuePattern PASSWORD = equalTo("secret");
  private static final StringValuePattern PROXY_VERSION = equalTo("2");
  private static final Duration TIMEOUT = Duration.ofSeconds(3);
  private static final StringValuePattern USERNAME = equalTo("testuser");
  private static final String X_ACROLINX_CLIENT = "X-Acrolinx-Client";

  private static void checkHttpResponse(HttpResponse<String> httpResponse) {
    Assertions.assertEquals(200, httpResponse.statusCode());
    Assertions.assertEquals("{}", httpResponse.body());
  }

  private static HttpClient createHttpClient() {
    return HttpClient.newBuilder()
        .connectTimeout(TIMEOUT)
        .followRedirects(Redirect.NEVER)
        .version(Version.HTTP_1_1)
        .build();
  }

  private static HttpRequest createHttpRequest(int tomcatPort) throws URISyntaxException {
    return HttpRequest.newBuilder()
        .GET()
        .uri(createUri(tomcatPort))
        .header(X_ACROLINX_CLIENT, CLIENT_SIGNATURE)
        .timeout(TIMEOUT)
        .build();
  }

  private static URI createUri(int tomcatPort) throws URISyntaxException {
    return new URI(
        "http",
        null,
        "localhost",
        tomcatPort,
        "/acrolinx-proxy-sample/proxy" + URL_STRING,
        null,
        null);
  }

  private static void deployWarFile(Tomcat tomcat) {
    final Path warFilePath = Path.of("target/acrolinx-proxy-sample.war").toAbsolutePath();
    Assertions.assertTrue(Files.exists(warFilePath));

    tomcat.addWebapp("", warFilePath.toString());
  }

  private static void stubWireMock(WireMockServer wireMockServer) {
    wireMockServer.stubFor(
        WireMock.request(HTTP_METHOD, urlEqualTo(URL_STRING))
            .withHeader(X_ACROLINX_CLIENT, equalTo(CLIENT_SIGNATURE))
            .withHeader("X-Acrolinx-Integration-Proxy-Version", PROXY_VERSION)
            .withHeader("username", USERNAME)
            .withHeader("password", PASSWORD)
            .withRequestBody(AbsentPattern.ABSENT)
            .willReturn(WireMock.okJson("{}")));
  }

  private static void verifyInteractionWithWireMock(WireMockServer wireMockServer) {
    wireMockServer.verify(
        new RequestPatternBuilder(RequestMethod.fromString(HTTP_METHOD), urlEqualTo(URL_STRING))
            .withHeader(X_ACROLINX_CLIENT, equalTo(CLIENT_SIGNATURE))
            .withHeader("X-Acrolinx-Integration-Proxy-Version", PROXY_VERSION)
            .withHeader("username", USERNAME)
            .withHeader("password", PASSWORD)
            .withRequestBody(AbsentPattern.ABSENT));

    Assertions.assertEquals(Collections.emptyList(), wireMockServer.findAllUnmatchedRequests());
  }

  @Test
  void embeddedTomcatIntegrationTest(@TempDir Path tempDirectory) throws Exception {
    try (WireMockServerWrapper wireMockServerWrapper = WireMockServerWrapper.startOnHttpPort(8031);
        TomcatWrapper tomcatWrapper = TomcatWrapper.startOnRandomHttpPort(tempDirectory)) {
      final WireMockServer wireMockServer = wireMockServerWrapper.getWireMockServer();
      stubWireMock(wireMockServer);

      final Tomcat tomcat = tomcatWrapper.getTomcat();
      deployWarFile(tomcat);

      HttpRequest httpRequest = createHttpRequest(tomcat.getConnector().getLocalPort());
      HttpResponse<String> httpResponse =
          createHttpClient().send(httpRequest, BodyHandlers.ofString());
      checkHttpResponse(httpResponse);

      verifyInteractionWithWireMock(wireMockServer);
    }
  }
}
