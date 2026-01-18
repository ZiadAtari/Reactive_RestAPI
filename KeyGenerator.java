import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class KeyGenerator {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        System.out.println("---PRIVATE_START---");
        System.out.println(Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()));
        System.out.println("---PRIVATE_END---");
        System.out.println("---PUBLIC_START---");
        System.out.println(Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()));
        System.out.println("---PUBLIC_END---");
    }
}
