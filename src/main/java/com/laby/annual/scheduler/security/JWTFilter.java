package com.laby.annual.scheduler.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JWTFilter extends OncePerRequestFilter {

    @Autowired
    private JWTService jwtService;

    @Autowired
    ApplicationContext context;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();  // Changed from getServletPath to getRequestURI

        if (path.startsWith("/actuator/health")) {
            filterChain.doFilter(request, response);
            return;
        }
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
                try {
                    username = jwtService.extractUsername(token);

                    String role = jwtService.extractRole(token);

                    if (role == null) {
                        // fallback: try to read 'roles' claim or default to STUDENT
                        Object rolesClaim = jwtService.extractClaimObject(token, "roles");
                        if (rolesClaim != null) {
                            role = rolesClaim.toString();
                        } else {
                            role = "STUDENT";
                        }
                    }

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // Ensure role doesn't already contain ROLE_ prefix
                        String normalizedRole = role.startsWith("ROLE_") ? role.substring(5) : role;

                        // Create authority with ROLE_ prefix
                        org.springframework.security.core.authority.SimpleGrantedAuthority authority =
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + normalizedRole);

                        org.springframework.security.core.userdetails.User userDetails =
                                new org.springframework.security.core.userdetails.User(
                                        username,
                                        "",
                                        java.util.Collections.singletonList(authority)
                                );

                        if (jwtService.isTokenValid(token, userDetails)) {

                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);

                        } else {
                            System.out.println("JWT token invalid for user: " + username);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
        } else {

        }

        filterChain.doFilter(request, response);
    }
}

