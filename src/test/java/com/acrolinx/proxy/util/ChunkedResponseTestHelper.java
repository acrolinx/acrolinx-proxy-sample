/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import static org.mockito.ArgumentMatchers.any;

import com.acrolinx.proxy.AcrolinxProxyHttpServlet;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import javax.servlet.ServletConfig;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class ChunkedResponseTestHelper {
  private static final String ACROLINX_BASE_URL = "X-Acrolinx-Base-Url";
  private static final int END_OF_INPUT_STREAM = -1;

  public static ChunkedResponseTestHelper createAndSetUpTestEnvironment(ServerSocket serverSocket)
      throws IOException {
    final String urlString = "http://localhost:" + serverSocket.getLocalPort();
    ChunkedResponseTestHelper chunkedResponseTestHelper =
        new ChunkedResponseTestHelper(
            createAndStartThread(createRunnable(serverSocket)), urlString);
    chunkedResponseTestHelper.setupTestEnvironment();

    return chunkedResponseTestHelper;
  }

  private static Thread createAndStartThread(Runnable runnable) {
    Thread thread = new Thread(runnable, "dummyServerThread");
    thread.start();
    return thread;
  }

  private static Runnable createRunnable(ServerSocket serverSocket) {
    return () -> {
      try (Socket socket = serverSocket.accept()) {
        final String chunkedHttpResponse =
            "HTTP/1.1 200 OK\r\n"
                + "Transfer-Encoding: chunked\r\n"
                + "\r\n"
                + "1\r\n"
                + "A\r\n"
                + "2\r\n"
                + "BC\r\n"
                + "0\r\n"
                + "\r\n";
        socket.getOutputStream().write(chunkedHttpResponse.getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    };
  }

  private final HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
  private final HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
  private final ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
  private final ServletInputStream servletInputStream = Mockito.mock(ServletInputStream.class);
  private final ServletOutputStream servletOutputStream = Mockito.mock(ServletOutputStream.class);
  private final Thread thread;
  private final String urlString;

  private ChunkedResponseTestHelper(Thread thread, String urlString) {
    this.thread = thread;
    this.urlString = urlString;
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

  public void verifyInteraction() throws IOException, InterruptedException {
    verifyInteractionWithServletInputStream();
    verifyInteractionWithServletOutputStream();
    verifyHttpServletResponse();
    verifyHttpServletRequest();

    thread.join(1_000);
    Assertions.assertSame(Thread.State.TERMINATED, thread.getState());
  }

  private void setupTestEnvironment() throws IOException {
    stubServletInputStream();

    stubHttpServletRequest();
    stubHttpServletResponse();

    ServletConfigUtil.stubServletConfigBase(servletConfig, urlString);
  }

  private void stubHttpServletRequest() throws IOException {
    Mockito.when(httpServletRequest.getInputStream()).thenReturn(servletInputStream);
    Mockito.when(httpServletRequest.getHeaderNames())
        .thenReturn(Collections.enumeration(Collections.singleton(ACROLINX_BASE_URL)));
    Mockito.when(httpServletRequest.getHeader(ACROLINX_BASE_URL)).thenReturn(urlString);
    Mockito.when(httpServletRequest.getRequestURL())
        .thenReturn(new StringBuffer(urlString + '/' + AcrolinxProxyHttpServlet.PROXY_PATH));
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

  private void verifyHttpServletRequest() {
    InOrder inOrder = Mockito.inOrder(httpServletRequest);

    inOrder.verify(httpServletRequest).getHeaderNames();
    inOrder.verify(httpServletRequest).getHeader(ACROLINX_BASE_URL);
    inOrder.verify(httpServletRequest, Mockito.times(2)).getQueryString();
    inOrder.verify(httpServletRequest).getPathInfo();
    inOrder.verify(httpServletRequest).getHeader(ACROLINX_BASE_URL);

    Mockito.verifyNoMoreInteractions(httpServletRequest);
  }

  private void verifyHttpServletResponse() throws IOException {
    final InOrder inOrder = Mockito.inOrder(httpServletResponse);
    inOrder.verify(httpServletResponse).setStatus(200);
    inOrder.verify(httpServletResponse).getOutputStream();
    inOrder.verify(httpServletResponse).setContentLength(3);

    Mockito.verifyNoMoreInteractions(httpServletResponse);
  }

  private void verifyInteractionWithServletInputStream() {
    Mockito.verifyNoMoreInteractions(servletInputStream);
  }

  private void verifyInteractionWithServletOutputStream() throws IOException {
    Mockito.verify(servletOutputStream).write("ABC".getBytes(StandardCharsets.UTF_8));
    Mockito.verify(servletOutputStream).close();
    Mockito.verifyNoMoreInteractions(servletOutputStream);
  }
}
