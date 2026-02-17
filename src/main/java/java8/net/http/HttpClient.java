package java8.net.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.ProxySelector;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import java8.util.concurrent.Flow;

/**
 * Java 11 HttpClient 互換(最小)
 * 内部実装は HttpURLConnection を使用。
 */
public class HttpClient {

    public enum Version { HTTP_1_1, HTTP_2 }
    public enum Redirect { NEVER, ALWAYS, NORMAL }

    public static HttpClient newHttpClient() {
        return new HttpClient();
    }

    // ====== 添付コード(HttpClientVcr)がoverrideしているメソッド群（最小のダミー） ======
    public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
    public Optional<Duration> connectTimeout() { return Optional.empty(); }
    public Redirect followRedirects() { return Redirect.NEVER; }
    public Optional<ProxySelector> proxy() { return Optional.empty(); }
    public SSLContext sslContext() { return null; }
    public SSLParameters sslParameters() { return new SSLParameters(); }
    public Optional<Authenticator> authenticator() { return Optional.empty(); }
    public Version version() { return Version.HTTP_1_1; }
    public Optional<Executor> executor() { return Optional.empty(); }

    // ====== 主要：send ======
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {

        if (request == null) throw new IllegalArgumentException("request is null");
        if (responseBodyHandler == null) throw new IllegalArgumentException("responseBodyHandler is null");

        URL url = request.uri().toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(request.method());

        // headers
        for (Map.Entry<String, String> e : request.headers().entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                conn.setRequestProperty(e.getKey(), e.getValue());
            }
        }

        // body
        if (request.bodyPublisher().isPresent()) {
            conn.setDoOutput(true);
            byte[] bodyBytes = bodyPublisherToBytes(request.bodyPublisher().get());
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            conn.getOutputStream().write(bodyBytes);
            conn.getOutputStream().flush();
            conn.getOutputStream().close();
        }

        int status = conn.getResponseCode();

        Map<String, List<String>> headerFields = conn.getHeaderFields();
        if (headerFields == null) headerFields = Collections.emptyMap();

        final HttpHeaders headers = HttpHeaders.of(headerFields, (a,b) -> true);

        HttpResponse.ResponseInfo info = new HttpResponse.ResponseInfo() {
            @Override public int statusCode() { return status; }
            @Override public HttpHeaders headers() { return headers; }
        };

        HttpResponse.BodySubscriber<T> subscriber = responseBodyHandler.apply(info);

        // read body (error stream優先)
        InputStream in = null;
        try {
            in = (status >= 400) ? conn.getErrorStream() : conn.getInputStream();
            if (in == null) in = conn.getInputStream();
        } catch (IOException ex) {
            in = conn.getErrorStream();
        }

        byte[] bytes = readAllBytes(in);

        // subscriber に投入（String handler想定だが、汎用でList<ByteBuffer>に詰める）
        java.util.List<java.nio.ByteBuffer> chunks =
                java.util.Collections.singletonList(java.nio.ByteBuffer.wrap(bytes));

        subscriber.onSubscribe(new Flow.SimpleSubscription());
        subscriber.onNext(chunks);
        subscriber.onComplete();

        T body = subscriber.getBody().join();

        return new SimpleHttpResponse<T>(status, headers, body);
    }

    // 互換のため（SDKでは未使用）
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        CompletableFuture<HttpResponse<T>> f = new CompletableFuture<HttpResponse<T>>();
        try {
            f.complete(send(request, responseBodyHandler));
        } catch (Throwable t) {
            f.completeExceptionally(t);
        }
        return f;
    }

    // 互換のため（SDKでは未使用）
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                           HttpResponse.BodyHandler<T> responseBodyHandler,
                                                           HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        return sendAsync(request, responseBodyHandler);
    }

    // ====== internal helpers ======
    private static byte[] bodyPublisherToBytes(HttpRequest.BodyPublisher publisher) {
        // BodyPublisher は subscribe すると ByteBuffer を 1回流す実装にしてあるので、それを回収
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        publisher.subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override public void onSubscribe(Flow.Subscription subscription) {
                if (subscription != null) subscription.request(Long.MAX_VALUE);
            }
            @Override public void onNext(java.nio.ByteBuffer item) {
                if (item == null) return;
                java.nio.ByteBuffer bb = item.slice();
                byte[] buf = new byte[bb.remaining()];
                bb.get(buf);
                try { baos.write(buf); } catch (IOException ignored) {}
            }
            @Override public void onError(Throwable throwable) { /* ignore */ }
            @Override public void onComplete() { /* done */ }
        });
        return baos.toByteArray();
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        if (in == null) return new byte[0];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) != -1) {
            baos.write(buf, 0, r);
        }
        in.close();
        return baos.toByteArray();
    }

    // ====== Simple HttpResponse impl ======
    private static final class SimpleHttpResponse<T> implements HttpResponse<T> {
        private final int status;
        private final HttpHeaders headers;
        private final T body;

        private SimpleHttpResponse(int status, HttpHeaders headers, T body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }

        @Override public int statusCode() { return status; }
        @Override public HttpHeaders headers() { return headers; }
        @Override public T body() { return body; }
    }
}
