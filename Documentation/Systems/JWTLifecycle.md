# JWT Lifecycle Documentation

This document traces the creation, usage, and lifecycle of JSON Web Tokens (JWT) within the ReactiveAPI system. The system employs a **Dual-Token Strategy** handling both User Tokens (for client authorization) and Service Tokens (for inter-service communication).

## User JWT Lifecycle
The User JWT is a short-lived bearer token issued to clients upon successful authentication. It allows the user to access protected resources.

### 1. Initiation: Login Request
The process begins when a client POSTs credentials to the login endpoint.
*   **Class**: `ziadatari.ReactiveAPI.web.AuthController`
*   **Method**: `login(RoutingContext ctx)`
*   **Action**: 
    1.  Receives `username` and `password` from the request body.
    2.  Sends an event bus message to `users.authenticate` to verify credentials.
    3.  If valid, sends an event bus message to `auth.token.issue` to request a new token.

### 2. Event Bus Routing
The `AuthVerticle` listens for token issuance requests.
*   **Class**: `ziadatari.ReactiveAPI.auth.AuthVerticle`
*   **Method**: `handleTokenIssue(Message<Object> message)`
*   **Address**: `auth.token.issue`
*   **Action**: 
    1.  Extracts the `username` from the message body.
    2.  Calls the `TokenService` to generate the token.
    3.  Replies to the event bus message with the generated token string.

### 3. Token Generation
The actual JWT creation logic resides in the service layer.
*   **Class**: `ziadatari.ReactiveAPI.auth.Rs256TokenService`
*   **Method**: `generateUserToken(String username)`
*   **Action**:
    1.  **Configuration**: Sets algorithm to `RS256` and expiry to **15 minutes** (900 seconds).
    2.  **Claims**:
        *   `sub`: The `username` provided.
        *   `role`: Hardcoded to `"user"`.
    3.  **Signing**: Uses the `JWTAuth` provider (Vert.x Auth) initialized with the **RSA Private Key**.
    4.  **No Caching**: Unlike service tokens, user tokens are generated on-demand for every login.

### 4. Response
*   **Flow**: `Rs256TokenService` returns the token -> `AuthVerticle` replies -> `AuthController` receives reply.
*   **Final Action**: `AuthController` sends a HTTP 200 response with the JSON payload: `{"token": "..."}`.

---

## Service JWT Lifecycle
The Service JWT is used by the ReactiveAPI to authenticate itself when calling the external DemoAPI (e.g., for IP verification).

### 1. Initiation: Verification Request
When a user requests an IP verification, the system needs to call the DemoAPI.
*   **Class**: `ziadatari.ReactiveAPI.auth.VerificationHandler` (or `HttpVerticle` logic dependent on version)
*   **Action**: Sends a request to the event bus address `auth.token.get`.

### 2. Caching Strategy
The service token is long-lived and cached to improve performance.
*   **Class**: `ziadatari.ReactiveAPI.auth.Rs256TokenService`
*   **Method**: `getToken()`
*   **Logic**:
    *   **Check Cache**: Returns the cached token if it is valid and not within the refresh buffer (5 minutes before expiry).
    *   **Generate New**: If expired or missing, calls `generateNewToken(long now)`.

### 3. Generation Details
*   **Method**: `generateNewToken(long now)`
*   **Claims**: `sub` = `"ReactiveAPI-Client"`.
*   **Expiry**: 1 hour.
*   **Signing**: Uses the same RSA Private Key as user tokens.

---

## Key Configuration & Infrastructure

### AuthVerticle Initialization
*   **Class**: `ziadatari.ReactiveAPI.auth.AuthVerticle`
*   **Method**: `start(Promise<Void> startPromise)`
*   **Responsibilities**:
    1.  Reads `rsa_private_key` from the configuration.
    2.  Normalizes the PEM key using `PemUtils.normalizePem`.
    3.  Initializes the `JWTAuth` provider with `RS256` algorithm and the private key buffer.
    4.  Instantiates `Rs256TokenService` with this provider.

### Dependencies
*   **Library**: `vertx-auth-jwt`
*   **Algorithm**: RS256 (RSA Signature with SHA-256)
