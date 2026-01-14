package ziadatari.ReactiveAPI.web;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.ServiceException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A custom, reactive implementation of the Circuit Breaker pattern.
 * <p>
 * This class ensures system stability by preventing cascading failures when
 * external services are flaky.
 * It manages three states:
 * <ul>
 * <li><b>CLOSED</b>: Normal operation, requests are allowed.</li>
 * <li><b>OPEN</b>: Failing state, requests are rejected immediately (Fast
 * Fallback).</li>
 * <li><b>HALF_OPEN</b>: Probing state, limited requests are allowed to test
 * recovery.</li>
 * </ul>
 */
public class CustomCircuitBreaker {

    private enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    private final Vertx vertx;
    private final long executionTimeoutMs;
    private final long resetTimeoutMs;
    private final int maxFailures;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);

    /**
     * Creates a new CustomCircuitBreaker.
     *
     * @param vertx              the Vert.x instance for scheduling timers
     * @param executionTimeoutMs max time (in ms) to wait for an operation before
     *                           considering it a failure
     * @param resetTimeoutMs     time (in ms) to wait in OPEN state before
     *                           attempting HALF_OPEN
     * @param maxFailures        number of consecutive failures required to trip the
     *                           circuit
     */
    public CustomCircuitBreaker(Vertx vertx, long executionTimeoutMs, long resetTimeoutMs, int maxFailures) {
        this.vertx = vertx;
        this.executionTimeoutMs = executionTimeoutMs;
        this.resetTimeoutMs = resetTimeoutMs;
        this.maxFailures = maxFailures;
    }

    /**
     * Executes the given command within the circuit breaker protection.
     *
     * @param command a Supplier returning a Future, representing the asynchronous
     *                operation
     * @param <T>     the type of the result
     * @return a Future containing the result or a failure
     */
    public <T> Future<T> execute(Supplier<Future<T>> command) {
        State currentState = state.get();

        if (currentState == State.OPEN) {
            // Fail Fast
            return Future.failedFuture(new ServiceException(ErrorCode.SERVICE_UNAVAILABLE, "Circuit Breaker is OPEN"));
        }

        // For HALF_OPEN, we only allow one probe at a time (simplistic optimistic
        // locking by just proceeding)
        // A more complex implementation might use a semaphore, but for this custom
        // impl, we let it race slightly
        // or we could enforce single-entry. Let's stick to simple pass-through
        // monitoring.

        Promise<T> promise = Promise.promise();

        // Timer to enforce execution timeout
        long timerId = vertx.setTimer(executionTimeoutMs, id -> {
            if (!promise.future().isComplete()) {
                String msg = "Operation timed out after " + executionTimeoutMs + "ms";
                handleFailure(new RuntimeException(msg));
                promise.tryFail(new ServiceException(ErrorCode.SERVICE_UNAVAILABLE, msg));
            }
        });

        try {
            command.get()
                    .onSuccess(result -> {
                        vertx.cancelTimer(timerId);
                        handleSuccess();
                        promise.tryComplete(result);
                    })
                    .onFailure(err -> {
                        vertx.cancelTimer(timerId);
                        handleFailure(err);
                        promise.tryFail(err);
                    });
        } catch (Exception e) {
            // Guard against synchronous exceptions in the supplier
            vertx.cancelTimer(timerId);
            handleFailure(e);
            promise.tryFail(e);
        }

        return promise.future();
    }

    /**
     * Handles a successful execution.
     * Resets failure count and closes the circuit if it was HALF_OPEN.
     */
    private void handleSuccess() {
        // If we were exploring in HALF_OPEN, or just normal CLOSED execution
        if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
            System.out.println("CIRCUIT BREAKER: CLOSED (Service recovered)");
            failureCount.set(0);
        } else if (state.get() == State.CLOSED) {
            // Keep failures at 0 while healthy
            failureCount.set(0);
        }
    }

    /**
     * Handles a failed execution (error or timeout).
     * Increments failure count and trips circuit if threshold reached.
     */
    private void handleFailure(Throwable t) {
        // If already OPEN, nothing to do (though we shouldn't get here often if we
        // blocked entry)
        if (state.get() == State.OPEN)
            return;

        int currentFailures = failureCount.incrementAndGet();

        // If we in HALF_OPEN, a single failure trips it back to OPEN
        if (state.get() == State.HALF_OPEN) {
            tripToOpen();
            return;
        }

        // In CLOSED state, check threshold
        if (currentFailures >= maxFailures) {
            tripToOpen();
        }
    }

    private void tripToOpen() {
        if (state.compareAndSet(State.CLOSED, State.OPEN) || state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
            System.out.println("CIRCUIT BREAKER: OPENED (Failures reached threshold)");

            // Schedule automatic transition to HALF_OPEN
            vertx.setTimer(resetTimeoutMs, id -> {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    System.out.println("CIRCUIT BREAKER: HALF-OPEN (Testing recovery...)");
                    // We can choose to reset failure count here or keep it 'hot'.
                    // Let's reset it so we count fresh failures in HALF_OPEN if we want multiple
                    // probes,
                    // but typically HALF_OPEN is stricter. We'll leave it as is, handleFailure
                    // checks state.
                }
            });
        }
    }
}
