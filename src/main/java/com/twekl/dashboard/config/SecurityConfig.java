package com.twekl.dashboard.config;

import com.twekl.dashboard.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Autowired
    public SecurityConfig(CustomUserDetailsService userDetailsService,
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
            .authenticationProvider(authenticationProvider())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                // Static web resources & web landing / SPA routes
                .requestMatchers("/", "/index.html", "/login", "/admin/**", "/followups/**", "/users/**", "/roles/**", "/role-templates/**", "/admins/**", "/customers/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                
                // Public Authentication Endpoints
                .requestMatchers("/api/auth/**").permitAll()
                
                // Super Administrator Sensitive Operations
                .requestMatchers(HttpMethod.POST, "/api/admins/**").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/admins/**").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/admins/**").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/admins/**").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admins/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                
                // Role Templates & Permission Controls
                .requestMatchers(HttpMethod.POST, "/api/roles/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_CREATE_ROLES")
                .requestMatchers(HttpMethod.PATCH, "/api/roles/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_CREATE_ROLES")
                .requestMatchers(HttpMethod.DELETE, "/api/roles/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "PERMISSION_CREATE_ROLES")
                .requestMatchers(HttpMethod.GET, "/api/roles/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                
                // User Management Endpoints
                .requestMatchers("/api/users/**").hasAnyAuthority("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_MANAGE_USERS")
                
                // Customer Follow-up, Orders, Presets, Dashboard Stats & Reports
                .requestMatchers("/api/customers/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/api/orders/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/api/time-filters/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/api/stats/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/api/reports/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                
                // All other API endpoints require authenticated session
                .requestMatchers("/api/**").authenticated()
                
                // Any other SPA page routes
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        return http.build();
    }
}
