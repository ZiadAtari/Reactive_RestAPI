package ziadatari.ReactiveAPI.dto;

import io.vertx.core.json.JsonObject;

/**
 * DTO for passing authenticated user context.
 * Used to return user information without exposing sensitive internal data
 * (like password hashes).
 */
public class UserContextDTO {

    private String username;
    private String role;

    public UserContextDTO(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public UserContextDTO(JsonObject json) {
        if (json != null) {
            this.username = json.getString("username");
            this.role = json.getString("role", "user"); // Default to "user" if missing
        }
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("username", username)
                .put("role", role);
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "UserContextDTO{username='" + username + "', role='" + role + "'}";
    }
}
