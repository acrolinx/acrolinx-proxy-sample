/* Copyright (c) 2024 Acrolinx GmbH */
package com.acrolinx.proxy.util;

import com.acrolinx.proxy.AcrolinxProxyHttpServlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class InvalidResponseTestHelper {
  private static final String CHECK_URL = "/api/v1/checking/checks";

  public static InvalidResponseTestHelper createAndSetUpTestEnvironment(ServerSocket serverSocket) {
    final String acrolinxUrlString = "http://localhost:" + serverSocket.getLocalPort();
    InvalidResponseTestHelper invalidResponseTestHelper =
        new InvalidResponseTestHelper(
            createAndStartThread(createRunnable(serverSocket)), acrolinxUrlString);
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
        socket.getOutputStream().write('\n');
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    };
  }

  private final String acrolinxUrlString;
  private final HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
  private final HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
  private final ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
  private final Thread thread;

  private InvalidResponseTestHelper(Thread thread, String acrolinxUrlString) {
    this.thread = thread;
    this.acrolinxUrlString = acrolinxUrlString;
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

    ServletConfigUtil.stubServletConfigBase(servletConfig, acrolinxUrlString);
  }

  private void stubHttpServletRequest() {
    Mockito.when(httpServletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    Mockito.when(httpServletRequest.getRequestURL())
        .thenReturn(
            new StringBuffer(acrolinxUrlString + '/' + AcrolinxProxyHttpServlet.PROXY_PATH));
    Mockito.when(httpServletRequest.getQueryString()).thenReturn("");
    Mockito.when(httpServletRequest.getPathInfo()).thenReturn(CHECK_URL);
  }

  private void verifyInteractionWithHttpServletResponse() throws IOException {
    Mockito.verify(httpServletResponse)
        .sendError(
            AdditionalMatchers.or(
                ArgumentMatchers.eq(HttpURLConnection.HTTP_UNAVAILABLE),
                ArgumentMatchers.eq(HttpURLConnection.HTTP_BAD_GATEWAY)),
            AdditionalMatchers.or(
                AdditionalMatchers.or(
                    ArgumentMatchers.eq("java.io.IOException: Invalid status line: \"\""),
                    ArgumentMatchers.eq("java.net.ProtocolException: Invalid status line: \"\"")),
                ArgumentMatchers.eq("java.net.SocketException: Connection reset")));
  }
}
