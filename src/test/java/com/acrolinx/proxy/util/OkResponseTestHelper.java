/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mockito.ArgumentMatchers.any;

import com.acrolinx.proxy.AcrolinxProxyHttpServlet;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.AbsentPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import javax.servlet.ServletConfig;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class OkResponseTestHelper {
  private static final String ACROLINX_BASE_URL = "X-Acrolinx-Base-Url";
  private static final int BUFFER_SIZE = 8_192;
  private static final int END_OF_INPUT_STREAM = -1;
  private static final byte[] RESPONSE_BODY = new byte[] {1, 2, 3};
  private static final String RESPONSE_CONTENT_TYPE = "application/json";
  private static final int RESPONSE_STATUS_CODE = 200;

  public static OkResponseTestHelper createAndSetUpTestEnvironment(
      WireMockServer wireMockServer,
      Optional<String> acrolinxBaseUrlHeaderValue,
      HttpMethod httpMethod,
      String acrolinxUrlString)
      throws IOException {
    OkResponseTestHelper okResponseTestHelper =
        new OkResponseTestHelper(
            acrolinxBaseUrlHeaderValue, httpMethod, acrolinxUrlString, wireMockServer);
    okResponseTestHelper.setUpTestEnvironment();
    return okResponseTestHelper;
  }

  private final Optional<String> acrolinxBaseUrlHeaderValue;
  private final HttpMethod httpMethod;
  private final HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
  private final HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
  private final ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
  private final ServletInputStream servletInputStream = Mockito.mock(ServletInputStream.class);
  private final ServletOutputStream servletOutputStream = Mockito.mock(ServletOutputStream.class);
  private final String acrolinxUrlString;
  private final WireMockServer wireMockServer;

  private OkResponseTestHelper(
      Optional<String> acrolinxBaseUrlHeaderValue,
      HttpMethod httpMethod,
      String acrolinxUrlString,
      WireMockServer wireMockServer) {
    this.acrolinxBaseUrlHeaderValue = acrolinxBaseUrlHeaderValue;
    this.httpMethod = httpMethod;
    this.acrolinxUrlString = acrolinxUrlString;
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

  public void verifyInteraction() throws IOException {
    verifyInteractionWithServletInputStream();
    verifyInteractionWithServletOutputStream();
    verifyHttpServletResponse();
    verifyInteractionWithWireMock();
  }

  private String createHeaderValue() {
    return acrolinxBaseUrlHeaderValue.orElse(
        acrolinxUrlString + '/' + AcrolinxProxyHttpServlet.PROXY_PATH);
  }

  private void setUpTestEnvironment() throws IOException {
    stubWireMock();

    stubServletInputStream();
    stubHttpServletRequest();
    stubHttpServletResponse();

    ServletConfigUtil.stubServletConfigBase(servletConfig, acrolinxUrlString);
  }

  private void stubHttpServletRequest() throws IOException {
    Mockito.when(httpServletRequest.getInputStream()).thenReturn(servletInputStream);
    Mockito.when(httpServletRequest.getHeaderNames())
        .thenReturn(Collections.enumeration(Collections.singleton(ACROLINX_BASE_URL)));
    Mockito.when(httpServletRequest.getHeader(ACROLINX_BASE_URL))
        .thenReturn(acrolinxBaseUrlHeaderValue.orElse(null));
    Mockito.when(httpServletRequest.getRequestURL())
        .thenReturn(
            new StringBuffer(acrolinxUrlString + '/' + AcrolinxProxyHttpServlet.PROXY_PATH));
    Mockito.when(httpServletRequest.getQueryString()).thenReturn("");
    Mockito.when(httpServletRequest.getPathInfo())
        .thenReturn(AcrolinxProxyTestCommonConstants.CHECK_URL);
  }

  private void stubHttpServletResponse() throws IOException {
    Mockito.when(httpServletResponse.getOutputStream()).thenReturn(servletOutputStream);
  }

  private void stubServletInputStream() throws IOException {
    Mockito.when(servletInputStream.read(any())).thenReturn(END_OF_INPUT_STREAM);
  }

  private void stubWireMock() {
    wireMockServer.stubFor(
        WireMock.request(httpMethod.name(), urlEqualTo(AcrolinxProxyTestCommonConstants.CHECK_URL))
            .withHeader(ACROLINX_BASE_URL, equalTo(createHeaderValue()))
            .withRequestBody(AbsentPattern.ABSENT)
            .willReturn(
                new ResponseDefinitionBuilder()
                    .withStatus(RESPONSE_STATUS_CODE)
                    .withHeader("Content-Type", RESPONSE_CONTENT_TYPE)
                    .withBody(RESPONSE_BODY)));
  }

  private void verifyHttpServletResponse() throws IOException {
    Mockito.verify(httpServletResponse).setStatus(RESPONSE_STATUS_CODE);

    Mockito.verify(httpServletResponse)
        .setHeader(ArgumentMatchers.eq("Matched-Stub-Id"), ArgumentMatchers.anyString());

    Mockito.verify(httpServletResponse).getOutputStream();

    Mockito.verify(httpServletResponse).setContentType(RESPONSE_CONTENT_TYPE);
    Mockito.verify(httpServletResponse).setContentLength(RESPONSE_BODY.length);

    Mockito.verifyNoMoreInteractions(httpServletResponse);
  }

  private void verifyInteractionWithServletInputStream() throws IOException {
    Mockito.verify(servletInputStream, Mockito.atMostOnce()).read(any());
    Mockito.verify(servletInputStream, Mockito.atMostOnce()).close();
    Mockito.verifyNoMoreInteractions(servletInputStream);
  }

  private void verifyInteractionWithServletOutputStream() throws IOException {
    byte[] bytes = Arrays.copyOf(RESPONSE_BODY, BUFFER_SIZE);

    Mockito.verify(servletOutputStream, Mockito.atMostOnce()).write(RESPONSE_BODY);
    Mockito.verify(servletOutputStream).close();
    Mockito.verifyNoMoreInteractions(servletOutputStream);
  }

  private void verifyInteractionWithWireMock() {
    wireMockServer.verify(
        new RequestPatternBuilder(
                RequestMethod.fromString(httpMethod.name()),
                urlEqualTo(AcrolinxProxyTestCommonConstants.CHECK_URL))
            .withHeader(ACROLINX_BASE_URL, equalTo(createHeaderValue()))
            .withRequestBody(AbsentPattern.ABSENT));

    Assertions.assertEquals(Collections.emptyList(), wireMockServer.findAllUnmatchedRequests());
  }
}
