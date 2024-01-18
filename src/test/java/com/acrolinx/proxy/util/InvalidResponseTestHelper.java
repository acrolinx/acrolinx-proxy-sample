/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import com.acrolinx.proxy.AcrolinxProxyHttpServlet;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

public class InvalidResponseTestHelper {
  public static InvalidResponseTestHelper createAndSetUpTestEnvironment(ServerSocket serverSocket) {
    InvalidResponseTestHelper invalidResponseTestHelper =
        new InvalidResponseTestHelper(
            createAndStartThread(createRunnable(serverSocket)), serverSocket);
    invalidResponseTestHelper.setupTestEnvironment();

    return invalidResponseTestHelper;
  }

  private static Thread createAndStartThread(Runnable runnable) {
    Thread thread = new Thread(runnable, "dummyServerThread");
    thread.start();
    return thread;
  }

  private static Runnable createRunnable(ServerSocket serverSocket) {
    return () -> {
      try (Socket socket = serverSocket.accept()) {
        socket.setSoTimeout(1);
        // serverSocket.setSoTimeout(1);
        socket.getOutputStream().write('\n');
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    };
  }

  private final HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
  private final HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
  private final ServerSocket serverSocket;
  private final ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
  private final Thread thread;

  private InvalidResponseTestHelper(Thread thread, ServerSocket serverSocket) {
    this.thread = thread;
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

  public void verifyInteraction() throws IOException, InterruptedException {
    verifyInteractionWithHttpServletResponse();

    thread.join(1_000);
    Assertions.assertSame(Thread.State.TERMINATED, thread.getState());
  }

  private void setupTestEnvironment() {
    stubHttpServletRequest();

    ServletConfigUtil.stubServletConfigBase(
        servletConfig, "http://localhost:" + serverSocket.getLocalPort());
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
    Mockito.when(httpServletRequest.getPathInfo())
        .thenReturn(AcrolinxProxyTestCommonConstants.CHECK_URL);
  }

  private void verifyInteractionWithHttpServletResponse() throws IOException {
    Mockito.verify(httpServletResponse)
        .sendError(
            HttpURLConnection.HTTP_UNAVAILABLE, "org.apache.http.client.ClientProtocolException");
    Mockito.verifyNoMoreInteractions(httpServletResponse);
  }
}
