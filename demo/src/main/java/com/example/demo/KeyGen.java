package com.example.demo;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class KeyGen {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        String privat = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        String publicK = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

        System.out.println("---BEGIN PRIVATE KEY---");
        System.out.println(privat);
        System.out.println("---END PRIVATE KEY---");
        System.out.println("---BEGIN PUBLIC KEY---");
        System.out.println(publicK);
        System.out.println("---END PUBLIC KEY---");
    }
}
