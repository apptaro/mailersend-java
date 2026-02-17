/*************************************************
 * MailerSend Java SDK
 * https://github.com/mailersend/mailersend-java
 * 
 * @author MailerSend <support@mailersend.com>
 * https://mailersend.com
 **************************************************/
package com.mailersend.sdk.vcr;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java8.net.http.HttpClient.Version;
import java8.net.http.HttpHeaders;
import java8.net.http.HttpRequest;
import java8.net.http.HttpResponse;

public class HttpClientVcrResponse implements HttpResponse<Object> {
	
	public String requestHash;
	
	public String body;
	
	public Map<String, List<String>> headers;
	
	public int statusCode;

	@Override
	public int statusCode() {
		return this.statusCode;
	}

	@Override
	public HttpRequest request() {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Optional previousResponse() {
		return null;
	}

	@Override
	public HttpHeaders headers() {
		return HttpHeaders.of(headers, (s1,s2) -> true);
	}

	@Override
	public Object body() {
		return this.body;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Optional sslSession() {
		return null;
	}

	@Override
	public URI uri() {
		return null;
	}

	@Override
	public Version version() {
		return null;
	}

}
