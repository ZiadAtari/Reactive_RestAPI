# v4.2 Report: Native Metrics Verification

This document provides the PromQL expressions required to visualize the application's performance directly in Prometheus, formatted for readability (Milliseconds, Percentages).

## 1. Response Times (Milliseconds)

### Average Response Time (ms)
```promql
(
  sum(rate(vertx_http_server_response_time_seconds_sum[1m]))
  /
  sum(rate(vertx_http_server_response_time_seconds_count[1m]))
) * 1000
```
*Note: Multiplied by 1000 to convert Seconds to Milliseconds.*

### 95th Percentile Latency (P95 ms)
```promql
histogram_quantile(0.95, sum(rate(vertx_http_server_response_time_seconds_bucket[5m])) by (le)) * 1000
```

### Maximum Response Time (ms)
```promql
max_over_time(vertx_http_server_response_time_seconds_max[1m]) * 1000
```

## 2. Failure Analysis (Business Only)

To prevent "Noise Dilution" (where thousands of successful health/metric checks hide the real errors), we exclude technical routes.

### Total Failed Calls (5xx)
```promql
sum(rate(vertx_http_server_response_time_seconds_count{code=~"5.."}[1m]))
```

### Failure Rate (%)
```promql
(
  sum(rate(vertx_http_server_response_time_seconds_count{code=~"5..", route!="/metrics", route!="/health/live"}[1m]))
  /
  sum(rate(vertx_http_server_response_time_seconds_count{route!="/metrics", route!="/health/live"}[1m]))
) * 100
```
*Note: Excludes `/metrics` and `/health/live` to show the true Error Rate of business logic.*
