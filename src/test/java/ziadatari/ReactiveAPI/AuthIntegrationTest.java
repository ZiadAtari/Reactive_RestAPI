package ziadatari.ReactiveAPI;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ziadatari.ReactiveAPI.main.MainVerticle;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class AuthIntegrationTest {

    private WebClient client;

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        client = WebClient.create(vertx);
        vertx.deployVerticle(new MainVerticle())
                .onComplete(testContext.succeeding(id -> testContext.completeNow()));
    }

    @Test
    @DisplayName("Login - Success with correct credentials")
    void loginSuccess(Vertx vertx, VertxTestContext testContext) {
        client.post(8888, "localhost", "/login")
                .sendJsonObject(new JsonObject()
                        .put("username", "admin")
                        .put("password", "password123"))
                .onComplete(testContext.succeeding(response -> {
                    testContext.verify(() -> {
                        assertEquals(200, response.statusCode());
                        assertNotNull(response.bodyAsJsonObject().getString("token"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    @DisplayName("Login - Failure with wrong credentials")
    void loginWrongPassword(Vertx vertx, VertxTestContext testContext) {
        client.post(8888, "localhost", "/login")
                .sendJsonObject(new JsonObject()
                        .put("username", "admin")
                        .put("password", "wrong_pass"))
                .onComplete(testContext.succeeding(response -> {
                    testContext.verify(() -> {
                        assertEquals(401, response.statusCode());
                        assertEquals("SEC_003", response.bodyAsJsonObject().getString("error_code"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    @DisplayName("Login - Failure with missing body")
    void loginMissingBody(Vertx vertx, VertxTestContext testContext) {
        client.post(8888, "localhost", "/login")
                .send()
                .onComplete(testContext.succeeding(response -> {
                    testContext.verify(() -> {
                        assertEquals(400, response.statusCode());
                        assertEquals("REQ_002", response.bodyAsJsonObject().getString("error_code"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    @DisplayName("Protected Route - Unauthorized without token")
    void protectedWithoutToken(Vertx vertx, VertxTestContext testContext) {
        client.post(8888, "localhost", "/v3/employees")
                .sendJsonObject(new JsonObject().put("name", "Test"))
                .onComplete(testContext.succeeding(response -> {
                    testContext.verify(() -> {
                        assertEquals(401, response.statusCode());
                        assertEquals("SEC_004", response.bodyAsJsonObject().getString("error_code"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    @DisplayName("Protected Route - Unauthorized with malformed token")
    void protectedWithMalformedToken(Vertx vertx, VertxTestContext testContext) {
        client.post(8888, "localhost", "/v3/employees")
                .putHeader("Authorization", "Bearer not_a_real_token")
                .sendJsonObject(new JsonObject().put("name", "Test"))
                .onComplete(testContext.succeeding(response -> {
                    testContext.verify(() -> {
                        assertEquals(401, response.statusCode());
                        assertEquals("SEC_005", response.bodyAsJsonObject().getString("error_code"));
                        testContext.completeNow();
                    });
                }));
    }
}
