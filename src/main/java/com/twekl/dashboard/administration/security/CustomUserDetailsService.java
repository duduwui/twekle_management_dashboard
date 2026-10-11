package com.twekl.dashboard.administration.security;

import com.twekl.dashboard.administration.model.admin.Admin;
import com.twekl.dashboard.administration.model.role.Role;
import com.twekl.dashboard.administration.repository.admin.AdminRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    public CustomUserDetailsService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found with username: " + username));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        if (admin.isSuperAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }
        if (admin.isSuperAdmin() || admin.isCanCreateRoles()) {
            authorities.add(new SimpleGrantedAuthority("PERMISSION_CREATE_ROLES"));
        }
        if (admin.isSuperAdmin() || admin.isCanManageUsers()) {
            authorities.add(new SimpleGrantedAuthority("PERMISSION_MANAGE_USERS"));
        }
        if (admin.getRoles() != null) {
            for (Role role : admin.getRoles()) {
                if (role != null && role.getName() != null && !role.getName().trim().isEmpty()) {
                    String raw = role.getName().trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
                    String roleName = raw.startsWith("ROLE_") ? raw : "ROLE_" + raw;
                    authorities.add(new SimpleGrantedAuthority(roleName));
                }
            }
        }

        return User.builder()
                .username(admin.getUsername())
                .password(admin.getPassword())
                .disabled(!admin.isActive())
                .authorities(authorities)
                .build();
    }
}

