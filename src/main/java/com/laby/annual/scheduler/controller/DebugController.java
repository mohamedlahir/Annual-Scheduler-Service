package com.laby.annual.scheduler.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/scheduler/debug")
public class DebugController {
    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

    @GetMapping("/principal")
    public ResponseEntity<Map<String, Object>> principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> resp = new HashMap<>();
        if (auth == null) {
            resp.put("authenticated", false);
            resp.put("principal", null);
            resp.put("authorities", List.of());
            logger.debug("Debug /principal called: no authentication present");
            return ResponseEntity.ok(resp);
        }

        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        resp.put("authenticated", auth.isAuthenticated());
        resp.put("principal", auth.getName());
        resp.put("authorities", authorities);
        logger.debug("Debug /principal called: principal={}, authorities={}", auth.getName(), authorities);
        return ResponseEntity.ok(resp);
    }
}

