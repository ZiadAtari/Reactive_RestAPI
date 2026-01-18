package ziadatari.ReactiveAPI.auth;

import io.vertx.core.Future;

public interface TokenService {

    /**
     * Retrieves a valid authentication token.
     * Implementations may cache the token and handle regeneration if it's expired.
     *
     * @return a Future containing the token string
     */
    Future<String> getToken();

    /**
     * Generates a unique JWT token for a specific user.
     *
     * @param username the username to include in the token claims
     * @return a Future containing the token string
     */
    Future<String> generateUserToken(String username);
}
