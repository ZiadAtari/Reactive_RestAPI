# Demo API Integration

The Demo API is a Spring Boot companion service that acts as an external verification authority for the Reactive API. It is used to simulate real-world service-to-service communication, latency, and failure scenarios.

## Components (Demo Service)

### [DemoApplication](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/demo/src/main/java/com/example/demo/DemoApplication.java)
- **Purpose**: Standard Spring Boot entry point.
- **Port**: Runs on `8080`.

### [IpController](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/demo/src/main/java/com/example/demo/IpController.java)
- **Purpose**: Exposes verification endpoints.
- **Endpoints**:
    - `/v1/ip`: Legacy, unauthenticated endpoint.
    - `/v3/ip`: Authenticated endpoint (requires valid RS256 JWT).
- **Simulation Logic**: 
    - **Latency**: 1% chance (code) vs 40% (comment) of randomized delay (500ms - 5000ms).
    - **Faults**: 1% chance of returning HTTP 408, 429, 503, or 500.

### [SecurityConfig](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/demo/src/main/java/com/example/demo/SecurityConfig.java)
- **Purpose**: Configures Spring Security for the service.
- **Rules**:
    - Permits all traffic to `/v1/ip`.
    - Requires authentication for `/v3/**`.
    - Disables CSRF for stateless operation.

### [JwtAuthenticationFilter](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/demo/src/main/java/com/example/demo/JwtAuthenticationFilter.java)
- **Purpose**: Custom security filter for RS256 JWT verification.
- **Logic**:
    - Extracts the `Bearer` token from the `Authorization` header.
    - Verifies the signature using a hardcoded **RSA Public Key** cached in an `AtomicReference`.
    - Handles malformed keys or missing tokens with custom JSON error responses containing internal codes (e.g., `SEC_CFG_001`).



## Integration in Reactive API

The [VerificationHandler](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/web/VerificationHandler.java) orchestrates the calls to this service.

### Resilience Patterns
- **Timeouts**: The Reactive API sets a strict execution timeout.
- **Circuit Breaking**: Calls are wrapped in a `CustomCircuitBreaker`. If the Demo API begins failing (due to the 1% simulated fault injection), the circuit opens to protect the Reactive API from blocking event threads.
- **Auth Bridging**: The handler automatically requests a token from `Rs256TokenService` when accessing `V3` routes, ensuring seamless authenticated communication.
