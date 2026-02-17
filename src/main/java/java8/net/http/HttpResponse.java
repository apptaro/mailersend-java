package java8.net.http;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java8.util.concurrent.Flow;

/**
 * Java 11 HttpResponse 互換(最小)
 */
public interface HttpResponse<T> {

    int statusCode();

    HttpHeaders headers();

    T body();

    // 以下は添付コードでは実質未使用だが、型整合のため定義
    default HttpRequest request() { return null; }
    default Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
    default Optional<Object> sslSession() { return Optional.empty(); }
    default java.net.URI uri() { return null; }
    default java8.net.http.HttpClient.Version version() { return java8.net.http.HttpClient.Version.HTTP_1_1; }

    // ========= BodyHandler / BodyHandlers =========

    public static interface ResponseInfo {
        int statusCode();
        HttpHeaders headers();
    }

    public static interface BodyHandler<R> {
        BodySubscriber<R> apply(ResponseInfo responseInfo);
    }

    public static final class BodyHandlers {
        private BodyHandlers() {}

        public static BodyHandler<String> ofString() {
            return ofString(Charset.forName("UTF-8"));
        }

        public static BodyHandler<String> ofString(final Charset charset) {
            return new BodyHandler<String>() {
                @Override
                public BodySubscriber<String> apply(ResponseInfo responseInfo) {
                    return BodySubscribers.ofString(charset);
                }
            };
        }
    }

    // ========= BodySubscriber / BodySubscribers =========

    public static interface BodySubscriber<R> extends Flow.Subscriber<List<ByteBuffer>> {
        CompletableFuture<R> getBody();
    }

    public static final class BodySubscribers {
        private BodySubscribers() {}

        public static BodySubscriber<String> ofString(final Charset charset) {
            final Charset cs = (charset == null) ? Charset.forName("UTF-8") : charset;

            return new BodySubscriber<String>() {
                private final CompletableFuture<String> future = new CompletableFuture<String>();
                private final java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

                @Override
                public CompletableFuture<String> getBody() {
                    return future;
                }

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    // 今回は同期投入される想定なので request は不要
                    if (subscription != null) subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(List<ByteBuffer> items) {
                    if (items == null) return;
                    for (ByteBuffer b : items) {
                        if (b == null) continue;
                        ByteBuffer bb = b.slice();
                        byte[] buf = new byte[bb.remaining()];
                        bb.get(buf);
                        baos.write(buf, 0, buf.length);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    future.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    future.complete(new String(baos.toByteArray(), cs));
                }
            };
        }
    }

    // ========= PushPromiseHandler（未使用だが型のため） =========
    public static interface PushPromiseHandler<R> { /* no-op for Java8 compat */ }
}
