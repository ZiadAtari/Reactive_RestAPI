Based on the review of the source code (`src`) and the requirements for **v4.1 (Metrics & Monitoring)**, here is the detailed implementation plan.

### **1. Gap Analysis & Critical Findings**

* **Startup Configuration Gap (`MainVerticle.java`)**:
    * **Current State:** The application uses the default `io.vertx.core.Launcher` (defined in `pom.xml`). `MainVerticle` only runs *after* Vert.x has started.
    * **Problem:** Metrics must be configured in `VertxOptions` **before** the Vert.x instance is created. You cannot enable Micrometer inside `MainVerticle.start()`.
    * **Solution:** We must create a custom **Launcher** class to inject the `MicrometerMetricsOptions` at startup.

* **Dependencies (`pom.xml`)**:
    * **Finding:** `vertx-micrometer-metrics` and `micrometer-registry-prometheus` are **already present** in `pom.xml`. No updates needed here.

* **Instrumentation Targets**:
    * **`HttpVerticle.java`**: Needs the `/metrics` endpoint.
    * **`AuthController.java`**: Needs logic to count login attempts (currently just handles flow).
    * **`CustomCircuitBreaker.java`**: Needs to expose its internal `AtomicReference` state to Micrometer.

* **Development Environment (`launch.json`)**:
    * **Finding:** The "Debug MainVerticle" configuration currently uses `io.vertx.core.Launcher`.
    * **Update Required:** This must be updated to use the new `ziadatari.ReactiveAPI.main.AppLauncher` to ensure metrics are active during debugging.

---

### **2. Implementation Plan**

#### **Step 1: Create Custom Launcher (Infrastructure Change)**

**New File:** `src/main/java/ziadatari/ReactiveAPI/main/AppLauncher.java`
This is required to enable metrics before `MainVerticle` deploys.

```java
package ziadatari.ReactiveAPI.main;

import io.vertx.core.Launcher;
import io.vertx.core.VertxOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.Label;
import java.util.EnumSet;

public class AppLauncher extends Launcher {

  public static void main(String[] args) {
    new AppLauncher().dispatch(args);
  }

  @Override
  public void beforeStartingVertx(VertxOptions options) {
    // 1. Enable Metrics
    options.setMetricsOptions(
      new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions()
          .setEnabled(true)
          .setPublishQuantiles(true))
        .setLabels(EnumSet.of(Label.HTTP_METHOD, Label.HTTP_CODE, Label.HTTP_ROUTE)) // CRITICAL for FR-02
        .setEnabled(true)
    );
  }
}
```

#### **Step 2: Update Configuration**

**File:** `pom.xml`
Change the launcher class to the new one so Maven uses it.

```xml
<launcher.class>ziadatari.ReactiveAPI.main.AppLauncher</launcher.class>
```

**File:** `.vscode/launch.json`
Update the debug configuration.

```json
{
    "name": "Debug MainVerticle",
    "mainClass": "ziadatari.ReactiveAPI.main.AppLauncher",
    // ... args and env remain the same
}
```

#### **Step 3: Expose Metrics Endpoint**

**File:** `src/main/java/ziadatari/ReactiveAPI/web/HttpVerticle.java`
Add the scraping handler.

* **Import:** `import io.vertx.micrometer.PrometheusScrapingHandler;`
* **Action:** Inside `start()`, register the route.

```java
// ... existing router setup ...
router.route().handler(BodyHandler.create());

// --- METRICS ---
router.route("/metrics").handler(PrometheusScrapingHandler.create());

// --- HEALTH CHECKS ---
// ... existing health checks ...
```

#### **Step 4: Instrument Authentication (Login Metric)**

**File:** `src/main/java/ziadatari/ReactiveAPI/web/AuthController.java`
Track login success/failures.

* **Import:** `import io.vertx.micrometer.backends.BackendRegistries;`
* **Import:** `import io.micrometer.core.instrument.MeterRegistry;`
* **Action:** Get the registry and increment counters.

```java
public void login(RoutingContext ctx) {
    // Get Registry
    MeterRegistry registry = BackendRegistries.getDefaultNow();
    
    // ... parse body ...

    vertx.eventBus().request("users.authenticate", loginRequest.toJson(), authReply -> {
        if (authReply.succeeded()) {
            // Success Counter
            registry.counter("api_auth_attempts_total", "result", "success").increment();
            // ... issue token logic ...
        } else {
            // Failure Counter
            registry.counter("api_auth_attempts_total", "result", "failure").increment();
            GlobalErrorHandler.handle(ctx, authReply.cause());
        }
    });
    // ... catch block ...
}
```

#### **Step 5: Instrument Circuit Breaker (Resilience Metric)**

**File:** `src/main/java/ziadatari/ReactiveAPI/web/CustomCircuitBreaker.java`
Expose the internal state as a Gauge.

* **Action:** In the constructor, register the gauge.

```java
public CustomCircuitBreaker(Vertx vertx, ...) {
    // ... existing init ...
    
    MeterRegistry registry = io.vertx.micrometer.backends.BackendRegistries.getDefaultNow();
    if (registry != null) {
        registry.gauge("circuit_breaker_state", state, s -> {
            if (s.get() == State.CLOSED) return 0;
            if (s.get() == State.OPEN) return 1;
            return 2; // HALF_OPEN
        });
    }
}
```

#### **Step 6: Instrument Business Ops (Optional but Recommended)**

**File:** `src/main/java/ziadatari/ReactiveAPI/repository/EmployeeVerticle.java`
Track employee creation events.

* **Action:** In `createEmployee`, increment a counter `business_ops_total` with tag `action=create`.

---

### **3. Verification Plan**

1. **Build & Run:**
* Run `mvn clean package`.
* Start using the new launcher: `java -jar target/reactive-rest-api-...-fat.jar run ziadatari.ReactiveAPI.main.MainVerticle`.


2. **Verify Endpoint:**
* `curl http://localhost:8888/metrics`
* **Expect:** Text output starting with `# HELP vertx_http_server_requests...`


3. **Verify Tags (FR-02):**
* Hit `GET /v3/employees` and `POST /login`.
* Check metrics for: `vertx_http_server_requests_seconds_count{method="POST", route="/login"}`.


4. **Verify Custom Metric (FR-03):**
* Attempt a login.
* Check metrics for: `api_auth_attempts_total`.