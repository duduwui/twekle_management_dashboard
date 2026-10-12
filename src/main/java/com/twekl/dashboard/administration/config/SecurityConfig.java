package com.twekl.dashboard.administration.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(UserDetailsService userDetailsService,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler) {
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .securityContext(securityContext -> securityContext
                .securityContextRepository(securityContextRepository())
            )
            .authenticationProvider(authenticationProvider())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                // Static web resources & web landing / SPA routes
                .requestMatchers("/", "/login", "/admin/**", "/manager/**", "/followups/**", "/users/**", "/roles/**", "/role-templates/**", "/admins/**", "/customers/**", "/resources/**", "/static/**", "/app/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                
                // OpenAPI 3.0 / Swagger UI Documentation & Actuator Probes
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                
                // Public Authentication Endpoints
                .requestMatchers("/admin/auth/**", "/auth/**", "/admin/api/auth/**", "/internal/auth/**", "/api/auth/**").permitAll()
                
                // Super Administrator Sensitive Operations
                .requestMatchers(HttpMethod.POST, "/admin/api/admins/**", "/internal/admins/**", "/api/admins/**", "/api/admin/admins/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_MANAGE_USERS")
                .requestMatchers(HttpMethod.PUT, "/admin/api/admins/**", "/internal/admins/**", "/api/admins/**", "/api/admin/admins/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_CREATE_ROLES", "PERMISSION_MANAGE_USERS")
                .requestMatchers(HttpMethod.PATCH, "/admin/api/admins/**", "/internal/admins/**", "/api/admins/**", "/api/admin/admins/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_MANAGE_USERS")
                .requestMatchers(HttpMethod.DELETE, "/admin/api/admins/**", "/internal/admins/**", "/api/admins/**", "/api/admin/admins/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_MANAGE_USERS")
                .requestMatchers(HttpMethod.GET, "/admin/api/admins/**", "/internal/admins/**", "/api/admins/**", "/api/admin/admins/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                
                // Role Templates & Permission Controls
                .requestMatchers(HttpMethod.POST, "/admin/api/roles/**", "/internal/roles/**", "/api/roles/**", "/api/admin/roles/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_CREATE_ROLES")
                .requestMatchers(HttpMethod.PATCH, "/admin/api/roles/**", "/internal/roles/**", "/api/roles/**", "/api/admin/roles/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_CREATE_ROLES")
                .requestMatchers(HttpMethod.DELETE, "/admin/api/roles/**", "/internal/roles/**", "/api/roles/**", "/api/admin/roles/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_CREATE_ROLES")
                .requestMatchers(HttpMethod.GET, "/admin/api/roles/**", "/internal/roles/**", "/api/roles/**", "/api/admin/roles/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                
                // User Management Endpoints
                .requestMatchers("/internal/users/**", "/api/users/**", "/api/admin/users/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_MANAGE_USERS")
                
                // Customer Follow-up, Orders, Presets, Dashboard Stats & Reports
                .requestMatchers("/admin/api/customers/**", "/internal/customers/**", "/api/customers/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/admin/api/orders/**", "/internal/orders/**", "/api/orders/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/admin/api/followups/**", "/internal/followups/**", "/api/followups/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/admin/api/timelines/**", "/admin/api/time-filters/**", "/internal/time-filters/**", "/api/time-filters/**", "/api/admin/time-filters/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/admin/api/stats/**", "/internal/stats/**", "/api/stats/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/admin/api/reports/**", "/internal/reports/**", "/api/reports/**", "/api/admin/reports/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                
                // All other internal data and API endpoints require authenticated session
                .requestMatchers("/admin/api/**", "/internal/**", "/api/**").authenticated()
                
                // Any other SPA page routes
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        return http.build();
    }
}

