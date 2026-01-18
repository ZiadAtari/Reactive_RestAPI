package ziadatari.ReactiveAPI;

import org.mindrot.jbcrypt.BCrypt;

public class HashGenerator {
    public static void main(String[] args) {
        String password = "password123";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(10));
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);

        // Verify immediately
        boolean matches = BCrypt.checkpw(password, hash);
        System.out.println("Matches self: " + matches);

        // Unsure about the previous hash? Let's verify it too
        // Old hash: $2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRkgVduVrz.SSDmcDR8EC76U.bG
        try {
            boolean oldMatches = BCrypt.checkpw(password,
                    "$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRkgVduVrz.SSDmcDR8EC76U.bG");
            System.out.println("Matches old hash from SQL: " + oldMatches);
        } catch (Exception e) {
            System.out.println("Old hash invalid format: " + e.getMessage());
        }
    }
}
