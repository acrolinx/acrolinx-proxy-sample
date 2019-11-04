package com.acrolinx;

import com.acrolinx.proxy.AcrolinxProxyServlet;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class AcrolinxProxyTestServlet extends Mockito {

    private static final String CLIENT_SIGNATURE = "SW50ZWdyYXRpb25EZXZlbG9wbWVudERlbW9Pbmx5";
    static Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private static final String ACROLINX_URL = dotenv.get("ACROLINX_URL");
    private static final String ACROLINX_API_SSO_TOKEN = dotenv.get("ACROLINX_API_SSO_TOKEN");
    private static final String ACROLINX_API_USERNAME = dotenv.get("ACROLINX_API_USERNAME");
    private static final String ACROLINX_API_TOKEN_STRING = dotenv.get("ACROLINX_API_TOKEN");
    private final StubServletOutputStream servletOutputStream = new StubServletOutputStream();
    @Captor
    private ArgumentCaptor<byte[]> valueCapture;
    private ServletConfig sg = mock(ServletConfig.class);
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);

    @Test
    public void testSimpleGet() throws IOException, ServletException {

        initializeRequestResponse();

        when(request.getRequestURL()).thenReturn(new StringBuffer(ACROLINX_URL + "/proxySample/proxy/api/v1"));
        when(request.getPathInfo()).thenReturn("/api/v1");

        final AcrolinxProxyServlet acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(sg);
        acrolinxProxyServlet.doGet(request, response);

        final byte[] data = servletOutputStream.os.toByteArray();
        Assert.assertNotNull(data);
        Assert.assertTrue(data.length > 0);
    }

    private void initializeRequestResponse() throws IOException {
        final ArrayList<String> headers = new ArrayList<>();
        headers.add("X-Acrolinx-Client");
        headers.add("X-Acrolinx-Client-Locale");

        when(request.getHeaderNames()).thenReturn(Collections.enumeration(headers));
        when(request.getQueryString()).thenReturn("");

        when(request.getHeader("X-Acrolinx-Client")).thenReturn(CLIENT_SIGNATURE);
        when(request.getHeader("X-Acrolinx-Client-Locale")).thenReturn("en-US");

        when(sg.getInitParameter("acrolinxURL")).thenReturn(ACROLINX_URL);
        when(sg.getInitParameter("genericToken")).thenReturn(ACROLINX_API_SSO_TOKEN);
        when(sg.getInitParameter("username")).thenReturn(ACROLINX_API_USERNAME);
        when(response.getOutputStream()).thenReturn(servletOutputStream);
    }
}
