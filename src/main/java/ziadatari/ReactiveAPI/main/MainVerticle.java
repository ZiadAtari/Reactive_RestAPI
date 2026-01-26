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
      dbPassword = "Zatari4321";
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

    // 3. Auth Configuration (RSA Private Key)
    String privateKey = System.getenv("RSA_PRIVATE_KEY");
    if (privateKey == null || privateKey.isBlank()) {
      logger.warn("RSA_PRIVATE_KEY environment variable is not set. Using INSECURE default for development.");
      privateKey = "-----BEGIN PRIVATE KEY-----\n" +
          "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDpTnhPTS5Xvb1i\n" +
          "ogTz9ULbcKh/UwYwNHkgN+ZLY3pVaJudoLRZ3IPWwpcOxupRTfQfd0uzHYLT95hy\n" +
          "lwNTWHue1VlN6i58xzhpVdfg7xy7M4rolicg15N2iy5oJzfUQIF9nzesuKXH3SNE\n" +
          "FfuP4Zy4wFO6PfejHPI3cP8VWdhZyUC1Vjb7g47h2QhLWs73vf+0fBr79YaEs7Qd\n" +
          "7UP2Gq8rDNbRVjCEQdE0cd3s4RlR64n4vEi+Aol5WcY3hqTRWo0oK/l4nLzV5kwd\n" +
          "IWtY2aUOMsF5gubV/NiRKQ4jrULQlBNB+/pOKS33d/bIUNJSJuK6PIC7aJUnNE5b\n" +
          "3GI3p+PZAgMBAAECggEAHN5IDj31WkeAz9vO45XpPihoPQr7pMrVYwpvrUvjlv5g\n" +
          "5QX90vGxgYkl8l7kHwfXiW40OHdSGSLVovYzI+S6tSpeiMDNnER1YzlLQ0qrAjHs\n" +
          "Qf/UWyC5ny01Io+ZvaJ2s2HYXH+jicG/65yRulm3D/hCU/T3d6AtC3Wopo5tshwz\n" +
          "pB6bub66dQR1kCDjG0cc5pNwfda9N3W9YKCer9zZPBZDNc4wsC/GFylrYFftPdFa\n" +
          "USREcTGMr5NFzajeauIIBDlqdRMfTNanpmMbqb79h+HRwWQpCF6W2EjOrKN+VvCh\n" +
          "cqRXNRT5PWO//K5JIF5ut1bgNTSWNdM8BKQRQ50tBQKBgQD+5RbVCCX3y6qHnesq\n" +
          "JbKVzlClp187k+bBk0UFOkagHlPoCOwrAkjWXInjvnJm6VW+PIv+Q6vHz8gnuWtt\n" +
          "Vc1zw3NJME8Lc9YRdv0QDrDTSbyQsG1zC/e6YENMWuY2L53MWWHP2sBhKCpP446c\n" +
          "8eso6QnKzfE1BvsjoX47JJ7lnQKBgQDqUWtrEBpFYvGsvKtvjv5EeeK4spF6CwMT\n" +
          "0cYrmn+V08OqKGkPEa7pFoHHmYc3IdgcJCh5mH7OMPgWcNN/Z4W3GoFPoG70Zavr\n" +
          "WLwfWFAxXrRAKy3LWDHMA2bkC88LeoUbJ0XrzgexHstKxxtZbjt0b5uc0eO08D+M\n" +
          "fCiZWRagbQKBgEzkWuDk1l9MByNmFhzexmK+nEF8nhPg6AmZHYcYL1n6DCHBH01S\n" +
          "IRrZS5dyShyVYxJCPZD0ZQufuKeTHb8b1SCI42w+sHmp9ffKx0hixiDW65VSQ5Ij\n" +
          "GBYXF/YerbfG4XlVtVX4jXRBo5H5+XmK2P+8XQHa4lq2wmfbPbMltvFZAoGAZ8R8\n" +
          "3KH58GZ7/nJjPXlG005jAEZcNH7x8vIAX8kA3Xo1eYKB3CGJo0HLYXh1MA415WiB\n" +
          "4C+PYILBKzb2AsL2rXr4bynuWR85fnUCgqMaiHKXQp+cnSPGkcGj7DPqkfvFPJws\n" +
          "09ue/mpTvx7j1rwBanQOpukwfS20BuPpGtggNBUCgYA07Zpo0P+zXJrVh5iH1V9x\n" +
          "/lLqPoFzfrVGf5/WTSwOJlm0ljMsICyf6hl1E4Sg+woOrAM6hIxTljO2O1krugOf\n" +
          "Ta3XNeJhXkgOKtA/DwKH54esVJBerncSRyHkIHaG56Wx70dBxQZBUkeGI7sSpK56\n" +
          "bGb/wWBYjM/QSnwcnPsePQ==\n" +
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
          "23Cof1MGMDR5IDfmS2N6VWibnaC0WdyD1sKXDsbqUU30H3dLsx2C0/eYcpcDU1h7\n" +
          "ntVZTeoufMc4aVXX4O8cuzOK6JYnINeTdosuaCc31ECBfZ83rLilx90jRBX7j+Gc\n" +
          "uMBTuj33oxzyN3D/FVnYWclAtVY2+4OO4dkIS1rO973/tHwa+/WGhLO0He1D9hqv\n" +
          "KwzW0VYwhEHRNHHd7OEZUeuJ+LxIvgKJeVnGN4ak0VqNKCv5eJy81eZMHSFrWNml\n" +
          "DjLBeYLm1fzYkSkOI61C0JQTQfv6Tikt93f2yFDSUibiujyAu2iVJzROW9xiN6fj\n" +
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
