package com.acrolinx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;

import org.apache.http.HttpException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpDelete;

import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

/**
 * This servlet acts as a reverse proxy. The Acrolinx plug-in uses this
 * servlet to communicate with the Acrolinx core server.
 */
public class AcrolinxProxyServlet extends HttpServlet {
	private static final long serialVersionUID = 2L;

	CloseableHttpClient httpClient;

	// Can be configured by init parameters in the web.xml
	private String acrolinxCoreServer;
	private String secret;
	private String username;

	public AcrolinxProxyServlet() {
		super();
		this.httpClient = createHttpClient();

	}

	private static class ContentLengthHeaderRemover implements HttpRequestInterceptor {
		public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
			request.removeHeaders(HTTP.CONTENT_LEN);		}

	}

	private CloseableHttpClient createHttpClient() {
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().setConnectionManager(cm);
		httpClientBuilder.addInterceptorFirst(new ContentLengthHeaderRemover());
		return httpClientBuilder.build();
	}

	@Override
	public void init() throws ServletException {
		super.init();
		// Can be configured by init parameters in the web.xml
		username = getInitParameterOrDefaultValue("username", "username");
		acrolinxCoreServer = getInitParameterOrDefaultValue("acrolinxCoreServer", "http://localhost:8031/");
		secret = getInitParameterOrDefaultValue("secret", "secret");
	}

	private String getInitParameterOrDefaultValue(String name, String defaultValue) {
		return getInitParameter(name) != null ? getInitParameter(name) : defaultValue;
	}

	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		HttpRequestBase httpMethod = new HttpDelete();
		try {
			proxyRequest(req, resp, httpMethod);
		} catch (HttpException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		HttpRequestBase httpMethod = new HttpPost();
		((HttpPost) httpMethod).setEntity(new InputStreamEntity(req.getInputStream()));
		try {
			proxyRequest(req, resp, httpMethod);
		} catch (HttpException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		HttpRequestBase httpMethod = new HttpGet();
		try {
			proxyRequest(req, resp, httpMethod);
		} catch (HttpException e) {
			e.printStackTrace();
		}
	}

	private void proxyRequest(HttpServletRequest req, HttpServletResponse resp, HttpRequestBase httpMethod)
			throws IOException, HttpException {
		final URI targetURL = getTargetUrl(req);
		copyHeaders(req, httpMethod);
		try {
			modifyRequest(httpMethod, targetURL);
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		addSingleSignOn(req, httpMethod);

		CloseableHttpResponse httpResponse = null;
		InputStream in = null;
		OutputStream out = null;
		try {

			httpResponse = httpClient.execute(httpMethod);
			copyResultHeader(resp, httpMethod);

			resp.setContentType(httpResponse.getEntity().getContentType().getValue());
			int statusCode = httpResponse.getStatusLine().getStatusCode();

			resp.setStatus(statusCode);
			in = httpResponse.getEntity().getContent();

			out = resp.getOutputStream();
			int size = spoolResponseBody(targetURL, in, out);
			resp.setContentLength(size);
			httpResponse.close();
		} catch (ConnectException e) {
			resp.setStatus(HttpURLConnection.HTTP_BAD_GATEWAY); // 502
			e.printStackTrace();
		} finally {
			cleanUp(httpMethod, in, out);
		}
	}

	private void cleanUp(HttpRequestBase httpMethod, InputStream in, OutputStream out) {
		if (in != null) {
			try {
				in.close();
			} catch (IOException ioe) {
				// ignore
			}
		}
		if (out != null) {
			try {
				out.close();
			} catch (IOException ioe) {
				// ignore
			}
		}
		if (httpMethod != null) {
			httpMethod.releaseConnection();
		}
	}

	private void copyResultHeader(HttpServletResponse resp, HttpRequestBase httpMethod) {
		for (Header header : httpMethod.getAllHeaders()) {
			resp.setHeader(header.getName(), header.getValue());
		}
	}

	private int spoolResponseBody(final URI targetURL, InputStream in, OutputStream out) throws IOException {
		int offset = 0;
		if (in != null) {
			byte[] buffer = new byte[8192];
			int n;
			while (-1 != (n = in.read(buffer))) {
				out.write(buffer, 0, n);
				offset = offset + n;
			}
		}
		return offset;
	}

	private void modifyRequest(HttpRequestBase httpMethod, final URI targetURL) throws URISyntaxException {
		httpMethod.setURI(targetURL);
		setRequestHeader(httpMethod, "User-Agent", "Acrolinx Proxy");
	}

	private void setRequestHeader(HttpRequestBase httpMethod, String headerName, String headerValue) {
		httpMethod.removeHeaders(headerName);
		httpMethod.setHeader(headerName, headerValue);
	}

	private void addSingleSignOn(HttpServletRequest req, HttpRequestBase httpMethod) {
		setRequestHeader(httpMethod, "username", username);
		setRequestHeader(httpMethod, "password", this.secret);
	}

	@SuppressWarnings("unchecked")
	private void copyHeaders(HttpServletRequest req, HttpRequestBase httpMethod) {
		Enumeration<String> headerNames = req.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			if (headerName.toLowerCase().equals("cookie")) {
				continue;
			}
			httpMethod.setHeader(headerName, req.getHeader(headerName));
		}
	}

	private URI getTargetUrl(HttpServletRequest req) throws IOException {
		final URI targetURL;
		try {
			String queryPart = req.getQueryString() != null ? "?" + req.getQueryString() : "";
			targetURL = new URI(acrolinxCoreServer + req.getPathInfo() + queryPart);
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
		return targetURL;
	}

}
