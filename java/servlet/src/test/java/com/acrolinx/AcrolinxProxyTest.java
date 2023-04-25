
package com.acrolinx;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.acrolinx.proxy.AcrolinxProxyServlet;

import io.github.cdimascio.dotenv.Dotenv;

@RunWith(MockitoJUnitRunner.class)
public class AcrolinxProxyTest
{
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private static final String CLIENT_SIGNATURE = dotenv.get("ACROLINX_DEV_SIGNATURE");
    private static final String LOCAL_SERVER_URL = "http://localhost:8080";
    private static final String ACROLINX_URL = dotenv.get("ACROLINX_URL");
    private static final String ACROLINX_API_SSO_TOKEN = dotenv.get("ACROLINX_API_SSO_TOKEN");
    private static final String ACROLINX_API_USERNAME = dotenv.get("ACROLINX_API_USERNAME");
    private static final String ACROLINX_API_TOKEN_STRING = dotenv.get("ACROLINX_API_TOKEN");
    private static final String PROXY_PATH = "proxySample/proxy";

    private final StubServletOutputStream servletOutputStream = new StubServletOutputStream();

    @Mock
    private ServletConfig servletConfig;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Before
    public void setUp() throws IOException
    {
        initializeRequestResponse(false);
    }

    @Test
    public void testSimpleGet() throws IOException, ServletException
    {
        String api = "/api/v1";
        when(httpServletRequest.getRequestURL()).thenReturn(
                new StringBuffer(LOCAL_SERVER_URL + "/proxySample/proxy" + api));
        when(httpServletRequest.getPathInfo()).thenReturn("/api/v1");

        final AcrolinxProxyServlet acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(servletConfig);
        acrolinxProxyServlet.doGet(httpServletRequest, httpServletResponse);

        final byte[] data = servletOutputStream.byteArrayOutputStream.toByteArray();
        Assert.assertTrue(data.length > 0);
    }

    @Test
    public void testFetchingSidebarHTML() throws IOException, ServletException
    {
        String api = "/sidebar/v14/index.html";
        setRequestUrl(api);

        final AcrolinxProxyServlet acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(servletConfig);
        acrolinxProxyServlet.doGet(httpServletRequest, httpServletResponse);

        final byte[] data = servletOutputStream.byteArrayOutputStream.toByteArray();
        Assert.assertTrue(data.length > 0);

        String responseBody = new String(data);
        assertTrue(responseBody.contains("<title>Acrolinx</title>"));
    }

    @Test
    public void testSignInSSO() throws IOException, ServletException {
        when(httpServletRequest.getInputStream()).thenReturn(new StubServletInputStream(""));

        final AcrolinxProxyServlet acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(servletConfig);

        String signInResult = post(acrolinxProxyServlet, "/api/v1/auth/sign-ins", "");

        assertTrue(signInResult + " should contain ACROLINX_SSO", signInResult.contains("ACROLINX_SSO"));
        assertTrue(signInResult + " should contain getUser", signInResult.contains("getUser"));
        assertTrue(signInResult + " should contain accessToken", signInResult.contains("accessToken"));
        assertTrue(signInResult + " should contain Success", signInResult.contains("Success"));
    }

    @Test
    public void testSubmitCheck() throws IOException, ServletException
    {
        AcrolinxProxyServlet acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(servletConfig);

        String postData = "{\"content\": \"Test content\", \"document\" : {\"reference\" : \"test.txt\""
                + ", \"customFields\": [ {\"key\":\"Name\", \"value\":\"A blog post\"} ] }" + "}";

        initializeRequestResponse(true);
        String resultResponseBody = post(acrolinxProxyServlet, "/api/v1/checking/checks", postData);

        assertTrue("Check response should look like json. Request: " + postData + " Response: " + resultResponseBody,
                resultResponseBody.trim().startsWith("{") && resultResponseBody.trim().endsWith("}"));
    }

    @Test
    public void testCheckAndGetScoreCardThroughProxy() throws IOException, ServletException, InterruptedException
    {
        AcrolinxProxyServlet acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(servletConfig);

        String postData = createCheckBody(false);
        String pollResponseBody = check(acrolinxProxyServlet, postData);

        if (pollResponseBody.contains("customFieldsIncorrect")) {
            assertTrue("Expect that the respnse would require to set the custom field called Name. Response: "
                    + pollResponseBody, pollResponseBody.contains("Name"));
            postData = createCheckBody(true); // Set hard coded custom field called "Name"
            pollResponseBody = check(acrolinxProxyServlet, postData);
        }

        String scorecardUrl = getJsonValue(pollResponseBody, "Score Card\",\"link");

        assertNotNull("Scorecard Url not found. Request: " + postData + "\nresponse: " + pollResponseBody,
                scorecardUrl);
        initializeRequestResponse(false);

        String scorecardUrlRequest = scorecardUrl.substring(scorecardUrl.indexOf(PROXY_PATH) + PROXY_PATH.length());

        Thread.sleep(1_000);

        String scorecardResponseBody = get(acrolinxProxyServlet, scorecardUrlRequest);
        assertTrue("Doesn't look like a score card. Request: " + postData + "\nresponse: " + scorecardResponseBody,
                scorecardResponseBody.contains("<title>Scorecard</title>"));
    }

