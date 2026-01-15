package ziadatari.ReactiveAPI.main;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;

import io.vertx.core.json.JsonObject;
import ziadatari.ReactiveAPI.web.HttpVerticle;

/**
 * Main entry point for the Reactive REST API application.
 * Responsible for initializing configuration and deploying the application's
 * verticles.
 */
public class MainVerticle extends AbstractVerticle {

  /**
   * Starts the MainVerticle.
   * Initializes database and application configurations, and deploys the
   * EmployeeVerticle
   * and HttpVerticle in a specific order.
   *
   * @param startPromise a promise to signal the success or failure of the
   *                     deployment
   */
  @Override
  public void start(Promise<Void> startPromise) {

    // Retrieve database password from environment variables for security.
    // Defaults to 'secret' if not provided (for development purposes).
    String dbPassword = System.getenv("DB_PASSWORD");
    if (dbPassword == null || dbPassword.isEmpty()) {
      System.out.println("LOG: DB_PASSWORD environment variable is not set. Using default.");
      dbPassword = "secret";
    }

    // Database connection configuration
    JsonObject dbconfig = new JsonObject()
        .put("host", "localhost")
        .put("port", 3306)
        .put("database", "payroll_db")
        .put("user", "root")
        .put("password", dbPassword);

    // Main application configuration including HTTP port and base URL
    JsonObject appConfig = new JsonObject()
        .put("http.port", 8888)
        .put("url", "http://localhost:8888")
        .put("db", dbconfig);

    // DeploymentOptions allows passing config to child verticles
    DeploymentOptions options = new DeploymentOptions().setConfig(appConfig);

    // Sequential deployment:
    // 1. Deploy EmployeeVerticle first (handles DB operations and business logic)
    vertx.deployVerticle(ziadatari.ReactiveAPI.repository.EmployeeVerticle.class.getName(), options)
        .compose(id -> {
          // 2. Setup JWT Auth
          // Ideally, this key should be loaded from a secure vault or environment
          // variable.
          // For this specific 'Reactive' implementation, it is hardcoded to demonstrate
          // RS256 signing.
          String privateKey = "-----BEGIN PRIVATE KEY-----\r\n" +
              "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDpTnhPTS5Xvb1i\r\n" + //
              "ogTz9ULbcKh/UwYwNHkgN+ZLY3pVaJudoLRZ3IPWwpcOxupRTfQfd0uzHYLT95hy\r\n" + //
              "lwNTWHue1VlN6i58xzhpVdfg7xy7M4rolicg15N2iy5oJzfUQIF9nzesuKXH3SNE\r\n" + //
              "FfuP4Zy4wFO6PfejHPI3cP8VWdhZyUC1Vjb7g47h2QhLWs73vf+0fBr79YaEs7Qd\r\n" + //
              "7UP2Gq8rDNbRVjCEQdE0cd3s4RlR64n4vEi+Aol5WcY3hqTRWo0oK/l4nLzV5kwd\r\n" + //
              "IWtY2aUOMsF5gubV/NiRKQ4jrULQlBNB+/pOKS33d/bIUNJSJuK6PIC7aJUnNE5b\r\n" + //
              "3GI3p+PZAgMBAAECggEAHN5IDj31WkeAz9vO45XpPihoPQr7pMrVYwpvrUvjlv5g\r\n" + //
              "5QX90vGxgYkl8l7kHwfXiW40OHdSGSLVovYzI+S6tSpeiMDNnER1YzlLQ0qrAjHs\r\n" + //
              "Qf/UWyC5ny01Io+ZvaJ2s2HYXH+jicG/65yRulm3D/hCU/T3d6AtC3Wopo5tshwz\r\n" + //
              "pB6bub66dQR1kCDjG0cc5pNwfda9N3W9YKCer9zZPBZDNc4wsC/GFylrYFftPdFa\r\n" + //
              "USREcTGMr5NFzajeauIIBDlqdRMfTNanpmMbqb79h+HRwWQpCF6W2EjOrKN+VvCh\r\n" + //
              "cqRXNRT5PWO//K5JIF5ut1bgNTSWNdM8BKQRQ50tBQKBgQD+5RbVCCX3y6qHnesq\r\n" + //
              "JbKVzlClp187k+bBk0UFOkagHlPoCOwrAkjWXInjvnJm6VW+PIv+Q6vHz8gnuWtt\r\n" + //
              "Vc1zw3NJME8Lc9YRdv0QDrDTSbyQsG1zC/e6YENMWuY2L53MWWHP2sBhKCpP446c\r\n" + //
              "8eso6QnKzfE1BvsjoX47JJ7lnQKBgQDqUWtrEBpFYvGsvKtvjv5EeeK4spF6CwMT\r\n" + //
              "0cYrmn+V08OqKGkPEa7pFoHHmYc3IdgcJCh5mH7OMPgWcNN/Z4W3GoFPoG70Zavr\r\n" + //
              "WLwfWFAxXrRAKy3LWDHMA2bkC88LeoUbJ0XrzgexHstKxxtZbjt0b5uc0eO08D+M\r\n" + //
              "fCiZWRagbQKBgEzkWuDk1l9MByNmFhzexmK+nEF8nhPg6AmZHYcYL1n6DCHBH01S\r\n" + //
              "IRrZS5dyShyVYxJCPZD0ZQufuKeTHb8b1SCI42w+sHmp9ffKx0hixiDW65VSQ5Ij\r\n" + //
              "GBYXF/YerbfG4XlVtVX4jXRBo5H5+XmK2P+8XQHa4lq2wmfbPbMltvFZAoGAZ8R8\r\n" + //
              "3KH58GZ7/nJjPXlG005jAEZcNH7x8vIAX8kA3Xo1eYKB3CGJo0HLYXh1MA415WiB\r\n" + //
              "4C+PYILBKzb2AsL2rXr4bynuWR85fnUCgqMaiHKXQp+cnSPGkcGj7DPqkfvFPJws\r\n" + //
              "09ue/mpTvx7j1rwBanQOpukwfS20BuPpGtggNBUCgYA07Zpo0P+zXJrVh5iH1V9x\r\n" + //
              "/lLqPoFzfrVGf5/WTSwOJlm0ljMsICyf6hl1E4Sg+woOrAM6hIxTljO2O1krugOf\r\n" + //
              "Ta3XNeJhXkgOKtA/DwKH54esVJBerncSRyHkIHaG56Wx70dBxQZBUkeGI7sSpK56\r\n" + //
              "bGb/wWBYjM/QSnwcnPsePQ==\r\n" +
              "-----END PRIVATE KEY-----";

          io.vertx.ext.auth.jwt.JWTAuth jwtAuth;
          ziadatari.ReactiveAPI.service.Rs256TokenService tokenService;

          try {
            io.vertx.ext.auth.jwt.JWTAuthOptions jwtOptions = new io.vertx.ext.auth.jwt.JWTAuthOptions()
                .addPubSecKey(new io.vertx.ext.auth.PubSecKeyOptions()
                    .setAlgorithm("RS256")
                    .setBuffer(privateKey));

            jwtAuth = io.vertx.ext.auth.jwt.JWTAuth.create(vertx, jwtOptions);
            tokenService = new ziadatari.ReactiveAPI.service.Rs256TokenService(
                jwtAuth);
          } catch (Exception e) {
            System.err.println("WARNING: Failed to initialize JWT Auth Service: " + e.getMessage());
            System.err.println("WARNING: API will still start, but /v3/ (authenticated) routes will fail.");
            jwtAuth = null;
            tokenService = new ziadatari.ReactiveAPI.service.Rs256TokenService(e.getMessage());
          }

          // 3. Deploy HttpVerticle with TokenService
          // We use setInstances(1) in a loop because we are passing a specific instance
          // (constructor injection)
          // But actually, passing a specific instance to deployVerticle is allowed.
          // However, to scale across event loops properly with a shared service, we must
          // be careful.
          // Vert.x documentation says: if you supply an instance, it is re-used.
          // So to have multiple verticles (one per thread), we need to create new
          // instances or deploy the class.
          // BUT we want to share the TokenService (Singleton).
          // So we loop and create new HttpVerticle(tokenService).

          int instances = Runtime.getRuntime().availableProcessors();
          java.util.List<io.vertx.core.Future> deployments = new java.util.ArrayList<>();

          for (int i = 0; i < instances; i++) {
            deployments.add(vertx.deployVerticle(new HttpVerticle(tokenService), options.setInstances(1)));
          }

          return io.vertx.core.CompositeFuture.all(deployments);
        })
        .onSuccess(id -> {
          // Log success and complete the start promise
          System.out.println("------------------------------------------------------------");
          System.out.println("APPLICATION STARTED SUCCESSFULLY");
          System.out.println("REST API URL: " + appConfig.getString("url"));
          System.out.println("------------------------------------------------------------");
          startPromise.complete();
        })
        .onFailure(err -> {
          // Log failure details and fail the start promise
          System.err.println("CRITICAL: Failed to deploy verticles: " + err.getMessage());
          startPromise.fail(err);
        });

  }
}
