/* Copyright (c) 2016 Acrolinx GmbH */

package com.acrolinx.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Enumeration;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet acts as a reverse proxy. The Acrolinx plug-in uses this servlet to communicate with
 * the Acrolinx core server.
 */
public class AcrolinxProxyServlet extends HttpServlet
{
    private static final Logger logger = LoggerFactory.getLogger(AcrolinxProxyServlet.class);
    private static final long serialVersionUID = 1L;

    // TODO: Set this path in context of your servlet's reverse proxy implementation
    private static final String PROXY_PATH = "proxySample/proxy";

    private final CloseableHttpClient closeableHttpClient;
    private String acrolinxURL;
    private String genericToken;

    public AcrolinxProxyServlet()
    {
        closeableHttpClient = createHttpClient();
    }

    static final class ContentLengthHeaderRemover implements HttpRequestInterceptor
    {
        static final HttpRequestInterceptor INSTANCE = new ContentLengthHeaderRemover();

        private ContentLengthHeaderRemover()
        {
            // do nothing
        }

        @Override
        public void process(final HttpRequest httpRequest, final HttpContext httpContext)
        {
            httpRequest.removeHeaders(HTTP.CONTENT_LEN);
        }
    }

    private static CloseableHttpClient createHttpClient()
    {
        HttpClientConnectionManager httpClientConnectionManager = new PoolingHttpClientConnectionManager();
        RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();

        return HttpClientBuilder.create().setConnectionManager(httpClientConnectionManager).setDefaultRequestConfig(
                requestConfig).disableRedirectHandling().addInterceptorFirst(
                        ContentLengthHeaderRemover.INSTANCE).useSystemProperties().build();
    }

    @Override
    public void init()
    {
        // Properties can be configured by init parameters in the web.xml.
        acrolinxURL = getInitParameterOrDefaultValue("acrolinxURL", "http://localhost:8031/").replaceAll("/$", "");
        genericToken = getInitParameterOrDefaultValue("genericToken", "secret");
    }

    private String getInitParameterOrDefaultValue(final String name, final String defaultValue)
    {
        String parameterValue = getInitParameter(name);
        return parameterValue != null ? parameterValue : defaultValue;
    }

    @Override
    public void doDelete(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final HttpDelete httpDelete = new HttpDelete();
        logger.debug("Processing delete");
        proxyRequest(httpServletRequest, httpServletResponse, httpDelete);
    }

    @Override
    public void doPost(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final HttpPost httpPost = new HttpPost();
        httpPost.setEntity(new InputStreamEntity(httpServletRequest.getInputStream()));
        logger.debug("Processing post");
        proxyRequest(httpServletRequest, httpServletResponse, httpPost);
    }

    @Override
    public void doPut(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final HttpPut httpPut = new HttpPut();
        httpPut.setEntity(new InputStreamEntity(httpServletRequest.getInputStream()));
        logger.debug("Processing put");
        proxyRequest(httpServletRequest, httpServletResponse, httpPut);
    }

    @Override
    public void doGet(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse)
            throws IOException
    {
        final HttpGet httpGet = new HttpGet();
        logger.debug("Processing get");
        proxyRequest(httpServletRequest, httpServletResponse, httpGet);
    }

