package com.sigepes.backend.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.users.UserRepository;
import com.sigepes.backend.users.UserStatus;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Valida el token Bearer y establece el principal.
 *
 * <p>Se releen rol y estado desde la base de datos en cada petición: así una suspensión o un cambio
 * de rol hechos por el administrador surten efecto de inmediato, sin esperar a que caduque el token.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            authenticate(header.substring(7));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            Claims claims = tokenProvider.parse(token);
            UserEntity user = userRepository.findById(claims.getSubject()).orElse(null);
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                SecurityContextHolder.clearContext();
                return;
            }
            Role role = user.getRole();
            AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getFullName(),
                    role);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}
