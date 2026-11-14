package de.tho.reloading.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/** Enables {@code @PreAuthorize}, see MasterDataRepository. */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
