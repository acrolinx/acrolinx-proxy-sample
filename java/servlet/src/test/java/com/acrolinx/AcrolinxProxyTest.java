package com.acrolinx;

import com.acrolinx.proxy.AcrolinxProxyServlet;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AcrolinxProxyTest {

    private static final String CLIENT_SIGNATURE = "SW50ZWdyYXRpb25EZXZlbG9wbWVudERlbW9Pbmx5";
    private static final String LOCAL_SERVER_URL = "http://localhost:8080";
    static Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private static final String ACROLINX_URL = dotenv.get("ACROLINX_URL");
    private static final String ACROLINX_API_SSO_TOKEN = dotenv.get("ACROLINX_API_SSO_TOKEN");
    private static final String ACROLINX_API_USERNAME = dotenv.get("ACROLINX_API_USERNAME");
    private static final String ACROLINX_API_TOKEN_STRING = dotenv.get("ACROLINX_API_TOKEN");
    private final StubServletOutputStream servletOutputStream = new StubServletOutputStream();
    private final String PROXY_PATH = "proxySample/proxy";

    @Mock
    private ServletConfig sg;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Before
    public void setUp() throws IOException {
        initializeRequestResponse(false);
    }

    @Test
    public void testSimpleGet() throws IOException, ServletException {
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
        String api = "/sidebar/v14/index.html";
        setRequestUrl(api);

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
        final StubServletInputStream stubServletInputStream = new StubServletInputStream("");
        when(request.getInputStream()).thenReturn(stubServletInputStream);

        final AcrolinxProxyServlet acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(sg);

        String signInResult = post(acrolinxProxyServlet, "/api/v1/auth/sign-ins", "");

        assertTrue(signInResult + " should contain ACROLINX_SSO", signInResult.contains("ACROLINX_SSO"));
        assertTrue(signInResult + " should contain getUser", signInResult.contains("getUser"));
        assertTrue(signInResult + " should contain accessToken", signInResult.contains("accessToken"));
        assertTrue(signInResult + " should contain Success", signInResult.contains("Success"));
    }

    @Test
    public void testSubmitCheck() throws IOException, ServletException, URISyntaxException, InterruptedException {

        AcrolinxProxyServlet acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(sg);

        String postData = "{\"content\": \"Test content\", \"document\" : {\"reference\" : \"test.txt\""
                + ", \"customFields\": [ {\"key\":\"Name\", \"value\":\"A blog post\"} ] }" + "}";

        initializeRequestResponse(true);
        String resultResponseBody = post(acrolinxProxyServlet, "/api/v1/checking/checks", postData);

        assertTrue("Check response should look like json. Request: " + postData + " Response: " + resultResponseBody,
                resultResponseBody.trim().startsWith("{") && resultResponseBody.trim().endsWith("}"));
    }

    @Test
    public void testCheckAndGetScoreCardThroughProxy()
            throws IOException, ServletException, URISyntaxException, InterruptedException {

        AcrolinxProxyServlet acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(sg);

        String postData = createCheckBody(false);
        String pollResponseBody = check(acrolinxProxyServlet, postData);

        if (pollResponseBody.contains("customFieldsIncorrect")) {
            postData = createCheckBody(true); // Set hard coded custom field called "Name"
            pollResponseBody = check(acrolinxProxyServlet, postData);
        }

        String scorecardUrl = getJsonValue(pollResponseBody, "Score Card\",\"link");

        assertNotNull("Scorecard Url not found. Request: " + postData + "\nresponse: " + pollResponseBody,
                scorecardUrl);
        initializeRequestResponse(false);

        String scorecardUrlRequest = scorecardUrl.substring(scorecardUrl.indexOf(PROXY_PATH) + PROXY_PATH.length());

        String scorecardResponseBody = get(acrolinxProxyServlet, scorecardUrlRequest);
        assertTrue("Doesn't look like a score card. Request: " + postData + "\nresponse: " + scorecardResponseBody,
                scorecardResponseBody.contains("<title>Scorecard</title>"));

    }

    private String check(AcrolinxProxyServlet acrolinxProxyServlet, String postData)
            throws IOException, InterruptedException {
        initializeRequestResponse(true);
        String resultResponseBody = post(acrolinxProxyServlet, "/api/v1/checking/checks", postData);

        String pollingUri = getJsonValue(resultResponseBody, "result");

        assertNotNull("no polling url found. Request: " + postData + "\nresponse: " + resultResponseBody, pollingUri);

        Thread.sleep(5000);

        initializeRequestResponse(true);
        String pollingUrlRequest = pollingUri.substring(pollingUri.indexOf(PROXY_PATH) + PROXY_PATH.length());

        String pollResponseBody = get(acrolinxProxyServlet, pollingUrlRequest);
        return pollResponseBody;
    }

    private String createCheckBody(boolean withCustomFields) {
        String postData = "{\"content\": \"Test content\", \"document\" : {\"reference\" : \"test.txt\""
                + (withCustomFields ? ", \"customFields\": [ {\"key\":\"Name\", \"value\":\"A blog post\"} ]" : "")
                + "}}";
        return postData;
    }

    private String get(AcrolinxProxyServlet acrolinxProxyServlet, String url) throws IOException {
        setRequestUrl(url);

        acrolinxProxyServlet.doGet(request, response);

        return readBody();
    }

    private String post(AcrolinxProxyServlet acrolinxProxyServlet, String url, String postData) throws IOException {
        setRequestUrl(url);

        setRequestData(postData);

        acrolinxProxyServlet.doPost(request, response);
        String resultResponseBody = readBody();
        return resultResponseBody;
    }

    private String getJsonValue(String resultResponseBody, String key) {
        String regex = key + "\":\"(.*?)\"";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(resultResponseBody);

        String result = null;
        while (matcher.find()) {
            result = matcher.group(1);
        }

        return result;
    }

    private void setRequestData(String postData) throws IOException {
        final StubServletInputStream stubServletInputStream = new StubServletInputStream(postData);
        when(request.getInputStream()).thenReturn(stubServletInputStream);
    }

    private void setRequestUrl(String api) {
        when(request.getRequestURL()).thenReturn(new StringBuffer(LOCAL_SERVER_URL + "/proxySample/proxy" + api));
        when(request.getPathInfo()).thenReturn(api);
    }

    private String readBody() {
        byte[] data = servletOutputStream.os.toByteArray();
        Assert.assertNotNull(data);
        Assert.assertTrue(data.length > 0);

        String responseBody = new String(data);
        return responseBody;
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
