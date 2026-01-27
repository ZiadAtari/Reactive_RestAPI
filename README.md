# Reactive REST API

## Overview
A high-performance, reactive microservice built with **Eclipse Vert.x**. This application leverages a non-blocking, event-driven architecture to handle high-concurrency workloads efficiently, utilizing specialized verticles for web, authentication, and data persistence.

## Architecture
The system is modularized into specialized Verticles, communicating strictly via the Vert.x Event Bus.

- **[AppLauncher](src/main/java/ziadatari/ReactiveAPI/main/AppLauncher.java)**: Custom entry point that configures Micrometer metrics before starting Vert.x.
- **[MainVerticle](src/main/java/ziadatari/ReactiveAPI/main/MainVerticle.java)**: Bootstraps the application and orchestrates verticle deployment.
- [HttpVerticle](src/main/java/ziadatari/ReactiveAPI/web/HttpVerticle.java): Manages the HTTP server, routing, and **isolated fault tolerance** via multiple circuit breakers.
- [AuthVerticle](src/main/java/ziadatari/ReactiveAPI/auth/AuthVerticle.java): Handles RS256 JWT issuance and verification, protecting the event loop with **non-blocking signing**.
- [EmployeeVerticle](src/main/java/ziadatari/ReactiveAPI/repository/EmployeeVerticle.java): Manages data persistence and employs database-level circuit breakers.
- **[UserVerticle](src/main/java/ziadatari/ReactiveAPI/repository/UserVerticle.java)**: Handles user credentials and authentication logic.

For detailed system documentation, please refer to the [Documentation/Systems](Documentation/Systems) directory:
- [Authentication System](Documentation/Systems/Authentication.md)
- [Router & API](Documentation/Systems/Router.md)
- [Repository Layer](Documentation/Systems/Repository.md)
- [Metrics & Monitoring](Documentation/Systems/Metric.md)
- [Exception Handling](Documentation/Systems/Exceptions.md)
- [JWT Lifecycle](Documentation/Systems/JWTLifecycle.md)

## Getting Started

### Prerequisites
- **Java 17+**
- **Maven**
- **MySQL Database**
- **Docker & Docker Compose** (for Prometheus & AlertManager)

### Environment Setup
The application is configured via environment variables. For local development, defaults are provided. In production (Docker), use `*_FILE` variables to point to mounted secrets.

| Variable | Description | Default |
| :--- | :--- | :--- |
| `DB_PASSWORD` | MySQL Connection Password | `secret` |
| `DB_PASSWORD_FILE` | Path to password file (Docker Secret) | - |
| `RSA_PRIVATE_KEY` | PEM encoded Private Key | *(Insecure Dev Key)* |
| `RSA_PRIVATE_KEY_FILE` | Path to Private Key file (Docker Secret) | - |
| `DB_HOST` | Database Hostname | `localhost` |
| `DB_PORT` | Database Port | `3306` |
| `VERIFICATION_HOST` | Demo API Hostname | `localhost` |

### Running the Application

#### Option 1: VS Code (Recommended)
The project includes a pre-configured debug launch file.
1. Open the **Run and Debug** view in VS Code.
2. Select **"Debug MainVerticle"** from the dropdown.
3. Press `F5` to start debugging.
*Note: Valid development keys/secrets are already configured in `.vscode/launch.json`.*

#### Option 2: Command Line
To build and run the application via Maven:
```bash
mvn clean package
java -jar target/reactive-rest-api-1.0.0-SNAPSHOT-fat.jar
```
The server will start on `http://localhost:8888`.

#### Option 3: Docker (Production / Full Stack)
Run the entire stack (App, MySQL, Demo API) using Docker Compose.
```bash
docker-compose up -d --build
```
This will start:
- **Reactive API**: `http://localhost:8888`
- **MySQL**: `localhost:3307` (External), `3306` (Internal)
- **Demo API**: `http://localhost:8081` (External), `8080` (Internal)

## API Usage

### Authentication
The API uses a dual-token strategy. Public routes (`/v1`) are open, while protected routes (`/v3`) require a Bearer Token.

#### 1. Login
**POST** `/login`
Authenticate to receive a JWT User Token.

**Request:**
```bash
curl -X POST http://localhost:8888/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password123"}'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJSUzI1Ni..."
}
```

### Employee Endpoints
All `/v3` endpoints below require the header: `Authorization: Bearer <token>`

#### 2. Get All Employees
**GET** `/v3/employees`

#### 3. Create Employee
**POST** `/v3/employees`
**Body:**
```json
{
  "name": "Jane Smith",
  "department": "Finance",
  "salary": 82000.0
}
```

