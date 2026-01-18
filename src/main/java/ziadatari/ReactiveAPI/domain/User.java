package ziadatari.ReactiveAPI.domain;

/**
 * Domain entity representing a system user.
 * Used for authentication and identity management.
 */
public class User {
    private Integer id;
    private String username;
    private String passwordHash;

    public User() {
    }

    public User(Integer id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