    @Test
    public void testSsoCheckAndGetScoreCardThroughProxy() throws IOException, ServletException, InterruptedException {
        when(httpServletRequest.getInputStream()).thenReturn(new StubServletInputStream(""));

        final AcrolinxProxyServlet acrolinxProxyServlet = new AcrolinxProxyServlet();
        acrolinxProxyServlet.init(servletConfig);

        String signInResult = post(acrolinxProxyServlet, "/api/v1/auth/sign-ins", "");

        assertTrue(signInResult + " should contain ACROLINX_SSO", signInResult.contains("ACROLINX_SSO"));
        assertTrue(signInResult + " should contain accessToken", signInResult.contains("accessToken"));

        initializeRequestResponse(getJsonValue(signInResult, "accessToken"));

        String postData = createCheckBody(false);
        String pollResponseBody = check(acrolinxProxyServlet, postData);

        if (pollResponseBody.contains("customFieldsIncorrect")) {
            assertTrue("Expect that the respnse would require to set the custom field called Name. Response: "
                    + pollResponseBody, pollResponseBody.contains("Name"));
            postData = createCheckBody(true); // Set hard coded custom field called "Name"
            pollResponseBody = check(acrolinxProxyServlet, postData);
        }

        String scorecardUrl = getJsonValue(pollResponseBody, "Score Card\",\"link");

        assertNotNull("Scorecard Url not found. Request: " + postData + "\nresponse: " + pollResponseBody,
                scorecardUrl);
        initializeRequestResponse(false);

        String scorecardUrlRequest = scorecardUrl.substring(scorecardUrl.indexOf(PROXY_PATH) + PROXY_PATH.length());

        Thread.sleep(1_000);
        String scorecardResponseBody = get(acrolinxProxyServlet, scorecardUrlRequest);
        assertTrue("Doesn't look like a score card. Request: " + postData + "\nresponse: " + scorecardResponseBody,
                scorecardResponseBody.contains("<title>Scorecard</title>"));
    }

    private String check(AcrolinxProxyServlet acrolinxProxyServlet, String postData)
            throws IOException, InterruptedException
    {
        initializeRequestResponse(true);
        String resultResponseBody = post(acrolinxProxyServlet, "/api/v1/checking/checks", postData);

        String pollingUri = getJsonValue(resultResponseBody, "result");

        assertNotNull("no polling url found. Request: " + postData + "\nresponse: " + resultResponseBody, pollingUri);

        Thread.sleep(5_000);

        initializeRequestResponse(true);
        String pollingUrlRequest = pollingUri.substring(pollingUri.indexOf(PROXY_PATH) + PROXY_PATH.length());

        String pollResponseBody = get(acrolinxProxyServlet, pollingUrlRequest);
        return pollResponseBody;
    }

    private static String createCheckBody(boolean withCustomFields)
    {
        return "{\"content\": \"Test content\", \"document\" : {\"reference\" : \"test.txt\""
                + (withCustomFields ? ", \"customFields\": [ {\"key\":\"Name\", \"value\":\"A blog post\"} ]" : "")
                + "}}";
    }

    private String get(AcrolinxProxyServlet acrolinxProxyServlet, String url) throws IOException
    {
        setRequestUrl(url);

        acrolinxProxyServlet.doGet(httpServletRequest, httpServletResponse);

        return readBody();
    }

    private String post(AcrolinxProxyServlet acrolinxProxyServlet, String url, String postData) throws IOException
    {
        setRequestUrl(url);

        setRequestData(postData);

        acrolinxProxyServlet.doPost(httpServletRequest, httpServletResponse);
        return readBody();
    }

    private static String getJsonValue(String resultResponseBody, String key)
    {
        String regex = key + "\":\"(.*?)\"";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(resultResponseBody);

        String result = null;
        while (matcher.find()) {
            result = matcher.group(1);
        }

        return result;
    }

    private void setRequestData(String postData) throws IOException
    {
        final StubServletInputStream stubServletInputStream = new StubServletInputStream(postData);
        when(httpServletRequest.getInputStream()).thenReturn(stubServletInputStream);
    }

    private void setRequestUrl(String api) {
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer(LOCAL_SERVER_URL + "/proxySample/proxy" + api));
        when(httpServletRequest.getPathInfo()).thenReturn(api);
    }

    private String readBody()
    {
        byte[] data = servletOutputStream.byteArrayOutputStream.toByteArray();
        Assert.assertTrue(data.length > 0);

        return new String(data);
    }

    private void initializeRequestResponse(boolean addAuthToken) throws IOException
    {
        initializeRequestResponse(addAuthToken, ACROLINX_API_TOKEN_STRING);
    }

    private void initializeRequestResponse(String token) throws IOException
    {
        initializeRequestResponse(true, token);
    }

    private void initializeRequestResponse(boolean addAuthToken, String token) throws IOException
    {
        final List<String> headers = new ArrayList<>();
        headers.add("X-Acrolinx-Client");
        headers.add("X-Acrolinx-Client-Locale");
        headers.add("Content-Type");

        if (addAuthToken) {
            headers.add("X-Acrolinx-Auth");
            when(httpServletRequest.getHeader("X-Acrolinx-Auth")).thenReturn(token);
        }

        when(httpServletRequest.getHeaderNames()).thenReturn(Collections.enumeration(headers));
        when(httpServletRequest.getQueryString()).thenReturn("");

        when(httpServletRequest.getHeader("X-Acrolinx-Client")).thenReturn(CLIENT_SIGNATURE);
        when(httpServletRequest.getHeader("X-Acrolinx-Client-Locale")).thenReturn("en-US");
        when(httpServletRequest.getHeader("Content-Type")).thenReturn("application/json");

        when(servletConfig.getInitParameter("acrolinxURL")).thenReturn(ACROLINX_URL);
        when(servletConfig.getInitParameter("genericToken")).thenReturn(ACROLINX_API_SSO_TOKEN);
        when(servletConfig.getInitParameter("username")).thenReturn(ACROLINX_API_USERNAME);
        when(httpServletResponse.getOutputStream()).thenReturn(servletOutputStream);
    }
}
