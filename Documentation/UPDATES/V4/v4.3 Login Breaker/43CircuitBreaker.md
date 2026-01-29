# Circuit Breaker Technical Analysis

The ReactiveAPI employs a **Two-Tier Circuit Breaker** strategy to ensure resilience against both external dependency failures and internal database bottlenecks.

## 1. Web-Layer Circuit Breaker (Custom)
This circuit breaker protects the `VerificationHandler`, which communicates with the external Demo API.

*   **Implementation**: `CustomCircuitBreaker.java`
*   **Target**: External Verification Service (`localhost:8080/v1/ip` and `/v3/ip`)
*   **Configuration**:
    *   **Execution Timeout**: `500ms`
    *   **Reset Timeout**: `800ms`
    *   **Failure Threshold**: `5 matches`
*   **Observations**:
    *   **Coupling**: V1 and V3 routes share the **same instance**. If the V3 verification service lags, the circuit trips for V1 as well.
    *   **Visibility**: Metrics are exposed via the `circuit_breaker_state` gauge (0=OK, 1=OPEN, 2=HALF_OPEN).
    *   **Error Handling**: Returns `503 Service Unavailable` immediately when in `OPEN` state (Fail Fast).

## 2. Service-Layer Circuit Breaker (Vert.x Native)
This circuit breaker protects the `EmployeeService` from database-level contention or failures.

*   **Implementation**: `io.vertx.circuitbreaker.CircuitBreaker` (Standard)
*   **Target**: MySQL Database Operations (via `EmployeeRepository`)
*   **Configuration**:
    *   **Execution Timeout**: `200ms` (Aggressive fail-fast for DB)
    *   **Reset Timeout**: `1000ms`
    *   **Failure Threshold**: `5 matches`
*   **Observations**:
    *   **Isolation**: This circuit breaker is independent of the web-layer verification.
    *   **No Fallback**: Configured with `setFallbackOnFailure(false)`, ensuring that database errors are propagated to the caller rather than masked with empty data.

## 3. Impact on Metrics & Alerting
*   **State Tracking**: Use `max(circuit_breaker_state)` in Prometheus to see the real-time status of the Web-layer circuit.
*   **Alerting**: The `HighLatency` alert is specifically designed to detect conditions that might trip these circuits (e.g., P95 > 500ms).

---

### Suggested Optimization
> [!TIP]
> To prevent V1 traffic from being throttled by V3 verification issues, consider instantiating two separate `CustomCircuitBreaker` instances in `HttpVerticle`: one for `/v1/` and one for `/v3/`.
