package com.bookverse.BookVerse.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Development helper: logs configured OAuth2 client registrations at startup.
 * This helps verify that GOOGLE client-id/secret were picked up from env vars.
 * Only runs if ClientRegistrationRepository bean exists (i.e., OAuth2 is configured).
 */
@Component
@ConditionalOnBean(ClientRegistrationRepository.class)
public class OAuth2DebugLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OAuth2DebugLogger.class);

    private final ClientRegistrationRepository clientRegistrationRepository;

    public OAuth2DebugLogger(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<ClientRegistration> regs = new ArrayList<>();
            if (clientRegistrationRepository != null) {
                // ClientRegistrationRepository is iterable in many implementations
                if (clientRegistrationRepository instanceof Iterable) {
                    for (Object o : (Iterable<?>) clientRegistrationRepository) {
                        if (o instanceof ClientRegistration) regs.add((ClientRegistration) o);
                    }
                }
            }

            if (regs.isEmpty()) {
                log.warn("No OAuth2 client registrations found. Google OAuth will not work until you set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET env vars or configure clients in application.properties");
            } else {
                for (ClientRegistration r : regs) {
                    log.info("OAuth2 client configured: registrationId={}, clientId={}, redirectUriTemplate={}", r.getRegistrationId(), r.getClientId(), r.getRedirectUri());
                }
            }
        } catch (Exception ex) {
            log.error("Failed to inspect ClientRegistrationRepository: {}", ex.getMessage());
        }
    }
}
