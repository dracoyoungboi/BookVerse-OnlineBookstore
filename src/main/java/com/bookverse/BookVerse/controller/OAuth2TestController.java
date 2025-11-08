package com.bookverse.BookVerse.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class OAuth2TestController {

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @GetMapping("/debug/oauth")
    public String debugOAuth(Model model) {
        List<String> info = new ArrayList<>();
        
        if (clientRegistrationRepository == null) {
            info.add("ERROR: ClientRegistrationRepository is NULL!");
            info.add("OAuth2 is NOT configured properly.");
        } else {
            info.add("SUCCESS: ClientRegistrationRepository is available");
            try {
                // Try to get Google registration
                ClientRegistration google = clientRegistrationRepository.findByRegistrationId("google");
                if (google != null) {
                    info.add("Google OAuth2 Client Registration found:");
                    info.add("  - Client ID: " + google.getClientId());
                    info.add("  - Redirect URI: " + google.getRedirectUri());
                    info.add("  - Scopes: " + google.getScopes());
                    info.add("  - Authorization URI: " + google.getProviderDetails().getAuthorizationUri());
                    info.add("  - Token URI: " + google.getProviderDetails().getTokenUri());
                } else {
                    info.add("ERROR: Google registration not found!");
                }
            } catch (Exception e) {
                info.add("ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        model.addAttribute("info", info);
        return "debug/oauth-debug";
    }
}

