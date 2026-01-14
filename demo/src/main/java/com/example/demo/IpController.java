package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
public class IpController {

    private final Random random = new Random();

    @GetMapping({ "/ip", "/v1/ip" })
    public ResponseEntity<Map<String, String>> getIpLegacy(@RequestParam String address) {
        return processIpRequest(address);
    }

    @GetMapping("/v3/ip")
    public ResponseEntity<Map<String, String>> getIpAuthenticated(@RequestParam String address) {
        return processIpRequest(address);
    }

    private ResponseEntity<Map<String, String>> processIpRequest(String address) {
        Map<String, String> response = new HashMap<>();
        response.put("ip", address);

        // Random Latency (40% chance, 500ms - 5000ms)
        // Simulates network jitter or slow processing to test client-side timeout
        // handling
        if (random.nextDouble(100) < 1) {
            long sleepMillis = 500 + random.nextInt(4501);
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Truly Randomized Failures (1% chance)
        // Simulates various server-side faults to test client-side circuit breaker and
        // error mapping
        if (random.nextDouble(100.0) < 1) {
            int errorCode = random.nextInt(4);
            switch (errorCode) {
                case 0: // 408 Request Timeout
                    response.put("message", "Failure: Request Timeout simulated");
                    return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(response);
                case 1: // 429 Too Many Requests
                    response.put("message", "Failure: Too Many Requests simulated");
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
                case 2: // 503 Service Unavailable
                    response.put("message", "Failure: Service Unavailable simulated");
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
                case 3: // 500 Internal Server Error
                default:
                    response.put("message", "Failure: Internal Server Error simulated");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }

        response.put("message", "Success");
        return ResponseEntity.ok(response);
    }
}
