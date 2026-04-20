package com.laby.annual.scheduler.controller.debug;

import com.laby.annual.scheduler.security.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler/debug")
@RequiredArgsConstructor
public class TokenDebugController {

    private final JWTService jwtService;

    @GetMapping("/token-info")
    public ResponseEntity<Map<String, Object>> tokenInfo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Map<String, Object> out = new HashMap<>();
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            out.put("error", "Missing or invalid Authorization header. Provide 'Authorization: Bearer <token>'");
            return ResponseEntity.badRequest().body(out);
        }

        String token = authorization.substring(7);
        try {
            String username = jwtService.extractUsername(token);
            String role = jwtService.extractRole(token);
            Long schoolId = jwtService.extractSchoolId(token);
            String profileId = jwtService.extractProfileId(token);

            out.put("username", username);
            out.put("role", role);
            out.put("schoolId", schoolId);
            out.put("profileId", profileId);

            // include a few raw claims if present
            Object rolesClaim = jwtService.extractClaimObject(token, "roles");
            Object authoritiesClaim = jwtService.extractClaimObject(token, "authorities");
            out.put("roles_claim", rolesClaim);
            out.put("authorities_claim", authoritiesClaim);

            return ResponseEntity.ok(out);
        } catch (Exception e) {
            out.put("error", "Failed to parse token: " + e.getMessage());
            return ResponseEntity.status(500).body(out);
        }
    }
}

