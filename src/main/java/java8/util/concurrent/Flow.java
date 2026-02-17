package java8.util.concurrent;

/**
 * Java 9+ の java.util.concurrent.Flow 互換(最小)。
 * java.net.http の BodyPublisher/BodySubscriber が参照するために用意。
 */
public final class Flow {

    private Flow() {}

    public static interface Subscription {
        void request(long n);
        void cancel();
    }

    public static interface Subscriber<T> {
        void onSubscribe(Subscription subscription);
        void onNext(T item);
        void onError(Throwable throwable);
        void onComplete();
    }

    public static interface Publisher<T> {
        void subscribe(Subscriber<? super T> subscriber);
    }

    /** 互換用の簡易Subscription */
    public static final class SimpleSubscription implements Subscription {
        private volatile boolean cancelled = false;

        @Override
        public void request(long n) { /* no-op */ }

        @Override
        public void cancel() { cancelled = true; }

        public boolean isCancelled() { return cancelled; }
    }
}
