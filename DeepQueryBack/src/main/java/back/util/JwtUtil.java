package back.util;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private static Key key; 
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24小时

    public JwtUtil(@Value("${jwt.secret}") String secretKeyString) {
        if (secretKeyString == null || secretKeyString.isEmpty()) {
            throw new IllegalArgumentException("JWT secret key cannot be null or empty. Please set jwt.secret in application.properties");
        }
        // Use BASE64URL decoder to allow for URL-safe characters like '-' and '_'
        byte[] keyBytes = Decoders.BASE64URL.decode(secretKeyString);
        if (keyBytes.length < 32) {
            System.err.println("WARNING: JWT secret key is shorter than 256 bits. This is insecure for HS256. Key length: " + keyBytes.length * 8 + " bits.");
            // Consider throwing an exception in a production environment if the key is too short
            // throw new IllegalArgumentException("JWT secret key must be at least 256 bits (32 bytes) long for HS256.");
        }
        JwtUtil.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public static String generateToken(String username) {
        if (key == null) {
            // This might happen if JwtUtil is not managed by Spring or if initialization failed.
            throw new IllegalStateException("JWT key has not been initialized. Ensure JwtUtil is a Spring managed bean and jwt.secret is correctly set in application.properties.");
        }
        return Jwts.builder()
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    public static String validateToken(String token) {
        if (key == null) {
            throw new IllegalStateException("JWT key has not been initialized.");
        }
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
