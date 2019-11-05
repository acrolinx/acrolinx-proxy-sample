package com.acrolinx;

import com.acrolinx.proxy.AcrolinxProxyServlet;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AcrolinxProxyTestServlet extends Mockito {

    private static final String CLIENT_SIGNATURE = "SW50ZWdyYXRpb25EZXZlbG9wbWVudERlbW9Pbmx5";
    private static final String LOCAL_SERVER_URL = "http://localhost:8080";
    static Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private static final String ACROLINX_URL = dotenv.get("ACROLINX_URL");
    private static final String ACROLINX_API_SSO_TOKEN = dotenv.get("ACROLINX_API_SSO_TOKEN");
    private static final String ACROLINX_API_USERNAME = dotenv.get("ACROLINX_API_USERNAME");
    private static final String ACROLINX_API_TOKEN_STRING = dotenv.get("ACROLINX_API_TOKEN");
    private final StubServletOutputStream servletOutputStream = new StubServletOutputStream();
    private final String PROXY_PATH = "proxySample/proxy";

    private ServletConfig sg = mock(ServletConfig.class);
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);

    @Test
    public void testSimpleGet() throws IOException, ServletException {

        initializeRequestResponse(false);

        String api = "/api/v1";
        when(request.getRequestURL()).thenReturn(new StringBuffer(LOCAL_SERVER_URL + "/proxySample/proxy" + api));
        when(request.getPathInfo()).thenReturn("/api/v1");

        final AcrolinxProxyServlet acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(sg);
        acrolinxProxyServlet.doGet(request, response);

        final byte[] data = servletOutputStream.os.toByteArray();
        Assert.assertNotNull(data);
        Assert.assertTrue(data.length > 0);
    }

    @Test
    public void testFetchingSidebarHTML() throws IOException, ServletException {
        initializeRequestResponse(false);

        String api = "/sidebar/v14/index.html";
        when(request.getRequestURL()).thenReturn(new StringBuffer(LOCAL_SERVER_URL + "/proxySample/proxy" + api));
        when(request.getPathInfo()).thenReturn(api);

        final AcrolinxProxyServlet acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(sg);
        acrolinxProxyServlet.doGet(request, response);

        final byte[] data = servletOutputStream.os.toByteArray();
        Assert.assertNotNull(data);
        Assert.assertTrue(data.length > 0);

        String responseBody = new String(data);
        assertTrue(responseBody.contains("<title>Acrolinx</title>"));
    }

    @Test
    public void testSignInSSO() throws IOException, ServletException {
        initializeRequestResponse(false);

        String api = "/api/v1/auth/sign-ins";
        when(request.getRequestURL()).thenReturn(new StringBuffer(LOCAL_SERVER_URL + "/proxySample/proxy" + api));
        when(request.getPathInfo()).thenReturn(api);

        final StubServletInputStream stubServletInputStream = new StubServletInputStream("");
        when(request.getInputStream()).thenReturn(stubServletInputStream);

        final AcrolinxProxyServlet acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(sg);
        acrolinxProxyServlet.doPost(request, response);


        final byte[] data = servletOutputStream.os.toByteArray();
        Assert.assertNotNull(data);
        Assert.assertTrue(data.length > 0);

        String responseBody = new String(data);
        assertTrue(responseBody.contains("links"));
        assertTrue(responseBody.contains("poll"));
        assertTrue(responseBody.contains("interactive"));

    }

    @Test
    public void testCheck() throws IOException, ServletException, URISyntaxException, InterruptedException {
        initializeRequestResponse(true);

        String api = "/api/v1/checking/checks";
        when(request.getRequestURL()).thenReturn(new StringBuffer(LOCAL_SERVER_URL + "/proxySample/proxy" + api));
        when(request.getPathInfo()).thenReturn(api);

        String postData = "{\"content\": \"Test content\", \"document\" : {\"reference\" : \"test.txt\"}}";

        final StubServletInputStream stubServletInputStream = new StubServletInputStream(postData);
        when(request.getInputStream()).thenReturn(stubServletInputStream);

        AcrolinxProxyServlet acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(sg);
        acrolinxProxyServlet.doPost(request, response);
        byte[] data = servletOutputStream.os.toByteArray();
        Assert.assertNotNull(data);
        Assert.assertTrue(data.length > 0);

        String responseBody = new String(data);

        String regex = "result\":\"(.*?)\"";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(responseBody);

        String pollingUri = null;
        while (matcher.find()) {
            pollingUri = matcher.group(1);
        }

        assertNotNull(pollingUri);

        Thread.sleep(5000);

        initializeRequestResponse(true);

        String pollingUrlRequest = pollingUri.substring(pollingUri.indexOf(PROXY_PATH) + PROXY_PATH.length());

        when(request.getRequestURL()).thenReturn(new StringBuffer(pollingUri));
        when(request.getPathInfo()).thenReturn(pollingUrlRequest);

        acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(sg);
        acrolinxProxyServlet.doGet(request, response);

        data = servletOutputStream.os.toByteArray();
        Assert.assertNotNull(data);
        Assert.assertTrue(data.length > 0);

        responseBody = new String(data);

        regex = "Score Card\",\"link\":\"(.*?)\"";
        pattern = Pattern.compile(regex, Pattern.MULTILINE);
        matcher = pattern.matcher(responseBody);

        String scorecardUrl = null;
        while (matcher.find()) {
            scorecardUrl = matcher.group(1);
        }

        assertNotNull(scorecardUrl);
        initializeRequestResponse(false);

        String scorecardUrlRequest = scorecardUrl.substring(scorecardUrl.indexOf(PROXY_PATH) + PROXY_PATH.length());

        when(request.getRequestURL()).thenReturn(new StringBuffer(scorecardUrl));
        when(request.getPathInfo()).thenReturn(scorecardUrlRequest);

        acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(sg);
        acrolinxProxyServlet.doGet(request, response);

        data = servletOutputStream.os.toByteArray();
        Assert.assertNotNull(data);
        Assert.assertTrue(data.length > 0);

        responseBody = new String(data);
        assertTrue(responseBody.contains("<title>Scorecard</title>"));

    }


    private void initializeRequestResponse(boolean addAuthToken) throws IOException {
        final ArrayList<String> headers = new ArrayList<>();
        headers.add("X-Acrolinx-Client");
        headers.add("X-Acrolinx-Client-Locale");
        headers.add("Content-Type");
        if (addAuthToken) {
            headers.add("X-Acrolinx-Auth");
            when(request.getHeader("X-Acrolinx-Auth")).thenReturn(ACROLINX_API_TOKEN_STRING);
        }

        when(request.getHeaderNames()).thenReturn(Collections.enumeration(headers));
        when(request.getQueryString()).thenReturn("");

        when(request.getHeader("X-Acrolinx-Client")).thenReturn(CLIENT_SIGNATURE);
        when(request.getHeader("X-Acrolinx-Client-Locale")).thenReturn("en-US");
        when(request.getHeader("Content-Type")).thenReturn("application/json");

        when(sg.getInitParameter("acrolinxURL")).thenReturn(ACROLINX_URL);
        when(sg.getInitParameter("genericToken")).thenReturn(ACROLINX_API_SSO_TOKEN);
        when(sg.getInitParameter("username")).thenReturn(ACROLINX_API_USERNAME);
        when(response.getOutputStream()).thenReturn(servletOutputStream);
    }
}
