package com.laby.annual.scheduler.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
public class JWTService {
    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

    // IMPORTANT: In a production environment, this secret should be stored securely.
    private static final String SECRET_KEY = "QjRKaGh0VmJxS3M1eWdOVmZzWkN1M0poeEdDTHF6WjY=";

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

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
            logger.debug("Extracted claims from token: {}", claims);
            return claims;
        } catch (Exception e) {
            logger.error("Error parsing token: {}", e.getMessage());
            throw e;
        }
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // New: Extract role from token as a string (handles different claim shapes)
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        if (claims == null) return null;

        Object roleObj = claims.get("role");
        if (roleObj instanceof String) {
            return (String) roleObj;
        }

        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof String) {
            return (String) rolesObj;
        }
        if (rolesObj instanceof List) {
            List<?> list = (List<?>) rolesObj;
            if (!list.isEmpty()) return String.valueOf(list.get(0));
        }

        Object authorities = claims.get("authorities");
        if (authorities instanceof List) {
            List<?> list = (List<?>) authorities;
            if (!list.isEmpty()) return String.valueOf(list.get(0));
        }

        logger.debug("No role-like claim found in token claims: {}", claims.keySet());
        return null;
    }

    public Long extractSchoolId(String token) {
        Claims claims = extractAllClaims(token);
        if (claims == null) return null;
        Object val = claims.get("schoolId");
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        try {
            return claims.get("schoolId", Long.class);
        } catch (Exception e) {
            logger.debug("Could not extract schoolId as Long directly: {}", e.getMessage());
            return null;
        }
    }

    public String extractProfileId(String token) {
        Claims claims = extractAllClaims(token);
        if (claims == null) return null;
        Object val = claims.get("profileId");
        return val == null ? null : String.valueOf(val);
    }
}