    private void proxyRequest(final HttpServletRequest httpServletRequest, final HttpServletResponse servletResponse,
            final HttpRequestBase httpRequestBase) throws IOException
    {
        copyHeaders(httpServletRequest, httpRequestBase);

        modifyRequest(httpRequestBase, httpServletRequest);

        // TODO: Make sure not to call the following line in case a user is not
        // authenticated to the application.
        addSingleSignOn(httpRequestBase);

        CloseableHttpResponse httpResponse = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            logger.debug("Sending request to Acrolinx");
            httpResponse = closeableHttpClient.execute(httpRequestBase);

            final int status = httpResponse.getStatusLine().getStatusCode();
            logger.debug("Response received: {}", status);
            servletResponse.setStatus(status);

            Header[] clonedHeaders = httpResponse.getAllHeaders().clone();

            for (Header header : clonedHeaders) {
                if (!(header.getName().startsWith("Transfer-Encoding") || header.getName().startsWith("Content-Length")
                        || header.getName().startsWith("Content-Type"))) {
                    servletResponse.setHeader(header.getName(), header.getValue());
                }
            }

            HttpEntity httpEntity = httpResponse.getEntity();

            if (httpEntity != null) {
                inputStream = httpEntity.getContent();
                outputStream = servletResponse.getOutputStream();

                servletResponse.setContentType(httpResponse.getEntity().getContentType().getValue());
                servletResponse.setContentLength((int) httpResponse.getEntity().getContentLength());

                spoolResponseBody(inputStream, outputStream);
                logger.debug("Forwarded response to client");
            }

            httpResponse.close();
        } catch (final ConnectException e) {
            servletResponse.sendError(HttpURLConnection.HTTP_BAD_GATEWAY, e.getClass().getName());
            logger.error("Error while processing proxy request", e);
        } finally {
            cleanUp(httpRequestBase, inputStream, outputStream);
        }
    }

    private static void cleanUp(final HttpRequestBase httpRequestBase, final InputStream inputStream,
            final OutputStream outputStream)
    {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (final IOException ioe) {
                // ignore
            }
        }

        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (final IOException ioe) {
                // ignore
            }
        }

        if (httpRequestBase != null) {
            httpRequestBase.releaseConnection();
        }
    }

    private static void spoolResponseBody(final InputStream inputStream, final OutputStream outputStream)
            throws IOException
    {
        if (inputStream != null) {
            final byte[] buffer = new byte[8_192];
            int n;

            while (-1 != (n = inputStream.read(buffer))) {
                outputStream.write(buffer, 0, n);
            }
        }
    }

    private void modifyRequest(final HttpRequestBase httpRequestBase, final HttpServletRequest httpServletRequest)
            throws IOException
    {
        final URI targetURL = getTargetUri(httpServletRequest);
        httpRequestBase.setURI(targetURL);
        setRequestHeader(httpRequestBase, "User-Agent", "Acrolinx Proxy");
        setRequestHeader(httpRequestBase, "Host", targetURL.getHost());
        setRequestHeader(httpRequestBase, "X-Acrolinx-Integration-Proxy-Version", "2");

        // add an extra header which is needed for acrolinx to support client's reverse proxy
        String baseUrl = httpServletRequest.getRequestURL().toString();
        baseUrl = baseUrl.substring(0, baseUrl.indexOf(PROXY_PATH) + PROXY_PATH.length());
        httpRequestBase.setHeader("X-Acrolinx-Base-Url", baseUrl);
    }

    private static void setRequestHeader(final HttpRequestBase httpRequestBase, final String headerName,
            final String headerValue)
    {
        httpRequestBase.removeHeaders(headerName);
        httpRequestBase.setHeader(headerName, headerValue);
    }

    private void addSingleSignOn(final HttpRequestBase httpRequestBase)
    {
        setRequestHeader(httpRequestBase, "username", getUsernameFromApplicationSession());
        setRequestHeader(httpRequestBase, "password", genericToken);
    }

    private String getUsernameFromApplicationSession()
    {
        return getInitParameterOrDefaultValue("username", "username");
        // TODO: Set user name from the current applications session. This is just an
        // example code the user name comes from web.xml.
    }

    private static void copyHeaders(final HttpServletRequest httpServletRequest, final HttpRequestBase httpRequestBase)
    {
        final Enumeration<String> headerNames = httpServletRequest.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();

            if (headerName.equalsIgnoreCase("cookie")) {
                httpRequestBase.addHeader(headerName, filterCookies(httpServletRequest.getHeader(headerName)));
            } else {
                httpRequestBase.addHeader(headerName, httpServletRequest.getHeader(headerName));
            }
        }
    }

    private static String filterCookies(String rawCookie)
    {
        String[] rawCookieParams = rawCookie.split(";");
        String[] rawCookieNameAndValues = Arrays.stream(rawCookieParams).filter(
                rawCookieNameAndValue -> rawCookieNameAndValue.toUpperCase().startsWith("X-ACROLINX-")).toArray(
                        String[]::new);
        String rawAcrolinxCookies = String.join(";", rawCookieNameAndValues);
        logger.debug("Processed acrolinx cookies: {}", rawAcrolinxCookies.isEmpty());
        return rawAcrolinxCookies;
    }

    private URI getTargetUri(final HttpServletRequest httpServletRequest) throws IOException
    {
        final String queryPart = httpServletRequest.getQueryString() != null ? "?" + httpServletRequest.getQueryString()
                : "";
        final String urlStr = acrolinxURL + httpServletRequest.getPathInfo() + queryPart;

        try {
            URI targetURI = new URI(urlStr);
            logger.debug("Request URL: {}", targetURI);
            return targetURI;
        } catch (final URISyntaxException e) {
            throw new IOException("Not a valid URI", e);
        }
    }
}
