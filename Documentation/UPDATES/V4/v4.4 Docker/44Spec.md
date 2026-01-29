
---

# v4.4 Specification: Containerization & Secure Secret Management

**Version:** 4.4.0
**Focus:** Dockerization, Secret Security (Docker Secrets), and Configuration Hardening
**Status:** Draft

---

## 1. Overview

v4.4 focuses on transitioning the application to a **Containerized Architecture** while remediating security vulnerabilities related to plain-text environment variables.

Currently, sensitive data (RSA Keys, DB Passwords) is loaded directly from Environment Variables in `MainVerticle`. This is insecure in containerized environments as variables are visible via `docker inspect`. We will implement a **File-Based Secret Loading** pattern compatible with Docker Swarm and Kubernetes.

---

## 2. Business Requirements (BR)

* **BR-01 Secure Secret Injection:** Secrets (RSA Private Keys, Database Passwords) must **never** be passed as plain-text environment variables in production. They must be mounted as files.
* **BR-02 Cloud-Native Distribution:** The application must be packaged as a lightweight, standalone Docker image including the JRE and application JAR.
* **BR-03 Environment Parity:** The configuration logic must support *both* local development (Env Vars) and Production (File Mounts) without code changes.

---

## 3. Scope

### In Scope

* **Application Logic:**
* Refactor `MainVerticle.java` to support the `_FILE` suffix convention.


* **Infrastructure:**
* Create a production-ready `Dockerfile`.
* Create a `docker-compose.yml` utilizing **Docker Secrets**.


* **Security:**
* Migration of the RSA Private Key from a hardcoded string/env var to a `.pem` file.



### Out of Scope

* **CI/CD:** Automated building of the Docker image is excluded.

---

## 4. Functional Requirements

### FR-01: File-Based Secret Precedence

The configuration loader in `MainVerticle` must adopt a "Check File First" strategy.
For a configuration key `CONFIG_KEY` (e.g., `DB_PASSWORD`), the system must:

1. Check for an environment variable named `CONFIG_KEY_FILE` (e.g., `DB_PASSWORD_FILE`).
2. If present, read the contents of the file path specified in that variable.
3. If the file variable is absent, fall back to the standard `CONFIG_KEY` environment variable (Legacy/Dev mode).

### FR-02: Handling Multiline Secrets

The system must correctly handle the **RSA Private Key** as a file.

* The system must read the `.pem` file content into the configuration object.
* The existing `PemUtils.normalizePem` logic must remain in place to handle whitespace issues, ensuring the loaded string is a valid PEM.

### FR-03: Docker Image Specifications

The Docker image must:

* Use a multi-stage build (Maven Build -> OpenJDK Runtime).
* Expose port `8888`.
* Run as a non-root user (if possible) for security.

---

## 5. Required Changes

### `MainVerticle.java`

* **Add**: A helper method `loadSecret(String key)` to implement FR-01.
* **Modify**: Update `start()` to use `loadSecret("DB_PASSWORD")` and `loadSecret("RSA_PRIVATE_KEY")`.
* **Refactor**: Remove the hardcoded "INSECURE default" keys for production builds, or ensure they strictly only trigger if no other config is found.

### `Dockerfile` (New File)

* **Stage 1**: `maven:3.8-openjdk-17` to compile the Fat JAR.
* **Stage 2**: `openjdk:17-slim` (or `eclipse-temurin`) to run the application.

### `docker-compose.yml` (New File)

* **Services**: `reactive-api`, `mysql`.
* **Secrets**: Define `db_password` and `rsa_private_key` pointing to local files.
* **Environment**: Set `DB_PASSWORD_FILE=/run/secrets/db_password`.