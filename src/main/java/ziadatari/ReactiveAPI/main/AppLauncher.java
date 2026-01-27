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
                                .setEnabled(true)) // Native Prometheus (No client-side quantiles)
                        .setLabels(EnumSet.of(Label.HTTP_METHOD, Label.HTTP_CODE, Label.HTTP_ROUTE))
                        .setEnabled(true));
    }

    @Override
    public void afterStartingVertx(Vertx vertx) {
        // 2. Configure Histogram Buckets for Native Graphing
        // We use explicit SLA boundaries to satisfy BR-01/BR-02
        BackendRegistries.getDefaultNow()
                .config()
                .meterFilter(new MeterFilter() {
                    @Override
                    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                        if (id.getName().startsWith("http.server.requests")
                                || id.getName().startsWith("vertx.http.server.response.time")) {
                            return DistributionStatisticConfig.builder()
                                    .serviceLevelObjectives(
                                            0.1, 0.5, 1.0, 5.0, 10.0 // 100ms, 500ms, 1s, 5s, 10s
                            )
                                    .build()
                                    .merge(config);
                        }
                        return config;
                    }
                });
    }
}
