# Reactive_RestAPI v3.0: Detailed Implementation Specification

This document explicitly details the architectural changes, security implementations, and versioning strategies introduced in Version 3.0.

## 1. Security Architecture: Asymmetric JWT (RS256)

We have moved from an open/trust-based model to a **Zero-Trust** model for service-to-service communication.

### 1.1 Cryptography Standards
-   **Algorithm**: `RS256` (RSA Signature with SHA-256).
-   **Key Size**: 2048-bit.
-   **Format**: `PKCS#8` (Private Key), `X.509` (Public Key).
-   **Rationale**: Asymmetric encryption allows `ReactiveAPI` to sign tokens without sharing the private secret with the `demo` service. The `demo` service only needs the public key to verify, meaning if the `demo` service is compromised, the attacker cannot forge new tokens.

### 1.2 Token Lifecycle (`Rs256TokenService`)
The token generation process is optimized for high-throughput environments:
1.  **Generation**: A JWT is created with:
    -   `sub`: "ReactiveAPI" (Subject).
    -   `iat`: Current Timestamp (Issued At).
    -   `exp`: Current Timestamp + 1 Hour (Expiration).
    -   `signature`: Signed using the **Private Key**.
2.  **Caching Strategy**: The generated token is cached in an `AtomicReference` in memory.
3.  **Refusal to Re-sign**: Subsequent calls return the cached token.
4.  **Auto-Refresh**: The service checks validitiy on every call. If the token is within 5 minutes of expiring, a fresh token is generated and the cache is updated atomically.

---

## 2. API Versioning & Routing Strategy

We implemented a **Dual-Path** strategy to support legacy clients while enforcing security for new integrations.

### 2.1 Router Configuration (`HttpVerticle`)

| Route Prefix | Access Level | Description | Handler Configuration |
| :--- | :--- | :--- | :--- |
| **/v1/** | **Public / Legacy** | Unauthenticated access. Proxies to legacy backend. | `VerificationHandler(auth=false)` |
| **/v3/** | **Secure / Modern** | Authenticated access. Proxies to secure backend. | `VerificationHandler(auth=true)` |

### 2.2 Middleware Logic (`VerificationHandler`)
The handler was refactored from a simple proxy to a smart decision engine:
```java
// Logic Flow
if (requireAuth) {
    1. Retrieve valid JWT from TokenService (Async).
    2. Inject "Authorization: Bearer <token>" header.
    3. Call backend endpoint (e.g., /v3/ip).
} else {
    1. Call backend endpoint directly (e.g., /v1/ip) with NO auth header.
}
```

---

## 3. Scalability & Deployment Mechanics

A critical challenge in Vert.x is managing stateful services (like our Token Cache) across stateless Event Loops.

### 3.1 The "Singleton" Problem
By default, `vertx.deployVerticle(HttpVerticle.class.getName(), opts.setInstances(4))` creates 4 totally separate instances of `HttpVerticle`. If we initialized `TokenService` inside `HttpVerticle`, we would have **4 separate caches** and generate **4x the tokens**.

### 3.2 The Solution: Dependency Injection Loop
We modified `MainVerticle` to control the instantiation graph manually:

1.  **Instantiate Singleton**: We create **one** instance of `Rs256TokenService` in `MainVerticle`.
2.  **Manual Scaling Loop**:
    ```java
    // Deployment Loop
    for (int i = 0; i < CPU_CORES; i++) {
        // We inject the SAME service instance into every verticle
        HttpVerticle verticle = new HttpVerticle(sharedTokenService);
        vertx.deployVerticle(verticle, new DeploymentOptions().setInstances(1));
    }
    ```
3.  **Result**: All Event Loops share the exact same `AtomicReference` cache. Token generation happens once per hour, regardless of whether you have 1 CPU or 64 CPUs.

---

## 4. Resilience Patterns

### 4.1 Circuit Breaker Integration
Both v1 and v3 flows are wrapped in the `CustomCircuitBreaker`.
-   **Threshold**: 5 failures trips the breaker.
-   **Reset**: 800ms cooldown.
-   **Timeout**: 500ms per verification call.
-   **Behavior**: If the `demo` service goes down, `ReactiveAPI` fails fast without tying up resources waiting for timeouts.

### 4.2 Error Handling Standardization
We mapped external errors to internal domain exceptions to prevent leaking implementation details:
-   `500...599` from Demo -> `ServiceException(INTERNAL_SERVER_ERROR)` -> Circuit Breaker Trip.
-   `400...499` from Demo -> `ServiceException(IP_VERIFICATION_FAILED)` -> User receives 403/400.

---

## 5. Spring Boot Security (Demo Service)

### 5.1 Security Filter Chain
We moved away from default security to a custom chain:
-   **CSRF**: Disabled (Stateless API).
-   **Session**: Stateless (No JSESSIONID).
-   **FilterChain**:
    -   Reqs to `/v3/**` -> **Authenticated**.
    -   Reqs to `/v1/**` -> **PermitAll**.

### 5.2 Key Sanitization
To improve Developer Experience (DX), we added a "Sanitizer" to the `JwtAuthenticationFilter`. It automatically strips:
-   `-----BEGIN PUBLIC KEY-----` / `-----END PUBLIC KEY-----` headers.
-   Newlines (`\n`, `\r`) and whitespaces.
-   This ensures that copy-pasting keys from a terminal or PEM file does not crash the application during startup.