#### 4. Batch Create Employees
**POST** `/v3/employees`
**Body:**
```json
[
  {
    "name": "Alice",
    "department": "IT",
    "salary": 75000.0
  },
  {
    "name": "Bob",
    "department": "HR",
    "salary": 68000.0
  }
]
```

#### 5. Update Employee
**PUT** `/v3/employees/:id`
**Body:**
```json
{
  "name": "John Doe",
  "department": "Engineering",
  "salary": 80000.0
}
```

#### 6. Soft Delete Employee
**DELETE** `/v3/employees/:id`
```bash
curl -X DELETE http://localhost:8888/v3/employees/550e8400-e29b... \
  -H "Authorization: Bearer <token>"
```

### Health Checks
#### 7. Liveness Probe
**GET** `/health/live`
Unauthenticated endpoint to check if the server is running.
```bash
curl -v http://localhost:8888/health/live
```
**Response:**
```json
{
  "outcome": "UP"
}
```

### Monitoring & Metrics
#### 8. Prometheus & Alerting
**GET** `/metrics`
Exposes application telemetry in Prometheus format. The system is configured for **Server-Side Histograms**, enabling accurate P95/P99 latency tracking.

**Stack:**
- **Prometheus** (Port 9090): Data collection and graphing.
- **AlertManager** (Port 9093): Proactive alerting on latency, error rates, and instance health.

See [Metric.md](Documentation/Systems/Metric.md) for a full reference and [ExpressionsList.md](ExpressionsList.md) for PromQL queries.

### Resilience Architecture
The application uses a **Two-Tier Resilience Strategy**:
1.  **Isolated Web Breakers**: Three dedicated `CustomCircuitBreakers` (`auth-login`, `v1-verify`, `v3-verify`) isolate failure domains in the web layer.
2.  **Worker Pool Offloading**: Heavy CPU tasks (RSA signing) and blocking IO (BCrypt, DB) are strictly offloaded to worker threads to keep the **Event Loop** responsive.

## Project Structure
| Package | Description |
| :--- | :--- |
| `ziadatari.ReactiveAPI.main` | Application lifecycle and boot. |
| `ziadatari.ReactiveAPI.web` | HTTP Controllers, Handlers, and Routers. |
| `ziadatari.ReactiveAPI.service` | Business logic and validation. |
| `ziadatari.ReactiveAPI.repository` | Database access and persistence. |
| `ziadatari.ReactiveAPI.auth` | Security, Token Service, and Verification. |
| `ziadatari.ReactiveAPI.dto` | Data Transfer Objects (Builder Pattern). |
| `ziadatari.ReactiveAPI.exception` | Global error handling and error codes. |

## Changelog

#### V4: Observability & Resilience
* **v4.4.0** Containerization, Docker Secrets, and Full-Stack Orchestration.
* **v4.3.0** Failure Domain Isolation & Event Loop Protection added (Isolated Circuit Breakers).
* **v4.2.0** Native Metrics Refinement & AlertManager integration (Server-side Histograms).
* **v4.1.0** Prometheus Metrics & Monitoring added.
* **v4.0.0** Health API checks added.

#### V3: Authentication
* **v3.4.0** JSON Schema Validation & Batch Updates added
* **v3.3.2** Bug Fixes
* **v3.3.1** Bug Fixes
* **v3.3.0** DTO Improvements
* **v3.2.1** Error Handling
* **v3.2.0** User Login added
* **v3.1.0** AuthVerticle added and refactored auth logic
* **v3.0.3** Expanded Documentation folder
* **v3.0.2** Polishing & Bug fixes
* **v3.0.1** Exception Handling for Invalid Keys
* **v3.0.0** JWT Authentication & API versioning added

#### V2: API Safeguards and Improvements
* **v2.2.0** Refactoring & Documentation
* **v2.1.3** Polishing & Documentation
* **v2.1.2** Fixed Race condition for Delete & Post
* **v2.1.1** Added Event Bus to isolate repository and service logic from web layer
* **v2.1.0** IP verification handler added
* **v2.0.1** Rate Limiting fixed for Race Conditions
* **v2.0.0** Rate Limiting and Circuit Breaking added

#### V1: Core Functionality
* **v1.3.0** Duplicate POST Handling & Reactivation added
* **v1.2.0** Exception Handling added
* **v1.1.0** Soft-deletion added
* **v1.0.0** CRUD Functionality Implemented

#### V0: Initial Setup
* **v0.2.0** Env Setup
* **v0.1.0** Structure Setup
