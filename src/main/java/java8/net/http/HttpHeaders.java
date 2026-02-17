package java8.net.http;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Java 11 HttpHeaders 互換(最小)
 */
public final class HttpHeaders {

    private final Map<String, List<String>> headers;

    private HttpHeaders(Map<String, List<String>> headers) {
        this.headers = headers == null ? Collections.<String, List<String>>emptyMap() : headers;
    }

    public static HttpHeaders of(Map<String, List<String>> headerMap, BiPredicate<String, String> filter) {
        // filter は今回の利用範囲では無視（常に受け入れ）
        return new HttpHeaders(headerMap);
    }

    public Optional<String> firstValue(String name) {
        if (name == null) return Optional.empty();
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                List<String> v = e.getValue();
                if (v != null && !v.isEmpty() && v.get(0) != null) return Optional.of(v.get(0));
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public Map<String, List<String>> map() {
        return headers;
    }
}
