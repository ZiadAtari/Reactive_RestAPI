package ziadatari.ReactiveAPI.main;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ziadatari.ReactiveAPI.auth.AuthVerticle;
import ziadatari.ReactiveAPI.repository.EmployeeVerticle;
import ziadatari.ReactiveAPI.repository.UserVerticle;
import ziadatari.ReactiveAPI.web.HttpVerticle;

/**
 * Main entry point for the Reactive REST API application.
 * Responsible for initializing configuration and deploying the application's
 * verticles.
 */
public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) {

    // 1. Database Configuration
    String dbPassword = System.getenv("DB_PASSWORD");
    if (dbPassword == null || dbPassword.isEmpty()) {
      logger.warn("DB_PASSWORD environment variable is not set. Using default.");
      dbPassword = "secret";
    }

    JsonObject dbconfig = new JsonObject()
        .put("host", "localhost")
        .put("port", 3306)
        .put("database", "payroll_db")
        .put("user", "root")
        .put("password", dbPassword);

    // 2. App Configuration
    JsonObject appConfig = new JsonObject()
        .put("http.port", 8888)
        .put("url", "http://localhost:8888")
        .put("db", dbconfig);

    DeploymentOptions dbOptions = new DeploymentOptions().setConfig(appConfig);

    // 3. Auth Configuration (RSA Key)
    String privateKey = System.getenv("RSA_PRIVATE_KEY");
    if (privateKey == null || privateKey.isBlank()) {
      logger.warn("RSA_PRIVATE_KEY environment variable is not set. Using INSECURE default for development.");
      privateKey = "-----BEGIN PRIVATE KEY-----\n" +
          "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDpTnhPTS5Xvb1i\n" +
          "ogTz9ULbcKh/UwYwNHkgN+ZLY3pVaJudoLRZ3IPWwpcOxupRTfQfd0uzHYLT95hy\n" +
          "lwNTWHue1VlN6i58xzhpVdfg7xy7M4rolicg15N2iy5oJzfUQIF9nzesuKXH3SNE\n" +
          "FfuP4Zy4wFO6PfejHPI3cP8VWdhZyUC1Vjb7g47h2QhLWs73vf+0fBr79YaEs7Qd\n" +
          "7UP2Gq8rDNbRVjCEQdE0cd3s4RlR64n4vEi+Aol5WcY3hqTRWo0oK/l4nLzV5kwd\n" +
          "IWtY2aUOMsF5gubV/NiRKQ4jrULQlBNB+/pOKS33d/bIUNJSJuK6PIC7aJUnNE5b\n" +
          "3GI3p+PZAgMBAAECggEAMjKBeJ8oHzv6M8n3VvXhKImfXm7pI8J2Xay8Wz6W5y5T\n" +
          "8Y/O1m3D/hCU/T3d6AtC3Wopo5tshwzpB6bub66dQR1mCDjG0cc5pNwfda9N3W9\n" +
          "YKCer9zZPBZDNc4wsC/GFylrYFftPdFaUSREcTGMr5NFzajeauIIBDlqdRMfTNan\n" +
          "pmMbqb79h+HRwWQpCF6W2EjOrKN+VvChcqRXNRT5PWO//K5JIF5ut1bgNTSWNdM8\n" +
          "BKQRQ50tCSuY6Qk6G5I5e8Z9K5I5ut1bgNTSWNdM8BKQRQ50tCSuY6Qk6G5I5e8Z\n" +
          "9K5I5ut1bgNTSWNdM8BKQRQ50tCSuY6Qk6G5I5e8Z9K5I5ut1bgNTSWNdM8BKQRQ\n" +
          "50tBQKBgQD95RbVCCX3y6qHnesqJbKVzlClp187k+bBk0UFOkagHlPoCOwrAkjW\n" +
          "XInjvnJm6VW+PIv+Q6vHz8gnuWttVc1zw3NJME8Lc9YRdv0QDrDTSbyQsG1zC/e6\n" +
          "YENMWuY2L53MWWHP2sBhKCpP446c8eso6QnKzfE1BvsjoX47JJ7lnQKBgQDqUWtr\n" +
          "EBpFYvGsvKtvjv5EeeK4spF6CwMT0cYrmn+V08OqKGkPEa7pFoHHmYc3IdgcJCh5\n" +
          "mH7OMPgWcNN/Z4W3GoFPoG70ZavrWLwfWFAxXrRAKy3LWDHMA2bkC88LeoUbJ0Xr\n" +
          "zgexHstKxxtZbjt0b5uc0eO08D+MfCiZWRagbQKBgEzkWuDk1l9MByNmFhzexmK+\n" +
          "nEF8nhPg6AmZHYcYL1n6DCHBH01SIRrZS5dyShyVYxJCPZD0ZQufuKeTHb8b1SCI\n" +
          "42w+sHmp9ffKx0hixiDW65VSQ5IjGBYXF/YerbfG4XlVtVX4jXRBo5H5+XmK2P+8\n" +
          "XQHa4lq2wmfbPbMltvFZAoGAZ8R83KH58GZ7/nJjPXlG005jAEZcNH7x8vIAX8kA\n" +
          "3Xo1eYKB3CGJo0HLYXh1MA415WiB4C+PYILBKzb2AsL2rXr4bynuWR85fnUCgqMa\n" +
          "iHKXQp+cnSPGkcGj7DPqkfvFPJws09ue/mpTvx7j1rwBanQOpukwfS20BuPpGtgg\n" +
          "NBUCgYA07Zpo0P+zXJrVh5iH1V9x/lLqPoFzfrVGf5/WTSwOJlm0ljMsICyf6hl1\n" +
          "E4Sg+woOrAM6hIxTljO2O1krugOfTa3XNeJhXkgOKtA/DwKH54esVJBernSRyHkI\n" +
          "HaG56Wx70dBxQZBUkeGI7sSpK56bGb/wWBYjM/QSnwcnPsePQ==\n" +
          "-----END PRIVATE KEY-----";
    }

    DeploymentOptions authOptions = new DeploymentOptions()
        .setConfig(new JsonObject().put("rsa_private_key", privateKey));

    // 3.1 Auth Configuration (RSA Public Key for Validation)
    String publicKey = System.getenv("RSA_PUBLIC_KEY");
    if (publicKey == null || publicKey.isBlank()) {
      logger.warn("RSA_PUBLIC_KEY environment variable is not set. Using INSECURE default for development.");
      publicKey = "-----BEGIN PUBLIC KEY-----\n" +
          "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6U54T00uV729YqIE8/VC\n" +
          "23Cof1MGmDR5IDfmS2N6VWibnaC0WdyD1sKXDsbqUU30H3dLsx2C0/eYcpccU1h7\n" +
          "ntVZTequfMc4aVXX4O8cuzOK6JYnINeTdocukCc31ECBfZ83rLilh90jRBX7j+Gc\n" +
          "uMBTuj33oxzyN3D/FVnYWclAtVY2+4OO4dkIS1rO973/tHwa+/WGhLO0He1D9hqv\n" +
          "KwzW0VYwhEHRENHd7OEZUeuJ+LxIvgKJeVnGN4ak0VqNKCv5eJy81eZMHStrWNmU\n" +
          "DjLBeYLm1fzYkSkOI61C0JQTPfv6Tiks93f2yFDSUibiujyAu2iVJzROW9xiN6fj\n" +
          "2QIDAQAB\n" +
          "-----END PUBLIC KEY-----";
    }
    appConfig.put("rsa_public_key", publicKey);

    // 4. Deployment Sequence
    Future<String> deployEmployee = vertx.deployVerticle(EmployeeVerticle.class.getName(), dbOptions);
    Future<String> deployUser = vertx.deployVerticle(UserVerticle.class.getName(), dbOptions);
    Future<String> deployAuth = vertx.deployVerticle(AuthVerticle.class.getName(), authOptions);

    Future.all(deployEmployee, deployUser, deployAuth)
        .compose(id -> {
          // 5. Deploy HttpVerticle
          // Scaling to multiple instances (1 per core)
          int instances = Runtime.getRuntime().availableProcessors();
          DeploymentOptions httpOptions = new DeploymentOptions()
              .setConfig(appConfig)
              .setInstances(instances);

          return vertx.deployVerticle(HttpVerticle.class.getName(), httpOptions);
        })
        .onSuccess(id -> {
          logger.info("------------------------------------------------------------");
          logger.info("APPLICATION STARTED SUCCESSFULLY");
          logger.info("REST API URL: {}", appConfig.getString("url"));
          logger.info("------------------------------------------------------------");
          startPromise.complete();
        })
        .onFailure(err -> {
          logger.error("CRITICAL: Failed to deploy verticles", err);
          startPromise.fail(err);
        });
  }
}
