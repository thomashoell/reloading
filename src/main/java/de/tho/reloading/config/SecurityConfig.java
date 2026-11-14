package de.tho.reloading.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Authentication is the default: every request except the health endpoint needs an OAuth2/OIDC login.
 * The profile {@value #NO_AUTH_PROFILE} switches it off for local development; it has to be requested explicitly.
 */
@Configuration
public class SecurityConfig {

    public static final String NO_AUTH_PROFILE = "noauth";

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    @Profile("!" + NO_AUTH_PROFILE)
    SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http,
                                                  ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        // also ends the session at the identity provider, then returns to the application
        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessHandler(logoutSuccessHandler));
        return http.build();
    }

    @Bean
    @Profile(NO_AUTH_PROFILE)
    SecurityFilterChain noAuthSecurityFilterChain(HttpSecurity http) throws Exception {
        log.warn("Profile '{}' is active: AUTHENTICATION IS DISABLED, every request runs as user '{}'. "
                + "Never use this profile outside of local development.", NO_AUTH_PROFILE, NoAuthUserFilter.USERNAME);

        http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                // there is no login page to return to; the next request is the development user again
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .addFilterBefore(new NoAuthUserFilter(), AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
