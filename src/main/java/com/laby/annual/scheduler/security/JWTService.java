package com.laby.annual.scheduler.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JWTService {
    // IMPORTANT: In a production environment, this secret should be stored securely.
    private static final String SECRET_KEY = "QjRKaGh0VmJxS3M1eWdOVmZzWkN1M0poeEdDTHF6WjY=";

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

//    // Updated: Accept role as a string and add as a claim
//    public String generateToken(String username, String role) {
//        Map<String, Object> claims = new HashMap<>();
//        claims.put("role", role);
//        return Jwts.builder()
//                .claims()
//                .add(claims)
//                .subject(username)
//                .issuedAt(new Date(System.currentTimeMillis()))
//                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour
//                .and()
//                .signWith(getSignInKey())
//                .compact();
//    }
//
//    // Overload for backward compatibility (if needed)
    ////    public String generateToken(String username) {
    ////        return generateToken(username, "ROLE_USER");
    ////    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            System.out.println("Extracted claims from token: " + claims);
            return claims;
        } catch (Exception e) {
            System.out.println("Error parsing token: " + e.getMessage());
            throw e;
        }
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // New: Extract role from token as a string
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    public Long extractSchoolId(String token) {
        Claims claims = extractAllClaims(token);
        Object value = claims.get("schoolId");
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.valueOf((String) value);
            } catch (NumberFormatException e) {
                System.out.println("Unable to parse schoolId claim to Long: " + value);
                return null;
            }
        }
        System.out.println("Unexpected type for schoolId claim: " + value.getClass());
        return null;
    }

    public String extractProfileId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("profileId", String.class);
    }

    /**
     * Return the raw claim value for the given name (may be String, Number, List, etc).
     */
    public Object extractClaimObject(String token, String claimName) {
        Claims claims = extractAllClaims(token);
        return claims.get(claimName);
    }
}
