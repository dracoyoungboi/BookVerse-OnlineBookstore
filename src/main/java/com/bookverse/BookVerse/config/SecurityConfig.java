package com.bookverse.BookVerse.config;

import com.bookverse.BookVerse.service.CustomOAuth2UserService;
import com.bookverse.BookVerse.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Autowired(required = false)
    private CustomOAuth2AuthenticationSuccessHandler customOAuth2AuthenticationSuccessHandler;

    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Log OAuth2 configuration status
        System.out.println("[DEBUG] Configuring SecurityFilterChain...");
        System.out.println("[DEBUG] ClientRegistrationRepository available: " + (clientRegistrationRepository != null));
        System.out.println("[DEBUG] CustomOAuth2UserService available: " + (customOAuth2UserService != null));
        System.out.println("[DEBUG] CustomOAuth2AuthenticationSuccessHandler available: " + (customOAuth2AuthenticationSuccessHandler != null));
        
        http
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints and static resources - must be first
                        .requestMatchers(
                                "/",
                                "/index",
                                "/login",
                                "/register",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/user/**",           // static assets served under /user/**
                                "/admin/**",          // admin static assets
                                "/common/**",         // thymeleaf fragments
                                "/img/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/GoogleLogin",
                                "/debug/oauth/**"
                        ).permitAll()
                        // Protected by role
                        .requestMatchers("/demo/admin").hasRole("ADMIN")
                        // Public user page
                        .requestMatchers("/demo/user").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureHandler(customAuthenticationFailureHandler)
                        .permitAll()
                );
        
        // Always configure OAuth2 login - Spring Boot will handle if beans are available
        http.oauth2Login(oauth2 -> {
            oauth2.loginPage("/login")
                    .failureUrl("/login?error=true");
            
            // Only set custom handlers if available
            if (customOAuth2AuthenticationSuccessHandler != null) {
                oauth2.successHandler(customOAuth2AuthenticationSuccessHandler);
                System.out.println("[DEBUG] Using custom OAuth2 success handler");
            }
            
            oauth2.userInfoEndpoint(userInfo -> {
                userInfo.userService(customOAuth2UserService);
                System.out.println("[DEBUG] Using custom OAuth2 user service: " + customOAuth2UserService.getClass().getName());
            });
        });
        
        System.out.println("[DEBUG] OAuth2 login configuration completed");
        
        http.logout(logout -> logout
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            // Session is already invalidated by Spring Security
                            // Create a new session for the logout message
                            HttpSession newSession = request.getSession(true);
                            newSession.setAttribute("logoutMessage", "You have been logged out successfully.");
                            // Redirect to home page
                            response.sendRedirect("/demo/user?logout=true");
                        })
                        .permitAll()
                )
                .userDetailsService(customUserDetailsService);

        return http.build();
    }
}

