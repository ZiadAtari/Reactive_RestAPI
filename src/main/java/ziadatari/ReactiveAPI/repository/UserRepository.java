package ziadatari.ReactiveAPI.repository;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import ziadatari.ReactiveAPI.domain.User;

/**
 * Repository for accessing User data from the database.
 */
public class UserRepository {

    private final Pool dbClient;

    public UserRepository(Pool dbClient) {
        this.dbClient = dbClient;
    }

    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return a Future containing the User if found, or null if not found
     */
    public Future<User> findByUsername(String username) {
        return dbClient.preparedQuery("SELECT id, username, password_hash FROM users WHERE username = ?")
                .execute(Tuple.of(username))
                .map(this::mapRowToUser);
    }

    private User mapRowToUser(RowSet<Row> rows) {
        if (rows.size() == 0) {
            return null;
        }
        Row row = rows.iterator().next();
        return new User(
                row.getInteger("id"),
                row.getString("username"),
                row.getString("password_hash"));
    }
}
