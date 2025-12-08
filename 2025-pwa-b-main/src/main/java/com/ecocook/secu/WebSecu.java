package com.ecocook.secu;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration centrale de Spring Security pour l'application.
 * On y définit les règles d'accès et l'authentification par formulaire.
 */
@Configuration
@EnableWebSecurity
public class WebSecu {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Pages publiques accessibles sans authentification
                .requestMatchers(
                    "/",
                    "/login",
                    "/register",
                    "/recipes",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/uploads/**"
                ).permitAll()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers(PathRequest.toH2Console()).permitAll()
                
                // Pages réservées aux membres inscrits (USER ou ADMIN)
                .requestMatchers(
                    "/scanner",
                    "/pantry/**",
                    "/shopping-list/**",
                    "/profile",
                    "/profile/**",
                    "/recipes/new",
                    "/recipes/edit/**",
                    "/recipes/save",
                    "/recipes/delete/**",
                    "/recipes/*/report",
                    "/recipes/*/review",
                    "/recipes/*/toggle-selection",
                    "/recipes/reviews/*/report",
                    "/recipes/reviews/*/reply",
                    "/recipes/reviews/*/edit",
                    "/recipes/reviews/*/delete",
                    "/recipes/reviews/replies/*/edit",
                    "/recipes/reviews/replies/*/delete",
                    "/recipes/reviews/replies/*/report"
                ).hasAnyRole("USER", "ADMIN")
                
                // Zone d'administration strictement réservée aux administrateurs
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(PathRequest.toH2Console())
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            )
            .formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/pantry", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}