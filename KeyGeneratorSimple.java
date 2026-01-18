import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class KeyGeneratorSimple {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        String priv = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        String pub = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

        System.out.println("-----BEGIN PRIVATE KEY-----");
        System.out.println(priv);
        System.out.println("-----END PRIVATE KEY-----");
        System.out.println("MATCHING_SPLIT");
        System.out.println("-----BEGIN PUBLIC KEY-----");
        System.out.println(pub);
        System.out.println("-----END PUBLIC KEY-----");
    }
}
