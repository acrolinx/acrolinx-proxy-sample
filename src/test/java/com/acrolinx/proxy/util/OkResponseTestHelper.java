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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.servlet.ServletConfig;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class OkResponseTestHelper {
  private static final String ACROLINX_BASE_URL = "X-Acrolinx-Base-Url";
  private static final String CHECK_URL = "/api/v1/checking/checks";
  private static final int END_OF_INPUT_STREAM = -1;
  private static final byte[] RESPONSE_BODY = new byte[] {1, 2, 3};
  private static final String RESPONSE_CONTENT_TYPE = "application/json";
  private static final int RESPONSE_STATUS_CODE = 200;

  public static OkResponseTestHelper createAndSetUpTestEnvironmentWithContentTypeResponseHeader(
      WireMockServer wireMockServer,
      Optional<String> acrolinxBaseUrlHeaderValue,
      HttpMethod httpMethod)
      throws IOException {
    return createAndSetUpTestEnvironment(
        wireMockServer, acrolinxBaseUrlHeaderValue, httpMethod, true);
  }

  public static OkResponseTestHelper createAndSetUpTestEnvironmentWithoutContentTypeResponseHeader(
      WireMockServer wireMockServer,
      Optional<String> acrolinxBaseUrlHeaderValue,
      HttpMethod httpMethod)
      throws IOException {
    return createAndSetUpTestEnvironment(
        wireMockServer, acrolinxBaseUrlHeaderValue, httpMethod, false);
  }

  private static OkResponseTestHelper createAndSetUpTestEnvironment(
      WireMockServer wireMockServer,
      Optional<String> acrolinxBaseUrlHeaderValue,
      HttpMethod httpMethod,
      boolean addContentTypeResponseHeader)
      throws IOException {
    final String acrolinxUrlString = "http://localhost:" + wireMockServer.port();
    OkResponseTestHelper okResponseTestHelper =
        new OkResponseTestHelper(
            acrolinxBaseUrlHeaderValue, httpMethod, acrolinxUrlString, wireMockServer);
    okResponseTestHelper.setUpTestEnvironment(addContentTypeResponseHeader);
    return okResponseTestHelper;
  }

  private static ResponseDefinitionBuilder createResponseDefinitionBuilder(
      boolean addContentTypeResponseHeader) {
    final ResponseDefinitionBuilder responseDefinitionBuilder =
        new ResponseDefinitionBuilder().withStatus(RESPONSE_STATUS_CODE).withBody(RESPONSE_BODY);

    if (addContentTypeResponseHeader) {
      responseDefinitionBuilder.withHeader("Content-Type", RESPONSE_CONTENT_TYPE);
    }

    return responseDefinitionBuilder;
  }

  private final Optional<String> acrolinxBaseUrlHeaderValue;
  private final String acrolinxUrlString;
  private final HttpMethod httpMethod;
  private final HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
  private final HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
  private final ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
  private final ServletInputStream servletInputStream = Mockito.mock(ServletInputStream.class);
  private final ServletOutputStream servletOutputStream = Mockito.mock(ServletOutputStream.class);
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

  public void verifyInteractionWithContentTypeHeader() throws IOException {
    verifyInteraction(true);
  }

  public void verifyInteractionWithoutContentTypeHeader() throws IOException {
    verifyInteraction(false);
  }

  private String createHeaderValue() {
    return acrolinxBaseUrlHeaderValue.orElse(
        acrolinxUrlString + '/' + AcrolinxProxyHttpServlet.PROXY_PATH);
  }

  private void setUpTestEnvironment(boolean addContentTypeResponseHeader) throws IOException {
    stubWireMock(createResponseDefinitionBuilder(addContentTypeResponseHeader));

    stubServletInputStream();
    stubHttpServletRequest();
    stubHttpServletResponse();

    ServletConfigUtil.stubServletConfigBase(servletConfig, acrolinxUrlString);
  }

  private void stubHttpServletRequest() throws IOException {
    Mockito.when(httpServletRequest.getInputStream()).thenReturn(servletInputStream);
    Mockito.when(httpServletRequest.getHeaderNames())
        .thenReturn(Collections.enumeration(List.of(ACROLINX_BASE_URL)));
    Mockito.when(httpServletRequest.getHeader(ACROLINX_BASE_URL))
        .thenReturn(acrolinxBaseUrlHeaderValue.orElse(null));
    Mockito.when(httpServletRequest.getRequestURL())
        .thenReturn(
            new StringBuffer(acrolinxUrlString + '/' + AcrolinxProxyHttpServlet.PROXY_PATH));
    Mockito.when(httpServletRequest.getQueryString()).thenReturn("");
    Mockito.when(httpServletRequest.getPathInfo()).thenReturn(CHECK_URL);
  }

  private void stubHttpServletResponse() throws IOException {
    Mockito.when(httpServletResponse.getOutputStream()).thenReturn(servletOutputStream);
  }

  private void stubServletInputStream() throws IOException {
    Mockito.when(servletInputStream.read(any())).thenReturn(END_OF_INPUT_STREAM);
  }

  private void stubWireMock(ResponseDefinitionBuilder responseDefinitionBuilder) {
    wireMockServer.stubFor(
        WireMock.request(httpMethod.name(), urlEqualTo(CHECK_URL))
            .withHeader(ACROLINX_BASE_URL, equalTo(createHeaderValue()))
            .withRequestBody(AbsentPattern.ABSENT)
            .willReturn(responseDefinitionBuilder));
  }

  private void verifyHttpServletResponseWithContentTypeHeader() throws IOException {
    final InOrder inOrder = Mockito.inOrder(httpServletResponse);
    inOrder.verify(httpServletResponse).setStatus(RESPONSE_STATUS_CODE);
    inOrder
        .verify(httpServletResponse)
        .setHeader(ArgumentMatchers.eq("Matched-Stub-Id"), ArgumentMatchers.anyString());
    inOrder.verify(httpServletResponse).getOutputStream();
    inOrder.verify(httpServletResponse).setContentType(RESPONSE_CONTENT_TYPE);
    inOrder.verify(httpServletResponse).setContentLength(RESPONSE_BODY.length);

    Mockito.verifyNoMoreInteractions(httpServletResponse);
  }

  private void verifyHttpServletResponseWithoutContentTypeHeader() throws IOException {
    final InOrder inOrder = Mockito.inOrder(httpServletResponse);
    inOrder.verify(httpServletResponse).setStatus(RESPONSE_STATUS_CODE);
    inOrder
        .verify(httpServletResponse)
        .setHeader(ArgumentMatchers.eq("Matched-Stub-Id"), ArgumentMatchers.anyString());
    inOrder.verify(httpServletResponse).getOutputStream();
    inOrder.verify(httpServletResponse).setContentLength(RESPONSE_BODY.length);

    Mockito.verifyNoMoreInteractions(httpServletResponse);
  }

  private void verifyInteraction(boolean hasContentTypeHeader) throws IOException {
    verifyInteractionWithServletInputStream();
    verifyInteractionWithServletOutputStream();

    if (hasContentTypeHeader) {
      verifyHttpServletResponseWithContentTypeHeader();
    } else {
      verifyHttpServletResponseWithoutContentTypeHeader();
    }

    verifyInteractionWithWireMock();
  }

  private void verifyInteractionWithServletInputStream() throws IOException {
    Mockito.verify(servletInputStream, Mockito.atMostOnce()).read(any());
    Mockito.verify(servletInputStream, Mockito.atMostOnce()).close();
    Mockito.verifyNoMoreInteractions(servletInputStream);
  }

  private void verifyInteractionWithServletOutputStream() throws IOException {
    Mockito.verify(servletOutputStream, Mockito.atMostOnce()).write(RESPONSE_BODY);
    Mockito.verify(servletOutputStream).close();
    Mockito.verifyNoMoreInteractions(servletOutputStream);
  }

  private void verifyInteractionWithWireMock() {
    wireMockServer.verify(
        new RequestPatternBuilder(
                RequestMethod.fromString(httpMethod.name()), urlEqualTo(CHECK_URL))
            .withHeader(ACROLINX_BASE_URL, equalTo(createHeaderValue()))
            .withRequestBody(AbsentPattern.ABSENT));

    Assertions.assertEquals(Collections.emptyList(), wireMockServer.findAllUnmatchedRequests());
  }
}
