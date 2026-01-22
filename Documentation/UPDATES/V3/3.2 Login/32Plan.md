V3.2 Login Implementation Plan

1. Architectural Impact & Overview
The core change is shifting from a "Service-to-Service" auth model (using a single cached token) to a "User-Centric" model where individual users authenticate to receive unique tokens.

We will introduce a new User Domain that mirrors the existing Employee structure but is dedicated to Identity Management.

Code snippet

graph TD
    Client -->|1. Login (POST /login)| HttpVerticle
    HttpVerticle -->|2. Event Bus (users.authenticate)| UserVerticle
    UserVerticle -->|3. Offload Hashing| WorkerExecutor
    UserVerticle -->|4. Query DB| MySQL
    UserVerticle -->|5. Generate Token| AuthVerticle
    AuthVerticle -->|6. Return JWT| Client
2. Component Design
A. Data Layer: UserVerticle & UserRepository
Principle: Single Responsibility. The EmployeeVerticle should not know about users or passwords. We will create a parallel UserVerticle.

Database Schema: Create a users table with columns: id, username, password_hash, salt.

UserRepository: Handles SELECT queries using the Reactive MySQL Client.

UserVerticle:

Listens on address: users.authenticate.

Crucial Performance Detail: As noted in your requirements, password verification (hashing) is CPU-intensive. You must wrap the BCrypt verification in vertx.executeBlocking or use a dedicated WorkerExecutor to avoid blocking the Event Loop.

B. Security Layer: AuthVerticle Enhancements
Principle: Open/Closed. Extend the AuthVerticle logic without modifying its core key management.

Current State: It caches a single token for service calls.

New Capability: Add an event bus listener for auth.token.issue.

Input: username, role.

Output: A signed JWT claiming this specific identity.

Logic: The Rs256TokenService needs a method generateUserToken(String username) that uses the existing RSA Private Key to sign tokens on demand, rather than returning the cached service token.

C. Web Layer: AuthController & JWTHandler
Principle: Dependency Inversion. The Router shouldn't implement security logic; it should rely on handlers that abstract it.

AuthController:

Exposes POST /login.

Extracts JSON body (username, password).

Sends message to users.authenticate.

On success, replies with the JWT.

JWTAuthHandler (New Middleware):

Unlike the VerificationHandler which checks IP, this handler validates the Authorization: Bearer header.

How to verify the signature:
(Introspection): Call the AuthVerticle via Event Bus to verify the signature.