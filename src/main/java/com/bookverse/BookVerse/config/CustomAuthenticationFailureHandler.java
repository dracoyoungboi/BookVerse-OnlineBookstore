package com.bookverse.BookVerse.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       AuthenticationException exception) throws IOException, ServletException {
        
        // Get username from request
        String username = request.getParameter("username");
        
        // Save username to session so it can be displayed in the login form
        HttpSession session = request.getSession();
        if (username != null && !username.isEmpty()) {
            session.setAttribute("lastUsername", username);
        }
        
        // Redirect to login page with error parameter
        response.sendRedirect("/login?error=true");
    }
}

