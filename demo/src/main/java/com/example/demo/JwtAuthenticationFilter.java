package com.example.demo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AtomicReference<PublicKey> cachedPublicKey = new AtomicReference<>();
    private boolean publicKeyInitializationFailed = false;

    private static final String PUBLIC_KEY_Str = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6U54T00uV729YqIE8/VC\r\n"
            + //
            "23Cof1MGMDR5IDfmS2N6VWibnaC0WdyD1sKXDsbqUU30H3dLsx2C0/eYcpcDU1h7\r\n" + //
            "ntVZTeoufMc4aVXX4O8cuzOK6JYnINeTdosuaCc31ECBfZ83rLilx90jRBX7j+Gc\r\n" + //
            "uMBTuj33oxzyN3D/FVnYWclAtVY2+4OO4dkIS1rO973/tHwa+/WGhLO0He1D9hqv\r\n" + //
            "KwzW0VYwhEHRNHHd7OEZUeuJ+LxIvgKJeVnGN4ak0VqNKCv5eJy81eZMHSFrWNml\r\n" + //
            "DjLBeYLm1fzYkSkOI61C0JQTQfv6Tikt93f2yFDSUibiujyAu2iVJzROW9xiN6fj\r\n" + //
            "2QIDAQAB";

    private PublicKey getPublicKey() {
        try {
            // Remove headers, newlines, and spaces to get raw Base64 string
            String sanitizedKey = PUBLIC_KEY_Str
                    .replaceAll("\\n", "")
                    .replaceAll("\\r", "")
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .trim();

            byte[] keyBytes = Base64.getDecoder().decode(sanitizedKey);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Invalid Public Key Configuration", e);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Check if we already know that the public key is invalid
        if (publicKeyInitializationFailed && request.getRequestURI().startsWith("/v3/")) {
            sendError(response, "Security Configuration Error", "Public Key is invalid or malformed", "SEC_CFG_001");
            return;
        }

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            PublicKey publicKey = cachedPublicKey.get();
            if (publicKey == null) {
                publicKey = getPublicKey();
                cachedPublicKey.set(publicKey);
            }

            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .setAllowedClockSkewSeconds(60) // clock differences
                    .build()
                    .parseClaimsJws(token);

            Claims body = claimsJws.getBody();
            // validity of the signature to trust the service.
            String subject = body.getSubject();

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    subject, null, Collections.emptyList());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // Check if it's a configuration error (RSA key) or just a bad token
            if (e.getMessage() != null && e.getMessage().contains("Invalid Public Key Configuration")) {
                publicKeyInitializationFailed = true;
                if (request.getRequestURI().startsWith("/v3/")) {
                    sendError(response, "Security Configuration Error", "Public Key is invalid or malformed",
                            "SEC_CFG_001");
                    return;
                }
            }
            // Log error or just ignore. If auth fails, context is empty, and SecurityConfig
            // will reject /v3 access
            System.err.println("JWT Verification Failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, String error, String message, String code) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        String json = String.format("{\"error\": \"%s\", \"message\": \"%s\", \"code\": \"%s\"}",
                error, message, code);
        response.getWriter().write(json);
    }
}
