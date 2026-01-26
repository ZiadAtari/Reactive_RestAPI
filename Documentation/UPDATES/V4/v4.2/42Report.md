# v4.2 Report: Native Metrics Verification

This document provides the PromQL expressions required to visualize the application's performance directly in Prometheus.

## 1. Response Times

### Average Response Time
Calculates the global average latency per second.
```promql
sum(rate(vertx_http_server_requests_seconds_sum[1m]))
/
sum(rate(vertx_http_server_requests_seconds_count[1m]))
```

### 95th Percentile Latency (P95)
Estimates the latency value that 95% of requests fall below.
**Requires:** Server-side Histograms (Buckets).
```promql
histogram_quantile(0.95, sum(rate(vertx_http_server_requests_seconds_bucket[5m])) by (le))
```

### Maximum Response Time
Shows the slowest request in the sampled period.
```promql
max_over_time(vertx_http_server_requests_seconds_max[1m])
```

## 2. Failure Analysis

### Total Failed Calls (5xx)
Count of internal server errors per second.
```promql
sum(rate(vertx_http_server_requests_seconds_count{code=~"5.."}[1m]))
```

### Failure Rate (%)
Percentage of requests resulting in a 5xx error.
```promql
(
  sum(rate(vertx_http_server_requests_seconds_count{code=~"5.."}[1m]))
  /
  sum(rate(vertx_http_server_requests_seconds_count[1m]))
) * 100
```
