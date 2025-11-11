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
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;

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
                        // 1. Static resources - must be first (always public)
                        // Allow admin static assets before protecting admin routes
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/img/**",
                                "/user/**",           // static assets served under /user/**
                                "/common/**",         // thymeleaf fragments
                                "/admin/css/**",      // admin static CSS (if exists)
                                "/admin/js/**",       // admin static JS (if exists)
                                "/admin/images/**",   // admin static images (if exists)
                                "/admin/img/**"       // admin static img (if exists)
                        ).permitAll()
                        
                        // 2. Admin routes - require ADMIN role
                        // This protects all admin pages (present and future)
                        .requestMatchers(
                                "/demo/admin",
                                "/admin/**"           // All admin routes (pages, APIs, etc.)
                        ).hasRole("ADMIN")
                        
                        // 3. Authentication and OAuth2 routes - public
                        .requestMatchers(
                                "/login",
                                "/register",
                                "/forgot-password",
                                "/reset-password",
                                "/logout",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/GoogleLogin",
                                "/perform_login",
                                "/debug/oauth/**"
                        ).permitAll()
                        
                        // 4. Checkout and payment routes - require authentication (will redirect to login if not authenticated)
                        .requestMatchers(
                                "/checkout/**",
                                "/payment/**",
                                "/order/**",
                                "/orders/**"
                        ).authenticated()
                        
                        // 4.1. My Account route - require authentication
                        .requestMatchers(
                                "/my-account"
                        ).authenticated()
                        
                        // 4.2. Wishlist routes - allow API endpoints to handle auth in controller
                        .requestMatchers(
                                "/wishlist/check",
                                "/wishlist/add",
                                "/wishlist/remove",
                                "/wishlist/toggle"
                        ).permitAll() // Allow API endpoints - controller will check auth and return JSON
                        .requestMatchers(
                                "/wishlist" // Only protect the page view
                        ).authenticated()
                        
                        // 5. Cart routes - public (can view cart without login)
                        .requestMatchers(
                                "/cart/**"
                        ).permitAll()
                        
                        // 6. Public pages - allow all (home, shop, about, contact, user page, etc.)
                        .requestMatchers(
                                "/",
                                "/index",
                                "/shop/**",
                                "/about",
                                "/contact",
                                "/demo/user"
                        ).permitAll()
                        
                        // 7. All other requests - permit all by default (for future pages)
                        // Change to .authenticated() if you want to protect all other routes
                        .anyRequest().permitAll()
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
                        .deleteCookies("JSESSIONID", "remember-me")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("bookverse-remember-me-secret-key-2024") // Secret key to encrypt remember-me token
                        .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 days in seconds
                        .userDetailsService(customUserDetailsService)
                        .rememberMeParameter("rememberme") // Match the checkbox name in login form
                        .rememberMeCookieName("remember-me") // Cookie name
                )
                .userDetailsService(customUserDetailsService)
                // Configure CSRF - disable for API endpoints that use fetch/AJAX
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/wishlist/add",
                                "/wishlist/remove", 
                                "/wishlist/toggle"
                        ) // Disable CSRF for wishlist API POST endpoints
                )
                // Configure exception handling for access denied
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedPage("/login?error=access_denied")
                );

        return http.build();
    }
}

