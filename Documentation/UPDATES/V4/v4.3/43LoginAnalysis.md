# Login Protection Analysis: Vulnerabilities & Bottlenecks

Under extreme load, the `/login` endpoint becomes a single point of failure that can cause the entire ReactiveAPI ecosystem to deteriorate.

## 1. Current Protection State
*   **Rate Limiting**: **YES** (Covered by global `RateLimitHandler`). Limits per IP, but doesn't prevent aggregate system overload.
*   **Circuit Breaker**: **NO**. Unlike employee routes, the login route has no fail-fast mechanism.
*   **Event Loop Safety**:
    *   **User Authentication**: **PARTIAL**. `BCrypt` hashing is offloaded to worker threads, but the DB pool (size 5) can easily be exhausted.
    *   **Token Generation**: **VULNERABLE**. RSA signing in `AuthVerticle` is currently performed on the **Event Loop**.

## 2. Why the system "Deteriorates"
When thousands of login requests arrive:
1.  **Event Loop Blocking**: The `AuthVerticle` spends excessive CPU time signing JWTs. Since this is synchronous on the Event Loop, it causes "Event Loop Lag," delaying all other asynchronous operations (including health checks and metrics).
2.  **Worker Pool Saturation**: BCrypt (in `UserVerticle`) is slow by design. Large numbers of logins fill the Worker Thread Pool, delaying other background tasks.
3.  **Pool Exhaustion**: The `UserVerticle`'s small DB pool (5 connections) causes requests to queue up, increasing latency exponentially.
4.  **No Fail-Fast**: Because there is no Circuit Breaker, the system continues to accept and attempt login requests long after it has reached its breaking point, leading to a total "death spiral."

## 3. Recommended Fixes
1.  **Add Circuit Breaker**: Wrap the `/login` controller call in a dedicated `CustomCircuitBreaker` instance.
2.  **Offload Signing**: Use `vertx.executeBlocking` in `AuthVerticle` for user token generation.
3.  **Isolate Pools**: Increase the User DB pool size or ensure it's properly monitored.
4.  **Health-Based Throttling**: Configure the Circuit Breaker to trip if `BCrypt` or `RSA` latency exceeds acceptable thresholds.
