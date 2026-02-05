package com.cts.backend.jwt.jwtprovider;

import com.cts.backend.jwt.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtFilter extends OncePerRequestFilter {

    // We need the service to load the user object
    private final CustomUserDetailsService userDetailsService;

    public JwtFilter(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip filter for auth endpoints
        return request.getServletPath().startsWith("/api/auth");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // 1. Validate Token
                Claims claims = JwtUtil.validateToken(token);
                String username = claims.getSubject();

                // 2. If token is valid and no auth is set in context yet
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // 3. Load User Details from Database
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // 4. Create Authentication Token
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 5. CRITICAL STEP: Set the Authentication in Spring Security Context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // If token is invalid, we can just let the request continue;
                // Spring Security will catch the 403 later if the endpoint is protected.
                System.out.println("JWT Verification failed: " + e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}