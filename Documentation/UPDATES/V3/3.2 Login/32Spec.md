## Requirements for V3.2

### Overview
A login system where mutation operations (`PUT`, `POST`, and `DELETE`) require a password and username. This system will integrate with the existing RS256 JWT authentication mechanism to secure the Reactive API.

---

1. Functional Requirements
1.1 Credential-Based Mutations
Secure Credential Submission: A new endpoint (e.g., /v4) must be exposed to securely receive username and password credentials from the client.

Access Control: Routes performing mutation operations (Creating, Updating, Deleting employees) must now require a valid JWT. The EmployeeController or Router configuration must be updated to enforce authentication on these verbs, potentially moving them under the protected /v4/ path logic.

Identity Verification: The system must validate the provided username and password against a stored record before authorizing any action.

1.2 JWT Integration
Token Issuance: The Rs256TokenService, which currently caches a single service token, must be expanded to generate unique JWTs for individual users upon successful login.

Signing Consistency: User tokens must be signed using the same RSA Private Key currently loaded in the MainVerticle. This maintains cryptographic consistency across the system.

Authorization Header: Clients must include the generated JWT in the Authorization: Bearer <token> header for all subsequent mutation requests.

Verification: The VerificationHandler must be adapted (or a new handler created) to validate these user tokens on incoming requests, checking the signature against the public key or internal validation logic.

2. Infrastructure Changes
2.1 User Repository
New Persistence Layer: A UserRepository and UserVerticle must be created to manage user data, strictly following the existing decoupled architecture where data access occurs only via the Event Bus.

Database Schema: A new database table (e.g., users) is required to store user identities. It must store a password hash (not plain text) to ensure security if the database is compromised.

Reactive Implementation: The repository must use the Vert.x MySQL Client to ensure database queries (credential lookups) are non-blocking and asynchronous.

Concurrency Safety: Similar to the EmployeeVerticle, the UserVerticle should employ a Circuit Breaker to protect against database outages and manage connection pooling efficiently.

3. Questions & Brainstorming Points
Blocking vs. Non-Blocking Hashing: Password hashing (e.g., BCrypt) is CPU-intensive. Since the current architecture emphasizes non-blocking event loops, should the hashing logic be offloaded to a WorkerExecutor to prevent freezing the Event Bus? (YES)


