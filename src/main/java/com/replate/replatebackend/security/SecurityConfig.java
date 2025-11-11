package com.replate.replatebackend.security;
// package com.replate.replatebackend.security;
// (ou com.replate.replatebackend.config)

import com.replate.replatebackend.security.AuthEntryPointJwt;
import com.replate.replatebackend.security.AuthTokenFilter;
import com.replate.replatebackend.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // Permet d'utiliser @PreAuthorize plus tard
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService; // Notre service (étape 2)

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler; // Gestion Erreur 401 (étape 5)

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter(); // Notre filtre (étape 3)
    }

    // Définit comment Spring va charger les utilisateurs et vérifier les mots de passe
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService); // Utilise notre service
        authProvider.setPasswordEncoder(passwordEncoder()); // Utilise notre encodeur

        return authProvider;
    }

    // Rend le gestionnaire d'authentification disponible pour notre AuthController
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // L'encodeur de mot de passe (très important)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Le filtre principal de sécurité
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Désactive CSRF pour les API stateless
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler)) // Gère les erreurs 401
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Pas de session
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/api/auth/**").permitAll() // Routes publiques (signin, signup)
                                .anyRequest().authenticated()
                        // Toutes les autres routes nécessitent une authentification
                );

        // Ajoute notre DaoAuthenticationProvider
        http.authenticationProvider(authenticationProvider());

        // Ajoute notre filtre JWT avant le filtre de base de Spring
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}