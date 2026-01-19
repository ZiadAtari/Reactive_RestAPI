package ziadatari.ReactiveAPI.dto;

import io.vertx.core.json.JsonObject;

/**
 * DTO for handling login requests.
 * Encapsulates username and password with validation and security features.
 */
public class LoginRequestDTO {

    private String username;
    private String password;

    public LoginRequestDTO(JsonObject json) {
        if (json != null) {
            this.username = json.getString("username");
            this.password = json.getString("password");
        }
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("username", username)
                .put("password", password);
    }

    /**
     * Validates that both username and password are present and not empty.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return username != null && !username.trim().isEmpty() &&
                password != null && !password.trim().isEmpty();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Secure toString method that redacts the password.
     * Prevents accidental leakage of credentials in logs.
     */
    @Override
    public String toString() {
        return "LoginRequestDTO{username='" + username + "', password='[PROTECTED]'}";
    }
}
