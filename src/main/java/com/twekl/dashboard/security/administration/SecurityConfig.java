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
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import jakarta.servlet.http.HttpSession;

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

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            HttpSession session = request.getSession(true);
            if (session != null) {
                session.setMaxInactiveInterval(30 * 24 * 60 * 60); // 30 days in seconds
            }

            String target = request.getParameter("redirect");
            if (target == null || target.trim().isEmpty() || target.equals("/login")) {
                if (session != null) {
                    target = (String) session.getAttribute("TARGET_AFTER_LOGIN");
                    if (target != null) session.removeAttribute("TARGET_AFTER_LOGIN");
                }
            }
            if (target == null || target.trim().isEmpty()) {
                jakarta.servlet.http.Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (jakarta.servlet.http.Cookie c : cookies) {
                        if ("TARGET_AFTER_LOGIN".equals(c.getName()) && c.getValue() != null && !c.getValue().trim().isEmpty()) {
                            try {
                                target = java.net.URLDecoder.decode(c.getValue(), java.nio.charset.StandardCharsets.UTF_8);
                            } catch (Exception ignored) {}
                            jakarta.servlet.http.Cookie clear = new jakarta.servlet.http.Cookie("TARGET_AFTER_LOGIN", "");
                            clear.setPath("/");
                            clear.setMaxAge(0);
                            response.addCookie(clear);
                            break;
                        }
                    }
                }
            }

            if (target != null && !target.trim().isEmpty() && !target.equals("/login")) {
                response.sendRedirect(target.trim());
                return;
            }

            SavedRequestAwareAuthenticationSuccessHandler delegate = new SavedRequestAwareAuthenticationSuccessHandler();
            delegate.setDefaultTargetUrl("/admin/dashboard");
            delegate.onAuthenticationSuccess(request, response, authentication);
        };
    }

    /**
     * Security Filter Chain: Defines authentication endpoints, session creation,
     * CSRF protection, and role/permission authorization checks.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
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
                .requestMatchers(
                    antMatcher("/"), antMatcher("/login"), antMatcher("/login/**"),
                    antMatcher("/css/**"), antMatcher("/js/**"), antMatcher("/images/**"), antMatcher("/favicon.ico"),
                    antMatcher("/review"), antMatcher("/review/**"),
                    antMatcher("/product-review"), antMatcher("/product-review/**"),
                    antMatcher("/api/review"), antMatcher("/api/review/**"),
                    antMatcher("/api/reviews"), antMatcher("/api/reviews/**"),
                    antMatcher("/api/last-review"), antMatcher("/api/last-review/**"),
                    antMatcher("/api/last-reviews"), antMatcher("/api/last-reviews/**"),
                    antMatcher("/api/latest-review"), antMatcher("/api/latest-review/**"),
                    antMatcher("/api/latest-reviews"), antMatcher("/api/latest-reviews/**"),
                    antMatcher("/api/customer-reviews"), antMatcher("/api/customer-reviews/**"),
                    antMatcher("/followup"), antMatcher("/followup/**"),
                    antMatcher("/followups"), antMatcher("/followups/**"),
                    antMatcher("/api/followup"), antMatcher("/api/followup/**"),
                    antMatcher("/api/jwt"), antMatcher("/api/jwt/**"),
                    antMatcher("/api/session"), antMatcher("/api/session/**"),
                    antMatcher("/api/external/**"), antMatcher("/admin/external/**"),
                    antMatcher("/api/admins"), antMatcher("/api/admins/**"),
                    antMatcher("/api/users"), antMatcher("/api/users/**")
                ).permitAll()
                .requestMatchers(
                    antMatcher("/admin/auth/**"),
                    antMatcher("/api/auth/**"),
                    antMatcher("/api/login"),
                    antMatcher("/api/token"),
                    antMatcher("/oauth/token")
                ).permitAll()
                .requestMatchers(antMatcher("/admin/admins"), antMatcher("/admin/admins/**")).hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_MANAGE_USERS", "ACTION_STAFF_MANAGE", "ACTION_STAFF_PERMISSIONS")
                .requestMatchers(antMatcher("/admin/roles"), antMatcher("/admin/roles/**")).hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_CREATE_ROLES", "ACTION_STAFF_PERMISSIONS", "ACTION_STAFF_MANAGE")
                .requestMatchers(antMatcher("/admin/**")).hasAnyRole("SUPER_ADMIN", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(authenticationSuccessHandler())
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout"))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        return http.build();
    }
}
