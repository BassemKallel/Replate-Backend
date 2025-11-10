package com.replate.replatebackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// S'exécute une fois par requête
public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils; // Notre utilitaire (étape 4)

    @Autowired
    private UserDetailsServiceImpl userDetailsService; // Notre service (étape 2)

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. Lire le token depuis le header "Authorization"
            String jwt = parseJwt(request);

            // 2. S'il y a un token et qu'il est valide...
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

                // 3. Extraire le 'username' du token
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                // 4. Charger l'utilisateur depuis la BDD
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 5. Créer une authentification
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Placer l'utilisateur authentifié dans le contexte de sécurité
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        // Passe la requête au filtre suivant
        filterChain.doFilter(request, response);
    }

    // Méthode pour extraire le token (en enlevant "Bearer ")
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7); // Récupère le token après "Bearer "
        }
        return null;
    }
}