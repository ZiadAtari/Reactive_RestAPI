Plan: SOLID Login & Verification Protections
Goal
Strengthen the system's resilience by isolating failure domains and protecting the Event Loop from CPU-intensive cryptographic operations.

Proposed Changes
1. Observability Improvements (
CustomCircuitBreaker.java
)
[MODIFY]: Add a String name parameter to the constructor.
[MODIFY]: Update the Micrometer gauge to use Tags.of("name", name) for distinct monitoring of each breaker instance.
2. Failure Domain Isolation (
HttpVerticle.java
)
[MODIFY]: Instantiate three distinct circuit breakers:
v1VerificationCB ("v1-verify"): Optimized for legacy verification latency.
v3VerificationCB ("v3-verify"): Optimized for authenticated verification latency.
loginCB ("auth-login"): Dedicated to protecting the authentication pipeline.
[MODIFY]: Refactor 
AuthController
 to accept 
CustomCircuitBreaker
 in its constructor.
[MODIFY]: Pass the loginCB to 
AuthController
.
[MODIFY]: Pass v1VerificationCB and v3VerificationCB to their respective 
VerificationHandler
 instances.
3. Controller Protection (
AuthController.java
)
[MODIFY]: Wrap the entire 
login
 logic (Event Bus call) inside the injected loginCB.execute(...). This prevents the controller from overwhelming the Event Bus when the system is struggling.
4. Event Loop Protection (
AuthVerticle.java
 & 
Rs256TokenService.java
)
[MODIFY]: Update 
Rs256TokenService
 constructor to accept 
Vertx
.
[MODIFY]: Internalize the thread-blocking safety: Move the vertx.executeBlocking call inside Rs256TokenService.generateUserToken. This ensures the service method is truly asynchronous and safe, rather than relying on the caller to handle concurrency correctly (Encapsulation Principle).
Verification Plan
Prometheus: Verify three distinct series for circuit_breaker_state with different labels.
Load Test: Overload /login and verify that /health/live and /v1/employees remain responsive (proving Event Loop safety).