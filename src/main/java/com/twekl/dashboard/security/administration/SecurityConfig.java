package com.twekl.dashboard.security.administration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Enterprise Spring Security Configuration.
 * Centralized inside the security folder for full visibility over:
 * 1. BCrypt Password Encryption
 * 2. Authentication Provider & Session Management
 * 3. Role-Based and Permission-Based Access Control Rules
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Autowired
    public SecurityConfig(final UserDetailsService userDetailsService,
                          final RestAuthenticationEntryPoint authenticationEntryPoint,
                          final RestAccessDeniedHandler accessDeniedHandler) {
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /**
     * Password Encoder: BCrypt with default 10 rounds of hashing.
     * All system passwords are encrypted using this bean before storage.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication Provider: Uses CustomUserDetailsService to load users,
     * roles, and permissions, verifying raw credentials against the BCrypt password.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(final AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean(name = "mvcHandlerMappingIntrospector")
    public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
        return new HandlerMappingIntrospector();
    }

    /**
     * Security Filter Chain: Defines authentication endpoints, session creation,
     * CSRF protection, and role/permission authorization checks.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .securityContext(securityContext -> securityContext
                .securityContextRepository(securityContextRepository())
            )
            .authenticationProvider(authenticationProvider())
            .httpBasic(org.springframework.security.config.Customizer.withDefaults())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            // Explicit authorization checks:
            // - Public assets & login form: permitAll
            // - Staff administrator management: requires ROLE_SUPER_ADMIN or PERMISSION_MANAGE_USERS
            // - Role & permission templates: requires ROLE_SUPER_ADMIN or PERMISSION_CREATE_ROLES
            // - General portal routes (/admin/**): requires ROLE_ADMIN or ROLE_SUPER_ADMIN
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(antMatcher("/"), antMatcher("/login"), antMatcher("/login/**"), antMatcher("/css/**"), antMatcher("/js/**"), antMatcher("/images/**"), antMatcher("/favicon.ico")).permitAll()
                .requestMatchers(antMatcher("/admin/auth/login")).permitAll()
                .requestMatchers(antMatcher("/admin/admins"), antMatcher("/admin/admins/**")).hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_MANAGE_USERS")
                .requestMatchers(antMatcher("/admin/roles"), antMatcher("/admin/roles/**")).hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_CREATE_ROLES")
                .requestMatchers(antMatcher("/admin/**")).hasAnyRole("SUPER_ADMIN", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/admin/dashboard", true)
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout"))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        return http.build();
    }
}
