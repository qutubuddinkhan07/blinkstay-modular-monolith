package blinkstay.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.IntFunction;

/**
 * Runs several tasks at the same moment: every thread waits at a start gate,
 * the gate opens once, and all go together (maximum overlap).
 */
public final class Concurrently {

    private Concurrently() {
    }

    /**
     * What one task produced: its value, or the exception it threw.
     */
    public record Outcome<T>(T value, Throwable error) {
        public boolean failed() {
            return error != null;
        }
    }

    /**
     * @param threads       how many tasks run at once
     * @param taskForThread builds the task for thread 0, 1, 2 ... (so each can use its own guest)
     */
    public static <T> List<Outcome<T>> runTogether(int threads, IntFunction<Callable<T>> taskForThread)
            throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        try {
            List<Future<Outcome<T>>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Callable<T> task = taskForThread.apply(i);
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    startGate.await();
                    try {
                        return new Outcome<T>(task.call(), null);
                    } catch (Throwable t) {
                        return new Outcome<T>(null, t);
                    }
                }));
            }

            if (!ready.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Not all threads started within 10 s");
            }
            startGate.countDown(); // go!

            List<Outcome<T>> outcomes = new ArrayList<>();
            for (Future<Outcome<T>> future : futures) {
                try {
                    outcomes.add(future.get(30, TimeUnit.SECONDS));
                } catch (ExecutionException | TimeoutException e) {
                    throw new AssertionError("A parallel task did not finish (deadlock or very slow lock?)", e);
                }
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }
}