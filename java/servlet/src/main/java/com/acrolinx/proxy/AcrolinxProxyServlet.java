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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet acts as a reverse proxy. The Acrolinx plug-in uses this servlet
 * to communicate with the Acrolinx core server.
 */
public class AcrolinxProxyServlet extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(AcrolinxProxyServlet.class.getName());
	private static final long serialVersionUID = 2L;

	// TODO: Set this path in context of your servlet's reverse proxy implementation
	private static final String PROXY_PATH = "proxySample/proxy";

	CloseableHttpClient httpClient;

	// Can be configured by init parameters in the web.xml
	private String acrolinxURL;
	private String genericToken;

	public AcrolinxProxyServlet() {
		super();
		this.httpClient = createHttpClient();
	}

	private static class ContentLengthHeaderRemover implements HttpRequestInterceptor {
		public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
			request.removeHeaders(HTTP.CONTENT_LEN);
		}
	}

	private static CloseableHttpClient createHttpClient() {
		final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().setConnectionManager(cm);
		httpClientBuilder.disableRedirectHandling();
		httpClientBuilder.addInterceptorFirst(new ContentLengthHeaderRemover());
		return httpClientBuilder.build();
	}

	@Override
	public void init() throws ServletException {
		super.init();
		// Properties can be configured by init parameters in the web.xml.
		acrolinxURL = getInitParameterOrDefaultValue("acrolinxURL", "http://localhost:8031/").replaceAll("/$", "");
		genericToken = getInitParameterOrDefaultValue("genericToken", "secret");
	}

	private String getInitParameterOrDefaultValue(final String name, final String defaultValue) {
		return getInitParameter(name) != null ? getInitParameter(name) : defaultValue;
	}

	@Override
	protected void doDelete(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		final HttpRequestBase httpMethod = new HttpDelete();
		logger.debug("Processing delete");
		proxyRequest(req, resp, httpMethod);

	}

	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		final HttpRequestBase httpMethod = new HttpPost();
		((HttpPost) httpMethod).setEntity(new InputStreamEntity(req.getInputStream()));
		logger.debug("Processing post");
		proxyRequest(req, resp, httpMethod);
	}

	@Override
	protected void doPut(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		final HttpRequestBase httpMethod = new HttpPut();
		((HttpPut) httpMethod).setEntity(new InputStreamEntity(req.getInputStream()));
		logger.debug("Processing put");
		proxyRequest(req, resp, httpMethod);
	}

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		final HttpRequestBase httpMethod = new HttpGet();
		logger.debug("Processing get");
		proxyRequest(req, resp, httpMethod);

	}

	private void proxyRequest(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse,
			final HttpRequestBase httpRequest) throws IOException {
		final URI targetURL = getTargetUri(servletRequest);
		copyHeaders(servletRequest, httpRequest);

		// add an extra header which is needed for acrolinx to support client's
		// reverse proxy
		String baseUrl = servletRequest.getRequestURL().toString();
		baseUrl = baseUrl.substring(0, baseUrl.indexOf(PROXY_PATH) + PROXY_PATH.length());
		httpRequest.setHeader("X-Acrolinx-Base-Url", baseUrl);

		modifyRequest(httpRequest, targetURL);

		// TODO: Make sure not to call the following line in case a user is not
		// authenticated to the application.
		addSingleSignOn(httpRequest);

		CloseableHttpResponse httpResponse = null;
		InputStream in = null;
		OutputStream out = null;
		try {
			logger.debug("Sending request to Acrolinx");
			httpResponse = httpClient.execute(httpRequest);

			final int status = httpResponse.getStatusLine().getStatusCode();
			logger.debug("Response received: " + status);
			servletResponse.setStatus(status);

			Header[] clonedHeaders = httpResponse.getAllHeaders().clone();
			for (Header header : clonedHeaders) {
				if (!(header.getName().startsWith("Content-Length") || header.getName().startsWith("Content-Type"))) {
					servletResponse.setHeader(header.getName(), header.getValue());
				}
			}

			HttpEntity httpEntity = httpResponse.getEntity();

			if (httpEntity != null) {

				in = httpEntity.getContent();
				out = servletResponse.getOutputStream();

				servletResponse.setContentType(httpResponse.getEntity().getContentType().getValue());
				servletResponse.setContentLength((int) httpResponse.getEntity().getContentLength());

				spoolResponseBody(in, out);
				logger.debug("Forwarded response to client");
			}

			httpResponse.close();

		} catch (final ConnectException e) {
			servletResponse.sendError(HttpURLConnection.HTTP_BAD_GATEWAY, e.getClass().getName());
			logger.error("Error while processing proxy request: " + e.getMessage());
			e.printStackTrace();
		} finally {
			cleanUp(httpRequest, in, out);
		}
	}

	private static void cleanUp(final HttpRequestBase httpRequest, final InputStream in, final OutputStream out) {
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
		if (httpRequest != null) {
			httpRequest.releaseConnection();
		}
	}

	private static void spoolResponseBody(final InputStream in, final OutputStream out) throws IOException {
		if (in != null) {
			final byte[] buffer = new byte[8192];
			int n;
			while (-1 != (n = in.read(buffer))) {
				out.write(buffer, 0, n);
			}
		}
	}

	private static void modifyRequest(final HttpRequestBase httpRequest, final URI targetURL) {
		httpRequest.setURI(targetURL);
		setRequestHeader(httpRequest, "User-Agent", "Acrolinx Proxy");
		setRequestHeader(httpRequest, "Host", targetURL.getHost() + ":" + targetURL.getPort());
	}

	private static void setRequestHeader(final HttpRequestBase httpRequest, final String headerName,
			final String headerValue) {
		httpRequest.removeHeaders(headerName);
		httpRequest.setHeader(headerName, headerValue);
	}

	private void addSingleSignOn(final HttpRequestBase httpRequest) {
		setRequestHeader(httpRequest, "username", getUsernameFromApplicationSession());
		setRequestHeader(httpRequest, "password", this.genericToken);
	}

	private String getUsernameFromApplicationSession() {
		return getInitParameterOrDefaultValue("username", "username");
		// TODO: Set user name from the current applications session. This is just an
		// example code
		// the user name comes from web.xml.
	}

	@SuppressWarnings("unchecked")
	private void copyHeaders(final HttpServletRequest servletRequest, final HttpRequestBase httpRequest) {
		final Enumeration<String> headerNames = servletRequest.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			final String headerName = headerNames.nextElement();
			if (headerName.equalsIgnoreCase("cookie")) {
				httpRequest.addHeader(headerName, filterCookies(servletRequest.getHeader(headerName)));
			} else {
				httpRequest.addHeader(headerName, servletRequest.getHeader(headerName));
			}

		}
	}

	private String filterCookies(String rawCookie) {
		String[] rawCookieParams = rawCookie.split(";");
		String[] rawCookieNameAndValues = Arrays.stream(rawCookieParams)
				.filter(rawCookieNameAndValue -> rawCookieNameAndValue.startsWith("X-Acrolinx-"))
				.toArray(String[]::new);
		String rawAcrolinxCookies = String.join(";", rawCookieNameAndValues);
		logger.debug("Processed acrolinx cookies: " + rawAcrolinxCookies.isEmpty());
		return rawAcrolinxCookies;
	}

	private URI getTargetUri(final HttpServletRequest servletRequest) throws IOException {
		final String queryPart = servletRequest.getQueryString() != null ? "?" + servletRequest.getQueryString() : "";
		final String urlStr = acrolinxURL + servletRequest.getPathInfo() + queryPart;
		try {
			URI targetURI = new URI(urlStr);
			logger.debug("Request URL: " + targetURI.toString());
			return targetURI;
		} catch (final URISyntaxException e) {
			throw new IOException("Not a valid URI");
		}
	}

}
