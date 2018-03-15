/* Copyright (c) 2016 Acrolinx GmbH */

package com.acrolinx.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

/**
 * This servlet acts as a reverse proxy. The Acrolinx plug-in uses this servlet to communicate with
 * the Acrolinx core server.
 */
public class AcrolinxProxyServlet extends HttpServlet
{
    private static final long serialVersionUID = 2L;

    CloseableHttpClient httpClient;

    // Can be configured by init parameters in the web.xml
    private String acrolinxServer;
    private String secret;

    public AcrolinxProxyServlet()
    {
        super();
        this.httpClient = createHttpClient();

    }

    private static class ContentLengthHeaderRemover implements HttpRequestInterceptor
    {
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException
        {
            request.removeHeaders(HTTP.CONTENT_LEN);
        }

    }

    private static CloseableHttpClient createHttpClient()
    {
        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().setConnectionManager(cm);
        httpClientBuilder.addInterceptorFirst(new ContentLengthHeaderRemover());
        return httpClientBuilder.build();
    }

    @Override
    public void init() throws ServletException
    {
        super.init();
        // Properties can be configured by init parameters in the web.xml.

        acrolinxServer = getInitParameterOrDefaultValue("acrolinxServer", "http://localhost:8031/");
        if (!acrolinxServer.endsWith("/")) {
            acrolinxServer += "/";
        }
        secret = getInitParameterOrDefaultValue("secret", "secret");
    }

    private String getInitParameterOrDefaultValue(final String name, final String defaultValue)
    {
        return getInitParameter(name) != null ? getInitParameter(name) : defaultValue;
    }

    @Override
    protected void doDelete(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException
    {
        final HttpRequestBase httpMethod = new HttpDelete();
        proxyRequest(req, resp, httpMethod);

    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException
    {
        final HttpRequestBase httpMethod = new HttpPost();
        ((HttpPost) httpMethod).setEntity(new InputStreamEntity(req.getInputStream()));
        proxyRequest(req, resp, httpMethod);
    }

    @Override
    protected void doPut(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException
    {
        final HttpRequestBase httpMethod = new HttpPut();
        ((HttpPut) httpMethod).setEntity(new InputStreamEntity(req.getInputStream()));
        proxyRequest(req, resp, httpMethod);
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException
    {
        final HttpRequestBase httpMethod = new HttpGet();
        proxyRequest(req, resp, httpMethod);


    }

    private void proxyRequest(final HttpServletRequest req, final HttpServletResponse resp,
            final HttpRequestBase httpMethod) throws IOException
    {
        final URI targetURL = getTargetUrl(req);
        copyHeaders(req, httpMethod);
        modifyRequest(httpMethod, targetURL);

        // TODO: Make sure not to call the following line in case a user is not authenticated to the
        // application.
        addSingleSignOn(httpMethod);

        CloseableHttpResponse httpResponse = null;
        InputStream in = null;
        OutputStream out = null;
        try {

            httpResponse = httpClient.execute(httpMethod);
            copyResultHeader(resp, httpMethod);

            resp.setContentType(httpResponse.getEntity().getContentType().getValue());
            final int statusCode = httpResponse.getStatusLine().getStatusCode();

            resp.setStatus(statusCode);
            in = httpResponse.getEntity().getContent();

            out = resp.getOutputStream();
            resp.setContentLength((int) httpResponse.getEntity().getContentLength());
            spoolResponseBody(in, out);
            httpResponse.close();
        } catch (final ConnectException e) {
            resp.setStatus(HttpURLConnection.HTTP_BAD_GATEWAY); // 502
            e.printStackTrace();
        } finally {
            cleanUp(httpMethod, in, out);
        }
    }

    private static void cleanUp(final HttpRequestBase httpMethod, final InputStream in, final OutputStream out)
    {
        if (in != null) {
            try {
                in.close();
            } catch (final IOException ioe) {
                // ignore
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (final IOException ioe) {
                // ignore
            }
        }
        if (httpMethod != null) {
            httpMethod.releaseConnection();
        }
    }

    private static void copyResultHeader(final HttpServletResponse resp, final HttpRequestBase httpMethod)
    {
        for (final Header header : httpMethod.getAllHeaders()) {
            resp.setHeader(header.getName(), header.getValue());
        }
    }

    private static void spoolResponseBody(final InputStream in, final OutputStream out) throws IOException
    {
        if (in != null) {
            final byte[] buffer = new byte[8192];
            int n;
            while (-1 != (n = in.read(buffer))) {
                out.write(buffer, 0, n);
            }
        }
    }

    private static void modifyRequest(final HttpRequestBase httpMethod, final URI targetURL)
    {
        httpMethod.setURI(targetURL);
        setRequestHeader(httpMethod, "User-Agent", "Acrolinx Proxy");
        setRequestHeader(httpMethod, "Host", targetURL.getHost());
    }

    private static void setRequestHeader(final HttpRequestBase httpMethod, final String headerName,
            final String headerValue)
    {
        httpMethod.removeHeaders(headerName);
        httpMethod.setHeader(headerName, headerValue);
    }

    private void addSingleSignOn(final HttpRequestBase httpMethod)
    {
        setRequestHeader(httpMethod, "username", getUsernameFromApplicationSession());
        setRequestHeader(httpMethod, "password", this.secret);
    }

    private String getUsernameFromApplicationSession()
    {
        return getInitParameterOrDefaultValue("username", "username");
        // TODO: Set user name from the current applications session. This is just an example code
        // the user name comes from web.xml.
    }

    @SuppressWarnings("unchecked")
    private static void copyHeaders(final HttpServletRequest req, final HttpRequestBase httpMethod)
    {
        final Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();
            httpMethod.setHeader(headerName, req.getHeader(headerName));
        }
    }

    private URI getTargetUrl(final HttpServletRequest req) throws IOException
    {
        final String queryPart = req.getQueryString() != null ? "?" + req.getQueryString() : "";
        final String urlStr = acrolinxServer + req.getPathInfo() + queryPart;
        try {
            return new URI(urlStr);
        } catch (final Exception e) {
            throw new IOException("Not a valid URI");
        }
    }

}
