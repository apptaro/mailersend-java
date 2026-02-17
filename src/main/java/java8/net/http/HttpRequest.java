package java8.net.http;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java8.util.concurrent.Flow;
import java8.util.concurrent.Flow.SimpleSubscription;

/**
 * Java 11 HttpRequest 互換(最小)
 */
public final class HttpRequest {

    private final URI uri;
    private final String method;
    private final Map<String, String> headers;
    private final BodyPublisher bodyPublisher;

    private HttpRequest(URI uri, String method, Map<String, String> headers, BodyPublisher bodyPublisher) {
        this.uri = uri;
        this.method = method;
        this.headers = headers == null ? Collections.<String, String>emptyMap() : headers;
        this.bodyPublisher = bodyPublisher;
    }

    public URI uri() {
        return uri;
    }

    public String method() {
        return method;
    }

    public Optional<BodyPublisher> bodyPublisher() {
        return Optional.ofNullable(bodyPublisher);
    }

    public Map<String, String> headers() {
        return headers;
    }

    public static Builder newBuilder(URI uri) {
        return new Builder(uri);
    }

    public static final class Builder {
        private final URI uri;
        private String method = "GET";
        private final Map<String, String> headers = new LinkedHashMap<String, String>();
        private BodyPublisher bodyPublisher;

        private Builder(URI uri) {
            this.uri = uri;
        }

        public Builder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Builder GET() {
            this.method = "GET";
            this.bodyPublisher = null;
            return this;
        }

        public Builder DELETE() {
            this.method = "DELETE";
            this.bodyPublisher = null;
            return this;
        }

        public Builder POST(BodyPublisher publisher) {
            this.method = "POST";
            this.bodyPublisher = publisher;
            return this;
        }

        public Builder PUT(BodyPublisher publisher) {
            this.method = "PUT";
            this.bodyPublisher = publisher;
            return this;
        }

        public Builder method(String method, BodyPublisher publisher) {
            this.method = method;
            this.bodyPublisher = publisher;
            return this;
        }

        public HttpRequest build() {
            return new HttpRequest(uri, method, headers, bodyPublisher);
        }
    }

    // ========= BodyPublishers / BodyPublisher =========

    public static interface BodyPublisher extends Flow.Publisher<ByteBuffer> {
        long contentLength();
    }

    public static final class BodyPublishers {
        private BodyPublishers() {}

        public static BodyPublisher ofString(String s) {
            return ofString(s, Charset.forName("UTF-8"));
        }

        public static BodyPublisher ofString(String s, Charset charset) {
            final byte[] bytes = (s == null ? "" : s).getBytes(charset == null ? Charset.forName("UTF-8") : charset);

            return new BodyPublisher() {
                @Override
                public long contentLength() {
                    return bytes.length;
                }

                @Override
                public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
                    if (subscriber == null) return;
                    SimpleSubscription sub = new SimpleSubscription();
                    subscriber.onSubscribe(sub);
                    if (sub.isCancelled()) return;
                    subscriber.onNext(ByteBuffer.wrap(bytes));
                    subscriber.onComplete();
                }
            };
        }
    }
}
