package ziadatari.ReactiveAPI.repository;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ziadatari.ReactiveAPI.domain.User;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.ServiceException;

/**
 * Worker verticle responsible for User authentication.
 * Offloads CPU-intensive password hashing to a blocking executor.
 */
public class UserVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(UserVerticle.class);
    private UserRepository userRepository;

    @Override
    public void start(Promise<Void> startPromise) {
        // 1. Setup Database Connection
        JsonObject dbConfig = config().getJsonObject("db");
        MySQLConnectOptions connectOptions = new MySQLConnectOptions()
                .setPort(dbConfig.getInteger("port"))
                .setHost(dbConfig.getString("host"))
                .setDatabase(dbConfig.getString("database"))
                .setUser(dbConfig.getString("user"))
                .setPassword(dbConfig.getString("password"));

        PoolOptions poolOptions = new PoolOptions().setMaxSize(5);
        Pool pool = MySQLBuilder.pool()
                .with(poolOptions)
                .connectingTo(connectOptions)
                .using(vertx)
                .build();

        userRepository = new UserRepository(pool);

        // 2. Register Event Bus Consumer
        vertx.eventBus().consumer("users.authenticate", message -> {
            JsonObject body = (JsonObject) message.body();
            String username = body.getString("username");
            String password = body.getString("password");

            authenticate(username, password)
                    .onSuccess(user -> message.reply(new JsonObject().put("username", user.getUsername())))
                    .onFailure(err -> {
                        if (err instanceof ServiceException) {
                            message.fail(((ServiceException) err).getErrorCode().getHttpStatus(), err.getMessage());
                        } else {
                            logger.error("Authentication error for user: " + username, err);
                            message.fail(500, "Internal Server Error");
                        }
                    });
        });

        logger.info("UserVerticle deployed");
        startPromise.complete();
    }

    private Future<User> authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .compose(user -> {
                    if (user == null) {
                        return Future.failedFuture(new ServiceException(ErrorCode.UNAUTHORIZED, "Invalid credentials"));
                    }

                    // Offload blocking BCrypt check
                    return vertx.executeBlocking(promise -> {
                        if (BCrypt.checkpw(password, user.getPasswordHash())) {
                            promise.complete(user);
                        } else {
                            promise.fail(new ServiceException(ErrorCode.UNAUTHORIZED, "Invalid credentials"));
                        }
                    }, false); // false = not ordered
                });
    }
}
