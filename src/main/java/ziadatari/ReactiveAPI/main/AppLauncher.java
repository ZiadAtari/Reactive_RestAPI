package ziadatari.ReactiveAPI.main;

import io.vertx.core.Launcher;
import io.vertx.core.VertxOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.Label;
import java.util.EnumSet;

import io.vertx.core.Vertx;
import io.vertx.micrometer.backends.BackendRegistries;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;

/**
 * Main entry point and configuration for the Reactive API.
 * Extends Vert.x {@link Launcher} to customize the application lifecycle.
 */
public class AppLauncher extends Launcher {

    /**
     * Main method to start the application.
     * 
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        new AppLauncher().dispatch(args);
    }

    /**
     * Configures {@link VertxOptions} before the Vert.x instance is created.
     * Used here to enable and configure Micrometer metrics with Prometheus.
     * 
     * @param options the configuration options for Vert.x
     */
    @Override
    public void beforeStartingVertx(VertxOptions options) {
        // 1. Enable Micrometer Metrics with Prometheus registry
        options.setMetricsOptions(
                new MicrometerMetricsOptions()
                        .setPrometheusOptions(new VertxPrometheusOptions()
                                .setEnabled(true)) // Use native Prometheus format (no client-side summaries)
                        .setLabels(EnumSet.of(Label.HTTP_METHOD, Label.HTTP_CODE, Label.HTTP_ROUTE))
                        .setEnabled(true));
    }

    /**
     * Executes logic after the Vert.x instance has been successfully started.
     * Used here to customize the Micrometer registry configuration.
     * 
     * @param vertx the started Vert.x instance
     */
    @Override
    public void afterStartingVertx(Vertx vertx) {
        // 2. Configure Histogram Buckets for Native Graphing
        // We use explicit SLA boundaries to satisfy BR-01/BR-02.
        // This allows Prometheus to calculate quantiles (e.g. P95) from raw buckets.
        BackendRegistries.getDefaultNow()
                .config()
                .meterFilter(new MeterFilter() {
                    @Override
                    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                        // Target HTTP server metrics to provide clean, aggregatable latency
                        // distribution
                        if (id.getName().startsWith("http.server.requests")
                                || id.getName().startsWith("vertx.http.server.response.time")) {
                            return DistributionStatisticConfig.builder()
                                    .serviceLevelObjectives(
                                            0.1, 0.5, 1.0, 5.0, 10.0 // Latency boundaries in seconds: 100ms, 500ms, 1s,
                                                                     // 5s, 10s
                            )
                                    .build()
                                    .merge(config);
                        }
                        return config;
                    }
                });
    }
}
